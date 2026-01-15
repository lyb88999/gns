<script setup>
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Bell, DataAnalysis, List, Document, Setting, Moon, Sunny, Key, User } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'

const { t } = useI18n()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const { isDark } = storeToRefs(themeStore)
const route = useRoute()

// Helper to determine active state
const isActive = (path) => route.path === path
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-white flex transition-colors duration-300">
    <!-- Sidebar -->
    <div class="w-64 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 flex-shrink-0 flex flex-col transition-colors duration-300">
      <div class="h-16 flex items-center px-6 border-b border-gray-200 dark:border-gray-700">
        <img src="/logo.png" alt="Logo" class="w-8 h-8 mr-2 object-contain" />
        <span class="font-bold text-lg">GNS Console</span>
      </div>
      
      <nav class="p-4 space-y-2 flex-1">
        <router-link to="/" 
          class="flex items-center px-4 py-3 rounded-lg transition"
          :class="isActive('/') ? 'bg-purple-50 dark:bg-gray-900 text-purple-600 dark:text-purple-400' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'"
        >
          <el-icon class="mr-3"><DataAnalysis /></el-icon>
          {{ t('common.dashboard') }}
        </router-link>
        
        <router-link to="/tasks" 
          class="flex items-center px-4 py-3 rounded-lg transition"
          :class="isActive('/tasks') ? 'bg-purple-50 dark:bg-gray-900 text-purple-600 dark:text-purple-400' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'"
        >
          <el-icon class="mr-3"><List /></el-icon>
          {{ t('common.tasks') }}
        </router-link>
        
        <router-link to="/logs" 
          class="flex items-center px-4 py-3 rounded-lg transition"
          :class="isActive('/logs') ? 'bg-purple-50 dark:bg-gray-900 text-purple-600 dark:text-purple-400' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'"
        >
          <el-icon class="mr-3"><Document /></el-icon>
          {{ t('common.logs') }}
        </router-link>

        <div class="px-4 pt-4 pb-2 text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wider">Access</div>

        <router-link to="/tokens" 
          class="flex items-center px-4 py-3 rounded-lg transition"
          :class="isActive('/tokens') ? 'bg-purple-50 dark:bg-gray-900 text-purple-600 dark:text-purple-400' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'"
        >
          <el-icon class="mr-3"><Key /></el-icon>
          {{ t('tokens.title') }}
        </router-link>

        <router-link to="/teams" 
          v-if="['admin', 'team_admin'].includes(authStore.user.role)"
          class="flex items-center px-4 py-3 rounded-lg transition"
          :class="isActive('/teams') ? 'bg-purple-50 dark:bg-gray-900 text-purple-600 dark:text-purple-400' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'"
        >
          <el-icon class="mr-3"><User /></el-icon>
          {{ t('teams.title') }}
        </router-link>
        
        <div class="px-4 pt-4 pb-2 text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wider">System</div>

        <router-link to="/settings" 
          class="flex items-center px-4 py-3 rounded-lg transition"
          :class="isActive('/settings') ? 'bg-purple-50 dark:bg-gray-900 text-purple-600 dark:text-purple-400' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'"
        >
          <el-icon class="mr-3"><Setting /></el-icon>
          {{ t('common.settings') }}
        </router-link>
      </nav>

      <div class="p-4 border-t border-gray-200 dark:border-gray-700">
         <div class="flex items-center justify-between">
            <div class="flex items-center">
                 <div class="w-8 h-8 rounded-full bg-purple-600 text-white flex items-center justify-center text-xs font-bold uppercase">
                    {{ authStore.user.username ? authStore.user.username.substring(0, 2) : 'UE' }}
                 </div>
                 <div class="ml-3">
                     <div class="text-sm font-medium">{{ authStore.user.username || 'User' }}</div>
                 </div>
            </div>
            <!-- Theme Toggle Mini -->
            <button @click="themeStore.toggleTheme" class="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-gray-500">
                <el-icon v-if="isDark"><Moon /></el-icon>
                <el-icon v-else><Sunny /></el-icon>
            </button>
         </div>
      </div>
    </div>

    <!-- Main Content Wrapper -->
    <div class="flex-1 overflow-auto bg-gray-50 dark:bg-gray-900 flex flex-col transition-colors duration-300">
       <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
             <component :is="Component" />
          </transition>
       </router-view>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
