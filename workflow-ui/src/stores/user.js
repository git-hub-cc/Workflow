import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { login, getAllUsers, getRoles, getGroups, getMyMenus, getUsersForPicker } from '@/api';
import { message } from 'ant-design-vue';
import router, { resetRouter, addDynamicRoutes } from '@/router';

export const useUserStore = defineStore('user', () => {
// --- State ---
    const token = ref(localStorage.getItem('token') || null);
    const currentUser = ref(JSON.parse(localStorage.getItem('user')) || null);
    const menus = ref(JSON.parse(localStorage.getItem('menus')) || []);
    const usersForManagement = ref([]);
    // 【核心移除】不再需要 usersForPicker 状态，因为现在是动态搜索
    const allRoles = ref([]);
    const allGroups = ref([]);
    const loading = ref(false);

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

            if (currentUser.value.passwordChangeRequired) {
                message.warn('为了您的账户安全，请先修改初始密码。');
                await router.push({ name: 'profile' });
            } else {
                message.success(`欢迎回来, ${currentUser.value.name}`);
                await router.push('/');
            }

            if (isAdmin.value) {
                // 不再在此处预加载
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
        usersForManagement.value = [];
        allRoles.value = [];
        allGroups.value = [];
        localStorage.clear();

        resetRouter();
        router.push('/login');
    }

    async function fetchUsersForManagement(force = false) {
        if (!isAdmin.value || (usersForManagement.value.length > 0 && !force)) return;
        loading.value = true;
        try {
            usersForManagement.value = await getAllUsers();
        } catch (error) {
            console.error("Failed to fetch users for management:", error);
        } finally {
            loading.value = false;
        }
    }

    async function fetchUsersForPicker(params = {}) {
        if (!isAdmin.value) return [];
        try {
            const response = await getUsersForPicker(params);
            // 【核心修改】返回整个分页响应对象，而不仅仅是 content
            return response;
        } catch (error) {
            console.error("Failed to fetch users for picker:", error);
            // 【核心修改】返回一个符合分页结构的对象
            return { content: [], totalElements: 0 };
        }
    }

    async function fetchAllRoles(force = false) {
        if (!isAdmin.value || (allRoles.value.length > 0 && !force)) return;
        loading.value = true;
        try { allRoles.value = await getRoles(); } catch (error) { console.error(error); } finally { loading.value = false; }
    }

    async function fetchAllGroups(force = false) {
        if (!isAdmin.value || (allGroups.value.length > 0 && !force)) return;
        loading.value = true;
        try { allGroups.value = await getGroups(); } catch (error) { console.error(error); } finally { loading.value = false; }
    }

    // 【新增】更新当前用户信息的方法
    function updateCurrentUser(profileData) {
        if(currentUser.value) {
            currentUser.value.name = profileData.name;
            currentUser.value.email = profileData.email;
            localStorage.setItem('user', JSON.stringify(currentUser.value));
        }
    }

    return {
        token, currentUser, menus,
        usersForManagement,
        allRoles, allGroups, loading,
        isAuthenticated, isAdmin, passwordChangeRequired,
        login: handleLogin, logout,
        fetchUsersForManagement, fetchUsersForPicker,
        fetchAllRoles, fetchAllGroups,
        fetchAndSetMenus,
        updateCurrentUser
    };
});