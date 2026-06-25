# Hive 数据仓库接口文档
## —— 汽车数据分析平台数据层接口

---

## 一、概述

本文档面向**后端开发人员**，描述数据存储与数据分析模块提供的 Hive 数据仓库接口。后端应用可通过 **JDBC 连接 HiveServer2** 执行 SQL 查询，获取车型数据的统计分析结果。

**接口性质**：数据查询接口（基于 HiveQL / SQL-on-Hadoop）  
**适用场景**：BI 报表、数据分析 API、大数据应用后端集成

---

## 二、连接信息

### 2.1 HiveServer2 连接参数

| 参数 | 值 |
|------|-----|
| 服务地址 | `121.4.19.133:10000`（公网） / `localhost:10000`（本机） |
| 协议 | JDBC（Hive JDBC Driver） |
| 数据库 | `default` |
| 认证方式 | 无认证（用户名任意，密码为空） |
| 驱动类 | `org.apache.hive.jdbc.HiveDriver` |

> **注意**：HiveServer2 运行在 10000 端口，后端 Spring Boot 服务运行在 8088 端口（本地），前端 Vite 开发服务器运行在 5173 端口。

### 2.2 JDBC 连接示例

#### Java
```java
// 1. 引入 Maven 依赖（需从本服务器复制 jar 到本地仓库）
// hive-jdbc-4.0.0-standalone.jar + hadoop-common-3.3.6.jar

// 2. 连接代码
Class.forName("org.apache.hive.jdbc.HiveDriver");
Connection conn = DriverManager.getConnection(
    "jdbc:hive2://121.4.19.133:10000/default", 
    "ubuntu",  // 用户名（任意）
    ""         // 密码（空字符串）
);

// 3. 执行查询
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cars");
while (rs.next()) {
    System.out.println("总记录数: " + rs.getInt(1));
}
```

#### Python
```python
from pyhive import hive

conn = hive.Connection(
    host='121.4.19.133',
    port=10000,
    username='ubuntu',
    database='default'
)

cursor = conn.cursor()
cursor.execute("SELECT brand, COUNT(*) AS cnt FROM cars GROUP BY brand LIMIT 5")
for row in cursor.fetchall():
    print(row)
```

#### beeline 命令行工具
```bash
beeline -u "jdbc:hive2://121.4.19.133:10000/default" -n ubuntu
```

---

## 三、数据表结构

### 3.1 表名：`cars`

**表类型**：外部表（EXTERNAL TABLE）  
**存储位置**：HDFS `/cars_data/car_clean.csv`  
**记录数**：25,022 条  
**数据格式**：CSV（逗号分隔，首行为字段名已跳过）

### 3.2 字段定义

| 字段名 | 类型 | 说明 | 示例值 |
|--------|------|------|--------|
| brand | STRING | 品牌 | `福田`、`江铃`、`丰田` |
| level | STRING | 车型级别 | `皮卡`、`紧凑型SUV`、`轻客` |
| energy_code | STRING | 能源编码 | `1`（汽油）、`2`（纯电动） |
| energy_name | STRING | 能源类型名称 | `汽油`、`纯电动`、`柴油`、`插电式混合动力` |
| fuel_grade | STRING | 燃油标号 | `92`、`95`、`0`（柴油） |
| launch_date | STRING | 上市日期 | `2025-03-15` |
| launch_year | INT | 上市年份 | `2025` |
| launch_month | INT | 上市月份 | `3` |
| launch_season | STRING | 上市季度 | `Q1`、`Q2` |
| price_wan | DOUBLE | 售价（万元） | `12.58`、`35.99` |
| warranty_years | INT | 质保年限（年） | `3`、`5` |
| warranty_km_wan | DOUBLE | 质保里程（万公里） | `10.0`、`-1.0`（无限制） |
| displacement_L | DOUBLE | 排量（升） | `1.5`、`2.0`、`NULL`（纯电动无此项） |
| horsepower | INT | 马力 | `150`、`200` |
| engine_type | STRING | 发动机类型 | `L4`（直列4缸）、`V6` |
| is_electric | STRING | 是否纯电 | `0`（否）、`1`（是） |
| gear_count | INT | 挡位数 | `5`、`6`、`NULL`（电动车） |
| trans_type | STRING | 变速箱类型 | `MT`（手动）、`AT`（自动）、`DCT`（双离合）、`CVT` |
| doors | INT | 车门数 | `2`、`4`、`5` |
| seats | INT | 座位数 | `2`、`5`、`7` |
| body_type | STRING | 车身类型 | `HATCHBACK`、`SEDAN`、`SUV` |
| body_structure | STRING | 车身结构 | `5门4座两厢车`、`4门5座三厢车` |

