# 汽车大数据分析平台

> 基于 Python 爬虫 + Spring Boot 后端 + Vue 3 前端的汽车大数据全栈分析平台，覆盖数据采集、清洗、存储、分析、可视化全链路。

---

## 一、项目简介

本项目是一个完整的汽车大数据分析平台，从汽车之家爬取 25,000+ 条车型参数数据，经结构化清洗后存入数据库，通过 57 个 REST API 接口提供多维度统计分析，最终在前端以柱状图、折线图、饼图、热力图、箱线图、散点图等多种图表形式可视化展示。

### 核心能力

- **数据采集**：异步并发爬取汽车之家 6000+ 车系，支持多 Cookie 轮换、断点续爬、失败重试
- **数据清洗**：将 11 列原始文本清洗为 22 列结构化数值，含价格、日期、发动机、变速箱等标准化编码
- **后端服务**：Spring Boot 提供 57 个 REST API，支持 Hive 数据仓库 + H2 内存数据库双数据源
- **前端可视化**：Vue 3 + ECharts 实现 4 大模块可视化，含 7 种图表类型分类构建器

---

## 二、技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **数据采集** | Python + aiohttp + requests | Python 3.10+ |
| **数据清洗** | pandas + numpy + openpyxl | - |
| **后端服务** | Spring Boot + Java + H2 + Hive JDBC | Spring Boot 2.7.18 / Java 11 |
| **前端可视化** | Vue 3 + Vite + ECharts + Axios | Vue 3.5 / Vite 8 |
| **版本控制** | Git | - |

---

## 三、目录结构

```
Big_Data_Analysis_Training_Session/
├── README.md                           # 本文档
├── back.md                             # 后端代码分析文档
├── front.md                            # 前端可视化文档
├── crawler.md                          # 爬虫架构与数据清洗文档
├── data/                               # 二手车源数据（懂车帝，独立数据集）
│   ├── data.csv
│   └── data.json
├── bigData-main/                       # 爬虫项目（数据采集与清洗）
│   └── scawler_carhome/
│       ├── config.py                   # 全局配置（API、Cookie、并发）
│       ├── collect_urls.py             # 车系URL采集器（A-Z遍历）
│       ├── crawler.py                  # 爬取引擎（异步+同步双模式）
│       ├── export.py                   # 数据导出（CSV/Excel）
│       ├── main.py                     # 主入口
│       ├── data_clean.py               # 数据清洗（11列→22列）
│       ├── bench_concurrency.py        # 并发基准测试
│       ├── data.csv                    # 原始爬取数据（25,021行×11列）
│       └── output/
│           └── car_clean.csv           # 清洗后数据（25,021行×22列）
├── back/                               # 后端项目（Spring Boot）
│   └── bigwork/bigwork/cars-analysis/
│       ├── src/main/java/com/example/cars/
│       │   ├── CarsApplication.java            # 启动类
│       │   ├── DataSourceConfig.java           # 数据源配置（Hive优先，H2降级）
│       │   ├── DataInitializer.java            # 启动加载CSV到H2
│       │   ├── CarsService/Controller          # 基础统计（5接口）
│       │   ├── AdvancedCarsService/Controller  # 高级分析（22接口）
│       │   ├── BusinessAnalysisService/Controller # 业务关联（17接口）
│       │   ├── FileDataService                 # 文件上传解析
│       │   ├── ReportService                   # 报表生成
│       │   └── DataController                  # 数据分析（7接口）
│       ├── src/main/resources/
│       │   ├── application.properties          # Spring Boot 配置
│       │   └── car_clean.csv                   # 嵌入JAR的清洗数据
│       └── pom.xml                             # Maven 构建配置
└── Visualization/                      # 前端项目（Vue 3）
    ├── src/
    │   ├── api.js                # API接口封装（57个接口）
    │   ├── echarts/                    # ECharts分类图表构建器
    │   │   ├── echarts.js              # ECharts库
    │   │   ├── histogram.js            # 柱状图（横向/纵向/堆叠/分组）
    │   │   ├── line.js                 # 折线图（面积/堆叠面积）
    │   │   ├── pie.js                  # 饼图（环形/玫瑰图）
    │   │   ├── heatmap.js              # 热力图
    │   │   ├── boxplot.js              # 箱线图
    │   │   ├── scatter.js              # 散点图
    │   │   ├── treemap.js              # 矩形树图
    │   │   └── index.js               # 统一导出
    │   ├── components/
    │   │   ├── BaseChart.vue           # ECharts通用图表组件
    │   │   └── ChartCard.vue           # 图表卡片容器
    │   ├── views/
    │   │   ├── DashboardView.vue       # 基础统计仪表盘
    │   │   ├── AdvancedAnalysisView.vue # 高级分析（分类+排序+散点图）
    │   │   ├── BusinessAnalysisView.vue # 业务关联分析（5大维度）
    │   │   └── DataManagementView.vue  # 数据管理（上传+报表）
    │   ├── router/index.js             # 路由配置
    │   └── App.vue                     # 主布局
    ├── vite.config.js                  # Vite配置（含API代理）
    └── package.json
```

