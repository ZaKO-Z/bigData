/**
 * 饼图（Pie）构建器
 * 包含：标准饼图、环形图、玫瑰图
 */

const defaultColors = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#5d7092',
]

/**
 * 标准饼图
 * @param {Object} params
 * @param {Array} params.data - 数据数组 [{ name, value }]
 * @param {String} params.name - 系列名称
 * @param {Array} params.colors - 自定义颜色
 * @param {Boolean} params.showLegend - 是否显示图例
 */
export function pie(params = {}) {
  const {
    data = [],
    name = '分布',
    colors = defaultColors,
    showLegend = true,
  } = params

  return {
    color: colors,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: showLegend
      ? { type: 'scroll', orient: 'vertical', left: 'left', textStyle: { color: '#666' } }
      : undefined,
    series: [
      {
        name,
        type: 'pie',
        radius: '65%',
        center: showLegend ? ['60%', '50%'] : ['50%', '50%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%' },
        emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
        data,
      },
    ],
  }
}

/**
 * 环形图（Doughnut）
 * @param {Object} params
 * @param {Array} params.data - 数据数组 [{ name, value }]
 * @param {String} params.name - 系列名称
 * @param {Array} params.colors - 自定义颜色
 * @param {String} params.centerText - 中心文字
 */
export function doughnut(params = {}) {
  const {
    data = [],
    name = '分布',
    colors = defaultColors,
    centerText = '',
  } = params

  return {
    color: colors,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { type: 'scroll', orient: 'vertical', left: 'left', textStyle: { color: '#666' } },
    graphic: centerText
      ? {
          type: 'text',
          left: 'center',
          top: 'center',
          style: { text: centerText, fontSize: 20, fontWeight: 'bold', fill: '#333' },
        }
      : undefined,
    series: [
      {
        name,
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%' },
        emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
        data,
      },
    ],
  }
}

/**
 * 玫瑰图（Nightingale Rose）
 * @param {Object} params
 * @param {Array} params.data - 数据数组 [{ name, value }]
 * @param {String} params.name - 系列名称
 * @param {Array} params.colors - 自定义颜色
 */
export function rose(params = {}) {
  const {
    data = [],
    name = '分布',
    colors = defaultColors,
  } = params

  return {
    color: colors,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', textStyle: { color: '#666' } },
    series: [
      {
        name,
        type: 'pie',
        radius: ['20%', '70%'],
        center: ['60%', '50%'],
        roseType: 'area',
        itemStyle: { borderRadius: 6 },
        label: { show: true, formatter: '{b}\n{d}%' },
        data,
      },
    ],
  }
}

/**
 * 百分比饼图（值为百分比）
 * @param {Object} params
 * @param {Array} params.data - 数据数组 [{ name, value }]（value 为百分比数值）
 * @param {String} params.name - 系列名称
 * @param {Array} params.colors - 自定义颜色
 */
export function percentPie(params = {}) {
  const {
    data = [],
    name = '占比',
    colors = defaultColors,
  } = params

  return {
    color: colors,
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    legend: { orient: 'vertical', left: 'left', textStyle: { color: '#666' } },
    series: [
      {
        name,
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c}%' },
        data,
      },
    ],
  }
}

export default {
  pie,
  doughnut,
  rose,
  percentPie,
}
