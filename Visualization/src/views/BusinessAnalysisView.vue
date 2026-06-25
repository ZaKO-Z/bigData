<template>
  <div class="business">
    <!-- 业务维度切换 -->
    <div class="business__tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="biz-tab"
        :class="{ 'biz-tab--active': activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <span class="biz-tab__icon">{{ tab.icon }}</span>
        <span class="biz-tab__label">{{ tab.label }}</span>
      </button>
    </div>

    <!-- 1. 品牌 × 级别 × 指导价 -->
    <div v-show="activeTab === 'brand-level-price'" class="business__panel">
      <div class="biz-grid">
        <ChartCard title="各品牌在各级别中的均价对比" :loading="loading.blpBand" :error="errors.blpBand">
          <BaseChart v-if="!loading.blpBand && !errors.blpBand" :option="blpBandOption" height="420px" />
        </ChartCard>
        <ChartCard title="指定级别下各品牌价格分布（箱线图）" :loading="loading.blpByLevel" :error="errors.blpByLevel">
          <template #extra>
            <select v-model="blpLevel" class="control-select" @change="loadBlpByLevel">
              <option v-for="lv in levelOptions" :key="lv" :value="lv">{{ lv }}</option>
            </select>
          </template>
          <BaseChart v-if="!loading.blpByLevel && !errors.blpByLevel" :option="blpByLevelOption" height="420px" />
        </ChartCard>
      </div>
      <ChartCard title="各级别品牌数量与价格跨度总览" :loading="loading.blpSummary" :error="errors.blpSummary">
        <BaseChart v-if="!loading.blpSummary && !errors.blpSummary" :option="blpSummaryOption" height="420px" />
      </ChartCard>
    </div>

    <!-- 2. 上市时间 × 能源类型 -->
    <div v-show="activeTab === 'energy-trend'" class="business__panel">
      <div class="biz-grid">
        <ChartCard title="按年份统计各能源类型新车数量（堆叠面积图）" :loading="loading.etYear" :error="errors.etYear">
          <BaseChart v-if="!loading.etYear && !errors.etYear" :option="etYearOption" height="420px" />
        </ChartCard>
        <ChartCard title="按年份统计新能源占比（面积图）" :loading="loading.etRatio" :error="errors.etRatio">
          <BaseChart v-if="!loading.etRatio && !errors.etRatio" :option="etRatioOption" height="420px" />
        </ChartCard>
      </div>
      <div class="biz-grid">
        <ChartCard title="按季度统计各能源类型新车数量" :loading="loading.etSeason" :error="errors.etSeason">
          <BaseChart v-if="!loading.etSeason && !errors.etSeason" :option="etSeasonOption" height="420px" />
        </ChartCard>
        <ChartCard title="各品牌新能源车型占比" :loading="loading.etBrand" :error="errors.etBrand">
          <BaseChart v-if="!loading.etBrand && !errors.etBrand" :option="etBrandOption" height="420px" />
        </ChartCard>
      </div>
    </div>

    <!-- 3. 排量 × 燃油标号 × 马力 -->
    <div v-show="activeTab === 'power-match'" class="business__panel">
      <div class="biz-grid">
        <ChartCard title="排量×燃油标号分布（热力图）" :loading="loading.pmDispFuel" :error="errors.pmDispFuel">
          <BaseChart v-if="!loading.pmDispFuel && !errors.pmDispFuel" :option="pmDispFuelOption" height="420px" />
        </ChartCard>
        <ChartCard title="各排量段马力分布（箱线图）" :loading="loading.pmDispHp" :error="errors.pmDispHp">
          <BaseChart v-if="!loading.pmDispHp && !errors.pmDispHp" :option="pmDispHpOption" height="420px" />
        </ChartCard>
      </div>
      <ChartCard title="排量×燃油标号×马力详细交叉（热力图）" :loading="loading.pmDetail" :error="errors.pmDetail">
        <BaseChart v-if="!loading.pmDetail && !errors.pmDetail" :option="pmDetailOption" height="460px" />
      </ChartCard>
    </div>

    <!-- 4. 车身结构 × 座位数 × 级别 -->
    <div v-show="activeTab === 'space-config'" class="business__panel">
      <div class="biz-grid">
        <ChartCard title="各级别座位数分布" :loading="loading.scLevelSeats" :error="errors.scLevelSeats">
          <BaseChart v-if="!loading.scLevelSeats && !errors.scLevelSeats" :option="scLevelSeatsOption" height="420px" />
        </ChartCard>
        <ChartCard title="各车身结构座位数分布" :loading="loading.scBodySeats" :error="errors.scBodySeats">
          <BaseChart v-if="!loading.scBodySeats && !errors.scBodySeats" :option="scBodySeatsOption" height="420px" />
        </ChartCard>
      </div>
      <div class="biz-grid">
        <ChartCard title="各级别7座车型占比" :loading="loading.scSevenSeat" :error="errors.scSevenSeat">
          <BaseChart v-if="!loading.scSevenSeat && !errors.scSevenSeat" :option="scSevenSeatOption" height="420px" />
        </ChartCard>
        <ChartCard title="指定级别下车身×座位交叉（热力图）" :loading="loading.scLevelBody" :error="errors.scLevelBody">
          <template #extra>
            <select v-model="scLevel" class="control-select" @change="loadScLevelBody">
              <option v-for="lv in levelOptions" :key="lv" :value="lv">{{ lv }}</option>
            </select>
          </template>
          <BaseChart v-if="!loading.scLevelBody && !errors.scLevelBody" :option="scLevelBodyOption" height="420px" />
        </ChartCard>
      </div>
    </div>

    <!-- 5. 变速箱 × 发动机配置 -->
    <div v-show="activeTab === 'powertrain'" class="business__panel">
      <div class="biz-grid">
        <ChartCard title="变速箱×发动机搭配频次（矩形树图）" :loading="loading.ptCombo" :error="errors.ptCombo">
          <BaseChart v-if="!loading.ptCombo && !errors.ptCombo" :option="ptComboOption" height="420px" />
        </ChartCard>
        <ChartCard title="排量×变速箱分布（热力图）" :loading="loading.ptDispTrans" :error="errors.ptDispTrans">
          <BaseChart v-if="!loading.ptDispTrans && !errors.ptDispTrans" :option="ptDispTransOption" height="420px" />
        </ChartCard>
      </div>
      <div class="biz-grid">
        <ChartCard title="各变速箱类型平均排量与马力" :loading="loading.ptTransDispHp" :error="errors.ptTransDispHp">
          <BaseChart v-if="!loading.ptTransDispHp && !errors.ptTransDispHp" :option="ptTransDispHpOption" height="420px" />
        </ChartCard>
        <ChartCard title="变速箱×是否电动搭配统计" :loading="loading.ptTransElectric" :error="errors.ptTransElectric">
          <BaseChart v-if="!loading.ptTransElectric && !errors.ptTransElectric" :option="ptTransElectricOption" height="420px" />
        </ChartCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import BaseChart from '@/components/BaseChart.vue'
