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

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
});
const emit = defineEmits(['update:modelValue']);
const { modelValue } = toRefs(props);

const fileList = ref([]);

// 将 modelValue (父组件的附件列表) 同步到内部的 fileList
watch(modelValue, (newVal) => {
  if (newVal) {
    fileList.value = newVal.map(file => ({
      uid: file.id.toString(),
      name: file.originalFilename,
      status: 'done',
      response: file, // 存储完整的文件信息
      url: `/api/files/${file.id}`, // 提供下载链接
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

    // 更新父组件的值
    const newAttachments = [...modelValue.value, res];
    emit('update:modelValue', newAttachments);

  } catch (error) {
    onError(error);
    message.error(`${file.name} 上传失败`);
  }
};

const handleRemove = (file) => {
  const fileIdToRemove = file.response?.id;
  if (fileIdToRemove) {
    const newAttachments = modelValue.value.filter(att => att.id !== fileIdToRemove);
    emit('update:modelValue', newAttachments);
  }
  return true; // 允许移除
};
</script>