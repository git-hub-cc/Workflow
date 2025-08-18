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
            <!-- 【核心修改】从 record 中直接取值，因为后端返回的数据已包含 dataJson 的内容 -->
            {{ record[column.dataIndex] }}
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
import { usePaginatedFetch } from '@/composables/usePaginatedFetch.js';

const route = useRoute();
const router = useRouter();

const pageTitle = ref(route.meta.title || '数据列表');
const formId = ref(route.meta.formId);
const menuId = ref(route.meta.menuId); // 【新增】获取 menuId
const formDefinition = ref(null);
const filterConfig = ref([]);

// 【核心修改】更新 API 调用函数，传递 menuId
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
} = usePaginatedFetch(
    apiFunction,
    {},
    { defaultSort: 'createdAt,desc' }
);

// 动态生成表格列
const columns = computed(() => {
  if (!formDefinition.value) return [];
  const allFields = flattenFields(formDefinition.value.schema.fields);

  const baseColumns = [
    { title: '提交人', dataIndex: 'submitterName', key: 'submitterName', width: 120 },
    { title: '流程状态', dataIndex: 'workflowStatus', key: 'workflowStatus', width: 120, align: 'center' },
  ];

  // 动态列现在从 dataJson 中提取
  const dynamicColumns = allFields
      .filter(f => !['GridRow', 'GridCol', 'Collapse', 'StaticText', 'RichText', 'Subform'].includes(f.type)) // 过滤掉不适合在表格中展示的组件
      .slice(0, 4) // 最多显示4个动态列
      .map(field => ({
        title: field.label,
        dataIndex: field.id, // dataIndex 直接使用字段 ID
        key: field.id,
        ellipsis: true,
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

    // 动态设置筛选条件
    const allFields = flattenFields(res.schema.fields);
    filterConfig.value = allFields.filter(f => ['Input', 'Select', 'DatePicker'].includes(f.type)).slice(0, 2);
    filterConfig.value.forEach(f => {
      if (!(f.id in filterState)) {
        filterState[f.id] = undefined;
      }
    });

    await fetchData();

  } catch (error) {
    message.error("初始化页面失败");
  }
};

// 监听路由元信息变化，支持在同一组件实例中切换不同菜单
watch(() => route.meta, (newMeta) => {
  if (newMeta && newMeta.menuId && newMeta.menuId !== menuId.value) {
    pageTitle.value = newMeta.title;
    formId.value = newMeta.formId;
    menuId.value = newMeta.menuId;
    handleReset(); // 重置筛选条件
    initialize();   // 重新初始化页面
  }
}, { immediate: true });

onMounted(() => {
  // watch 的 immediate: true 已经会调用一次 initialize
});

const goToDetail = (submissionId) => {
  router.push({ name: 'submission-detail', params: { submissionId } });
};

// Helper Functions
const getComponentByType = (type) => ({ 'Input': 'a-input', 'DatePicker': 'a-date-picker' }[type] || 'a-input');
const getStatusColor = (status) => ({ '审批中': 'processing', '已通过': 'success', '已拒绝': 'error' }[status] || 'default');
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
</style>