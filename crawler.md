# bigData-main 爬虫架构与数据采集清洗文档

> 本文档详细记录了 `bigData-main` 项目的爬虫架构、数据采集流程、数据清洗逻辑及产出数据结构，便于后续调用与维护。

---

## 一、项目概述

`bigData-main` 是一个针对**汽车之家**（autohome.com.cn）的汽车参数数据爬虫项目，采用 Python 异步并发架构，支持断点续爬、Cookie 轮换、失败重试等特性，并对爬取的原始数据进行结构化清洗，输出标准化的 CSV 数据供下游分析使用。

- **数据源**：汽车之家车型参数 API（`https://www.autohome.com.cn/web-main/car/param/getParamConf`）
- **爬取范围**：A-Z 全字母页面的所有车系（约 6000+ 车系，预估 5万+ 车型版本）
- **技术栈**：Python 3.10+ / aiohttp（异步）/ requests（同步）/ openpyxl / pandas

---

## 二、目录结构

```
bigData-main/
├── .gitignore                          # Git 忽略配置
├── package-lock.json
└── scawler_carhome/                    # 爬虫主目录
    ├── config.py                       # 全局配置（API、Cookie、并发、字段）
    ├── collect_urls.py                 # 车系URL采集器（A-Z遍历）
    ├── crawler.py                      # 爬取引擎（异步+同步双模式）
    ├── export.py                       # 数据导出（CSV增量/全量、Excel）
    ├── main.py                         # 主入口（爬取流程编排）
    ├── data_clean.py                   # 数据清洗（原始→结构化）
    ├── bench_concurrency.py            # 并发基准测试
    ├── series_urls.xlsx                # 车系URL列表（输入）
    ├── data.csv                        # 原始爬取数据（11列）
    ├── 汽车详细参数1.xlsx              # Excel 格式原始数据
    └── output/
        └── car_clean.csv               # 清洗后结构化数据（22列）
```

---

## 三、核心配置（config.py）

### 3.1 API 配置

```python
API_URL_TEMPLATE = "https://www.autohome.com.cn/web-main/car/param/getParamConf?mode=1&site=1&seriesid={seriesid}"
```

通过车系 ID（seriesid）请求该 API，返回该车系所有版本的详细参数。

### 3.2 多 Cookie 轮换机制

项目支持**最多 20 个 Cookie 轮换**，通过 `.env` 文件配置：

```bash
AUTOHOME_COOKIE_1=xxx
AUTOHOME_COOKIE_2=xxx
...
AUTOHOME_COOKIE_20=xxx
```

`CookieRotator` 类实现线程安全的轮换，每个请求自动取不同 Cookie，降低单 Cookie 被封风险。

### 3.3 爬取参数分类

```python
TARGET_CATEGORIES = [
    '基本参数', '车身', '发动机', '电动机', '电池/充电',
    '变速箱', '底盘转向', '车轮制动'
]
```

只提取这 8 个类别的参数，过滤无关数据。

### 3.4 并发与重试控制

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `MAX_CONCURRENCY` | 1 | 最大并发数（可通过环境变量调整） |
| `REQUEST_TIMEOUT` | 15 秒 | 单次请求超时 |
| `MIN_DELAY` / `MAX_DELAY` | 0.3 / 0.8 秒 | 同步模式请求间隔 |
| `MAX_RETRIES` | 3 | 最大重试次数 |
| `RETRY_BACKOFF_BASE` | 5 | 退避基数（429 状态码） |
| `RETRY_BACKOFF_MAX` | 120 | 最大退避时间 |

### 3.5 文件路径

| 文件 | 用途 |
|------|------|
| `series_urls.xlsx` | 输入：车系 URL 列表 |
| `data.csv` | 输出：原始爬取数据 |
| `汽车详细参数1.xlsx` | 输出：Excel 格式数据 |
| `checkpoint.json` | 断点续爬记录 |
| `failed.json` | 失败队列记录 |
| `crawler.log` | 运行日志 |

---

## 四、数据采集流程

### 4.1 第一步：URL 采集（collect_urls.py）

**功能**：从汽车之家 A-Z 字母页面采集所有车系 URL。

