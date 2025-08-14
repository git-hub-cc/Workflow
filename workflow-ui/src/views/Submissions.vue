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
      <!-- ... (省略自定义渲染, 与 Home.vue 类似) ... -->
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { getSubmissions, getFormById } from '@/api';
import { message } from 'ant-design-vue';

const props = defineProps({ formId: String });
const loading = ref(true);
const submissions = ref([]);
const formSchema = ref({ fields: [] });
const formName = ref('');

const columns = computed(() => {
  const baseColumns = [
    { title: '提交ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: '流程状态', dataIndex: 'workflowStatus', key: 'workflowStatus' },
  ];
  const dynamicColumns = formSchema.value.fields.map(field => ({
    title: field.label,
    dataIndex: ['data', field.id], // antd-vue支持路径索引
    key: field.id,
  }));
  const finalColumns = [
    { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt' },
  ];
  return [...baseColumns, ...dynamicColumns, ...finalColumns];
});

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