import { ref } from 'vue';
import { defineStore } from 'pinia';
// --- [FIX] Import downloadFile API for secure preview generation ---
import { getPublicSettings, getAdminSettings, updateSettings, downloadFile } from '@/api';
import { message } from 'ant-design-vue';

/**
 * 【新增】用于全局管理系统配置的 Pinia Store
 */
export const useSystemStore = defineStore('system', () => {
    // --- State ---
    const settings = ref(JSON.parse(localStorage.getItem('system_settings')) || {});
    const loading = ref(false);
    // --- [FIX] Add new state to hold the secure blob URL for the icon ---
    const iconBlobUrl = ref(null);
    let currentIconBlobUrl = null; // Internal variable to track for memory management

    // --- Actions ---

    /**
     * 获取公开的系统设置，并存储到 state 和 localStorage
     * 这个方法会在应用启动时调用，确保所有用户（包括未登录的）都能看到正确的系统外观。
     */
    async function fetchPublicSettings() {
        try {
            const data = await getPublicSettings();
            settings.value = data;
            localStorage.setItem('system_settings', JSON.stringify(data));

            // --- [FIX] Securely fetch the icon and create a blob URL ---
            const iconId = data.SYSTEM_ICON_ID;

            // 1. Revoke the old URL to prevent memory leaks
            if (currentIconBlobUrl) {
                URL.revokeObjectURL(currentIconBlobUrl);
                currentIconBlobUrl = null;
                iconBlobUrl.value = null;
            }

            // 2. Fetch the new icon if an ID exists
            if (iconId) {
                try {
                    const response = await downloadFile(iconId);
                    const newBlobUrl = URL.createObjectURL(response.data);
                    iconBlobUrl.value = newBlobUrl;
                    currentIconBlobUrl = newBlobUrl;
                } catch (iconError) {
                    console.error("Failed to fetch system icon:", iconError);
                    iconBlobUrl.value = null; // Fallback if icon download fails
                }
            }
            // --- [FIX END] ---

        } catch (error) {
            console.error("获取公共系统设置失败:", error);
        }
    }

    /**
     * 获取所有系统设置 (仅限管理员页面调用)
     * @returns {Promise<Object>}
     */
    async function fetchAdminSettings() {
        loading.value = true;
        try {
            return await getAdminSettings();
        } catch (error) {
            message.error('加载系统设置失败');
            return {};
        } finally {
            loading.value = false;
        }
    }

    /**
     * 更新系统设置 (仅限管理员页面调用)
     * @param {Object} newSettings - 包含新设置的键值对对象
     */
    async function saveSettings(newSettings) {
        loading.value = true;
        try {
            await updateSettings(newSettings);
            await fetchPublicSettings();
            message.success('系统设置已更新！部分设置可能需要刷新页面才能完全生效。');
        } catch (error) {
            // 错误已由全局拦截器处理
        } finally {
            loading.value = false;
        }
    }


    return {
        settings,
        loading,
        // --- [FIX] Expose the new blob URL state ---
        iconBlobUrl,
        fetchPublicSettings,
        fetchAdminSettings,
        saveSettings,
    };
});