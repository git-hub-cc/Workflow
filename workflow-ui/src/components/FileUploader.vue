<template>
  <div>
    <a-upload
        v-model:file-list="fileList"
        :list-type="listType"
        :custom-request="handleUpload"
        :before-upload="beforeUpload"
        @remove="handleRemove"
        :multiple="true"
    >
      <!-- 根据列表类型显示不同的上传触发器 -->
      <template v-if="listType === 'picture-card'">
        <div v-if="fileList.length < (field.props.maxCount || 99)">
          <plus-outlined />
          <div style="margin-top: 8px">上传</div>
        </div>
      </template>
      <template v-else>
        <a-button>
          <upload-outlined />
          点击上传
        </a-button>
      </template>
    </a-upload>
  </div>
</template>

<script setup>
import { ref, watch, toRefs } from 'vue';
import { message } from 'ant-design-vue';
import { UploadOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { uploadFile } from '@/api';

const props = defineProps({
  value: {
    type: Array,
    default: () => [],
  },
  listType: {
    type: String,
    default: 'text',
  },
  field: {
    type: Object,
    default: () => ({ props: {} }),
  }
});
const emit = defineEmits(['update:value']);
const { value } = toRefs(props);

const fileList = ref([]);

watch(value, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(fileList.value.map(f => f.response))) {
    fileList.value = (newVal || []).map(file => ({
      uid: file.id.toString(),
      name: file.originalFilename,
      status: 'done',
      response: file,
      url: `/api/files/${file.id}`,
      thumbUrl: `/api/files/${file.id}`,
    }));
  }
}, { immediate: true, deep: true });


const beforeUpload = (file) => {
  // 【核心修改】增加文件大小和类型校验
  const { maxSize, allowedTypes } = props.field.props;

  // 1. 校验文件大小
  if (maxSize) {
    const isLtSize = file.size / 1024 / 1024 < maxSize;
    if (!isLtSize) {
      message.error(`文件大小不能超过 ${maxSize}MB!`);
      return false;
    }
  }

  // 2. 校验文件类型
  if (allowedTypes) {
    const typesArray = allowedTypes.split(',').map(t => t.trim().toLowerCase());
    const fileExtension = `.${file.name.split('.').pop()?.toLowerCase()}`;
    const mimeType = file.type.toLowerCase();

    const isValidType = typesArray.some(type => {
      if (type.startsWith('.')) { // e.g., .jpg
        return fileExtension === type;
      }
      if (type.includes('/')) { // e.g., image/jpeg
        return mimeType === type;
      }
      return false;
    });

    if (!isValidType) {
      message.error(`不支持的文件类型。只允许上传: ${allowedTypes}`);
      return false;
    }
  }

  return true;
};

const handleUpload = async ({ file, onSuccess, onError }) => {
  try {
    const res = await uploadFile(file);
    onSuccess(res, file);
    message.success(`${file.name} 上传成功`);

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
    const currentAttachments = value.value || [];
    const newAttachments = currentAttachments.filter(att => att.id !== fileIdToRemove);
    emit('update:value', newAttachments);
  }
  return true;
};
</script>