import ChartCard from '@/components/ChartCard.vue'
import { businessApi, basicApi } from '@/api'
// 引入分类图表构建器
import {
  barGrouped,
  barStacked,
  barHorizontal,
  barLineCombo,
  stackedArea,
  percentLine,
  heatmap as heatmapChart,
  buildHeatmapData,
  boxplot as boxplotChart,
  treemap as treemapChart,
} from '@/echarts'

const tabs = [
  { key: 'brand-level-price', label: '品牌×级别×价格', icon: '品' },
  { key: 'energy-trend', label: '上市时间×能源', icon: '时' },
  { key: 'power-match', label: '排量×燃油×马力', icon: '动' },
  { key: 'space-config', label: '车身×座位×级别', icon: '空' },
  { key: 'powertrain', label: '变速箱×发动机', icon: '变' },
]

const activeTab = ref('brand-level-price')
const levelOptions = ref(['SUV', '轿车', 'MPV', '跑车', '微面', '轻客', '微卡', '轻卡'])

const loading = reactive({})
const errors = reactive({})
const data = reactive({})

const setData = (key, value) => {
  data[key] = Array.isArray(value) ? value : (value?.data || [])
}
const setLoading = (key, val) => { loading[key] = val }
const setError = (key, val) => { errors[key] = val }

