# Visualization 前端可视化项目文档

> 本文档详细记录了汽车大数据分析平台前端可视化模块的代码结构、功能说明与维护指引，便于后续开发与维护。

---

## 一、项目概述

`Visualization` 是基于 **Vue 3 + Vite + ECharts** 构建的汽车大数据分析平台前端可视化项目，对接后端 Spring Boot 服务（共 51 个 REST API 接口），提供基础统计、高级分析、业务关联分析、数据管理四大模块的完整可视化能力。

- **前端端口**：`http://localhost:5173`
- **后端代理**：通过 Vite 代理转发至 `http://localhost:8088`
- **数据来源**：后端支持 Hive 数据仓库 / H2 内存数据库（自动降级）

---

## 二、技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Vue | ^3.5.38 | 渐进式 JavaScript 框架（Composition API + `<script setup>`） |
| Vue Router | ^5.1.0 | 官方路由管理器 |
| Vite | ^8.0.16 | 下一代前端构建工具 |
| ECharts | ^5.5.1 | 数据可视化图表库 |
| Axios | ^1.7.9 | HTTP 请求库 |
| @vitejs/plugin-vue | ^6.0.7 | Vite 的 Vue 插件 |
| vite-plugin-vue-devtools | ^8.1.2 | Vue 开发者工具 |

---

## 三、目录结构

```
Visualization/
├── public/
│   └── favicon.ico
├── src/
│   ├── api.js                    # API 接口统一管理（57个接口）
│   ├── assets/
│   │   ├── base.css                 # 基础样式变量
│   │   ├── main.css                 # 全局样式
│   │   └── logo.svg
│   ├── components/
│   │   ├── BaseChart.vue            # ECharts 通用图表组件
│   │   ├── ChartCard.vue            # 图表卡片容器（含 loading/error/empty）
│   │   └── icons/                   # 图标组件（脚手架自带）
│   ├── router/
│   │   └── index.js                 # 路由配置
│   ├── views/
│   │   ├── DashboardView.vue        # 基础统计仪表盘
│   │   ├── AdvancedAnalysisView.vue # 高级分析（分类维度+排序度量）
│   │   ├── BusinessAnalysisView.vue # 业务关联分析（5大业务维度）
│   │   └── DataManagementView.vue   # 数据管理（文件上传+报表）
│   ├── App.vue                      # 主布局（侧边栏+顶栏+内容区）
│   └── main.js                      # 应用入口
├── .gitignore
├── index.html
├── jsconfig.json
├── package.json
├── README.md
└── vite.config.js                   # Vite 配置（含 API 代理）
```

---

## 四、核心配置说明

### 4.1 Vite 配置（`vite.config.js`）

```js
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8088',
      changeOrigin: true,
    },
  },
}
```

**作用**：将前端所有 `/api/*` 请求代理到后端 Spring Boot 服务（8088端口），解决开发环境跨域问题。

### 4.2 路径别名

`@` 指向 `./src`，例如 `import request from '@/utils/request'`。

### 4.3 全局样式（`main.css`）

- 移除了 Vue 脚手架默认的 `max-width: 1280px` 限制，适配全屏布局
- 自定义滚动条样式
- 统一表格、按钮的基础样式

---

## 五、API 接口封装层

### 5.1 Axios 实例（`src/utils/request.js`）

- `baseURL`: `/api`
- `timeout`: 30 秒
- **请求拦截器**：预留 token 注入位置
- **响应拦截器**：自动解包 `response.data`，统一错误日志输出

### 5.2 接口模块（`src/api.js`）

按后端模块划分为 5 个 API 对象：

| 模块对象 | 对应后端 | 接口数量 | 说明 |
|---------|---------|---------|------|
| `basicApi` | `/api/cars/` | 11 | 数据源状态、品牌排名、价格分布、能源分布、级别分布、概览、搜索、品牌列表、级别列表、品牌对比、级别对比 |
| `groupApi` | `/api/cars/advanced/group/` | 13 | 13 个分类维度查询 |
| `sortApi` | `/api/cars/advanced/sort/` | 9 | 9 个度量排序查询（支持 order/limit 参数） |
| `businessApi` | `/api/cars/biz/` | 17 | 5 大业务关联分析维度 |
| `dataApi` | `/api/data/` | 7 | 文件上传、导入、预览、报表生成/保存/列表/下载 |

