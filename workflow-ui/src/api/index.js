import axios from 'axios';
import { message } from 'ant-design-vue';
import { useUserStore } from '@/stores/user';

// --- Authentication API ---
export const login = (credentials) => service.post('/auth/login', credentials);

// 创建 axios 实例
const service = axios.create({
    baseURL: '/api', // Vite 代理会自动处理
    timeout: 10000, // 请求超时时间
});

// 请求拦截器
service.interceptors.request.use(
    config => {
        const userStore = useUserStore();
        if (userStore.token) {
            config.headers['Authorization'] = `Bearer ${userStore.token}`;
        }
        return config;
    },
    error => {
        console.log(error);
        return Promise.reject(error);
    }
);

// 响应拦截器
service.interceptors.response.use(
    response => {
        if (response.headers['content-disposition']?.includes('attachment')) {
            return response;
        }
        return response.data;
    },
    error => {
        console.error('API Error:', error);
        if (error.response?.status === 499) {
            return Promise.reject(error);
        }
        if (error.response?.status === 401 || error.response?.status === 403) {
            message.error('认证失败或权限不足，请重新登录。');
            const userStore = useUserStore();
            userStore.logout();
        } else {
            message.error(error.response?.data?.message || error.message || '请求失败');
        }
        return Promise.reject(error);
    }
);

// --- 表单 API ---
export const getForms = () => service.get('/forms');
export const getFormById = (id) => service.get(`/forms/${id}`);
export const createForm = (data) => service.post('/forms', data);
export const deleteForm = (id) => service.delete(`/forms/${id}`);
export const getSubmissions = (formId, params) => service.get(`/forms/${formId}/submissions`, { params });
export const getSubmissionById = (submissionId) => service.get(`/forms/submissions/${submissionId}`);
export const submitForm = (formId, data) => service.post(`/forms/${formId}/submissions`, data);
export const fetchTableData = (url, params) => service.get(url, { params });
export const fetchTreeData = (source) => service.get('/admin/tree-data-source', { params: { source } });
export const importFromWord = (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return service.post('/forms/import-word', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 30000
    });
};

// --- 文件 API ---
export const uploadFile = (file) => { /* ... */ };
export const downloadFile = (fileId) => service.get(`/files/${fileId}`, { responseType: 'blob' });
export const getFilesForSubmission = (submissionId) => service.get(`/files/submission/${submissionId}`);

// --- 工作流 API ---
export const deployWorkflow = (data) => service.post('/workflows/deploy', data);
export const getWorkflowTemplate = (formId) => service.get(`/workflows/templates?formId=${formId}`);
export const getWorkflowHistory = (submissionId) => service.get(`/workflows/history/${submissionId}`);
export const updateWorkflowTemplate = (formId, data) => service.put(`/workflows/templates/${formId}`, data);
export const getGroupsForWorkflow = () => service.get('/workflows/groups');
// 【修改】获取用户列表的API指向新的、专门为选择器优化的接口
export const getUsersForPicker = () => service.get('/workflows/users');

// --- 任务 API ---
export const getPendingTasks = () => service.get('/tasks/pending');
export const getTaskById = (taskId) => service.get(`/tasks/${taskId}`);
export const completeTask = (taskId, data) => service.post(`/tasks/${taskId}/complete`, data);

// --- 用户和管理 API ---
export const changePassword = (data) => service.post('/users/me/change-password');
export const getOrganizationTree = () => service.get('/admin/organization-tree');

// --- 【核心重构】Admin User Management ---
// 获取完整用户列表的API指向新的、专门为管理页面优化的接口
export const getAllUsers = () => service.get('/admin/users');
export const createUser = (data) => service.post('/admin/users', data);
export const updateUser = (id, data) => service.put(`/admin/users/${id}`, data);
export const deleteUser = (id) => service.delete(`/admin/users/${id}`); // 语义上是禁用
export const resetPassword = (id) => service.post(`/admin/users/${id}/reset-password`);

// --- 角色管理 API ---
export const getRoles = () => service.get('/admin/roles');
export const createRole = (data) => service.post('/admin/roles', data);
export const updateRole = (id, data) => service.put(`/admin/roles/${id}`, data);
export const deleteRole = (id) => service.delete(`/admin/roles/${id}`);

// --- 用户组管理 API ---
export const getGroups = () => service.get('/admin/groups');
export const createGroup = (data) => service.post('/admin/groups', data);
export const updateGroup = (id, data) => service.put(`/admin/groups/${id}`, data);
export const deleteGroup = (id) => service.delete(`/admin/groups/${id}`);

// --- 菜单管理 API ---
export const getMyMenus = () => service.get('/menus/my-menus');
export const getMenuTree = () => service.get('/admin/menus/tree');
export const createMenu = (data) => service.post('/admin/menus', data);
export const updateMenu = (id, data) => service.put(`/admin/menus/${id}`, data);
export const deleteMenu = (id) => service.delete(`/admin/menus/${id}`);

// Admin Instance Management
export const getActiveInstances = () => service.get('/admin/instances');
export const terminateInstance = (instanceId, reason) => service.delete(`/admin/instances/${instanceId}?reason=${reason}`);
export const suspendInstance = (instanceId) => service.post(`/admin/instances/${instanceId}/suspend`);
export const activateInstance = (instanceId) => service.post(`/admin/instances/${instanceId}/activate`);
export const reassignTask = (taskId, newAssigneeId) => service.post(`/admin/tasks/${taskId}/reassign`, { newAssigneeId });

// Admin Dashboard
export const getDashboardStats = () => service.get('/dashboard/stats');

// --- 日志管理 API ---
export const getLoginLogs = (params) => service.get('/admin/logs/login', { params });
export const getOperationLogs = (params) => service.get('/admin/logs/operation', { params });

export default service;