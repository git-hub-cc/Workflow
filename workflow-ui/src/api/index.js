import axios from 'axios';
import { message } from 'ant-design-vue';
import { useUserStore } from '@/stores/user';

// --- Authentication API ---
export const login = (credentials) => service.post('/auth/login', credentials);

// 创建 axios 实例
const service = axios.create({
    //baseURL: '/workflow/api', //服务器配置
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

        if (!error.response) {
            message.error('网络错误，请检查您的连接或联系管理员。');
            return Promise.reject(error);
        }

        const status = error.response.status;
        const data = error.response.data;
        const errorMessage = data?.message || error.message || '请求失败';

        switch (status) {
            case 400:
            case 409:
                message.error(`操作失败: ${errorMessage}`);
                break;
            case 401:
                if (error.config.url.endsWith('/auth/login')) {
                    message.error(`登录失败: ${errorMessage}`);
                } else {
                    message.error('认证已过期，请重新登录。');
                    try {
                        const userStore = useUserStore();
                        if (userStore.isAuthenticated) {
                            userStore.logout();
                        }
                    } catch (storeError) {
                        console.error("登出时 UserStore 发生错误:", storeError);
                        window.location.href = '/login';
                    }
                }
                break;
            case 403:
                message.error(`权限不足: ${errorMessage}`);
                break;
            // 【新增】处理 404 Not Found
            case 404:
                message.error(`资源未找到: ${errorMessage}`);
                break;
            case 500:
                message.error(`服务器内部错误: ${errorMessage}`);
                break;
            default:
                message.error(`请求失败: ${errorMessage} (状态码: ${status})`);
        }

        return Promise.reject(error);
    }
);

// --- 表单 API ---
// 【阶段二修改】getForms 函数现在接受参数
export const getForms = (params) => service.get('/forms', { params });
export const getFormById = (id) => service.get(`/forms/${id}`);
export const createForm = (data) => service.post('/forms', data);
export const updateForm = (id, data) => service.put(`/forms/${id}`, data);
export const deleteForm = (id, cascade = false) => service.delete(`/forms/${id}?cascade=${cascade}`);
export const getFormDependencies = (id) => service.get(`/forms/${id}/dependencies`);
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
// --- 【核心新增】用户草稿相关 API ---
export const createDraft = (formId, data) => service.post(`/forms/${formId}/submissions/draft`, data);
export const updateMyDraft = (submissionId, data) => service.put(`/forms/my-submissions/${submissionId}`, data);
export const submitDraft = (submissionId, data) => service.put(`/forms/submissions/${submissionId}/submit`, data);
// --- 【核心新增】删除草稿的 API ---
export const deleteMyDraft = (submissionId) => service.delete(`/forms/my-submissions/${submissionId}`);


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
export const getUsersForPicker = (params) => service.get('/workflows/users', { params });
export const getMySubmissions = (params) => service.get('/workflows/my-submissions', { params });
export const getWorkflowDiagram = (submissionId) => service.get(`/workflows/submission/${submissionId}/diagram`);
export const getAvailableBeans = (params) => service.get('/workflows/designer/beans', { params });
// 【新增】通过实例ID获取流程图
export const getWorkflowDiagramByInstanceId = (instanceId) => service.get(`/workflows/instance/${instanceId}/diagram`);


// --- 任务 API ---
export const getPendingTasks = (params) => service.get('/tasks/pending', { params });
export const getTaskById = (taskId) => service.get(`/tasks/${taskId}`);
export const completeTask = (taskId, data) => service.post(`/tasks/${taskId}/complete`, data);
export const getCompletedTasks = (params) => service.get('/tasks/completed', { params });


// --- 用户和管理 API ---
export const changePassword = (data) => service.post('/users/me/change-password', data);
export const getMyProfile = () => service.get('/users/me/profile');
export const updateMyProfile = (data) => service.put('/users/me/profile', data);
export const getOrganizationTree = () => service.get('/admin/organization-tree');
export const searchUsersForPicker = (keyword) => service.get('/users/search-for-picker', { params: { keyword } });


// --- 部门管理 API ---
export const getDepartmentTree = () => service.get('/admin/departments/tree');
export const createDepartment = (data) => service.post('/admin/departments', data);
export const updateDepartment = (id, data) => service.put(`/admin/departments/${id}`, data);
export const deleteDepartment = (id) => service.delete(`/admin/departments/${id}`);

