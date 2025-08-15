<template>
  <div class="page-container">
    <a-page-header title="流程实例管理" sub-title="监控并管理所有正在运行的流程实例" />

    <div style="padding: 0 24px;">
      <a-table
          :columns="columns"
          :data-source="instances"
          :loading="loading"
          row-key="processInstanceId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'businessKey'">
            <router-link :to="{ name: 'submission-detail', params: { submissionId: record.businessKey } }">
              {{ record.businessKey }}
            </router-link>
          </template>
          <template v-else-if="column.key === 'startTime'">
            {{ new Date(record.startTime).toLocaleString() }}
          </template>
          <template v-else-if="column.key === 'processDefinitionName'">
            {{ record.processDefinitionName }} (v{{ record.version }})
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-popconfirm
                title="确定要终止这个流程实例吗？"
                content="此操作不可逆，将立即停止该流程的执行。"
                ok-text="确认终止"
                cancel-text="取消"
                @confirm="handleTerminate(record)"
            >
              <a-button type="primary" danger size="small">终止流程</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue';
import { getActiveInstances, terminateInstance } from '@/api';
import { message, Modal, Input } from 'ant-design-vue';

const loading = ref(true);
const instances = ref([]);

const columns = [
  { title: '实例ID', dataIndex: 'processInstanceId', key: 'processInstanceId', ellipsis: true },
  { title: '业务ID (申请)', dataIndex: 'businessKey', key: 'businessKey', align: 'center' },
  { title: '流程定义', dataIndex: 'processDefinitionName', key: 'processDefinitionName' },
  { title: '发起人', dataIndex: 'startUserName', key: 'startUserName' },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime' },
  { title: '当前节点', dataIndex: 'currentActivityName', key: 'currentActivityName' },
  { title: '操作', key: 'actions', align: 'center' },
];

const fetchInstances = async () => {
  loading.value = true;
  try {
    instances.value = await getActiveInstances();
  } catch (error) {
    // 错误已在 api/index.js 中全局处理
  } finally {
    loading.value = false;
  }
};

onMounted(fetchInstances);

const handleTerminate = (record) => {
  let reason = '';
  Modal.confirm({
    title: '请输入终止原因',
    content: h(Input, {
      placeholder: '请输入终止原因 (必填)',
      onChange: (e) => {
        reason = e.target.value;
      },
    }),
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      if (!reason.trim()) {
        message.error('终止原因不能为空！');
        return Promise.reject(new Error('终止原因不能为空')); // 阻止弹窗关闭
      }
      try {
        await terminateInstance(record.processInstanceId, reason);
        message.success(`流程实例 ${record.processInstanceId} 已成功终止。`);
        await fetchInstances(); // 刷新列表
      } catch (error) {
        // 错误已全局处理
      }
    },
  });
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>