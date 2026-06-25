<template>
  <div class="chart-card" :class="{ 'is-loading': loading }">
    <div class="chart-card__header">
      <div class="chart-card__title">
        <slot name="title">{{ title }}</slot>
      </div>
      <div class="chart-card__extra">
        <slot name="extra"></slot>
      </div>
    </div>
    <div class="chart-card__body">
      <div v-if="loading" class="chart-card__loading">
        <span class="loading-spinner"></span>
        <span class="loading-text">数据加载中...</span>
      </div>
      <div v-else-if="error" class="chart-card__error">
        <span>{{ error }}</span>
      </div>
      <div v-else-if="empty" class="chart-card__empty">
        <span>暂无数据</span>
      </div>
      <slot v-else></slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
  empty: {
    type: Boolean,
    default: false,
  },
})
</script>

<style scoped>
.chart-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  padding: 16px;
  margin-bottom: 16px;
  transition: box-shadow 0.3s;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-card:hover {
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.12);
}

.chart-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.chart-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.chart-card__extra {
  font-size: 13px;
  color: #6b7280;
}

.chart-card__body {
  flex: 1;
  position: relative;
  min-height: 280px;
}

.chart-card__loading,
.chart-card__error,
.chart-card__empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 14px;
  gap: 8px;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #e5e7eb;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.chart-card__error {
  color: #ef4444;
}
</style>
