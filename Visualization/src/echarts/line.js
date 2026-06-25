/**
 * 折线图（Line）构建器
 * 包含：折线图、面积图、堆叠面积图、多系列折线图
 */

const defaultColors = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#5d7092',
]

/**
 * 基础折线图（适合时间序列趋势）
 * @param {Object} params
 * @param {Array} params.categories - X 轴类别（如年份）
 * @param {Array} params.series - 系列数组 [{ name, data: [] }]
 * @param {String} params.yAxisName - Y 轴名称
 * @param {Boolean} params.smooth - 是否平滑
 * @param {Boolean} params.area - 是否显示面积
 * @param {Array} params.colors - 自定义颜色
 */
export function line(params = {}) {
  const {
    categories = [],
    series = [],
    yAxisName = '数量',
    smooth = true,
    area = false,
    colors = defaultColors,
  } = params

  return {
    color: colors,
    tooltip: { trigger: 'axis' },
    legend: series.length > 1 ? { top: 0, textStyle: { color: '#666' } } : undefined,
    grid: { left: '3%', right: '4%', bottom: '3%', top: series.length > 1 ? '12%' : '8%', containLabel: true },
    xAxis: {
      type: 'category',
      data: categories,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666' },
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: series.map((s, idx) => ({
      name: s.name,
      type: 'line',
      smooth,
      data: s.data,
      itemStyle: { color: colors[idx % colors.length] },
      lineStyle: { width: 3 },
      areaStyle: area
        ? {
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: hexToRgba(colors[idx % colors.length], 0.5) },
                { offset: 1, color: hexToRgba(colors[idx % colors.length], 0.05) },
              ],
            },
          }
        : undefined,
    })),
  }
}

/**
 * 堆叠面积图（适合时间序列趋势 + 构成）
 * @param {Object} params
 * @param {Array} params.categories - X 轴类别
 * @param {Array} params.series - 系列数组 [{ name, data: [] }]
 * @param {String} params.yAxisName - Y 轴名称
 * @param {Boolean} params.smooth - 是否平滑
 * @param {Array} params.colors - 自定义颜色
 */
export function stackedArea(params = {}) {
  const {
    categories = [],
    series = [],
    yAxisName = '数量',
    smooth = true,
    colors = defaultColors,
  } = params

  return {
    color: colors,
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross', label: { backgroundColor: '#6a7985' } } },
    legend: { top: 0, textStyle: { color: '#666' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: categories,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666' },
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: series.map((s, idx) => ({
      name: s.name,
      type: 'line',
      stack: 'total',
      smooth,
      data: s.data,
      itemStyle: { color: colors[idx % colors.length] },
      lineStyle: { width: 2 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: hexToRgba(colors[idx % colors.length], 0.6) },
            { offset: 1, color: hexToRgba(colors[idx % colors.length], 0.1) },
          ],
        },
      },
      emphasis: { focus: 'series' },
    })),
  }
}

/**
 * 百分比折线图（适合占比趋势）
 * @param {Object} params
 * @param {Array} params.categories - X 轴类别
 * @param {Array} params.series - 系列数组 [{ name, data: [] }]（值为百分比）
 * @param {String} params.yAxisName - Y 轴名称
 * @param {Array} params.colors - 自定义颜色
 */
export function percentLine(params = {}) {
  const {
    categories = [],
    series = [],
    yAxisName = '占比(%)',
    colors = defaultColors,
  } = params

  return {
    color: colors,
    tooltip: { trigger: 'axis', formatter: '{b}<br/>{a}: {c}%' },
    legend: { top: 0, textStyle: { color: '#666' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: categories,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666' },
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      axisLabel: { formatter: '{value}%' },
    },
    series: series.map((s, idx) => ({
      name: s.name,
      type: 'line',
      smooth: true,
      data: s.data,
      itemStyle: { color: colors[idx % colors.length] },
      lineStyle: { width: 3 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: hexToRgba(colors[idx % colors.length], 0.5) },
            { offset: 1, color: hexToRgba(colors[idx % colors.length], 0.05) },
          ],
        },
      },
    })),
  }
}

// 工具函数：hex 转 rgba
function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

export default {
  line,
  stackedArea,
  percentLine,
}
