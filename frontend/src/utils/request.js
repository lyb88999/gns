import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
    baseURL: '/api/v1',
    timeout: 5000
})

service.interceptors.request.use(
    config => {
        // We access localStorage directly here to avoid circular dependency with Pinia store
        // which imports this request instance.
        const token = localStorage.getItem('token')
        const userStr = localStorage.getItem('user')
        const user = userStr ? JSON.parse(userStr) : {}

        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }

        // Pass User ID for the backend interceptor (as required by README)
        // Removed: X-USER-ID is no longer used for security check
        // if (user.id) {
        //    config.headers['X-USER-ID'] = user.id.toString()
        // }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

service.interceptors.response.use(
    response => {
        return response.data
    },
    error => {
        const msg = error.response?.data?.error || error.response?.data?.message || error.message || 'Request Error'
        ElMessage.error(msg)
        // Optional: Auto logout on 401
        // Optional: Auto logout on 401, but NOT if we are already logging in
        if (error.response?.status === 401 && !error.config.url.includes('/auth/login')) {
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            window.location.href = '/login'
        }
        return Promise.reject(error)
    }
)

export default service
