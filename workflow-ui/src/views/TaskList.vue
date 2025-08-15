<template>
  <div class="page-container">
    <a-page-header title="我的待办任务" />
    <div style="padding: 0 24px;">
      <a-list
          :data-source="tasks"
          :loading="loading"
          item-layout="horizontal"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta
                :title="`${item.formName} - ${item.stepName}`"
                :description="`提交人: ${item.submitterName || '未知'} | 到达时间: ${new Date(item.createdAt).toLocaleString()}`"
            />
            <template #actions>
              <a-button type="primary" @click="goToTaskDetail(item.camundaTaskId)">去处理</a-button>
            </template>
          </a-list-item>
        </template>
        <template #loadMore v-if="!loading && tasks.length === 0">
          <div style="text-align: center; margin: 24px 0;">
            <a-empty description="恭喜！您当前没有待办任务。" />
          </div>
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