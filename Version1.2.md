# Version 1.2 — 问题修复与文档同步

> **版本**：v1.2.0
> **日期**：2026-06-25
> **范围**：修复项目检查中发现的所有代码缺陷与文档不一致问题
> **前置版本**：[Version1.1](Version1.1.md)

---

## 一、修复概览

| 类别 | 修复数 | 优先级 |
|------|--------|--------|
| 代码缺陷（功能性） | 7 | 高 |
| 文件创建/结构清理 | 2 | 中 |
| 文档同步 | 7 个文件 | 高 |

---

## 二、代码修复详情

### 2.1 数据库连接配置化 — `DataSourceConfig.java`

**问题**：Hive 连接参数（URL、用户名、密码）全部硬编码在 Java 源码中，`application.properties` 中的 `hive.url`、`hive.user`、`hive.password` 配置项从未被代码读取，属于"死配置"。

**影响**：更换 Hive 服务器地址时，修改 `application.properties` 无效，必须改源码重新编译。

**修复方案**：
- 为 `DataSourceConfig` 添加 `@Component` 注解，纳入 Spring 容器管理
- 使用 `@Value("${hive.url:jdbc:hive2://121.4.19.133:10000/default}")` 注入配置
- 通过 `@PostConstruct` 将实例字段拷贝到静态字段，保持静态方法兼容
- 保留默认值作为兜底，确保即使配置缺失也能运行

**文件**：`back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/DataSourceConfig.java`

**修改前后对比**：

```java
// 修改前（硬编码）
private static boolean tryHive() {
    String url = "jdbc:hive2://121.4.19.133:10000/default";
    String user = "ubuntu";
    ...
}

// 修改后（配置化）
@Value("${hive.url:jdbc:hive2://121.4.19.133:10000/default}")
private String hiveUrl;

@Value("${hive.user:ubuntu}")
private String hiveUser;

@Value("${hive.password:}")
private String hivePassword;

@PostConstruct
public void init() {
    staticHiveUrl = hiveUrl;
    staticHiveUser = hiveUser;
    staticHivePassword = hivePassword;
    log.info("Hive 配置加载完成: url={}, user={}", staticHiveUrl, staticHiveUser);
}
```

---

### 2.2 修复两个业务接口 500 错误 — `BusinessAnalysisService.java`

**问题**：`power-match/displacement-fuel` 和 `power-match/detail` 两个 API 返回 500 错误。

**根因**：Hive 表中 `fuel_grade` 字段为 **STRING** 类型（CSV 文本加载），但 SQL 中使用 `fuel_grade = 0`、`fuel_grade > 0` 进行数值比较，导致 Hive SQL 执行异常。

**修复方案**：所有 `fuel_grade` 数值比较处使用 `CAST(fuel_grade AS INT)` 显式类型转换，兼容 STRING（Hive）和 INT（H2）两种数据源。

**涉及方法**：

| 方法 | 接口路径 | 修改内容 |
|------|---------|---------|
| `displacementFuelMatch()` | `/api/cars/biz/power-match/displacement-fuel` | `fuel_grade = 0` → `CAST(fuel_grade AS INT) = 0`<br>`fuel_grade > 0` → `CAST(fuel_grade AS INT) > 0` |
| `displacementFuelHorsepowerDetail()` | `/api/cars/biz/power-match/detail` | 同上 |

**文件**：`back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/BusinessAnalysisService.java`

---

### 2.3 修复 Hive OFFSET 兼容性问题 — `CarsService.java`

**问题**：`search()` 方法使用 `LIMIT ... OFFSET ...` 语法，Hive 不支持 OFFSET，导致车辆搜索分页功能在 Hive 数据源下报错。

**修复方案**：改为 `LIMIT (limit + offset)` 多取数据，在 Java 层使用 `subList` 跳过前 `offset` 条记录。此方案兼容 H2 和 Hive。

```java
// 修改前
sql.append(" LIMIT ").append(limit).append(" OFFSET ").append(offset);
return query(sql.toString());

// 修改后
int fetchSize = limit + offset;
sql.append(" LIMIT ").append(fetchSize);
List<Map<String, Object>> rows = query(sql.toString());
if (offset > 0 && rows.size() > offset) {
    return new ArrayList<>(rows.subList(offset, rows.size()));
}
return offset > 0 ? new ArrayList<>() : rows;
```

**文件**：`back/bigwork/bigwork/cars-analysis/src/main/java/com/example/cars/CarsService.java`

---

### 2.4 前端版本号修正 — `App.vue`

**问题**：侧边栏底部显示 `v1.0.0`，实际项目已迭代至 v1.1。

**修复**：`v1.0.0` → `v1.1.0`

**文件**：`Visualization/src/App.vue`

---

### 2.5 ECharts CDN 版本固定 — `index.html`

**问题**：CDN 引用 `echarts@5`（指向最新 5.x），Version1.1.md 记录应为固定 `echarts@5.5.1`，存在版本漂移风险。

**修复**：`echarts@5` → `echarts@5.5.1`

