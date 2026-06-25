# Version 1.1 更新文档

> **版本号**：v1.1.0  
> **更新日期**：2026-06-21  
> **上一版本**：v1.0.0（1.0前后端）  
> **维护人**：lsy  
> **仓库地址**：https://github.com/ZaKO-Z/bigData.git（lsy 分支）

---

## 一、版本概述

v1.1 版本以**数据链路打通**和**可视化图表修复**为核心，完成了从 Hive 数据仓库到前端图表展示的全链路调试，解决了 ECharts 渲染、字段映射、布局溢出等关键问题，使前端可视化系统在对接真实 Hive 数据后能正常运行。

---

## 二、更新内容分类

### 2.1 数据链路与后端部署

| # | 内容 | 说明 |
|---|------|------|
| 1 | Hive 数据仓库接口分析 | 阅读 `Hive数据仓库接口文档.md`，确认 HiveServer2 连接配置（121.4.19.133:10000）与后端 `DataSourceConfig.java` 一致 |
| 2 | 后端 JAR 编译 | 修改 `pom.xml`：Hive JDBC 改为 `system` scope，引用 `lib/` 目录本地 JAR；`spring-boot-maven-plugin` 添加 `includeSystemScope=true` |
| 3 | 后端 JAR 运行 | 成功启动 Spring Boot（端口 8080），Hive 数据源连接状态为 `connected`，51 个 API 接口正常返回数据 |
| 4 | 前端代理切换 | Vite 代理从 `localhost:8088` → `121.4.19.133:8088` → 回退至 `localhost:8088`（用于本地测试） |

### 2.2 前端 API 统一管理

| # | 内容 | 说明 |
|---|------|------|
| 1 | 创建 `src/api.js` | 将 51 个 API 接口按 5 个对象统一管理（`basicApi` / `groupApi` / `sortApi` / `businessApi` / `dataApi`） |
| 2 | 删除旧 `src/api/index.js` | 统一入口，所有 Vue 组件从 `@/api` 导入 |
| 3 | 调用方式标准化 | 组件中按名称调用，如 `basicApi.getBrandRanking()`、`businessApi.energyTrendByYear()` |

### 2.3 ECharts 渲染问题修复

| # | 问题 | 原因 | 修复 |
|---|------|------|------|
| 1 | `echarts.init is not a function` | 本地 `echarts.js` 为 UMD 格式，`import * as echarts` 将导出包裹在 namespace 对象中 | 改用 `import echarts from 'echarts'`（npm 包 ESM 格式） |
| 2 | `does not provide an export named 'default'` | UMD 模块无 default 导出 | 改用 CDN 方式加载 |
| 3 | 最终方案：CDN 加载 | 避免本地环境配置依赖 | `index.html` 添加 `<script src="https://cdn.jsdelivr.net/npm/echarts@5.5.1/dist/echarts.min.js">`，`src/echarts/index.js` 改为 `const echarts = window.echarts` |

### 2.4 图表字段映射修复

#### 2.4.1 基础统计模块（DashboardView.vue）

| 图表 | 原字段 | API 实际字段 | 修复 |
|------|--------|------------|------|
| 品牌车型数量 Top10 | `count` | `cnt` | `cnt ?? count ?? value ?? 0` |
| 价格区间分布 | `count` | `cnt` | `cnt ?? count ?? value ?? 0` |
| 能源类型分布 | `count` | `cnt` | `cnt ?? count ?? value ?? 0` |
| 车型级别分布 | `count` | `cnt` | `cnt ?? count ?? value ?? 0` |

#### 2.4.2 高级分析模块（AdvancedAnalysisView.vue）

| 图表类型 | 原字段 | API 实际字段 | 修复 |
|---------|--------|------------|------|
| 分类图表 value | `count` | `cnt` | `cnt ?? count ?? value ?? 0` |
| 分类图表 name | `name` | `brand` / `level` / `energy_name` 等 | 添加多字段兼容 |

#### 2.4.3 业务关联分析模块（BusinessAnalysisView.vue）

