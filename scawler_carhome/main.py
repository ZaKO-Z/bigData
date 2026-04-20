import requests
import json
import openpyxl
import re
import time
import csv
from pathlib import Path
from openpyxl.styles import Font, Alignment


BASE_DIR = Path(__file__).resolve().parent


def resolve_data_path(filename):
    path = Path(filename)
    if path.is_absolute():
        return path
    return BASE_DIR / path

# 全局变量存储爬取的汽车数据
car_brand = []  # 品牌
car_series = []  # 车系
car_version = []  # 版本名称
car_price = []  # 厂商指导价
car_seriesid = []  # seriesid
car_year = []  # 年份款
car_params = {}  # 存储所有参数（键：参数名，值：参数值列表）

csv_fields = [
    '品牌',
    '基本参数_级别',
    '基本参数_能源类型',
    '基本参数_上市时间',
    '基本参数_车身结构',
    '基本参数_厂商指导价(元)',
    '基本参数_整车质保',
    '基本参数_变速箱',
    '基本参数_发动机',
    '车身_车身结构',
    '发动机_燃油标号'
]


def extract_series_id(url):
    """从URL中提取seriesid"""
    pattern = r'series/(\d+)\.html'
    match = re.search(pattern, url)
    if match:
        return match.group(1)
    return None


def fetch_car_data(seriesid, headers):
    """获取汽车数据"""
    url = f"https://www.autohome.com.cn/web-main/car/param/getParamConf?mode=1&site=1&seriesid={seriesid}"
    try:
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"获取数据失败 (seriesid: {seriesid}): {e}")
        return None
    except json.JSONDecodeError:
        print(f"JSON解析失败 (seriesid: {seriesid})")
        return None


def get_car_info(url, headers):
    """
    解析单个URL的汽车参数并存储到全局变量
    :param url: 汽车详情页URL
    :param headers: 请求头
    :return: None
    """
    # 提取seriesid
    seriesid = extract_series_id(url)
    if not seriesid:
        print(f"无法从URL提取seriesid: {url}")
        return

    print(f"提取到seriesid: {seriesid}")

    # 获取JSON数据
    json_data = fetch_car_data(seriesid, headers)
    if not json_data or 'result' not in json_data:
        print(f"数据格式错误 (seriesid: {seriesid})")
        return

    try:
        # 获取车型基本信息
        brand_info = json_data['result']['bread']
        brand_name = brand_info['brandname']
        series_name = brand_info['seriesname']

        # 定义需要提取的类别
        target_categories = [
            '基本参数', '车身', '发动机', '电动机', '电池/充电',
            '变速箱', '底盘转向', '车轮制动'
        ]

        # 获取所有需要提取的参数标题映射
        params_mapping = {}
        category_mapping = {}  # 记录每个参数属于哪个类别
        for title_group in json_data['result']['titlelist']:
            category = title_group['itemtype']
            if category in target_categories:
                for item in title_group['items']:
                    titleid = item['titleid']
                    params_mapping[titleid] = item['itemname']
                    category_mapping[titleid] = category

        # 提取各版本数据并存储到全局变量
        for version in json_data['result']['datalist']:
            # 基础信息存储
            car_brand.append(brand_name)
            car_series.append(series_name)
            car_version.append(version['specname'])
            car_price.append(version['minprice'])
            car_seriesid.append(seriesid)
            car_year.append(version['condition'][0] if version['condition'] else '')

            # 提取所有参数
            for param in version['paramconflist']:
                titleid = param['titleid']
                if titleid in params_mapping:
                    param_name = params_mapping[titleid]
                    category = category_mapping[titleid]
                    full_param_name = f"{category}_{param_name}"

                    # 处理参数值
                    if param['sublist']:
                        sub_values = []
                        for sub_item in param['sublist']:
                            if sub_item['value'] == '●':
                                sub_values.append(sub_item['name'])
                            elif sub_item['value'] == '○':
                                sub_values.append(f"{sub_item['name']}(选配)")
                            else:
                                sub_values.append(f"{sub_item['name']}: {sub_item['value']}")
                        param_value = '; '.join(sub_values)
                    else:
                        param_value = param['itemname']

                    # 初始化参数列表（首次出现时）
                    if full_param_name not in car_params:
                        car_params[full_param_name] = []

                    # 填充参数值（确保列表长度匹配）
                    current_len = len(car_params[full_param_name])
                    target_len = len(car_brand)
                    # 补全之前的空值（如果有版本缺失该参数）
                    while current_len < target_len - 1:
                        car_params[full_param_name].append('')
                        current_len += 1
                    # 添加当前参数值
                    car_params[full_param_name].append(param_value if param_value else '')

            # 补全所有参数列表的长度（确保每个版本的参数数量一致）
            for param_name in car_params:
                if len(car_params[param_name]) < len(car_brand):
                    car_params[param_name].append('')

        print(f"成功提取 {len(json_data['result']['datalist'])} 个版本的数据")
        if json_data['result']['datalist']:
            params_count = len(params_mapping)
            print(f"每个版本包含 {params_count} 个参数")

    except KeyError as e:
        print(f"数据字段缺失 (seriesid: {seriesid}): {e}")


