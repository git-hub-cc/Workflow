import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { login, getAllUsers, getRoles, getGroups, deleteRole } from '@/api';
import { message } from 'ant-design-vue';
import router from '@/router';

export const useUserStore = defineStore('user', () => {
    // --- State ---
    const token = ref(localStorage.getItem('token') || null);
    const currentUser = ref(JSON.parse(localStorage.getItem('user')) || null);
    const allUsers = ref([]); // 用于用户管理和选择器
    const allRoles = ref([]); // 【新增】用于角色分配
    const allGroups = ref([]); // 【新增】用于用户组分配
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

            // 登录成功后，获取基础数据
            await fetchAllUsers();
            if (isAdmin.value) {
                await fetchAllRoles();
                await fetchAllGroups(); // 【新增】
            }

            await router.push('/');
            message.success(`欢迎回来, ${currentUser.value.name}`);

        } catch (error) {
            if (error.response?.status === 499) {
                message.error('您的密码是初始密码或已由管理员重置，请登录后立即修改！');
                const response = error.response.data;
                token.value = response.token;
                currentUser.value = response.user;
                localStorage.setItem('token', token.value);
                localStorage.setItem('user', JSON.stringify(currentUser.value));
                await router.push('/profile');
            }
            console.error("Login failed:", error);
        } finally {
            loading.value = false;
        }
    }

    function logout() {
        token.value = null;
        currentUser.value = null;
        allUsers.value = [];
        allRoles.value = [];
        allGroups.value = []; // 【新增】
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        router.push('/login');
    }

    async function fetchAllUsers() {
        if (!isAuthenticated.value) return;
        try {
            allUsers.value = await getAllUsers();
        } catch (error) {
            console.error(error);
        }
    }

    async function fetchAllRoles() {
        if (!isAdmin.value) return;
        try {
            allRoles.value = await getRoles();
        } catch (error) {
            console.error(error);
        }
    }

    // 【新增】获取所有用户组
    async function fetchAllGroups() {
        if (!isAdmin.value) return;
        try {
            allGroups.value = await getGroups();
        } catch (error) {
            console.error(error);
        }
    }

    // --- Admin-related actions ---
    function addUser(user) {
        allUsers.value.push(user);
    }
    function updateUserInStore(updatedUser) {
        const index = allUsers.value.findIndex(u => u.id === updatedUser.id);
        if (index !== -1) {
            allUsers.value[index] = updatedUser;
        }
    }
    function removeUser(userId) {
        const user = allUsers.value.find(u => u.id === userId);
        if (user) {
            user.status = 'INACTIVE';
        }
    }

    // --- 【修改】角色管理 actions ---
    function addRole(role) {
        allRoles.value.push(role);
    }
    function updateRoleInStore(updatedRole) {
        const index = allRoles.value.findIndex(r => r.id === updatedRole.id);
        if (index !== -1) {
            allRoles.value[index] = updatedRole;
        }
    }
    function removeRole(roleId) {
        allRoles.value = allRoles.value.filter(r => r.id !== roleId);
    }

    // 【新增】用户组管理 actions
    function addGroup(group) {
        allGroups.value.push(group);
    }
    function updateGroupInStore(updatedGroup) {
        const index = allGroups.value.findIndex(g => g.id === updatedGroup.id);
        if (index !== -1) {
            allGroups.value[index] = updatedGroup;
        }
    }
    function removeGroup(groupId) {
        allGroups.value = allGroups.value.filter(g => g.id !== groupId);
    }

    return {
        token,
        currentUser,
        allUsers,
        allRoles,
        allGroups,
        loading,
        isAuthenticated,
        isAdmin,
        passwordChangeRequired,
        login: handleLogin,
        logout,
        fetchAllUsers,
        fetchAllRoles,
        fetchAllGroups,
        addUser,
        updateUser: updateUserInStore,
        removeUser,
        addRole,
        updateRole: updateRoleInStore,
        removeRole,
        addGroup,
        updateGroup: updateGroupInStore,
        removeGroup,
    };
});