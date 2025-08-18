<template>
  <div class="page-container">
    <a-page-header :title="pageTitle" />

    <div style="padding: 24px;">
      <!-- 筛选区域 -->
      <a-card :bordered="false" style="margin-bottom: 24px;">
        <a-form :model="filterState" layout="inline">
          <!-- 动态生成筛选条件 -->
          <a-form-item v-for="filter in filterConfig" :key="filter.id" :label="filter.label">
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
            <a-button type="link" @click="goToDetail(record.id)">查看详情</a-button>
          </template>
          <template v-else>
            {{ getRecordValue(record, column.dataIndex) }}
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getFormById, getSubmissions } from '@/api';
import { message } from 'ant-design-vue';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue';
import { flattenFields } from '@/utils/formUtils.js';
// --- 【核心修改】引入 usePaginatedFetch hook ---
import { usePaginatedFetch } from '@/composables/usePaginatedFetch.js';

const route = useRoute();
const router = useRouter();

const pageTitle = ref(route.meta.title || '数据列表');
const formId = ref(route.meta.formId);
const formDefinition = ref(null);
const filterConfig = ref([]);

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
    (params) => getSubmissions(formId.value, params),
    {}, // 初始筛选条件为空，将在 initialize 中动态设置
    { defaultSort: 'createdAt,desc' }
);

// 动态生成表格列
const columns = computed(() => {
  const baseColumns = [
    { title: '提交人', dataIndex: 'submitterName', key: 'submitterName', width: 120 },
    { title: '流程状态', dataIndex: 'workflowStatus', key: 'workflowStatus', width: 120, align: 'center' },
  ];
  const dynamicColumns = (formDefinition.value?.schema.fields || []).slice(0, 4)
      .map(field => ({
        title: field.label, dataIndex: field.id, key: field.id, ellipsis: true,
      }));
  const finalColumns = [
    { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 180, sorter: true },
    { title: '操作', key: 'actions', width: 120, align: 'center' },
  ];
  return [...baseColumns, ...dynamicColumns, ...finalColumns];
});

// 获取表单定义并初始化页面
const initialize = async () => {
  if (!formId.value) {
    message.error("页面配置错误：未关联表单定义！");
    return;
  }
  try {
    const res = await getFormById(formId.value);
    res.schema = JSON.parse(res.schemaJson);
    formDefinition.value = res;

    const allFields = flattenFields(res.schema.fields);
    filterConfig.value = allFields.filter(f => ['Input', 'Select', 'DatePicker'].includes(f.type)).slice(0, 2);
    // 动态设置 hook 的筛选条件
    filterConfig.value.forEach(f => {
      if (!(f.id in filterState)) {
        filterState[f.id] = undefined;
      }
    });

    // 解析后的数据源包含 dataJson 的内容
    dataSource.value.forEach(s => Object.assign(s, JSON.parse(s.dataJson)));

    await fetchData();
  } catch (error) {
    message.error("初始化页面失败");
  }
};

// 监听路由变化，以便在同一个组件实例中切换不同菜单
watch(() => route.meta, (newMeta) => {
  if (newMeta && newMeta.formId && newMeta.formId !== formId.value) {
    pageTitle.value = newMeta.title;
    formId.value = newMeta.formId;
    handleReset();
    initialize();
  }
}, { immediate: true });

onMounted(initialize);

const goToDetail = (submissionId) => {
  router.push({ name: 'submission-detail', params: { submissionId } });
};

// --- Helper Functions ---
const getComponentByType = (type) => ({ 'Input': 'a-input', 'DatePicker': 'a-date-picker' }[type] || 'a-input');
const getStatusColor = (status) => ({ '审批中': 'processing', '已通过': 'success', '已拒绝': 'error' }[status] || 'default');
const getRecordValue = (record, dataIndex) => record[dataIndex];
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
</style>