<template>
  <div class="page-container">
    <a-page-header :title="pageTitle">
      <!-- 【新增】新增按钮 -->
      <template #extra>
        <a-button type="primary" @click="handleAddNew">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </template>
    </a-page-header>

    <div style="padding: 24px;">
      <!-- 【核心修改】筛选区域动态生成 -->
      <a-card :bordered="false" style="margin-bottom: 24px;">
        <a-form :model="filterState" layout="inline">
          <a-form-item v-for="filter in filterableFields" :key="filter.id" :label="filter.label">
            <component
                :is="getComponentByType(filter.type)"
                v-model:value="filterState[filter.id]"
                :placeholder="`请输入${filter.label}`"
                style="width: 180px;"
                allow-clear
            />
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
          <template v-if="column.key === 'workflowStatus'">
            <a-tag :color="getStatusColor(record.workflowStatus)">{{ record.workflowStatus }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ new Date(record.createdAt).toLocaleString() }}
          </template>
          <!-- 【核心修改】操作列 -->
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="goToDetail(record.id)">查看</a-button>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-popconfirm
                  title="确定要删除这条记录吗？"
                  content="如果有关联的流程正在运行，删除将失败。"
                  @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
          <template v-else>
            {{ record.dataJson ? JSON.parse(record.dataJson)[column.dataIndex] : 'N/A' }}
          </template>
        </template>
      </a-table>
    </div>

    <!-- 【新增】编辑模态框 -->
    <EditSubmissionModal
        v-model:open="editModalVisible"
        :submission-id="currentEditingId"
        @refresh="fetchData"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
// 【核心修改】引入新的API
import { getFormById, getSubmissions, deleteSubmission } from '@/api';
import { message, Modal } from 'ant-design-vue';
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { usePaginatedFetch } from '@/composables/usePaginatedFetch.js';
// 【核心修改】引入新增的组件
import EditSubmissionModal from '@/views/viewer-components/EditSubmissionModal.vue';

const route = useRoute();
const router = useRouter();

const pageTitle = ref(route.meta.title || '数据列表');
const formId = ref(route.meta.formId);
const menuId = ref(route.meta.menuId);

// 【核心修改】动态字段配置状态
const formDefinition = ref(null);
const filterableFields = ref([]);
const listDisplayFields = ref([]);

// 【新增】编辑模态框状态
const editModalVisible = ref(false);
const currentEditingId = ref(null);

const apiFunction = (params) => getSubmissions(formId.value, { ...params, menuId: menuId.value });

const {
  loading,
  dataSource,
  pagination,
  filterState,
  handleTableChange,
  handleSearch,
  handleReset,
  fetchData,
} = usePaginatedFetch(apiFunction, {}, { defaultSort: 'createdAt,desc' });

// 【核心修改】动态生成表格列
const columns = computed(() => {
  if (!listDisplayFields.value) return [];
  const baseColumns = [
    { title: '提交人', dataIndex: 'submitterName', key: 'submitterName', width: 120 },
    { title: '流程状态', dataIndex: 'workflowStatus', key: 'workflowStatus', width: 120, align: 'center' },
  ];

  const dynamicColumns = listDisplayFields.value.map(field => ({
    title: field.label,
    dataIndex: field.id,
    key: field.id,
    ellipsis: true,
  }));

  const finalColumns = [
    { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 180, sorter: true },
    { title: '操作', key: 'actions', width: 180, align: 'center' },
  ];
  return [...baseColumns, ...dynamicColumns, ...finalColumns];
});

const initialize = async () => {
  if (!formId.value) {
    message.error("页面配置错误：未关联表单定义！");
    return;
  }
  loading.value = true;
  try {
    const res = await getFormById(formId.value);
    formDefinition.value = res;
    filterableFields.value = res.filterableFields || [];
    listDisplayFields.value = res.listDisplayFields || [];

    // 初始化筛选条件
    filterableFields.value.forEach(f => {
      if (!(f.id in filterState)) {
        filterState[f.id] = undefined;
      }
    });

    await fetchData();
  } catch (error) {
    message.error("初始化页面失败");
  } finally {
    loading.value = false;
  }
};

watch(() => route.meta, (newMeta) => {
  if (newMeta && newMeta.menuId && newMeta.menuId !== menuId.value) {
    pageTitle.value = newMeta.title;
    formId.value = newMeta.formId;
    menuId.value = newMeta.menuId;
    handleReset();
    initialize();
  }
}, { immediate: true });

// --- 【新增】操作处理函数 ---
const handleAddNew = () => {
  router.push({ name: 'form-viewer', params: { formId: formId.value } });
};

const handleEdit = (record) => {
  currentEditingId.value = record.id;
  editModalVisible.value = true;
};

const handleDelete = async (record) => {
  try {
    await deleteSubmission(record.id);
    message.success("删除成功！");
    await fetchData(); // 刷新列表
  } catch (error) {
    // 错误已由API拦截器处理
  }
};

const goToDetail = (submissionId) => {
  router.push({ name: 'submission-detail', params: { submissionId } });
};

const getComponentByType = (type) => ({ 'Input': 'a-input', 'DatePicker': 'a-date-picker', 'Select': 'a-select' }[type] || 'a-input');
const getStatusColor = (status) => ({ '审批中': 'processing', '已通过': 'success', '已拒绝': 'error' }[status] || 'default');
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
</style>