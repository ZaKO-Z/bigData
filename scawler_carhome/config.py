import os
import itertools
import threading
from pathlib import Path
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / '.env')

# --- API ---
API_URL_TEMPLATE = "https://www.autohome.com.cn/web-main/car/param/getParamConf?mode=1&site=1&seriesid={seriesid}"

# --- 多Cookie加载 ---
def _load_cookies():
    """加载所有 AUTOHOME_COOKIE_N，返回列表"""
    cookies = []
    for i in range(1, 21):  # 最多20个
        c = os.getenv(f'AUTOHOME_COOKIE_{i}', '').strip()
        if c:
            cookies.append(c)
    # fallback: 兼容旧的单Cookie格式
    if not cookies:
        c = os.getenv('AUTOHOME_COOKIE', '').strip()
        if c:
            cookies.append(c)
    return cookies

AUTOHOME_COOKIES = _load_cookies()
COOKIE_COUNT = len(AUTOHOME_COOKIES)


class CookieRotator:
    """Cookie轮换器（线程安全），每个请求取一个不同的Cookie"""

    def __init__(self):
        self._cookies = AUTOHOME_COOKIES.copy()
        self._cycle = itertools.cycle(range(len(self._cookies)))
        self._lock = threading.Lock()
        self._user_agent = os.getenv(
            'USER_AGENT',
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        )

    def next_cookie(self):
        with self._lock:
            idx = next(self._cycle)
        return self._cookies[idx]

    def next_headers(self):
        """返回带有轮换Cookie的请求头"""
        return {
            'User-Agent': self._user_agent,
            'Cookie': self.next_cookie(),
        }

    def __len__(self):
        return len(self._cookies)


# 全局单例
_cookie_rotator = CookieRotator()


def get_headers():
    """获取带轮换Cookie的请求头"""
    return _cookie_rotator.next_headers()


def get_cookie_count():
    return len(_cookie_rotator)


# --- 请求头（单Cookie场景兼容） ---
HEADERS = {
    'User-Agent': os.getenv('USER_AGENT', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'),
    'Cookie': AUTOHOME_COOKIES[0] if AUTOHOME_COOKIES else '',
}

# --- 爬取配置 ---
TARGET_CATEGORIES = [
    '基本参数', '车身', '发动机', '电动机', '电池/充电',
    '变速箱', '底盘转向', '车轮制动'
]

# --- 并发控制 ---
MAX_CONCURRENCY = int(os.getenv('MAX_CONCURRENCY', '1'))
REQUEST_TIMEOUT = int(os.getenv('REQUEST_TIMEOUT', '15'))

# --- 延时与重试 ---
MIN_DELAY = 0.3
MAX_DELAY = 0.8
MAX_RETRIES = 3
RETRY_BACKOFF_BASE = 5
RETRY_BACKOFF_MAX = 120

# --- 文件路径 ---
INPUT_FILE = 'series_urls.xlsx'
OUTPUT_EXCEL = '汽车详细参数1.xlsx'
OUTPUT_CSV = 'data.csv'
CHECKPOINT_FILE = 'checkpoint.json'
FAILED_FILE = 'failed.json'

# --- CSV 导出字段 ---
CSV_FIELDS = [
    '品牌',
    '基本参数_级别',
    '基本参数_能源类型',
    '基本参数_上市时间',
    '基本参数_车身结构',
    '基本参数_厂商指导价',
    '基本参数_整车质保',
    '基本参数_变速箱',
    '基本参数_发动机',
    '车身_车身结构',
    '发动机_燃油标号'
]


def resolve_path(filename):
    path = Path(filename)
    if path.is_absolute():
        return path
    return BASE_DIR / path
