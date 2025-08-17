<template>
  <div class="page-container">
    <a-page-header :title="`“${formName}” 的提交记录`" @back="() => $router.go(-1)" />
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

const props = defineProps({ formId: String });
const router = useRouter();
const loading = ref(true);
const submissions = ref([]);
const formSchema = ref({ fields: [] });
const formName = ref('');

// 【新增】分页状态
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
    dataIndex: field.id, // 使用字段ID
    key: field.id,
    ellipsis: true,
  }));
  const finalColumns = [
    { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
    { title: '操作', key: 'actions', width: 120, align: 'center' },
  ];
  return [...baseColumns, ...dynamicColumns, ...finalColumns];
});

const getStatusColor = (status) => {
  if (status === '审批中') return 'processing';
  if (status === '已通过') return 'success';
  if (status === '已拒绝') return 'error';
  return 'default';
};

const goToDetail = (submissionId) => {
  router.push({ name: 'submission-detail', params: { submissionId } });
};

// 【修改】获取数据函数，以支持分页
const fetchData = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: 'createdAt,desc'
    };
    const subRes = await getSubmissions(props.formId, params);
    submissions.value = subRes.content.map(s => ({
      ...s,
      ...JSON.parse(s.dataJson) // 展开 dataJson
    }));
    pagination.total = subRes.totalElements;
  } catch (error) {
    message.error('加载数据失败');
  } finally {
    loading.value = false;
  }
}

// 【新增】处理表格变化事件（分页、排序等）
const handleTableChange = (pager) => {
  pagination.current = pager.current;
  pagination.pageSize = pager.pageSize;
  fetchData();
}

const getRecordValue = (record, dataIndex) => {
  return record[dataIndex];
}

onMounted(async () => {
  loading.value = true;
  try {
    const formRes = await getFormById(props.formId);
    formName.value = formRes.name;
    formSchema.value = JSON.parse(formRes.schemaJson);
    await fetchData(); // 初始加载
  } catch (error) {
    message.error('加载表单信息失败');
  } finally {
    loading.value = false;
  }
});
</script>