**调用示例**：
```js
import { basicApi, sortApi } from '@/api'

// 获取品牌排名
const data = await basicApi.getBrandRanking()

// 按价格降序取前 10
const list = await sortApi.getByMetric('price', { order: 'desc', limit: 10 })
```

---

## 六、可复用组件说明

### 6.1 `BaseChart.vue` — ECharts 通用图表组件

**Props**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `option` | Object | - | ECharts 配置项（必填） |
| `height` | String | `'400px'` | 图表高度 |
| `width` | String | `'100%'` | 图表宽度 |
| `theme` | String | `''` | ECharts 主题 |

**特性**：
- 自动在 `onMounted` 初始化图表
- `watch` 监听 `option` 深度变化，自动 `setOption`
- 监听 `window.resize` 自动调整图表尺寸
- `onBeforeUnmount` 自动销毁实例，避免内存泄漏
- 通过 `defineExpose` 暴露 `getInstance` 和 `resize` 方法

### 6.2 `ChartCard.vue` — 图表卡片容器

**Props**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `title` | String | `''` | 卡片标题 |
| `loading` | Boolean | `false` | 加载中状态（显示 spinner） |
| `error` | String | `''` | 错误信息（显示红色提示） |
| `empty` | Boolean | `false` | 空数据状态 |

**插槽**：
- `default`：卡片主体内容
- `title`：自定义标题
- `extra`：标题右侧扩展区域（如下拉选择器）

**特性**：统一卡片视觉风格（圆角、阴影、hover 效果），统一 loading/error/empty 三态展示。

---

## 七、页面功能详解

### 7.1 基础统计仪表盘 — `DashboardView.vue`

**路由**：`/dashboard`（默认首页）

**对应接口**：`basicApi` 的 5 个接口

**功能模块**：

1. **顶部状态卡片**（4 张）
   - 数据源状态（Hive/H2/异常）
   - 品牌数量统计
   - 价格区间数量
   - 能源类型数量

2. **图表区域**（4 个图表）
   - 品牌车型数量 Top 10：横向柱状图（渐变色）
   - 价格区间分布：环形饼图
   - 能源类型分布：玫瑰图
   - 车型级别分布：纵向柱状图（渐变色）

**维护要点**：
- 所有数据在 `onMounted` 时并行加载
- 每个图表独立管理 `loading` 和 `error` 状态
- 通过 `defineExpose` 暴露 `refresh` 方法供外部刷新

### 7.2 高级分析 — `AdvancedAnalysisView.vue`

**路由**：`/advanced`

**对应接口**：`groupApi`（13个）+ `sortApi`（9个）

#### 7.2.1 分类维度分析

- **13 个维度 Tab 切换**：品牌、级别、能源类型、车身结构、变速箱大类/亚型、燃油标号、发动机布局、质保里程不限、上市年份、价格区间、座位数、档位数
- **图表类型切换**：柱状图 / 饼图 / 漏斗图
- **显示数量控制**：Top 10 / Top 20 / 全部

#### 7.2.2 排序度量分析

- **9 种度量**：上市时间、厂商指导价、发动机排量、最大马力、门数、座位数、档位数、质保年限、质保里程
- **排序方式**：降序（高→低）/ 升序（低→高）
- **显示数量**：Top 5 / 10 / 20 / 50
- **双视图展示**：
  - 左侧：横向柱状图（带 tooltip 显示完整明细）
  - 右侧：明细数据表（动态列、排名序号）

**维护要点**：
- 切换维度 Tab 自动重新请求数据
- 排序参数变更后需点击「查询」按钮触发（避免频繁请求）
- 表格列动态生成：取所有数据行的字段并集
- `formatValue` 函数统一处理数字格式化（保留 2 位小数）

### 7.3 业务关联分析 — `BusinessAnalysisView.vue`

**路由**：`/business`

**对应接口**：`businessApi`（17个）

**5 大业务维度 Tab**：

#### Tab 1：品牌 × 级别 × 指导价
- 各品牌在各级别中的价格区间（堆叠柱状图）
- 各级别品牌数量与价格跨度总览（柱状图+折线图双轴）
- 指定级别下各品牌定价对比（最低/最高/均价三系列）

#### Tab 2：上市时间 × 能源类型
- 按年份统计各能源类型新车数量（多线折线图，带面积）
- 按年份统计新能源占比（单线面积图）
- 按季度统计各能源类型新车数量（堆叠柱状图）
- 各品牌新能源车型占比（横向柱状图 Top 15）

