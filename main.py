import json
import csv
import os
import re
import time
import io
import requests
from fontTools.ttLib import TTFont
from PIL import Image, ImageDraw, ImageFont
import numpy as np

# ======================== 配置 ========================
API_URL = "https://www.dongchedi.com/motor/pc/sh/sh_sku_list"
PAGE_URL = "https://www.dongchedi.com/usedcar/x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x"
MAX_PAGES = 15  # 爬取页数
PAGE_SIZE = 40  # 每页数量

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_JSON = os.path.join(SCRIPT_DIR, "data.json")
DATA_JS = os.path.join(SCRIPT_DIR, "data.js")
DATA_CSV = os.path.join(SCRIPT_DIR, "data.csv")

API_ITEM_FIELDS = [
    "brand_name",
    "brand_source_city_name",
    "car_age",
    "car_mileage",
    "car_name",
    "car_source_city_name",
    "car_year",
    "transfer_cnt",
    "image",
]

DERIVED_FIELDS = [
    "decoded_price",
]

CSV_FIELDNAMES = [
    "brand_name",
    "brand_source_city_name",
    "car_age",
    "car_mileage",
    "car_name",
    "car_source_city_name",
    "car_year",
    "transfer_cnt",
    "decoded_price",
    "image",
]

CSV_CHINESE_HEADERS = {
    "brand_name": "品牌名称",
    "brand_source_city_name": "品牌来源城市",
    "car_age": "车龄",
    "car_mileage": "车辆里程",
    "car_name": "车型款名",
    "car_source_city_name": "车源城市",
    "car_year": "车辆年份",
    "transfer_cnt": "过户次数",
    "decoded_price": "解密售价",
    "image": "图片",
}

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0.0.0 Safari/537.36"
    ),
    "Content-Type": "application/x-www-form-urlencoded",
    "Referer": PAGE_URL,
    "Origin": "https://www.dongchedi.com",
}

SESSION = requests.Session()
SESSION.trust_env = False

# 常见中国城市列表（用于城市名解密）
COMMON_CITIES = [
    "北京", "上海", "广州", "深圳", "成都", "重庆", "杭州", "武汉", "西安",
    "苏州", "天津", "南京", "长沙", "郑州", "东莞", "青岛", "沈阳", "宁波",
    "昆明", "大连", "厦门", "合肥", "佛山", "福州", "哈尔滨", "济南", "温州",
    "长春", "石家庄", "常州", "泉州", "南宁", "贵阳", "南昌", "太原", "烟台",
    "兰州", "银川", "海口", "呼和浩特", "乌鲁木齐", "襄阳", "临沂", "保定",
    "湛江", "徐州", "赣州", "泸州", "宜昌", "桂林", "台州", "中山", "惠州",
    "威海", "潍坊", "绍兴", "金华", "珠海", "南通", "洛阳", "邯郸", "株洲",
]


# ======================== API请求 ========================
def fetch_page(page=1):
    """获取一页二手车数据"""
    data = f"&sh_city_name=全国&page={page}&limit={PAGE_SIZE}"
    resp = SESSION.post(API_URL, headers=HEADERS, data=data)

    result = resp.json()
    zhal = resp.headers.get("x-tt-zhal", "")
    font_url = _extract_font_url(zhal)

    return result, font_url


def _extract_font_url(zhal):
    """从响应头x-tt-zhal提取字体下载URL"""
    params = {}
    for p in zhal.split(";"):
        if "=" in p:
            k, v = p.split("=", 1)
            params[k.strip()] = v.strip()

    f_hash = params.get("f", "")
    domain = params.get("d1", "lf6-awef.bytetos.com")
    if f_hash:
        return f"https://{domain}/obj/awesome-font/c/{f_hash}.woff2"
    return ""


# ======================== 字体解密 ========================
def download_font(font_url):
    """下载字体文件"""
    if not font_url:
        return None
    try:
        resp = SESSION.get(font_url, timeout=10)
        if resp.status_code == 200 and len(resp.content) > 100:
            return io.BytesIO(resp.content)
    except Exception:
        pass
    return None


