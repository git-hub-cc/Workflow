<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header :style="{ background: '#fff', padding: '0 24px', borderBottom: '1px solid #f0f0f0' }">
      <div class="header-content">
        <div class="logo" @click="$router.push('/')">
          <img src="/logo.svg" alt="logo" />
          <h1>表单工作流引擎</h1>
        </div>
        <div class="user-actions">
          <a-space>
            <span>当前用户:</span>
            <a-select
                :value="userStore.currentUserId"
                style="width: 200px"
                placeholder="请选择用户"
                @change="handleUserChange"
                :options="userOptions"
            ></a-select>
            <a-button type="primary" @click="$router.push({ name: 'task-list' })">
              我的待办
            </a-button>
          </a-space>
        </div>
      </div>
    </a-layout-header>
    <a-layout-content :style="{ padding: '0 24px' }">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </a-layout-content>
  </a-layout>
</template>

<script setup>
import { onMounted, computed } from 'vue';
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';

const userStore = useUserStore();
const router = useRouter();

onMounted(() => {
  userStore.fetchAllUsers();
});

const userOptions = computed(() =>
    userStore.allUsers.map(user => ({
      value: user.id,
      label: user.name,
    }))
);

const handleUserChange = (userId) => {
  userStore.setCurrentUserId(userId);
  // 用户切换后，如果当前在任务列表或详情页，刷新页面以获取新用户的任务
  if (router.currentRoute.value.name === 'task-list' || router.currentRoute.value.name === 'task-detail') {
    router.go(0); // 刷新当前页面
  }
};
</script>

<style scoped>
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
}
.logo img {
  height: 32px;
  margin-right: 16px;
}
.logo h1 {
  color: #1890ff;
  font-size: 20px;
  margin: 0;
  font-weight: 600;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>