<template>
  <div class="page-container">
    <a-page-header title="表单管理">
      <template #extra>
        <!-- 按钮只对管理员可见 -->
        <a-button v-if="userStore.isAdmin" type="primary" @click="goToBuilder">
          <template #icon><PlusOutlined /></template>
          创建新表单
        </a-button>
      </template>
      <div class="content">
        <p>这是一个基于 Vue 和 Camunda 的表单工作流引擎。从这里开始创建和管理你的表单与流程。</p>
      </div>
    </a-page-header>

    <div style="padding: 0 24px;">
      <a-table
          :columns="columns"
          :data-source="formList"
          :loading="loading"
          row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <strong>{{ record.name }}</strong>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ new Date(record.createdAt).toLocaleString() }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" @click="goToViewer(record.id)">发起申请</a-button>
              <!-- 按钮只对管理员可见 -->
              <a-button v-if="userStore.isAdmin" type="link" @click="goToDesigner(record.id)">设计流程</a-button>
              <a-button type="link" @click="goToSubmissions(record.id)">数据记录</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getForms } from '@/api';
import { useUserStore } from '@/stores/user';
import { message } from 'ant-design-vue';
import { PlusOutlined } from '@ant-design/icons-vue';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(true);
const formList = ref([]);

const columns = [
  { title: '表单名称', dataIndex: 'name', key: 'name' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 200 },
  { title: '操作', key: 'actions', width: 320, align: 'center' },
];

const fetchForms = async () => {
  loading.value = true;
  try {
    formList.value = await getForms();
  } catch (error) {
    // 错误已全局处理
  } finally {
    loading.value = false;
  }
};

onMounted(fetchForms);

const goToBuilder = () => router.push({ name: 'form-builder' });
const goToDesigner = (formId) => router.push({ name: 'workflow-designer', params: { formId } });
const goToViewer = (formId) => router.push({ name: 'form-viewer', params: { formId } });
const goToSubmissions = (formId) => router.push({ name: 'form-submissions', params: { formId } });
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>