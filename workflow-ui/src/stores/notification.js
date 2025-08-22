import { ref } from 'vue';
import { defineStore } from 'pinia';
import { getUnreadNotificationCount, getNotifications, markAllNotificationsAsRead, markNotificationsAsRead } from '@/api';

/**
 * 【新增】用于管理应用内通知的 Pinia Store
 */
export const useNotificationStore = defineStore('notification', () => {
    // --- State ---
    const unreadCount = ref(0);
    const notifications = ref([]);
    const loading = ref(false);
    const pagination = ref({
        current: 1,
        pageSize: 10,
        total: 0,
    });

    // --- Actions ---

    /**
     * 获取未读通知数量
     */
    async function fetchUnreadCount() {
        try {
            const response = await getUnreadNotificationCount();
            unreadCount.value = response.count;
        } catch (error) {
            console.error("获取未读通知数失败:", error);
        }
    }

    /**
     * 获取通知列表（分页）
     */
    async function fetchNotifications(page = 1, size = 10) {
        loading.value = true;
        try {
            const response = await getNotifications({ page: page - 1, size });
            notifications.value = response.content;
            pagination.value.current = page;
            pagination.value.total = response.totalElements;
        } catch (error) {
            console.error("获取通知列表失败:", error);
        } finally {
            loading.value = false;
        }
    }

    /**
     * 将所有通知标记为已读
     */
    async function markAllAsRead() {
        try {
            await markAllNotificationsAsRead();
            notifications.value.forEach(n => n.isRead = true);
            unreadCount.value = 0;
        } catch (error) {
            console.error("标记全部已读失败:", error);
        }
    }

    /**
     * 将单个通知标记为已读
     */
    async function markAsRead(notificationId) {
        try {
            await markNotificationsAsRead([notificationId]);
            const notification = notifications.value.find(n => n.id === notificationId);
            if (notification && !notification.isRead) {
                notification.isRead = true;
                if (unreadCount.value > 0) {
                    unreadCount.value--;
                }
            }
        } catch (error) {
            console.error("标记已读失败:", error);
        }
    }

    return {
        unreadCount,
        notifications,
        loading,
        pagination,
        fetchUnreadCount,
        fetchNotifications,
        markAllAsRead,
        markAsRead,
    };
});