### 3.3 字段类型说明

> **注意**：以下字段在 Hive 中为 STRING 类型（CSV 文本加载），但在 H2 内存数据库中为 INT 类型：
> - `energy_code`：Hive STRING / H2 INT
> - `fuel_grade`：Hive STRING / H2 INT
> - `is_electric`：Hive STRING / H2 INT
>
> 后端 SQL 已使用 `CAST(... AS INT)` 进行类型转换，确保两种数据源下查询均正常。

### 3.4 数据质量说明

- **空值情况**：部分字段存在 `NULL` 或空字符串，查询时需注意处理
  - `trans_type` 约 5,066 条为空（未知）
  - `launch_year` 约 814 条为 `NULL`
  - `seats` 约 7,063 条为 `NULL`
- **单位统一**：价格统一为"万元"，里程为"万公里"

---

## 四、数据查询接口（推荐 SQL）

本节提供 **5 个预设分析维度的标准 SQL**，后端可直接使用或根据需求修改。

### 4.1 价格分布分析

**业务目标**：按价格区间统计车型数量，分析市场价格结构。

```sql
SELECT 
  CASE 
    WHEN price_wan < 10 THEN '10万以下'
    WHEN price_wan < 20 THEN '10-20万'
    WHEN price_wan < 30 THEN '20-30万'
    WHEN price_wan < 50 THEN '30-50万'
    ELSE '50万以上' 
  END AS price_range,
  COUNT(*) AS cnt
FROM cars
GROUP BY 
  CASE 
    WHEN price_wan < 10 THEN '10万以下'
    WHEN price_wan < 20 THEN '10-20万'
    WHEN price_wan < 30 THEN '20-30万'
    WHEN price_wan < 50 THEN '30-50万'
    ELSE '50万以上' 
  END
ORDER BY cnt DESC;
```

**返回结果示例**

| price_range | cnt |
|-------------|-----|
| 10-20万 | 11141 |
| 10万以下 | 7191 |
| 20-30万 | 2766 |
| 30-50万 | 1719 |
| 50万以上 | 2205 |

---

### 4.2 品牌车型数量排行

**业务目标**：统计各品牌在售车型数量 Top 10，反映产品线丰富度。

```sql
SELECT 
  brand, 
  COUNT(*) AS cnt
FROM cars
GROUP BY brand
ORDER BY cnt DESC
LIMIT 10;
```

**返回结果示例**

| brand | cnt |
|-------|-----|
| 福田 | 1583 |
| 江铃 | 747 |
| 长安凯程 | 713 |
| 长安跨越 | 671 |
| 江汽集团 | 651 |

---

### 4.3 能源类型分布分析

**业务目标**：统计汽油、电动、混动等能源类型占比，分析新能源趋势。

```sql
SELECT 
  energy_name, 
  COUNT(*) AS cnt
FROM cars
GROUP BY energy_name
ORDER BY cnt DESC;
```

**返回结果示例**

| energy_name | cnt |
|-------------|-----|
| 汽油 | 11973 |
| 纯电动 | 5873 |
| 柴油 | 3829 |
| 插电式混合动力 | 1373 |
| 汽油+48V轻混系统 | 592 |

---

### 4.4 车型级别分布分析

**业务目标**：统计皮卡、SUV、轿车等级别分布，分析车型结构。

```sql
SELECT 
  level, 
  COUNT(*) AS cnt
FROM cars
GROUP BY level
ORDER BY cnt DESC;
```

**返回结果示例**

| level | cnt |
|-------|-----|
| 皮卡 | 3532 |
| 轻客 | 2779 |
| 紧凑型SUV | 2532 |
| 微卡 | 2441 |
| 中型SUV | 2236 |

---

### 4.5 变速箱类型分布分析

**业务目标**：统计手动挡、自动挡、双离合等变速箱类型分布。

```sql
SELECT 
  CASE 
    WHEN trans_type IS NULL OR trans_type = '' THEN '未知' 
    ELSE trans_type 
  END AS trans_type,
  COUNT(*) AS cnt
FROM cars
GROUP BY 
  CASE 
    WHEN trans_type IS NULL OR trans_type = '' THEN '未知' 
    ELSE trans_type 
  END
ORDER BY cnt DESC;
```

**返回结果示例**

| trans_type | cnt |
|------------|-----|
| MT | 8385 |
| AT | 5619 |
| 未知 | 5066 |
| DCT | 1958 |
| CVT | 1581 |

---

## 五、自定义查询指南

### 5.1 常用 HiveQL 语法

