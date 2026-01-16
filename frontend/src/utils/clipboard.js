import { ElMessage } from 'element-plus'

/**
 * Copies text to clipboard with fallback for non-secure contexts.
 * @param {string} text - The text to copy
 * @param {function} t - i18n translation function (optional)
 * @returns {Promise<boolean>} - Success or failure
 */
export const copyText = async (text, t = null) => {
    if (!text) return false

    try {
        // Try modern API first
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text)
            if (t) ElMessage.success(t('common.copied') || 'Copied')
            return true
        }
    } catch (err) {
        console.warn('Clipboard API failed, trying fallback...', err)
    }

    // Fallback: Create textarea
    try {
        const textArea = document.createElement("textarea")
        textArea.value = text

        // Ensure it's not visible but part of DOM
        textArea.style.position = "fixed"
        textArea.style.left = "-9999px"
        textArea.style.top = "0"
        document.body.appendChild(textArea)

        textArea.focus()
        textArea.select()

        const successful = document.execCommand('copy')
        document.body.removeChild(textArea)

        if (successful) {
            if (t) ElMessage.success(t('common.copied') || 'Copied')
            return true
        } else {
            if (t) ElMessage.error('Clone failed')
            return false
        }
    } catch (err) {
        console.error('Copy failed', err)
        if (t) ElMessage.error('Copy failed')
        return false
    }
}
