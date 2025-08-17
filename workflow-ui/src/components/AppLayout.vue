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
            <a-badge :dot="hasPendingTasks">
              <a-button type="primary" ghost @click="$router.push({ name: 'task-list' })">
                我的待办
              </a-button>
            </a-badge>
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
                  <a-menu-item v-if="userStore.isAdmin" key="admin-dashboard" @click="$router.push({ name: 'admin-dashboard' })">
                    <DashboardOutlined /> 仪表盘
                  </a-menu-item>
                  <a-menu-divider v-if="userStore.isAdmin" />
                  <a-menu-item v-if="userStore.isAdmin" key="admin-users" @click="$router.push({ name: 'admin-users' })">
                    <TeamOutlined /> 用户管理
                  </a-menu-item>
                  <!-- 【新增菜单项】 -->
                  <a-menu-item v-if="userStore.isAdmin" key="admin-roles" @click="$router.push({ name: 'admin-roles' })">
                    <SafetyCertificateOutlined /> 角色管理
                  </a-menu-item>
                  <a-menu-item v-if="userStore.isAdmin" key="admin-org-chart" @click="$router.push({ name: 'admin-org-chart' })">
                    <ApartmentOutlined /> 组织架构
                  </a-menu-item>
                  <a-menu-item v-if="userStore.isAdmin" key="admin-instances" @click="$router.push({ name: 'admin-instances' })">
                    <NodeIndexOutlined /> 实例管理
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="profile" @click="$router.push({ name: 'profile' })">
                    <UserOutlined /> 个人中心
                  </a-menu-item>
                  <a-menu-item key="logout" @click="handleLogout">
                    <LogoutOutlined /> 退出登录
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
import { onMounted, ref } from 'vue';
import { useUserStore } from '@/stores/user';
import { getPendingTasks } from "@/api";
import {
  DownOutlined,
  DashboardOutlined,
  TeamOutlined,
  SafetyCertificateOutlined,
  ApartmentOutlined,
  NodeIndexOutlined,
  UserOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue';
import { Modal } from "ant-design-vue";

const userStore = useUserStore();
const hasPendingTasks = ref(false);

const checkTasks = async () => {
  if (!userStore.isAuthenticated) return;
  try {
    const tasks = await getPendingTasks();
    hasPendingTasks.value = tasks.length > 0;
  } catch (e) {
    // ignore
  }
};

onMounted(() => {
  if (userStore.isAuthenticated) {
    userStore.fetchAllUsers();
    if(userStore.isAdmin) userStore.fetchAllRoles();
    checkTasks();
  }
});

const handleLogout = () => {
  Modal.confirm({
    title: '确认退出',
    content: '您确定要退出登录吗？',
    onOk: () => {
      userStore.logout();
    }
  });
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
.ant-menu-item .anticon {
  margin-right: 8px;
}
</style>