def build_digit_mapping(font_bytes):
    """
    通过渲染字体字符并使用结构特征识别，构建加密字符→数字的映射表
    策略：高分辨率渲染 + 二值化 + 与系统字体NCC对比 + 孔洞特征辅助
    """
    if font_bytes is None:
        return {}

    RENDER_SIZE = 160  # 高分辨率渲染
    STD_SIZE = (40, 50)  # 标准化尺寸

    try:
        custom_font = ImageFont.truetype(font_bytes, RENDER_SIZE)
    except Exception:
        return {}

    sys_font = _get_system_font(RENDER_SIZE)

    # 渲染参考数字(系统字体)并提取特征
    ref_data = {}
    for digit in "0123456789":
        img = _render_binary(digit, sys_font, RENDER_SIZE, STD_SIZE)
        if img is not None:
            ref_data[digit] = {
                "img": img,
                "holes": _count_holes(img),
                "width_ratio": _width_ratio(img),
            }

    if len(ref_data) < 10:
        return {}

    # 获取字体中所有加密字符
    font_bytes.seek(0)
    try:
        tt = TTFont(font_bytes)
    except Exception:
        return {}

    cmap = tt.getBestCmap()
    hmtx = tt["hmtx"] if "hmtx" in tt else None
    if not hmtx:
        tt.close()
        return {}

    # 找数字宽度范围（取最多的宽度值作为数字宽度）
    width_counts = {}
    for code, name in cmap.items():
        if code >= 0xE000:
            w = hmtx.metrics.get(name, (0, 0))[0]
            width_counts[w] = width_counts.get(w, 0) + 1

    # 数字宽度: 通常是出现频率最高的宽度之一，且在400-700范围内
    digit_width = None
    for w, cnt in sorted(width_counts.items(), key=lambda x: -x[1]):
        if 400 <= w <= 700:
            digit_width = w
            break

    if digit_width is None:
        tt.close()
        return {}

    # 渲染每个加密数字字符并识别
    candidates = []  # [(char, img, holes, width_ratio)]
    for code, name in cmap.items():
        if code < 0xE000:
            continue
        w = hmtx.metrics.get(name, (0, 0))[0]
        if w != digit_width:
            continue

        ch = chr(code)
        img = _render_binary(ch, custom_font, RENDER_SIZE, STD_SIZE)
        if img is None:
            continue

        holes = _count_holes(img)
        wr = _width_ratio(img)
        candidates.append((ch, img, holes, wr))

    tt.close()

    # 识别策略: 先用孔洞数分组，再用NCC精细匹配
    mapping = {}
    used_digits = set()

    # 第一轮: 用孔洞特征做确定性匹配
    for ch, img, holes, wr in candidates:
        # 1个孔洞: 0, 6, 9
        if holes == 1:
            # 0: 对称, 6: 重心偏下, 9: 重心偏上
            cy = _center_y(img)
            if cy < 0.42:
                digit = "9"
            elif cy > 0.58:
                digit = "6"
            else:
                digit = "0"
            if digit not in used_digits:
                mapping[ch] = digit
                used_digits.add(digit)
        # 2个孔洞: 8
        elif holes == 2:
            if "8" not in used_digits:
                mapping[ch] = "8"
                used_digits.add("8")

    # 第二轮: 对未识别的字符，用NCC与参考匹配
    for ch, img, holes, wr in candidates:
        if ch in mapping:
            continue

        best_digit = "?"
        best_ncc = -1
        for digit, rd in ref_data.items():
            if digit in used_digits:
                continue
            ncc = _ncc(img, rd["img"])
            if ncc > best_ncc:
                best_ncc = ncc
                best_digit = digit

        if best_digit != "?" and best_ncc > 0.3:
            mapping[ch] = best_digit
            used_digits.add(best_digit)

    # 第三轮: 如果还有未匹配的，放宽条件
    remaining = [(ch, img) for ch, img, _, _ in candidates if ch not in mapping]
    for ch, img in remaining:
        best_digit = "?"
        best_ncc = -1
        for digit, rd in ref_data.items():
            ncc = _ncc(img, rd["img"])
            if ncc > best_ncc:
                best_ncc = ncc
                best_digit = digit
        if best_digit != "?" and best_ncc > 0.2:
            mapping[ch] = best_digit

    return mapping


