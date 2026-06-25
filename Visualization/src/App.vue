<script setup>
import { RouterLink, RouterView } from 'vue-router'

const navItems = [
  { path: '/dashboard', label: '基础统计', icon: '📊' },
  { path: '/advanced', label: '高级分析', icon: '🔍' },
  { path: '/business', label: '业务关联', icon: '🔗' },
  { path: '/search', label: '车辆搜索', icon: '🔎' },
  { path: '/compare', label: '对比分析', icon: '⚖️' },
  { path: '/data', label: '数据管理', icon: '📁' },
]
</script>

<template>
  <div class="app-layout">
    <aside class="app-sidebar">
      <div class="app-logo">
        <div class="app-logo__icon">🚗</div>
        <div class="app-logo__text">
          <div class="app-logo__title">汽车大数据</div>
          <div class="app-logo__subtitle">分析平台</div>
        </div>
      </div>
      <nav class="app-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="app-nav__item"
          active-class="app-nav__item--active"
        >
          <span class="app-nav__icon">{{ item.icon }}</span>
          <span class="app-nav__label">{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div class="app-sidebar__footer">
        <div class="app-version">v1.1.0</div>
      </div>
    </aside>
    <main class="app-main">
      <header class="app-header">
        <div class="app-header__title">汽车大数据分析平台</div>
        <div class="app-header__info">
          <span class="app-header__tag">Spring Boot + Vue 3</span>
          <span class="app-header__tag app-header__tag--accent">ECharts</span>
        </div>
      </header>
      <div class="app-content">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.app-sidebar {
  width: 220px;
  background: linear-gradient(180deg, #1f2937 0%, #111827 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
  flex-shrink: 0;
}

.app-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.app-logo__icon {
  font-size: 32px;
  line-height: 1;
}

.app-logo__title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.app-logo__subtitle {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.app-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.app-nav__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  color: #d1d5db;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.app-nav__item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.app-nav__item--active {
  background: linear-gradient(135deg, #5470c6, #73c0de);
  color: #fff;
  box-shadow: 0 2px 8px rgba(84, 112, 198, 0.4);
}

.app-nav__icon {
  font-size: 18px;
  width: 20px;
  text-align: center;
}

.app-sidebar__footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.app-version {
  font-size: 11px;
  color: #6b7280;
  text-align: center;
}

.app-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: #fff;
  padding: 14px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 10;
}

.app-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.app-header__info {
  display: flex;
  gap: 8px;
}

.app-header__tag {
  padding: 4px 10px;
  background: #f3f4f6;
  color: #6b7280;
  border-radius: 12px;
  font-size: 12px;
}

.app-header__tag--accent {
  background: #fef3c7;
  color: #92400e;
}

.app-content {
  flex: 1;
  padding: 20px 24px;
  overflow-x: auto;
}

@media (max-width: 768px) {
  .app-sidebar {
    width: 64px;
  }

  .app-logo__text,
  .app-nav__label,
  .app-version {
    display: none;
  }

  .app-logo {
    justify-content: center;
    padding: 16px 0;
  }

  .app-nav__item {
    justify-content: center;
    padding: 12px 0;
  }

  .app-content {
    padding: 12px;
  }
}
</style>
