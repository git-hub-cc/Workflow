<template>
  <div class="page-container">
    <a-page-header title="系统设置" sub-title="自定义系统外观和基本信息" />
    <div style="padding: 24px;">
      <a-spin :spinning="loading">
        <a-form
            :model="formState"
            layout="vertical"
            style="max-width: 800px; margin: auto;"
        >
          <a-card title="基础信息">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="系统名称" name="SYSTEM_NAME">
                  <a-input v-model:value="formState.SYSTEM_NAME" placeholder="例如：PPMC 协同办公平台" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="系统主题色" name="THEME_COLOR">
                  <input type="color" v-model="formState.THEME_COLOR" class="color-picker" />
                  <span style="margin-left: 8px;">{{ formState.THEME_COLOR }}</span>
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="页脚信息" name="FOOTER_INFO">
                  <a-textarea v-model:value="formState.FOOTER_INFO" placeholder="例如：© 2025 PPMC Inc. All Rights Reserved." :rows="2" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-card>

          <a-card title="外观设置" style="margin-top: 24px;">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="系统图标 (Favicon)">
                  <a-upload
                      :file-list="iconFileList"
                      :custom-request="options => handleUpload(options, 'SYSTEM_ICON_ID')"
                      @remove="file => handleRemove(file, 'SYSTEM_ICON_ID')"
                      :max-count="1"
                      list-type="picture"
                  >
                    <a-button><upload-outlined /> 上传新图标 (.ico, .png, .svg)</a-button>
                  </a-upload>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="登录页背景图">
                  <a-upload
                      :file-list="bgFileList"
                      :custom-request="options => handleUpload(options, 'LOGIN_BACKGROUND_ID')"
                      @remove="file => handleRemove(file, 'LOGIN_BACKGROUND_ID')"
                      :max-count="1"
                      list-type="picture-card"
                  >
                    <div>
                      <plus-outlined />
                      <div style="margin-top: 8px">上传背景图</div>
                    </div>
                  </a-upload>
                </a-form-item>
              </a-col>
            </a-row>
          </a-card>

          <a-form-item style="margin-top: 24px; text-align: center;">
            <a-button type="primary" :loading="systemStore.loading" @click="handleSave">
              保存设置
            </a-button>
          </a-form-item>
        </a-form>
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
import { useSystemStore } from '@/stores/system';
import { uploadFile, downloadFile } from '@/api';
import { message } from 'ant-design-vue';
import { UploadOutlined, PlusOutlined } from '@ant-design/icons-vue';

const systemStore = useSystemStore();
const loading = ref(true);
const formState = reactive({
  // 【修复】为 formState 提供初始默认值，
  // 特别是为 THEME_COLOR 设置一个有效的十六进制颜色，
  // 以防止在从API加载数据前，<input type="color"> 因绑定空值而报错。
  SYSTEM_NAME: '',
  THEME_COLOR: '#1890ff',
  FOOTER_INFO: '',
});

const iconFileList = ref([]);
const bgFileList = ref([]);

const generatePreviewUrl = async (fileId) => {
  if (!fileId) return null;
  try {
    const response = await downloadFile(fileId);
    // --- [FIX] The 'downloadFile' API returns the full axios response object for blobs.
    // We need to access the '.data' property which contains the actual Blob.
    return URL.createObjectURL(response.data);
  } catch (error) {
    console.error("Failed to generate preview URL for file:", fileId, error);
    return null;
  }
};

const updateFileLists = async () => {
  if (formState.SYSTEM_ICON_ID) {
    const previewUrl = await generatePreviewUrl(formState.SYSTEM_ICON_ID);
    iconFileList.value = [{
      uid: formState.SYSTEM_ICON_ID,
      name: '当前图标',
      status: 'done',
      url: previewUrl,
      thumbUrl: previewUrl,
    }];
  } else {
    iconFileList.value = [];
  }
  if (formState.LOGIN_BACKGROUND_ID) {
    const previewUrl = await generatePreviewUrl(formState.LOGIN_BACKGROUND_ID);
    bgFileList.value = [{
      uid: formState.LOGIN_BACKGROUND_ID,
      name: '当前背景',
      status: 'done',
      url: previewUrl,
      thumbUrl: previewUrl,
    }];
  } else {
    bgFileList.value = [];
  }
};

watch(() => [formState.SYSTEM_ICON_ID, formState.LOGIN_BACKGROUND_ID], () => {
  updateFileLists();
});


onMounted(async () => {
  const settings = await systemStore.fetchAdminSettings();
  Object.assign(formState, settings);
  loading.value = false;
});


const handleUpload = async ({ file, onSuccess, onError }, key) => {
  try {
    const res = await uploadFile(file);
    formState[key] = res.id;
    message.success(`${file.name} 上传成功`);
    onSuccess(res);
  } catch (error) {
    message.error(`${file.name} 上传失败`);
    onError(error);
  }
};

const handleRemove = (file, key) => {
  if (file && file.url && file.url.startsWith('blob:')) {
    URL.revokeObjectURL(file.url);
  }
  formState[key] = null;
};

const handleSave = async () => {
  await systemStore.saveSettings(formState);
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
.color-picker {
  vertical-align: middle;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  width: 32px;
  height: 32px;
  cursor: pointer;
  padding: 2px;
}
</style>