<template>
  <div class="page-container">
    <a-page-header title="个人设置" />
    <div style="padding: 24px;">
      <a-tabs v-model:activeKey="activeTab" centered>
        <!-- 个人资料 Tab -->
        <a-tab-pane key="profile" tab="个人资料">
          <div class="tab-content">
            <a-spin :spinning="profileLoading">
              <a-form :model="profileState" layout="vertical" ref="profileFormRef" @finish="handleUpdateProfile">
                <a-form-item label="用户ID">
                  <a-input :value="userStore.currentUser.id" disabled />
                </a-form-item>
                <a-form-item label="姓名" name="name" :rules="[{ required: true, message: '请输入姓名' }]">
                  <a-input v-model:value="profileState.name" />
                </a-form-item>
                <a-form-item label="邮箱" name="email" :rules="[{ type: 'email', message: '请输入有效的邮箱地址' }]">
                  <a-input v-model:value="profileState.email" />
                </a-form-item>
                <a-form-item label="手机号" name="phoneNumber">
                  <a-input v-model:value="profileState.phoneNumber" />
                </a-form-item>
                <a-form-item>
                  <a-button type="primary" html-type="submit">保存资料</a-button>
                </a-form-item>
              </a-form>
            </a-spin>
          </div>
        </a-tab-pane>

        <!-- 修改密码 Tab -->
        <a-tab-pane key="password" tab="修改密码">
          <div class="tab-content">
            <a-alert
                v-if="userStore.passwordChangeRequired"
                message="强制密码修改"
                description="您的密码为初始密码或已被管理员重置，为了账户安全，请立即修改密码后再进行其他操作。"
                type="warning"
                show-icon
                style="margin-bottom: 24px;"
            />
            <a-form :model="passwordState" :rules="passwordRules" ref="passwordFormRef" layout="vertical" @finish="handleChangePassword">
              <a-form-item label="旧密码" name="oldPassword">
                <a-input-password v-model:value="passwordState.oldPassword" placeholder="请输入当前密码" />
              </a-form-item>
              <a-form-item label="新密码" name="newPassword">
                <a-input-password v-model:value="passwordState.newPassword" placeholder="请输入至少6位的新密码" />
              </a-form-item>
              <a-form-item label="确认新密码" name="confirmPassword">
                <a-input-password v-model:value="passwordState.confirmPassword" placeholder="请再次输入新密码" />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" html-type="submit" :loading="passwordLoading">确认修改</a-button>
              </a-form-item>
            </a-form>
          </div>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useUserStore } from '@/stores/user';
import { changePassword, getMyProfile, updateMyProfile } from '@/api';
import { message } from 'ant-design-vue';
import { useRouter } from "vue-router";

const userStore = useUserStore();
const router = useRouter();

const activeTab = ref('profile');

const profileLoading = ref(true);
const profileFormRef = ref();
const profileState = reactive({
  name: '',
  email: '',
  phoneNumber: '',
});

const passwordLoading = ref(false);
const passwordFormRef = ref();
const passwordState = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const validateConfirmPassword = (rule, value) => {
  if (value === '') {
    return Promise.reject('请再次输入新密码');
  } else if (value !== passwordState.newPassword) {
    return Promise.reject("两次输入的密码不一致!");
  } else {
    return Promise.resolve();
  }
};

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码' }],
  newPassword: [{ required: true, message: '请输入新密码' }, { min: 6, message: '密码长度不能少于6位' }],
  confirmPassword: [{ required: true, validator: validateConfirmPassword }],
};

onMounted(async () => {
  if (userStore.passwordChangeRequired) {
    activeTab.value = 'password';
  }

  try {
    const profileData = await getMyProfile();
    Object.assign(profileState, profileData);
  } catch (error) {
    message.error("加载个人资料失败");
  } finally {
    profileLoading.value = false;
  }
});

const handleUpdateProfile = async () => {
  try {
    await profileFormRef.value.validate();
    profileLoading.value = true;
    const updatedProfile = await updateMyProfile(profileState);
    userStore.updateCurrentUser(updatedProfile);
    message.success('个人资料更新成功！');
  } catch (error) {
    // API 错误已在拦截器中处理
  } finally {
    profileLoading.value = false;
  }
};

const handleChangePassword = async () => {
  try {
    await passwordFormRef.value.validate();
    passwordLoading.value = true;
    await changePassword({
      oldPassword: passwordState.oldPassword,
      newPassword: passwordState.newPassword,
    });
    message.success('密码修改成功，请重新登录！');
    userStore.logout();
  } catch (error) {
    console.error('Password change failed:', error);
  } finally {
    passwordLoading.value = false;
  }
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
.tab-content {
  max-width: 600px;
  margin: 24px auto;
}
:deep(.ant-tabs-nav) {
  margin-bottom: 32px !important;
}

/* 【核心新增】移动端样式调整 */
@media (max-width: 768px) {
  .page-container {
    padding: 12px;
  }
  .tab-content {
    margin: 16px 0;
  }
}
</style>