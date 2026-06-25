/**
 * 散点图（Scatter）构建器
 * 适合两变量相关性分析
 */

const defaultColors = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
]

/**
 * 标准散点图
 * @param {Object} params
 * @param {Array} params.data - 数据数组 [[x, y, name?], ...]
 * @param {String} params.name - 系列名称
 * @param {String} params.xAxisName - X 轴名称
 * @param {String} params.yAxisName - Y 轴名称
 * @param {Number} params.symbolSize - 散点大小
 * @param {Array} params.colors - 自定义颜色
 */
export function scatter(params = {}) {
  const {
    data = [],
    name = '数据点',
    xAxisName = 'X',
    yAxisName = 'Y',
    symbolSize = 10,
    colors = defaultColors,
  } = params

  return {
    color: colors,
    tooltip: {
      trigger: 'item',
      formatter: (p) => {
        const label = p.data[2] ? `<br/>${p.data[2]}` : ''
        return `${xAxisName}: ${p.data[0]}<br/>${yAxisName}: ${p.data[1]}${label}`
      },
    },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '8%', containLabel: true },
    xAxis: {
      type: 'value',
      name: xAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      scale: true,
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      scale: true,
    },
    series: [
      {
        name,
        type: 'scatter',
        data,
        symbolSize,
        itemStyle: {
          color: hexToRgba(colors[0], 0.7),
          borderColor: colors[0],
          borderWidth: 1,
        },
        emphasis: {
          itemStyle: {
            color: colors[0],
            shadowBlur: 10,
            shadowColor: 'rgba(0,0,0,0.3)',
          },
        },
      },
    ],
  }
}

/**
 * 多系列散点图
 * @param {Object} params
 * @param {Array} params.series - 系列数组 [{ name, data: [[x, y, name?], ...] }]
 * @param {String} params.xAxisName - X 轴名称
 * @param {String} params.yAxisName - Y 轴名称
 * @param {Number} params.symbolSize - 散点大小
 * @param {Array} params.colors - 自定义颜色
 */
export function scatterMulti(params = {}) {
  const {
    series = [],
    xAxisName = 'X',
    yAxisName = 'Y',
    symbolSize = 10,
    colors = defaultColors,
  } = params

  return {
    color: colors,
    tooltip: {
      trigger: 'item',
      formatter: (p) => {
        const label = p.data[2] ? `<br/>${p.data[2]}` : ''
        return `${p.seriesName}<br/>${xAxisName}: ${p.data[0]}<br/>${yAxisName}: ${p.data[1]}${label}`
      },
    },
    legend: { top: 0, textStyle: { color: '#666' } },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '12%', containLabel: true },
    xAxis: {
      type: 'value',
      name: xAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      scale: true,
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      scale: true,
    },
    series: series.map((s, idx) => ({
      name: s.name,
      type: 'scatter',
      data: s.data,
      symbolSize,
      itemStyle: {
        color: hexToRgba(colors[idx % colors.length], 0.7),
        borderColor: colors[idx % colors.length],
        borderWidth: 1,
      },
      emphasis: {
        itemStyle: {
          color: colors[idx % colors.length],
          shadowBlur: 10,
          shadowColor: 'rgba(0,0,0,0.3)',
        },
      },
    })),
  }
}

// 工具函数
function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

export default {
  scatter,
  scatterMulti,
}
