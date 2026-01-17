<script setup>
import { ref, computed } from 'vue'
import { ChatLineRound, VideoPlay, Loading } from '@element-plus/icons-vue'
import axios from '../utils/request'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const emit = defineEmits(['task-parsed'])

const prompt = ref('')
const loading = ref(false)
const conversation = ref([
    { role: 'system', content: computed(() => t('ai.welcome')) }
])

const handleSend = async () => {
    if (!prompt.value.trim()) return

    const userMsg = prompt.value
    conversation.value.push({ role: 'user', content: userMsg })
    prompt.value = ''
    loading.value = true

    try {
        const res = await axios.post('/ai/parse-task', { prompt: userMsg })
        const result = res // DTO: { name, cron, channel, ... }
        
        conversation.value.push({ 
            role: 'assistant', 
            content: t('ai.success', { name: result.name, explanation: result.explanation || '' }),
            isAction: true,
            data: result
        })
    } catch (error) {
        console.error(error)
        conversation.value.push({ role: 'assistant', content: t('ai.error') })
    } finally {
        loading.value = false
    }
}

const applyTask = (data) => {
    emit('task-parsed', data)
}
</script>

<template>
    <div class="flex flex-col h-full bg-gray-50 dark:bg-gray-900/50 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
        <!-- Header -->
        <div class="px-4 py-3 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex items-center">
            <el-icon class="mr-2 text-purple-600"><ChatLineRound /></el-icon>
            <span class="font-bold text-gray-700 dark:text-white">{{ t('ai.title') }}</span>
        </div>

        <!-- Chat Area -->
        <div class="flex-1 overflow-y-auto p-4 space-y-4">
            <div v-for="(msg, idx) in conversation" :key="idx" class="flex" :class="msg.role === 'user' ? 'justify-end' : 'justify-start'">
                <div 
                   class="max-w-[85%] rounded-2xl px-4 py-3 text-sm shadow-sm"
                   :class="msg.role === 'user' 
                       ? 'bg-purple-600 text-white rounded-br-none' 
                       : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 border border-gray-100 dark:border-gray-700 rounded-bl-none'"
                >
                    <div class="whitespace-pre-wrap leading-relaxed">{{ msg.content }}</div>
                    
                    <!-- Action Button for Assistant's Config Proposal -->
                    <div v-if="msg.isAction" class="mt-3 pt-3 border-t border-gray-100 dark:border-gray-700">
                        <el-button size="small" type="primary" plain @click="applyTask(msg.data)">{{ t('ai.apply') }}</el-button>
                    </div>
                </div>
            </div>
            <div v-if="loading" class="flex justify-start">
                 <div class="bg-white dark:bg-gray-800 rounded-2xl px-4 py-3 rounded-bl-none border border-gray-100 dark:border-gray-700">
                     <el-icon class="is-loading text-purple-600"><Loading /></el-icon>
                 </div>
            </div>
        </div>

        <!-- Input Area -->
        <div class="p-3 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700">
            <div class="relative">
                <el-input 
                    v-model="prompt" 
                    :placeholder="t('ai.placeholder')" 
                    @keydown.enter.prevent="handleSend"
                    :disabled="loading"
                    class="custom-input"
                >
                    <template #suffix>
                        <el-button 
                            type="primary" 
                            circle 
                            :icon="VideoPlay"
                            @click="handleSend"
                            :disabled="!prompt.trim() || loading"
                            color="#9333ea"
                            size="small"
                        />
                    </template>
                </el-input>
            </div>
        </div>
    </div>
</template>

<style scoped>
/* Fix element-plus input suffix alignment if needed */
.custom-input :deep(.el-input__wrapper) {
    border-radius: 9999px;
    padding-right: 8px;
    background-color: var(--el-fill-color-light);
    box-shadow: none;
}
.custom-input :deep(.el-input__inner) {
    background: transparent;
}
</style>
