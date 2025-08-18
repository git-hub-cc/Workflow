<template>
  <div class="page-container">
    <a-page-header title="我的待办任务" />
    <div style="padding: 24px;">
      <!-- 【核心新增】搜索区域 -->
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

      <!-- 【核心修改】将 a-list 替换为 a-table -->
      <a-table
          :columns="columns"
          :data-source="dataSource"
          :loading="loading"
          :pagination="pagination"
          row-key="camundaTaskId"
          @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'taskName'">
            <a @click="goToTaskDetail(record.camundaTaskId)">{{ record.formName }} - {{ record.stepName }}</a>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ new Date(record.createdAt).toLocaleString() }}
          </template>
          <template v-else-if="column.key === 'statusTag'">
            <a-tag v-if="isRejectedTask(record)" color="error">待修改</a-tag>
            <a-tag v-else color="processing">待处理</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="primary" @click="goToTaskDetail(record.camundaTaskId)">去处理</a-button>
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

// 【核心修改】使用 usePaginatedFetch hook
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
    // Camunda Task Query 不直接支持按 createTime 排序，故不设置默认排序
);

const columns = [
  { title: '任务名称', key: 'taskName' },
  { title: '提交人', dataIndex: 'submitterName', key: 'submitterName', width: 150 },
  { title: '到达时间', dataIndex: 'createdAt', key: 'createdAt', width: 200 },
  { title: '状态', key: 'statusTag', align: 'center', width: 100 },
  { title: '操作', key: 'actions', align: 'center', width: 120 },
];

onMounted(fetchData);

const isRejectedTask = (task) => {
  return task.stepName.includes('修改') || task.stepName.includes('调整');
};

const goToTaskDetail = (taskId) => {
  router.push({ name: 'task-detail', params: { taskId } });
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>