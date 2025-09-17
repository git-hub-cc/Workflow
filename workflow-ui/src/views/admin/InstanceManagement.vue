<template>
  <div class="page-container">
    <a-page-header title="流程实例管理" sub-title="监控、诊断并干预所有流程实例" />

    <div style="padding: 24px;">
      <!-- 筛选区域 -->
      <a-card :bordered="false" style="margin-bottom: 24px;">
        <a-form :model="filterState" layout="inline">
          <a-form-item label="业务ID">
            <a-input v-model:value="filterState.businessKey" placeholder="输入表单提交ID" allow-clear />
          </a-form-item>
          <a-form-item label="发起人">
            <a-input v-model:value="filterState.startUser" placeholder="输入发起人ID" allow-clear />
          </a-form-item>
          <a-form-item label="实例状态">
            <a-select v-model:value="filterState.state" placeholder="选择状态" style="width: 150px" allow-clear>
              <a-select-option value="RUNNING">运行中</a-select-option>
              <a-select-option value="COMPLETED">已完成</a-select-option>
              <a-select-option value="TERMINATED">已终止</a-select-option>
            </a-select>
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

      <!-- 批量操作与表格 -->
      <a-card :bordered="false">
        <div style="margin-bottom: 16px;">
          <a-space>
            <a-button :disabled="!hasSelected" @click="handleBatchSuspend">批量挂起</a-button>
            <a-button :disabled="!hasSelected" @click="handleBatchActivate">批量激活</a-button>
            <a-button :disabled="!hasSelected" danger @click="handleBatchTerminate">批量终止</a-button>
          </a-space>
          <span v-if="hasSelected" style="margin-left: 8px;">
            已选择 {{ selectedRowKeys.length }} 项
          </span>
        </div>

        <a-table
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :pagination="pagination"
            :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
            row-key="processInstanceId"
            @change="handleTableChange"
            :scroll="{ x: 'max-content' }"
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
            <template v-else-if="column.key === 'state'">
              <a-tooltip v-if="record.hasIncident" title="流程存在技术异常，请检查日志或联系开发人员">
                <a-tag color="error">
                  <template #icon><ExclamationCircleOutlined /></template>
                  异常
                </a-tag>
              </a-tooltip>
              <a-tag v-else :color="getStateColor(record.state, record.suspended)">
                {{ getStateText(record.state, record.suspended) }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'duration'">
              {{ record.durationInMillis ? formatDuration(record.durationInMillis) : '-' }}
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-dropdown>
                <a class="ant-dropdown-link" @click.prevent>
                  操作 <DownOutlined />
                </a>
                <template #overlay>
                  <a-menu @click="({ key }) => handleMenuClick(key, record)">
                    <a-menu-item key="variables"><PartitionOutlined /> 管理变量</a-menu-item>
                    <a-menu-item key="diagram"><ApartmentOutlined /> 查看图表</a-menu-item>
                    <a-menu-item key="reassign" v-if="record.state === 'RUNNING'"><UserSwitchOutlined /> 转办</a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="suspend" v-if="!record.suspended && record.state === 'RUNNING'"><PauseCircleOutlined /> 挂起</a-menu-item>
                    <a-menu-item key="activate" v-if="record.suspended && record.state === 'RUNNING'"><PlayCircleOutlined /> 激活</a-menu-item>
                    <a-menu-item key="terminate" danger v-if="record.state === 'RUNNING'"><StopOutlined /> 终止</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <ProcessVariablesModal
        v-if="variablesModalVisible"
        v-model:open="variablesModalVisible"
        :instance-id="currentInstanceId"
    />
    <ProcessDiagramModal
        v-if="diagramModalVisible"
        v-model:open="diagramModalVisible"
        :bpmn-xml="bpmnXml"
        :history-activities="[]"
    />
    <a-modal
        v-model:open="reassignModalVisible"
        title="转办任务"
        :confirm-loading="reassignLoading"
        @ok="handleReassignOk"
    >
      <a-form-item label="选择新办理人">
        <a-select
            v-model:value="newAssigneeId"
            show-search
            placeholder="输入姓名或ID搜索用户"
            :options="userPickerOptions"
            :filter-option="false"
            :not-found-content="userSearchLoading ? undefined : null"
            @search="handleUserSearch"
            style="width: 100%;"
        >
          <template v-if="userSearchLoading" #notFoundContent>
            <a-spin size="small" />
          </template>
        </a-select>
      </a-form-item>
    </a-modal>

  </div>
</template>

<script setup>
import { ref, onMounted, h, computed, defineAsyncComponent } from 'vue';
import {
  getProcessInstances, terminateInstance, suspendInstance, activateInstance,
  batchSuspendInstances, batchActivateInstances, batchTerminateInstances,
  getWorkflowDiagramByInstanceId, reassignTask, getPendingTasks, searchUsersForPicker
} from '@/api';
import { usePaginatedFetch } from '@/composables/usePaginatedFetch';
import { message, Modal, Input } from 'ant-design-vue';
import {
  DownOutlined, SearchOutlined, ReloadOutlined, ExclamationCircleOutlined,
  PauseCircleOutlined, PlayCircleOutlined, StopOutlined, PartitionOutlined, ApartmentOutlined, UserSwitchOutlined
} from '@ant-design/icons-vue';
import ProcessVariablesModal from './components/ProcessVariablesModal.vue';
const ProcessDiagramModal = defineAsyncComponent(() => import('@/components/ProcessDiagramModal.vue'));

const {
  loading, dataSource, pagination, filterState,
  handleTableChange, handleSearch, handleReset, fetchData
} = usePaginatedFetch(getProcessInstances, { state: 'RUNNING', businessKey: '', startUser: '' });

const variablesModalVisible = ref(false);
const diagramModalVisible = ref(false);
const reassignModalVisible = ref(false);
const currentInstanceId = ref(null);
const bpmnXml = ref(null);

const selectedRowKeys = ref([]);
const hasSelected = computed(() => selectedRowKeys.value.length > 0);
const onSelectChange = keys => {
  selectedRowKeys.value = keys;
};

const columns = [
  { title: '实例ID', dataIndex: 'processInstanceId', key: 'processInstanceId', ellipsis: true, width: 150 },
  { title: '业务ID (申请)', dataIndex: 'businessKey', key: 'businessKey', align: 'center', width: 120 },
  { title: '流程定义', dataIndex: 'processDefinitionName', key: 'processDefinitionName' },
  { title: '状态', dataIndex: 'state', key: 'state', align: 'center', width: 100 },
  { title: '发起人', dataIndex: 'startUserName', key: 'startUserName', width: 120 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 180 },
  { title: '当前节点', dataIndex: 'currentActivityName', key: 'currentActivityName', ellipsis: true },
  { title: '持续时间', key: 'duration', align: 'center', width: 150 },
  { title: '操作', key: 'actions', align: 'center', width: 100, fixed: 'right' },
];

onMounted(fetchData);

const handleMenuClick = async (key, record) => {
  currentInstanceId.value = record.processInstanceId;
  switch (key) {
    case 'variables':
      variablesModalVisible.value = true;
      break;
    case 'diagram':
      try {
        const res = await getWorkflowDiagramByInstanceId(record.processInstanceId);
        bpmnXml.value = res.bpmnXml;
        diagramModalVisible.value = true;
      } catch (err) {}
      break;
    case 'reassign':
      await showReassignModal(record.processInstanceId);
      break;
    case 'suspend':
      await handleSingleAction(suspendInstance, record.processInstanceId, '挂起');
      break;
    case 'activate':
      await handleSingleAction(activateInstance, record.processInstanceId, '激活');
      break;
    case 'terminate':
      handleTerminate(record);
      break;
  }
};

const reassignLoading = ref(false);
const newAssigneeId = ref(null);
const currentTaskId = ref(null);
const userPickerOptions = ref([]);
const userSearchLoading = ref(false);
let searchTimeout;

const showReassignModal = async (instanceId) => {
  try {
    const tasks = await getPendingTasks({ processInstanceId: instanceId });
    if (!tasks.content || tasks.content.length === 0) {
      message.warn("该流程实例当前没有待处理的任务可转办。");
      return;
    }
    currentTaskId.value = tasks.content[0].camundaTaskId;
    newAssigneeId.value = null;
    userPickerOptions.value = [];
    reassignModalVisible.value = true;
  } catch (error) {
    message.error("获取待办任务失败，无法转办。");
  }
};

const handleUserSearch = (keyword) => {
  clearTimeout(searchTimeout);
  if (!keyword) {
    userPickerOptions.value = [];
    return;
  }
  userSearchLoading.value = true;
  searchTimeout = setTimeout(async () => {
    try {
      const results = await searchUsersForPicker(keyword);
      userPickerOptions.value = results.map(u => ({ label: `${u.name} (${u.id})`, value: u.id }));
    } finally {
      userSearchLoading.value = false;
    }
  }, 300);
};

const handleReassignOk = async () => {
  if (!newAssigneeId.value) {
    message.warn('请选择一个新的办理人。');
    return;
  }
  reassignLoading.value = true;
  try {
    await reassignTask(currentTaskId.value, newAssigneeId.value);
    message.success('任务转办成功！');
    reassignModalVisible.value = false;
    await fetchData();
  } finally {
    reassignLoading.value = false;
  }
};


const handleSingleAction = async (apiFunc, instanceId, actionName) => {
  try {
    await apiFunc(instanceId);
    message.success(`流程实例已${actionName}`);
    await fetchData();
  } catch (err) {}
};

const handleTerminate = (record) => {
  let reason = '';
  Modal.confirm({
    title: '请输入终止原因',
    content: h(Input, {
      placeholder: '请输入终止原因 (必填)',
      onChange: (e) => { reason = e.target.value; },
    }),
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      if (!reason.trim()) {
        message.error('终止原因不能为空！');
        return Promise.reject(new Error('终止原因不能为空'));
      }
      await handleSingleAction(id => terminateInstance(id, reason), record.processInstanceId, '终止');
    },
  });
};

