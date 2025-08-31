import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { login, getMyMenus, getUsersForPicker, getPendingTasks } from '@/api';
import { message } from 'ant-design-vue';
import router, { resetRouter, addDynamicRoutes } from '@/router';

export const useUserStore = defineStore('user', () => {
// --- State ---
    const token = ref(localStorage.getItem('token') || null);
    const currentUser = ref(JSON.parse(localStorage.getItem('user')) || null);
    const menus = ref(JSON.parse(localStorage.getItem('menus')) || []);
    // 【阶段二修改】移除不再需要由 Store 管理的列表状态
    // const usersForManagement = ref([]);
    // const allRoles = ref([]);
    // const allGroups = ref([]);
    const loading = ref(false);
    const pendingTasksCount = ref(0);

// --- Getters ---
    const isAuthenticated = computed(() => !!token.value && !!currentUser.value);
    const isAdmin = computed(() => currentUser.value?.role === 'ADMIN');
    const passwordChangeRequired = computed(() => currentUser.value?.passwordChangeRequired || false);

// --- Actions ---
    async function handleLogin(credentials) {
        loading.value = true;
        try {
            const response = await login(credentials);
            token.value = response.token;
            currentUser.value = response.user;
            localStorage.setItem('token', token.value);
            localStorage.setItem('user', JSON.stringify(currentUser.value));

            await fetchAndSetMenus();
            await fetchPendingTasksCount();

            if (currentUser.value.passwordChangeRequired) {
                message.warn('为了您的账户安全，请先修改初始密码。');
                await router.push({ name: 'profile' });
            } else {
                message.success(`欢迎回来, ${currentUser.value.name}`);
                await router.push('/');
            }

        } catch (error) {
            console.error("Login failed:", error);
        } finally {
            loading.value = false;
        }
    }

    async function fetchAndSetMenus() {
        if (!isAuthenticated.value) return;
        try {
            const menuData = await getMyMenus();
            menus.value = menuData;
            localStorage.setItem('menus', JSON.stringify(menuData));
            addDynamicRoutes(menuData);
        } catch (error) {
            console.error("获取用户菜单失败:", error);
            message.error("获取用户菜单失败，请联系管理员");
            logout();
        }
    }

    function logout() {
        token.value = null;
        currentUser.value = null;
        menus.value = [];
        // 【阶段二修改】移除重置
        // usersForManagement.value = [];
        // allRoles.value = [];
        // allGroups.value = [];
        pendingTasksCount.value = 0;
        localStorage.clear();

        resetRouter();
        router.push('/login');
    }

    async function fetchPendingTasksCount() {
        if (!isAuthenticated.value) return;
        try {
            const response = await getPendingTasks({ page: 0, size: 1 });
            pendingTasksCount.value = response.totalElements;
        } catch (error) {
            console.error("Failed to fetch pending tasks count:", error);
            pendingTasksCount.value = 0;
        }
    }

    // 【阶段二移除】不再需要这些 Action，因为页面会直接通过 API Hook 获取数据
    // async function fetchUsersForManagement(force = false) { ... }
    // async function fetchAllRoles(force = false) { ... }
    // async function fetchAllGroups(force = false) { ... }

    async function fetchUsersForPicker(params = {}) {
        if (!isAdmin.value) return [];
        try {
            const response = await getUsersForPicker(params);
            return response;
        } catch (error) {
            console.error("Failed to fetch users for picker:", error);
            return { content: [], totalElements: 0 };
        }
    }

    function updateCurrentUser(profileData) {
        if(currentUser.value) {
            currentUser.value.name = profileData.name;
            currentUser.value.email = profileData.email;
            localStorage.setItem('user', JSON.stringify(currentUser.value));
        }
    }

    return {
        token, currentUser, menus,
        loading,
        pendingTasksCount,
        isAuthenticated, isAdmin, passwordChangeRequired,
        login: handleLogin, logout,
        fetchUsersForPicker,
        fetchAndSetMenus,
        updateCurrentUser,
        fetchPendingTasksCount
    };
});