def _render_binary(char, font, render_size, std_size):
    """渲染字符为二值图像"""
    canvas_size = render_size + 40
    img = Image.new("L", (canvas_size, canvas_size), 255)
    draw = ImageDraw.Draw(img)
    draw.text((20, 10), char, fill=0, font=font)
    arr = np.array(img)

    rows = np.any(arr < 128, axis=1)
    cols = np.any(arr < 128, axis=0)
    if not rows.any() or not cols.any():
        return None

    rmin, rmax = np.where(rows)[0][[0, -1]]
    cmin, cmax = np.where(cols)[0][[0, -1]]
    cropped = arr[rmin : rmax + 1, cmin : cmax + 1]

    # 缩放到标准尺寸并二值化
    std = np.array(Image.fromarray(cropped).resize(std_size))
    binary = (std < 128).astype(np.float32)
    return binary


def _count_holes(binary_img):
    """计算二值图像中的孔洞数（使用flood fill）"""
    from PIL import Image as PILImage
    arr = (binary_img * 255).astype(np.uint8)
    img = PILImage.fromarray(arr)
    pixels = img.load()
    w, h = img.size

    # Flood fill from edges (mark all connected background as visited)
    visited = set()
    stack = []
    for x in range(w):
        if pixels[x, 0] > 128:
            stack.append((x, 0))
        if pixels[x, h - 1] > 128:
            stack.append((x, h - 1))
    for y in range(h):
        if pixels[0, y] > 128:
            stack.append((0, y))
        if pixels[w - 1, y] > 128:
            stack.append((w - 1, y))

    while stack:
        x, y = stack.pop()
        if (x, y) in visited or x < 0 or x >= w or y < 0 or y >= h:
            continue
        if pixels[x, y] <= 128:
            continue
        visited.add((x, y))
        stack.extend([(x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)])

    # 剩余的白色区域就是孔洞
    holes = 0
    for y in range(h):
        for x in range(w):
            if pixels[x, y] > 128 and (x, y) not in visited:
                # 新的孔洞
                holes += 1
                hstack = [(x, y)]
                while hstack:
                    hx, hy = hstack.pop()
                    if (hx, hy) in visited or hx < 0 or hx >= w or hy < 0 or hy >= h:
                        continue
                    if pixels[hx, hy] <= 128:
                        continue
                    visited.add((hx, hy))
                    hstack.extend([(hx - 1, hy), (hx + 1, hy), (hx, hy - 1), (hx, hy + 1)])

    return holes


def _center_y(binary_img):
    """计算二值图像的Y方向重心(归一化到0-1)"""
    h, w = binary_img.shape
    total = binary_img.sum()
    if total == 0:
        return 0.5
    y_sum = sum(y * binary_img[y, :].sum() for y in range(h))
    return y_sum / total / h


def _width_ratio(binary_img):
    """计算有效宽度占比"""
    h, w = binary_img.shape
    cols = binary_img.any(axis=0)
    if not cols.any():
        return 0
    left = np.where(cols)[0][0]
    right = np.where(cols)[0][-1]
    return (right - left + 1) / w


def _ncc(img1, img2):
    """计算归一化互相关系数"""
    a = img1.flatten().astype(np.float64)
    b = img2.flatten().astype(np.float64)
    a_mean = a.mean()
    b_mean = b.mean()
    a_centered = a - a_mean
    b_centered = b - b_mean
    denom = np.sqrt((a_centered ** 2).sum() * (b_centered ** 2).sum())
    if denom < 1e-10:
        return 0
    return float((a_centered * b_centered).sum() / denom)