---

## 四、数据链路

```
汽车之家 API (autohome.com.cn)
    │
    ▼ collect_urls.py 采集A-Z车系URL
series_urls.xlsx (6000+ 车系)
    │
    ▼ crawler.py 异步并发爬取（多Cookie轮换+断点续爬）
data.csv (25,021行 × 11列原始数据)
    │
    ▼ data_clean.py 结构化清洗
car_clean.csv (25,021行 × 22列结构化数据)
    │
    ▼ 复制到后端 resources/
后端 JAR 内嵌 car_clean.csv
    │
    ▼ DataInitializer.java 启动加载
H2 内存数据库 cars 表 (25,021行 × 22字段)
    │
    ▼ 57 个 REST API 多维度查询
前端 Vue 可视化 (4大模块 / 7种图表类型)
```

---

## 五、数据字段说明

清洗后数据共 22 个字段，与后端 `cars` 表完全一致：

| 字段名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| brand | VARCHAR | 品牌 | smart |
| level | VARCHAR | 级别 | 微型车 |
| energy_code | STRING | 能源编码 | 1 |
| energy_name | VARCHAR | 能源名称 | 汽油 |
| fuel_grade | STRING | 燃油标号 | 95 |
| launch_date | VARCHAR | 上市日期 | 2018-06-14 |
| launch_year | INT | 上市年份 | 2018 |
| launch_month | INT | 上市月份 | 6 |
| launch_season | VARCHAR | 上市季度 | Q2 |
| price_wan | DOUBLE | 价格（万元） | 12.28 |
| warranty_years | INT | 质保年限 | 3 |
| warranty_km_wan | DOUBLE | 质保里程（万公里，-1为不限） | -1.0 |
| displacement_L | DOUBLE | 排量（L） | 1.0 |
| horsepower | DOUBLE | 马力 | 71.0 |
| engine_type | VARCHAR | 发动机类型 | L3 |
| is_electric | STRING | 是否电动（0/1） | 0 |
| gear_count | INT | 档位数 | 6 |
| trans_type | VARCHAR | 变速箱类型 | DCT |
| doors | INT | 门数 | 5 |
| seats | INT | 座位数 | 4 |
| body_type | VARCHAR | 车身类型编码 | HATCHBACK |
| body_structure | VARCHAR | 车身结构原文 | 5门4座两厢车 |

---

## 六、功能模块

### 6.1 爬虫模块（bigData-main）

| 功能 | 说明 |
|------|------|
| URL 采集 | 遍历汽车之家 A-Z 字母页，提取 6000+ 车系 URL |
| 异步爬取 | aiohttp 并发爬取，支持多 Cookie 轮换、断点续爬、失败重试 |
| 数据清洗 | 价格标准化、日期解析、质保拆分、发动机/变速箱/车身编码 |
| 并发测试 | 基准测试不同并发数的耗时与速率 |

### 6.2 后端模块（back）

| 模块 | 接口数 | 说明 |
|------|--------|------|
| 基础统计 | 11 | 数据源状态、品牌排名、价格分布、能源分布、级别分布、概览、搜索、品牌列表、级别列表、品牌对比、级别对比 |
| 高级分析-分类 | 13 | 13个维度分组统计（品牌/级别/能源/车身/变速箱等） |
| 高级分析-排序 | 9 | 9种度量排序（价格/马力/排量/座位数等） |
| 业务关联分析 | 17 | 5大业务维度交叉分析 |
| 数据分析 | 7 | 文件上传、数据导入、报表生成/下载 |
| **合计** | **57** | - |

### 6.3 前端模块（Visualization）

| 页面 | 路由 | 图表类型 |
|------|------|---------|
| 基础统计 | /dashboard | 横向柱状图、环形图、玫瑰图、纵向柱状图 |
| 高级分析 | /advanced | 柱状图、饼图、漏斗图、折线图、散点图 |
| 业务关联分析 | /business | 分组柱状图、堆叠面积图、热力图、箱线图、矩形树图、双轴组合图 |
| 车辆搜索 | /search | 数据表格、筛选条件、分页 |
| 对比分析 | /compare | 分组柱状图、雷达图、对比表格 |
| 数据管理 | /data | 文件上传、数据表预览、报表管理 |

