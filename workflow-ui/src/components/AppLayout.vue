<template>
  <a-layout style="min-height: 100vh">
    <!-- 在非移动端显示 Sider -->
    <a-layout-sider v-if="!isMobile" v-model:collapsed="collapsed" collapsible>
      <div class="logo-sidebar" @click="navigateTo('/')">
        <img :src="systemStore.iconBlobUrl || '/logo.svg'" alt="logo" />
        <h1 v-if="!collapsed">{{ systemStore.settings.SYSTEM_NAME || '工作流引擎' }}</h1>
      </div>
      <a-menu
          v-model:selectedKeys="selectedKeys"
          theme="dark"
          mode="inline"
          @click="handleMenuClick"
      >
        <a-sub-menu key="user-center">
          <template #icon><UserOutlined /></template>
          <template #title>个人中心</template>
          <a-menu-item key="/tasks">
            <template #icon><CheckSquareOutlined /></template>
            <span>我的待办</span>
            <a-badge :count="pendingTasksCount" :offset="[10, 0]" />
          </a-menu-item>
          <a-menu-item key="/my-submissions">
            <template #icon><SendOutlined /></template>
            <span>我的申请</span>
          </a-menu-item>
          <a-menu-item key="/completed-tasks">
            <template #icon><FileDoneOutlined /></template>
            <span>我的已办</span>
          </a-menu-item>
        </a-sub-menu>

        <MenuRenderer :menus="displayMenus" />
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header :style="{ background: '#fff', padding: '0 16px', borderBottom: '1px solid #f0f0f0' }">
        <div class="header-content">
          <div class="header-left">
            <!-- 在移动端显示汉堡包菜单按钮 -->
            <a-button v-if="isMobile" type="text" @click="drawerVisible = true" style="font-size: 20px; margin-right: 8px;">
              <MenuOutlined />
            </a-button>

            <a-breadcrumb>
              <a-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
                <router-link v-if="item.path" :to="item.path">{{ item.title }}</router-link>
                <span v-else>{{ item.title }}</span>
              </a-breadcrumb-item>
            </a-breadcrumb>
          </div>
          <div class="user-actions">
            <a-space>
              <a-popover v-model:open="notificationVisible" placement="bottomRight" trigger="click" overlayClassName="notification-popover-global">
                <template #content>
                  <NotificationPanel @close="notificationVisible = false" />
                </template>
                <a-badge :count="unreadCount" :overflow-count="99">
                  <a-button type="text" shape="circle" @click="notificationVisible = !notificationVisible">
                    <template #icon><BellOutlined /></template>
                  </a-button>
                </a-badge>
              </a-popover>

              <a-dropdown v-if="userStore.currentUser">
                <a class="ant-dropdown-link" @click.prevent>
                  <a-avatar :style="{ backgroundColor: systemStore.settings.THEME_COLOR, marginRight: '8px' }">
                    {{ userStore.currentUser.name.charAt(0) }}
                  </a-avatar>
                  <span v-if="!isMobile">{{ userStore.currentUser.name }}</span>
                  <DownOutlined />
                </a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="profile" @click="$router.push({ name: 'profile' })"><SettingOutlined /> 个人设置</a-menu-item>
                    <a-menu-item key="logout" @click="handleLogout"><LogoutOutlined /> 退出登录</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </div>
        </div>
      </a-layout-header>

      <a-layout-content :style="{ display: 'flex', flexDirection: 'column' }">
        <div style="background: #fff; border-radius: 4px; flex: 1 1 0; min-height: 0;">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
        <a-layout-footer style="text-align: center; padding: 12px 24px; flex-shrink: 0;">
          {{ systemStore.settings.FOOTER_INFO || '© 2025 PPMC Workflow' }}
        </a-layout-footer>
      </a-layout-content>
    </a-layout>

    <!-- 移动端抽屉导航 -->
    <a-drawer
        v-if="isMobile"
        v-model:open="drawerVisible"
        placement="left"
        :closable="false"
        width="250px"
    :body-style="{ padding: 0, backgroundColor: '#001529' }"
    >
    <div class="logo-sidebar" @click="navigateTo('/')">
      <img :src="systemStore.iconBlobUrl || '/logo.svg'" alt="logo" />
      <h1>{{ systemStore.settings.SYSTEM_NAME || '工作流引擎' }}</h1>
    </div>
    <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="inline"
        @click="handleMenuClick"
    >
      <a-sub-menu key="user-center">
        <template #icon><UserOutlined /></template>
        <template #title>个人中心</template>
        <a-menu-item key="/tasks">
          <template #icon><CheckSquareOutlined /></template>
          <span>我的待办</span>
          <a-badge :count="pendingTasksCount" :offset="[10, 0]" />
        </a-menu-item>
        <a-menu-item key="/my-submissions">
          <template #icon><SendOutlined /></template>
          <span>我的申请</span>
        </a-menu-item>
        <a-menu-item key="/completed-tasks">
          <template #icon><FileDoneOutlined /></template>
          <span>我的已办</span>
        </a-menu-item>
      </a-sub-menu>

      <MenuRenderer :menus="displayMenus" />
    </a-menu>
    </a-drawer>
  </a-layout>
</template>