| # | 图表 | 原字段 | API 实际字段 | 修复 |
|---|------|--------|------------|------|
| 1 | 各品牌在各级别中的均价对比 | `avg_price` | `price_avg` | `price_avg ?? avg_price ?? avg ?? price ?? 0` |
| 2 | 指定级别下各品牌价格分布（箱线图） | `min_price` / `max_price` / `avg_price` | `price_min` / `price_max` / `price_avg` | 全部修正 |
| 3 | 各级别品牌数量与价格跨度 | `max_price` / `min_price` | `price_max` / `price_min` | 全部修正 |
| 4 | 按年份统计新能源占比（面积图） | `ratio` | `new_energy_pct` | `new_energy_pct ?? ratio ?? percentage ?? 0` |
| 5 | 各品牌新能源车型占比 | `ratio` | `new_energy_pct` | `new_energy_pct ?? ratio ?? percentage ?? 0` |
| 6 | 各排量段马力分布（箱线图） | `min_hp` / `max_hp` / `avg_hp` | `hp_min` / `hp_max` / `hp_avg` | 全部修正 |
| 7 | 排量段名称 | `displacement` | `displacement_range` | `displacement_range ?? displacement ?? range` |
| 8 | 各变速箱类型平均排量与马力 | `avg_disp` / `avg_hp` | `disp_avg` / `hp_avg` | 全部修正 |
| 9 | 变速箱×电动/非电动 | `is_electric` | `power_type` | 添加 `power_type === '电动'` 兼容 |
| 10 | 排量×燃油标号分布（热力图） | `count` | `cnt` | `k === 'cnt' \|\| includes('count')` |
| 11 | 排量×燃油×马力详细交叉（热力图） | `count` | `cnt` | 同上 |
| 12 | 指定级别下车身×座位交叉（热力图） | `count` | `cnt` | 同上 |
| 13 | 排量×变速箱分布（热力图） | `count` | `cnt` | 同上 |

### 2.5 图表布局与显示修复

| # | 问题 | 原因 | 修复 |
|---|------|------|------|
| 1 | 业务关联分析图表横向坐标被压缩，图表挤在容器最左侧 | `v-show` 隐藏的 tab 中图表初始化时容器宽度为 0 | ① `BaseChart.vue` CSS 添加 `width: 100% !important`；② `BusinessAnalysisView.vue` 添加 `watch(activeTab)` 触发 `window resize` 事件 |
| 2 | 箱线图和热力图初始无数据 | `blpLevel` 和 `scLevel` 初始值为 `'SUV'`，但数据库中级别名为"紧凑型SUV"等，不匹配 | `loadLevelOptions` 加载级别列表后自动设置第一个有效级别，并立即加载对应数据 |
| 3 | 数据管理页面加载后内容变成约 2 倍宽度溢出 | CSS Grid 子项默认 `min-width: auto`，22 列数据表格撑开布局 | 给 `.data-mgmt__left` 和 `.data-mgmt__right` 添加 `min-width: 0` |

### 2.6 文档新增

| # | 文件名 | 内容 |
|---|--------|------|
| 1 | `crawler.md` | 爬虫架构与数据采集清洗文档（13 章节） |
| 2 | `README.md` | 项目总览文档 |
| 3 | `业务需求.md` | 8 个数据分析目标及业务场景 |
| 4 | `Version1.1.md` | 本文档（版本更新记录） |

### 2.7 数据对应性分析

| 分析项 | 结论 |
|--------|------|
| 爬虫数据 vs `data/` 文件夹 | ❌ 不对应（汽车之家新车参数 vs 懂车帝二手车源） |
| 爬虫数据 vs 后端项目 | ✅ 完全对应（22 个字段 100% 匹配，25,021 行数据一致） |
| Hive 数据仓库 vs 后端配置 | ✅ 完全一致（JDBC 连接参数、表结构、字段类型均匹配） |

---

## 三、修改文件清单

### 3.1 后端文件

| 文件 | 修改内容 |
|------|---------|
| `back/.../pom.xml` | Hive JDBC / Hadoop Common 改为 `system` scope；`spring-boot-maven-plugin` 添加 `includeSystemScope=true` |

### 3.2 前端文件

