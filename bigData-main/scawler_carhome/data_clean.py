"""
汽车参数数据清洗脚本

原始数据: data.csv (25k行×11列, 汽车之家新车参数)
清洗输出: output/car_clean.csv (17列结构化数据)

清洗动作:
  1. 价格标准化     12.28万 → 12.28 (float)
  2. 上市时间解析    2018-06-14 → year/month/season
  3. 质保拆分        三年或10万公里 → years=3, km=10
  4. 发动机拆分      1.5L 115马力 L4 → 排量/马力/类型
  5. 变速箱编码      6挡手自一体 → gear_count=6, type=AT
  6. 车身拆分        5门5座SUV → doors/seats/body_type
  7. 能源/燃油编码    汽油→1, 纯电动→0, 92号→92

用法: python data_clean.py
"""
import sys
import re
import logging
from pathlib import Path

_BASE_DIR = Path(__file__).resolve().parent
if str(_BASE_DIR) not in sys.path:
    sys.path.insert(0, str(_BASE_DIR))

import pandas as pd
import numpy as np

logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s] %(message)s')
logger = logging.getLogger(__name__)

# 中英文数字映射
CN_NUM = {'一': 1, '二': 2, '两': 2, '三': 3, '四': 4, '五': 5,
          '六': 6, '七': 7, '八': 8, '九': 9, '十': 10}

ENERGY_MAP = {
    '纯电动': 0, '汽油': 1, '柴油': 1, '插电式混合动力': 2,
    '增程式': 3, '油电混合': 4, '汽油+48V轻混系统': 5,
    '汽油+24V轻混系统': 6, '汽油电驱': 7, '氢燃料电池': 8,
    '甲醇重整': 9, 'CNG': 10, '插电式混合动力(非运营)': 2,
    '汽油+CNG': 11, '纯电动(非运营)': 0,
}

OUTPUT_DIR = _BASE_DIR / 'output'
OUTPUT_CSV = OUTPUT_DIR / 'car_clean.csv'


def load_raw(path=None):
    """加载原始CSV"""
    path = path or (_BASE_DIR / 'data.csv')
    df = pd.read_csv(path, encoding='utf-8-sig')
    logger.info("加载原始数据: %d行 × %d列", len(df), len(df.columns))
    return df


# ============================================================
#  清洗函数
# ============================================================

def clean_price(df):
    """价格: "12.28万" → float"""
    raw = df['基本参数_厂商指导价'].astype(str).str.replace('万', '', regex=False)
    return pd.to_numeric(raw, errors='coerce')


def clean_launch_date(df):
    """上市时间: "2018-06-14" → year, month, season"""
    dates = pd.to_datetime(df['基本参数_上市时间'], errors='coerce')
    year = dates.dt.year
    month = dates.dt.month
    season = dates.dt.quarter.apply(lambda q: f"Q{int(q)}" if pd.notna(q) else None)
    return dates, year.astype('Int64'), month.astype('Int64'), season


def clean_warranty(df):
    """质保: "三年或10万公里" → (years, km)"""
    raw = df['基本参数_整车质保'].fillna('').astype(str)

    def parse_one(s):
        if not s or s in ('-', '暂无', '不详'):
            return (None, None)
        # 跳过说明性文字
        if '参考' in s or '为准' in s or len(s) > 20:
            return (None, None)
        # 提取年数: 支持"三年"或"3年"
        years = None
        cn_match = re.search(r'([一二两三四五六七八九十]+)年', s)
        if cn_match:
            years = CN_NUM.get(cn_match.group(1))
        else:
            digit_match = re.search(r'(\d+)\s*年', s)
            if digit_match:
                years = int(digit_match.group(1))
        # 不限公里
        if '不限' in s:
            return (years, -1)
        # 提取公里数（万公里）
        km_match = re.search(r'(\d+)\s*万公里', s)
        km = float(km_match.group(1)) if km_match else None
        return (years, km)

    parsed = raw.apply(parse_one)
    years = parsed.apply(lambda x: x[0]).astype('Int64')
    km = parsed.apply(lambda x: x[1])
    return years, km


