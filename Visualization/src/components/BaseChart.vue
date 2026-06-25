<template>
  <div ref="chartRef" class="echarts-container" :style="{ height: height, width: width }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { echarts } from '@/echarts'

const props = defineProps({
  option: {
    type: Object,
    required: true,
  },
  height: {
    type: String,
    default: '400px',
  },
  width: {
    type: String,
    default: '100%',
  },
  theme: {
    type: String,
    default: '',
  },
})

const chartRef = ref(null)
let chartInstance = null

const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartRef.value, props.theme || undefined)
  chartInstance.setOption(props.option, true)
}

const resize = () => {
  chartInstance && chartInstance.resize()
}

watch(
  () => props.option,
  () => {
    if (chartInstance) {
      chartInstance.setOption(props.option, true)
    } else {
      initChart()
    }
  },
  { deep: true }
)

onMounted(async () => {
  await nextTick()
  initChart()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

defineExpose({
  getInstance: () => chartInstance,
  resize,
})
</script>

<style scoped>
.echarts-container {
  width: 100% !important;
  min-height: 200px;
}
</style>