#### Tab 3：排量 × 燃油标号 × 马力
- 各排量段的主流燃油标号分布（堆叠柱状图）
- 各排量段的平均/最大/最小马力（多线折线图）
- 排量×燃油标号×马力详细交叉表（数据表格）

#### Tab 4：车身结构 × 座位数 × 级别
- 各级别座位数分布（堆叠柱状图）
- 各车身结构座位数分布（堆叠柱状图）
- 各级别 7 座车型占比（环形饼图）
- 指定级别下车身结构与座位数交叉分布（堆叠柱状图）

#### Tab 5：变速箱 × 发动机配置
- 变速箱×发动机搭配频次（柱状图 Top 20）
- 各排量段主流变速箱类型分布（堆叠柱状图）
- 各变速箱类型平均排量与马力（柱状图+折线图双轴）
- 变速箱×是否电动搭配统计（堆叠柱状图）

**维护要点**：
- 使用 `fetchData` 通用函数统一处理加载/错误/数据存储
- `setData` 函数兼容多种返回结构（数组、`{data}`）
- `onMounted` 时一次性加载所有 17 个接口数据（并行）
- 级别下拉选项从 `basicApi.getLevelDistribution` 动态获取

### 7.4 数据管理 — `DataManagementView.vue`

**路由**：`/data`

**对应接口**：`dataApi`（7个）

**功能模块**：

#### 7.4.1 文件上传区
- **拖拽上传**：支持点击选择和拖拽文件到上传区域
- **文件类型**：CSV / Excel（.xlsx / .xls），最大 100MB
- **两种操作模式**：
  - 仅解析预览：调用 `/api/data/upload`，返回前 20 行预览
  - 上传并导入：调用 `/api/data/import`，写入数据库
- **上传进度**：实时显示百分比
- **状态反馈**：成功/失败消息提示

#### 7.4.2 数据库数据预览
- **表切换**：cars 表 / generic_data 表
- **行数控制**：20 / 50 / 100 行
- **动态表格**：根据返回数据自动生成列

#### 7.4.3 报表管理
- **生成报表（JSON）**：调用 `/api/data/report/generate`，预览前 2000 字符
- **生成并保存 CSV**：调用 `/api/data/report/save`，保存到后端文件系统
- **报表列表**：展示已保存的报表文件名、大小、修改时间
- **下载报表**：通过 Blob 方式触发浏览器下载

**维护要点**：
- 文件上传使用 `FormData`，需设置 `Content-Type: multipart/form-data`
- 下载报表需设置 `responseType: 'blob'`
- 上传进度通过 `onUploadProgress` 回调计算
- 初始化时自动加载数据预览和报表列表

### 7.5 车辆搜索 — `SearchView.vue`

**路由**：`/search`

**对应接口**：`basicApi` 的 search、brands、levels 接口

**功能模块**：
- 多条件筛选：品牌、级别、价格区间、能源类型、变速箱类型
- 品牌和级别下拉选项从 API 动态获取
- 分页展示：支持翻页浏览搜索结果
- 动态表格：根据返回数据自动生成列

### 7.6 对比分析 — `CompareView.vue`

**路由**：`/compare`

**对应接口**：`basicApi` 的 compare/brands、compare/levels 接口

**功能模块**：
- 品牌对比：选择两个品牌对比各项指标（车型数、均价、排量、马力等）
- 级别对比：选择两个级别对比各项指标
- 可视化对比：分组柱状图展示差异
- 详细数据表格

---

## 八、路由配置（`src/router/index.js`）

| 路径 | 名称 | 组件 | 说明 |
|------|------|------|------|
| `/` | - | - | 重定向至 `/dashboard` |
| `/dashboard` | dashboard | DashboardView | 基础统计（同步加载） |
| `/advanced` | advanced | AdvancedAnalysisView | 高级分析（懒加载） |
| `/business` | business | BusinessAnalysisView | 业务关联分析（懒加载） |
| `/search` | search | SearchView | 车辆搜索（懒加载） |
| `/compare` | compare | CompareView | 对比分析（懒加载） |
| `/data` | data | DataManagementView | 数据管理（懒加载） |

**特性**：除首页外均采用路由懒加载（`() => import(...)`），减小首屏体积。

---

## 九、主布局（`App.vue`）

### 9.1 布局结构