const formatValue = (val) => {
  if (val === null || val === undefined) return '-'
  if (typeof val === 'number') {
    return Number.isInteger(val) ? val.toString() : val.toFixed(2)
  }
  return String(val)
}

const fetchData = async (key, apiFn) => {
  setLoading(key, true)
  setError(key, '')
  try {
    const res = await apiFn()
    setData(key, res)
  } catch (e) {
    setError(key, `加载失败：${e.message || '未知错误'}`)
    setData(key, [])
  } finally {
    setLoading(key, false)
  }
}

// ============ 1. 品牌×级别×价格 ============
const blpLevel = ref('SUV')
const loadBlpBand = () => fetchData('blpBand', businessApi.brandLevelPriceBand)
const loadBlpSummary = () => fetchData('blpSummary', businessApi.brandLevelPriceLevelSummary)
const loadBlpByLevel = () => fetchData('blpByLevel', () => businessApi.brandLevelPriceByLevel(blpLevel.value))

// 分组柱状图：各品牌在各级别中的均价
const blpBandOption = computed(() => {
  const rows = data.blpBand || []
  const brands = [...new Set(rows.map((r) => r.brand || r.name))].slice(0, 10)
  const levels = [...new Set(rows.map((r) => r.level || r.category))]
  return barGrouped({
    categories: brands,
    series: levels.map((lv) => ({
      name: lv,
      data: brands.map((b) => {
        const row = rows.find((r) => (r.brand || r.name) === b && (r.level || r.category) === lv)
        return row ? Number(row.price_avg ?? row.avg_price ?? row.avg ?? row.price ?? 0) : 0
      }),
    })),
    yAxisName: '均价(万元)',
    rotateLabel: true,
  })
})

// 箱线图：指定级别下各品牌价格分布
const blpByLevelOption = computed(() => {
  const rows = data.blpByLevel || []
  const brands = rows.map((r) => r.brand || r.name || '')
  // 构建箱线图数据 [min, Q1, median, Q3, max]
  const boxData = rows.map((r) => {
    const min = Number(r.price_min ?? r.min_price ?? r.min ?? 0)
    const max = Number(r.price_max ?? r.max_price ?? r.max ?? 0)
    const avg = Number(r.price_avg ?? r.avg_price ?? r.avg ?? 0)
    // 若后端未提供 Q1/median/Q3，用 min/avg/avg/avg/max 近似
    const q1 = Number(r.q1 ?? r.price_min ?? r.min_price ?? r.min ?? 0)
    const median = Number(r.median ?? r.price_avg ?? r.avg_price ?? r.avg ?? 0)
    const q3 = Number(r.q3 ?? r.price_max ?? r.max_price ?? r.max ?? 0)
    return [min, q1, median, q3, max]
  })
  return boxplotChart({
    categories: brands,
    data: boxData,
    name: '价格分布',
    yAxisName: '价格(万元)',
    colors: ['#5470c6'],
  })
})

// 双轴组合图：各级别品牌数量与价格跨度
const blpSummaryOption = computed(() => {
  const rows = data.blpSummary || []
  const levels = rows.map((r) => r.level || r.name || r.category || '')
  return barLineCombo({
    categories: levels,
    bar: {
      name: '品牌数量',
      data: rows.map((r) => Number(r.brand_count ?? r.cnt ?? r.count ?? 0)),
      color: '#5470c6',
    },
    line: {
      name: '价格跨度',
      data: rows.map((r) => {
        const max = Number(r.price_max ?? r.max_price ?? r.max ?? 0)
        const min = Number(r.price_min ?? r.min_price ?? r.min ?? 0)
        return Number((max - min).toFixed(2))
      }),
      color: '#ee6666',
    },
    barAxisName: '品牌数',
    lineAxisName: '价格跨度(万)',
  })
})

