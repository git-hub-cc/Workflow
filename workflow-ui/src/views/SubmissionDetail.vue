<template>
  <div class="page-container">
    <a-page-header :title="formDefinition.name || '申请详情'" @back="() => $router.go(-1)" />

    <a-spin :spinning="loading" tip="正在加载详情...">
      <div v-if="!loading" style="padding: 24px;">
        <a-row :gutter="[24, 24]">
          <!-- 左侧：表单数据 -->
          <a-col :span="12">
            <a-card title="表单数据">
              <a-descriptions bordered :column="1">
                <a-descriptions-item
                    v-for="field in formDefinition.schema.fields"
                    :key="field.id"
                    :label="field.label"
                >
                  {{ formatValue(formData[field.id]) }}
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-col>

          <!-- 右侧：审批历史 -->
          <a-col :span="12">
            <a-card title="审批历史">
              <a-timeline v-if="history.length > 0">
                <a-timeline-item
                    v-for="item in history"
                    :key="item.activityId"
                    :color="getTimelineColor(item)"
                >
                  <p><strong>{{ item.activityName }}</strong></p>
                  <p v-if="item.assigneeName">处理人: {{ item.assigneeName }}</p>
                  <p v-if="item.startTime">
                    时间: {{ new Date(item.startTime).toLocaleString() }}
                    <span v-if="item.durationInMillis"> (耗时: {{ formatDuration(item.durationInMillis) }})</span>
                  </p>
                  <p v-if="item.decision">
                    审批决定:
                    <a-tag :color="item.decision === 'APPROVED' ? 'green' : 'red'">
                      {{ item.decision === 'APPROVED' ? '通过' : '拒绝' }}
                    </a-tag>
                  </p>
                  <p v-if="item.comment">审批意见: {{ item.comment }}</p>
                </a-timeline-item>
              </a-timeline>
              <a-empty v-else description="暂无审批历史记录" />
            </a-card>
          </a-col>
        </a-row>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getSubmissionById, getFormById, getWorkflowHistory } from '@/api';
import { message } from 'ant-design-vue';

const props = defineProps({ submissionId: String });
const loading = ref(true);
const formDefinition = ref({ schema: { fields: [] } });
const formData = ref({});
const history = ref([]);

const formatValue = (value) => {
  if (value === null || value === undefined) return '(未填写)';
  if (typeof value === 'boolean') return value ? '是' : '否';
  // 简单判断是否为日期格式字符串
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(value)) {
    return new Date(value).toLocaleString();
  }
  return value;
};

const formatDuration = (ms) => {
  if (!ms) return '';
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days}天 ${hours % 24}小时`;
  if (hours > 0) return `${hours}小时 ${minutes % 60}分钟`;
  if (minutes > 0) return `${minutes}分钟 ${seconds % 60}秒`;
  return `${seconds}秒`;
};

const getTimelineColor = (item) => {
  if (item.activityType.endsWith('EndEvent')) {
    return item.decision === 'APPROVED' ? 'green' : 'red';
  }
  if (!item.endTime) {
    return 'blue'; // 当前活动节点
  }
  return 'gray'; // 已完成
};

onMounted(async () => {
  try {
    // 并行获取数据
    const [subRes, historyRes] = await Promise.all([
      getSubmissionById(props.submissionId),
      getWorkflowHistory(props.submissionId)
    ]);

    formData.value = JSON.parse(subRes.dataJson);
    history.value = historyRes;

    const formRes = await getFormById(subRes.formDefinitionId);
    formDefinition.value = {
      ...formRes,
      schema: JSON.parse(formRes.schemaJson),
    };

  } catch (error) {
    message.error('加载申请详情失败');
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
.ant-timeline-item p {
  margin-bottom: 4px;
}
</style>