import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: { title: '基础统计' },
    },
    {
      path: '/advanced',
      name: 'advanced',
      component: () => import('../views/AdvancedAnalysisView.vue'),
      meta: { title: '高级分析' },
    },
    {
      path: '/business',
      name: 'business',
      component: () => import('../views/BusinessAnalysisView.vue'),
      meta: { title: '业务关联分析' },
    },
    {
      path: '/data',
      name: 'data',
      component: () => import('../views/DataManagementView.vue'),
      meta: { title: '数据管理' },
    },
    {
      path: '/search',
      name: 'search',
      component: () => import('../views/SearchView.vue'),
      meta: { title: '车辆搜索' },
    },
    {
      path: '/compare',
      name: 'compare',
      component: () => import('../views/CompareView.vue'),
      meta: { title: '对比分析' },
    },
  ],
})

export default router
