<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Search, Edit, Delete, VideoPlay, CopyDocument, MagicStick } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/auth'
import axios from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import AIChatAssist from '../components/AIChatAssist.vue'

const { t } = useI18n()
const authStore = useAuthStore()

const tasks = ref([])
const dialogVisible = ref(false)
const showAI = ref(false)
const isEdit = ref(false)
const currentId = ref(null)
const searchQuery = ref('')

import { copyText } from '../utils/clipboard'

const copyToClipboard = (text) => {
    copyText(text, t)
}

const handleRunNow = async (row) => {
    try {
        await axios.post(`/tasks/${row.taskId}/execute`)
        ElMessage.success(t('tasks.runSuccess'))
        // Optional: refresh tasks to see last run update
        setTimeout(fetchTasks, 1000)
        setTimeout(fetchTasks, 1000)
    } catch (error) {
        // Handled by request interceptor
    }
}

const form = ref({
  name: '',
  description: '',
  channels: [],
  cronExpression: '',
  triggerType: 'cron',
  messageTemplate: '',
  customData: {
      dingTalkWebhook: '',
      dingTalkSecret: '',
      wechatCorpId: '',
      wechatCorpSecret: '',
      wechatAgentId: '',
      wechatToUser: '',
      wechatWebhook: ''
  },
  rateLimitEnabled: false,
  maxPerHour: 0,
  maxPerDay: 0,
  silentStart: null,
  silentEnd: null,
})

const fetchTasks = async () => {
    try {
        const res = await axios.get('/tasks', {
            params: {
                search: searchQuery.value || undefined
            }
        })
        // Backend returns PageResult { content: [], ... }
        tasks.value = res.content || []
    } catch (error) {
        console.error('Failed to fetch tasks', error)
    }
}

const handleStatusChange = async (row) => {
    try {
        await axios.put(`/tasks/${row.taskId}`, { ...row })
        ElMessage.success(t('tasks.statusUpdated'))
    } catch (error) {
        row.status = !row.status // revert on fail
        ElMessage.error(t('tasks.statusUpdateFailed'))
    }
}

const handleEdit = (row) => {
    isEdit.value = true
    currentId.value = row.taskId
    // Need to parse channels if it comes as string, but Jackson handles it as List.
    // form.value needs to match backend DTO structure roughly or be adapted.
    form.value = { 
        taskId: row.taskId,
        name: row.name, 
        description: row.description,
        channels: row.channels || [],
        cronExpression: row.cronExpression || '',
        triggerType: row.triggerType || 'cron',
        messageTemplate: row.messageTemplate || '',
        customData: row.customData || { 
            dingTalkWebhook: '', 
            dingTalkSecret: '',
            wechatCorpId: '',
            wechatCorpSecret: '',
            wechatAgentId: '',
            wechatToUser: '',
            wechatWebhook: ''
        },
        rateLimitEnabled: row.rateLimitEnabled || false,
        maxPerHour: row.maxPerHour || 0,
        maxPerDay: row.maxPerDay || 0,
        silentStart: row.silentStart || null,
        silentEnd: row.silentEnd || null,
    }
    dialogVisible.value = true
}

const handleDelete = (row) => {
    ElMessageBox.confirm(
        t('tasks.deleteConfirm'),
        t('common.warning'),
        {
            confirmButtonText: t('common.delete'),
            cancelButtonText: t('common.cancel'),
            type: 'warning',
        }
    ).then(async () => {
        try {
            await axios.delete(`/tasks/${row.taskId}`)
            ElMessage.success(t('tasks.deleteSuccess'))
            fetchTasks()
        } catch (error) {
            console.error(error)
        }
    }).catch(() => {})
}

const handleCreate = () => {
    isEdit.value = false
    form.value = { 
        name: '', 
        description: '', 
        channels: [], 
        cronExpression: '', 
        messageTemplate: '', 
        triggerType: 'cron',
        customData: { 
            dingTalkWebhook: '', 
            dingTalkSecret: '',
            wechatCorpId: '',
            wechatCorpSecret: '',
            wechatAgentId: '',
            wechatToUser: '',
            wechatWebhook: ''
        },
        rateLimitEnabled: false,
        maxPerHour: 0,
        maxPerDay: 0,
        silentStart: null,
        silentEnd: null,
    }
    dialogVisible.value = true
}

