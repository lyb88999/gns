<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'
import { Bell, Top, CircleCheckFilled, Reading, ArrowDown } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useThemeStore } from '../stores/theme'
import { storeToRefs } from 'pinia'
import axios from '../utils/request'

const { t } = useI18n()
const themeStore = useThemeStore()
const { isDark } = storeToRefs(themeStore)
const lineChartRef = ref(null)
const pieChartRef = ref(null)
const statusChartRef = ref(null)

let lineChart = null
let pieChart = null
let statusChart = null

const stats = ref({
    totalNotifications: 0,
    successRate: 0,
    activeTasks: 0,
    notificationVolume24h: [],
    channelDistribution: {},
    statusDistribution: {},
    recentLogs: [],
    topErrors: []
})

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', handleResize)
})

const handleResize = () => {
    lineChart?.resize()
    pieChart?.resize()
    statusChart?.resize()
}

watch(isDark, () => {
  lineChart?.dispose()
  pieChart?.dispose()
  statusChart?.dispose()
  initCharts()
})

const initCharts = () => {
  initLineChart()
  initChannelPie()
  initStatusDonut()
}

const initLineChart = () => {
  if (!lineChartRef.value) return
  if (!lineChart) {
      lineChart = echarts.init(lineChartRef.value, isDark.value ? 'dark' : undefined)
  }
  lineChart.clear() // Clear previous data to prevent animation artifacts
  const textColor = isDark.value ? '#E5E7EB' : '#374151'
  const axisColor = isDark.value ? '#6B7280' : '#E5E7EB'
  const splitLineColor = isDark.value ? '#374151' : '#E5E7EB'

  const times = stats.value.notificationVolume24h?.map(v => v.time) || []
  const counts = stats.value.notificationVolume24h?.map(v => v.count) || []


  lineChart.setOption({
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
    series: [{
        name: 'Notifications',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: {
          width: 3,
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#8B5CF6' }, { offset: 1, color: '#3B82F6' }])
        },
        areaStyle: {
          opacity: 0.3,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#8B5CF6' }, { offset: 1, color: 'transparent' }])
        },
        data: counts
    }]
  }, true) // notMerge: true to prevent animation artifacts on granularity change
}

const granularity = ref('5m')
const granularityOptions = [
    { label: '1 Minute', value: '1m' },
    { label: '5 Minutes', value: '5m' },
    { label: '30 Minutes', value: '30m' },
    { label: '1 Hour', value: '60m' }
]

watch(granularity, () => {
    fetchStats()
})

const fetchStats = async () => {
    try {
        const res = await axios.get('/dashboard/stats', {
            params: { granularity: granularity.value } // Use dynamic granularity
        })
        if (res) {
            stats.value = res
            initCharts()
        }
    } catch (error) {
        console.error('Failed to fetch dashboard stats', error)
    }
}

const initChannelPie = () => {
    if (!pieChartRef.value) return
    pieChart = echarts.init(pieChartRef.value, isDark.value ? 'dark' : undefined)
    
    const data = []
    if (stats.value.channelDistribution) {
        Object.entries(stats.value.channelDistribution).forEach(([key, val]) => {
            data.push({ value: val, name: key })
        })
    }
    
    pieChart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'item' },
        legend: { bottom: '0%', left: 'center', textStyle: { color: isDark.value ? '#E5E7EB' : '#374151' } },
            series: [{
            name: 'Channel',
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '45%'],
            minAngle: 15, // Ensure small sectors are clickable
            avoidLabelOverlap: false,
            itemStyle: { borderRadius: 10, borderColor: isDark.value ? '#1F2937' : '#fff', borderWidth: 2 },
            label: { show: false, position: 'center' },
            emphasis: { label: { show: true, fontSize: 20, fontWeight: 'bold' } },
            labelLine: { show: false },
            data: data
        }]
    })
}

