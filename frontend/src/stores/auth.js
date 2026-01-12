import { defineStore } from 'pinia'
import axios from '../utils/request'
import router from '../router'

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem('token') || '',
        user: JSON.parse(localStorage.getItem('user') || '{}'),
    }),
    getters: {
        isLoggedIn: (state) => !!state.token,
        currentUserId: (state) => state.user.id
    },
    actions: {
        async login(username, password) {
            // Mock login for MVP until backend JWT is ready or use real endpoint if available
            // For now we assume backend has /api/v1/auth/login or similar, but based on analysis,
            // the backend currently only has basic User CRUD and Token tables.
            // We will implement a mock login that performs a basic check or uses a dev backdoor
            // until actual AuthController is verified.

            // Checking backend analysis:
            // We saw `UserService` but no explicit `AuthController` in the file list earlier.
            // `NotificationSendService` uses `UserContextHolder`.
            // Let's implement a flexible login that tries to hit an endpoint,
            // but for now, we might need to simulate it or use the existing /users endpoint if possible?
            // Actually, the PRD mentions JWT. Let's assume there is or will be an endpoint.
            // But looking at file list: `com/gns/notification/security` exists.

            try {
                const res = await axios.post('/auth/login', { username, password })
                // Backend returns: { token: '...', user: { ... } }
                this.token = res.token
                this.user = res.user

                localStorage.setItem('token', this.token)
                localStorage.setItem('user', JSON.stringify(this.user))
                router.push('/')
                return true
            } catch (error) {
                console.error('Login failed', error)
                throw error
            }
        },
        async register(username, password, email) {
            try {
                const res = await axios.post('/auth/register', { username, password, email })
                this.token = res.token
                this.user = res.user

                localStorage.setItem('token', this.token)
                localStorage.setItem('user', JSON.stringify(this.user))
                router.push('/')
                return true
            } catch (error) {
                console.error('Registration failed', error)
                throw error
            }
        },
        logout() {
            this.token = ''
            this.user = {}
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            router.push('/login')
        }
    }
})