后端开发者可根据业务需求编写自定义查询，以下为常用语法：

#### 基础查询
```sql
-- 查看表结构
DESCRIBE cars;

-- 查看总记录数
SELECT COUNT(*) FROM cars;

-- 查看前 10 条数据
SELECT * FROM cars LIMIT 10;
```

#### 条件过滤
```sql
-- 查询纯电动车型
SELECT brand, level, price_wan 
FROM cars 
WHERE energy_name = '纯电动'
LIMIT 20;

-- 查询 20-30 万价格区间的 SUV
SELECT brand, level, price_wan, energy_name
FROM cars
WHERE price_wan >= 20 AND price_wan < 30 
  AND level LIKE '%SUV%'
ORDER BY price_wan;
```

#### 聚合统计
```sql
-- 各品牌平均售价
SELECT brand, AVG(price_wan) AS avg_price
FROM cars
GROUP BY brand
ORDER BY avg_price DESC
LIMIT 10;

-- 按年份统计上市车型数量
SELECT launch_year, COUNT(*) AS cnt
FROM cars
WHERE launch_year IS NOT NULL
GROUP BY launch_year
ORDER BY launch_year DESC;
```

#### 多维度交叉分析
```sql
-- 不同能源类型的平均价格
SELECT energy_name, 
       COUNT(*) AS cnt, 
       ROUND(AVG(price_wan), 2) AS avg_price
FROM cars
GROUP BY energy_name
ORDER BY cnt DESC;
```

### 5.2 性能优化建议

1. **避免 `SELECT *`**：明确指定需要的字段，减少数据传输
2. **合理使用 LIMIT**：大结果集加 `LIMIT` 分页返回
3. **WHERE 过滤优先**：先过滤再聚合，减少计算量
4. **字段为空判断**：`IS NULL` 或 `IS NOT NULL`，不要用 `= NULL`

---

## 六、错误处理与注意事项

### 6.1 常见错误

| 错误信息 | 原因 | 解决方案 |
|---------|------|---------|
| `Connection refused` | HiveServer2 未启动或端口未开放 | 检查服务状态：`netstat -tlnp \| grep 10000`；腾讯云安全组需放行 10000 端口 |
| `Java heap space` | 服务器内存不足 | 查询结果集过大或并发过高，联系数据组优化或增加 `LIMIT` |
| `Table not found: cars` | 未连接到 default 数据库 | JDBC URL 确认包含 `/default` |
| `ClassNotFoundException: HiveDriver` | 缺少 Hive JDBC 驱动 | 从数据组获取 `hive-jdbc-standalone.jar` 并加入 classpath |

### 6.2 注意事项

1. **当前无认证**：10000 端口暴露公网无密码保护，仅限内部使用，不要泄露连接地址
2. **查询耗时**：HiveQL 基于 MapReduce，首次查询约 5-15 秒（后续有缓存优化）
3. **数据更新**：当前为静态数据集，如需更新需联系数据组重新上传 CSV 到 HDFS
4. **并发限制**：服务器内存有限，建议后端做好缓存，避免高频实时查询
5. **空值处理**：查询结果可能包含 `NULL`，后端需做防御性判断

---

## 七、测试与验证

### 7.1 快速连通性测试

```bash
# 方法一：beeline 命令行测试
beeline -u "jdbc:hive2://121.4.19.133:10000/default" -n ubuntu \
  -e "SELECT COUNT(*) FROM cars;"

# 预期输出：25022

# 方法二：curl + HiveServer2 HTTP 模式（如已启用）
# （当前配置为 Thrift 二进制模式，暂不支持 HTTP）
```

### 7.2 JDBC 连接测试代码

```java
public class HiveConnectionTest {
    public static void main(String[] args) throws Exception {
        Class.forName("org.apache.hive.jdbc.HiveDriver");
        Connection conn = DriverManager.getConnection(
            "jdbc:hive2://121.4.19.133:10000/default", "ubuntu", "");
        
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT brand, COUNT(*) AS cnt FROM cars GROUP BY brand LIMIT 3");
        
        while (rs.next()) {
            System.out.println(rs.getString("brand") + ": " + rs.getInt("cnt"));
        }
        
        conn.close();
        System.out.println("连接测试成功！");
    }
}
```

---

## 八、联系与支持

- **数据组负责人**：[填写联系方式]
- **问题反馈**：数据查询异常、性能问题、字段疑问请联系数据组
- **服务状态查询**：`jps | grep RunJar`（查看 HiveServer2 进程）

---

*本文档对应大数据综合实训课程作业 —— 数据存储与数据分析模块。*  
*版本：v1.0*  
*更新日期：2026-06-23*
