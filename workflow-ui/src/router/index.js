import { createRouter, createWebHistory } from 'vue-router';
import AppLayout from '@/components/AppLayout.vue';
import { useUserStore } from '@/stores/user';
import { message } from "ant-design-vue";

// --- 1. 定义静态路由 (所有人都能访问的，或作为布局容器的) ---
const staticRoutes = [
    {
        path: '/login',
        name: 'login',
        component: () => import('../views/Login.vue'),
        meta: { title: '登录' }
    },
    {
        path: '/',
        // 【新增】为父布局路由添加一个唯一的名称
        name: 'layout',
        component: AppLayout,
        meta: { requiresAuth: true },
        children: [
            // 基础页面，如个人中心、首页欢迎页
            {
                path: '',
                name: 'home',
                component: () => import('../views/Home.vue'),
                meta: { title: '首页' }
            },
            {
                path: 'profile',
                name: 'profile',
                component: () => import('../views/Profile.vue'),
                meta: { title: '个人中心' }
            },
            // 管理员专属的静态页面
            {
                path: 'admin/dashboard',
                name: 'admin-dashboard',
                component: () => import('../views/admin/Dashboard.vue'),
                meta: { title: '仪表盘', requiresAdmin: true }
            },
            // 【新增】表单管理页面路由
            {
                path: 'admin/forms',
                name: 'admin-forms',
                component: () => import('../views/admin/FormManagement.vue'),
                meta: { title: '表单管理', requiresAdmin: true }
            },
            // ... 其他固定的管理员页面
            {
                path: 'admin/users',
                name: 'admin-users',
                component: () => import('../views/admin/UserManagement.vue'),
                meta: { title: '用户管理', requiresAdmin: true }
            },
            {
                path: 'admin/roles',
                name: 'admin-roles',
                component: () => import('../views/admin/RoleManagement.vue'),
                meta: { title: '角色管理', requiresAdmin: true }
            },
            {
                path: 'admin/groups',
                name: 'admin-groups',
                component: () => import('../views/admin/UserGroupManagement.vue'),
                meta: { title: '用户组管理', requiresAdmin: true }
            },
            // 【新增】菜单管理页面路由
            {
                path: 'admin/menus',
                name: 'admin-menus',
                component: () => import('../views/admin/MenuManagement.vue'),
                meta: { title: '菜单管理', requiresAdmin: true }
            },
            {
                path: 'admin/instances',
                name: 'admin-instances',
                component: () => import('../views/admin/InstanceManagement.vue'),
                meta: { title: '实例管理', requiresAdmin: true }
            },
            {
                path: 'admin/org-chart',
                name: 'admin-org-chart',
                component: () => import('../views/admin/OrganizationChart.vue'),
                meta: { title: '组织架构', requiresAdmin: true }
            },
            {
                path: 'admin/logs/login',
                name: 'admin-login-log',
                component: () => import('../views/admin/LoginLog.vue'),
                meta: { title: '登录日志', requiresAdmin: true }
            },
            {
                path: 'admin/logs/operation',
                name: 'admin-operation-log',
                component: () => import('../views/admin/OperationLog.vue'),
                meta: { title: '操作日志', requiresAdmin: true }
            },
            // 固定的表单/流程/任务相关页面
            {
                path: 'form/builder',
                name: 'form-builder',
                component: () => import('../views/FormBuilder.vue'),
                meta: { title: '表单设计器', requiresAdmin: true }
            },
            // 【新增】查看指定表单所有提交记录的路由
            {
                path: 'forms/:formId/submissions',
                name: 'form-submissions',
                component: () => import('../views/SubmissionDetail.vue'),
                props: true, // 允许将路由参数 :formId 作为 props 传递给组件
                meta: { title: '提交记录' }
            },
            {
                path: 'submission/:submissionId',
                name: 'submission-detail',
                component: () => import('../views/SubmissionDetail.vue'),
                props: true,
                meta: { title: '申请详情' }
            },
            {
                path: 'workflow/designer/:formId',
                name: 'workflow-designer',
                component: () => import('../views/WorkflowDesigner.vue'),
                props: true,
                meta: { title: '流程设计器', requiresAdmin: true }
            },
            {
                path: 'tasks',
                name: 'task-list',
                component: () => import('../views/TaskList.vue'),
                meta: { title: '我的待办' }
            },
            {
                path: 'tasks/:taskId',
                name: 'task-detail',
                component: () => import('../views/TaskDetail.vue'),
                props: true,
                meta: { title: '任务处理' }
            },
        ]
    }
];

