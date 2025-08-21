<template>
  <div>
    <a-upload
        v-model:file-list="fileList"
        :custom-request="handleUpload"
        :before-upload="beforeUpload"
        @remove="handleRemove"
        :multiple="true"
    >
      <a-button>
        <upload-outlined></upload-outlined>
        点击上传
      </a-button>
    </a-upload>
  </div>
</template>

<script setup>
import { ref, watch, toRefs } from 'vue';
import { message } from 'ant-design-vue';
import { UploadOutlined } from '@ant-design/icons-vue';
import { uploadFile } from '@/api';

// 【核心修改】将 props 从 modelValue 改为 value
const props = defineProps({
  value: {
    type: Array,
    default: () => [],
  },
});
// 【核心修改】将 emits 从 update:modelValue 改为 update:value
const emit = defineEmits(['update:value']);
const { value } = toRefs(props);

const fileList = ref([]);

// 【核心修改】监听 value prop 的变化
watch(value, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(fileList.value.map(f => f.response))) {
    fileList.value = (newVal || []).map(file => ({
      uid: file.id.toString(),
      name: file.originalFilename,
      status: 'done',
      response: file,
      url: `/api/files/${file.id}`,
    }));
  }
}, { immediate: true, deep: true });


const beforeUpload = (file) => {
  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    message.error('文件大小不能超过 10MB!');
  }
  return isLt10M;
};

const handleUpload = async ({ file, onSuccess, onError }) => {
  try {
    const res = await uploadFile(file);
    onSuccess(res, file);
    message.success(`${file.name} 上传成功`);

    // 【核心修改】使用正确的 prop (value) 和 emit (update:value)
    const newAttachments = [...(value.value || []), res];
    emit('update:value', newAttachments);

  } catch (error) {
    onError(error);
    message.error(`${file.name} 上传失败`);
  }
};

const handleRemove = (file) => {
  const fileIdToRemove = file.response?.id;
  if (fileIdToRemove) {
    // 【核心修改】使用正确的 prop (value) 和 emit (update:value)
    const currentAttachments = value.value || [];
    const newAttachments = currentAttachments.filter(att => att.id !== fileIdToRemove);
    emit('update:value', newAttachments);
  }
  return true;
};
</script>