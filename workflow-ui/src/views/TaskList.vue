<template>
  <div class="page-container">
    <a-page-header title="我的待办任务" />
    <div style="padding: 24px;">
      <!-- 搜索区域 -->
      <a-card :bordered="false" style="margin-bottom: 24px;">
        <a-form :model="filterState" layout="inline">
          <a-form-item label="关键字">
            <a-input v-model:value="filterState.keyword" placeholder="按表单名称或提交人搜索" allow-clear />
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
          row-key="camundaTaskId"
          @change="handleTableChange"
          :scroll="{ x: 'max-content' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'taskName'">
            <a @click="handleTaskClick(record)">{{ record.formName }} - {{ record.stepName }}</a>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ new Date(record.createdAt).toLocaleString() }}
          </template>
          <template v-else-if="column.key === 'statusTag'">
            <a-tag v-if="isModificationTask(record)" color="error">待修改</a-tag>
            <a-tag v-else color="processing">待处理</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="primary" @click="handleTaskClick(record)">去处理</a-button>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getPendingTasks } from '@/api';
import { usePaginatedFetch } from '@/composables/usePaginatedFetch.js';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue';

const router = useRouter();

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
    getPendingTasks,
    { keyword: '' },
);

const columns = [
  { title: '任务名称', key: 'taskName' },
  { title: '提交人', dataIndex: 'submitterName', key: 'submitterName', width: 150 },
  { title: '到达时间', dataIndex: 'createdAt', key: 'createdAt', width: 200 },
  { title: '状态', key: 'statusTag', align: 'center', width: 100 },
  { title: '操作', key: 'actions', align: 'center', width: 120 },
];

onMounted(fetchData);

const isModificationTask = (task) => {
  const taskName = (task.stepName || '').trim();
  const modificationKeywords = ['修改', '调整', '重新', '发起', '申请'];
  return modificationKeywords.some(keyword => taskName.includes(keyword));
};

const handleTaskClick = (record) => {
  if (isModificationTask(record)) {
    router.push({
      name: 'form-viewer',
      params: { formId: record.formDefinitionId },
      query: {
        submissionId: record.formSubmissionId,
        taskId: record.camundaTaskId,
      },
    });
  } else {
    router.push({ name: 'task-detail', params: { taskId: record.camundaTaskId } });
  }
};
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