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
        <template #loadMore>
          <div v-if="!loading && hasMore" class="load-more">
            <a-button @click="loadMore" size="small">加载更多</a-button>
          </div>
        </template>
      </a-list>
      <a-empty v-else-if="!loading" description="暂无消息" />
    </a-spin>

    <div class="notification-footer">
      <a-button type="link" @click="handleMarkAllRead" :disabled="unreadCount === 0">全部已读</a-button>
      <a-button type="link" @click="goToNotificationCenter">查看全部</a-button>
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

const hasMore = computed(() => store.pagination.total > store.notifications.length);

onMounted(() => {
  store.fetchNotifications(1, 10);
});

watch(activeTab, () => {
  // 切换 tab 时，如果列表为空，则重新加载第一页
  if (store.notifications.length === 0) {
    store.fetchNotifications(1, 10);
  }
});

const loadMore = () => {
  const nextPage = store.pagination.current + 1;
  store.fetchNotifications(nextPage, store.pagination.pageSize);
};

const handleItemClick = (item) => {
  if (!item.isRead) {
    store.markAsRead(item.id);
  }
  if (item.link) {
    router.push(item.link);
    emit('close'); // 点击后关闭 Popover
  }
};

const handleMarkAllRead = () => {
  store.markAllAsRead();
};

const goToNotificationCenter = () => {
  // 未来可以跳转到独立的通知中心页面
  // router.push('/notifications');
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