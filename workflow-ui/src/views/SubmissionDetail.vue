<template>
  <div class="page-container">
    <a-page-header
        :title="formDefinition.name || '申请详情'"
        :sub-title="`提交人: ${submission.submitterName}`"
        @back="() => $router.go(-1)"
    />

    <a-spin :spinning="loading" tip="正在加载详情...">
      <div v-if="!loading" style="padding: 24px; display: flex; gap: 24px;">
        <!-- 左侧: 表单内容详情 -->
        <a-card title="表单内容" style="flex: 2;">
          <a-form :model="formData" layout="vertical">
            <!-- 渲染器会以只读模式展示表单 -->
            <FormItemRenderer
                v-for="field in formDefinition.schema.fields"
                :key="field.id"
                :field="field"
                :form-data="formData"
                :mode="'readonly'"
            />
          </a-form>
        </a-card>

        <!-- 右侧: 流程历史时间线 -->
        <a-card title="流程历史" style="flex: 1;">
          <a-timeline>
            <a-timeline-item v-for="item in history" :key="item.activityId" :color="getTimelineColor(item)">
              <h4>{{ item.activityName }}</h4>
              <p v-if="item.assigneeName">处理人: {{ item.assigneeName }}</p>
              <p>开始: {{ item.startTime ? new Date(item.startTime).toLocaleString() : 'N/A' }}</p>
              <p v-if="item.endTime">结束: {{ new Date(item.endTime).toLocaleString() }}</p>
              <p v-if="item.durationInMillis">耗时: {{ formatDuration(item.durationInMillis) }}</p>
              <p v-if="item.comment">意见: {{ item.comment }}</p>
            </a-timeline-item>
          </a-timeline>
        </a-card>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { getSubmissionById, getFormById, getWorkflowHistory } from '@/api';
import FormItemRenderer from './viewer-components/FormItemRenderer.vue';
import { message } from 'ant-design-vue';

const props = defineProps({ submissionId: String });

const loading = ref(true);
const submission = ref({});
const formDefinition = ref({ schema: { fields: [] } });
const formData = reactive({});
const history = ref([]);

// 组件挂载时，并行获取所有需要的数据
onMounted(async () => {
  try {
    // 1. 获取提交记录本身
    const subRes = await getSubmissionById(props.submissionId);
    submission.value = subRes;
    Object.assign(formData, JSON.parse(subRes.dataJson));

    // 特别处理文件上传组件的数据
    if (subRes.attachments) {
      // 假设表单中只有一个文件上传字段，实际应用中可能需要更复杂的逻辑来匹配
      // 这里的 key 需要是你在表单设计器中为文件上传组件设置的ID
      const fileUploadField = findFileUploadField(JSON.parse((await getFormById(subRes.formDefinitionId)).schemaJson));
      if (fileUploadField) {
        formData[fileUploadField.id] = subRes.attachments;
      }
    }

    // 2. 并行获取表单定义和流程历史
    const [formRes, historyRes] = await Promise.all([
      getFormById(subRes.formDefinitionId),
      getWorkflowHistory(props.submissionId),
    ]);

    formRes.schema = JSON.parse(formRes.schemaJson);
    formDefinition.value = formRes;
    history.value = historyRes;

  } catch (error) {
    message.error('加载详情失败');
  } finally {
    loading.value = false;
  }
});

// 递归查找文件上传字段的辅助函数
const findFileUploadField = (schema) => {
  for (const field of schema.fields) {
    if (field.type === 'FileUpload') return field;
    if (field.columns) {
      for (const col of field.columns) {
        const found = findFileUploadField({ fields: col.fields });
        if (found) return found;
      }
    }
    if (field.panels) {
      for (const panel of field.panels) {
        const found = findFileUploadField({ fields: panel.fields });
        if (found) return found;
      }
    }
  }
  return null;
}

// 根据历史节点状态返回时间线颜色
const getTimelineColor = (item) => {
  if (item.activityType.endsWith('EndEvent')) {
    return item.decision === 'REJECTED' ? 'red' : 'green';
  }
  if (item.endTime) return 'blue'; // 已完成的节点
  return 'gray'; // 未完成或进行中的节点
};

// 将毫秒时长格式化为 "Xh Ym Zs"
const formatDuration = (ms) => {
  if (!ms) return 'N/A';
  let seconds = Math.floor(ms / 1000);
  let minutes = Math.floor(seconds / 60);
  let hours = Math.floor(minutes / 60);
  seconds %= 60;
  minutes %= 60;
  return `${hours}h ${minutes}m ${seconds}s`;
};
</script>