<script setup>
import { onMounted, ref, watch, computed, h, defineAsyncComponent, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { useSystemStore } from '@/stores/system';
import { useNotificationStore } from '@/stores/notification';
import { Modal, Menu } from "ant-design-vue";
import {
  DownOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  CheckSquareOutlined,
  SendOutlined,
  FileDoneOutlined,
  BellOutlined,
  MenuOutlined,
} from '@ant-design/icons-vue';

import { iconMap } from '@/utils/iconLibrary.js';
import { adminMenus } from '@/router';

const NotificationPanel = defineAsyncComponent(() => import('@/views/notifications/NotificationPanel.vue'));

const { Item: MenuItem, SubMenu } = Menu;
const userStore = useUserStore();
const systemStore = useSystemStore();
const notificationStore = useNotificationStore();
const router = useRouter();
const route = useRoute();

const collapsed = ref(false);
const selectedKeys = ref([]);

const notificationVisible = ref(false);
const unreadCount = computed(() => notificationStore.unreadCount);
const pendingTasksCount = computed(() => userStore.pendingTasksCount);

const isMobile = ref(window.innerWidth < 768);
const drawerVisible = ref(false);

const handleResize = () => {
  isMobile.value = window.innerWidth < 768;
  if (!isMobile.value) {
    drawerVisible.value = false;
  }
};

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
});

const displayMenus = computed(() => {
  if (userStore.isAdmin) {
    return [...adminMenus, ...userStore.menus];
  }
  return userStore.menus;
});

const MenuRenderer = {
  name: 'MenuRenderer',
  props: { menus: { type: Array, default: () => [] } },
  setup(props) {
    const renderMenuItems = (menuItems) => {
      return menuItems.map(menu => {
        const iconComponent = menu.icon ? (iconMap[menu.icon] || null) : null;
        const iconNode = iconComponent ? () => h(iconComponent) : null;

        if (menu.type === 'EXTERNAL_LINK') {
          return h(MenuItem, { key: `ext-${menu.id}` }, {
            icon: iconNode,
            default: () => h('a', { href: menu.path, target: '_blank', rel: 'noopener noreferrer' }, menu.name)
          });
        }

        if (menu.children && menu.children.length > 0) {
          return h(SubMenu, { key: menu.path || menu.id }, { icon: iconNode, title: () => menu.name, default: () => renderMenuItems(menu.children) });
        } else {
          const itemKey = menu.path || `item-${menu.id}`;
          return h(MenuItem, { key: itemKey, disabled: !menu.path && menu.type !== 'DIRECTORY' }, { icon: iconNode, default: () => menu.name });
        }
      });
    };
    return () => renderMenuItems(props.menus);
  }
};

const breadcrumbs = computed(() => {
  const pathArray = [{ title: '首页', path: '/' }];
  const findPath = (menus, targetPath) => {
    for (const menu of menus) {
      if (menu.path === targetPath) return [menu];
      if (menu.children) {
        const found = findPath(menu.children, targetPath);
        if (found.length > 0) return [menu, ...found];
      }
    }
    return [];
  };
  const matchedMenus = findPath(displayMenus.value, route.path);
  if (matchedMenus.length > 0) {
    matchedMenus.forEach(menu => { if (pathArray.findIndex(item => item.path === menu.path) === -1) { pathArray.push({ title: menu.name, path: menu.path }); } });
  } else if (route.meta.title && route.path !== '/') {
    const staticMenuMapping = {
      '/tasks': { title: '我的待办', parent: { title: '个人中心' } },
      '/my-submissions': { title: '我的申请', parent: { title: '个人中心' } },
      '/completed-tasks': { title: '我的已办', parent: { title: '个人中心' } }
    };
    const staticInfo = staticMenuMapping[route.path];
    if (staticInfo) {
      if (staticInfo.parent) pathArray.push(staticInfo.parent);
      pathArray.push({ title: staticInfo.title });
    } else {
      pathArray.push({ title: route.meta.title });
    }
  }
  return pathArray;
});


watch(route, (newRoute) => {
  selectedKeys.value = [newRoute.path];
}, { immediate: true });

const handleMenuClick = ({ key }) => {
  if (key && typeof key === 'string' && key.startsWith('/')) {
    router.push(key);
    if (isMobile.value) {
      drawerVisible.value = false;
    }
  }
};

const navigateTo = (path) => {
  router.push(path);
  if (isMobile.value) {
    drawerVisible.value = false;
  }
};

const checkTasks = async () => {
  if (!userStore.isAuthenticated) return;
  await userStore.fetchPendingTasksCount();
};

onMounted(() => {
  window.addEventListener('resize', handleResize);
  if (userStore.isAuthenticated) {
    checkTasks();
    notificationStore.fetchUnreadCount();
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
:global(.notification-popover-global) {
  width: 360px;
}
.header-content { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; }
.logo-sidebar { height: 32px; margin: 16px; display: flex; align-items: center; justify-content: center; cursor: pointer; overflow: hidden; }
.logo-sidebar img { height: 32px; margin-right: 8px; }
.logo-sidebar h1 { color: white; font-size: 18px; margin: 0; font-weight: 600; white-space: nowrap; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.ant-menu-item .anticon, .ant-sub-menu-title .anticon { margin-right: 8px; }
.ant-menu-item { display: flex; align-items: center; }

.ant-dropdown-link span {
  display: inline-block;
}
</style>