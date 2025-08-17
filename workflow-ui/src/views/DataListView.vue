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
            <!-- 可以在此根据菜单配置添加更多操作，如编辑、删除等 -->
          </template>
          <template v-else>
            <!-- 动态渲染表单数据列 -->
            {{ getRecordValue(record, column.dataIndex) }}
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getFormById, getSubmissions } from '@/api';
import { message } from 'ant-design-vue';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue';
import { flattenFields } from '@/utils/formUtils.js';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const pageTitle = ref(route.meta.title || '数据列表');
const formId = ref(route.meta.formId);

const formDefinition = ref(null);
const dataSource = ref([]);
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`,
});
const filterState = reactive({});

// --- 动态配置 ---
// 在真实的应用中，这些配置可以从后端 menu 的定义中获取
const filterConfig = ref([]);
const columnConfig = ref([]);

// 动态生成表格列
const columns = computed(() => {
  const baseColumns = [
    { title: '提交人', dataIndex: 'submitterName', key: 'submitterName', width: 120 },
    { title: '流程状态', dataIndex: 'workflowStatus', key: 'workflowStatus', width: 120, align: 'center' },
  ];

  const dynamicColumns = (columnConfig.value.length > 0 ? columnConfig.value : (formDefinition.value?.schema.fields || []).slice(0, 4))
      .map(field => ({
        title: field.label,
        dataIndex: field.id, // 直接使用字段ID
        key: field.id,
        ellipsis: true,
      }));

  const finalColumns = [
    { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
    { title: '操作', key: 'actions', width: 120, align: 'center' },
  ];
  return [...baseColumns, ...dynamicColumns, ...finalColumns];
});

// 获取表单定义并初始化页面
const initialize = async () => {
  if (!formId.value) {
    message.error("页面配置错误：未关联表单定义！");
    loading.value = false;
    return;
  }
  try {
    const res = await getFormById(formId.value);
    res.schema = JSON.parse(res.schemaJson);
    formDefinition.value = res;

    // 简单实现：默认使用前2个字段作为筛选条件
    const allFields = flattenFields(res.schema.fields);
    filterConfig.value = allFields.filter(f => ['Input', 'Select', 'DatePicker'].includes(f.type)).slice(0, 2);
    filterConfig.value.forEach(f => {
      filterState[f.id] = undefined;
    });

    await fetchData();
  } catch (error) {
    message.error("初始化页面失败");
  } finally {
    loading.value = false;
  }
};


const fetchData = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: 'createdAt,desc',
      ...filterState,
    };
    // 清理空参数
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === undefined || params[key] === '') {
        delete params[key];
      }
    });

    const response = await getSubmissions(formId.value, params);
    dataSource.value = response.content.map(s => ({
      ...s,
      ...JSON.parse(s.dataJson) // 将 dataJson 展开，便于表格直接访问
    }));
    pagination.total = response.totalElements;
  } catch (error) {
    // 错误已全局处理
  } finally {
    loading.value = false;
  }
};

// 监听路由变化，以便在同一个组件实例中切换不同菜单
watch(() => route.meta, (newMeta) => {
  if (newMeta && newMeta.formId && newMeta.formId !== formId.value) {
    pageTitle.value = newMeta.title;
    formId.value = newMeta.formId;
    handleReset(); // 重置状态并重新加载
    initialize();
  }
}, { immediate: true });


onMounted(initialize);

const handleTableChange = (pager) => {
  pagination.current = pager.current;
  pagination.pageSize = pager.pageSize;
  fetchData();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

const handleReset = () => {
  Object.keys(filterState).forEach(key => {
    filterState[key] = undefined;
  });
  handleSearch();
};

const goToDetail = (submissionId) => {
  router.push({ name: 'submission-detail', params: { submissionId } });
};

// --- Helper Functions ---
const getComponentByType = (type) => ({ 'Input': 'a-input', 'DatePicker': 'a-date-picker' }[type] || 'a-input');
const getStatusColor = (status) => ({ '审批中': 'processing', '已通过': 'success', '已拒绝': 'error' }[status] || 'default');
const getRecordValue = (record, dataIndex) => {
  // antd table 的 dataIndex 支持路径 a.b.c，但我们已经展开了
  return record[dataIndex];
}
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
</style>