def read_urls_from_excel(filename='series_urls.xlsx'):
    """从Excel文件中读取URL列表"""
    file_path = resolve_data_path(filename)
    try:
        wb = openpyxl.load_workbook(file_path)
        ws = wb.active
        urls = []
        # 遍历A列，跳过第一行（标题行）
        for row in range(2, ws.max_row + 1):
            cell_value = ws.cell(row=row, column=1).value
            if cell_value and isinstance(cell_value, str) and cell_value.startswith('http'):
                urls.append(cell_value)
        return urls
    except Exception as e:
        print(f"读取Excel文件失败 ({file_path}): {e}")
        return []


def save_to_excel(excel_name='汽车详细参数1.xlsx'):
    """
    将全局变量中的数据保存到Excel
    :param excel_name: 保存的文件名
    :return: None
    """
    if not car_brand:
        print("没有数据可保存")
        return

    # 创建Excel工作簿
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "汽车详细参数"

    # 构建表头和数据列表
    basic_fields = ['品牌', '车系', '版本名称', '厂商指导价', 'seriesid', '年份款']
    param_fields = list(car_params.keys())
    all_fields = basic_fields + param_fields

    # 写入表头
    for col, field in enumerate(all_fields, 1):
        cell = ws.cell(row=1, column=col, value=field)
        cell.font = Font(bold=True)
        cell.alignment = Alignment(horizontal='center')

    # 写入数据
    data_rows = len(car_brand)
    for row in range(2, data_rows + 2):
        idx = row - 2  # 数据索引
        # 写入基础信息
        ws.cell(row=row, column=1, value=car_brand[idx])
        ws.cell(row=row, column=2, value=car_series[idx])
        ws.cell(row=row, column=3, value=car_version[idx])
        ws.cell(row=row, column=4, value=car_price[idx])
        ws.cell(row=row, column=5, value=car_seriesid[idx])
        ws.cell(row=row, column=6, value=car_year[idx])

        # 写入参数信息
        for col, param_name in enumerate(param_fields, 7):
            # 确保索引不越界
            value = car_params[param_name][idx] if idx < len(car_params[param_name]) else ''
            ws.cell(row=row, column=col, value=value)

    # 调整列宽
    for column in ws.columns:
        max_length = 0
        column_letter = column[0].column_letter
        for cell in column:
            try:
                if len(str(cell.value)) > max_length:
                    max_length = len(str(cell.value))
            except:
                pass
        adjusted_width = min(max_length + 2, 50)
        ws.column_dimensions[column_letter].width = adjusted_width

    # 保存文件
    file_path = resolve_data_path(excel_name)
    file_path.parent.mkdir(parents=True, exist_ok=True)
    wb.save(file_path)
    print(f"数据已成功保存到 '{file_path}'")


def get_csv_field_value(field_name, idx):
    """根据CSV字段名获取对应行的数据"""
    if field_name == '品牌':
        return car_brand[idx]

    if field_name == '基本参数_厂商指导价(元)' and field_name not in car_params:
        return car_price[idx]

    values = car_params.get(field_name, [])
    return values[idx] if idx < len(values) else ''


