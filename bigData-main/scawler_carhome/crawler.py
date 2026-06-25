import re
import json
import time
import random
import logging
import asyncio
import sys
from datetime import datetime
from pathlib import Path

_BASE_DIR = Path(__file__).resolve().parent
if str(_BASE_DIR) not in sys.path:
    sys.path.insert(0, str(_BASE_DIR))

import requests
import aiohttp
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from config import (
    API_URL_TEMPLATE, HEADERS, TARGET_CATEGORIES,
    MIN_DELAY, MAX_DELAY, MAX_RETRIES, REQUEST_TIMEOUT,
    MAX_CONCURRENCY, RETRY_BACKOFF_BASE, RETRY_BACKOFF_MAX,
    CSV_FIELDS, CHECKPOINT_FILE, FAILED_FILE, OUTPUT_EXCEL,
    resolve_path, get_headers,
)

logger = logging.getLogger(__name__)


# ============================================================
#  通用工具函数
# ============================================================

def extract_series_id(url):
    match = re.search(r'series/(\d+)\.html', url)
    return match.group(1) if match else None


def _format_param_value(param):
    """将API参数对象格式化为可读字符串（纯函数）"""
    if param.get('sublist'):
        parts = []
        for sub in param['sublist']:
            if sub['value'] == '●':
                parts.append(sub['name'])
            elif sub['value'] == '○':
                parts.append(f"{sub['name']}(选配)")
            else:
                parts.append(f"{sub['name']}: {sub['value']}")
        return '; '.join(parts)
    return param.get('itemname') or ''


def parse_series_result(result):
    """
    解析API返回的result，提取所有版本的参数
    返回: (brand_name, series_name, rows)
      其中 rows 是 list[dict]，每个 dict key 为 CSV_FIELDS 中的字段名
    """
    brand_name = result['bread']['brandname']
    series_name = result['bread']['seriesname']

    # 建立 titleid → (category, param_name) 映射
    param_map = {}
    for title_group in result['titlelist']:
        category = title_group['itemtype']
        if category in TARGET_CATEGORIES:
            for item in title_group['items']:
                param_map[item['titleid']] = (category, item['itemname'])

    rows = []
    for version in result['datalist']:
        row = {
            '品牌': brand_name,
            '基本参数_厂商指导价': version.get('minprice', ''),
        }
        for param in version.get('paramconflist', []):
            titleid = param['titleid']
            if titleid in param_map:
                category, param_name = param_map[titleid]
                row[f"{category}_{param_name}"] = _format_param_value(param)

        rows.append(row)

    return brand_name, series_name, rows


# ============================================================
#  断点续爬 & 失败队列
# ============================================================

class CheckpointManager:
    """管理爬取进度，支持断点续爬"""

    def __init__(self, filepath=None):
        self.filepath = resolve_path(filepath or CHECKPOINT_FILE)
        self.completed: set[str] = set()
        self._load()

    def _load(self):
        if self.filepath.exists():
            try:
                data = json.loads(self.filepath.read_text(encoding='utf-8'))
                self.completed = set(data.get('completed', []))
                logger.info("加载checkpoint: %d 个已完成的series_id", len(self.completed))
            except (json.JSONDecodeError, KeyError):
                logger.warning("checkpoint文件损坏，从头开始")
                self.completed = set()

    def save(self):
        data = {
            'completed': sorted(self.completed),
            'last_update': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        }
        self.filepath.parent.mkdir(parents=True, exist_ok=True)
        self.filepath.write_text(json.dumps(data, ensure_ascii=False, indent=2),
                                 encoding='utf-8')

    def mark_done(self, series_id):
        self.completed.add(series_id)
        self.save()

    def is_done(self, series_id):
        return series_id in self.completed

    def __len__(self):
        return len(self.completed)


class FailedQueue:
    """记录失败URL，支持单独重试"""

    def __init__(self, filepath=None):
        self.filepath = resolve_path(filepath or FAILED_FILE)
        self.failed: list[dict] = []
        self._load()

    def _load(self):
        if self.filepath.exists():
            try:
                self.failed = json.loads(self.filepath.read_text(encoding='utf-8'))
            except (json.JSONDecodeError, KeyError):
                self.failed = []

    def save(self):
        self.filepath.parent.mkdir(parents=True, exist_ok=True)
        self.filepath.write_text(json.dumps(self.failed, ensure_ascii=False, indent=2),
                                 encoding='utf-8')

    def add(self, url, series_id, reason):
        entry = {
            'url': url,
            'series_id': series_id,
            'reason': str(reason),
            'time': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        }
        self.failed.append(entry)
        self.save()

    def get_urls(self):
        """返回待重试的URL列表"""
        return [e['url'] for e in self.failed]

    def clear(self):
        self.failed.clear()
        if self.filepath.exists():
            self.filepath.unlink()

    def __len__(self):
        return len(self.failed)