// ============ 2. 上市时间×能源 ============
const loadEtYear = () => fetchData('etYear', businessApi.energyTrendByYear)
const loadEtRatio = () => fetchData('etRatio', businessApi.energyTrendNewEnergyRatio)
const loadEtSeason = () => fetchData('etSeason', businessApi.energyTrendBySeason)
const loadEtBrand = () => fetchData('etBrand', businessApi.energyTrendBrandRatio)

// 堆叠面积图：按年份统计各能源类型新车数量
const etYearOption = computed(() => {
  const rows = data.etYear || []
  const years = [...new Set(rows.map((r) => r.year || r.launch_year))].sort()
  const energyTypes = [...new Set(rows.map((r) => r.energy || r.energy_name || r.type))]
  return stackedArea({
    categories: years.map(String),
    series: energyTypes.map((et) => ({
      name: et,
      data: years.map((y) => {
        const row = rows.find((r) => (r.year || r.launch_year) == y && (r.energy || r.energy_name || r.type) === et)
        return row ? Number(row.cnt ?? row.count ?? row.value ?? 0) : 0
      }),
    })),
    yAxisName: '新车数量',
  })
})

// 百分比折线图：新能源占比
const etRatioOption = computed(() => {
  const rows = data.etRatio || []
  const years = rows.map((r) => String(r.launch_year ?? r.year ?? ''))
  return percentLine({
    categories: years,
    series: [
      {
        name: '新能源占比',
        data: rows.map((r) => Number(r.new_energy_pct ?? r.ratio ?? r.new_energy_ratio ?? r.percentage ?? 0).toFixed(2)),
      },
    ],
  })
})

// 堆叠柱状图：按季度统计
const etSeasonOption = computed(() => {
  const rows = data.etSeason || []
  const seasons = [...new Set(rows.map((r) => r.season || r.launch_season))].sort()
  const energyTypes = [...new Set(rows.map((r) => r.energy || r.energy_name || r.type))]
  return barStacked({
    categories: seasons.map(String),
    series: energyTypes.map((et) => ({
      name: et,
      data: seasons.map((s) => {
        const row = rows.find((r) => (r.season || r.launch_season) === s && (r.energy || r.energy_name || r.type) === et)
        return row ? Number(row.cnt ?? row.count ?? row.value ?? 0) : 0
      }),
    })),
    yAxisName: '数量',
  })
})

// 横向柱状图：各品牌新能源占比
const etBrandOption = computed(() => {
  const rows = (data.etBrand || []).slice(0, 15)
  return barHorizontal({
    data: rows.map((r) => ({
      name: r.brand || r.name || '',
      value: Number(r.new_energy_pct ?? r.ratio ?? r.percentage ?? r.new_energy_ratio ?? 0).toFixed(2),
    })),
    name: '新能源占比',
    xAxisName: '占比(%)',
    gradient: { from: '#73c0de', to: '#91cc75' },
  })
})

// ============ 3. 排量×燃油×马力 ============
const loadPmDispFuel = () => fetchData('pmDispFuel', businessApi.powerMatchDisplacementFuel)
const loadPmDispHp = () => fetchData('pmDispHp', businessApi.powerMatchDisplacementHorsepower)
const loadPmDetail = () => fetchData('pmDetail', businessApi.powerMatchDetail)

// 热力图：排量×燃油标号分布
const pmDispFuelOption = computed(() => {
  const rows = data.pmDispFuel || []
  if (!rows.length) return {}
  // 动态识别字段名
  const keys = Object.keys(rows[0])
  const xField = keys.find((k) => k.toLowerCase().includes('disp')) || keys[0]
  const yField = keys.find((k) => k.toLowerCase().includes('fuel')) || keys[1]
  const valueField = keys.find((k) => k === 'cnt' || k.toLowerCase().includes('count') || k.toLowerCase().includes('num')) || keys[keys.length - 1]
  const { xAxis, yAxis, data: hmData } = buildHeatmapData(rows, xField, yField, valueField)
  return heatmapChart({
    xAxis: xAxis.map(String),
    yAxis: yAxis.map(String),
    data: hmData,
    name: '车型数量',
    xAxisName: '排量(L)',
    yAxisName: '燃油标号',
    visualMapColors: ['#e0f2fe', '#0369a1'],
  })
})

