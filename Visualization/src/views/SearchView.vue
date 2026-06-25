<template>
  <div class="search-page">
    <!-- 搜索条件 -->
    <div class="search-panel">
      <div class="search-panel__header">
        <h2 class="search-panel__title">车辆搜索</h2>
        <span class="search-panel__hint">支持多条件组合筛选，查询结果可导出 CSV</span>
      </div>
      <div class="search-filters">
        <div class="filter-group">
          <label class="filter-label">品牌</label>
          <select v-model="filters.brand" class="filter-select">
            <option value="">全部</option>
            <option v-for="b in brandOptions" :key="b.brand" :value="b.brand">{{ b.brand }}</option>
          </select>
        </div>
        <div class="filter-group">
          <label class="filter-label">级别</label>
          <select v-model="filters.level" class="filter-select">
            <option value="">全部</option>
            <option v-for="l in levelOptions" :key="l.level" :value="l.level">{{ l.level }}</option>
          </select>
        </div>
        <div class="filter-group">
          <label class="filter-label">最低价(万)</label>
          <input v-model.number="filters.minPrice" type="number" class="filter-input" placeholder="0" />
        </div>
        <div class="filter-group">
          <label class="filter-label">最高价(万)</label>
          <input v-model.number="filters.maxPrice" type="number" class="filter-input" placeholder="不限" />
        </div>
        <div class="filter-group">
          <label class="filter-label">能源类型</label>
          <select v-model="filters.energy" class="filter-select">
            <option value="">全部</option>
            <option value="汽油">汽油</option>
            <option value="柴油">柴油</option>
            <option value="纯电动">纯电动</option>
            <option value="插电式混合动力">插电式混合动力</option>
            <option value="油电混合">油电混合</option>
          </select>
        </div>
        <div class="filter-group">
          <label class="filter-label">变速箱</label>
          <select v-model="filters.transType" class="filter-select">
            <option value="">全部</option>
            <option value="手动">手动</option>
            <option value="自动">自动</option>
            <option value="双离合">双离合</option>
            <option value="无级变速">无级变速</option>
          </select>
        </div>
        <div class="filter-group filter-group--actions">
          <button class="btn btn--primary" @click="doSearch" :disabled="loading">
            {{ loading ? '搜索中...' : '搜索' }}
          </button>
          <button class="btn btn--default" @click="resetFilters">重置</button>
          <button class="btn btn--default" @click="exportCsv" :disabled="!searchData.length">
            导出 CSV
          </button>
        </div>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div class="search-results">
      <div class="results-header">
        <span class="results-count" v-if="totalCount > 0">
          共找到 <strong>{{ totalCount.toLocaleString() }}</strong> 条结果，当前显示
          <select v-model="pageSize" class="results-select" @change="doSearch">
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
          条
        </span>
        <div class="results-pagination" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">上一页</button>
          <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">下一页</button>
        </div>
      </div>

      <div v-if="error" class="search-error">{{ error }}</div>

      <div v-if="searchData.length" class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>#</th>
              <th v-for="col in displayColumns" :key="col">{{ col }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in searchData" :key="idx">
              <td>{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td v-for="col in displayColumns" :key="col">
                <template v-if="col === 'price_wan'">{{ formatPrice(row[col]) }}</template>
                <template v-else-if="col === 'launch_date'">{{ formatDate(row[col]) }}</template>
                <template v-else>{{ formatValue(row[col]) }}</template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="!loading && searched" class="empty-tip">未找到匹配的车辆数据</div>
      <div v-else-if="!loading && !searched" class="empty-tip">输入筛选条件后点击搜索</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { basicApi, dataApi } from '@/api'

const brandOptions = ref([])
const levelOptions = ref([])
const searchData = ref([])
const totalCount = ref(0)
const loading = ref(false)
const error = ref('')
const searched = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)

const filters = reactive({
  brand: '',
  level: '',
  minPrice: null,
  maxPrice: null,
  energy: '',
  transType: '',
})

const totalPages = ref(1)

const displayColumns = [
  'brand', 'level', 'energy_name', 'price_wan', 'launch_date',
  'displacement_L', 'horsepower', 'gear_count', 'trans_type', 'seats', 'body_type',
]

const doSearch = async () => {
  loading.value = true
  error.value = ''
  searched.value = true
  try {
    const params = {
      limit: pageSize.value,
      offset: (currentPage.value - 1) * pageSize.value,
    }
    if (filters.brand) params.brand = filters.brand
    if (filters.level) params.level = filters.level
    if (filters.minPrice != null && filters.minPrice !== '') params.minPrice = filters.minPrice
    if (filters.maxPrice != null && filters.maxPrice !== '') params.maxPrice = filters.maxPrice
    if (filters.energy) params.energy = filters.energy
    if (filters.transType) params.transType = filters.transType
    const res = await basicApi.search(params)
    searchData.value = res.data || []
    totalCount.value = res.total || 0
    totalPages.value = Math.ceil(totalCount.value / pageSize.value) || 1
  } catch (e) {
    error.value = '搜索失败：' + (e.message || '未知错误')
    searchData.value = []
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.brand = ''
  filters.level = ''
  filters.minPrice = null
  filters.maxPrice = null
  filters.energy = ''
  filters.transType = ''
  currentPage.value = 1
  searchData.value = []
  totalCount.value = 0
  searched.value = false
}

const goPage = (page) => {
  currentPage.value = page
  doSearch()
}

const exportCsv = async () => {
  try {
    const params = {}
    if (filters.brand) params.brand = filters.brand
    if (filters.level) params.level = filters.level
    if (filters.minPrice != null) params.minPrice = filters.minPrice
    if (filters.maxPrice != null) params.maxPrice = filters.maxPrice
    if (filters.energy) params.energy = filters.energy
    if (filters.transType) params.transType = filters.transType
    const blob = await dataApi.exportData(params)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `car_data_${Date.now()}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    error.value = '导出失败：' + (e.message || '未知错误')
  }
}

const formatValue = (val) => {
  if (val === null || val === undefined) return '-'
  return String(val)
}

const formatPrice = (val) => {
  if (val === null || val === undefined) return '-'
  return Number(val).toFixed(2) + ' 万'
}

const formatDate = (val) => {
  if (!val) return '-'
  const s = String(val)
  if (s.length >= 10) return s.slice(0, 10)
  return s
}

const loadOptions = async () => {
  try {
    const [brands, levels] = await Promise.all([basicApi.getBrands(), basicApi.getLevels()])
    brandOptions.value = Array.isArray(brands) ? brands : (brands?.data || [])
    levelOptions.value = Array.isArray(levels) ? levels : (levels?.data || [])
  } catch (e) {
    // 选项加载失败不阻塞
  }
}

onMounted(loadOptions)
</script>

<style scoped>
.search-page {
  padding: 4px;
}

.search-panel {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.search-panel__header {
  margin-bottom: 16px;
}

.search-panel__title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
  padding-left: 12px;
  border-left: 4px solid #5470c6;
}

.search-panel__hint {
  font-size: 12px;
  color: #9ca3af;
  padding-left: 16px;
}

.search-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.filter-select,
.filter-input {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 13px;
  color: #374151;
  background: #fff;
  min-width: 120px;
}

.filter-select:focus,
.filter-input:focus {
  outline: none;
  border-color: #5470c6;
}

.filter-input {
  width: 100px;
}

.filter-group--actions {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.btn {
  padding: 7px 16px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.btn:hover:not(:disabled) {
  border-color: #5470c6;
  color: #5470c6;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.btn--primary {
  background: #5470c6;
  color: #fff;
  border-color: #5470c6;
}

.btn--primary:hover:not(:disabled) {
  background: #4060b0;
}

.btn--default {
  background: #fff;
}

.search-results {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.results-count {
  font-size: 14px;
  color: #6b7280;
}

.results-count strong {
  color: #5470c6;
}

.results-select {
  padding: 3px 6px;
  border: 1px solid #d1d5db;
  border-radius: 3px;
  font-size: 13px;
}

.results-pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-btn {
  padding: 5px 12px;
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #374151;
}

.page-btn:hover:not(:disabled) {
  border-color: #5470c6;
  color: #5470c6;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #6b7280;
}

.search-error {
  padding: 12px;
  background: #fee2e2;
  color: #991b1b;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 13px;
}

.table-wrapper {
  overflow: auto;
  max-height: 600px;
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
  color: #9ca3af;
  font-size: 12px;
  width: 40px;
}

.empty-tip {
  padding: 60px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}
</style>