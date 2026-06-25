<template>
  <div class="advanced">
    <!-- 分类维度分析 -->
    <section class="advanced__section">
      <div class="section-header">
        <h2 class="section-title">分类维度分析</h2>
        <div class="section-desc">从 13 个维度对车型进行分组统计，了解各维度的分布情况</div>
      </div>

      <div class="dimension-tabs">
        <button
          v-for="dim in dimensions"
          :key="dim.key"
          class="dimension-tab"
          :class="{ 'dimension-tab--active': activeDimension === dim.key }"
          @click="switchDimension(dim.key)"
        >
          {{ dim.label }}
        </button>
      </div>

      <div class="advanced__content">
        <ChartCard :title="`按${currentDimensionLabel}分类统计`" :loading="groupLoading" :error="groupError">
          <div class="chart-controls" v-if="!groupLoading && !groupError">
            <label class="control-label">图表类型：</label>
            <select v-model="groupChartType" class="control-select">
              <option v-if="isTimeDimension" value="line">折线图</option>
              <option v-if="isTimeDimension" value="area">面积图</option>
              <option v-if="!isTimeDimension" value="bar">柱状图</option>
              <option v-if="!isTimeDimension" value="pie">饼图</option>
              <option v-if="!isTimeDimension" value="funnel">漏斗图</option>
            </select>
            <label class="control-label">显示数量：</label>
            <select v-model="groupLimit" class="control-select">
              <option :value="10">Top 10</option>
              <option :value="20">Top 20</option>
              <option :value="50">全部</option>
            </select>
          </div>
          <BaseChart
            v-if="!groupLoading && !groupError && groupData.length"
            :option="groupOption"
            height="420px"
          />
        </ChartCard>
      </div>
    </section>

    <!-- 排序度量分析 -->
    <section class="advanced__section">
      <div class="section-header">
        <h2 class="section-title">排序度量分析</h2>
        <div class="section-desc">按度量字段排序，查看 Top 车型详情，支持散点图分析两度量相关性</div>
      </div>

      <div class="sort-controls">
        <div class="sort-control-group">
          <label class="control-label">度量字段：</label>
          <select v-model="sortMetric" class="control-select" @change="loadSortData">
            <option v-for="m in metrics" :key="m.key" :value="m.key">{{ m.label }}</option>
          </select>
        </div>
        <div class="sort-control-group">
          <label class="control-label">排序方式：</label>
          <select v-model="sortOrder" class="control-select" @change="loadSortData">
            <option value="desc">降序（高→低）</option>
            <option value="asc">升序（低→高）</option>
          </select>
        </div>
        <div class="sort-control-group">
          <label class="control-label">显示数量：</label>
          <select v-model="sortLimit" class="control-select" @change="loadSortData">
            <option :value="5">Top 5</option>
            <option :value="10">Top 10</option>
            <option :value="20">Top 20</option>
            <option :value="50">Top 50</option>
          </select>
        </div>
        <button class="btn btn--primary" @click="loadSortData">查询</button>
      </div>

      <div class="advanced__content">
        <div class="sort-grid">
          <ChartCard title="Top 排名可视化" :loading="sortLoading" :error="sortError">
            <BaseChart
              v-if="!sortLoading && !sortError && sortData.length"
              :option="sortChartOption"
              height="420px"
            />
          </ChartCard>

          <ChartCard title="明细数据表" :loading="sortLoading" :error="sortError">
            <div class="table-wrapper" v-if="!sortLoading && !sortError && sortData.length">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>排名</th>
                    <th v-for="col in sortColumns" :key="col">{{ col }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, idx) in sortData" :key="idx">
                    <td>{{ idx + 1 }}</td>
                    <td v-for="col in sortColumns" :key="col">{{ formatValue(row[col]) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </ChartCard>
        </div>

        <!-- 散点图：两度量相关性分析 -->
        <ChartCard title="双度量相关性散点图（价格 × 马力）" :loading="sortLoading" :error="sortError">
          <template #extra>
            <div class="scatter-controls">
              <label class="control-label">X轴：</label>
              <select v-model="scatterX" class="control-select">
                <option v-for="col in numericColumns" :key="col" :value="col">{{ col }}</option>
              </select>
              <label class="control-label">Y轴：</label>
              <select v-model="scatterY" class="control-select">
                <option v-for="col in numericColumns" :key="col" :value="col">{{ col }}</option>
              </select>
            </div>
          </template>
          <BaseChart
            v-if="!sortLoading && !sortError && sortData.length && numericColumns.length >= 2"
            :option="scatterOption"
            height="420px"
          />
          <div v-else-if="!sortLoading && !sortError" class="empty-tip">暂无足够数值字段生成散点图</div>
        </ChartCard>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import BaseChart from '@/components/BaseChart.vue'
import ChartCard from '@/components/ChartCard.vue'
import { groupApi, sortApi } from '@/api'
// 引入分类图表构建器
import {
  barVertical,
  barHorizontal,
  pie as pieChart,
  scatter as scatterChart,
  colorPalette,
} from '@/echarts'

// 分类维度配置
const dimensions = [
  { key: 'brand', label: '品牌' },
  { key: 'level', label: '级别' },
  { key: 'energy', label: '能源类型' },
  { key: 'body-type', label: '车身结构' },
  { key: 'trans-type', label: '变速箱大类' },
  { key: 'trans-subtype', label: '变速箱亚型' },
  { key: 'fuel-grade', label: '燃油标号' },
  { key: 'engine-layout', label: '发动机布局' },
  { key: 'warranty-unlimited', label: '质保里程不限' },
  { key: 'launch-year', label: '上市年份' },
  { key: 'price-range', label: '价格区间' },
  { key: 'seats', label: '座位数' },
  { key: 'gear-count', label: '档位数' },
]

// 排序度量配置
const metrics = [
  { key: 'launch-date', label: '上市时间' },
  { key: 'price', label: '厂商指导价' },
  { key: 'displacement', label: '发动机排量' },
  { key: 'horsepower', label: '最大马力' },
  { key: 'doors', label: '门数' },
  { key: 'seats', label: '座位数' },
  { key: 'gear-count', label: '档位数' },
  { key: 'warranty-years', label: '质保年限' },
  { key: 'warranty-km', label: '质保里程' },
]

const activeDimension = ref('brand')
const groupData = ref([])
const groupLoading = ref(false)
const groupError = ref('')
const groupChartType = ref('bar')
const groupLimit = ref(20)

const currentDimensionLabel = computed(() => {
  const dim = dimensions.find((d) => d.key === activeDimension.value)
  return dim ? dim.label : ''
})

// 时间维度判断（launch-year 应使用折线图）
const isTimeDimension = computed(() => activeDimension.value === 'launch-year')

// 分类维度的图表配置
const groupOption = computed(() => {
  const limited = groupData.value.slice(0, groupLimit.value)
  const dataArr = limited.map((item, idx) => ({
    name: item.name || item.key || item.label || item.category || item.brand || item.level || item.energy_name || item.body_type || item.trans_type || item.fuel_grade || item.engine_type || `项${idx + 1}`,
    value: item.cnt ?? item.count ?? item.value ?? item.num ?? 0,
  }))

  // 时间维度使用折线图/面积图
  if (isTimeDimension.value) {
    // 按年份排序
    const sorted = [...dataArr].sort((a, b) => {
      const ya = parseInt(a.name)
      const yb = parseInt(b.name)
      if (!isNaN(ya) && !isNaN(yb)) return ya - yb
      return String(a.name).localeCompare(String(b.name))
    })
    return {
      color: colorPalette,
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
      xAxis: {
        type: 'category',
        data: sorted.map((d) => d.name),
        boundaryGap: false,
        axisLine: { lineStyle: { color: '#999' } },
        axisLabel: { color: '#666' },
      },
      yAxis: {
        type: 'value',
        name: '数量',
        axisLine: { lineStyle: { color: '#999' } },
        splitLine: { lineStyle: { color: '#f0f0f0' } },
      },
      series: [
        {
          name: '车型数量',
          type: 'line',
          smooth: true,
          data: sorted.map((d) => d.value),
          itemStyle: { color: '#5470c6' },
          lineStyle: { width: 3 },
          areaStyle: groupChartType.value === 'area'
            ? {
                color: {
                  type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                  colorStops: [
                    { offset: 0, color: 'rgba(84,112,198,0.5)' },
                    { offset: 1, color: 'rgba(84,112,198,0.05)' },
                  ],
                },
              }
            : undefined,
          label: { show: true, position: 'top', color: '#666' },
        },
      ],
    }
  }

  // 饼图
  if (groupChartType.value === 'pie') {
    return pieChart({
      data: dataArr,
      name: currentDimensionLabel.value,
    })
  }

  // 漏斗图
  if (groupChartType.value === 'funnel') {
    return {
      color: colorPalette,
      tooltip: { trigger: 'item', formatter: '{b}: {c}' },
      legend: { type: 'scroll', orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#666' } },
      series: [
        {
          name: currentDimensionLabel.value,
          type: 'funnel',
          left: '10%',
          width: '70%',
          top: 20,
          bottom: 20,
          label: { show: true, position: 'inside', formatter: '{b}: {c}' },
          data: dataArr,
        },
      ],
    }
  }

  // 默认柱状图：类别多时使用横向
  if (dataArr.length > 8) {
    return barHorizontal({
      data: dataArr,
      name: '数量',
      xAxisName: '数量',
      gradient: { from: '#73c0de', to: '#5470c6' },
    })
  }
  return barVertical({
    data: dataArr,
    name: '数量',
    yAxisName: '数量',
    rotateLabel: dataArr.length > 6,
    gradient: { from: '#5470c6', to: '#91cc75' },
  })
})

const switchDimension = async (key) => {
  activeDimension.value = key
  // 时间维度默认折线图，其他默认柱状图
  groupChartType.value = key === 'launch-year' ? 'line' : 'bar'
  await loadGroupData()
}

const loadGroupData = async () => {
  groupLoading.value = true
  groupError.value = ''
  try {
    const res = await groupApi.getByDimension(activeDimension.value)
    let data = Array.isArray(res) ? res : (res?.data || [])
    groupData.value = data
  } catch (e) {
    groupError.value = '加载失败：' + (e.message || '未知错误')
    groupData.value = []
  } finally {
    groupLoading.value = false
  }
}

// 排序度量
const sortMetric = ref('price')
const sortOrder = ref('desc')
const sortLimit = ref(10)
const sortData = ref([])
const sortLoading = ref(false)
const sortError = ref('')

// 散点图轴选择
const scatterX = ref('price_wan')
const scatterY = ref('horsepower')

const sortColumns = computed(() => {
  if (!sortData.value.length) return []
  const keys = []
  sortData.value.forEach((row) => {
    Object.keys(row).forEach((k) => {
      if (!keys.includes(k)) keys.push(k)
    })
  })
  return keys
})

// 数值型列（用于散点图）
const numericColumns = computed(() => {
  if (!sortData.value.length) return []
  const numericSet = new Set()
  const sample = sortData.value[0]
  Object.keys(sample).forEach((k) => {
    const v = sample[k]
    if (typeof v === 'number' || (!isNaN(parseFloat(v)) && isFinite(v))) {
      numericSet.add(k)
    }
  })
  const arr = [...numericSet]
  // 默认值兜底
  if (!scatterX.value || !arr.includes(scatterX.value)) {
    scatterX.value = arr.find((k) => k.toLowerCase().includes('price')) || arr[0]
  }
  if (!scatterY.value || !arr.includes(scatterY.value)) {
    scatterY.value = arr.find((k) => k.toLowerCase().includes('horse')) || arr[1] || arr[0]
  }
  return arr
})

const sortChartOption = computed(() => {
  const metricLabel = metrics.find((m) => m.key === sortMetric.value)?.label || sortMetric.value
  const valueKey = sortColumns.value.find((k) =>
    k.toLowerCase().includes(sortMetric.value.toLowerCase().replace('-', '')) ||
    k.toLowerCase().includes(sortMetric.value.toLowerCase()) ||
    ['price_wan', 'price', 'horsepower', 'displacement_l', 'displacement', 'doors', 'seats', 'gear_count', 'warranty_years', 'warranty_km_wan', 'launch_date', 'launch_year'].includes(k.toLowerCase())
  ) || sortColumns.value[sortColumns.value.length - 1]

  const nameKey = sortColumns.value.find((k) =>
    ['brand', 'series', 'model', 'name', 'level'].includes(k.toLowerCase())
  ) || sortColumns.value[0]

  const items = sortData.value.slice().reverse()
  return {
    color: colorPalette,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        const row = sortData.value[sortData.value.length - 1 - p.dataIndex]
        let html = `<b>${p.name}</b><br/>`
        sortColumns.value.forEach((col) => {
          html += `${col}: ${formatValue(row[col])}<br/>`
        })
        return html
      },
    },
    grid: { left: '3%', right: '8%', bottom: '3%', top: '5%', containLabel: true },
    xAxis: {
      type: 'value',
      name: metricLabel,
      axisLine: { lineStyle: { color: '#999' } },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    yAxis: {
      type: 'category',
      data: items.map((row) => truncate(formatValue(row[nameKey]), 20)),
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666' },
    },
    series: [
      {
        name: metricLabel,
        type: 'bar',
        data: items.map((row) => Number(row[valueKey]) || 0),
        barWidth: '60%',
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#73c0de' },
              { offset: 1, color: '#ee6666' },
            ],
          },
        },
        label: { show: true, position: 'right', color: '#666', formatter: (p) => formatValue(p.value) },
      },
    ],
  }
})