# ============================================================
#  异步爬取引擎 (aiohttp)
# ============================================================

async def _fetch_json_async(session, semaphore, series_id):
    """异步获取API JSON，带并发控制、重试、自适应退避，每次请求轮换Cookie"""
    url = API_URL_TEMPLATE.format(seriesid=series_id)

    for attempt in range(1, MAX_RETRIES + 2):
        async with semaphore:
            try:
                async with session.get(url, headers=get_headers(),
                                       timeout=aiohttp.ClientTimeout(total=REQUEST_TIMEOUT)) as resp:
                    if resp.status == 403:
                        logger.critical("403 Forbidden (series_id=%s) — 可能IP被封或Cookie过期，请检查!", series_id)
                        return None
                    if resp.status == 429:
                        wait = min(RETRY_BACKOFF_BASE * (2 ** (attempt - 1)), RETRY_BACKOFF_MAX)
                        logger.warning("429 Too Many Requests (series_id=%s)，等待 %.0fs 重试 (%d/%d)",
                                       series_id, wait, attempt, MAX_RETRIES + 1)
                        await asyncio.sleep(wait)
                        continue
                    if resp.status != 200:
                        logger.warning("HTTP %d (series_id=%s), attempt %d",
                                       resp.status, series_id, attempt)
                        if attempt <= MAX_RETRIES:
                            await asyncio.sleep(random.uniform(1, 3))
                            continue
                        return None

                    text = await resp.text()
                    # Cookie过期检测：被重定向到登录页
                    if 'login' in resp.url.path.lower() or 'passport' in resp.url.host.lower():
                        logger.critical("检测到登录重定向 — Cookie可能已过期！请更新 .env 中的 AUTOHOME_COOKIE")
                        return None

                    return json.loads(text)

            except asyncio.TimeoutError:
                logger.warning("请求超时 (series_id=%s), attempt %d", series_id, attempt)
                if attempt <= MAX_RETRIES:
                    await asyncio.sleep(random.uniform(1, 3))
            except aiohttp.ClientError as e:
                logger.error("网络错误 (series_id=%s): %s", series_id, e)
                if attempt <= MAX_RETRIES:
                    await asyncio.sleep(random.uniform(2, 5))

    return None


async def _crawl_one_async(session, semaphore, url, csv_appender, checkpoint, failed_queue):
    """异步爬取单个车系，解析后立即追加写入CSV"""
    series_id = extract_series_id(url)

    if not series_id:
        logger.warning("跳过无效URL: %s", url)
        return 0

    if checkpoint.is_done(series_id):
        logger.debug("跳过已完成: series_id=%s", series_id)
        return 0

    logger.info("爬取 series_id=%s", series_id)

    json_data = await _fetch_json_async(session, semaphore, series_id)
    if not json_data or 'result' not in json_data:
        failed_queue.add(url, series_id, '网络请求失败或数据格式错误')
        logger.warning("series_id=%s 失败，已记录到失败队列", series_id)
        return 0

    try:
        _, _, rows = parse_series_result(json_data['result'])
    except KeyError as e:
        failed_queue.add(url, series_id, f'数据字段缺失: {e}')
        logger.error("解析失败 series_id=%s: %s", series_id, e)
        return 0

    # 转换为 CSV 行顺序
    csv_rows = []
    for row in rows:
        csv_rows.append([row.get(field, '') for field in CSV_FIELDS])

    # 写入CSV + 写checkpoint（保证一致性：先写数据再标记完成）
    csv_appender.append_rows(csv_rows)
    checkpoint.mark_done(series_id)

    logger.info("series_id=%s 完成，写入 %d 条", series_id, len(rows))
    return len(rows)