**流程**：
1. 遍历 `https://www.autohome.com.cn/grade/carhtml/{A~Z}.html` 共 26 个页面
2. 使用正则 `<li[^>]*\sid="s(\d+)"` 提取车系 ID
3. 去重后生成车系 URL：`https://www.autohome.com.cn/config/series/{id}.html`
4. 写入 `series_urls.xlsx`

**预估产出**：约 6000+ 个唯一车系，按每车系 8.5 个版本计算，预估 5万+ 条记录。

**运行**：
```bash
python collect_urls.py
```

### 4.2 第二步：数据爬取（main.py + crawler.py）

**功能**：根据 URL 列表，调用汽车之家 API 爬取每个车系的详细参数。

#### 4.2.1 异步爬取引擎（推荐）

```
main.py → crawl_urls_async() → _crawl_one_async() → _fetch_json_async()
```

**核心特性**：
- **异步并发**：使用 `aiohttp` + `asyncio.Semaphore` 控制并发
- **Cookie 轮换**：每次请求自动切换 Cookie
- **断点续爬**：`CheckpointManager` 记录已完成的 series_id，重启后自动跳过
- **失败队列**：`FailedQueue` 记录失败 URL，支持单独重试
- **增量写入**：每爬完一个车系立即追加写入 CSV，避免内存堆积
- **智能重试**：
  - 403 Forbidden → 终止并提示（IP 封禁或 Cookie 过期）
  - 429 Too Many Requests → 指数退避重试
  - 网络错误 → 随机延时重试
  - 登录重定向 → 检测 Cookie 过期

#### 4.2.2 同步爬取（fallback）

使用 `requests.Session` + `urllib3.Retry`，适合小规模调试。

#### 4.2.3 运行方式

```bash
# 异步模式（默认）
python main.py

# 同步模式（调试用）
python main.py --sync

# 重试失败队列
python main.py --retry-failed
```

### 4.3 第三步：数据导出（export.py）

| 方法 | 说明 |
|------|------|
| `CsvAppender` | 增量追加写入 CSV（异步模式使用） |
| `to_csv()` | 全量导出 CSV（同步模式使用） |
| `to_excel()` | 全量导出 Excel（带格式化） |

---

## 五、数据清洗流程（data_clean.py）

### 5.1 清洗概览

| 输入 | 输出 |
|------|------|
| `data.csv`（11 列原始数据） | `output/car_clean.csv`（22 列结构化数据） |

### 5.2 清洗动作详解

| # | 清洗项 | 原始示例 | 清洗后字段 | 清洗后示例 |
|---|--------|---------|-----------|-----------|
| 1 | 价格标准化 | `12.28万` | `price_wan` | `12.28` (float) |
| 2 | 上市时间解析 | `2018-06-14` | `launch_date` / `launch_year` / `launch_month` / `launch_season` | `2018-06-14` / `2018` / `6` / `Q2` |
| 3 | 质保拆分 | `三年或10万公里` | `warranty_years` / `warranty_km_wan` | `3` / `10`（不限公里为 -1） |
| 4 | 发动机拆分 | `1.5L 115马力 L4` | `displacement_L` / `horsepower` / `engine_type` / `is_electric` | `1.0` / `71` / `L3` / `0` |
| 5 | 变速箱编码 | `6挡手自一体` | `gear_count` / `trans_type` | `6` / `AT` |
| 6 | 车身拆分 | `5门4座两厢车` | `doors` / `seats` / `body_type` | `5` / `4` / `HATCHBACK` |
| 7 | 能源编码 | `汽油` | `energy_code` / `energy_name` | `1` / `汽油` |
| 8 | 燃油标号编码 | `95号` | `fuel_grade` | `95` |

### 5.3 编码映射表

#### 能源类型编码（ENERGY_MAP）

| 编码 | 能源类型 |
|------|---------|
| 0 | 纯电动 |
| 1 | 汽油 / 柴油 |
| 2 | 插电式混合动力 |
| 3 | 增程式 |
| 4 | 油电混合 |
| 5 | 汽油+48V轻混系统 |
| 6 | 汽油+24V轻混系统 |
| 7 | 汽油电驱 |
| 8 | 氢燃料电池 |
| 9 | 甲醇重整 |
| 10 | CNG |
| 11 | 汽油+CNG |