// 箱线图：各排量段马力分布
const pmDispHpOption = computed(() => {
  const rows = data.pmDispHp || []
  const disps = rows.map((r) => String(r.displacement_range ?? r.displacement ?? r.displacement_L ?? r.range ?? ''))
  // 构建箱线图数据 [min, Q1, median, Q3, max]
  const boxData = rows.map((r) => {
    const min = Number(r.hp_min ?? r.min_hp ?? r.min_horsepower ?? r.min ?? 0)
    const max = Number(r.hp_max ?? r.max_hp ?? r.max_horsepower ?? r.max ?? 0)
    const avg = Number(r.hp_avg ?? r.avg_hp ?? r.avg_horsepower ?? r.avg ?? 0)
    const q1 = Number(r.q1 ?? r.hp_min ?? r.min_hp ?? min)
    const median = Number(r.median ?? r.hp_avg ?? r.avg_hp ?? avg)
    const q3 = Number(r.q3 ?? r.hp_max ?? r.max_hp ?? max)
    return [min, q1, median, q3, max]
  })
  return boxplotChart({
    categories: disps,
    data: boxData,
    name: '马力分布',
    yAxisName: '马力',
    colors: ['#91cc75'],
  })
})

// 热力图：排量×燃油×马力详细交叉
const pmDetailOption = computed(() => {
  const rows = data.pmDetail || []
  if (!rows.length) return {}
  // 动态识别字段
  const keys = Object.keys(rows[0])
  const xField = keys.find((k) => k.toLowerCase().includes('disp')) || keys[0]
  const yField = keys.find((k) => k.toLowerCase().includes('fuel')) || keys[1]
  const valueField = keys.find((k) => k === 'cnt' || k.toLowerCase().includes('count') || k.toLowerCase().includes('num') || k.toLowerCase().includes('horse')) || keys[keys.length - 1]
  const { xAxis, yAxis, data: hmData } = buildHeatmapData(rows, xField, yField, valueField)
  return heatmapChart({
    xAxis: xAxis.map(String),
    yAxis: yAxis.map(String),
    data: hmData,
    name: valueField,
    xAxisName: xField,
    yAxisName: yField,
    visualMapColors: ['#f0f9ff', '#0c4a6e'],
  })
})

// ============ 4. 车身×座位×级别 ============
const scLevel = ref('SUV')
const loadScLevelSeats = () => fetchData('scLevelSeats', businessApi.spaceConfigLevelSeats)
const loadScBodySeats = () => fetchData('scBodySeats', businessApi.spaceConfigBodyTypeSeats)
const loadScSevenSeat = () => fetchData('scSevenSeat', businessApi.spaceConfigSevenSeatRatio)
const loadScLevelBody = () => fetchData('scLevelBody', () => businessApi.spaceConfigLevelBodySeats(scLevel.value))

// 堆叠柱状图：各级别座位数分布
const scLevelSeatsOption = computed(() => {
  const rows = data.scLevelSeats || []
  const levels = [...new Set(rows.map((r) => r.level || r.name))]
  const seats = [...new Set(rows.map((r) => r.seats || r.seat_count))].sort((a, b) => a - b)
  return barStacked({
    categories: levels,
    series: seats.map((s) => ({
      name: `${s}座`,
      data: levels.map((lv) => {
        const row = rows.find((r) => (r.level || r.name) === lv && (r.seats || r.seat_count) == s)
        return row ? Number(row.cnt ?? row.count ?? row.value ?? 0) : 0
      }),
    })),
    yAxisName: '数量',
  })
})

