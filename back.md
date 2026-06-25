# Back 文件夹后端代码分析文档

## 一、项目概述

`back` 文件夹包含一个**汽车大数据分析平台**的后端服务，基于 **Spring Boot 2.7.18 + Java 11** 构建，提供 RESTful API 用于汽车数据的统计分析、可视化支持与报表生成。

**核心特性：**
- **双数据源策略**：优先连接 Hive 数据仓库，连接失败自动降级到 H2 内存数据库（从 CSV 加载数据）
- **多维度分析能力**：基础统计（4项）、分类维度（13项）、排序度量（9项）、业务关联（17项）
- **文件上传与报表**：支持 CSV/Excel 文件上传解析、数据预览、全维度分析报表生成与下载

---

## 二、目录结构

```
back/
└── bigwork/
    ├── bigwork/
    │   └── cars-analysis/                       # Spring Boot 后端主项目
    │       ├── src/main/
    │       │   ├── java/com/example/cars/
    │       │   │   ├── CarsApplication.java             # 启动类
    │       │   │   ├── DataSourceConfig.java            # 数据源配置（Hive优先，H2降级）
    │       │   │   ├── HiveConfig.java                  # 数据库连接配置 Bean
    │       │   │   ├── DataInitializer.java             # 启动时从CSV加载H2
    │       │   │   ├── CarsService.java                 # 基础统计业务层
    │       │   │   ├── CarsController.java              # 基础统计接口
    │       │   │   ├── AdvancedCarsService.java         # 高级分析业务层
    │       │   │   ├── AdvancedCarsController.java      # 高级分析接口
    │       │   │   ├── BusinessAnalysisService.java     # 业务关联分析业务层
    │       │   │   ├── BusinessAnalysisController.java  # 业务关联分析接口
    │       │   │   ├── FileDataService.java             # 文件上传解析服务
    │       │   │   ├── ReportService.java               # 报表生成与存储服务
    │       │   │   └── DataController.java              # 数据分析接口
    │       │   └── resources/
    │       │       ├── application.properties           # Spring Boot 配置
    │       │       ├── car_clean.csv                    # 嵌入JAR的清洗数据
    │       │       └── static/index.html                # 前端可视化页面
    │       ├── lib/                                     # 手动引入的JAR包
    │       │   ├── hadoop-common-3.3.6.jar
    │       │   └── hive-jdbc-4.0.0-standalone.jar
    │       ├── pom.xml                                  # Maven 构建配置
    │       └── car_clean.csv
    ├── maven/                                           # 内置 Maven 3.9.16
    └── 项目文档.md
```

---

## 三、技术栈

| 组件 | 版本/说明 |
|------|----------|
| Spring Boot | 2.7.18 |
| Java | 11 |
| Hive JDBC | 4.0.0（standalone） |
| Hadoop Common | 3.3.6 |
| H2 Database | 内存数据库（运行时降级使用） |
| OpenCSV | 5.9（CSV 解析） |
| Apache POI | 5.2.5（Excel 解析） |
| Maven | 3.9.16（项目内置） |

---

## 四、核心配置

### 4.1 application.properties

```properties
server.port=8088
spring.mvc.pathmatch.use-suffix-pattern=false

# 文件上传配置
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# Hive 连接配置
hive.url=jdbc:hive2://121.4.19.133:10000/default
hive.user=ubuntu
hive.password=
```

### 4.2 数据源策略（Hive 优先、H2 降级）

1. 应用启动时，`DataSourceConfig` 先尝试连接 Hive（3秒超时）
2. Hive 可用 → 使用 Hive 数据仓库
3. Hive 不可用 → 自动降级到 H2 内存数据库，`DataInitializer` 从 classpath 的 `car_clean.csv` 加载数据
4. 运行时 Hive 连接失败 → 自动降级到 H2

---

## 五、Java 源文件功能说明

### 5.1 核心框架层

| 文件 | 功能说明 |
|------|---------|
| [CarsApplication.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/CarsApplication.java) | Spring Boot 启动类，`@SpringBootApplication` 注解 |
| [DataSourceConfig.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/DataSourceConfig.java) | 数据源核心配置：Hive 优先、H2 降级策略。首次连接时检测 Hive 可用性（3秒超时），运行时 Hive 连接失败也会降级 |
| [HiveConfig.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/HiveConfig.java) | Spring 配置类，提供 `ConnectionProvider` Bean，委托 `DataSourceConfig` 获取连接 |
| [DataInitializer.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/DataInitializer.java) | 实现 `CommandLineRunner`，启动时从 classpath 的 `car_clean.csv` 加载数据到 H2；若数据源为 Hive 则跳过 |

### 5.2 基础统计层

| 文件 | 功能说明 |
|------|---------|
| [CarsService.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/CarsService.java) | 4 个基础查询方法：品牌排名、价格分布、能源分布、级别分布 |
| [CarsController.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/CarsController.java) | 5 个 REST 接口：数据源状态 + 4 个基础统计接口 |

### 5.3 高级分析层（分类 + 排序）