const handleBatchSuspend = async () => {
  await batchSuspendInstances(selectedRowKeys.value);
  message.success('批量挂起成功');
  selectedRowKeys.value = [];
  await fetchData();
};
const handleBatchActivate = async () => {
  await batchActivateInstances(selectedRowKeys.value);
  message.success('批量激活成功');
  selectedRowKeys.value = [];
  await fetchData();
};
const handleBatchTerminate = () => {
  let reason = '';
  Modal.confirm({
    title: `确认终止 ${selectedRowKeys.value.length} 个实例?`,
    content: h(Input, {
      placeholder: '请输入终止原因 (必填)',
      onChange: (e) => { reason = e.target.value; },
    }),
    onOk: async () => {
      if (!reason.trim()) {
        message.error('终止原因不能为空！');
        return Promise.reject(new Error('终止原因不能为空'));
      }
      await batchTerminateInstances(selectedRowKeys.value, reason);
      message.success('批量终止成功');
      selectedRowKeys.value = [];
      await fetchData();
    },
  });
};

const getStateColor = (state, suspended) => {
  if (suspended) return 'orange';
  switch (state) {
    case 'RUNNING': return 'processing';
    case 'COMPLETED': return 'success';
    case 'TERMINATED': return 'error';
    default: return 'default';
  }
};
const getStateText = (state, suspended) => {
  if (suspended) return '已挂起';
  switch (state) {
    case 'RUNNING': return '运行中';
    case 'COMPLETED': return '已完成';
    case 'TERMINATED': return '已终止';
    default: return state;
  }
};
const formatDuration = (ms) => {
  if (!ms) return '-';
  let seconds = Math.floor(ms / 1000);
  let minutes = Math.floor(seconds / 60);
  let hours = Math.floor(minutes / 60);
  let days = Math.floor(hours / 24);
  seconds %= 60;
  minutes %= 60;
  hours %= 24;
  let result = '';
  if (days > 0) result += `${days}天`;
  if (hours > 0) result += `${hours}时`;
  if (minutes > 0) result += `${minutes}分`;
  if (seconds > 0) result += `${seconds}秒`;
  return result || '0秒';
};

</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
@media (max-width: 768px) {
  :deep(.ant-form-inline .ant-form-item) {
    margin-bottom: 16px;
  }
}
</style>