// --- Admin User Management ---
// 【阶段二修改】getAllUsers 函数现在接受参数
export const getAllUsers = (params) => service.get('/admin/users', { params });
export const createUser = (data) => service.post('/admin/users', data);
export const updateUser = (id, data) => service.put(`/admin/users/${id}`, data);
export const disableUser = (id) => service.delete(`/admin/users/${id}`);
export const enableUser = (id) => service.post(`/admin/users/${id}/enable`);
export const resetPassword = (id) => service.post(`/admin/users/${id}/reset-password`);

// --- 角色管理 API ---
// 【阶段二修改】getRoles 函数现在接受参数
export const getRoles = (params) => service.get('/admin/roles', { params });
export const createRole = (data) => service.post('/admin/roles', data);
export const updateRole = (id, data) => service.put(`/admin/roles/${id}`, data);
export const deleteRole = (id) => service.delete(`/admin/roles/${id}`);

// --- 用户组管理 API ---
// 【阶段二修改】getGroups 函数现在接受参数
export const getGroups = (params) => service.get('/admin/groups', { params });
export const createGroup = (data) => service.post('/admin/groups', data);
export const updateGroup = (id, data) => service.put(`/admin/groups/${id}`, data);
export const deleteGroup = (id) => service.delete(`/admin/groups/${id}`);

// --- 菜单管理 API ---
export const getMyMenus = () => service.get('/menus/my-menus');
export const getMenuTree = () => service.get('/admin/menus/tree');
export const createMenu = (data) => service.post('/admin/menus', data);
export const updateMenu = (id, data) => service.put(`/admin/menus/${id}`, data);
export const deleteMenu = (id) => service.delete(`/admin/menus/${id}`);
export const updateMenuTree = (treeData) => service.put('/admin/menus/update-tree', treeData);

// --- Admin Instance Management ---
export const getProcessInstances = (params) => service.get('/admin/instances', { params });
export const terminateInstance = (instanceId, reason) => service.delete(`/admin/instances/${instanceId}?reason=${reason}`);
export const suspendInstance = (instanceId) => service.post(`/admin/instances/${instanceId}/suspend`);
export const activateInstance = (instanceId) => service.post(`/admin/instances/${instanceId}/activate`);
export const reassignTask = (taskId, newAssigneeId) => service.post(`/admin/tasks/${taskId}/reassign`, { newAssigneeId });
export const getProcessVariables = (instanceId) => service.get(`/admin/instances/${instanceId}/variables`);
export const updateProcessVariable = (instanceId, data) => service.put(`/admin/instances/${instanceId}/variables`, data);
export const batchSuspendInstances = (instanceIds) => service.post('/admin/instances/batch-suspend', { instanceIds });
export const batchActivateInstances = (instanceIds) => service.post('/admin/instances/batch-activate', { instanceIds });
export const batchTerminateInstances = (instanceIds, reason) => service.post('/admin/instances/batch-terminate', { instanceIds, reason });
export const getProcessDefinitionXml = (processDefinitionId) => service.get(`/admin/process-definitions/${processDefinitionId}/xml`);
export const getActivityInstances = (instanceId) => service.get(`/admin/instances/${instanceId}/activity-instances`);
export const getIncidents = (instanceId) => service.get(`/admin/instances/${instanceId}/incidents`);
export const retryJob = (jobId) => service.post(`/admin/jobs/${jobId}/retry`);


// Admin Dashboard
export const getDashboardStats = () => service.get('/dashboard/stats');

// --- 日志管理 API ---
export const getLoginLogs = (params) => service.get('/admin/logs/login', { params });
export const getOperationLogs = (params) => service.get('/admin/logs/operation', { params });

// --- 系统设置 API ---
export const getPublicSettings = () => service.get('/public/settings');
export const getAdminSettings = () => service.get('/admin/settings');
export const updateSettings = (settingsMap) => service.put('/admin/settings', settingsMap);

// --- 外部系统 API (模拟) ---
export const getExternalSuppliers = () => service.get('/external/suppliers');

// --- 报表 API ---
export const getReportData = (reportKey) => service.get(`/reports/${reportKey}`);

// --- 通知 API ---
export const getNotifications = (params) => service.get('/notifications', { params });
export const getUnreadNotificationCount = () => service.get('/notifications/unread-count');
export const markAllNotificationsAsRead = () => service.post('/notifications/mark-all-as-read');
export const markNotificationsAsRead = (ids) => service.post('/notifications/mark-as-read', ids);


export default service;