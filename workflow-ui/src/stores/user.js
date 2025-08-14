import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getUsers } from '@/api'
import { message } from 'ant-design-vue'

// 定义一个名为 'user' 的 store
export const useUserStore = defineStore('user', () => {
    // state: 存储所有用户列表和当前选中的用户ID
    const allUsers = ref([])
    const currentUserId = ref(localStorage.getItem('currentUserId') || null)

    // getters: 计算属性，获取当前用户的详细信息
    const currentUser = computed(() => {
        return allUsers.value.find(u => u.id === currentUserId.value)
    })

    // actions: 方法，用于修改 state
    async function fetchAllUsers() {
        try {
            const users = await getUsers()
            allUsers.value = users
            // 如果没有选中的用户ID，或者选中的ID无效，则默认选中第一个
            if (!currentUserId.value || !allUsers.value.some(u => u.id === currentUserId.value)) {
                if (users.length > 0) {
                    setCurrentUserId(users[0].id)
                }
            }
        } catch (error) {
            message.error('获取用户列表失败!')
            console.error(error)
        }
    }

    function setCurrentUserId(userId) {
        currentUserId.value = userId
        localStorage.setItem('currentUserId', userId)
    }

    return {
        allUsers,
        currentUserId,
        currentUser,
        fetchAllUsers,
        setCurrentUserId
    }
})