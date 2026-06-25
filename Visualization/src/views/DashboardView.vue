<template>
  <div class="dashboard">
    <!-- 顶部数据源状态卡片 -->
    <div class="dashboard__status">
      <div class="status-card">
        <div class="status-card__icon" :class="`status-card__icon--${dataSource.status}`">
          <span v-if="dataSource.status === 'ok' || dataSource.label === 'Hive 数据仓库'">H</span>
          <span v-else-if="dataSource.status === 'fallback'">H2</span>
          <span v-else>?</span>
        </div>
        <div class="status-card__info">
          <div class="status-card__label">当前数据源</div>
          <div class="status-card__value">{{ dataSource.label || '加载中...' }}</div>
          <div class="status-card__tag" :class="`status-card__tag--${dataSource.status}`">
            {{ statusText }}
          </div>
        </div>
      </div>
      <div class="status-card">
        <div class="status-card__icon status-card__icon--brand">
          <span>品</span>
        </div>
        <div class="status-card__info">
          <div class="status-card__label">总品牌数</div>
          <div class="status-card__value">{{ overview.brands || 0 }} 个</div>
          <div class="status-card__tag">品牌覆盖</div>
        </div>
      </div>
      <div class="status-card">
        <div class="status-card__icon status-card__icon--price">
          <span>价</span>
        </div>
        <div class="status-card__info">
          <div class="status-card__label">均价 / 价格区间</div>
          <div class="status-card__value">{{ overview.avg_price || 0 }} 万</div>
          <div class="status-card__tag">{{ overview.min_price || 0 }} - {{ overview.max_price || 0 }} 万</div>
        </div>
      </div>
      <div class="status-card">
        <div class="status-card__icon status-card__icon--energy">
          <span>总</span>
        </div>
        <div class="status-card__info">
          <div class="status-card__label">车型总量</div>
          <div class="status-card__value">{{ (overview.total || 0).toLocaleString() }}</div>
          <div class="status-card__tag">全量数据</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="dashboard__grid">
      <div class="dashboard__item dashboard__item--full">
        <ChartCard title="品牌车型数量 Top 10" :loading="loading.brand" :error="errors.brand">
          <BaseChart v-if="!loading.brand && !errors.brand" :option="brandOption" height="380px" />
        </ChartCard>
      </div>

      <div class="dashboard__item">
        <ChartCard title="价格区间分布" :loading="loading.price" :error="errors.price">
          <BaseChart v-if="!loading.price && !errors.price" :option="priceOption" height="360px" />
        </ChartCard>
      </div>

      <div class="dashboard__item">
        <ChartCard title="能源类型分布" :loading="loading.energy" :error="errors.energy">
          <BaseChart v-if="!loading.energy && !errors.energy" :option="energyOption" height="360px" />
        </ChartCard>
      </div>

      <div class="dashboard__item dashboard__item--full">
        <ChartCard title="车型级别分布" :loading="loading.level" :error="errors.level">
          <BaseChart v-if="!loading.level && !errors.level" :option="levelOption" height="360px" />
        </ChartCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import BaseChart from '@/components/BaseChart.vue'
import ChartCard from '@/components/ChartCard.vue'
import { basicApi } from '@/api'
// 引入分类图表构建器
import {
  barHorizontal,
  barVertical,
  doughnut,
  rose,
  colorPalette,
} from '@/echarts'

const dataSource = ref({ source: '', label: '', status: '' })
const overview = ref({})
const brandData = ref([])
const priceData = ref([])
const energyData = ref([])
const levelData = ref([])

const loading = reactive({
  brand: false,
  price: false,
  energy: false,
  level: false,
})

const errors = reactive({
  brand: '',
  price: '',
  energy: '',
  level: '',
})

const statusText = computed(() => {
  const map = { ok: '正常', fallback: '降级', error: '异常' }
  return map[dataSource.value.status] || dataSource.value.status || ''
})

// 品牌排名 - 横向柱状图（适合排名场景）
const brandOption = computed(() =>
  barHorizontal({
    data: brandData.value.map((item) => ({
      name: item.brand || item.name || item.key,
      value: item.cnt ?? item.count ?? item.value,
    })),
    name: '车型数量',
    xAxisName: '车型数量',
    gradient: { from: '#73c0de', to: '#5470c6' },
  })
)