const handleSubmit = async () => {
    try {
        const payload = { ...form.value }
        // Clear cron if API trigger
        if (payload.triggerType === 'api') {
            payload.cronExpression = ''
        }
        
        if (isEdit.value) {
            await axios.put(`/tasks/${currentId.value}`, payload)
            ElMessage.success(t('tasks.updated'))
        } else {
            await axios.post('/tasks', payload)
            ElMessage.success(t('tasks.created'))
        }
        dialogVisible.value = false
        fetchTasks()
    } catch (error) {
        console.error(error)
    }
}

const handleTaskParsed = (data) => {
    // Fill the form with AI data
    if (data.name) form.value.name = data.name
    
    // Smart Trigger Type Detection
    if (data.cron && data.cron.trim() !== '') {
        form.value.triggerType = 'cron'
        form.value.cronExpression = data.cron
    } else {
        // AI suggests no cron -> Likely Manual/API Trigger
        form.value.triggerType = 'api'
        form.value.cronExpression = ''
    }

    if (data.channels && Array.isArray(data.channels)) {
        data.channels.forEach(c => {
             // Map AI output to exact case if needed, or rely on loose match
             // Typical AI output: "DingTalk", "WeChat", "Email"
             if (!form.value.channels.includes(c)) {
                 form.value.channels.push(c)
             }
        })
    }
    if (data.template) form.value.messageTemplate = data.template
    if (data.recipient) {
         // Try to guess where to put recipient based on channel
         if (form.value.channels.includes('Email')) {
             // We don't have a direct recipient field in main form (it depends on customData usually or logic).
             // But let's assume messageTemplate might hold it? No.
             // Usually GNS recipients are in customData or part of task specific logic.
             // For now, let's append to description or specialized field if available.
             // Actually, for Email, we might need a 'to' field in customData if not reusing WechatToUser.
             // Let's assume user has to manually fill sensitive details, or AI puts it in description.
             form.value.description += ` [To: ${data.recipient}]`
         }
    }
    
    ElMessage.success('Form filled by AI!')
    showAI.value = false // Close chat or keep open? Close is cleaner.
}


onMounted(() => {
    fetchTasks()
})
</script>

