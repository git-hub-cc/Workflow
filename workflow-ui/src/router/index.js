import { createRouter, createWebHistory } from 'vue-router';
import AppLayout from '@/components/AppLayout.vue';
import { useUserStore } from '@/stores/user';
import { message } from "ant-design-vue";

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/login',
            name: 'login',
            component: () => import('../views/Login.vue'),
            meta: { title: '登录' }
        },
        {
            path: '/',
            component: AppLayout,
            meta: { requiresAuth: true }, // All routes under this layout require login
            children: [
                {
                    path: '',
                    name: 'home',
                    component: () => import('../views/Home.vue'),
                    meta: { title: '首页 - 表单管理' }
                },
                {
                    path: 'form/builder',
                    name: 'form-builder',
                    component: () => import('../views/FormBuilder.vue'),
                    meta: { title: '表单设计器', requiresAdmin: true }
                },
                {
                    path: 'form/viewer/:formId',
                    name: 'form-viewer',
                    component: () => import('../views/FormViewer.vue'),
                    props: true,
                    meta: { title: '填写表单' }
                },
                {
                    path: 'form/submissions/:formId',
                    name: 'form-submissions',
                    component: () => import('../views/Submissions.vue'),
                    props: true,
                    meta: { title: '查看提交数据' }
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
                {
                    path: 'profile',
                    name: 'profile',
                    component: () => import('../views/Profile.vue'),
                    meta: { title: '个人中心' }
                },
                // Admin Routes (已更新)
                {
                    path: 'admin/dashboard',
                    name: 'admin-dashboard',
                    component: () => import('../views/admin/Dashboard.vue'),
                    meta: { title: '仪表盘', requiresAdmin: true }
                },
                {
                    path: 'admin/users',
                    name: 'admin-users',
                    component: () => import('../views/admin/UserManagement.vue'),
                    meta: { title: '用户管理', requiresAdmin: true }
                },
                {
                    path: 'admin/instances',
                    name: 'admin-instances',
                    component: () => import('../views/admin/InstanceManagement.vue'),
                    meta: { title: '实例管理', requiresAdmin: true }
                },
                // --- 【新增路由】 ---
                {
                    path: 'admin/roles',
                    name: 'admin-roles',
                    component: () => import('../views/admin/RoleManagement.vue'),
                    meta: { title: '角色管理', requiresAdmin: true }
                },
                {
                    path: 'admin/org-chart',
                    name: 'admin-org-chart',
                    component: () => import('../views/admin/OrganizationChart.vue'),
                    meta: { title: '组织架构', requiresAdmin: true }
                }
            ]
        }
    ]
});

// 全局导航守卫 (已更新)
router.beforeEach((to, from, next) => {
    document.title = to.meta.title || '表单工作流引擎';
    const userStore = useUserStore();

    if (to.meta.requiresAuth && !userStore.isAuthenticated) {
        // 未登录访问受保护页面，跳转到登录页
        next({ name: 'login' });
    } else if (to.name === 'login' && userStore.isAuthenticated) {
        // 已登录访问登录页，跳转到首页
        next({ name: 'home' });
    } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
        // 非管理员访问管理员页面，跳转到首页
        message.warn('您没有权限访问此页面');
        next({ name: 'home' });
    } else if (userStore.isAuthenticated && userStore.passwordChangeRequired && to.name !== 'profile') {
        // 【新增】如果需要强制修改密码，且目标页面不是个人中心，则强制跳转
        message.warn('为了您的账户安全，请先修改初始密码。');
        next({ name: 'profile' });
    } else {
        next();
    }
});

export default router;