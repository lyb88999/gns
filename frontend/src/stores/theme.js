import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
    // Initialize from localStorage or system preference
    const isDark = ref(localStorage.getItem('theme') === 'dark' ||
        (!localStorage.getItem('theme') && window.matchMedia('(prefers-color-scheme: dark)').matches))

    const toggleTheme = () => {
        isDark.value = !isDark.value
    }

    // Watch changes and apply to HTML element
    watch(isDark, (val) => {
        const html = document.documentElement
        if (val) {
            html.classList.add('dark')
            localStorage.setItem('theme', 'dark')
        } else {
            html.classList.remove('dark')
            localStorage.setItem('theme', 'light')
        }
    }, { immediate: true })

    return { isDark, toggleTheme }
})
