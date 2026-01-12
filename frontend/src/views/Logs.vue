<script setup>
import { ref, onMounted, watch } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import axios from '../utils/request'

const { t } = useI18n()

const logs = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const currentLog = ref({})

const query = ref({
    page: 1,
    size: 10,
    status: 'all',
    search: ''
})

const handleDetails = (row) => {
    currentLog.value = row
    dialogVisible.value = true
}

const fetchLogs = async () => {
    loading.value = true
    try {
        const res = await axios.get('/logs', { 
            params: {
                page: query.value.page - 1, // backend is 0-indexed
                size: query.value.size,
                status: query.value.status === 'all' ? undefined : query.value.status,
                search: query.value.search || undefined
            }
        })
        // Backend returns PageResult { content: [], totalElements: 0, totalPages: 0 }
        // Adjust based on actual DTO, assuming standard PageResult structure
        logs.value = res.content || []
        total.value = res.totalElements || 0
    } catch (error) {
        console.error('Failed to fetch logs', error)
    } finally {
        loading.value = false
    }
}

const handlePageChange = (p) => {
    query.value.page = p
    fetchLogs()
}

const handleFilterChange = () => {
    query.value.page = 1
    fetchLogs()
}

onMounted(() => {
    fetchLogs()
})
</script>

<template>
  <div class="h-full flex flex-col p-8 bg-gray-50 dark:bg-gray-900 transition-colors duration-300">
    <div class="mb-8 flex justify-between items-end">
        <div>
            <h1 class="text-2xl font-bold mb-1 text-gray-900 dark:text-white transition-colors">{{ t('common.logs') }}</h1>
            <p class="text-gray-500 dark:text-gray-400 transition-colors">Monitor notification delivery history.</p>
        </div>
        <el-button :icon="Refresh" circle @click="fetchLogs" :loading="loading" />
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none overflow-hidden flex-1 flex flex-col transition-all duration-300">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center bg-gray-50/50 dark:bg-gray-800/50 w-full">
            <div class="flex gap-4 flex-1">
                 <el-input 
                    v-model="query.search" 
                    :placeholder="t('common.search')" 
                    :prefix-icon="Search" 
                    class="w-64" 
                    size="large" 
                    @keyup.enter="handleFilterChange" 
                    clearable 
                    @clear="handleFilterChange"
                 />
                 <el-select 
                    v-model="query.status" 
                    :placeholder="t('logs.filterByStatus')" 
                    class="w-32" 
                    size="large" 
                    @change="handleFilterChange"
                 >
                     <el-option :label="t('common.all')" value="all" />
                     <el-option :label="t('common.success')" value="SUCCESS" />
                     <el-option :label="t('common.failed')" value="FAILED" />
                     <el-option :label="t('common.blocked')" value="BLOCKED" />
                 </el-select>
            </div>
            <div class="text-sm text-gray-500">{{ t('logs.showingLast7Days') }}</div>
        </div>
        
        <el-table 
            v-loading="loading"
            :data="logs" 
            style="width: 100%;" 
            class="!bg-transparent" 
            :row-style="{ background: 'transparent' }" 
            :header-cell-style="{ background: 'transparent' }"
        >
            <el-table-column prop="taskName" :label="t('tasks.name')">
                <template #default="{ row }">
                    <div class="font-medium text-gray-900 dark:text-gray-200">{{ row.taskName || 'N/A' }}</div>
                    <div class="text-xs text-gray-500">{{ row.taskId }}</div>
                </template>
            </el-table-column>
            <el-table-column prop="channel" :label="t('tasks.channels')">
                <template #default="{ row }">
                    <div class="flex items-center gap-2">
                        <span class="text-sm">{{ row.channel }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="recipient" :label="'Recipient'" />
            <el-table-column prop="status" :label="t('common.status')">
                 <template #default="{ row }">
                     <el-tag 
                        :type="row.status === 'SUCCESS' ? 'success' : (row.status === 'blocked' || row.status === 'BLOCKED' ? 'warning' : 'danger')" 
                        effect="dark" 
                        round 
                        size="small"
                     >
                         {{ 
                             row.status === 'SUCCESS' ? t('common.success') : 
                             (row.status === 'blocked' || row.status === 'BLOCKED' ? t('common.blocked') : t('common.failed')) 
                         }}
                     </el-tag>
                 </template>
            </el-table-column>
            <el-table-column prop="sentAt" label="Time">
                <template #default="{ row }">
                    {{ new Date(row.sentAt).toLocaleString() }}
                </template>
            </el-table-column>
            <el-table-column width="100" align="right">
                <template #default="{ row }">
                    <el-button link size="small" @click="handleDetails(row)">{{ t('common.details') }}</el-button>
                </template>
            </el-table-column>
        </el-table>
        
        <div class="p-4 border-t border-gray-200 dark:border-gray-700 flex justify-end">
             <el-pagination 
                background 
                layout="prev, pager, next" 
                :total="total" 
                :page-size="query.size"
                :current-page="query.page"
                @current-change="handlePageChange"
             />
        </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="t('common.details')" width="600px" class="rounded-xl overflow-hidden !bg-white dark:!bg-gray-800 dark:!text-white">
        <el-descriptions :column="1" border class="mt-4">
            <el-descriptions-item label="Task Name">{{ currentLog.taskName }}</el-descriptions-item>
            <el-descriptions-item label="Task ID">{{ currentLog.taskId }}</el-descriptions-item>
            <el-descriptions-item label="Channel">{{ currentLog.channel }}</el-descriptions-item>
            <el-descriptions-item label="Recipient">{{ currentLog.recipient }}</el-descriptions-item>
            <el-descriptions-item label="Status">
                <el-tag 
                    :type="currentLog.status === 'SUCCESS' ? 'success' : (currentLog.status === 'blocked' || currentLog.status === 'BLOCKED' ? 'warning' : 'danger')"
                >
                    {{ 
                         currentLog.status === 'SUCCESS' ? t('common.success') : 
                         (currentLog.status === 'blocked' || currentLog.status === 'BLOCKED' ? t('common.blocked') : t('common.failed')) 
                    }}
                </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Time">{{ new Date(currentLog.sentAt).toLocaleString() }}</el-descriptions-item>
            <el-descriptions-item label="Error" v-if="currentLog.errorMessage">
                <span class="text-red-500">{{ currentLog.errorMessage }}</span>
            </el-descriptions-item>
        </el-descriptions>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="dialogVisible = false">{{ t('common.close') }}</el-button>
            </span>
        </template>
    </el-dialog>
  </div>
</template>
