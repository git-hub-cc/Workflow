<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible>
      <div class="logo-sidebar" @click="$router.push('/')">
        <!-- [FIX] Bind src to the secure blob URL from the store -->
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
        </a-sub-menu>

        <MenuRenderer :menus="displayMenus" />
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header :style="{ background: '#fff', padding: '0 24px', borderBottom: '1px solid #f0f0f0' }">
        <div class="header-content">
          <div class="header-left">
            <a-breadcrumb>
              <a-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
                <router-link v-if="item.path" :to="item.path">{{ item.title }}</router-link>
                <span v-else>{{ item.title }}</span>
              </a-breadcrumb-item>
            </a-breadcrumb>
          </div>
          <div class="user-actions">
            <a-space>
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
                    <a-menu-item key="profile" @click="$router.push({ name: 'profile' })"><SettingOutlined /> 个人设置</a-menu-item>
                    <a-menu-item key="logout" @click="handleLogout"><LogoutOutlined /> 退出登录</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </div>
        </div>
      </a-layout-header>

      <!-- 【核心修改】将 a-layout-content 设置为 flex 容器，使其子元素可以弹性伸缩 -->
      <a-layout-content :style="{ display: 'flex', flexDirection: 'column' }">
        <!-- 【核心修改】移除 min-height，使用 flex: 1 让此 div 自动填充剩余空间 -->
        <div style="background: #fff; border-radius: 4px; flex: 1 1 0; min-height: 0;">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
        <a-layout-footer style="text-align: center; padding: 12px 50px; flex-shrink: 0;">
          {{ systemStore.settings.FOOTER_INFO || '© 2025 PPMC Workflow' }}
        </a-layout-footer>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { onMounted, ref, watch, computed, h } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { useSystemStore } from '@/stores/system';
import { getPendingTasks } from "@/api";
import { Modal, Menu } from "ant-design-vue";
import {
  DownOutlined, DashboardOutlined, TeamOutlined, SafetyCertificateOutlined, UsergroupAddOutlined,
  ApartmentOutlined, NodeIndexOutlined, FileSearchOutlined, UserOutlined, LogoutOutlined,
  SettingOutlined, MenuOutlined, TableOutlined,
  CheckSquareOutlined, SendOutlined, ForkOutlined, BuildOutlined
} from '@ant-design/icons-vue';
import { iconMap } from '@/utils/iconLibrary.js';

const { Item: MenuItem, SubMenu } = Menu;
const userStore = useUserStore();
const systemStore = useSystemStore();
const router = useRouter();
const route = useRoute();

const pendingTasksCount = ref(0);
const collapsed = ref(false);
const selectedKeys = ref([]);
const openKeys = ref(['user-center']);

// [FIX] The 'systemIconUrl' computed property is no longer needed here.
// The template now directly uses `systemStore.iconBlobUrl`.

const adminMenus = [
  {
    id: 'admin-root', name: '系统管理', icon: 'SettingOutlined', type: 'DIRECTORY',
    children: [
      { id: 'admin-dashboard', name: '仪表盘', path: '/admin/dashboard', icon: 'DashboardOutlined', type: 'DATA_LIST' },
      { id: 'admin-forms', name: '表单管理', path: '/admin/forms', icon: 'FormOutlined', type: 'DATA_LIST' },
      { id: 'admin-menus', name: '菜单管理', path: '/admin/menus', icon: 'AppstoreOutlined', type: 'DATA_LIST' },
      {
        id: 'admin-permissions', name: '用户权限', icon: 'TeamOutlined', type: 'DIRECTORY',
        children: [
          { id: 'admin-users', name: '用户管理', path: '/admin/users', icon: 'UserOutlined', type: 'DATA_LIST' },
          { id: 'admin-roles', name: '角色管理', path: '/admin/roles', icon: 'SafetyCertificateOutlined', type: 'DATA_LIST' },
          { id: 'admin-groups', name: '用户组管理', path: '/admin/groups', icon: 'UsergroupAddOutlined', type: 'DATA_LIST' },
        ]
      },
      {
        id: 'admin-org', name: '组织架构', icon: 'ApartmentOutlined', type: 'DIRECTORY',
        children: [
          { id: 'admin-org-chart', name: '组织架构图', path: '/admin/org-chart', icon: 'ApartmentOutlined', type: 'DATA_LIST' },
          { id: 'admin-org-management', name: '架构管理', path: '/admin/org-management', icon: 'ForkOutlined', type: 'DATA_LIST' }
        ]
      },
      { id: 'admin-instances', name: '实例管理', path: '/admin/instances', icon: 'NodeIndexOutlined', type: 'DATA_LIST' },
      {
        id: 'admin-logs', name: '日志管理', icon: 'FileSearchOutlined', type: 'DIRECTORY',
        children: [
          { id: 'admin-login-log', name: '登录日志', path: '/admin/logs/login', type: 'DATA_LIST' },
          { id: 'admin-operation-log', name: '操作日志', path: '/admin/logs/operation', type: 'DATA_LIST' },
        ]
      },
      { id: 'admin-settings', name: '系统设置', path: '/admin/settings', icon: 'BuildOutlined', type: 'DATA_LIST' },
    ]
  }
];

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
      '/my-submissions': { title: '我的申请', parent: { title: '个人中心' } }
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
  }
};

const checkTasks = async () => {
  if (!userStore.isAuthenticated) return;
  try {
    const tasks = await getPendingTasks();
    pendingTasksCount.value = tasks.length;
  } catch (e) { /* ignore */ }
};

onMounted(() => {
  if (userStore.isAuthenticated) {
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
.header-content { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; }
.logo-sidebar { height: 32px; margin: 16px; display: flex; align-items: center; justify-content: center; cursor: pointer; overflow: hidden; }
.logo-sidebar img { height: 32px; margin-right: 8px; }
.logo-sidebar h1 { color: white; font-size: 18px; margin: 0; font-weight: 600; white-space: nowrap; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.ant-menu-item .anticon, .ant-sub-menu-title .anticon { margin-right: 8px; }
.ant-menu-item { display: flex; align-items: center; }
</style>