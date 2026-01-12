<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'
import { Bell, Top } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useThemeStore } from '../stores/theme'
import { storeToRefs } from 'pinia'
import axios from '../utils/request'

const { t } = useI18n()
const themeStore = useThemeStore()
const { isDark } = storeToRefs(themeStore)
const chartRef = ref(null)
let myChart = null

const stats = ref({
    totalNotifications: 0,
    successRate: 0,
    activeTasks: 0,
    notificationVolume24h: []
})

const fetchStats = async () => {
    try {
        const res = await axios.get('/dashboard/stats')
        stats.value = res
        initChart()
    } catch (error) {
        console.error('Failed to fetch dashboard stats', error)
    }
}

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', () => {
    myChart?.resize()
  })
})

watch(isDark, () => {
  myChart?.dispose()
  initChart()
})

const initChart = () => {
  if (!chartRef.value) return
  
  myChart = echarts.init(chartRef.value, isDark.value ? 'dark' : undefined)
  
  const textColor = isDark.value ? '#E5E7EB' : '#374151'
  const axisColor = isDark.value ? '#6B7280' : '#E5E7EB'
  const splitLineColor = isDark.value ? '#374151' : '#E5E7EB'

  // Prepare data
  const times = stats.value.notificationVolume24h?.map(v => v.time) || []
  const counts = stats.value.notificationVolume24h?.map(v => v.count) || []

  const option = {
    backgroundColor: 'transparent',
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: times,
      axisLine: { lineStyle: { color: axisColor } },
      axisLabel: { color: textColor }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: splitLineColor } },
      axisLine: { lineStyle: { color: axisColor } },
      axisLabel: { color: textColor }
    },
    series: [
      {
        name: 'Notifications',
        type: 'line',
        smooth: true,
        lineStyle: {
          width: 3,
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#8B5CF6' },
            { offset: 1, color: '#3B82F6' }
          ])
        },
        areaStyle: {
          opacity: 0.3,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#8B5CF6' },
            { offset: 1, color: 'transparent' }
          ])
        },
        data: counts,
        symbol: 'none'
      }
    ]
  }
  myChart.setOption(option)
}
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- Page Header -->
    <header class="h-16 bg-white/80 dark:bg-gray-800/50 backdrop-blur border-b border-gray-200 dark:border-gray-700 sticky top-0 z-10 flex items-center justify-between px-8">
      <h2 class="text-xl font-semibold text-gray-800 dark:text-white">{{ t('dashboard.title') }}</h2>
      <div class="flex items-center space-x-4">
           <el-button circle :icon="Bell" class="!bg-gray-100 dark:!bg-gray-700 !border-gray-200 dark:!border-gray-600 !text-gray-600 dark:!text-gray-300" />
      </div>
    </header>

    <main class="p-8 space-y-8 flex-1 overflow-auto">
      <!-- Stats Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none relative overflow-hidden group hover:border-purple-500/50 transition duration-300">
           <div class="absolute right-0 top-0 p-6 opacity-10 group-hover:scale-110 transition">
               <el-icon class="text-8xl text-purple-500"><Bell /></el-icon>
           </div>
           <div class="relative z-10">
               <div class="text-gray-500 dark:text-gray-400 text-sm font-medium mb-1">{{ t('dashboard.totalNotifications') }}</div>
               <div class="text-4xl font-bold text-gray-900 dark:text-white">{{ stats.totalNotifications.toLocaleString() }}</div>
               <div class="mt-2 text-sm text-green-500 dark:text-green-400 flex items-center">
                   <el-icon class="mr-1"><Top /></el-icon> (Realtime)
               </div>
           </div>
        </div>

        <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none relative overflow-hidden group hover:border-green-500/50 transition duration-300">
           <div class="text-gray-500 dark:text-gray-400 text-sm font-medium mb-1">{{ t('dashboard.successRate') }}</div>
           <div class="text-4xl font-bold text-gray-900 dark:text-white">{{ stats.successRate }}%</div>
           <div class="w-full bg-gray-200 dark:bg-gray-700 h-1.5 rounded-full mt-4 overflow-hidden">
               <div class="bg-green-500 h-full rounded-full" :style="{ width: stats.successRate + '%' }"></div>
           </div>
        </div>

        <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none relative overflow-hidden group hover:border-blue-500/50 transition duration-300">
           <div class="text-gray-500 dark:text-gray-400 text-sm font-medium mb-1">{{ t('dashboard.activeTasks') }}</div>
           <div class="text-4xl font-bold text-gray-900 dark:text-white">{{ stats.activeTasks }}</div>
           <div class="mt-2 text-sm text-blue-500 dark:text-blue-400">{{ t('dashboard.runningSmoothly') }}</div>
        </div>
      </div>

      <!-- Chart -->
      <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none">
          <h3 class="text-lg font-medium mb-6 text-gray-900 dark:text-white">{{ t('dashboard.volume24h') }}</h3>
          <div ref="chartRef" class="w-full h-80"></div>
      </div>
    </main>
  </div>
</template>

<style>
/* CSS Variable overwrite for Dark Mode Table */
html.dark .el-table {
    --el-table-border-color: #374151;
    --el-table-text-color: #9CA3AF;
    --el-table-header-text-color: #E5E7EB;
}
</style>