// --- 2. 创建 Router 实例 ---
let router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: staticRoutes
});

// --- 3. 动态添加路由的核心逻辑 ---
const dynamicRouteModules = {
    'FORM_ENTRY': () => import('../views/FormViewer.vue'),
    'DATA_LIST': () => import('../views/DataListView.vue'),
};

export function addDynamicRoutes(menus) {
    const layoutRouteName = 'layout'; // 【修改】使用路由名称
    if (!router.hasRoute(layoutRouteName)) {
        console.error(`Layout route named '${layoutRouteName}' not found!`);
        return;
    }

    const processMenus = (menuItems, parentPath = '') => {
        for (const menu of menuItems) {
            if (menu.path && menu.type !== 'DIRECTORY') {
                const componentLoader = dynamicRouteModules[menu.type];
                if (componentLoader) {
                    const route = {
                        path: menu.path,
                        // 【修改】确保动态路由的 name 也是唯一的
                        name: menu.path.replace(/^\//, '').replace(/\//g, '-'),
                        component: componentLoader,
                        meta: {
                            title: menu.name,
                            formId: menu.formDefinitionId, // 将 formId 注入 meta
                            menuId: menu.id,
                        },
                        // 如果组件是 FormViewer.vue 或 DataListView.vue，它们可能需要 formId 作为 prop
                        // 为了简化，我们让它们从 route.meta 中获取 formId
                        props: route => ({ formId: route.meta.formId })
                    };

                    // 【修改】使用父路由的名称来添加子路由
                    if (!router.hasRoute(route.name)) {
                        router.addRoute(layoutRouteName, route);
                    }
                }
            }
            if (menu.children && menu.children.length > 0) {
                processMenus(menu.children, menu.path);
            }
        }
    };
    processMenus(menus);
}


// --- 4. 重置路由的函数 ---
export function resetRouter() {
    const newRouter = createRouter({
        history: createWebHistory(import.meta.env.BASE_URL),
        routes: staticRoutes
    });
    router.matcher = newRouter.matcher; // a simple way to reset the routes
}


// --- 5. 全局导航守卫 (已更新) ---
let initialRoutesAdded = false;

router.beforeEach(async (to, from, next) => {
    document.title = to.meta.title || '表单工作流引擎';
    const userStore = useUserStore();

    if (to.meta.requiresAuth && !userStore.isAuthenticated) {
        next({ name: 'login' });
    } else if (to.name === 'login' && userStore.isAuthenticated) {
        next({ name: 'home' });
    } else if (userStore.isAuthenticated && !initialRoutesAdded) {
        // 如果用户已登录，但动态路由还未添加（例如刷新页面），则添加
        try {
            await userStore.fetchAndSetMenus();
            initialRoutesAdded = true;
            // 使用 replace: true, 这样浏览器历史记录就不会包含我们正在离开的（可能无效的）路由
            next({ ...to, replace: true });
        } catch (e) {
            // 如果菜单加载失败（例如 token 失效），登出并跳转到登录页
            userStore.logout();
            next({ name: 'login' });
        }
    } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
        message.warn('您没有权限访问此页面');
        next({ name: 'home' });
    } else if (userStore.isAuthenticated && userStore.passwordChangeRequired && to.name !== 'profile') {
        message.warn('为了您的账户安全，请先修改初始密码。');
        next({ name: 'profile' });
    } else {
        next();
    }
});

export default router;