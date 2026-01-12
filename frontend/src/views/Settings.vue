<script setup>
import { useAuthStore } from '../stores/auth'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useThemeStore } from '../stores/theme'
import { storeToRefs } from 'pinia'
import { Moon, Sunny, Monitor } from '@element-plus/icons-vue'

const { t, locale } = useI18n()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const { isDark } = storeToRefs(themeStore)
const router = useRouter()

const handleLogout = () => {
    authStore.logout()
}

// Since isDark is a ref from Pinia, v-model will update it.
// The watch in the store will handle the side effects.
// We DO NOT need @change="themeStore.toggleTheme" as that would double-toggle.
</script>

<template>
  <div class="h-full flex flex-col p-8 bg-gray-50 dark:bg-gray-900 transition-colors duration-300">
    <div class="mb-8">
        <h1 class="text-2xl font-bold mb-1 text-gray-900 dark:text-white transition-colors">{{ t('common.settings') }}</h1>
        <p class="text-gray-500 dark:text-gray-400 transition-colors">{{ t('dashboard.title') }} configuration and preferences.</p>
    </div>

    <!-- Removed max-w-3xl to use full width -->
    <div class="space-y-6">
        <!-- Appearance Section -->
        <section class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6 shadow-sm transition-all duration-300">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-6 flex items-center">
                <el-icon class="mr-2"><Monitor /></el-icon> {{ t('settings.appearance') }}
            </h3>
            
            <div class="flex items-center justify-between py-4 border-b border-gray-100 dark:border-gray-700/50 last:border-0">
                <div>
                    <div class="font-medium text-gray-900 dark:text-gray-200">{{ t('settings.darkMode') }}</div>
                    <div class="text-sm text-gray-500">{{ t('settings.darkModeDesc') }}</div>
                </div>
                <el-switch 
                    v-model="isDark" 
                    size="large" 
                    inline-prompt 
                    :active-icon="Moon" 
                    :inactive-icon="Sunny" 
                    style="--el-switch-on-color: #374151; --el-switch-off-color: #d1d5db" 
                />
            </div>

            <div class="flex items-center justify-between py-4">
                <div>
                    <div class="font-medium text-gray-900 dark:text-gray-200">{{ t('settings.language') }}</div>
                    <div class="text-sm text-gray-500">{{ t('settings.languageDesc') }}</div>
                </div>
                <el-radio-group v-model="locale" size="large">
                    <el-radio-button label="en">English</el-radio-button>
                    <el-radio-button label="zh">中文</el-radio-button>
                </el-radio-group>
            </div>
        </section>

        <!-- Account Section -->
        <section class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6 shadow-sm transition-all duration-300">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-6 text-red-500">{{ t('settings.account') }}</h3>
            
            <div class="flex items-center justify-between">
                <div>
                    <div class="font-medium text-gray-900 dark:text-gray-200">{{ t('settings.signOut') }}</div>
                    <div class="text-sm text-gray-500">{{ t('settings.signOutDesc') }}</div>
                </div>
                <el-button type="danger" plain @click="handleLogout">{{ t('common.logout') }}</el-button>
            </div>
        </section>
    </div>
  </div>
</template>