const initStatusDonut = () => {
    if (!statusChartRef.value) return
    statusChart = echarts.init(statusChartRef.value, isDark.value ? 'dark' : undefined)
    
    const data = []
    if (stats.value.statusDistribution) {
        Object.entries(stats.value.statusDistribution).forEach(([key, val]) => {
            let color = undefined
            const k = key.toUpperCase()
            if (k === 'SUCCESS') color = '#10B981'
            if (k === 'FAILED') color = '#EF4444'
            if (k === 'BLOCKED' || k === 'SKIPPED') color = '#F59E0B'
            
            data.push({ value: val, name: key, itemStyle: color ? { color } : undefined })
        })
    }
    
    statusChart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'item' },
        legend: { bottom: '0%', left: 'center', textStyle: { color: isDark.value ? '#E5E7EB' : '#374151' } },
        series: [{
            name: 'Status',
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '45%'],
            minAngle: 15, // Ensure small sectors are clickable
            avoidLabelOverlap: false,
            itemStyle: { borderRadius: 10, borderColor: isDark.value ? '#1F2937' : '#fff', borderWidth: 2 },
            label: { show: false, position: 'center' },
            emphasis: { label: { show: true, fontSize: 20, fontWeight: 'bold' } },
            labelLine: { show: false },
            data: data
        }]
    })
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

    <main class="p-8 space-y-6 flex-1 overflow-auto">
      <!-- 1. Key Performance Indicators (KPIs) -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none relative overflow-hidden group transition duration-300 hover:shadow-lg hover:border-purple-200 dark:hover:border-purple-900/50">
           <div class="relative z-10">
               <div class="flex items-center space-x-2 mb-2">
                   <div class="p-2 bg-purple-50 dark:bg-purple-900/20 rounded-lg">
                       <el-icon class="text-xl text-purple-600 dark:text-purple-400"><Bell /></el-icon>
                   </div>
                   <span class="text-gray-500 dark:text-gray-400 text-sm font-medium">{{ t('dashboard.totalNotifications') }}</span>
               </div>
               <div class="text-3xl font-bold text-gray-900 dark:text-white mt-4">{{ stats.totalNotifications.toLocaleString() }}</div>
               <div class="mt-2 text-xs font-medium text-green-500 dark:text-green-400 flex items-center bg-green-50 dark:bg-green-900/20 w-fit px-2 py-1 rounded-full">
                   <el-icon class="mr-1"><Top /></el-icon> {{ t('dashboard.realtime') }}
               </div>
           </div>
        </div>

        <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none relative overflow-hidden group transition duration-300 hover:shadow-lg hover:border-green-200 dark:hover:border-green-900/50">
           <div class="relative z-10">
               <div class="flex items-center space-x-2 mb-2">
                   <div class="p-2 bg-green-50 dark:bg-green-900/20 rounded-lg">
                       <el-icon class="text-xl text-green-600 dark:text-green-400"><CircleCheckFilled /></el-icon>
                   </div>
                   <span class="text-gray-500 dark:text-gray-400 text-sm font-medium">{{ t('dashboard.successRate') }}</span>
               </div>
               <div class="text-3xl font-bold text-gray-900 dark:text-white mt-4">{{ stats.successRate }}%</div>
               <div class="mt-2 text-xs font-medium text-green-500 dark:text-green-400 flex items-center bg-green-50 dark:bg-green-900/20 w-fit px-2 py-1 rounded-full">
                   <el-icon class="mr-1"><CircleCheckFilled /></el-icon> {{ t('dashboard.systemHealthy') }}
               </div>
           </div>
        </div>

        <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none relative overflow-hidden group transition duration-300 hover:shadow-lg hover:border-blue-200 dark:hover:border-blue-900/50">
            <div class="relative z-10">
               <div class="flex items-center space-x-2 mb-2">
                   <div class="p-2 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
                       <el-icon class="text-xl text-blue-600 dark:text-blue-400"><Reading /></el-icon>
                   </div>
                   <span class="text-gray-500 dark:text-gray-400 text-sm font-medium">{{ t('dashboard.activeTasks') }}</span>
               </div>
               <div class="text-3xl font-bold text-gray-900 dark:text-white mt-4">{{ stats.activeTasks }}</div>
               <div class="mt-2 text-xs font-medium text-blue-500 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/20 w-fit px-2 py-1 rounded-full">
                   {{ t('dashboard.runningSmoothly') }}
               </div>
           </div>
        </div>
      </div>

      <!-- 2. Charts Section -->
      <!-- 2. Charts Section -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
           <!-- Distributions (Channel & Status) -->
           <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none">
                <h3 class="text-lg font-medium mb-2 text-gray-900 dark:text-white">{{ t('dashboard.channelDistribution') }}</h3>
                <div ref="pieChartRef" class="w-full h-64"></div>
           </div>
           
           <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none">
                <h3 class="text-lg font-medium mb-2 text-gray-900 dark:text-white">{{ t('dashboard.statusDistribution') }}</h3>
                <div ref="statusChartRef" class="w-full h-64"></div>
           </div>
      </div>

      <!-- 24h Volume Line Chart (Full Width) -->
      <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none">
          <div class="flex items-center space-x-4 mb-6">
               <h3 class="text-lg font-medium text-gray-900 dark:text-white">{{ t('dashboard.volume24h') }}</h3>
               <el-dropdown trigger="click" @command="(val) => granularity = val">
                   <span class="el-dropdown-link cursor-pointer text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-purple-600 dark:hover:text-purple-400 flex items-center transition-colors">
                       {{ granularityOptions.find(o => o.value === granularity)?.label }}
                       <el-icon class="el-icon--right"><arrow-down /></el-icon>
                   </span>
                   <template #dropdown>
                       <el-dropdown-menu>
                           <el-dropdown-item v-for="opt in granularityOptions" :key="opt.value" :command="opt.value" :class="{ '!text-purple-600 font-medium': granularity === opt.value }">
                               {{ opt.label }}
                           </el-dropdown-item>
                       </el-dropdown-menu>
                   </template>
               </el-dropdown>
          </div>
          <div ref="lineChartRef" class="w-full h-80"></div>
      </div>

      <!-- 3. Details Section -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 pb-6">
          <!-- Recent Logs -->
          <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none">
              <div class="flex justify-between items-center mb-6">
                  <h3 class="text-lg font-medium text-gray-900 dark:text-white">{{ t('dashboard.recentLogs') }}</h3>
                  <router-link to="/logs" class="text-sm text-purple-600 dark:text-purple-400 hover:underline font-medium">{{ t('dashboard.viewAll') }}</router-link>
              </div>
              
              <div class="space-y-3">
                  <div v-for="log in stats.recentLogs" :key="log.id" class="flex items-center justify-between p-3.5 rounded-xl bg-gray-50/50 dark:bg-gray-700/30 border border-gray-100 dark:border-gray-700/50 hover:bg-white dark:hover:bg-gray-700 transition duration-200 group">
                      <div class="flex items-center space-x-3.5 truncate">
                          <div class="relative flex-shrink-0">
                             <div :class="{
                                  'bg-green-500': log.status === 'SUCCESS',
                                  'bg-red-500': log.status === 'FAILED',
                                  'bg-yellow-500': log.status === 'BLOCKED' || log.status === 'SKIPPED'
                              }" class="w-2.5 h-2.5 rounded-full ring-4 ring-white dark:ring-gray-800 group-hover:ring-gray-100 dark:group-hover:ring-gray-700 transition duration-200"></div>
                          </div>
                          <span class="text-sm font-medium text-gray-700 dark:text-gray-200 truncate">{{ log.taskName || log.taskId }}</span>
                      </div>
                      <div class="text-xs text-gray-400 font-mono">{{ new Date(log.createdAt).toLocaleTimeString() }}</div>
                  </div>
                   <div v-if="!stats.recentLogs || stats.recentLogs.length === 0" class="text-center text-gray-400 text-sm py-12 flex flex-col items-center">
                       <el-icon class="text-4xl mb-3 text-gray-300 dark:text-gray-600"><reading /></el-icon>
                       No logs recently
                   </div>
              </div>
          </div>

          <!-- Top Errors -->
          <div class="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none">
              <h3 class="text-lg font-medium mb-6 text-gray-900 dark:text-white">{{ t('dashboard.topErrors') }}</h3>
              <div class="space-y-3">
                  <div v-for="(error, idx) in stats.topErrors" :key="idx" class="group flex items-start justify-between p-4 rounded-xl bg-gray-50/50 dark:bg-gray-700/30 border border-gray-100 dark:border-gray-700/50 hover:bg-red-50/50 dark:hover:bg-red-900/10 transition duration-200">
                      <div class="flex gap-3 overflow-hidden">
                          <div class="flex-shrink-0 mt-0.5">
                              <div class="w-1.5 h-1.5 rounded-full bg-red-500 ring-4 ring-red-100 dark:ring-red-900/30"></div>
                          </div>
                          <div class="flex-1 min-w-0">
                             <div class="text-sm font-medium text-gray-700 dark:text-gray-200 truncate pr-4" :title="error.errorMessage">
                                 {{ error.errorMessage }}
                             </div>
                             <div class="text-xs text-gray-400 mt-1">Occurred {{ error.count }} times</div>
                          </div>
                      </div>
                      <div class="flex-shrink-0 self-center">
                          <span class="inline-flex items-center justify-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300">
                              {{ error.count }}
                          </span>
                      </div>
                  </div>
                  <div v-if="!stats.topErrors || stats.topErrors.length === 0" class="flex flex-col items-center justify-center py-12 text-gray-400">
                      <div class="w-16 h-16 bg-green-50 dark:bg-green-900/20 rounded-full flex items-center justify-center mb-3">
                          <el-icon class="text-2xl text-green-500"><CircleCheckFilled /></el-icon>
                      </div>
                      <span class="text-sm font-medium text-gray-500 dark:text-gray-400">{{ t('dashboard.noErrors') }}</span>
                  </div>
              </div>
          </div>
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
