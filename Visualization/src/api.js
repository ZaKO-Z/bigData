/**
 * API 统一管理文件
 * 所有接口集中定义，Vue 组件按名称调用
 *
 * 服务器地址配置：修改下方 BASE_URL 或 vite.config.js 中的 proxy target
 */
import request from '@/utils/request'

/**
 * 基础统计接口（5个）
 */
export const basicApi = {
  getDataSource: () => request.get('/cars/datasource'),
  getOverview: () => request.get('/cars/overview'),
  getBrandRanking: () => request.get('/cars/brand-ranking'),
  getPriceDistribution: () => request.get('/cars/price-distribution'),
  getEnergyDistribution: () => request.get('/cars/energy-distribution'),
  getLevelDistribution: () => request.get('/cars/level-distribution'),
  getBrands: () => request.get('/cars/brands'),
  getLevels: () => request.get('/cars/levels'),
  search: (params) => request.get('/cars/search', { params }),
  compareBrands: (brand1, brand2) =>
    request.get('/cars/compare/brands', { params: { brand1, brand2 } }),
  compareLevels: (level1, level2) =>
    request.get('/cars/compare/levels', { params: { level1, level2 } }),
}

/**
 * 高级分析 - 分类维度接口（13个）
 */
export const groupApi = {
  getByDimension: (dimension) => request.get(`/cars/advanced/group/${dimension}`),
  brand: () => request.get('/cars/advanced/group/brand'),
  level: () => request.get('/cars/advanced/group/level'),
  energy: () => request.get('/cars/advanced/group/energy'),
  bodyType: () => request.get('/cars/advanced/group/body-type'),
  transType: () => request.get('/cars/advanced/group/trans-type'),
  transSubtype: () => request.get('/cars/advanced/group/trans-subtype'),
  fuelGrade: () => request.get('/cars/advanced/group/fuel-grade'),
  engineLayout: () => request.get('/cars/advanced/group/engine-layout'),
  warrantyUnlimited: () => request.get('/cars/advanced/group/warranty-unlimited'),
  launchYear: () => request.get('/cars/advanced/group/launch-year'),
  priceRange: () => request.get('/cars/advanced/group/price-range'),
  seats: () => request.get('/cars/advanced/group/seats'),
  gearCount: () => request.get('/cars/advanced/group/gear-count'),
}

/**
 * 高级分析 - 排序度量接口（9个）
 */
export const sortApi = {
  getByMetric: (metric, params = {}) =>
    request.get(`/cars/advanced/sort/${metric}`, { params }),
  launchDate: (params) => request.get('/cars/advanced/sort/launch-date', { params }),
  price: (params) => request.get('/cars/advanced/sort/price', { params }),
  displacement: (params) => request.get('/cars/advanced/sort/displacement', { params }),
  horsepower: (params) => request.get('/cars/advanced/sort/horsepower', { params }),
  doors: (params) => request.get('/cars/advanced/sort/doors', { params }),
  seats: (params) => request.get('/cars/advanced/sort/seats', { params }),
  gearCount: (params) => request.get('/cars/advanced/sort/gear-count', { params }),
  warrantyYears: (params) => request.get('/cars/advanced/sort/warranty-years', { params }),
  warrantyKm: (params) => request.get('/cars/advanced/sort/warranty-km', { params }),
}

/**
 * 业务关联分析接口（17个）
 */
export const businessApi = {
  // 品牌 × 级别 × 指导价
  brandLevelPriceBand: () => request.get('/cars/biz/brand-level-price/band'),
  brandLevelPriceByLevel: (level) =>
    request.get('/cars/biz/brand-level-price/by-level', { params: { level } }),
  brandLevelPriceLevelSummary: () => request.get('/cars/biz/brand-level-price/level-summary'),

  // 上市时间 × 能源类型
  energyTrendByYear: () => request.get('/cars/biz/energy-trend/by-year'),
  energyTrendNewEnergyRatio: () => request.get('/cars/biz/energy-trend/new-energy-ratio'),
  energyTrendBySeason: () => request.get('/cars/biz/energy-trend/by-season'),
  energyTrendBrandRatio: () => request.get('/cars/biz/energy-trend/brand-ratio'),

  // 排量 × 燃油标号 × 马力
  powerMatchDisplacementFuel: () => request.get('/cars/biz/power-match/displacement-fuel'),
  powerMatchDisplacementHorsepower: () => request.get('/cars/biz/power-match/displacement-horsepower'),
  powerMatchDetail: () => request.get('/cars/biz/power-match/detail'),

  // 车身结构 × 座位数 × 级别
  spaceConfigLevelSeats: () => request.get('/cars/biz/space-config/level-seats'),
  spaceConfigBodyTypeSeats: () => request.get('/cars/biz/space-config/body-type-seats'),
  spaceConfigLevelBodySeats: (level) =>
    request.get('/cars/biz/space-config/level-body-seats', { params: { level } }),
  spaceConfigSevenSeatRatio: () => request.get('/cars/biz/space-config/seven-seat-ratio'),

  // 变速箱 × 发动机配置
  powertrainTransEngineCombo: () => request.get('/cars/biz/powertrain/trans-engine-combo'),
  powertrainDisplacementTrans: () => request.get('/cars/biz/powertrain/displacement-trans'),
  powertrainTransDispHp: () => request.get('/cars/biz/powertrain/trans-disp-hp'),
  powertrainTransElectric: () => request.get('/cars/biz/powertrain/trans-electric'),
}

/**
 * 数据分析接口（7个）
 */
export const dataApi = {
  upload: (formData, onUploadProgress) =>
    request.post('/data/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
    }),
  import: (formData, onUploadProgress) =>
    request.post('/data/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
    }),
  preview: (params = {}) => request.get('/data/preview', { params }),
  generateReport: () => request.get('/data/report/generate', { timeout: 120000 }),
  saveReport: () => request.post('/data/report/save', null, { timeout: 120000 }),
  reportList: () => request.get('/data/report/list'),
  downloadReport: (filename) =>
    request.get('/data/report/download', {
      params: { filename },
      responseType: 'blob',
    }),
  exportData: (params = {}) =>
    request.get('/data/export', {
      params,
      responseType: 'blob',
    }),
}

/**
 * 统一导出所有 API
 * 组件中按需导入：import { basicApi, businessApi } from '@/api'
 */
export default {
  basicApi,
  groupApi,
  sortApi,
  businessApi,
  dataApi,
}