def save_to_csv(csv_name='data.csv'):
    """
    将指定字段保存到CSV文件
    :param csv_name: 保存的CSV文件名
    :return: None
    """
    if not car_brand:
        print("没有数据可保存")
        return

    file_path = resolve_data_path(csv_name)
    file_path.parent.mkdir(parents=True, exist_ok=True)

    with open(file_path, 'w', newline='', encoding='utf-8-sig') as f:
        writer = csv.writer(f)
        writer.writerow(csv_fields)

        for idx in range(len(car_brand)):
            row = [get_csv_field_value(field, idx) for field in csv_fields]
            writer.writerow(row)

    print(f"指定字段已成功保存到 '{file_path}'")


if __name__ == "__main__":
    # 定义请求头
    headers = {
        'priority': 'u=0, i',
        'sec-fetch-user': '?1',
        'upgrade-insecure-requests': '1',
        'Cookie': 'pcpopclub=774c510a390740e7b63abd1a3290a773064f3fff; clubUserShow=105857023|0|25|adt34sjwk|0|0|0||2025-10-22 12:42:53|0; sessionlogin=774c510a390740e7b63abd1a3290a773064f3fff; autouserid=105857023; cookieCityId=110100; __ah_uuid_ng=u_105857023; fvlid=17619793108077PSsnu1YjJ; area=620105; sessionid=8BB1AFEA-BE81-4842-B3F9-94C14624522B%7C%7C2025-11-01+14%3A41%3A47.284%7C%7Ccn.bing.com; autoid=9aa7bf791fdd61c294eb4781a5de4205; _hjSessionUser_6545983=eyJpZCI6IjhkODE5M2MzLWUxMDYtNWU4My05MTllLWQwYzU4NGU3NGI5YiIsImNyZWF0ZWQiOjE3NjE5ODA0NDgyMjAsImV4aXN0aW5nIjp0cnVlfQ==; _ac=OAu4zw2Yq4B9yHoKV7VDvn9FGg4TfK5Xq7pdGZ9FyMw9iWKXQPIj; sessionip=42.91.0.150; sessionvid=CCC23EC2-2828-40A4-B569-77B01C4E5C67; pvidlist="781d43c1-466d-49b5-853f-1c8f9a3bb3250:757985:1134380:0:1:5662904:5481:0,d0686318-5e23-4fd8-86e7-da07bc299bc40:750828:1125867:0:1:5659306:5481:0"; ahpvno=7; pvidchain=6863887,6863887,3311277; v_no=7; visit_info_ad=8BB1AFEA-BE81-4842-B3F9-94C14624522B||CCC23EC2-2828-40A4-B569-77B01C4E5C67||-1||-1||7; historyseries=5769; ref=cn.bing.com%7C0%7C0%7C0%7C2025-12-12+20%3A19%3A24.252%7C2025-11-01+14%3A41%3A47.284; _as=fffmCLuHpuuTrfBnI8259qSAsGoHNYu_l90hxKKfjzE198qjybU; ahrlid=1765541965996ZhUrVzpulP-1765541980091'
    }

    # 读取URL列表
    print("正在读取Excel文件...")
    urls = read_urls_from_excel('series_urls.xlsx')

    if not urls:
        print("未找到有效的URL")
    else:
        print(f"找到 {len(urls)} 个URL")

        # 遍历URL爬取数据
        for i, url in enumerate(urls, 1):
            print(f"正在处理第 {i}/{len(urls)} 个URL: {url}")
            get_car_info(url, headers)
            time.sleep(1)  # 延时避免请求过快

        # 显示统计信息
        if car_brand:
            print(f"\n总共成功提取 {len(car_brand)} 个版本的数据")
            brands = set(car_brand)
            series = set(car_series)
            print(f"涉及品牌: {', '.join(brands)}")
            print(f"涉及车系: {', '.join(series)}")

        # 保存数据到Excel
        save_to_excel(excel_name='汽车详细参数1.xlsx')
        save_to_csv(csv_name='data.csv')