| 文件 | 功能说明 |
|------|---------|
| [AdvancedCarsService.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/AdvancedCarsService.java) | 22 个方法：9 个分组维度 + 4 个离散化分类 + 9 个排序度量。排序支持 `order`（asc/desc）和 `limit`（1~100）参数 |
| [AdvancedCarsController.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/AdvancedCarsController.java) | 22 个 REST 接口，分组用 `/group/{dimension}`，排序用 `/sort/{metric}` |

### 5.4 业务关联分析层

| 文件 | 功能说明 |
|------|---------|
| [BusinessAnalysisService.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/BusinessAnalysisService.java) | 17 个方法，覆盖 5 大业务维度：品牌×级别×指导价、上市时间×能源类型、排量×燃油标号×马力、车身结构×座位数×级别、变速箱×发动机配置 |
| [BusinessAnalysisController.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/BusinessAnalysisController.java) | 17 个 REST 接口，按维度分组 |

### 5.5 文件上传与报表层

| 文件 | 功能说明 |
|------|---------|
| [FileDataService.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/FileDataService.java) | 文件解析（CSV 用 OpenCSV，Excel 用 Apache POI）、数据导入（匹配 cars 表结构则导入 cars 表，否则导入 generic_data 表）、数据预览、表结构查询 |
| [ReportService.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/ReportService.java) | 全维度分析报表生成（汇总基础统计 + 分类维度 + 业务关联）、保存为 CSV 文件（BOM 头兼容 Excel）、报表列表查询、报表文件读取 |
| [DataController.java](file:///e:/VScode/大数据分析实训-肖/back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/DataController.java) | 7 个 REST 接口：文件上传、数据导入、数据预览、报表生成/保存/列表/下载 |

---

## 六、API 接口总览

服务端口：**8088**，所有接口返回 JSON 格式数据。

### 6.1 基础统计接口 — `/api/cars/`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/cars/datasource` | 获取当前数据源状态（hive/h2） |
| GET | `/api/cars/brand-ranking` | 品牌数量 Top 10 |
| GET | `/api/cars/price-distribution` | 价格区间分布（10万以下/10-20万/20-30万/30-50万/50万以上） |
| GET | `/api/cars/energy-distribution` | 能源类型分布 |
| GET | `/api/cars/level-distribution` | 车型级别分布 |

### 6.2 高级分析 - 分类维度 — `/api/cars/advanced/group/`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/group/brand` | 按品牌分类 |
| GET | `/group/level` | 按级别分类 |
| GET | `/group/energy` | 按能源类型分类 |
| GET | `/group/body-type` | 按车身结构分类 |
| GET | `/group/trans-type` | 按变速箱大类分类 |
| GET | `/group/trans-subtype` | 按变速箱亚型分类（大类+档位数） |
| GET | `/group/fuel-grade` | 按燃油标号分类 |
| GET | `/group/engine-layout` | 按发动机气缸布局分类 |
| GET | `/group/warranty-unlimited` | 按质保里程是否不限分类 |
| GET | `/group/launch-year` | 按上市年份分类（离散化） |
| GET | `/group/price-range` | 按价格区间分类（离散化） |
| GET | `/group/seats` | 按座位数分类（离散化） |
| GET | `/group/gear-count` | 按档位数分类（离散化） |

### 6.3 高级分析 - 排序度量 — `/api/cars/advanced/sort/`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/sort/launch-date` | order, limit | 按上市时间排序 |
| GET | `/sort/price` | order, limit | 按厂商指导价排序 |
| GET | `/sort/displacement` | order, limit | 按发动机排量排序 |
| GET | `/sort/horsepower` | order, limit | 按最大马力排序 |
| GET | `/sort/doors` | order, limit | 按门数排序 |
| GET | `/sort/seats` | order, limit | 按座位数排序 |
| GET | `/sort/gear-count` | order, limit | 按变速箱档位数排序 |
| GET | `/sort/warranty-years` | order, limit | 按质保年限排序 |
| GET | `/sort/warranty-km` | order, limit | 按质保里程排序 |

> **参数说明：** `order` = asc/desc（默认 desc），`limit` = 1~100（默认 10，超出范围自动截断）

### 6.4 业务关联分析 — `/api/cars/biz/`

#### 6.4.1 品牌 × 级别 × 指导价

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/brand-level-price/band` | 各品牌在各级别中的价格区间（最低/最高/均价/车型数） |
| GET | `/brand-level-price/by-level?level=SUV` | 指定级别下各品牌定价对比 |
| GET | `/brand-level-price/level-summary` | 各级别品牌数量与价格跨度总览 |

#### 6.4.2 上市时间 × 能源类型

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/energy-trend/by-year` | 按年份统计各能源类型新车数量 |
| GET | `/energy-trend/new-energy-ratio` | 按年份统计新能源占比 |
| GET | `/energy-trend/by-season` | 按季度统计各能源类型新车数量 |
| GET | `/energy-trend/brand-ratio` | 各品牌新能源车型占比 |

#### 6.4.3 排量 × 燃油标号 × 马力

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/power-match/displacement-fuel` | 各排量段的主流燃油标号分布 |
| GET | `/power-match/displacement-horsepower` | 各排量段的平均马力与区间 |
| GET | `/power-match/detail` | 排量×燃油标号×马力详细交叉表 |

#### 6.4.4 车身结构 × 座位数 × 级别

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/space-config/level-seats` | 各级别座位数分布 |
| GET | `/space-config/body-type-seats` | 各车身结构座位数分布 |
| GET | `/space-config/level-body-seats?level=SUV` | 指定级别下车身结构与座位数交叉分布 |
| GET | `/space-config/seven-seat-ratio` | 各级别7座车型占比 |

#### 6.4.5 变速箱 × 发动机配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/powertrain/trans-engine-combo` | 变速箱×发动机搭配频次 |
| GET | `/powertrain/displacement-trans` | 各排量段主流变速箱类型分布 |
| GET | `/powertrain/trans-disp-hp` | 各变速箱类型平均排量与马力 |
| GET | `/powertrain/trans-electric` | 变速箱×是否电动搭配统计 |

### 6.5 数据分析接口 — `/api/data/`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| POST | `/upload` | file (MultipartFile) | 上传数据文件（CSV/Excel），解析返回预览（前20行） |
| POST | `/import` | file (MultipartFile) | 上传并导入数据到数据库（匹配 cars 表则导入 cars，否则导入 generic_data） |
| GET | `/preview` | table, limit | 预览数据库表数据（默认 cars 表，前 50 行） |
| GET | `/report/generate` | - | 生成全维度分析报表（JSON 格式） |
| POST | `/report/save` | - | 生成报表并保存为 CSV 文件（带 BOM 头兼容 Excel） |
| GET | `/report/list` | - | 获取已保存的报表列表 |
| GET | `/report/download` | filename | 下载报表文件 |

---

## 七、数据表结构

### 7.1 cars 表（核心数据表，22 个字段）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| brand | VARCHAR(100) | 品牌 |
| level | VARCHAR(50) | 级别 |
| energy_code | STRING | 能源代码 |
| energy_name | VARCHAR(50) | 能源名称 |
| fuel_grade | STRING | 燃油标号 |
| launch_date | VARCHAR(30) | 上市日期 |
| launch_year | INT | 上市年份 |
| launch_month | INT | 上市月份 |
| launch_season | VARCHAR(5) | 上市季度 |
| price_wan | DOUBLE | 价格（万元） |
| warranty_years | INT | 质保年限 |
| warranty_km_wan | DOUBLE | 质保里程（万公里，-1 表示不限） |
| displacement_L | DOUBLE | 排量（L） |
| horsepower | DOUBLE | 马力 |
| engine_type | VARCHAR(20) | 发动机类型 |
| is_electric | STRING | 是否电动（0/1） |
| gear_count | INT | 档位数 |
| trans_type | VARCHAR(20) | 变速箱类型 |
| doors | INT | 门数 |
| seats | INT | 座位数 |
| body_type | VARCHAR(30) | 车身类型 |
| body_structure | VARCHAR(100) | 车身结构 |

### 7.2 generic_data 表（通用数据表）

当上传文件不匹配 cars 表结构时，自动创建并导入此表，所有字段均为 VARCHAR(500)。

---

## 八、接口调用示例

### 8.1 获取数据源状态

```http
GET http://localhost:8088/api/cars/datasource
```

响应示例：
```json
{
  "source": "h2",
  "label": "H2 本地数据库",
  "status": "fallback"
}
```

### 8.2 品牌排名 Top 10

```http
GET http://localhost:8088/api/cars/brand-ranking
```

### 8.3 按价格降序取前 5

```http
GET http://localhost:8088/api/cars/advanced/sort/price?order=desc&limit=5
```

### 8.4 上传 CSV 文件

```http
POST http://localhost:8088/api/data/upload
Content-Type: multipart/form-data

file: <CSV 或 Excel 文件>
```

### 8.5 生成全维度报表

```http
GET http://localhost:8088/api/data/report/generate
```

---

## 九、接口统计汇总

| 模块 | 接口数 | 路径前缀 |
|------|--------|---------|
| 基础统计 | 5 | `/api/cars/` |
| 高级分析-分类 | 13 | `/api/cars/advanced/group/` |
| 高级分析-排序 | 9 | `/api/cars/advanced/sort/` |
| 业务关联分析 | 17 | `/api/cars/biz/` |
| 数据分析 | 7 | `/api/data/` |
| **合计** | **51** | - |

---

## 十、构建与运行

### 10.1 构建

```bash
cd back/bigwork/bigwork/cars-analysis
mvn clean package
```

构建产物：`target/cars-analysis-1.0.0.jar`

### 10.2 运行

```bash
java -jar target/cars-analysis-1.0.0.jar
```

启动后访问：`http://localhost:8088/`（前端页面）或 `http://localhost:8088/api/cars/datasource`（API 测试）

### 10.3 数据源切换

- **Hive 可用**：自动连接 `jdbc:hive2://121.4.19.133:10000/default`
- **Hive 不可用**：自动降级到 H2 内存数据库，从 JAR 内嵌的 `car_clean.csv` 加载数据
