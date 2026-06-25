<template>
  <div class="compare-page">
    <div class="compare-tabs">
      <button
        class="compare-tab"
        :class="{ 'compare-tab--active': compareType === 'brand' }"
        @click="compareType = 'brand'"
      >品牌对比</button>
      <button
        class="compare-tab"
        :class="{ 'compare-tab--active': compareType === 'level' }"
        @click="compareType = 'level'"
      >级别对比</button>
    </div>

    <!-- 品牌对比 -->
    <div v-if="compareType === 'brand'" class="compare-panel">
      <div class="compare-selectors">
        <div class="compare-selector">
          <label class="selector-label">品牌 A</label>
          <select v-model="brand1" class="selector-select" @change="doCompareBrands">
            <option value="">请选择</option>
            <option v-for="b in brandOptions" :key="b.brand" :value="b.brand">{{ b.brand }}</option>
          </select>
        </div>
        <div class="compare-vs">VS</div>
        <div class="compare-selector">
          <label class="selector-label">品牌 B</label>
          <select v-model="brand2" class="selector-select" @change="doCompareBrands">
            <option value="">请选择</option>
            <option v-for="b in brandOptions" :key="b.brand" :value="b.brand">{{ b.brand }}</option>
          </select>
        </div>
      </div>

      <div v-if="compareData.stats && compareData.stats.length === 2" class="compare-results">
        <div class="compare-cards">
          <div class="compare-card" v-for="(stat, idx) in compareData.stats" :key="idx">
            <div class="compare-card__header">
              <span class="compare-card__rank" :class="idx === 0 ? 'rank-gold' : 'rank-silver'">
                {{ idx === 0 ? 'A' : 'B' }}
              </span>
              <span class="compare-card__name">{{ stat.brand }}</span>
            </div>
            <div class="compare-card__metrics">
              <div class="metric">
                <span class="metric__label">车型数</span>
                <span class="metric__value">{{ stat.total }}</span>
              </div>
              <div class="metric">
                <span class="metric__label">均价</span>
                <span class="metric__value">{{ stat.avg_price }} 万</span>
              </div>
              <div class="metric">
                <span class="metric__label">价格区间</span>
                <span class="metric__value">{{ stat.min_price }} - {{ stat.max_price }} 万</span>
              </div>
              <div class="metric">
                <span class="metric__label">平均排量</span>
                <span class="metric__value">{{ stat.avg_displacement }} L</span>
              </div>
              <div class="metric">
                <span class="metric__label">平均马力</span>
                <span class="metric__value">{{ stat.avg_horsepower }} 匹</span>
              </div>
              <div class="metric">
                <span class="metric__label">平均档位</span>
                <span class="metric__value">{{ stat.avg_gear_count }} 挡</span>
              </div>
            </div>
          </div>
        </div>

        <ChartCard title="级别分布对比" v-if="compareData.levelDistribution">
          <BaseChart :option="brandLevelOption" height="360px" />
        </ChartCard>

        <ChartCard title="能源类型对比" v-if="compareData.energyDistribution">
          <BaseChart :option="brandEnergyOption" height="360px" />
        </ChartCard>
      </div>

      <div v-else-if="!brand1 || !brand2" class="empty-tip">请选择两个品牌进行对比</div>
    </div>

    <!-- 级别对比 -->
    <div v-if="compareType === 'level'" class="compare-panel">
      <div class="compare-selectors">
        <div class="compare-selector">
          <label class="selector-label">级别 A</label>
          <select v-model="level1" class="selector-select" @change="doCompareLevels">
            <option value="">请选择</option>
            <option v-for="l in levelOptions" :key="l.level" :value="l.level">{{ l.level }}</option>
          </select>
        </div>
        <div class="compare-vs">VS</div>
        <div class="compare-selector">
          <label class="selector-label">级别 B</label>
          <select v-model="level2" class="selector-select" @change="doCompareLevels">
            <option value="">请选择</option>
            <option v-for="l in levelOptions" :key="l.level" :value="l.level">{{ l.level }}</option>
          </select>
        </div>
      </div>

      <div v-if="compareData.stats && compareData.stats.length === 2" class="compare-results">
        <div class="compare-cards">
          <div class="compare-card" v-for="(stat, idx) in compareData.stats" :key="idx">
            <div class="compare-card__header">
              <span class="compare-card__rank" :class="idx === 0 ? 'rank-gold' : 'rank-silver'">
                {{ idx === 0 ? 'A' : 'B' }}
              </span>
              <span class="compare-card__name">{{ stat.level }}</span>
            </div>
            <div class="compare-card__metrics">
              <div class="metric">
                <span class="metric__label">车型数</span>
                <span class="metric__value">{{ stat.total }}</span>
              </div>
              <div class="metric">
                <span class="metric__label">品牌数</span>
                <span class="metric__value">{{ stat.brands }}</span>
              </div>
              <div class="metric">
                <span class="metric__label">均价</span>
                <span class="metric__value">{{ stat.avg_price }} 万</span>
              </div>
              <div class="metric">
                <span class="metric__label">价格区间</span>
                <span class="metric__value">{{ stat.min_price }} - {{ stat.max_price }} 万</span>
              </div>
              <div class="metric">
                <span class="metric__label">平均排量</span>
                <span class="metric__value">{{ stat.avg_displacement }} L</span>
              </div>
              <div class="metric">
                <span class="metric__label">平均马力</span>
                <span class="metric__value">{{ stat.avg_horsepower }} 匹</span>
              </div>
            </div>
          </div>
        </div>

        <ChartCard title="能源类型对比" v-if="compareData.energyDistribution">
          <BaseChart :option="levelEnergyOption" height="360px" />
        </ChartCard>
      </div>

      <div v-else-if="!level1 || !level2" class="empty-tip">请选择两个级别进行对比</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import BaseChart from '@/components/BaseChart.vue'
