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
        // 在 Vue 组件之外使用 Pinia store
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
        // 如果是文件下载，直接返回整个响应
        if (response.headers['content-disposition']?.includes('attachment')) {
            return response;
        }
        return response.data;
    },
    error => {
        console.error('API Error:', error);

        // 【修改】针对登录时需要修改密码的特殊状态码，不在此处全局处理
        if (error.response?.status === 499) {
            return Promise.reject(error);
        }

        if (error.response?.status === 401 || error.response?.status === 403) {
            message.error('认证失败或权限不足，请重新登录。');
            // 如果认证失败，强制登出
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
export const getSubmissions = (formId) => service.get(`/forms/${formId}/submissions`);
export const getSubmissionById = (submissionId) => service.get(`/forms/submissions/${submissionId}`);
export const submitForm = (formId, data) => service.post(`/forms/${formId}/submissions`, data);
export const fetchTableData = (url, params) => service.get(url, { params });
export const fetchTreeData = (source) => service.get('/admin/tree-data-source', { params: { source } });


// --- 文件 API ---
export const uploadFile = (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return service.post('/files/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
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


// --- 任务 API ---
export const getPendingTasks = () => service.get('/tasks/pending');
export const getTaskById = (taskId) => service.get(`/tasks/${taskId}`);
export const completeTask = (taskId, data) => service.post(`/tasks/${taskId}/complete`, data);

// --- 用户和管理 API ---
export const getAllUsers = () => service.get('/workflows/users');
export const changePassword = (data) => service.post('/users/me/change-password', data);
export const getOrganizationTree = () => service.get('/admin/organization-tree');

// Admin User Management
export const createUser = (data) => service.post('/admin/users', data);
export const updateUser = (id, data) => service.put(`/admin/users/${id}`, data);
export const deleteUser = (id) => service.delete(`/admin/users/${id}`);
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

// --- 【新增】表单导入 API ---
export const importFromWord = (file) => {
    const formData = new FormData();
    formData.append('file', file);
    // 增加超时时间，因为后端解析可能需要一些时间
    return service.post('/forms/import-word', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        timeout: 30000 // 30秒
    });
};


export default service;