<template>
  <div class="page-container">
    <a-page-header title="欢迎使用表单工作流引擎" />

    <div class="content-padding">
      <a-card>
        <a-result
            status="success"
            title="您已成功登录系统"
            sub-title="请通过左侧的菜单栏开始您的工作。您可以发起新的申请，或处理您的待办任务。"
        >
          <template #extra>
            <a-space>
              <a-button type="primary" @click="goToFirstMenu">
                <template #icon><RocketOutlined /></template>
                开始工作
              </a-button>
              <a-button @click="$router.push({ name: 'task-list' })">
                <template #icon><CheckSquareOutlined /></template>
                查看我的待办
              </a-button>
            </a-space>
          </template>
        </a-result>
      </a-card>

      <!-- 管理员专属快捷入口 -->
      <div v-if="userStore.isAdmin" style="margin-top: 24px;">
        <a-divider>管理员快捷入口</a-divider>
        <!-- 【样式完善】使用响应式 gutter -->
        <a-row :gutter="{ xs: 16, sm: 16, md: 24 }">
          <a-col :xs="24" :sm="12" :md="8">
            <a-card hoverable @click="$router.push({ name: 'admin-forms' })">
              <a-card-meta title="表单管理" description="管理、创建和设计业务表单。">
                <template #avatar><FormOutlined style="font-size: 24px; color: #722ed1" /></template>
              </a-card-meta>
            </a-card>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8">
            <a-card hoverable @click="$router.push({ name: 'admin-menus' })">
              <a-card-meta title="菜单管理" description="配置用户在左侧看到的导航菜单。">
                <template #avatar><AppstoreOutlined style="font-size: 24px; color: #52c41a" /></template>
              </a-card-meta>
            </a-card>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8">
            <a-card hoverable @click="$router.push({ name: 'admin-dashboard' })">
              <a-card-meta title="系统仪表盘" description="查看系统运行的关键指标和统计。">
                <template #avatar><DashboardOutlined style="font-size: 24px; color: #faad14" /></template>
              </a-card-meta>
            </a-card>
          </a-col>
        </a-row>
      </div>

    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import {
  RocketOutlined,
  CheckSquareOutlined,
  FormOutlined,
  AppstoreOutlined,
  DashboardOutlined
} from '@ant-design/icons-vue';

const router = useRouter();
const userStore = useUserStore();

const goToFirstMenu = () => {
  const findFirstNavigableMenu = (menus) => {
    for (const menu of menus) {
      if (menu.path && menu.type !== 'DIRECTORY') {
        return menu;
      }
      if (menu.children && menu.children.length > 0) {
        const found = findFirstNavigableMenu(menu.children);
        if (found) return found;
      }
    }
    return null;
  };

  const firstMenu = findFirstNavigableMenu(userStore.menus);
  if (firstMenu) {
    router.push(firstMenu.path);
  } else {
    router.push({ name: 'task-list' });
  }
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
  border-radius: 4px;
}
</style>