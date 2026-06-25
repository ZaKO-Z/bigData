/**
 * 矩形树图（Treemap）构建器
 * 适合层次占比关系
 */

const defaultColors = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#5d7092',
]

/**
 * 标准矩形树图
 * @param {Object} params
 * @param {Array} params.data - 数据数组 [{ name, value, children? }]
 * @param {String} params.name - 系列名称
 * @param {Array} params.colors - 自定义颜色
 */
export function treemap(params = {}) {
  const {
    data = [],
    name = '分布',
    colors = defaultColors,
  } = params

  return {
    color: colors,
    tooltip: {
      formatter: (p) => `${p.name}<br/>数量: ${p.value}<br/>占比: ${p.percent}%`,
    },
    series: [
      {
        name,
        type: 'treemap',
        data,
        roam: false,
        nodeClick: 'zoomToNode',
        visualDimension: 0,
        squareRatio: 0.5,
        label: {
          show: true,
          formatter: '{b}\n{c}',
          color: '#fff',
          fontSize: 12,
        },
        upperLabel: { show: true, height: 22, color: '#fff' },
        itemStyle: { borderColor: '#fff', borderWidth: 2, gapWidth: 2 },
        levels: [
          {
            itemStyle: { borderColor: '#ddd', borderWidth: 0, gapWidth: 1 },
            upperLabel: { show: false },
          },
          {
            colorSaturation: [0.35, 0.5],
            itemStyle: { borderColor: '#fff', borderWidth: 2, gapWidth: 2 },
          },
        ],
      },
    ],
  }
}

export default {
  treemap,
}
