<template>
  <div class="page-container">
    <a-page-header title="我的申请" sub-title="跟踪我发起的所有流程和申请" />

    <div style="padding: 24px;">
      <!-- 【核心新增】筛选区域 -->
      <a-card :bordered="false" style="margin-bottom: 24px;">
        <a-form :model="filterState" layout="inline">
          <a-form-item label="关键字">
            <a-input v-model:value="filterState.keyword" placeholder="按表单名称搜索" allow-clear />
          </a-form-item>
          <a-form-item label="流程状态">
            <a-select v-model:value="filterState.status" placeholder="请选择状态" style="width: 150px" allow-clear>
              <a-select-option value="PROCESSING">审批中</a-select-option>
              <a-select-option value="APPROVED">已通过</a-select-option>
              <a-select-option value="REJECTED">已拒绝</a-select-option>
            </a-select>
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

      <!-- 【核心修改】表格区域，绑定分页 hook 返回的状态 -->
      <a-table
          :columns="columns"
          :data-source="dataSource"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createdAt'">
            {{ new Date(record.createdAt).toLocaleString() }}
          </template>
          <template v-else-if="column.key === 'workflowStatus'">
            <a-tag :color="getStatusColor(record.workflowStatus)">
              {{ record.workflowStatus }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="goToDetail(record.id)">
              查看详情
            </a-button>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getMySubmissions } from '@/api';
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
    getMySubmissions,
    { keyword: '', status: undefined },
    { defaultSort: 'createdAt,desc' }
);

const columns = [
  { title: '申请ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '表单名称', dataIndex: 'formName', key: 'formName' },
  { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 200, sorter: true },
  { title: '流程状态', dataIndex: 'workflowStatus', key: 'workflowStatus', align: 'center', width: 120 },
  { title: '操作', key: 'actions', align: 'center', width: 120 },
];

// 页面加载时获取第一页数据
onMounted(fetchData);

const getStatusColor = (status) => {
  const colorMap = {
    '审批中': 'processing',
    '已通过': 'success',
    '已拒绝': 'error',
    '无流程': 'default',
  };
  return colorMap[status] || 'default';
};

const goToDetail = (submissionId) => {
  router.push({ name: 'submission-detail', params: { submissionId } });
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>