def build_year_mapping(items):
    """
    通过car_year（未加密）和sub_title年份部分构建基础映射
    注意：car_year可能与sub_title年份不同（车型年 vs 上牌年）
    使用多数投票法提高准确性
    """
    char_digit_votes = {}  # {encrypted_char: {digit: count}}

    for item in items:
        year = item.get("car_year")
        sub = item.get("sub_title", "")
        if not year or not sub:
            continue

        parts = sub.split(" | ")
        if not parts:
            continue

        year_part = parts[0]
        year_str = str(year)

        if len(year_part) != len(year_str) + 1:
            continue

        for i, digit in enumerate(year_str):
            enc_char = year_part[i]
            if enc_char not in char_digit_votes:
                char_digit_votes[enc_char] = {}
            char_digit_votes[enc_char][digit] = char_digit_votes[enc_char].get(digit, 0) + 1

    # 每个加密字符取票数最多的数字
    mapping = {}
    for enc_char, votes in char_digit_votes.items():
        if votes:
            best_digit = max(votes, key=votes.get)
            # 只有超过半数一致才采纳
            total = sum(votes.values())
            if votes[best_digit] / total > 0.5:
                mapping[enc_char] = best_digit

    return mapping


def build_price_mapping(items, existing_mapping):
    """从价格字段末尾提取'万'的映射"""
    mapping = dict(existing_mapping)
    for item in items:
        for field in ["sh_price", "official_price"]:
            val = item.get(field, "")
            if val and len(val) > 0:
                last = val[-1]
                if last not in mapping and ord(last) > 0xE000:
                    mapping[last] = "万"
                    break
    return mapping


def build_mileage_mapping(items, existing_mapping):
    """从里程字段末尾提取'公里'的映射"""
    mapping = dict(existing_mapping)
    for item in items:
        sub = item.get("sub_title", "")
        parts = sub.split(" | ")
        if len(parts) >= 2:
            mp = parts[1]
            if len(mp) >= 2:
                mapping[mp[-1]] = "里"
                mapping[mp[-2]] = "公"
    return mapping


def build_city_mapping(items, existing_mapping):
    """利用brand_source_city_name（明文）与car_source_city_name（密文）构建城市名映射"""
    mapping = dict(existing_mapping)
    for item in items:
        plain_city = item.get("brand_source_city_name", "")
        enc_city = item.get("car_source_city_name", "")
        if not plain_city or not enc_city:
            continue
        if len(plain_city) != len(enc_city):
            continue
        for pc, ec in zip(plain_city, enc_city):
            if ord(ec) > 0xE000 and ec not in mapping:
                mapping[ec] = pc
    return mapping


def _get_system_font(size):
    """获取系统字体"""
    font_paths = [
        "C:/Windows/Fonts/simhei.ttf",
        "C:/Windows/Fonts/msyh.ttc",
        "C:/Windows/Fonts/simsun.ttc",
        "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
    ]
    for path in font_paths:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except Exception:
                continue
    return ImageFont.load_default()


def decrypt(text, mapping):
    """解密加密文本"""
    result = []
    for c in text:
        if c in mapping:
            result.append(mapping[c])
        else:
            result.append(c)
    return "".join(result)


