<template>
  <div class="page-container">
    <a-page-header title="通知中心" sub-title="查看所有系统通知和消息" />
    <div style="padding: 24px;">
      <a-card :bordered="false">
        <!-- 操作按钮 -->
        <div style="margin-bottom: 16px;">
          <a-space>
            <a-button type="primary" @click="handleMarkAllRead" :disabled="unreadCount === 0">
              <template #icon><CheckCircleOutlined /></template>
              全部已读
            </a-button>
            <a-button @click="fetchData" :loading="loading">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </a-space>
        </div>

        <!-- 通知列表 -->
        <a-table
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :pagination="pagination"
            row-key="id"
            @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'title'">
              <a-badge :dot="!record.isRead">
                <a @click="handleItemClick(record)" class="notification-title">{{ record.title }}</a>
              </a-badge>
            </template>
            <template v-else-if="column.key === 'createdAt'">
              {{ new Date(record.createdAt).toLocaleString() }}
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="record.isRead ? 'default' : 'processing'">
                {{ record.isRead ? '已读' : '未读' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-button type="link" size="small" @click="handleItemClick(record)">
                {{ record.link ? '查看详情' : '标记已读' }}
              </a-button>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getNotifications, markAllNotificationsAsRead, markNotificationsAsRead } from '@/api';
import { usePaginatedFetch } from '@/composables/usePaginatedFetch.js';
import { useNotificationStore } from '@/stores/notification';
import { message } from 'ant-design-vue';
import { CheckCircleOutlined, ReloadOutlined } from '@ant-design/icons-vue';

const router = useRouter();
const notificationStore = useNotificationStore();

// 使用 usePaginatedFetch Hook 管理表格数据
const {
  loading,
  dataSource,
  pagination,
  filterState, // 虽然这里没用筛选，但hook会返回
  handleTableChange,
  handleSearch, // 虽无搜索框，但刷新按钮可复用此方法
  handleReset,
  fetchData,
} = usePaginatedFetch(
    getNotifications,
    {}, // 无初始筛选条件
    { defaultSort: 'createdAt,desc' } // 默认按创建时间降序
);

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '内容', dataIndex: 'content', key: 'content', ellipsis: true },
  { title: '状态', key: 'status', align: 'center', width: 100 },
  { title: '接收时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', align: 'center', width: 120 },
];

const unreadCount = notificationStore.unreadCount;

onMounted(fetchData);

// 点击通知项（标题或操作按钮）
const handleItemClick = async (record) => {
  // 如果未读，则标记为已读
  if (!record.isRead) {
    try {
      await markNotificationsAsRead([record.id]);
      // 更新本地数据状态和全局未读数
      record.isRead = true;
      await notificationStore.fetchUnreadCount();
    } catch (error) {
      // 错误已全局处理
    }
  }
  // 如果有链接，则跳转
  if (record.link) {
    router.push(record.link);
  }
};

// 全部标记为已读
const handleMarkAllRead = async () => {
  try {
    await markAllNotificationsAsRead();
    // 更新本地所有数据的状态
    dataSource.value.forEach(item => item.isRead = true);
    // 更新全局未读数
    await notificationStore.fetchUnreadCount();
    message.success('所有通知已标记为已读');
  } catch (error) {
    // 错误已全局处理
  }
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
.notification-title {
  font-weight: 500;
}
</style>