// 散点图：两度量相关性分析
const scatterOption = computed(() => {
  if (!sortData.value.length || !scatterX.value || !scatterY.value) return {}
  const data = sortData.value.map((row) => {
    const x = parseFloat(row[scatterX.value])
    const y = parseFloat(row[scatterY.value])
    const nameKey = sortColumns.value.find((k) =>
      ['brand', 'series', 'model', 'name', 'level'].includes(k.toLowerCase())
    ) || sortColumns.value[0]
    if (isNaN(x) || isNaN(y)) return null
    return [x, y, row[nameKey] || '']
  }).filter(Boolean)

  return scatterChart({
    data,
    name: '车型',
    xAxisName: scatterX.value,
    yAxisName: scatterY.value,
    symbolSize: 12,
    colors: ['#5470c6'],
  })
})

const truncate = (str, len) => {
  const s = String(str ?? '')
  return s.length > len ? s.slice(0, len) + '...' : s
}

const formatValue = (val) => {
  if (val === null || val === undefined) return '-'
  if (typeof val === 'number') {
    return Number.isInteger(val) ? val.toString() : val.toFixed(2)
  }
  return String(val)
}

const loadSortData = async () => {
  sortLoading.value = true
  sortError.value = ''
  try {
    const res = await sortApi.getByMetric(sortMetric.value, {
      order: sortOrder.value,
      limit: sortLimit.value,
    })
    sortData.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (e) {
    sortError.value = '加载失败：' + (e.message || '未知错误')
    sortData.value = []
  } finally {
    sortLoading.value = false
  }
}

onMounted(() => {
  loadGroupData()
  loadSortData()
})
</script>

<style scoped>
.advanced {
  padding: 4px;
}

.advanced__section {
  margin-bottom: 32px;
}

.section-header {
  margin-bottom: 16px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 6px 0;
  padding-left: 12px;
  border-left: 4px solid #5470c6;
}

.section-desc {
  font-size: 13px;
  color: #6b7280;
  padding-left: 16px;
}

.dimension-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.06);
}

.dimension-tab {
  padding: 6px 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #6b7280;
  border-radius: 16px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.dimension-tab:hover {
  color: #5470c6;
  border-color: #5470c6;
}

.dimension-tab--active {
  background: #5470c6;
  color: #fff;
  border-color: #5470c6;
}

.chart-controls,
.sort-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
}

.sort-control-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.scatter-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.control-label {
  font-size: 13px;
  color: #6b7280;
}

.control-select {
  padding: 5px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 13px;
  color: #374151;
  background: #fff;
  cursor: pointer;
}

.control-select:focus {
  outline: none;
  border-color: #5470c6;
}

.btn {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.btn--primary {
  background: #5470c6;
  color: #fff;
}

.btn--primary:hover {
  background: #4060b0;
}

.sort-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 1024px) {
  .sort-grid {
    grid-template-columns: 1fr;
  }
}

.table-wrapper {
  overflow: auto;
  max-height: 420px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table thead {
  position: sticky;
  top: 0;
  background: #f5f7fa;
  z-index: 1;
}

.data-table th,
.data-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}

.data-table th {
  font-weight: 600;
  color: #374151;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.data-table td:first-child {
  font-weight: 600;
  color: #5470c6;
}

.empty-tip {
  padding: 40px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}
</style>