# ======================== 主爬取逻辑 ========================
def process_page(items, font_url):
    """处理一页数据：构建映射、解密、格式化"""
    # 下载字体
    font_bytes = download_font(font_url)

    # 构建映射：多层策略
    # 1) 从car_year构建基础映射
    mapping = build_year_mapping(items)

    # 2) 从字体渲染补充缺失数字
    if font_bytes:
        font_mapping = build_digit_mapping(font_bytes)
        for ch, digit in font_mapping.items():
            if ch not in mapping:
                mapping[ch] = digit

    # 3) 从价格/里程/城市补充非数字映射
    mapping = build_price_mapping(items, mapping)
    mapping = build_mileage_mapping(items, mapping)
    mapping = build_city_mapping(items, mapping)

    known_digits = set(v for v in mapping.values() if v.isdigit())
    print(f"  已映射数字: {sorted(known_digits)} ({len(known_digits)}/10)")

    # 解密并格式化每辆车
    cars = []
    for item in items:
        price_str = decrypt(item.get("sh_price", ""), mapping)
        guide_str = decrypt(item.get("official_price", ""), mapping)
        sub_decoded = decrypt(item.get("sub_title", ""), mapping)
        city = decrypt(item.get("car_source_city_name", ""), mapping)

        # 如果城市名仍有未解密字符，使用brand_source_city_name
        plain_city = item.get("brand_source_city_name", "")
        if any(ord(c) > 0xE000 for c in city):
            city = plain_city if plain_city else city

        # 解析里程
        mileage = ""
        if " | " in sub_decoded:
            mileage = sub_decoded.split(" | ")[1]

        # 解析价格数值
        try:
            price_val = float(re.search(r"([\d.]+)", price_str).group(1))
        except (AttributeError, ValueError):
            price_val = 0

        try:
            guide_val = float(re.search(r"([\d.]+)", guide_str).group(1))
        except (AttributeError, ValueError):
            guide_val = 0

        car = {field: item.get(field, "") for field in API_ITEM_FIELDS}
        car.update({
            "decoded_price": price_str,
        })
        cars.append(car)

    return cars


def scrape(max_pages=MAX_PAGES):
    """主爬取函数"""
    all_cars = []
    seen_ids = set()

    for page in range(1, max_pages + 1):
        print(f"正在爬取第 {page} 页...")
        try:
            result, font_url = fetch_page(page)
        except Exception as e:
            print(f"  请求失败: {e}")
            continue

        if result.get("status") != 0:
            print(f"  API错误: {result.get('message')}")
            break

        data = result.get("data", {})
        items = data.get("search_sh_sku_info_list", [])
        if not items:
            print("  无数据")
            break

        print(f"  获取 {len(items)} 条数据")

        cars = process_page(items, font_url)
        # 去重
        new_count = 0
        for item, car in zip(items, cars):
            sku_id = item.get("sku_id")
            dedupe_key = sku_id if sku_id not in ("", None) else repr(car)
            if dedupe_key not in seen_ids:
                seen_ids.add(dedupe_key)
                all_cars.append(car)
                new_count += 1

        print(f"  新增 {new_count} 辆（去重后），总计 {len(all_cars)} 辆")

        if not data.get("has_more"):
            print("  已到最后一页")
            break

        time.sleep(1)

    return all_cars


def _csv_value(value):
    if isinstance(value, (dict, list)):
        return json.dumps(value, ensure_ascii=False)
    if value is None:
        return ""
    return value


def save_data(cars):
    """保存数据到JSON和JS文件"""
    with open(DATA_JSON, "w", encoding="utf-8") as f:
        json.dump(cars, f, ensure_ascii=False, indent=2)

    js_content = f"const CAR_DATA = {json.dumps(cars, ensure_ascii=False)};"
    with open(DATA_JS, "w", encoding="utf-8") as f:
        f.write(js_content)

    with open(DATA_CSV, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.writer(f)
        writer.writerow([CSV_CHINESE_HEADERS.get(field, field) for field in CSV_FIELDNAMES])
        writer.writerow(CSV_FIELDNAMES)
        for car in cars:
            writer.writerow([_csv_value(car.get(field, "")) for field in CSV_FIELDNAMES])

    print(f"\n数据已保存:")
    print(f"  JSON: {DATA_JSON}")
    print(f"  JS:   {DATA_JS}")
    print(f"  CSV:  {DATA_CSV}")
    print(f"  共 {len(cars)} 辆车")


# ======================== 入口 ========================
if __name__ == "__main__":
    print("=" * 50)
    print("  懂车帝二手车爬虫")
    print("=" * 50)
    print()
    cars = scrape()
    save_data(cars)
