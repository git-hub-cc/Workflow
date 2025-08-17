<template>
  <div class="page-container">
    <a-page-header title="操作日志" sub-title="查询系统关键操作的审计记录" />
    <div style="padding: 24px;">
      <!-- 筛选区域 -->
      <a-card :bordered="false" style="margin-bottom: 24px;">
        <a-form :model="filterState" layout="inline">
          <a-form-item label="操作人ID">
            <a-input v-model:value="filterState.operatorId" placeholder="请输入操作人ID" allow-clear />
          </a-form-item>
          <a-form-item label="操作模块">
            <a-select v-model:value="filterState.module" placeholder="请选择模块" style="width: 150px" allow-clear>
              <a-select-option value="用户管理">用户管理</a-select-option>
              <a-select-option value="角色管理">角色管理</a-select-option>
              <a-select-option value="用户组管理">用户组管理</a-select-option>
              <a-select-option value="流程管理">流程管理</a-select-option>
              <a-select-option value="实例管理">实例管理</a-select-option>
              <a-select-option value="任务处理">任务处理</a-select-option>
              <a-select-option value="表单管理">表单管理</a-select-option>
              <a-select-option value="表单提交">表单提交</a-select-option>
              <a-select-option value="个人中心">个人中心</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="操作时间">
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
          <template v-if="column.key === 'operationTime'">
            {{ new Date(record.operationTime).toLocaleString() }}
          </template>
          <template v-if="column.key === 'operator'">
            {{ record.operatorName }} ({{ record.operatorId }})
          </template>
          <template v-if="column.key === 'details'">
            <a-button type="link" size="small" @click="showDetailsModal(record.details)">
              查看详情
            </a-button>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 详情展示弹窗 -->
    <a-modal v-model:open="detailsModalVisible" title="操作详情" :footer="null">
      <pre style="background-color: #f5f5f5; padding: 16px; border-radius: 4px; max-height: 60vh; overflow: auto;"><code>{{ formattedDetails }}</code></pre>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { getOperationLogs } from '@/api';
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
  operatorId: '',
  module: undefined,
  dateRange: [],
});

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '操作人', key: 'operator', width: 180 },
  { title: '操作时间', dataIndex: 'operationTime', key: 'operationTime', width: 180 },
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress', width: 140 },
  { title: '操作模块', dataIndex: 'module', key: 'module', width: 120 },
  { title: '具体操作', dataIndex: 'action', key: 'action', width: 150 },
  { title: '目标ID', dataIndex: 'targetId', key: 'targetId', ellipsis: true },
  { title: '操作详情', key: 'details', align: 'center', width: 120 },
];

const fetchLogs = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: 'operationTime,desc',
      operatorId: filterState.operatorId || null,
      module: filterState.module || null,
      startTime: filterState.dateRange?.[0] ? filterState.dateRange[0].startOf('day').toISOString() : null,
      endTime: filterState.dateRange?.[1] ? filterState.dateRange[1].endOf('day').toISOString() : null,
    };
    const response = await getOperationLogs(params);
    dataSource.value = response.content;
    pagination.total = response.totalElements;
  } catch (error) {
    message.error('加载操作日志失败');
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
  filterState.operatorId = '';
  filterState.module = undefined;
  filterState.dateRange = [];
  handleSearch();
};

// Details Modal
const detailsModalVisible = ref(false);
const currentDetails = ref('');

const formattedDetails = computed(() => {
  try {
    const obj = JSON.parse(currentDetails.value);
    return JSON.stringify(obj, null, 2); // 格式化JSON
  } catch (e) {
    return currentDetails.value; // 如果不是JSON，直接返回原文
  }
});

const showDetailsModal = (details) => {
  currentDetails.value = details;
  detailsModalVisible.value = true;
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>