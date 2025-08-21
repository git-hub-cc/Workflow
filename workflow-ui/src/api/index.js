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

        // 1. 网络错误或请求被取消等没有响应体的情况
        if (!error.response) {
            message.error('网络错误，请检查您的连接或联系管理员。');
            return Promise.reject(error);
        }

        // 2. 有响应体，根据状态码和后端返回的结构化信息进行处理
        const status = error.response.status;
        const data = error.response.data;
        // 优先使用后端在 ErrorResponse DTO 中定义的友好消息
        const errorMessage = data?.message || error.message || '请求失败';

        switch (status) {
            case 400: // Bad Request: 通常是参数验证失败
            case 409: // Conflict: 业务逻辑冲突，如资源被占用
                message.error(`操作失败: ${errorMessage}`);
                break;
            case 401: // Unauthorized: Token 无效或过期
                message.error('认证已过期，请重新登录。');
                try {
                    const userStore = useUserStore();
                    // 只有当用户当前是登录状态时才执行登出，避免在登录页重复跳转
                    if (userStore.isAuthenticated) {
                        userStore.logout();
                    }
                } catch (storeError) {
                    // 如果 store 初始化失败或出现其他问题，强制跳转
                    console.error("登出时 UserStore 发生错误:", storeError);
                    window.location.href = '/login';
                }
                break;
            case 403: // Forbidden: 用户已认证，但无权访问该资源
                message.error(`权限不足: ${errorMessage}`);
                break;
            case 499: // 自定义状态码: 强制修改密码
                // 此状态码由登录页面专门处理，拦截器层面仅需将其继续抛出即可
                return Promise.reject(error);
            case 500: // Internal Server Error: 服务器内部错误
                message.error(`服务器内部错误: ${errorMessage}`);
                break;
            default:
                message.error(`请求失败: ${errorMessage} (状态码: ${status})`);
        }

        return Promise.reject(error);
    }
);

// --- 表单 API ---
export const getForms = () => service.get('/forms');
export const getFormById = (id) => service.get(`/forms/${id}`);
export const createForm = (data) => service.post('/forms', data);
export const updateForm = (id, data) => service.put(`/forms/${id}`, data);
export const deleteForm = (id) => service.delete(`/forms/${id}`);
export const getSubmissions = (formId, params) => service.get(`/forms/${formId}/submissions`, { params });
export const getSubmissionById = (submissionId) => service.get(`/forms/submissions/${submissionId}`);
export const submitForm = (formId, data) => service.post(`/forms/${formId}/submissions`, data);
export const updateSubmission = (submissionId, data) => service.put(`/forms/submissions/${submissionId}`, data);
export const deleteSubmission = (submissionId) => service.delete(`/forms/submissions/${submissionId}`);

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
export const uploadFile = (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return service.post('/files/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 30000
    });
};
export const downloadFile = (fileId) => service.get(`/files/${fileId}`, { responseType: 'blob' });
export const getFilesForSubmission = (submissionId) => service.get(`/files/submission/${submissionId}`);

// --- 工作流 API ---
export const deployWorkflow = (data) => service.post('/workflows/deploy', data);
export const getWorkflowTemplate = (formId) => service.get(`/workflows/templates?formId=${formId}`);
export const getWorkflowHistory = (submissionId) => service.get(`/workflows/history/${submissionId}`);
export const updateWorkflowTemplate = (formId, data) => service.put(`/workflows/templates/${formId}`, data);
export const getGroupsForWorkflow = () => service.get('/workflows/groups');
export const getUsersForPicker = () => service.get('/workflows/users');
export const getMySubmissions = (params) => service.get('/workflows/my-submissions', { params });

// --- 任务 API ---
export const getPendingTasks = (params) => service.get('/tasks/pending', { params });
export const getTaskById = (taskId) => service.get(`/tasks/${taskId}`);
export const completeTask = (taskId, data) => service.post(`/tasks/${taskId}/complete`, data);

// --- 用户和管理 API ---
export const changePassword = (data) => service.post('/users/me/change-password');
export const getOrganizationTree = () => service.get('/admin/organization-tree');

// --- 部门管理 API ---
export const getDepartmentTree = () => service.get('/admin/departments/tree');
export const createDepartment = (data) => service.post('/admin/departments', data);
export const updateDepartment = (id, data) => service.put(`/admin/departments/${id}`, data);
export const deleteDepartment = (id) => service.delete(`/admin/departments/${id}`);

// --- Admin User Management ---
export const getAllUsers = () => service.get('/admin/users');
export const createUser = (data) => service.post('/admin/users', data);
export const updateUser = (id, data) => service.put(`/admin/users/${id}`, data);
export const disableUser = (id) => service.delete(`/admin/users/${id}`);
export const enableUser = (id) => service.post(`/admin/users/${id}/enable`);
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

// --- 【核心新增】系统设置 API ---
export const getPublicSettings = () => service.get('/public/settings');
export const getAdminSettings = () => service.get('/admin/settings');
export const updateSettings = (settingsMap) => service.put('/admin/settings', settingsMap);

export default service;