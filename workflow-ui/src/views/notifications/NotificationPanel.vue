<template>
  <div class="notification-panel">
    <a-tabs v-model:activeKey="activeTab" centered>
      <a-tab-pane key="unread" :tab="`未读 (${unreadCount})`" />
      <a-tab-pane key="all" tab="全部消息" />
    </a-tabs>

    <a-spin :spinning="loading">
      <a-list
          v-if="filteredNotifications.length > 0"
          item-layout="horizontal"
          :data-source="filteredNotifications"
          class="notification-list"
      >
        <template #renderItem="{ item }">
          <a-list-item
              :class="{ 'notification-unread': !item.isRead }"
              @click="handleItemClick(item)"
          >
            <a-list-item-meta>
              <template #title>
                <a-badge :dot="!item.isRead" :offset="[-8, 4]">
                  <span>{{ item.title }}</span>
                </a-badge>
              </template>
              <template #description>
                <p class="notification-content">{{ item.content }}</p>
                <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
        <!-- 【核心修改】移除加载更多按钮，因为这个面板只显示最新通知 -->
      </a-list>
      <a-empty v-else-if="!loading" description="暂无消息" />
    </a-spin>

    <div class="notification-footer">
      <a-button type="link" @click="handleMarkAllRead" :disabled="unreadCount === 0">全部已读</a-button>
      <!-- 【核心修改】按钮的点击事件 -->
      <a-button type="link" @click="goToNotificationCenter">进入通知中心</a-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useNotificationStore } from '@/stores/notification';

const props = defineProps(['close']);
const emit = defineEmits(['close']);
const router = useRouter();
const store = useNotificationStore();

const activeTab = ref('unread');
const loading = computed(() => store.loading);
const unreadCount = computed(() => store.unreadCount);

const filteredNotifications = computed(() => {
  if (activeTab.value === 'unread') {
    return store.notifications.filter(n => !n.isRead);
  }
  return store.notifications;
});

// 【核心修改】简化逻辑，面板只加载一次最新数据
onMounted(() => {
  // 只加载第一页，即最新的5条通知
  store.fetchNotifications(1, 5);
});

const handleItemClick = (item) => {
  if (!item.isRead) {
    store.markAsRead(item.id);
  }
  if (item.link) {
    router.push(item.link);
    emit('close');
  }
};

const handleMarkAllRead = () => {
  store.markAllAsRead();
};

// 【核心修改】跳转到新的通知中心页面
const goToNotificationCenter = () => {
  router.push({ name: 'notification-center' });
  emit('close');
};

const formatTime = (time) => {
  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / (1000 * 60));
  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes} 分钟前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} 小时前`;
  return date.toLocaleDateString();
};
</script>

<style scoped>
.notification-panel {
  width: 360px;
  display: flex;
  flex-direction: column;
}
.notification-list {
  max-height: 400px;
  overflow-y: auto;
}
.notification-list .ant-list-item {
  cursor: pointer;
  transition: background-color 0.2s;
}
.notification-list .ant-list-item:hover {
  background-color: #f5f5f5;
}
.notification-unread {
  background-color: #e6f7ff;
}
.notification-content {
  margin-bottom: 4px;
}
.notification-time {
  font-size: 12px;
  color: #8c8c8c;
}
.notification-footer {
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  padding: 8px 16px;
}
.load-more {
  text-align: center;
  margin-top: 12px;
  height: 32px;
  line-height: 32px;
}
</style>