// 价格区间分布 - 环形图（适合有序区间占比）
const priceOption = computed(() =>
  doughnut({
    data: priceData.value.map((item) => ({
      name: item.price_range || item.range || item.name || item.key,
      value: item.cnt ?? item.count ?? item.value,
    })),
    name: '价格区间分布',
    colors: colorPalette,
  })
)

// 能源分布 - 玫瑰图（适合占比 + 数值大小）
const energyOption = computed(() =>
  rose({
    data: energyData.value.map((item) => ({
      name: item.energy_name || item.energy || item.name || item.key,
      value: item.cnt ?? item.count ?? item.value,
    })),
    name: '能源类型分布',
  })
)

// 级别分布 - 纵向柱状图（适合分类比较）
const levelOption = computed(() =>
  barVertical({
    data: levelData.value.map((item) => ({
      name: item.level || item.name || item.key,
      value: item.cnt ?? item.count ?? item.value,
    })),
    name: '车型数量',
    yAxisName: '车型数量',
    rotateLabel: levelData.value.length > 8,
    gradient: { from: '#91cc75', to: '#73c0de' },
  })
)

const loadDataSource = async () => {
  try {
    const res = await basicApi.getDataSource()
    dataSource.value = res || {}
  } catch (e) {
    dataSource.value = { source: 'error', label: '连接失败', status: 'error' }
  }
}

const loadOverview = async () => {
  try {
    const res = await basicApi.getOverview()
    overview.value = res || {}
  } catch (e) {
    overview.value = {}
  }
}

const loadBrand = async () => {
  loading.brand = true
  errors.brand = ''
  try {
    const res = await basicApi.getBrandRanking()
    brandData.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (e) {
    errors.brand = '加载失败：' + (e.message || '未知错误')
  } finally {
    loading.brand = false
  }
}

const loadPrice = async () => {
  loading.price = true
  errors.price = ''
  try {
    const res = await basicApi.getPriceDistribution()
    priceData.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (e) {
    errors.price = '加载失败：' + (e.message || '未知错误')
  } finally {
    loading.price = false
  }
}

const loadEnergy = async () => {
  loading.energy = true
  errors.energy = ''
  try {
    const res = await basicApi.getEnergyDistribution()
    energyData.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (e) {
    errors.energy = '加载失败：' + (e.message || '未知错误')
  } finally {
    loading.energy = false
  }
}

const loadLevel = async () => {
  loading.level = true
  errors.level = ''
  try {
    const res = await basicApi.getLevelDistribution()
    levelData.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (e) {
    errors.level = '加载失败：' + (e.message || '未知错误')
  } finally {
    loading.level = false
  }
}

const loadAll = () => {
  loadDataSource()
  loadOverview()
  loadBrand()
  loadPrice()
  loadEnergy()
  loadLevel()
}

onMounted(loadAll)

defineExpose({ refresh: loadAll })
</script>

<style scoped>
.dashboard {
  padding: 4px;
}

.dashboard__status {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.status-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all 0.3s;
}

.status-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.12);
}

.status-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  flex-shrink: 0;
}

.status-card__icon--ok {
  background: linear-gradient(135deg, #91cc75, #73c0de);
}

.status-card__icon--fallback {
  background: linear-gradient(135deg, #fac858, #ee6666);
}

.status-card__icon--error {
  background: linear-gradient(135deg, #ee6666, #c0392b);
}

.status-card__icon--brand {
  background: linear-gradient(135deg, #5470c6, #73c0de);
}

.status-card__icon--price {
  background: linear-gradient(135deg, #fac858, #91cc75);
}

.status-card__icon--energy {
  background: linear-gradient(135deg, #fc8452, #ee6666);
}

.status-card__info {
  flex: 1;
  min-width: 0;
}

.status-card__label {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 4px;
}

.status-card__value {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-card__tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  background: #e0f2fe;
  color: #0369a1;
}

.status-card__tag--ok {
  background: #dcfce7;
  color: #166534;
}

.status-card__tag--fallback {
  background: #fef3c7;
  color: #92400e;
}

.status-card__tag--error {
  background: #fee2e2;
  color: #991b1b;
}

.dashboard__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.dashboard__item {
  min-width: 0;
}

.dashboard__item--full {
  grid-column: 1 / -1;
}

@media (max-width: 768px) {
  .dashboard__grid {
    grid-template-columns: 1fr;
  }
}
</style>