**文件**：`Visualization/index.html`

---

### 2.6 修复"仅解析预览"不显示数据 — `DataManagementView.vue`

**问题**：在数据管理页面选择 CSV 文件后点击"仅解析预览"，提示"文件解析成功，已返回预览数据"，但上传卡片下方不显示任何预览表格内容。

**根因**：后端 `/data/upload` 返回 `{ success, filename, headers, rowCount, preview }`，但前端 `handleUpload` 只检查 `res.rows` 和 `res.data`，从未读取 `res.preview`，导致预览数据被静默丢弃。

**修复**：
1. 新增 `uploadPreviewData`、`uploadPreviewHeaders` 状态，与数据库预览 `previewData` 分离
2. 新增 `uploadPreviewColumns` 计算属性，优先使用后端返回的 `headers`，兜底从行数据提取列名
3. `handleUpload` 增加提取 `res.preview` 和 `res.headers` 的逻辑
4. 模板中在上传卡片内新增"解析预览"表格，展示前 20 行数据
5. `clearFile` 移除文件时同步清空预览状态
6. 修正 `handleImport` 消息字段：`res.imported` → `res.importedCount`

**文件**：`Visualization/src/views/DataManagementView.vue`

---

### 2.7 修复报表生成超时 — `api.js` + `DataManagementView.vue`

**问题**：点击"生成报表(JSON)"返回"报表生成失败：timeout of 30000ms exceeded"。

**根因**：`ReportService.generateFullReport()` 串行执行 29 个 Hive SQL 查询（基础统计 4 + 分类维度 13 + 业务关联 12），总计需要 30-60 秒，超过 axios 默认 30 秒超时。

**修复**：
- `api.js`：`generateReport` 增加 `{ timeout: 120000 }`（2 分钟）
- `DataManagementView.vue`：按钮加载提示更新为"生成中（请稍候，约30-60秒）..."

**文件**：`Visualization/src/api.js`、`Visualization/src/views/DataManagementView.vue`

---

### 2.8 修复 saveReport POST 超时配置失效 — `api.js`

**问题**：点击"生成并保存CSV"仍然返回"保存失败：timeout of 30000ms exceeded"，尽管 2.7 已添加了超时配置。

**根因**：axios 的 `post()` 方法签名为 `post(url[, data[, config]])`，第二参数是请求体而非配置。原代码 `request.post('/data/report/save', { timeout: 120000 })` 将 `{ timeout: 120000 }` 当作请求体发送了，超时配置未生效。

**修复**：

```javascript
// 修改前 — timeout 被当作 POST body
saveReport: () => request.post('/data/report/save', { timeout: 120000 }),

// 修改后 — null 为请求体，第三参数为配置
saveReport: () => request.post('/data/report/save', null, { timeout: 120000 }),
```

**文件**：`Visualization/src/api.js`

---

### 2.9 新增报表下载功能 + 修复报表列表加载 — `DataManagementView.vue`

**问题**：
1. JSON 报表生成后只显示截断预览，提示"更多内容请下载查看"但没有下载入口
2. CSV 保存后文件名提取错误（后端返回 `filePath`，前端读 `filename`/`file`）
3. "已保存的报表列表"加载不出来（后端返回 `res.reports`，前端只检查 `res.files`/`res.data`）

**修复**：
1. 新增 `downloadJsonReport()` — 前端直接将 `reportData` 生成 Blob 下载为 `.json` 文件
2. 新增 `savedCsvFilename` 状态 — 从 `res.filePath` 用 `split(/[\\/]/).pop()` 提取文件名
3. 模板新增两个下载按钮：
   - JSON 预览标题栏右侧："下载完整 JSON"
   - CSV 保存成功消息旁："下载 CSV"
4. `loadReportList` 补充 `res.reports` 字段检查
5. `generateReport` 提取 `res.report` 作为报表数据（而非整个响应对象）

**文件**：`Visualization/src/views/DataManagementView.vue`

---

## 三、文件创建与结构清理

### 3.1 创建 `.env.example`

**问题**：README.md 和 crawler.md 均指导用户 `cp .env.example .env`，但该文件不存在。

**修复**：创建 `bigData-main/scawler_carhome/.env.example`，包含 Cookie 配置、User-Agent、并发数、超时等模板项。

**文件**：`bigData-main/scawler_carhome/.env.example`

---

### 3.2 删除重复爬虫模块

**问题**：爬虫代码存在两份完全相同的副本：
- `bigData-main/scawler_carhome/`（根目录，文档记录的正式位置）
- `back/bigwork/bigwork/cars-analysis/bigData-main/scawler_carhome/`（后端内嵌，无文档记录）

**风险**：修改一处另一处不同步，维护混乱。

**修复**：删除后端内嵌的 `back/bigwork/bigwork/cars-analysis/bigData-main/` 整个目录，保留根目录的正式版本。已确认 `pom.xml` 未引用此目录。

---

## 四、文档同步详情

### 4.1 端口修正：8080 → 8088

