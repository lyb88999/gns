<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Search, Delete, CopyDocument } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import axios from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t } = useI18n()

const tokens = ref([])
const dialogVisible = ref(false)
const form = ref({
  name: '',
})
const searchQuery = ref('')

const fetchTokens = async () => {
    try {
        const res = await axios.get('/tokens', {
            params: {
                search: searchQuery.value || undefined
            }
        })
        tokens.value = res.content || []
    } catch (error) {
        console.error('Failed to fetch tokens', error)
    }
}

const handleCopy = (token) => {
    navigator.clipboard.writeText(token)
    ElMessage.success(t('common.copied'))
}

const handleRevoke = (row) => {
    ElMessageBox.confirm(
        t('tokens.revokePrompt'),
        t('common.warning') || 'Warning',
        {
            confirmButtonText: t('common.revoke'),
            cancelButtonText: t('common.cancel'),
            type: 'warning',
        }
    ).then(async () => {
        try {
            await axios.delete(`/tokens/${row.id}`)
            ElMessage.success(t('tokens.revokeSuccess'))
            fetchTokens()
        } catch (error) {
            console.error(error)
        }
    }).catch(() => {})
}

const handleCreate = () => {
    form.value.name = ''
    dialogVisible.value = true
}

const handleSubmit = async () => {
    try {
        const res = await axios.post('/tokens', { name: form.value.name })
        dialogVisible.value = false
        
        // Show the raw token to the user
        ElMessageBox.alert(
            `<div>
                <p>${t('tokens.tokenCopyPrompt')}</p>
                <div class="mt-2 p-2 bg-gray-100 dark:bg-gray-700 rounded font-mono break-all border border-gray-200 dark:border-gray-600 select-all">${res.token}</div>
            </div>`,
            t('tokens.tokenGenerated'),
            {
                confirmButtonText: t('tokens.copyClose'),
                dangerouslyUseHTMLString: true,
                callback: () => {
                    navigator.clipboard.writeText(res.token)
                    ElMessage.success(t('tokens.tokenCopied'))
                }
            }
        )
        
        fetchTokens()
    } catch (error) {
        console.error(error)
    }
}

onMounted(() => {
    fetchTokens()
})
</script>

<template>
  <div class="h-full flex flex-col p-8 bg-gray-50 dark:bg-gray-900 transition-colors duration-300">
    <div class="flex justify-between items-center mb-8">
        <div>
            <h1 class="text-2xl font-bold mb-1 text-gray-900 dark:text-white transition-colors">{{ t('tokens.title') }}</h1>
            <p class="text-gray-500 dark:text-gray-400 transition-colors">{{ t('tokens.subtitle') }}</p>
        </div>
        <el-button type="primary" :icon="Plus" color="#9333ea" @click="handleCreate" class="shadow-lg shadow-purple-900/20">{{ t('tokens.createButton') }}</el-button>
    </div>

    <!-- Enhanced Card UI -->
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none overflow-hidden flex-1 flex flex-col transition-all duration-300">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700 flex justify-between bg-gray-50/50 dark:bg-gray-800/50">
            <el-input 
                v-model="searchQuery" 
                :placeholder="t('common.search')" 
                :prefix-icon="Search" 
                class="w-72" 
                size="large" 
                clearable
                @keyup.enter="fetchTokens"
                @clear="fetchTokens"
            />
        </div>
        
        <el-table :data="tokens" 
            style="width: 100%;" 
            class="!bg-transparent"
            :row-style="{ background: 'transparent' }"
            :header-cell-style="{ background: 'transparent' }"
        >
            <el-table-column prop="name" :label="t('tokens.name')">
                <template #default="{ row }">
                    <span class="font-medium text-gray-900 dark:text-gray-200">{{ row.name }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="token" :label="t('tokens.token')">
                <template #default="{ row }">
                    <div class="flex items-center space-x-2">
                        <code class="px-2 py-1 bg-gray-100 dark:bg-gray-700 rounded text-xs font-mono text-gray-800 dark:text-gray-300">{{ row.token }}</code>
                        <el-button link :icon="CopyDocument" @click="handleCopy(row.token)" />
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="lastUsedAt" :label="t('tokens.lastUsedAt')">
                <template #default="{ row }">
                    {{ row.lastUsedAt ? new Date(row.lastUsedAt).toLocaleString() : '-' }}
                </template>
            </el-table-column>
            <el-table-column prop="expiresAt" :label="t('tokens.expiresAt')">
                <template #default="{ row }">
                   {{ row.expiresAt ? new Date(row.expiresAt).toLocaleDateString() : 'Never' }}
                </template>
            </el-table-column>
            <el-table-column :label="t('common.actions')" width="180">
                <template #default="{ row }">
                    <el-button link type="danger" :icon="Delete" @click="handleRevoke(row)">{{ t('common.revoke') }}</el-button>
                </template>
            </el-table-column>
        </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="t('tokens.createDialog')" width="500px" class="rounded-xl overflow-hidden !bg-white dark:!bg-gray-800 dark:!text-white">
        <el-form :model="form" label-position="top" class="mt-4">
            <el-form-item :label="t('tokens.name')">
                <el-input v-model="form.name" size="large" placeholder="e.g. Production API" />
            </el-form-item>
        </el-form>
        <template #footer>
            <span class="dialog-footer">
                <el-button size="large" @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
                <el-button size="large" type="primary" color="#9333ea" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
            </span>
        </template>
    </el-dialog>
  </div>
</template>
