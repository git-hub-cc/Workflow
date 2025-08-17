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
import { ref, reactive, onMounted } from 'vue';
import { getLoginLogs } from '@/api';
import { message } from 'ant-design-vue';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue';
import dayjs from 'dayjs';

const loading = ref(false);
const dataSource = ref([]);
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`,
});

const filterState = reactive({
  userId: '',
  status: undefined,
  dateRange: [],
});

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime' },
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress' },
  { title: '登录状态', dataIndex: 'status', key: 'status', align: 'center' },
  { title: '失败原因', dataIndex: 'failureReason', key: 'failureReason', ellipsis: true },
  { title: 'User Agent', dataIndex: 'userAgent', key: 'userAgent', ellipsis: true },
];

const fetchLogs = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: 'loginTime,desc',
      userId: filterState.userId || null,
      status: filterState.status || null,
      startTime: filterState.dateRange?.[0] ? filterState.dateRange[0].startOf('day').toISOString() : null,
      endTime: filterState.dateRange?.[1] ? filterState.dateRange[1].endOf('day').toISOString() : null,
    };
    const response = await getLoginLogs(params);
    dataSource.value = response.content;
    pagination.total = response.totalElements;
  } catch (error) {
    message.error('加载登录日志失败');
  } finally {
    loading.value = false;
  }
};

onMounted(fetchLogs);

const handleTableChange = (pager) => {
  pagination.current = pager.current;
  pagination.pageSize = pager.pageSize;
  fetchLogs();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchLogs();
};

const handleReset = () => {
  filterState.userId = '';
  filterState.status = undefined;
  filterState.dateRange = [];
  handleSearch();
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>