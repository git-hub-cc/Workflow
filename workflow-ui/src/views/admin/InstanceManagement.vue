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
          <template v-else-if="column.key === 'suspended'">
            <a-tag :color="record.suspended ? 'orange' : 'green'">
              {{ record.suspended ? '已挂起' : '运行中' }}
            </a-tag>
          </template>
          <!-- 【核心修改】将 Dropdown 菜单改为直接的文字链接按钮 -->
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button
                  v-if="!record.suspended"
                  type="link"
                  size="small"
                  @click="handleSuspend(record.processInstanceId)"
              >
                挂起
              </a-button>
              <a-button
                  v-if="record.suspended"
                  type="link"
                  size="small"
                  @click="handleActivate(record.processInstanceId)"
              >
                激活
              </a-button>
              <a-button
                  type="link"
                  size="small"
                  danger
                  @click="handleTerminate(record)"
              >
                终止
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue';
import { getActiveInstances, terminateInstance, suspendInstance, activateInstance } from '@/api';
import { message, Modal, Input } from 'ant-design-vue';

const loading = ref(true);
const instances = ref([]);

const columns = [
  { title: '实例ID', dataIndex: 'processInstanceId', key: 'processInstanceId', ellipsis: true },
  { title: '业务ID (申请)', dataIndex: 'businessKey', key: 'businessKey', align: 'center' },
  { title: '流程定义', dataIndex: 'processDefinitionName', key: 'processDefinitionName' },
  { title: '状态', dataIndex: 'suspended', key: 'suspended', align: 'center' },
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

const handleSuspend = async (instanceId) => {
  try {
    await suspendInstance(instanceId);
    message.success('流程实例已挂起');
    await fetchInstances();
  } catch (err) { /* Global handler */ }
};

const handleActivate = async (instanceId) => {
  try {
    await activateInstance(instanceId);
    message.success('流程实例已激活');
    await fetchInstances();
  } catch (err) { /* Global handler */ }
};

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