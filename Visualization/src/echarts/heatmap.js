/**
 * 热力图（Heatmap）构建器
 * 适合二维交叉密度分析
 */

const defaultColors = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
]

/**
 * 标准热力图
 * @param {Object} params
 * @param {Array} params.xAxis - X 轴类别数组
 * @param {Array} params.yAxis - Y 轴类别数组
 * @param {Array} params.data - 数据数组 [[xIndex, yIndex, value], ...]
 * @param {String} params.name - 系列名称
 * @param {Array} params.visualMapColors - 视觉映射颜色 [low, high]
 * @param {Number} params.min - 最小值
 * @param {Number} params.max - 最大值
 * @param {String} params.xAxisName - X 轴名称
 * @param {String} params.yAxisName - Y 轴名称
 */
export function heatmap(params = {}) {
  const {
    xAxis = [],
    yAxis = [],
    data = [],
    name = '数量',
    visualMapColors = ['#e0f2fe', '#0369a1'],
    min = 0,
    max = null,
    xAxisName = '',
    yAxisName = '',
  } = params

  const maxValue = max !== null ? max : Math.max(...data.map((d) => d[2] || 0), 1)

  return {
    tooltip: {
      position: 'top',
      formatter: (p) => `${xAxis[p.data[0]]} × ${yAxis[p.data[1]]}<br/>${name}: ${p.data[2]}`,
    },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '5%', containLabel: true },
    xAxis: {
      type: 'category',
      data: xAxis,
      name: xAxisName,
      splitArea: { show: true },
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666', rotate: xAxis.length > 8 ? 30 : 0 },
    },
    yAxis: {
      type: 'category',
      data: yAxis,
      name: yAxisName,
      splitArea: { show: true },
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666' },
    },
    visualMap: {
      min: 0,
      max: maxValue,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '2%',
      inRange: { color: visualMapColors },
    },
    series: [
      {
        name,
        type: 'heatmap',
        data,
        label: { show: true, color: '#333', fontSize: 11 },
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' },
        },
      },
    ],
  }
}

/**
 * 从二维表格数据构建热力图数据
 * @param {Array} rows - 数据行数组
 * @param {String} xField - X 轴字段名
 * @param {String} yField - Y 轴字段名
 * @param {String} valueField - 数值字段名
 * @returns {Object} { xAxis, yAxis, data }
 */
export function buildHeatmapData(rows = [], xField, yField, valueField = 'count') {
  const xSet = new Set()
  const ySet = new Set()
  const lookup = {}

  rows.forEach((row) => {
    const x = row[xField]
    const y = row[yField]
    const v = Number(row[valueField] || 0)
    if (x == null || y == null) return
    xSet.add(x)
    ySet.add(y)
    lookup[`${x}|${y}`] = v
  })

  const xAxis = [...xSet]
  const yAxis = [...ySet]
  const data = []

  yAxis.forEach((y, yIdx) => {
    xAxis.forEach((x, xIdx) => {
      const v = lookup[`${x}|${y}`] || 0
      data.push([xIdx, yIdx, v])
    })
  })

  return { xAxis, yAxis, data }
}

export default {
  heatmap,
  buildHeatmapData,
}