#### 变速箱类型编码

| 编码 | 变速箱类型 | 识别关键词 |
|------|----------|-----------|
| `AT` | 手自一体 / 自动 | 手自一体、自动 |
| `MT` | 手动 | 手动 |
| `CVT` | 无级变速 | CVT、无级 |
| `DCT` | 双离合 | 双离合 |
| `DHT` | 混合动力专用 | DHT |
| `EV` | 电动车单速 | 电动 |
| `FIXED` | 固定齿比 | 固定齿比 |
| `OTHER` | 其他 | - |

#### 车身类型编码

| 编码 | 车身类型 |
|------|---------|
| `SUV` | SUV |
| `SEDAN` | 三厢车 / 轿车 |
| `HATCHBACK` | 两厢车 / 掀背车 |
| `MPV` | MPV |
| `WAGON` | 旅行车 |
| `COUPE` | 跑车 / 硬顶跑车 |
| `PICKUP` | 皮卡 |
| `MINIVAN` | 微面 / 面包车 |
| `VAN` | 轻客 |
| `BUS` | 客车 |
| `TRUCK` | 货车 / 卡车 |
| `CONVERTIBLE` | 硬顶敞篷车 / 软顶敞篷车 |

### 5.4 运行清洗

```bash
python data_clean.py
```

运行后会输出数据质量报告，包含总行数、品牌数、价格范围、年份范围及各字段空值统计。

---

## 六、产出数据结构

### 6.1 原始数据（data.csv）— 11 列

| 列名 | 说明 | 示例 |
|------|------|------|
| 品牌 | 汽车品牌 | smart |
| 基本参数_级别 | 车型级别 | 微型车 |
| 基本参数_能源类型 | 能源类型 | 汽油 |
| 基本参数_上市时间 | 上市日期 | 2018-06-14 |
| 基本参数_车身结构 | 车身结构 | 5门4座两厢车 |
| 基本参数_厂商指导价 | 指导价 | 12.28万 |
| 基本参数_整车质保 | 质保信息 | 三年不限公里 |
| 基本参数_变速箱 | 变速箱 | 6挡干式双离合 |
| 基本参数_发动机 | 发动机 | 1.0L 71马力 L3 |
| 车身_车身结构 | 车身类型 | 两厢车 |
| 发动机_燃油标号 | 燃油标号 | 95号 |

### 6.2 清洗后数据（output/car_clean.csv）— 22 列

| 列名 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `brand` | str | 品牌 | smart |
| `level` | str | 级别 | 微型车 |
| `energy_code` | int | 能源编码 | 1 |
| `energy_name` | str | 能源名称 | 汽油 |
| `fuel_grade` | int | 燃油标号 | 95 |
| `launch_date` | date | 上市日期 | 2018-06-14 |
| `launch_year` | int | 上市年份 | 2018 |
| `launch_month` | int | 上市月份 | 6 |
| `launch_season` | str | 上市季度 | Q2 |
| `price_wan` | float | 价格（万元） | 12.28 |
| `warranty_years` | int | 质保年限 | 3 |
| `warranty_km_wan` | float | 质保里程（万公里，-1 为不限） | -1.0 |
| `displacement_L` | float | 排量（L） | 1.0 |
| `horsepower` | int | 马力 | 71 |
| `engine_type` | str | 发动机类型 | L3 |
| `is_electric` | int | 是否电动（0/1） | 0 |
| `gear_count` | int | 档位数 | 6 |
| `trans_type` | str | 变速箱类型 | DCT |
| `doors` | int | 车门数 | 5 |
| `seats` | int | 座位数 | 4 |
| `body_type` | str | 车身类型编码 | HATCHBACK |
| `body_structure` | str | 车身结构原文 | 5门4座两厢车 |

---

## 七、并发基准测试（bench_concurrency.py）

**功能**：取前 30 个 URL，测试不同并发数（1/2/3）的爬取耗时与速率。

