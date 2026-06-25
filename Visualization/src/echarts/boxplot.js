/**
 * 箱线图（Boxplot）构建器
 * 适合展示数据分布区间（最小值、Q1、中位数、Q3、最大值）
 */

const defaultColors = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
]

/**
 * 标准箱线图
 * @param {Object} params
 * @param {Array} params.categories - X 轴类别
 * @param {Array} params.data - 箱线图数据 [[min, Q1, median, Q3, max], ...]
 * @param {Array} params.outliers - 离群点数据 [[xIndex, value], ...]
 * @param {String} params.name - 系列名称
 * @param {String} params.yAxisName - Y 轴名称
 * @param {Array} params.colors - 自定义颜色
 */
export function boxplot(params = {}) {
  const {
    categories = [],
    data = [],
    outliers = [],
    name = '分布',
    yAxisName = '数值',
    colors = defaultColors,
  } = params

  const series = [
    {
      name: name,
      type: 'boxplot',
      data: data,
      itemStyle: {
        color: hexToRgba(colors[0], 0.5),
        borderColor: colors[0],
        borderWidth: 2,
      },
    },
  ]

  if (outliers && outliers.length) {
    series.push({
      name: '离群点',
      type: 'scatter',
      data: outliers,
      symbolSize: 6,
      itemStyle: { color: colors[3] },
    })
  }

  return {
    color: colors,
    tooltip: {
      trigger: 'item',
      formatter: (param) => {
        if (param.seriesType === 'boxplot') {
          const d = param.data
          return `${param.name}<br/>`
            + `最大值: ${d[5] || d[4]}<br/>`
            + `Q3: ${d[4] || d[3]}<br/>`
            + `中位数: ${d[2] || d[3]}<br/>`
            + `Q1: ${d[1] || d[1]}<br/>`
            + `最小值: ${d[0] || d[0]}`
        }
        return `${param.seriesName}: ${param.data[1]}`
      },
    },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category',
      data: categories,
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666', rotate: categories.length > 8 ? 30 : 0 },
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series,
  }
}

/**
 * 从原始数据构建箱线图所需的五数概括
 * @param {Array} values - 数值数组
 * @returns {Array} [min, Q1, median, Q3, max]
 */
export function buildBoxData(values = []) {
  if (!values.length) return [0, 0, 0, 0, 0]
  const sorted = [...values].filter((v) => v != null && !isNaN(v)).sort((a, b) => a - b)
  if (!sorted.length) return [0, 0, 0, 0, 0]

  const quantile = (arr, q) => {
    const pos = (arr.length - 1) * q
    const base = Math.floor(pos)
    const rest = pos - base
    return arr[base + 1] !== undefined ? arr[base] + rest * (arr[base + 1] - arr[base]) : arr[base]
  }

  return [
    Number(sorted[0].toFixed(2)),
    Number(quantile(sorted, 0.25).toFixed(2)),
    Number(quantile(sorted, 0.5).toFixed(2)),
    Number(quantile(sorted, 0.75).toFixed(2)),
    Number(sorted[sorted.length - 1].toFixed(2)),
  ]
}

// 工具函数
function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

export default {
  boxplot,
  buildBoxData,
}
