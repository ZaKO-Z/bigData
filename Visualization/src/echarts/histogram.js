/**
 * 柱状图（Histogram）构建器
 * 包含：纵向柱状图、横向柱状图、堆叠柱状图、分组柱状图
 */

// 默认调色板
const defaultColors = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#5d7092',
]

/**
 * 纵向柱状图
 * @param {Object} params
 * @param {Array} params.data - 数据数组 [{ name, value }]
 * @param {String} params.name - 系列名称
 * @param {String} params.yAxisName - Y 轴名称
 * @param {Boolean} params.rotateLabel - X 轴标签是否旋转
 * @param {Array} params.colors - 自定义颜色
 * @param {Object} params.gradient - 渐变色 { from, to }
 * @returns {Object} ECharts option
 */
export function barVertical(params = {}) {
  const {
    data = [],
    name = '数量',
    yAxisName = '数量',
    rotateLabel = false,
    colors = defaultColors,
    gradient = null,
  } = params

  const itemStyle = gradient
    ? {
        borderRadius: [4, 4, 0, 0],
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: gradient.from || '#5470c6' },
            { offset: 1, color: gradient.to || '#91cc75' },
          ],
        },
      }
    : { borderRadius: [4, 4, 0, 0], color: colors[0] }

  return {
    color: colors,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.name),
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666', rotate: rotateLabel ? 30 : 0, interval: 0 },
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [
      {
        name,
        type: 'bar',
        data: data.map((d) => d.value),
        barWidth: '55%',
        itemStyle,
        label: { show: true, position: 'top', color: '#666', rotate: rotateLabel ? 30 : 0 },
      },
    ],
  }
}

/**
 * 横向柱状图（适合排名、类别多场景）
 * @param {Object} params
 * @param {Array} params.data - 数据数组 [{ name, value }]（按从大到小排序，函数内会反转）
 * @param {String} params.name - 系列名称
 * @param {String} params.xAxisName - X 轴名称
 * @param {Array} params.colors - 自定义颜色
 * @param {Object} params.gradient - 渐变色 { from, to }
 */
export function barHorizontal(params = {}) {
  const {
    data = [],
    name = '数量',
    xAxisName = '数量',
    colors = defaultColors,
    gradient = { from: '#73c0de', to: '#5470c6' },
  } = params

  const reversed = [...data].reverse()
  return {
    color: colors,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '8%', bottom: '3%', top: '5%', containLabel: true },
    xAxis: {
      type: 'value',
      name: xAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    yAxis: {
      type: 'category',
      data: reversed.map((d) => d.name),
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666' },
    },
    series: [
      {
        name,
        type: 'bar',
        data: reversed.map((d) => d.value),
        barWidth: '60%',
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: gradient
            ? {
                type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
                colorStops: [
                  { offset: 0, color: gradient.from },
                  { offset: 1, color: gradient.to },
                ],
              }
            : colors[0],
        },
        label: { show: true, position: 'right', color: '#666' },
      },
    ],
  }
}

/**
 * 堆叠柱状图（多系列构成对比）
 * @param {Object} params
 * @param {Array} params.categories - 类别数组
 * @param {Array} params.series - 系列数组 [{ name, data: [] }]
 * @param {String} params.yAxisName - Y 轴名称
 * @param {Boolean} params.rotateLabel - X 轴标签是否旋转
 * @param {Array} params.colors - 自定义颜色
 */
export function barStacked(params = {}) {
  const {
    categories = [],
    series = [],
    yAxisName = '数量',
    rotateLabel = false,
    colors = defaultColors,
  } = params

  return {
    color: colors,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, textStyle: { color: '#666' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: categories,
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666', rotate: rotateLabel ? 20 : 0 },
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: series.map((s) => ({
      name: s.name,
      type: 'bar',
      stack: 'total',
      data: s.data,
      itemStyle: { borderRadius: 0 },
      emphasis: { focus: 'series' },
    })),
  }
}

/**
 * 分组柱状图（多系列并列对比）
 * @param {Object} params
 * @param {Array} params.categories - 类别数组
 * @param {Array} params.series - 系列数组 [{ name, data: [] }]
 * @param {String} params.yAxisName - Y 轴名称
 * @param {Boolean} params.rotateLabel - X 轴标签是否旋转
 * @param {Array} params.colors - 自定义颜色
 */
export function barGrouped(params = {}) {
  const {
    categories = [],
    series = [],
    yAxisName = '数量',
    rotateLabel = false,
    colors = defaultColors,
  } = params

  return {
    color: colors,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, textStyle: { color: '#666' } },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: categories,
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666', rotate: rotateLabel ? 30 : 0 },
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: series.map((s) => ({
      name: s.name,
      type: 'bar',
      data: s.data,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
    })),
  }
}

/**
 * 双轴组合图（柱状图+折线图，量纲不同的双指标）
 * @param {Object} params
 * @param {Array} params.categories - 类别数组
 * @param {Object} params.bar - 柱状图配置 { name, data, color }
 * @param {Object} params.line - 折线图配置 { name, data, color, yAxisIndex }
 * @param {String} params.barAxisName - 柱状图轴名称
 * @param {String} params.lineAxisName - 折线图轴名称
 * @param {Boolean} params.rotateLabel - X 轴标签是否旋转
 * @param {Array} params.colors - 自定义颜色
 */
export function barLineCombo(params = {}) {
  const {
    categories = [],
    bar = { name: '', data: [] },
    line = { name: '', data: [], yAxisIndex: 1 },
    barAxisName = '',
    lineAxisName = '',
    rotateLabel = false,
    colors = defaultColors,
  } = params

  return {
    color: colors,
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#666' } },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: categories,
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666', rotate: rotateLabel ? 20 : 0 },
    },
    yAxis: [
      { type: 'value', name: barAxisName, axisLine: { lineStyle: { color: '#999' } }, splitLine: { lineStyle: { color: '#f0f0f0' } } },
      { type: 'value', name: lineAxisName, axisLine: { lineStyle: { color: '#999' } }, splitLine: { show: false } },
    ],
    series: [
      {
        name: bar.name,
        type: 'bar',
        data: bar.data,
        itemStyle: { color: bar.color || colors[0], borderRadius: [4, 4, 0, 0] },
      },
      {
        name: line.name,
        type: 'line',
        yAxisIndex: line.yAxisIndex ?? 1,
        data: line.data,
        itemStyle: { color: line.color || colors[3] },
        lineStyle: { width: 3 },
        smooth: true,
      },
    ],
  }
}

export default {
  barVertical,
  barHorizontal,
  barStacked,
  barGrouped,
  barLineCombo,
}
