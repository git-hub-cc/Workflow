<template>
  <div class="page-container">
    <a-page-header title="表单管理" sub-title="管理所有已创建的业务表单">
      <template #extra>
        <a-button type="primary" @click="$router.push({ name: 'form-builder-create' })">
          <template #icon><PlusOutlined /></template>
          新建表单
        </a-button>
      </template>
    </a-page-header>

    <div style="padding: 24px;">
      <a-table
          :columns="columns"
          :data-source="forms"
          :loading="loading"
          row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createdAt' || column.key === 'updatedAt'">
            {{ new Date(record[column.dataIndex]).toLocaleString() }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="goToBuilder(record.id)">
                编辑
              </a-button>
              <a-button type="primary" size="small" @click="goToDesigner(record.id)">
                设计流程
              </a-button>
              <a-button type="link" size="small" @click="viewSubmissions(record.id)">
                查看数据
              </a-button>
              <!-- 【核心修改】移除 a-popconfirm，改为直接调用 showDeleteModal 方法 -->
              <a-button type="link" danger size="small" @click="showDeleteModal(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 【核心新增】删除确认与依赖展示模态框 -->
    <a-modal
        v-model:open="deleteModalInfo.visible"
        :title="`删除确认：${deleteModalInfo.formName}`"
        :confirm-loading="deleteModalInfo.loading"
        @cancel="closeDeleteModal"
    >
      <template #footer>
        <a-button key="back" @click="closeDeleteModal">取消</a-button>
        <a-button
            key="submit"
            type="primary"
            danger
            :loading="deleteModalInfo.loading"
            :disabled="!cascadeDeleteConfirmed && deleteModalInfo.canCascade"
            @click="handleCascadeDelete"
        >
          确认删除
        </a-button>
      </template>

      <a-alert
          message="无法删除此表单"
          :description="`表单 “${deleteModalInfo.formName}” 仍被以下项目使用，请先解除关联：`"
          type="error"
          show-icon
      />
      <a-list :data-source="deleteModalInfo.dependencies" size="small" style="margin-top: 16px;">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #avatar>
                <a-avatar style="background-color: #f56a00"><component :is="getDependencyIcon(item.type)" /></a-avatar>
              </template>
              <template #title>
                <a @click="navigateToDependency(item)" :title="`跳转到 ${item.name}`">{{ item.name }}</a>
              </template>
              <template #description>
                {{ getDependencyDescription(item) }}
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
      <div v-if="deleteModalInfo.canCascade" style="margin-top: 24px; border-top: 1px solid #f0f0f0; padding-top: 16px;">
        <a-checkbox v-model:checked="cascadeDeleteConfirmed">
          <span style="color: #cf1322; font-weight: bold;">我已了解风险，并确认要一并删除所有关联的 <strong>工作流模板</strong> 和 <strong>全部提交数据</strong>。此操作不可恢复！</span>
        </a-checkbox>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import {ref, onMounted, reactive, h} from 'vue';
import { useRouter } from 'vue-router';
// --- 【核心修改】引入新的 API 函数和图标 ---
import { getForms, deleteForm, getFormDependencies } from '@/api';
import { message, Modal } from 'ant-design-vue';
import {
  PlusOutlined,
  ExclamationCircleOutlined,
  ForkOutlined,
  MenuOutlined,
  DatabaseOutlined
} from '@ant-design/icons-vue';

const router = useRouter();
const loading = ref(true);
const forms = ref([]);

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '表单名称', dataIndex: 'name', key: 'name' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 200 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 200 },
  { title: '操作', key: 'actions', align: 'center', width: 320 },
];

const fetchForms = async () => {
  loading.value = true;
  try {
    forms.value = await getForms();
  } catch (error) {
    // 错误已全局处理
  } finally {
    loading.value = false;
  }
};

onMounted(fetchForms);

const goToBuilder = (formId) => router.push({ name: 'form-builder-edit', params: { formId } });
const goToDesigner = (formId) => router.push({ name: 'workflow-designer', params: { formId } });
const viewSubmissions = (formId) => router.push({ name: 'form-submissions', params: { formId } });


// --- 【核心新增】删除模态框相关逻辑 ---
const deleteModalInfo = reactive({
  visible: false,
  loading: false,
  formId: null,
  formName: '',
  dependencies: [],
  canCascade: false, // 是否可以提供级联删除选项
});
const cascadeDeleteConfirmed = ref(false);

const showDeleteModal = async (record) => {
  deleteModalInfo.loading = true;
  try {
    const res = await getFormDependencies(record.id);
    if (res.canDelete) {
      // 如果可以安全删除，则显示传统确认框
      confirmSimpleDelete(record);
    } else {
      // 否则，显示包含依赖信息的复杂模态框
      deleteModalInfo.formId = record.id;
      deleteModalInfo.formName = record.name;
      deleteModalInfo.dependencies = res.dependencies;
      // 只有当依赖项不包含菜单时，才允许级联删除
      deleteModalInfo.canCascade = !res.dependencies.some(d => d.type === 'MENU');
      deleteModalInfo.visible = true;
    }
  } catch(e) {
    // API 错误已全局处理
  } finally {
    deleteModalInfo.loading = false;
  }
};

const confirmSimpleDelete = (record) => {
  Modal.confirm({
    title: `确定要删除表单 “${record.name}” 吗？`,
    icon: h(ExclamationCircleOutlined),
    content: '此操作不可恢复。',
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteForm(record.id, false);
        message.success('表单删除成功！');
        await fetchForms();
      } catch (error) {}
    },
  });
};

const handleCascadeDelete = async () => {
  deleteModalInfo.loading = true;
  try {
    await deleteForm(deleteModalInfo.formId, true);
    message.success(`表单 “${deleteModalInfo.formName}” 及其关联数据已成功删除！`);
    closeDeleteModal();
    await fetchForms();
  } catch (error) {
    // API 错误已全局处理
  } finally {
    deleteModalInfo.loading = false;
  }
};

const closeDeleteModal = () => {
  deleteModalInfo.visible = false;
  deleteModalInfo.dependencies = [];
  deleteModalInfo.formId = null;
  cascadeDeleteConfirmed.value = false;
};

// --- 依赖项显示辅助函数 ---
const getDependencyIcon = (type) => {
  const map = { WORKFLOW: ForkOutlined, MENU: MenuOutlined, SUBMISSION: DatabaseOutlined };
  return map[type];
};

const getDependencyDescription = (item) => {
  const map = {
    WORKFLOW: `ID: ${item.id}`,
    MENU: `ID: ${item.id}`,
    SUBMISSION: `共 ${item.count} 条记录`
  };
  return map[item.type];
};

const navigateToDependency = (item) => {
  if (item.type === 'WORKFLOW') {
    goToDesigner(deleteModalInfo.formId);
  } else if (item.type === 'MENU') {
    router.push({ name: 'admin-menus' });
  }
  closeDeleteModal();
};

</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>