def clean_engine(df):
    """发动机: "1.5L 115马力 L4" → (displacement, horsepower, engine_type, is_electric)"""
    raw = df['基本参数_发动机'].fillna('').astype(str)

    displacements = []
    horsepowers = []
    engine_types = []
    is_electrics = []

    for s in raw:
        if not s or s == '-' or s == '纯电动':
            displacements.append(None)
            horsepowers.append(None)
            engine_types.append('EV' if s == '纯电动' else None)
            is_electrics.append(1 if s == '纯电动' else 0)
            continue

        # 增程器
        if '增程器' in s:
            hp = re.search(r'(\d+)\s*马力', s)
            displacements.append(None)
            horsepowers.append(int(hp.group(1)) if hp else None)
            engine_types.append('RANGE_EXT')
            is_electrics.append(1)
            continue

        # 常规发动机: 1.5L 115马力 L4 或 1.4T 140马力 L4
        disp = re.search(r'([\d.]+)\s*[LT]', s)
        hp = re.search(r'马力\s*(\S+)', s)
        hp_num = re.search(r'(\d+)\s*马力', s)

        displacements.append(float(disp.group(1)) if disp else None)
        engine_types.append(hp.group(1) if hp else None)
        horsepowers.append(int(hp_num.group(1)) if hp_num else None)
        is_electrics.append(0)

    return (displacements, horsepowers, engine_types, is_electrics)


def clean_transmission(df):
    """变速箱: "6挡手自一体" → (gear_count, trans_type)"""
    raw = df['基本参数_变速箱'].fillna('').astype(str)

    gear_counts = []
    trans_types = []

    for s in raw:
        if not s or s == '-':
            gear_counts.append(None)
            trans_types.append(None)
            continue

        # 电动车单速
        if '电动' in s:
            gear_counts.append(1)
            trans_types.append('EV')
            continue

        # CVT
        if 'CVT' in s.upper():
            gear_counts.append(0)
            trans_types.append('CVT')
            continue

        # 提取档位数
        gear_match = re.search(r'(\d+)挡', s)
        gc = int(gear_match.group(1)) if gear_match else None
        gear_counts.append(gc)

        # 变速箱类型
        if '双离合' in s:
            trans_types.append('DCT')
        elif 'DHT' in s.upper():
            trans_types.append('DHT')
        elif '手自一体' in s:
            trans_types.append('AT')
        elif '自动' in s:
            trans_types.append('AT')
        elif '手动' in s:
            trans_types.append('MT')
        elif '无级' in s:
            trans_types.append('CVT')
        elif '固定齿比' in s:
            trans_types.append('FIXED')
        else:
            trans_types.append('OTHER')

    return (gear_counts, trans_types)


def clean_body(df):
    """车身: "5门5座SUV" → (doors, seats, body_type)"""
    raw = df['基本参数_车身结构'].fillna('').astype(str)

    doors_list = []
    seats_list = []
    body_types = []

    body_type_map = {
        'SUV': 'SUV', '三厢车': 'SEDAN', '两厢车': 'HATCHBACK',
        'MPV': 'MPV', '掀背车': 'HATCHBACK', '旅行车': 'WAGON',
        '跑车': 'COUPE', '皮卡': 'PICKUP', '微面': 'MINIVAN',
        '轻客': 'VAN', '客车': 'BUS', '货车': 'TRUCK', '卡车': 'TRUCK',
        '硬顶敞篷车': 'CONVERTIBLE', '硬顶跑车': 'COUPE',
        '软顶敞篷车': 'CONVERTIBLE', '轿车': 'SEDAN', '面包车': 'MINIVAN',
    }

    for s in raw:
        if not s or s == '-':
            doors_list.append(None)
            seats_list.append(None)
            body_types.append(None)
            continue

        # 提取门/座数（各自独立，避免一个缺失导致另一个也丢弃）
        door_match = re.search(r'(\d+)门', s)
        seat_match = re.search(r'(\d+)座', s)
        doors_list.append(int(door_match.group(1)) if door_match else None)
        seats_list.append(int(seat_match.group(1)) if seat_match else None)

        # 提取车型
        bt = None
        for cn, en in body_type_map.items():
            if cn in s:
                bt = en
                break
        body_types.append(bt)

    return (doors_list, seats_list, body_types)