| 文件 | 修改内容 |
|------|---------|
| `Visualization/index.html` | 添加 ECharts CDN script 标签 |
| `Visualization/vite.config.js` | 代理 target 切换（`localhost:8088` ↔ `121.4.19.133:8088`） |
| `Visualization/src/api.js` | 新建：51 个 API 统一管理 |
| `Visualization/src/echarts/index.js` | ECharts 导入方式改为 `window.echarts`（CDN） |
| `Visualization/src/components/BaseChart.vue` | CSS 添加 `width: 100% !important` |
| `Visualization/src/views/DashboardView.vue` | 4 个图表 value 字段添加 `cnt` 兼容 |
| `Visualization/src/views/AdvancedAnalysisView.vue` | 分类图表 value/name 字段兼容修复 |
| `Visualization/src/views/BusinessAnalysisView.vue` | 12 处字段映射修复 + 4 处热力图 `cnt` 检测 + 图表宽度修复 + 初始级别选择修复 |
| `Visualization/src/views/DataManagementView.vue` | Grid 子项添加 `min-width: 0` 修复布局溢出 |

### 3.3 文档文件

| 文件 | 说明 |
|------|------|
| `crawler.md` | 爬虫架构文档 |
| `README.md` | 项目总览 |
| `业务需求.md` | 业务需求分析 |
| `Version1.1.md` | 本文档 |

---

## 四、已知问题

| # | 问题 | 状态 | 说明 |
|---|------|------|------|
| 1 | `power-match/displacement-fuel` API 返回 500 | ✅ 已修复 | 修复 SQL 中 `fuel_grade` 类型比较，使用 `CAST(fuel_grade AS INT)` 兼容 Hive STRING 类型 |
| 2 | `power-match/detail` API 返回 500 | ✅ 已修复 | 同上，修复 `fuel_grade` 类型比较问题 |
| 3 | ECharts 依赖 CDN 网络 | 🟡 设计取舍 | 若部署环境无外网，需改回本地 npm 包方式 |

---

## 五、验证结果

| 验证项 | 状态 |
|--------|------|
| 后端 JAR 启动 | ✅ 端口 8080 正常 |
| Hive 数据源连接 | ✅ connected |
| 基础统计 4 个图表 | ✅ 正常显示数据 |
| 高级分析-分类 13 个图表 | ✅ 正常显示数据 |
| 高级分析-排序 9 个图表 | ✅ 正常显示数据 |
| 业务关联分析 17 个图表 | ✅ 全部正常显示数据 |
| 数据管理页面 | ✅ 布局正常，无溢出 |
| 图表容器宽度 | ✅ 占满容器，无压缩 |
| 初始级别选择 | ✅ 自动选中第一个级别 |

---

## 六、版本对比

| 对比项 | v1.0 | v1.1 |
|--------|------|------|
| 数据源 | H2 内存数据库（本地 CSV） | Hive 数据仓库（远程 121.4.19.133:10000） |
| ECharts 加载方式 | npm 包 import | CDN script 标签 |
| API 管理 | `src/api/index.js`（分散） | `src/api.js`（统一） |
| 字段映射 | 部分不匹配 | 全部修复（兼容 `cnt` / `price_avg` / `new_energy_pct` 等） |
| 图表宽度 | 部分压缩 | 修复（CSS + resize 触发） |
| 初始级别选择 | 无默认值 | 自动选中第一个有效级别 |
| 数据管理布局 | 溢出 | 修复（`min-width: 0`） |
| 文档 | back.md / front.md | 新增 crawler.md / README.md / 业务需求.md / Version1.1.md |

---

## 七、后续计划

| 优先级 | 任务 | 说明 |
|--------|------|------|
| 高 | ~~修复后端 500 错误~~ | ✅ 已修复：`displacement-fuel` 和 `detail` 两个 API 的 SQL 查询 |
| 中 | 前端代理切回远程服务器 | 测试完成后将 `vite.config.js` 改回 `121.4.19.133:8088` |
| 中 | 服务器端口放行 | 确认云服务器安全组放行 8088 端口（TCP） |
| 低 | ECharts 离线部署 | 若需离线环境，将 CDN 改回本地 npm 包 |
| 低 | 性能优化 | Hive 查询较慢，可考虑添加缓存或预计算 |