```
┌─────────────────────────────────────────────┐
│  侧边栏 (220px)  │       主内容区            │
│                  ├─────────────────────────┤
│  🚗 汽车大数据    │   顶部栏（标题+标签）    │
│     分析平台      ├─────────────────────────┤
│                  │                         │
│  📊 基础统计      │                         │
│  🔍 高级分析      │     <RouterView />      │
│  🔗 业务关联      │     （页面内容区）       │
│  🔎 车辆搜索      │                         │
│  ⚖️ 对比分析      │                         │
│  📁 数据管理      │                         │
│                  │                         │
│  v1.0.0          │                         │
└─────────────────────────────────────────────┘
```

### 9.2 响应式适配

- **桌面端**：侧边栏 220px，显示完整文字
- **移动端**（≤768px）：侧边栏收起为 64px，仅显示图标

### 9.3 导航项配置

```js
const navItems = [
  { path: '/dashboard', label: '基础统计', icon: '📊' },
  { path: '/advanced', label: '高级分析', icon: '🔍' },
  { path: '/business', label: '业务关联', icon: '🔗' },
  { path: '/search', label: '车辆搜索', icon: '🔎' },
  { path: '/compare', label: '对比分析', icon: '⚖️' },
  { path: '/data', label: '数据管理', icon: '📁' },
]
```

---

## 十、运行与构建

### 10.1 环境要求

- Node.js `^22.18.0` 或 `>=24.12.0`

### 10.2 开发模式

```bash
cd Visualization
npm install      # 安装依赖
npm run dev      # 启动开发服务器 http://localhost:5173
```

### 10.3 生产构建

```bash
npm run build    # 构建产物输出至 dist/
npm run preview  # 本地预览生产构建
```

### 10.4 与后端联调

1. 启动后端 Spring Boot 服务（端口 8088）
2. 启动前端开发服务器（端口 5173）
3. 前端通过 Vite 代理自动转发 `/api/*` 请求至后端
4. 浏览器访问 `http://localhost:5173` 即可

---

## 十一、维护指引

### 11.1 新增图表

1. 在对应 `views/*.vue` 文件中引入 `BaseChart` 和 `ChartCard`
2. 编写 ECharts `option` 计算属性（`computed`）
3. 在 `onMounted` 中调用对应 API 加载数据
4. 管理 `loading` 和 `error` 状态

### 11.2 新增 API 接口

1. 在 `src/api.js` 对应模块对象中添加方法
2. 遵循现有命名规范（如 `getXxx`、`byYyy`）
3. 对于带参数接口，使用 `params` 对象传递

### 11.3 新增页面

1. 在 `src/views/` 下创建 `XxxView.vue`
2. 在 `src/router/index.js` 添加路由配置（建议使用懒加载）
3. 在 `src/App.vue` 的 `navItems` 数组中添加导航项

### 11.4 修改主题色

- **图表配色**：在各 `views/*.vue` 文件中搜索 `colorPalette` 数组修改
- **界面主色**：在 `App.vue` 中搜索 `#5470c6`（蓝色主调）替换
- **侧边栏渐变**：在 `App.vue` 的 `.app-sidebar` 样式中修改

### 11.5 数据字段兼容

由于后端返回结构可能存在差异，前端在所有数据消费处做了兼容处理：

```js
// 兼容数组、{data: []}、{rows: []} 等多种格式
const list = Array.isArray(res) ? res : (res?.data || res?.rows || [])
```

如后端字段名变更，需在对应 `computed` 属性中调整字段映射逻辑（搜索 `r.brand || r.name` 等模式）。

---

## 十二、已知限制与注意事项

1. **后端依赖**：前端所有数据依赖后端服务，后端未启动时所有图表会显示「加载失败」
2. **字段假设**：业务关联分析页面的图表配置基于对后端返回字段的合理推测，若实际字段名不同需调整 `computed` 中的字段映射
3. **浏览器兼容**：使用了 ES2020+ 语法和可选链操作符，需现代浏览器支持
4. **文件大小**：ECharts 全量引入，生产环境建议按需引入以减小体积
5. **跨域问题**：开发环境通过 Vite 代理解决；生产部署需配置 Nginx 反向代理

---

## 十三、版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.1.0 | 2026-06-21 | 完成 6 大模块 57 个接口的全量可视化，新增车辆搜索与对比分析 |

---

## 十四、相关文档

- [后端代码分析文档](./back.md)
- [Vue 3 官方文档](https://vuejs.org/)
- [ECharts 官方文档](https://echarts.apache.org/zh/index.html)
- [Vite 官方文档](https://vitejs.dev/)
- [Axios 官方文档](https://axios-http.com/zh/)
