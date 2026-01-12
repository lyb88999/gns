import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import Login from '../views/Login.vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'

// Lazy load views
const Dashboard = () => import('../views/Dashboard.vue')
const Tasks = () => import('../views/Tasks.vue')
const Logs = () => import('../views/Logs.vue')
const Settings = () => import('../views/Settings.vue')
const ApiTokens = () => import('../views/ApiTokens.vue')
const Teams = () => import('../views/Teams.vue')

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    {
        path: '/',
        component: DefaultLayout,
        meta: { requiresAuth: true },
        children: [
            {
                path: '',
                name: 'Dashboard',
                component: Dashboard
            },
            {
                path: 'tasks',
                name: 'Tasks',
                component: Tasks
            },
            {
                path: 'logs',
                name: 'Logs',
                component: Logs
            },
            {
                path: 'tokens',
                name: 'ApiTokens',
                component: ApiTokens
            },
            {
                path: 'teams',
                name: 'Teams',
                component: Teams
            },
            {
                path: 'settings',
                name: 'Settings',
                component: Settings
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()
    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
        next('/login')
    } else if (to.path === '/login' && authStore.isLoggedIn) {
        next('/')
    } else {
        next()
    }
})

export default router
