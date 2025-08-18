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
import { ref, onMounted, computed } from 'vue';
import { getOperationLogs } from '@/api';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue';
// --- 【核心修改】引入 usePaginatedFetch hook ---
import { usePaginatedFetch } from '@/composables/usePaginatedFetch.js';

// --- 【核心修改】使用 hook 管理表格数据和状态 ---
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
    getOperationLogs,
    { operatorId: '', module: undefined, dateRange: [] }, // 初始筛选条件
    { defaultSort: 'operationTime,desc' }
);

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '操作人', key: 'operator', width: 180, sorter: true, dataIndex: 'operatorId' },
  { title: '操作时间', dataIndex: 'operationTime', key: 'operationTime', width: 180, sorter: true },
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress', width: 140 },
  { title: '操作模块', dataIndex: 'module', key: 'module', width: 120, sorter: true },
  { title: '具体操作', dataIndex: 'action', key: 'action', width: 150 },
  { title: '目标ID', dataIndex: 'targetId', key: 'targetId', ellipsis: true },
  { title: '操作详情', key: 'details', align: 'center', width: 120 },
];

onMounted(fetchData);

// Details Modal
const detailsModalVisible = ref(false);
const currentDetails = ref('');
const formattedDetails = computed(() => {
  try {
    const obj = JSON.parse(currentDetails.value);
    return JSON.stringify(obj, null, 2);
  } catch (e) {
    return currentDetails.value;
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