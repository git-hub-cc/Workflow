<template>
  <div class="page-container">
    <a-page-header title="我的待办任务" />
    <a-list
        :data-source="tasks"
        :loading="loading"
        item-layout="horizontal"
        style="margin: 24px;"
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
        <div style="text-align: center; margin-top: 12px; height: 32px; line-height: 32px;">
          您当前没有待办任务。
        </div>
      </template>
    </a-list>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { getPendingTasks } from '@/api';
import { message } from 'ant-design-vue';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(true);
const tasks = ref([]);

onMounted(async () => {
  if (!userStore.currentUserId) {
    message.warn('请先选择一个用户');
    loading.value = false;
    return;
  }
  try {
    tasks.value = await getPendingTasks(userStore.currentUserId);
  } catch (error) {
    message.error('加载待办任务失败');
  } finally {
    loading.value = false;
  }
});

const goToTaskDetail = (taskId) => {
  router.push({ name: 'task-detail', params: { taskId } });
};
</script>