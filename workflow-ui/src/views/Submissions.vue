<template>
  <div class="page-container">
    <a-page-header :title="`“${formName}” 的提交记录`" @back="() => $router.push('/')" />
    <a-table
        :columns="columns"
        :data-source="submissions"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        style="margin: 24px;"
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
          <!-- 动态渲染表单数据列 -->
          {{ getRecordValue(record, column.dataIndex) }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { getSubmissions, getFormById } from '@/api';
import { message } from 'ant-design-vue';

// ⭐ 核心修改: 允许 formId 接收字符串或数字类型
const props = defineProps({ formId: [String, Number] });
const router = useRouter();
const loading = ref(true);
const submissions = ref([]);
const formSchema = ref({ fields: [] });
const formName = ref('');

// 分页状态
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});


const columns = computed(() => {
  const baseColumns = [
    { title: '提交人', dataIndex: 'submitterName', key: 'submitterName', width: 120 },
    { title: '流程状态', dataIndex: 'workflowStatus', key: 'workflowStatus', width: 120, align: 'center' },
  ];
  // 只显示前 3 个字段作为预览
  const dynamicColumns = formSchema.value.fields.slice(0, 3).map(field => ({
    title: field.label,
    dataIndex: field.id, // 直接使用字段ID，因为数据已被展平
    key: field.id,
    ellipsis: true,
  }));
  const finalColumns = [
    { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
    { title: '操作', key: 'actions', width: 120, align: 'center' },
  ];
  return [...baseColumns, ...dynamicColumns, ...finalColumns];
});

// 根据流程状态返回不同的Tag颜色
const getStatusColor = (status) => {
  if (status === '审批中') return 'processing';
  if (status === '已通过') return 'success';
  if (status === '已拒绝') return 'error';
  return 'default';
};

// 点击“查看详情”按钮，跳转到详情页
const goToDetail = (submissionId) => {
  router.push({ name: 'submission-detail', params: { submissionId } });
};

// 获取数据函数，支持分页
const fetchData = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: 'createdAt,desc'
    };
    const subRes = await getSubmissions(props.formId, params);
    // 将 dataJson 里的数据展平到顶层，方便表格直接通过 dataIndex 访问
    submissions.value = subRes.content.map(s => ({
      ...s,
      ...JSON.parse(s.dataJson)
    }));
    pagination.total = subRes.totalElements;
  } catch (error) {
    message.error('加载数据失败');
  } finally {
    loading.value = false;
  }
}

// 处理表格变化事件（分页、排序等）
const handleTableChange = (pager) => {
  pagination.current = pager.current;
  pagination.pageSize = pager.pageSize;
  fetchData();
}

// 获取记录中特定字段的值
const getRecordValue = (record, dataIndex) => {
  return record[dataIndex];
}

// 组件挂载时，初始化数据
onMounted(async () => {
  loading.value = true;
  try {
    // 先获取表单定义，用于生成列
    const formRes = await getFormById(props.formId);
    formName.value = formRes.name;
    formSchema.value = JSON.parse(formRes.schemaJson);
    // 再获取第一页的提交数据
    await fetchData();
  } catch (error) {
    message.error('加载表单信息失败');
  } finally {
    loading.value = false;
  }
});
</script>