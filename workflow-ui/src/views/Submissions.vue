<template>
  <div class="page-container">
    <a-page-header :title="`“${formName}” 的提交记录`" @back="() => $router.push('/')" />
    <a-table
        :columns="columns"
        :data-source="submissions"
        :loading="loading"
        row-key="id"
        style="margin: 24px;"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'workflowStatus'">
          <a-tag :color="getStatusColor(record.workflowStatus)">{{ record.workflowStatus }}</a-tag>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-button type="link" @click="goToDetail(record.id)">查看详情</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { getSubmissions, getFormById } from '@/api';
import { message } from 'ant-design-vue';

const props = defineProps({ formId: String });
const router = useRouter();
const loading = ref(true);
const submissions = ref([]);
const formSchema = ref({ fields: [] });
const formName = ref('');

const columns = computed(() => {
  const baseColumns = [
    { title: '提交人', dataIndex: 'submitterName', key: 'submitterName', width: 120 },
    { title: '流程状态', dataIndex: 'workflowStatus', key: 'workflowStatus', width: 120, align: 'center' },
  ];
  // 只显示前 3 个字段作为预览
  const dynamicColumns = formSchema.value.fields.slice(0, 3).map(field => ({
    title: field.label,
    dataIndex: ['data', field.id],
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

onMounted(async () => {
  loading.value = true;
  try {
    const [formRes, subRes] = await Promise.all([
      getFormById(props.formId),
      getSubmissions(props.formId),
    ]);
    formName.value = formRes.name;
    formSchema.value = JSON.parse(formRes.schemaJson);
    submissions.value = subRes.map(s => ({
      ...s,
      data: JSON.parse(s.dataJson),
      createdAt: new Date(s.createdAt).toLocaleString(),
    }));
  } catch (error) {
    message.error('加载数据失败');
  } finally {
    loading.value = false;
  }
});
</script>