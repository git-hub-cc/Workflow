<template>
  <div class="page-container">
    <a-page-header title="我的待办任务" />
    <div style="padding: 0 24px;">
      <!-- 条件渲染：加载完成且列表为空时，显示 a-empty -->
      <div v-if="!loading && tasks.length === 0" style="text-align: center; margin: 24px 0;">
        <a-empty description="恭喜！您当前没有待办任务。" />
      </div>
      <!-- 条件渲染：列表有数据或正在加载时，显示 a-list -->
      <a-list
          v-else
          :data-source="tasks"
          :loading="loading"
          item-layout="horizontal"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <a @click="goToTaskDetail(item.camundaTaskId)">{{ item.formName }} - {{ item.stepName }}</a>
              </template>
              <template #description>
                <span>提交人: {{ item.submitterName || '未知' }} | 到达时间: {{ new Date(item.createdAt).toLocaleString() }}</span>
              </template>
              <template #avatar>
                <a-tag v-if="isRejectedTask(item)" color="error">待修改</a-tag>
                <a-tag v-else color="processing">待处理</a-tag>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button type="primary" @click="goToTaskDetail(item.camundaTaskId)">去处理</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getPendingTasks } from '@/api';

const router = useRouter();
const loading = ref(true);
const tasks = ref([]);

onMounted(async () => {
  try {
    tasks.value = await getPendingTasks();
  } catch (error) {
    // 错误已全局处理
  } finally {
    loading.value = false;
  }
});

const isRejectedTask = (task) => {
  // 简单约定：如果任务名包含“修改”或“调整”，则认为是驳回任务
  return task.stepName.includes('修改') || task.stepName.includes('调整');
};

const goToTaskDetail = (taskId) => {
  router.push({ name: 'task-detail', params: { taskId } });
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>