---

## 七、快速开始

### 7.1 环境要求

- Node.js ^22.18.0
- Java 11
- Python 3.10+
- Maven 3.6+

### 7.2 数据采集（可选，已有清洗数据）

```bash
cd bigData-main/scawler_carhome

# 配置 Cookie
cp .env.example .env  # 编辑填入 AUTOHOME_COOKIE_1

# 采集URL
python collect_urls.py

# 爬取数据
python main.py

# 数据清洗
python data_clean.py
# 产出：output/car_clean.csv
```

### 7.3 启动后端

```bash
cd back/bigwork/bigwork/cars-analysis
mvn clean package
java -jar target/cars-analysis-1.0.0.jar
# 启动后访问 http://localhost:8088
```

### 7.4 启动前端

```bash
cd Visualization
npm install
npm run dev
# 启动后访问 http://localhost:5173
```

### 7.5 联调验证

1. 启动后端服务（端口 8088）
2. 启动前端开发服务器（端口 5173）
3. 前端通过 Vite 代理自动转发 `/api/*` 至后端
4. 浏览器访问 `http://localhost:5173` 即可查看可视化图表

---

## 八、API 接口概览

服务端口：**8088**，所有接口返回 JSON 格式数据。

### 基础统计

```
GET /api/cars/datasource              # 数据源状态
GET /api/cars/brand-ranking           # 品牌排名 Top 10
GET /api/cars/price-distribution      # 价格区间分布
GET /api/cars/energy-distribution     # 能源类型分布
GET /api/cars/level-distribution      # 车型级别分布
GET /api/cars/overview               # 仪表盘概览
GET /api/cars/search                 # 车辆搜索（支持品牌/级别/价格/能源/变速箱筛选）
GET /api/cars/brands                # 品牌列表
GET /api/cars/levels                # 级别列表
GET /api/cars/compare/brands        # 品牌对比
GET /api/cars/compare/levels        # 级别对比
```

### 高级分析

```
GET /api/cars/advanced/group/{dimension}   # 13个分类维度
GET /api/cars/advanced/sort/{metric}       # 9个排序度量（支持order/limit参数）
```

### 业务关联分析

```
GET /api/cars/biz/brand-level-price/*      # 品牌×级别×指导价
GET /api/cars/biz/energy-trend/*           # 上市时间×能源类型
GET /api/cars/biz/power-match/*            # 排量×燃油×马力
GET /api/cars/biz/space-config/*           # 车身×座位×级别
GET /api/cars/biz/powertrain/*             # 变速箱×发动机
```

### 数据分析

```
POST /api/data/upload                # 上传文件解析
POST /api/data/import                # 上传并导入数据库
GET  /api/data/preview               # 数据预览
GET  /api/data/report/generate       # 生成报表
POST /api/data/report/save           # 保存报表
GET  /api/data/report/list           # 报表列表
GET  /api/data/report/download       # 下载报表
```

完整接口文档详见 [back.md](./back.md)。

---

## 九、项目文档

| 文档 | 说明 |
|------|------|
| [back.md](./back.md) | 后端代码分析文档（57个API接口详解） |
| [front.md](./front.md) | 前端可视化文档（4大模块+7种图表） |
| [crawler.md](./crawler.md) | 爬虫架构与数据清洗文档（采集流程+编码映射） |

---

## 十、关键设计亮点

| 模块 | 设计点 | 价值 |
|------|--------|------|
| 爬虫 | 多 Cookie 轮换 | 降低单 Cookie 被封风险 |
| 爬虫 | 断点续爬 + 失败队列 | 中断后无需重头，支持针对性重试 |
| 爬虫 | 增量写入 CSV | 避免内存堆积，崩溃不丢数据 |
| 爬虫 | 结构化清洗 | 原始文本转为可分析数值，统一编码 |
| 后端 | Hive + H2 双数据源 | 生产用 Hive，开发降级 H2 |
| 后端 | 57 个 API 全维度覆盖 | 基础+高级+业务关联完整分析能力 |
| 前端 | ECharts 分类构建器 | 7种图表类型分类封装，便于维护 |
| 前端 | 推荐图表选型 | 热力图/箱线图/散点图等科学选型 |

---

## 十一、版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-21 | 初始版本：爬虫+后端+前端完整链路 |

---

## 十二、许可证

本项目仅供学习和实训使用。
