<template>
  <div class="page-container">
    <a-page-header :title="pageTitle">
      <template #extra>
        <a-button type="primary" @click="handleAddNew">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </template>
    </a-page-header>

    <div style="padding: 24px;">
      <!-- 筛选区域动态生成 -->
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

    <!-- 编辑模态框 -->
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
import { getFormById, getSubmissions, deleteSubmission } from '@/api';
import { message } from 'ant-design-vue';
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { usePaginatedFetch } from '@/composables/usePaginatedFetch.js';
import EditSubmissionModal from '@/views/viewer-components/EditSubmissionModal.vue';

const route = useRoute();
const router = useRouter();

const pageTitle = ref('');
const formId = ref(null);
const menuId = ref(null);

const formDefinition = ref(null);
const filterableFields = ref([]);
const listDisplayFields = ref([]);

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

const columns = computed(() => {
  if (!listDisplayFields.value) return [];

  const baseColumns = [
    { title: '提交人', dataIndex: 'submitterName', key: 'submitterName', width: 120, fixed: 'left' },
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
    { title: '操作', key: 'actions', width: 180, align: 'center', fixed: 'right' },
  ];
  return [...baseColumns, ...dynamicColumns, ...finalColumns];
});

// --- 【核心修复】创建可复用的初始化函数 ---
const initialize = async () => {
  // 从当前路由获取最新的元数据
  pageTitle.value = route.meta.title || '数据列表';
  formId.value = route.meta.formId;
  menuId.value = route.meta.menuId;

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

    // 清空旧的筛选条件并根据新表单定义初始化
    Object.keys(filterState).forEach(key => delete filterState[key]);
    filterableFields.value.forEach(f => {
      filterState[f.id] = undefined;
    });

    await fetchData(); // 初始化完成后，获取第一页数据
  } catch (error) {
    // 错误已由全局拦截器处理，无需再次弹窗
    console.error("初始化页面失败:", error);
  } finally {
    loading.value = false;
  }
};

// --- 【核心修复】使用 onMounted 来处理组件的首次加载 ---
onMounted(() => {
  initialize();
});

// --- 【核心修复】保留 watch 用于处理在不同数据列表页之间的切换 ---
watch(() => route.meta, (newMeta, oldMeta) => {
  // 仅当 menuId 确实发生变化时才重新初始化，避免不必要的重载
  if (newMeta && newMeta.menuId && newMeta.menuId !== oldMeta.menuId) {
    initialize();
  }
});


// --- 其他函数保持不变 ---
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
    await fetchData();
  } catch (error) {
    // error handled by interceptor
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