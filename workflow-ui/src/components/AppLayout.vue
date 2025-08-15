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
            <a-button type="primary" ghost @click="$router.push({ name: 'task-list' })">
              我的待办
            </a-button>
            <a-dropdown v-if="userStore.currentUser">
              <a class="ant-dropdown-link" @click.prevent>
                <a-avatar style="background-color: #1890ff; margin-right: 8px;">
                  {{ userStore.currentUser.name.charAt(0) }}
                </a-avatar>
                {{ userStore.currentUser.name }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item v-if="userStore.isAdmin" key="admin-users" @click="$router.push({ name: 'admin-users' })">
                    用户管理
                  </a-menu-item>
                  <a-menu-item v-if="userStore.isAdmin" key="admin-instances" @click="$router.push({ name: 'admin-instances' })">
                    实例管理
                  </a-menu-item>
                  <a-menu-divider v-if="userStore.isAdmin" />
                  <a-menu-item key="logout" @click="userStore.logout">
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </div>
      </div>
    </a-layout-header>
    <a-layout-content :style="{ padding: '24px' }">
      <div style="background: #fff; padding: 24px; min-height: 280px; border-radius: 4px;">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </a-layout-content>
  </a-layout>
</template>

<script setup>
import { onMounted } from 'vue';
import { useUserStore } from '@/stores/user';
import { DownOutlined } from '@ant-design/icons-vue';

const userStore = useUserStore();

onMounted(() => {
  // Fetch users for dropdowns if needed, only if logged in
  if (userStore.isAuthenticated) {
    userStore.fetchAllUsers();
  }
});

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