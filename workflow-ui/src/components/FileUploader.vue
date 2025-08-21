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
// 【核心修改】引入 PlusOutlined
import { UploadOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { uploadFile } from '@/api';

const props = defineProps({
  value: {
    type: Array,
    default: () => [],
  },
  // 【核心新增】接收 listType prop
  listType: {
    type: String,
    default: 'text', // 默认为文本列表
  },
  // 【核心新增】接收 field prop 以获取 maxCount 等配置
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
      // 【核心修改】为已有文件生成预览URL，这是显示缩略图的关键
      url: `/api/files/${file.id}`,
      thumbUrl: `/api/files/${file.id}`, // thumbUrl 同样重要
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