// 堆叠柱状图：各车身结构座位数分布
const scBodySeatsOption = computed(() => {
  const rows = data.scBodySeats || []
  const bodies = [...new Set(rows.map((r) => r.body_type || r.body || r.name))]
  const seats = [...new Set(rows.map((r) => r.seats || r.seat_count))].sort((a, b) => a - b)
  return barStacked({
    categories: bodies,
    series: seats.map((s) => ({
      name: `${s}座`,
      data: bodies.map((b) => {
        const row = rows.find((r) => (r.body_type || r.body || r.name) === b && (r.seats || r.seat_count) == s)
        return row ? Number(row.cnt ?? row.count ?? row.value ?? 0) : 0
      }),
    })),
    yAxisName: '数量',
    rotateLabel: true,
  })
})

// 横向柱状图：各级别7座占比
const scSevenSeatOption = computed(() => {
  const rows = data.scSevenSeat || []
  return barHorizontal({
    data: rows.map((r) => ({
      name: r.level || r.name || '',
      value: Number(r.ratio || r.percentage || r.seven_seat_ratio || 0).toFixed(2),
    })),
    name: '7座占比',
    xAxisName: '占比(%)',
    gradient: { from: '#fac858', to: '#ee6666' },
  })
})

// 热力图：指定级别下车身×座位交叉
const scLevelBodyOption = computed(() => {
  const rows = data.scLevelBody || []
  if (!rows.length) return {}
  const keys = Object.keys(rows[0])
  const xField = keys.find((k) => k.toLowerCase().includes('body')) || keys[0]
  const yField = keys.find((k) => k.toLowerCase().includes('seat')) || keys[1]
  const valueField = keys.find((k) => k === 'cnt' || k.toLowerCase().includes('count') || k.toLowerCase().includes('num')) || keys[keys.length - 1]
  const { xAxis, yAxis, data: hmData } = buildHeatmapData(rows, xField, yField, valueField)
  return heatmapChart({
    xAxis: xAxis.map(String),
    yAxis: yAxis.map(String),
    data: hmData,
    name: '数量',
    xAxisName: xField,
    yAxisName: yField,
    visualMapColors: ['#fef3c7', '#92400e'],
  })
})

// ============ 5. 变速箱×发动机 ============
const loadPtCombo = () => fetchData('ptCombo', businessApi.powertrainTransEngineCombo)
const loadPtDispTrans = () => fetchData('ptDispTrans', businessApi.powertrainDisplacementTrans)
const loadPtTransDispHp = () => fetchData('ptTransDispHp', businessApi.powertrainTransDispHp)
const loadPtTransElectric = () => fetchData('ptTransElectric', businessApi.powertrainTransElectric)

// 矩形树图：变速箱×发动机搭配频次
const ptComboOption = computed(() => {
  const rows = (data.ptCombo || []).slice(0, 30)
  return treemapChart({
    data: rows.map((r) => ({
      name: `${r.trans_type || r.transmission || ''}-${r.engine_type || r.engine || ''}`,
      value: Number(r.cnt ?? r.count ?? r.value ?? 0),
    })),
    name: '搭配频次',
  })
})

// 热力图：排量×变速箱分布
const ptDispTransOption = computed(() => {
  const rows = data.ptDispTrans || []
  if (!rows.length) return {}
  const keys = Object.keys(rows[0])
  const xField = keys.find((k) => k.toLowerCase().includes('disp')) || keys[0]
  const yField = keys.find((k) => k.toLowerCase().includes('trans')) || keys[1]
  const valueField = keys.find((k) => k === 'cnt' || k.toLowerCase().includes('count') || k.toLowerCase().includes('num')) || keys[keys.length - 1]
  const { xAxis, yAxis, data: hmData } = buildHeatmapData(rows, xField, yField, valueField)
  return heatmapChart({
    xAxis: xAxis.map(String),
    yAxis: yAxis.map(String),
    data: hmData,
    name: '数量',
    xAxisName: '排量(L)',
    yAxisName: '变速箱类型',
    visualMapColors: ['#ede9fe', '#5b21b6'],
  })
})

