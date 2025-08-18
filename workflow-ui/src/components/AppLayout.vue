<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible>
      <div class="logo-sidebar" @click="$router.push('/')">
        <img src="/logo.svg" alt="logo" />
        <h1 v-if="!collapsed">工作流引擎</h1>
      </div>
      <a-menu
          v-model:selectedKeys="selectedKeys"
          theme="dark"
          mode="inline"
          @click="handleMenuClick"
      >
        <!-- 【修改】使用新的 displayMenus 计算属性作为数据源 -->
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
                    <!-- 【核心移除】删除了这里的整个 <a-sub-menu> 管理员菜单 -->
                    <!-- 通用菜单 -->
                    <a-menu-item key="profile" @click="$router.push({ name: 'profile' })"><UserOutlined /> 个人中心</a-menu-item>
                    <a-menu-item key="logout" @click="handleLogout"><LogoutOutlined /> 退出登录</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </div>
        </div>
      </a-layout-header>

      <a-layout-content :style="{ margin: '16px' }">
        <div style="background: #fff; padding: 24px; min-height: calc(100vh - 64px - 32px - 53px); border-radius: 4px;">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { onMounted, ref, watch, computed, h } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { getPendingTasks } from "@/api";
import { Modal, Menu } from "ant-design-vue";
// --- 【核心新增】导入了更多图标用于静态菜单 ---
import {
  DownOutlined, DashboardOutlined, TeamOutlined, SafetyCertificateOutlined, UsergroupAddOutlined,
  ApartmentOutlined, NodeIndexOutlined, FileSearchOutlined, UserOutlined, LogoutOutlined,
  SettingOutlined, MenuOutlined, TableOutlined
} from '@ant-design/icons-vue';
import { iconMap } from '@/utils/iconLibrary.js';

const { Item: MenuItem, SubMenu } = Menu;

const userStore = useUserStore();
const router = useRouter();
const route = useRoute();

const hasPendingTasks = ref(false);
const collapsed = ref(false);
const selectedKeys = ref([]);

// --- 【核心新增】定义静态的管理员菜单结构 ---
const adminMenus = [
  {
    id: 'admin-root',
    name: '系统管理',
    icon: 'SettingOutlined',
    type: 'DIRECTORY',
    children: [
      { id: 'admin-dashboard', name: '仪表盘', path: '/admin/dashboard', icon: 'DashboardOutlined', type: 'DATA_LIST' },
      { id: 'admin-forms', name: '表单管理', path: '/admin/forms', icon: 'TableOutlined', type: 'DATA_LIST' },
      { id: 'admin-menus', name: '菜单管理', path: '/admin/menus', icon: 'MenuOutlined', type: 'DATA_LIST' },
      {
        id: 'admin-permissions',
        name: '用户权限',
        icon: 'TeamOutlined',
        type: 'DIRECTORY',
        children: [
          { id: 'admin-users', name: '用户管理', path: '/admin/users', icon: 'UserOutlined', type: 'DATA_LIST' },
          { id: 'admin-roles', name: '角色管理', path: '/admin/roles', icon: 'SafetyCertificateOutlined', type: 'DATA_LIST' },
          { id: 'admin-groups', name: '用户组管理', path: '/admin/groups', icon: 'UsergroupAddOutlined', type: 'DATA_LIST' },
        ]
      },
      { id: 'admin-org-chart', name: '组织架构', path: '/admin/org-chart', icon: 'ApartmentOutlined', type: 'DATA_LIST' },
      { id: 'admin-instances', name: '实例管理', path: '/admin/instances', icon: 'NodeIndexOutlined', type: 'DATA_LIST' },
      {
        id: 'admin-logs',
        name: '日志管理',
        icon: 'FileSearchOutlined',
        type: 'DIRECTORY',
        children: [
          { id: 'admin-login-log', name: '登录日志', path: '/admin/logs/login', type: 'DATA_LIST' },
          { id: 'admin-operation-log', name: '操作日志', path: '/admin/logs/operation', type: 'DATA_LIST' },
        ]
      },
    ]
  }
];

// --- 【核心新增】创建计算属性来合并菜单 ---
const displayMenus = computed(() => {
  if (userStore.isAdmin) {
    // 如果是管理员，将静态管理员菜单与动态菜单合并
    return [...adminMenus, ...userStore.menus];
  }
  // 否则，只显示动态菜单
  return userStore.menus;
});


// --- 修复后的递归菜单渲染器 ---
const MenuRenderer = {
  name: 'MenuRenderer',
  props: {
    menus: { type: Array, default: () => [] }
  },
  setup(props) {
    const renderMenuItems = (menuItems) => {
      return menuItems.map(menu => {
        const iconComponent = menu.icon ? (iconMap[menu.icon] || null) : null;
        const iconNode = iconComponent ? () => h(iconComponent) : null;

        if (menu.children && menu.children.length > 0) {
          return h(
              SubMenu,
              { key: menu.path || menu.id },
              {
                icon: iconNode,
                title: () => menu.name,
                default: () => renderMenuItems(menu.children)
              }
          );
        } else {
          // 【修改】为没有 path 但可点击的目录项（如根管理菜单）设置一个不可路由的 key
          const itemKey = menu.path || `item-${menu.id}`;
          return h(
              MenuItem,
              { key: itemKey, disabled: !menu.path }, // 如果没有 path，则禁用点击
              {
                icon: iconNode,
                default: () => menu.name
              }
          );
        }
      });
    };
    return () => renderMenuItems(props.menus);
  }
};

// --- 【修改】面包屑逻辑现在需要同时考虑动态菜单和静态管理菜单 ---
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

  // 使用合并后的 displayMenus 进行查找
  const matchedMenus = findPath(displayMenus.value, route.path);
  if (matchedMenus.length > 0) {
    matchedMenus.forEach(menu => {
      if (pathArray.findIndex(item => item.path === menu.path) === -1) {
        pathArray.push({ title: menu.name, path: menu.path });
      }
    });
  } else if (route.meta.title && route.path !== '/') {
    pathArray.push({ title: route.meta.title });
  }
  return pathArray;
});

// 监听路由变化，更新菜单选中项
watch(route, (newRoute) => {
  selectedKeys.value = [newRoute.path];
}, { immediate: true });

const handleMenuClick = ({ key }) => {
  // 防止点击不可导航的目录项
  if (key && typeof key === 'string' && key.startsWith('/')) {
    router.push(key);
  }
};

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
    // --- 【安全修复与状态管理修复】调整预加载逻辑 ---
    // AppLayout 不再负责加载数据，登录时 userStore 已完成预加载。
    // 这里只负责检查任务状态。
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
.header-left {
  display: flex;
  align-items: center;
}
.logo-sidebar {
  height: 32px;
  margin: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
}
.logo-sidebar img {
  height: 32px;
  margin-right: 8px;
}
.logo-sidebar h1 {
  color: white;
  font-size: 18px;
  margin: 0;
  font-weight: 600;
  white-space: nowrap;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.ant-menu-item .anticon, .ant-sub-menu-title .anticon {
  margin-right: 8px;
}
</style>