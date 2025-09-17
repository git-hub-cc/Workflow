<template>
  <div class="page-container">
    <a-page-header title="登录日志" sub-title="查询用户的登录尝试记录" />
    <div style="padding: 24px;">
      <!-- 筛选区域 -->
      <a-card :bordered="false" style="margin-bottom: 24px;">
        <a-form :model="filterState" layout="inline">
          <a-form-item label="用户ID">
            <a-input v-model:value="filterState.userId" placeholder="请输入用户ID" allow-clear />
          </a-form-item>
          <a-form-item label="登录状态">
            <a-select v-model:value="filterState.status" placeholder="请选择状态" style="width: 120px" allow-clear>
              <a-select-option value="SUCCESS">成功</a-select-option>
              <a-select-option value="FAILURE">失败</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="登录时间">
            <a-range-picker v-model:value="filterState.dateRange" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <!-- 表格区域 -->
      <a-table
          :columns="columns"
          :data-source="dataSource"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
          :scroll="{ x: 'max-content' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'SUCCESS' ? 'success' : 'error'">
              {{ record.status === 'SUCCESS' ? '成功' : '失败' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'loginTime'">
            {{ new Date(record.loginTime).toLocaleString() }}
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { getLoginLogs } from '@/api';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue';
import { usePaginatedFetch } from '@/composables/usePaginatedFetch.js';

const {
  loading,
  dataSource,
  pagination,
  filterState,
  handleTableChange,
  handleSearch,
  handleReset,
  fetchData,
} = usePaginatedFetch(
    getLoginLogs,
    { userId: '', status: undefined, dateRange: [] },
    { defaultSort: 'loginTime,desc' }
);

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', sorter: true },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime', sorter: true, width: 200 },
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress' },
  { title: '登录状态', dataIndex: 'status', key: 'status', align: 'center', sorter: true, width: 100 },
  { title: '失败原因', dataIndex: 'failureReason', key: 'failureReason', ellipsis: true },
  { title: 'User Agent', dataIndex: 'userAgent', key: 'userAgent', ellipsis: true },
];

onMounted(fetchData);

</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
@media (max-width: 768px) {
  :deep(.ant-form-inline .ant-form-item) {
    margin-bottom: 16px;
  }
}
</style>