**运行**：
```bash
python bench_concurrency.py
```

**输出示例**：
```
并发数   耗时(s)    数据(条)   失败    速率(条/s)
------------------------------------------------------------
1        45.2       240        0       5
2        24.8       240        0       10 (x1.8)
3        17.3       240        0       14 (x2.6)
------------------------------------------------------------
```

用于评估最优并发数，平衡速度与被封风险。

---

## 八、环境配置

### 8.1 依赖安装

```bash
pip install requests aiohttp openpyxl pandas numpy python-dotenv
```

### 8.2 环境变量（.env）

```bash
# Cookie 配置（支持多个，轮换使用）
AUTOHOME_COOKIE_1=your_cookie_1
AUTOHOME_COOKIE_2=your_cookie_2
# ... 最多 20 个

# 并发控制
MAX_CONCURRENCY=3
REQUEST_TIMEOUT=15

# User-Agent
USER_AGENT=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
```

> **注意**：`.env` 文件已在 `.gitignore` 中忽略，不会提交到仓库。

---

## 九、完整运行流程

```bash
# 1. 配置 .env（填入 Cookie）
cp .env.example .env
# 编辑 .env 填入 AUTOHOME_COOKIE_1

# 2. 采集车系 URL（首次运行）
python collect_urls.py
# 产出：series_urls.xlsx

# 3. 爬取数据（异步模式）
python main.py
# 产出：data.csv（原始数据）

# 4. 数据清洗
python data_clean.py
# 产出：output/car_clean.csv（结构化数据）

# 5.（可选）重试失败
python main.py --retry-failed

# 6.（可选）并发测试
python bench_concurrency.py
```

---

## 十、关键设计亮点

| 设计点 | 实现方式 | 价值 |
|--------|---------|------|
| **多 Cookie 轮换** | `CookieRotator` 线程安全轮询 | 降低单 Cookie 被封风险 |
| **断点续爬** | `CheckpointManager` 持久化已完成 ID | 中断后无需重头开始 |
| **失败队列** | `FailedQueue` 记录失败原因 | 支持针对性重试 |
| **增量写入** | `CsvAppender` 追加模式 | 避免内存堆积，崩溃不丢数据 |
| **异步并发** | `aiohttp` + `Semaphore` | 大幅提升爬取效率 |
| **智能重试** | 403/429/超时差异化处理 | 平衡成功率与被封风险 |
| **结构化清洗** | 正则 + 映射表 | 原始文本转为可分析数值 |
| **编码标准化** | 能源/变速箱/车身统一编码 | 便于下游统计分析 |

---

## 十一、与下游系统的数据对接

清洗后的 `output/car_clean.csv`（22 列结构化数据）可直接对接：

1. **后端 Spring Boot 服务**：导入数据库 `cars` 表，提供 57 个 REST API 接口
2. **前端可视化系统**：通过 API 调用，渲染基础统计、高级分析、业务关联分析等图表
3. **大数据分析平台**：导入 Hive 数据仓库，支持 SQL 查询分析

**字段映射关系**：清洗后字段名（如 `brand`、`price_wan`、`horsepower`）与后端 API 返回字段保持一致，确保数据链路畅通。

---

## 十二、维护注意事项

1. **Cookie 更新**：汽车之家 Cookie 会过期，需定期更新 `.env` 中的 `AUTOHOME_COOKIE_N`
2. **反爬升级**：若出现 403 频发，需降低 `MAX_CONCURRENCY` 或增加 Cookie 数量
3. **数据完整性**：运行 `data_clean.py` 后检查数据质量报告，关注空值比例高的字段
4. **断点文件**：`checkpoint.json` 和 `failed.json` 已在 `.gitignore` 中忽略，不要提交
5. **日志查看**：运行日志写入 `crawler.log`，便于排查问题
6. **字段扩展**：如需新增爬取字段，修改 `config.py` 的 `CSV_FIELDS` 和 `TARGET_CATEGORIES`

---

## 十三、相关文档

- [后端代码分析文档](./back.md)
- [前端可视化文档](./front.md)
- [汽车之家官网](https://www.autohome.com.cn/)