def clean_energy_fuel(df):
    """能源类型 + 燃油标号编码"""
    energy = df['基本参数_能源类型'].fillna('').astype(str)
    energy_codes = energy.map(ENERGY_MAP).fillna(-1).astype(int)

    fuel = df['发动机_燃油标号'].fillna('').astype(str)
    fuel_nums = fuel.str.extract(r'(\d+)', expand=False)
    fuel_codes = pd.to_numeric(fuel_nums, errors='coerce').fillna(0).astype(int)

    return energy_codes, fuel_codes


# ============================================================
#  主流程
# ============================================================

def run():
    df = load_raw()

    logger.info("=" * 50)
    logger.info("开始数据清洗...")

    # 1. 品牌
    brand = df['品牌'].fillna('未知')

    # 2. 级别
    level = df['基本参数_级别'].fillna('未知')

    # 3-4. 能源 + 燃油
    energy_code, fuel_code = clean_energy_fuel(df)

    # 5-8. 上市时间
    launch_date, launch_year, launch_month, launch_season = clean_launch_date(df)

    # 9. 价格
    price = clean_price(df)

    # 10-11. 质保
    warranty_years, warranty_km = clean_warranty(df)

    # 12-14. 发动机
    disp, hp, eng_type, is_ev = clean_engine(df)

    # 15-16. 变速箱
    gear_cnt, trans_t = clean_transmission(df)

    # 17-19. 车身
    doors, seats, body_t = clean_body(df)

    # 车身默认值填充（轿车→4门5座, 皮卡→4门5座）
    doors = pd.Series(doors).fillna(
        pd.Series(body_t).map({'SEDAN': 4, 'PICKUP': 4})
    ).astype('Int64')
    seats = pd.Series(seats).fillna(
        pd.Series(body_t).map({'SEDAN': 5, 'PICKUP': 5, 'BUS': None})
    ).astype('Int64')

    # 组装输出
    out = pd.DataFrame({
        'brand': brand,
        'level': level,
        'energy_code': energy_code,
        'energy_name': df['基本参数_能源类型'].fillna('未知'),
        'fuel_grade': fuel_code,
        'launch_date': launch_date,
        'launch_year': launch_year,
        'launch_month': launch_month,
        'launch_season': launch_season,
        'price_wan': price,
        'warranty_years': warranty_years,
        'warranty_km_wan': warranty_km,
        'displacement_L': disp,
        'horsepower': hp,
        'engine_type': eng_type,
        'is_electric': is_ev,
        'gear_count': gear_cnt,
        'trans_type': trans_t,
        'doors': doors,
        'seats': seats,
        'body_type': body_t,
        'body_structure': df['基本参数_车身结构'].fillna('未知'),
    })

    # 保存
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out.to_csv(OUTPUT_CSV, index=False, encoding='utf-8-sig')
    logger.info("清洗完成: %d行 × %d列", len(out), len(out.columns))
    logger.info("输出: %s", OUTPUT_CSV)

    # 统计
    logger.info("=" * 50)
    logger.info("数据质量报告:")
    logger.info("  总行数: %d", len(out))
    logger.info("  品牌数: %d", out['brand'].nunique())
    logger.info("  价格范围: %.1f ~ %.1f 万", out['price_wan'].min(), out['price_wan'].max())
    logger.info("  年份范围: %d ~ %d", out['launch_year'].min(), out['launch_year'].max())

    for col in out.columns:
        null_cnt = out[col].isna().sum()
        if null_cnt > 0:
            logger.info("  %s: %d空 (%.1f%%)", col, null_cnt, null_cnt/len(out)*100)

    return out


if __name__ == '__main__':
    run()
