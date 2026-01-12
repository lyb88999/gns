<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Search, Edit, Delete } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import axios from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t } = useI18n()

const members = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(null)

const form = ref({
  username: '',
  email: '',
  role: 'member',
  password: '' // Only for create
})

const fetchMembers = async () => {
    try {
        // Fetch all users as team members for now (Simple Mode)
        const usersRes = await axios.get('/users')
        members.value = usersRes.content || []
    } catch (error) {
        console.error('Failed to fetch members', error)
    }
}

const handleEdit = (row) => {
    isEdit.value = true
    currentId.value = row.id
    form.value = { 
        username: row.username, 
        email: row.email, 
        role: row.role 
    }
    dialogVisible.value = true
}

const handleDelete = (row) => {
    ElMessageBox.confirm(
        'Are you sure you want to delete this member?',
        'Warning',
        {
            confirmButtonText: 'Delete',
            cancelButtonText: 'Cancel',
            type: 'warning',
        }
    ).then(async () => {
        try {
            await axios.delete(`/users/${row.id}`)
            ElMessage.success('Member deleted')
            fetchMembers()
        } catch (error) {
            console.error(error)
        }
    }).catch(() => {})
}

const handleCreate = () => {
    isEdit.value = false
    form.value = { username: '', email: '', role: 'member', password: '' }
    dialogVisible.value = true
}

const handleSubmit = async () => {
    try {
        if (isEdit.value) {
            await axios.put(`/users/${currentId.value}`, form.value)
            ElMessage.success('Member updated')
        } else {
            await axios.post('/users', form.value)
            ElMessage.success('Member added')
        }
        dialogVisible.value = false
        fetchMembers()
    } catch (error) {
        console.error(error)
    }
}

onMounted(() => {
    fetchMembers()
})
</script>

<template>
  <div class="h-full flex flex-col p-8 bg-gray-50 dark:bg-gray-900 transition-colors duration-300">
    <div class="flex justify-between items-center mb-8">
        <div>
            <h1 class="text-2xl font-bold mb-1 text-gray-900 dark:text-white transition-colors">{{ t('teams.title') }}</h1>
            <p class="text-gray-500 dark:text-gray-400 transition-colors">{{ t('teams.subtitle') }}</p>
        </div>
        <el-button type="primary" :icon="Plus" color="#9333ea" @click="handleCreate" class="shadow-lg shadow-purple-900/20">{{ t('teams.createButton') }}</el-button>
    </div>

    <!-- Enhanced Card UI -->
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 shadow-sm dark:shadow-none overflow-hidden flex-1 flex flex-col transition-all duration-300">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700 flex justify-between bg-gray-50/50 dark:bg-gray-800/50">
            <el-input :placeholder="t('common.search')" :prefix-icon="Search" class="w-72" size="large" />
        </div>
        
        <el-table :data="members" 
            style="width: 100%;" 
            class="!bg-transparent"
            :row-style="{ background: 'transparent' }"
            :header-cell-style="{ background: 'transparent' }"
        >
            <el-table-column prop="username" :label="t('teams.name')">
                <template #default="{ row }">
                    <div class="flex items-center">
                        <div class="w-8 h-8 rounded-full bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 flex items-center justify-center text-xs font-bold mr-3">
                            {{ row.username?.substring(0,2).toUpperCase() }}
                        </div>
                        <span class="font-medium text-gray-900 dark:text-gray-200">{{ row.username }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="email" :label="t('teams.email')" />
            <el-table-column prop="role" :label="t('teams.role')">
                <template #default="{ row }">
                    <el-tag :type="row.role === 'admin' ? 'warning' : 'info'" size="small" effect="plain" class="capitalize">
                        {{ row.role === 'admin' ? t('common.admin') : t('common.member') }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column :label="t('common.actions')" width="180">
                <template #default="{ row }">
                    <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">{{ t('common.edit') }}</el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
                </template>
            </el-table-column>
        </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? 'Edit Member' : t('teams.createDialog')" width="500px" class="rounded-xl overflow-hidden !bg-white dark:!bg-gray-800 dark:!text-white">
        <el-form :model="form" label-position="top" class="mt-4">
            <el-form-item :label="t('teams.name')">
                <el-input v-model="form.username" size="large" />
            </el-form-item>
            <el-form-item :label="t('teams.email')">
                <el-input v-model="form.email" size="large" />
            </el-form-item>
            <el-form-item v-if="!isEdit" label="Password">
                <el-input v-model="form.password" type="password" size="large" show-password />
            </el-form-item>
            <el-form-item :label="t('teams.role')">
                 <el-select v-model="form.role" class="w-full" size="large">
                     <el-option value="member" :label="t('common.member')" />
                     <el-option value="admin" :label="t('common.admin')" />
                 </el-select>
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
