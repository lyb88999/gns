<script setup>
import { ref, reactive } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const authStore = useAuthStore()
const loading = ref(false)

const isRegister = ref(false)
const form = reactive({
  username: '',
  password: '',
  email: ''
})

const handleLogin = async () => {
  loading.value = true
  try {
    if (isRegister.value) {
        await authStore.register(form.username, form.password, form.email)
        ElMessage.success(t('login.registerSuccess'))
    } else {
        await authStore.login(form.username, form.password)
        ElMessage.success(t('login.success'))
    }
// ... (omitting intermediate code for brevity in tool call, will target specific blocks)
// Actually I need to do multiple replace calls or one big one.
// Let's do logical chunks.
  } catch (err) {
    // Error is handled by request interceptor
    console.error(err)
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-900 px-4">
    <div class="w-full max-w-md bg-gray-800 rounded-lg shadow-xl overflow-hidden border border-gray-700">
      <div class="p-8">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-white mb-2">{{ isRegister ? t('login.createAccount') : t('login.welcome') }}</h1>
          <p class="text-gray-400">{{ isRegister ? t('login.join') : t('login.subtitle') }}</p>
        </div>
        
        <form @submit.prevent="handleLogin" class="space-y-6">
          <div>
            <label class="block text-sm font-medium text-gray-400 mb-1">{{ t('login.username') }}</label>
            <el-input 
              v-model="form.username" 
              placeholder="admin" 
              size="large"
              class="w-full"
            />
          </div>
          
          <div v-if="isRegister">
            <label class="block text-sm font-medium text-gray-400 mb-1">{{ t('login.email') }}</label>
            <el-input 
              v-model="form.email" 
              placeholder="user@example.com" 
              size="large"
              class="w-full"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-400 mb-1">{{ t('login.password') }}</label>
            <el-input 
              v-model="form.password" 
              type="password" 
              placeholder="••••••••" 
              size="large"
              show-password
              class="w-full"
            />
          </div>

          <el-button 
            type="primary" 
            size="large" 
            class="w-full !bg-purple-600 !border-purple-600 hover:!bg-purple-500" 
            :loading="loading"
            native-type="submit"
          >
            {{ isRegister ? t('login.register') : t('login.submit') }}
          </el-button>
        </form>
        
        <div class="mt-6 text-center text-sm text-gray-400">
           <span v-if="!isRegister">
               {{ t('login.noAccount') }} 
               <a href="#" @click.prevent="isRegister = true" class="text-purple-400 hover:text-purple-300">{{ t('login.registerLink') }}</a>
           </span>
           <span v-else>
               {{ t('login.haveAccount') }} 
               <a href="#" @click.prevent="isRegister = false" class="text-purple-400 hover:text-purple-300">{{ t('login.loginLink') }}</a>
           </span>
        </div>
        
        <div v-if="!isRegister" class="mt-4 text-center text-xs text-gray-500">
          Default: admin / 123456
        </div>
      </div>
    </div>
  </div>
</template>
