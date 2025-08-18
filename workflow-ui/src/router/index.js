import { createRouter, createWebHistory } from 'vue-router';
import AppLayout from '@/components/AppLayout.vue';
import { useUserStore } from '@/stores/user';
import { message } from "ant-design-vue";

// --- 1. 静态路由定义 (保持不变) ---
const staticRoutes = [
    {
        path: '/login',
        name: 'login',
        component: () => import('../views/Login.vue'),
        meta: { title: '登录' }
    },
    {
        path: '/',
        name: 'layout',
        component: AppLayout,
        meta: { requiresAuth: true },
        children: [
            // 基础页面
            { path: '', name: 'home', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
            { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } },
            // 管理员专属的静态页面
            { path: 'admin/dashboard', name: 'admin-dashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { title: '仪表盘', requiresAdmin: true } },
            { path: 'admin/forms', name: 'admin-forms', component: () => import('../views/admin/FormManagement.vue'), meta: { title: '表单管理', requiresAdmin: true } },
            { path: 'admin/users', name: 'admin-users', component: () => import('../views/admin/UserManagement.vue'), meta: { title: '用户管理', requiresAdmin: true } },
            { path: 'admin/roles', name: 'admin-roles', component: () => import('../views/admin/RoleManagement.vue'), meta: { title: '角色管理', requiresAdmin: true } },
            { path: 'admin/groups', name: 'admin-groups', component: () => import('../views/admin/UserGroupManagement.vue'), meta: { title: '用户组管理', requiresAdmin: true } },
            { path: 'admin/menus', name: 'admin-menus', component: () => import('../views/admin/MenuManagement.vue'), meta: { title: '菜单管理', requiresAdmin: true } },
            { path: 'admin/instances', name: 'admin-instances', component: () => import('../views/admin/InstanceManagement.vue'), meta: { title: '实例管理', requiresAdmin: true } },
            { path: 'admin/org-chart', name: 'admin-org-chart', component: () => import('../views/admin/OrganizationChart.vue'), meta: { title: '组织架构', requiresAdmin: true } },
            { path: 'admin/logs/login', name: 'admin-login-log', component: () => import('../views/admin/LoginLog.vue'), meta: { title: '登录日志', requiresAdmin: true } },
            { path: 'admin/logs/operation', name: 'admin-operation-log', component: () => import('../views/admin/OperationLog.vue'), meta: { title: '操作日志', requiresAdmin: true } },
            // 固定的表单/流程/任务相关页面
            { path: 'form/builder', name: 'form-builder', component: () => import('../views/FormBuilder.vue'), meta: { title: '表单设计器', requiresAdmin: true } },
            { path: 'forms/:formId/submissions', name: 'form-submissions', component: () => import('../views/Submissions.vue'), props: true, meta: { title: '提交记录' } },
            { path: 'submission/:submissionId', name: 'submission-detail', component: () => import('../views/SubmissionDetail.vue'), props: true, meta: { title: '申请详情' } },
            { path: 'workflow/designer/:formId', name: 'workflow-designer', component: () => import('../views/WorkflowDesigner.vue'), props: true, meta: { title: '流程设计器', requiresAdmin: true } },
            { path: 'tasks', name: 'task-list', component: () => import('../views/TaskList.vue'), meta: { title: '我的待办' } },
            { path: 'tasks/:taskId', name: 'task-detail', component: () => import('../views/TaskDetail.vue'), props: true, meta: { title: '任务处理' } },
        ]
    }
];

// --- 2. 创建 Router 实例 (保持不变) ---
let router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: staticRoutes
});

// --- 3. 动态添加路由的核心逻辑 (保持不变) ---
const dynamicRouteModules = {
    'FORM_ENTRY': () => import('../views/FormViewer.vue'),
    'DATA_LIST': () => import('../views/DataListView.vue'),
};

export function addDynamicRoutes(menus) {
    const layoutRouteName = 'layout';
    if (!router.hasRoute(layoutRouteName)) {
        console.error(`未找到名为 '${layoutRouteName}' 的布局路由!`);
        return;
    }
    const processMenus = (menuItems) => {
        for (const menu of menuItems) {
            if (menu.path && menu.type !== 'DIRECTORY') {
                const componentLoader = dynamicRouteModules[menu.type];
                if (componentLoader) {
                    const route = {
                        path: menu.path,
                        name: menu.path.replace(/^\//, '').replace(/\//g, '-'),
                        component: componentLoader,
                        meta: { title: menu.name, formId: menu.formDefinitionId, menuId: menu.id },
                        props: route => ({ formId: route.meta.formId })
                    };
                    if (!router.hasRoute(route.name)) {
                        router.addRoute(layoutRouteName, route);
                    }
                }
            }
            if (menu.children && menu.children.length > 0) {
                processMenus(menu.children);
            }
        }
    };
    processMenus(menus);
}

// --- 4. 重置路由的函数 (保持不变) ---
export function resetRouter() {
    const newRouter = createRouter({
        history: createWebHistory(import.meta.env.BASE_URL),
        routes: staticRoutes
    });
    router.matcher = newRouter.matcher;
}

// --- 5. 全局导航守卫 (核心修改) ---
router.beforeEach(async (to, from, next) => {
    document.title = to.meta.title || '表单工作流引擎';
    const userStore = useUserStore();

    // 检查1: 未登录用户访问需要认证的页面 -> 跳转登录页
    if (to.meta.requiresAuth && !userStore.isAuthenticated) {
        next({ name: 'login' });
    }
    // 检查2: 已登录用户访问登录页 -> 跳转首页
    else if (to.name === 'login' && userStore.isAuthenticated) {
        next({ name: 'home' });
    }
    // 检查3: 非管理员访问管理员页面 -> 提示并跳转首页
    else if (to.meta.requiresAdmin && !userStore.isAdmin) {
        message.warn('您没有权限访问此页面');
        next({ name: 'home' });
    }
    // 检查4: 需要强制修改密码的用户访问非个人中心页面 -> 提示并跳转个人中心
    else if (userStore.isAuthenticated && userStore.passwordChangeRequired && to.name !== 'profile') {
        message.warn('为了您的账户安全，请先修改初始密码。');
        next({ name: 'profile' });
    }
    // 所有检查通过
    else {
        next();
    }
});

export default router;