**问题**：所有文档写后端端口为 8080，实际 `application.properties` 配置为 8088，前端 vite 代理也指向 8088。代码自洽但文档全部错误。

**涉及文件**：README.md、back.md、front.md、Version1.1.md、项目文档.md、crawler.md

### 4.2 API 数量修正：51 → 57

**问题**：v1.1 后新增了 6 个接口（overview、search、brands、levels、compare/brands、compare/levels），但所有文档仍写"51 个 API"。

**涉及文件**：README.md、back.md、front.md、Version1.1.md、crawler.md

### 4.3 新增功能文档化

**问题**：前端新增 SearchView（车辆搜索）和 CompareView（对比分析）两个页面，App.vue 导航 6 项，路由 6 条，但文档只记录 4 个模块。

**修复**：在 README.md、front.md、back.md、项目文档.md 中补充完整的页面描述、路由配置、接口说明。

### 4.4 Hive 字段类型统一

**问题**：三份文档对 `fuel_grade`、`energy_code`、`is_electric` 三个字段的类型定义矛盾：

| 字段 | Hive接口文档 | 项目文档 (CREATE TABLE) | back.md |
|------|-------------|------------------------|---------|
| fuel_grade | STRING | INT | INT |
| energy_code | STRING | INT | INT |
| is_electric | STRING | INT | INT |

**修复**：统一为 STRING（与 Hive 实际表结构一致），在 Hive 接口文档中补充字段类型差异说明，注明后端 SQL 已通过 `CAST(... AS INT)` 兼容处理。

### 4.5 前端 API 文件路径修正

**问题**：front.md 引用 `src/api/index.js`（v1.1 已删除，替换为 `src/api.js`）。

**修复**：所有引用更新为 `src/api.js`。

### 4.6 Version1.1.md 已知问题状态更新

**修复**：将 `power-match/displacement-fuel` 和 `power-match/detail` 两个 500 错误标记为 `✅ 已修复`，验证结果更新为"全部正常显示数据"。

### 4.7 文档变更清单

| 文件 | 变更项 |
|------|--------|
| README.md | 端口、API 数量、新增模块/接口、字段类型、目录结构 |
| back.md | 端口、基础统计接口 5→11、合计 51→57、字段类型、文件路径链接 |
| front.md | api.js 路径、新增 SearchView/CompareView 文档、路由/导航更新、端口、API 数量 |
| Version1.1.md | 端口、已知问题标记已修复、验证结果更新 |
| 项目文档.md | 端口、Hive 建表 SQL 字段类型、新增接口、删除重复爬虫模块描述 |
| Hive数据仓库接口文档.md | 新增字段类型差异说明、端口备注 |
| crawler.md | API 数量修正 |

---

## 五、未修改项

| 项目 | 原因 |
|------|------|
| `bigDataPage/` 目录 | 早期前端原型（ECharts 6），保留不动，不影响主项目 |
| `data/` 目录 | 懂车帝二手车数据，README 已标注与爬虫数据不对应 |
| SQL 注入防护 | `search()` 等方法使用字符串拼接 + 单引号转义，已有基础防护。如需进一步加固为 `PreparedStatement` 需较大重构，暂不在本次范围 |
| Version1.1.md 历史记录中的 "51" | 版本对比表中 v1.0 有 51 个 API 是历史事实，保留不动 |

---

## 六、验证清单

### 6.1 后端验证

```bash
cd back/bigwork/bigwork/cars-analysis
mvn clean package
java -jar target/cars-analysis-1.0.0.jar
```

启动日志应包含：
```
Hive 配置加载完成: url=jdbc:hive2://121.4.19.133:10000/default, user=ubuntu
数据源: Hive 数据仓库
```

### 6.2 接口验证

| 接口 | 预期结果 |
|------|---------|
| `GET /api/cars/biz/power-match/displacement-fuel` | 返回排量-燃油匹配数据（不再 500） |
| `GET /api/cars/biz/power-match/detail` | 返回排量-燃油-马力详情（不再 500） |
| `GET /api/cars/search?limit=10&offset=10` | 返回第 2 页数据（Hive 分页正常） |
| `GET /api/cars/overview` | 返回仪表盘概览数据 |
| `GET /api/cars/compare/brands?brand1=...&brand2=...` | 返回品牌对比数据 |

### 6.3 前端验证

```bash
cd Visualization
npm install
npm run dev
```

| 检查项 | 预期 |
|--------|------|
| 侧边栏版本号 | 显示 v1.1.0 |
| 导航项数量 | 6 项（含车辆搜索、对比分析） |
| ECharts 版本 | 控制台显示 5.5.1 |
| 车辆搜索页 | 分页切换正常 |
| 业务关联分析 | 17 个图表全部显示数据 |

---

## 七、文件变更统计

| 类型 | 文件数 |
|------|--------|
| Java 源码 | 3 |
| 前端源码 | 3（App.vue、index.html、DataManagementView.vue）+ api.js |
| 文档 | 7 |
| 新建文件 | 1（.env.example） |
| 删除目录 | 1（重复爬虫模块） |
| **合计** | **16 项变更** |