import ChartCard from '@/components/ChartCard.vue'
import { basicApi } from '@/api'
import { barGrouped, barStacked } from '@/echarts'

const compareType = ref('brand')
const brand1 = ref('')
const brand2 = ref('')
const level1 = ref('')
const level2 = ref('')
const brandOptions = ref([])
const levelOptions = ref([])
const loading = ref(false)
const compareData = reactive({ stats: [], levelDistribution: [], energyDistribution: [] })

const brandLevelOption = computed(() => {
  const rows = compareData.levelDistribution || []
  if (!rows.length) return {}
  const levels = [...new Set(rows.map((r) => r.level))]
  const brands = [...new Set(rows.map((r) => r.brand))]
  return barGrouped({
    categories: levels,
    series: brands.map((b) => ({
      name: b,
      data: levels.map((l) => {
        const row = rows.find((r) => r.brand === b && r.level === l)
        return row ? Number(row.cnt || 0) : 0
      }),
    })),
    yAxisName: '车型数',
    rotateLabel: true,
  })
})

const brandEnergyOption = computed(() => {
  const rows = compareData.energyDistribution || []
  if (!rows.length) return {}
  const energies = [...new Set(rows.map((r) => r.energy_name))]
  const brands = [...new Set(rows.map((r) => r.brand))]
  return barStacked({
    categories: brands,
    series: energies.map((e) => ({
      name: e,
      data: brands.map((b) => {
        const row = rows.find((r) => r.brand === b && r.energy_name === e)
        return row ? Number(row.cnt || 0) : 0
      }),
    })),
    yAxisName: '车型数',
  })
})

const levelEnergyOption = computed(() => {
  const rows = compareData.energyDistribution || []
  if (!rows.length) return {}
  const energies = [...new Set(rows.map((r) => r.energy_name))]
  const levels = [...new Set(rows.map((r) => r.level))]
  return barStacked({
    categories: levels,
    series: energies.map((e) => ({
      name: e,
      data: levels.map((l) => {
        const row = rows.find((r) => r.level === l && r.energy_name === e)
        return row ? Number(row.cnt || 0) : 0
      }),
    })),
    yAxisName: '车型数',
  })
})

const doCompareBrands = async () => {
  if (!brand1.value || !brand2.value) return
  loading.value = true
  try {
    const res = await basicApi.compareBrands(brand1.value, brand2.value)
    compareData.stats = res.stats || []
    compareData.levelDistribution = res.levelDistribution || []
    compareData.energyDistribution = res.energyDistribution || []
  } catch (e) {
    compareData.stats = []
  } finally {
    loading.value = false
  }
}

const doCompareLevels = async () => {
  if (!level1.value || !level2.value) return
  loading.value = true
  try {
    const res = await basicApi.compareLevels(level1.value, level2.value)
    compareData.stats = res.stats || []
    compareData.levelDistribution = []
    compareData.energyDistribution = res.energyDistribution || []
  } catch (e) {
    compareData.stats = []
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  try {
    const [brands, levels] = await Promise.all([basicApi.getBrands(), basicApi.getLevels()])
    brandOptions.value = Array.isArray(brands) ? brands : (brands?.data || [])
    levelOptions.value = Array.isArray(levels) ? levels : (levels?.data || [])
  } catch (e) {}
}

onMounted(loadOptions)
</script>

<style scoped>
.compare-page {
  padding: 4px;
}

.compare-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.06);
}

.compare-tab {
  flex: 1;
  padding: 12px 20px;
  border: none;
  background: #fff;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.compare-tab:hover {
  color: #5470c6;
}

.compare-tab--active {
  background: #5470c6;
  color: #fff;
}

.compare-panel {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
}

.compare-selectors {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 24px;
  justify-content: center;
}

.compare-selector {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.selector-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.selector-select {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 14px;
  color: #374151;
  min-width: 200px;
  cursor: pointer;
}

.selector-select:focus {
  outline: none;
  border-color: #5470c6;
}

.compare-vs {
  font-size: 24px;
  font-weight: bold;
  color: #ee6666;
  padding-bottom: 8px;
}

.compare-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.compare-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
}

.compare-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.compare-card__rank {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  color: #fff;
}

.rank-gold {
  background: linear-gradient(135deg, #fac858, #91cc75);
}

.rank-silver {
  background: linear-gradient(135deg, #73c0de, #5470c6);
}

.compare-card__name {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.compare-card__metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.metric__label {
  font-size: 11px;
  color: #9ca3af;
}

.metric__value {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.compare-results {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-tip {
  padding: 60px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}

@media (max-width: 768px) {
  .compare-selectors {
    flex-direction: column;
    align-items: stretch;
  }
  .compare-vs {
    text-align: center;
    padding: 0;
  }
  .compare-cards {
    grid-template-columns: 1fr;
  }
}
</style>