// 双轴组合图：各变速箱类型平均排量与马力
const ptTransDispHpOption = computed(() => {
  const rows = data.ptTransDispHp || []
  const trans = rows.map((r) => r.trans_type || r.transmission || r.name || '')
  return barLineCombo({
    categories: trans,
    bar: {
      name: '平均排量',
      data: rows.map((r) => Number(r.disp_avg ?? r.avg_disp ?? r.avg_displacement ?? 0).toFixed(2)),
      color: '#5470c6',
    },
    line: {
      name: '平均马力',
      data: rows.map((r) => Number(r.hp_avg ?? r.avg_hp ?? r.avg_horsepower ?? 0).toFixed(2)),
      color: '#ee6666',
    },
    barAxisName: '排量(L)',
    lineAxisName: '马力',
    rotateLabel: true,
  })
})

// 堆叠柱状图：变速箱×电动/非电动
const ptTransElectricOption = computed(() => {
  const rows = data.ptTransElectric || []
  const trans = [...new Set(rows.map((r) => r.trans_type || r.transmission || r.type))]
  return barStacked({
    categories: trans,
    series: [
      {
        name: '电动',
        data: trans.map((t) => {
          const row = rows.find((r) => (r.trans_type || r.transmission || r.type) === t && (r.power_type === '电动' || r.is_electric == 1 || r.electric === '是'))
          return row ? Number(row.cnt ?? row.count ?? row.value ?? 0) : 0
        }),
      },
      {
        name: '非电动',
        data: trans.map((t) => {
          const row = rows.find((r) => (r.trans_type || r.transmission || r.type) === t && (r.power_type === '燃油' || r.is_electric == 0 || r.electric === '否'))
          return row ? Number(row.cnt ?? row.count ?? row.value ?? 0) : 0
        }),
      },
    ],
    yAxisName: '数量',
    rotateLabel: true,
  })
})

// 加载级别选项
const loadLevelOptions = async () => {
  try {
    const res = await basicApi.getLevelDistribution()
    const list = Array.isArray(res) ? res : (res?.data || [])
    levelOptions.value = list.map((item) => item.level || item.name || item.key).filter(Boolean)
    if (!levelOptions.value.length) {
      levelOptions.value = ['SUV', '轿车', 'MPV', '跑车', '微面', '轻客', '微卡', '轻卡']
    }
    // 设置初始选中第一个有效级别
    const firstLevel = levelOptions.value[0]
    if (firstLevel) {
      blpLevel.value = firstLevel
      scLevel.value = firstLevel
    }
  } catch (e) {
    // 使用默认值
  }
  // 加载依赖级别选择的数据
  loadBlpByLevel()
  loadScLevelBody()
}

// 切换 tab 时触发图表 resize（解决 v-show 隐藏时宽度为 0 的问题）
watch(activeTab, () => {
  nextTick(() => {
    window.dispatchEvent(new Event('resize'))
  })
})

onMounted(() => {
  loadLevelOptions()
  // 加载所有业务数据（blpByLevel 和 scLevelBody 在 loadLevelOptions 中加载）
  loadBlpBand()
  loadBlpSummary()
  loadEtYear()
  loadEtRatio()
  loadEtSeason()
  loadEtBrand()
  loadPmDispFuel()
  loadPmDispHp()
  loadPmDetail()
  loadScLevelSeats()
  loadScBodySeats()
  loadScSevenSeat()
  loadPtCombo()
  loadPtDispTrans()
  loadPtTransDispHp()
  loadPtTransElectric()
})
</script>

<style scoped>
.business {
  padding: 4px;
}

.business__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.06);
}

.biz-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #6b7280;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.biz-tab:hover {
  color: #5470c6;
  border-color: #5470c6;
}

.biz-tab--active {
  background: linear-gradient(135deg, #5470c6, #73c0de);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(84, 112, 198, 0.3);
}

.biz-tab__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.06);
  font-size: 12px;
  font-weight: bold;
}

.biz-tab--active .biz-tab__icon {
  background: rgba(255, 255, 255, 0.3);
}

.biz-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 1024px) {
  .biz-grid {
    grid-template-columns: 1fr;
  }
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
</style>