async def crawl_urls_async(urls, checkpoint=None, failed_queue=None,
                           csv_appender=None, max_concurrency=None):
    """
    异步并发爬取所有URL，增量写入CSV

    返回: (total_versions, failed_count)
    """
    if max_concurrency is None:
        max_concurrency = MAX_CONCURRENCY
    if checkpoint is None:
        checkpoint = CheckpointManager()
    if failed_queue is None:
        failed_queue = FailedQueue()
    if csv_appender is None:
        from export import CsvAppender
        csv_appender = CsvAppender(OUTPUT_EXCEL.replace('.xlsx', '.csv'), CSV_FIELDS)
        csv_appender.init_header()

    # 过滤已完成的URL
    pending_urls = []
    for url in urls:
        sid = extract_series_id(url)
        if sid and checkpoint.is_done(sid):
            logger.debug("跳过已完成: %s", url)
            continue
        pending_urls.append(url)

    if not pending_urls:
        logger.info("所有URL已完成，无需爬取")
        return 0, len(failed_queue)

    logger.info("待爬取: %d 个URL（已跳过 %d 个已完成）",
                 len(pending_urls), len(urls) - len(pending_urls))

    semaphore = asyncio.Semaphore(max_concurrency)
    connector = aiohttp.TCPConnector(limit=max_concurrency + 2, limit_per_host=max_concurrency)

    async with aiohttp.ClientSession(connector=connector) as session:
        tasks = [
            _crawl_one_async(session, semaphore, url, csv_appender,
                             checkpoint, failed_queue)
            for url in pending_urls
        ]
        results = await asyncio.gather(*tasks, return_exceptions=True)

    total = 0
    for i, r in enumerate(results):
        if isinstance(r, Exception):
            logger.error("未捕获异常 [%s]: %s", pending_urls[i], r)
        else:
            total += r

    return total, len(failed_queue)


# ============================================================
#  同步爬取（保留作为 fallback，小规模或调试用）
# ============================================================

def create_session():
    session = requests.Session()
    retry_strategy = Retry(
        total=MAX_RETRIES,
        backoff_factor=1,
        status_forcelist=[429, 500, 502, 503, 504],
        allowed_methods=['GET'],
    )
    adapter = HTTPAdapter(max_retries=retry_strategy)
    session.mount('https://', adapter)
    session.mount('http://', adapter)
    return session


def fetch_car_data(session, seriesid):
    url = API_URL_TEMPLATE.format(seriesid=seriesid)
    try:
        response = session.get(url, headers=get_headers(), timeout=REQUEST_TIMEOUT)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        logger.error("网络请求失败 (seriesid=%s): %s", seriesid, e)
        return None
    except ValueError:
        logger.error("JSON解析失败 (seriesid=%s)", seriesid)
        return None


class CarDataCollector:
    """汽车数据收集器（同步模式使用，全量内存存储）"""

    def __init__(self):
        self.brands = []
        self.series = []
        self.versions = []
        self.prices = []
        self.series_ids = []
        self.years = []
        self.params: dict[str, list[str]] = {}

    def __len__(self):
        return len(self.brands)

    def reset(self):
        self.brands.clear()
        self.series.clear()
        self.versions.clear()
        self.prices.clear()
        self.series_ids.clear()
        self.years.clear()
        self.params.clear()

    def crawl_series(self, session, url):
        seriesid = extract_series_id(url)
        if not seriesid:
            logger.warning("无法从URL提取seriesid: %s", url)
            return 0

        logger.info("正在爬取 seriesid=%s", seriesid)
        json_data = fetch_car_data(session, seriesid)
        if not json_data or 'result' not in json_data:
            logger.warning("数据格式错误 (seriesid=%s)", seriesid)
            return 0

        try:
            brand_name, series_name, rows = parse_series_result(json_data['result'])
        except KeyError as e:
            logger.error("数据字段缺失 (seriesid=%s): %s", seriesid, e)
            return 0

        for row in rows:
            self.brands.append(row.get('品牌', brand_name))
            self.series.append(series_name)
            self.versions.append(row.get('基本参数_级别', ''))
            self.prices.append(row.get('基本参数_厂商指导价', ''))
            self.series_ids.append(seriesid)
            self.years.append(row.get('基本参数_上市时间', ''))

            target_len = len(self.brands)
            for key, value in row.items():
                if key not in self.params:
                    self.params[key] = [''] * (target_len - 1)
                self.params[key].append(value if value else '')
            for key in self.params:
                if len(self.params[key]) < target_len:
                    self.params[key].append('')

        logger.info("seriesid=%s 完成，提取 %d 个版本", seriesid, len(rows))
        return len(rows)

    def get_param_value(self, field_name, idx):
        if field_name == '品牌':
            return self.brands[idx] if idx < len(self.brands) else ''
        if field_name == '基本参数_厂商指导价' and field_name not in self.params:
            return self.prices[idx] if idx < len(self.prices) else ''
        values = self.params.get(field_name, [])
        return values[idx] if idx < len(values) else ''


def crawl_urls(urls, collector=None):
    """同步串行爬取（fallback）"""
    if collector is None:
        collector = CarDataCollector()

    session = create_session()
    total_versions = 0

    for i, url in enumerate(urls, 1):
        logger.info("[%d/%d] %s", i, len(urls), url)
        count = collector.crawl_series(session, url)
        total_versions += count

        if i < len(urls):
            time.sleep(random.uniform(MIN_DELAY, MAX_DELAY))

    logger.info("全部完成：%d 个车系，共 %d 个版本", len(urls), total_versions)
    return collector
