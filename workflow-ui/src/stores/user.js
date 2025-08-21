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
// --- 【核心重构】数据源分离 ---
    const usersForManagement = ref([]); // 用于用户管理页面的完整用户数据
    const usersForPicker = ref([]);     // 用于选择器的简化用户数据
    const allRoles = ref([]);
    const allGroups = ref([]);
    const loading = ref(false);

// --- Getters ---
    const isAuthenticated = computed(() => !!token.value && !!currentUser.value);
    const isAdmin = computed(() => currentUser.value?.role === 'ADMIN');
    // --- 【核心修改】直接从 currentUser 获取 passwordChangeRequired 状态 ---
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

            // --- 【核心修改】检查 passwordChangeRequired 标志 ---
            if (currentUser.value.passwordChangeRequired) {
                // 如果需要修改密码，提示并直接跳转到个人设置页面
                message.warn('为了您的账户安全，请先修改初始密码。');
                await router.push({ name: 'profile' });
            } else {
                // 否则，正常进入系统
                message.success(`欢迎回来, ${currentUser.value.name}`);
                await router.push('/');
            }

            // 预加载管理员数据（在后台进行，不影响用户体验）
            if (isAdmin.value) {
                fetchUsersForPicker();
                fetchUsersForManagement();
                fetchAllRoles();
                fetchAllGroups();
            }

        } catch (error) {
            // 其他错误（如密码错误、用户不存在）仍由全局拦截器处理
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
        usersForPicker.value = [];
        allRoles.value = [];
        allGroups.value = [];
        localStorage.clear(); // 清空所有localStorage

        resetRouter();
        router.push('/login');
    }

// --- 【核心重构】数据获取方法分离 ---
    async function fetchUsersForManagement() {
        if (!isAdmin.value) return;
        try {
            usersForManagement.value = await getAllUsers();
        } catch (error) {
            console.error("Failed to fetch users for management:", error);
        }
    }

    async function fetchUsersForPicker() {
// --- 【安全修复】增加前端权限判断，与后端API权限保持一致 ---
// 确保只有管理员才能调用这个获取全量用户的接口
        if (!isAdmin.value) {
            console.warn("Attempted to fetch all users for picker without admin rights. Skipped.");
            usersForPicker.value = []; // 清空可能存在的旧数据
            return;
        }
        try {
            usersForPicker.value = await getUsersForPicker();
        } catch (error) {
            console.error("Failed to fetch users for picker:", error);
        }
    }

    async function fetchAllRoles() {
        if (!isAdmin.value) return;
        try { allRoles.value = await getRoles(); } catch (error) { console.error(error); }
    }

    async function fetchAllGroups() {
        if (!isAdmin.value) return;
        try { allGroups.value = await getGroups(); } catch (error) { console.error(error); }
    }

    return {
        token, currentUser, menus,
        usersForManagement, usersForPicker, // 分离的数据源
        allRoles, allGroups, loading,
        isAuthenticated, isAdmin, passwordChangeRequired,
        login: handleLogin, logout,
        fetchUsersForManagement, fetchUsersForPicker, // 分离的获取方法
        fetchAllRoles, fetchAllGroups,
        fetchAndSetMenus
    };
});