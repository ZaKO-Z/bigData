/**
 * ECharts 图表构建器统一导出
 *
 * 按图表类型分类，便于按需引入：
 *   import { barVertical, line, heatmap, echarts } from '@/echarts'
 *
 * 或整体引入：
 *   import * as Charts from '@/echarts'
 *
 * 图表类型对照：
 *   - histogram (柱状图): barVertical / barHorizontal / barStacked / barGrouped / barLineCombo
 *   - line     (折线图): line / stackedArea / percentLine
 *   - pie      (饼图)  : pie / doughnut / rose / percentPie
 *   - heatmap  (热力图): heatmap / buildHeatmapData
 *   - boxplot  (箱线图): boxplot / buildBoxData
 *   - scatter  (散点图): scatter / scatterMulti
 *   - treemap  (矩形树图): treemap
 */

// 导出 ECharts 库（从 CDN 全局加载）
const echarts = window.echarts
export { echarts }
export default echarts

// 柱状图系列
export {
  barVertical,
  barHorizontal,
  barStacked,
  barGrouped,
  barLineCombo,
} from './histogram.js'

// 折线图系列
export {
  line,
  stackedArea,
  percentLine,
} from './line.js'

// 饼图系列
export {
  pie,
  doughnut,
  rose,
  percentPie,
} from './pie.js'

// 热力图系列
export {
  heatmap,
  buildHeatmapData,
} from './heatmap.js'

// 箱线图系列
export {
  boxplot,
  buildBoxData,
} from './boxplot.js'

// 散点图系列
export {
  scatter,
  scatterMulti,
} from './scatter.js'

// 矩形树图系列
export { treemap } from './treemap.js'

// 通用调色板
export const colorPalette = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#5d7092',
]