<template>
  <div class="h-full flex flex-col p-8 bg-gray-50 dark:bg-gray-900 transition-colors duration-300">
    <div class="flex justify-between items-center mb-8">
        <div>
            <h1 class="text-2xl font-bold mb-1 text-gray-900 dark:text-white transition-colors">{{ t('tasks.title') }}</h1>
            <p class="text-gray-500 dark:text-gray-400 transition-colors">{{ t('tasks.subtitle') }}</p>
        </div>
        <el-button type="primary" :icon="Plus" color="#9333ea" @click="handleCreate" class="shadow-lg shadow-purple-900/20">{{ t('tasks.createButton') }}</el-button>
    </div>

    <!-- Enhanced Card UI for Table -->
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none overflow-hidden flex-1 flex flex-col transition-all duration-300">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700 flex justify-between bg-gray-50/50 dark:bg-gray-800/50">
            <el-input 
                v-model="searchQuery" 
                :placeholder="t('common.search')" 
                :prefix-icon="Search" 
                class="w-72" 
                size="large" 
                clearable
                @keyup.enter="fetchTasks"
                @clear="fetchTasks"
            />
        </div>
        
        <el-table :data="tasks" 
            style="width: 100%;" 
            class="!bg-transparent"
            :row-style="{ background: 'transparent' }"
            :header-cell-style="{ background: 'transparent' }"
        >
            <el-table-column prop="taskId" :label="t('tasks.taskId')" width="200">
                <template #default="{ row }">
                    <div class="flex items-center group cursor-pointer" @click="copyToClipboard(row.taskId)">
                        <span class="font-mono text-xs text-gray-500 truncate mr-2">{{ row.taskId }}</span>
                        <el-icon class="text-gray-400 opacity-0 group-hover:opacity-100 transition-opacity"><CopyDocument /></el-icon>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="name" :label="t('tasks.name')">
                <template #default="{ row }">
                    <span class="font-medium text-gray-900 dark:text-gray-200">{{ row.name }}</span>
                </template>
            </el-table-column>
            <el-table-column v-if="authStore.user.role === 'admin'" prop="creatorName" :label="t('tasks.creator')" width="120">
                 <template #default="{ row }">
                     <el-tag size="small" type="info">{{ row.creatorName || 'Unknown' }}</el-tag>
                 </template>
            </el-table-column>
            <el-table-column prop="description" :label="t('tasks.description')" />
            <el-table-column prop="cronExpression" :label="t('tasks.cron')" />
            <el-table-column prop="channel" :label="t('tasks.channels')" min-width="180">
                <template #default="{ row }">
                    <div class="flex gap-1 items-center">
                        <el-tag v-for="c in row.channels.slice(0, 1)" :key="c" size="small" effect="plain" class="dark:bg-gray-900/50">{{ c }}</el-tag>
                        <el-tooltip 
                            v-if="row.channels.length > 1"
                            :content="row.channels.slice(1).join(', ')" 
                            placement="top"
                        >
                            <el-tag size="small" type="info" effect="plain" class="cursor-pointer dark:bg-gray-900/50">+{{ row.channels.length - 1 }}</el-tag>
                        </el-tooltip>
                    </div>
                </template>
            </el-table-column>
            <el-table-column :label="t('tasks.lastRun')" width="160">
                <template #default="{ row }">
                   <div class="text-xs text-gray-500">{{ row.lastRunAt ? new Date(row.lastRunAt).toLocaleString() : '-' }}</div>
                </template>
            </el-table-column>
            <el-table-column :label="t('tasks.nextRun')" width="160">
                <template #default="{ row }">
                   <div class="text-xs text-gray-500">{{ row.nextRunAt ? new Date(row.nextRunAt).toLocaleString() : '-' }}</div>
                </template>
            </el-table-column>
            <el-table-column prop="status" :label="t('tasks.active')">
                <template #default="{ row }">
                    <el-switch 
                        v-model="row.status" 
                        @change="handleStatusChange(row)"
                        inline-prompt 
                        :active-text="t('common.on')" 
                        :inactive-text="t('common.off')" 
                        style="--el-switch-on-color: #10B981; --el-switch-off-color: #EF4444"
                    />
                </template>
            </el-table-column>
            <el-table-column :label="t('common.actions')" width="280">
                <template #default="{ row }">
                    <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">{{ t('common.edit') }}</el-button>
                    <el-button v-if="row.triggerType === 'cron'" link type="success" :icon="VideoPlay" @click="handleRunNow(row)">{{ t('tasks.runNow') }}</el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
                </template>
            </el-table-column>
        </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? t('tasks.editDialog') : t('tasks.createDialog')" width="600px" class="rounded-xl overflow-hidden !bg-white dark:!bg-gray-800 dark:!text-white">
        <!-- AI Toggle -->
        <div v-if="!isEdit" class="mb-4">
             <el-button v-if="!showAI" type="primary" plain class="w-full" :icon="MagicStick" @click="showAI = true">
                 {{ t('tasks.aiAssist') }}
             </el-button>
             <div v-else class="h-80 mb-4 transition-all duration-300 ease-in-out">
                 <AIChatAssist @task-parsed="handleTaskParsed" />
                 <el-button link type="info" size="small" class="mt-2 w-full" @click="showAI = false">{{ t('tasks.closeAI') }}</el-button>
             </div>
        </div>

        <el-form :model="form" label-position="top" class="mt-4">
            <el-form-item :label="t('tasks.name')">
                <el-input v-model="form.name" size="large" />
            </el-form-item>
            <el-form-item :label="t('tasks.description')">
                <el-input v-model="form.description" size="large" />
            </el-form-item>
            <el-form-item :label="t('tasks.triggerType') || 'Trigger Type'">
                <el-radio-group v-model="form.triggerType">
                    <el-radio-button label="cron">{{ t('tasks.cronTrigger') }}</el-radio-button>
                    <el-radio-button label="api">{{ t('tasks.apiTrigger') }}</el-radio-button>
                </el-radio-group>
            </el-form-item>
            <el-form-item v-if="form.triggerType === 'cron'" label="Cron Expression">
                <el-input v-model="form.cronExpression" size="large" placeholder="0 0 12 * * ?" />
            </el-form-item>
            <el-form-item :label="t('tasks.channels')">
                <el-checkbox-group v-model="form.channels">
                   <el-checkbox-button v-for="c in ['Email', 'WeChat', 'DingTalk', 'SMS', 'Webhook']" :key="c" :label="c">{{c}}</el-checkbox-button>
                </el-checkbox-group>
            </el-form-item>
            
            <div v-if="form.channels.includes('DingTalk')" class="p-4 mb-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg border border-gray-200 dark:border-gray-600">
                <p class="mb-3 text-sm font-bold text-gray-700 dark:text-gray-300">{{ t('tasks.dingTalkConfig') }}</p>
                <el-form-item :label="t('tasks.dingTalkWebhook')">
                    <el-input v-model="form.customData.dingTalkWebhook" placeholder="https://oapi.dingtalk.com/robot/send?access_token=..." />
                </el-form-item>
                <el-form-item :label="t('tasks.dingTalkSecret')">
                    <el-input v-model="form.customData.dingTalkSecret" placeholder="SEC..." show-password />
                </el-form-item>
            </div>

            <div v-if="form.channels.includes('WeChat')" class="p-4 mb-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg border border-gray-200 dark:border-gray-600">
                <p class="mb-3 text-sm font-bold text-gray-700 dark:text-gray-300">{{ t('tasks.wechatConfig') }}</p>
                
                <el-form-item :label="t('tasks.wechatWebhook')">
                    <el-input v-model="form.customData.wechatWebhook" placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..." />
                    <div class="text-xs text-gray-400 mt-1">{{ t('tasks.wechatWebhookHint') }}</div>
                </el-form-item>
                
                <div class="relative flex py-5 items-center">
                    <div class="flex-grow border-t border-gray-300 dark:border-gray-600"></div>
                    <span class="flex-shrink-0 mx-4 text-gray-400 text-xs">{{ t('tasks.orUseAppConfig') }}</span>
                    <div class="flex-grow border-t border-gray-300 dark:border-gray-600"></div>
                </div>

                <el-form-item :label="t('tasks.wechatCorpId')">
                    <el-input v-model="form.customData.wechatCorpId" placeholder="ww..." />
                </el-form-item>
                <el-form-item :label="t('tasks.wechatCorpSecret')">
                    <el-input v-model="form.customData.wechatCorpSecret" placeholder="Secret..." show-password />
                </el-form-item>
                <div class="grid grid-cols-2 gap-4">
                    <el-form-item :label="t('tasks.wechatAgentId')">
                        <el-input v-model="form.customData.wechatAgentId" placeholder="1000002" />
                    </el-form-item>
                    <el-form-item :label="t('tasks.wechatToUser')">
                        <el-input v-model="form.customData.wechatToUser" placeholder="@all" />
                    </el-form-item>
                </div>
            </div>
            <el-form-item :label="t('tasks.messageTemplate')">
                <el-input 
                    v-model="form.messageTemplate" 
                    type="textarea" 
                    :rows="3" 
                    placeholder="Hello ${user}, check out this update!"
                />
            </el-form-item>
        </el-form>
        
        <div class="mt-4 p-4 bg-gray-50 dark:bg-gray-700/30 rounded-lg border border-gray-100 dark:border-gray-700">
            <h3 class="font-bold text-sm mb-3 text-gray-700 dark:text-gray-300">{{ t('tasks.advancedSettings') }}</h3>
            <div class="grid grid-cols-2 gap-4">
                <el-form-item :label="t('tasks.rateLimitEnabled')">
                    <el-switch v-model="form.rateLimitEnabled" />
                </el-form-item>
                 <el-form-item v-if="form.rateLimitEnabled" :label="t('tasks.maxPerHour')">
                    <el-input-number v-model="form.maxPerHour" :min="0" controls-position="right" class="w-full" />
                </el-form-item>
                <el-form-item v-if="form.rateLimitEnabled" :label="t('tasks.maxPerDay')">
                    <el-input-number v-model="form.maxPerDay" :min="0" controls-position="right" class="w-full" />
                </el-form-item>
            </div>
            <div class="grid grid-cols-2 gap-4">
                <el-form-item :label="t('tasks.silentStart')">
                    <el-time-picker v-model="form.silentStart" format="HH:mm" value-format="HH:mm:ss" placeholder="Start time" class="w-full" />
                </el-form-item>
                <el-form-item :label="t('tasks.silentEnd')">
                    <el-time-picker v-model="form.silentEnd" format="HH:mm" value-format="HH:mm:ss" placeholder="End time" class="w-full" />
                </el-form-item>
            </div>
        </div>
        <template #footer>
            <span class="dialog-footer">
                <el-button size="large" @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
                <el-button size="large" type="primary" color="#9333ea" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
            </span>
        </template>
    </el-dialog>
  </div>
</template>
