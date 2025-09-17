<template>
  <div class="page-container">
    <a-page-header title="我的已办任务" sub-title="查询我处理过的所有任务记录" />
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
            <a @click="goToDetail(record.formSubmissionId)">{{ record.formName }} - {{ record.stepName }}</a>
          </template>
          <template v-else-if="column.key === 'endTime'">
            {{ new Date(record.endTime).toLocaleString() }}
          </template>
          <template v-else-if="column.key === 'duration'">
            {{ formatDuration(record.durationInMillis) }}
          </template>
          <template v-else-if="column.key === 'decision'">
            <a-tag :color="getDecisionColor(record.decision)">{{ getDecisionText(record.decision) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="goToDetail(record.formSubmissionId)">查看详情</a-button>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getCompletedTasks } from '@/api';
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
    getCompletedTasks,
    { keyword: '' }
);

const columns = [
  { title: '任务名称', key: 'taskName' },
  { title: '提交人', dataIndex: 'submitterName', key: 'submitterName', width: 150 },
  { title: '完成时间', dataIndex: 'endTime', key: 'endTime', width: 200 },
  { title: '处理耗时', key: 'duration', align: 'center', width: 150 },
  { title: '处理决策', key: 'decision', align: 'center', width: 120 },
  { title: '操作', key: 'actions', align: 'center', width: 120 },
];

onMounted(fetchData);

const formatDuration = (ms) => {
  if (!ms || ms < 0) return '-';
  let seconds = Math.floor(ms / 1000);
  let minutes = Math.floor(seconds / 60);
  let hours = Math.floor(minutes / 60);
  seconds %= 60;
  minutes %= 60;
  return `${hours > 0 ? hours + 'h ' : ''}${minutes > 0 ? minutes + 'm ' : ''}${seconds}s`;
};

const getDecisionColor = (decision) => {
  switch (decision) {
    case 'APPROVED':
      return 'success';
    case 'REJECTED':
      return 'error';
    case 'RETURN_TO_INITIATOR':
    case 'RETURN_TO_PREVIOUS':
      return 'warning';
    default:
      return 'default';
  }
};

const getDecisionText = (decision) => {
  switch (decision) {
    case 'APPROVED':
      return '同意';
    case 'REJECTED':
      return '拒绝';
    case 'RETURN_TO_INITIATOR':
      return '打回至发起人';
    case 'RETURN_TO_PREVIOUS':
      return '打回上一节点';
    default:
      return '未知';
  }
};

const goToDetail = (submissionId) => {
  if (!submissionId) return;
  router.push({ name: 'submission-detail', params: { submissionId } });
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