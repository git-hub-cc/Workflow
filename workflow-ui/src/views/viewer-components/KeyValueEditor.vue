<template>
  <div class="key-value-editor">
    <div v-for="(item, index) in localValue" :key="index" class="kv-row">
      <!-- Key 输入框 -->
      <a-form-item
          :name="[fieldId, index, 'key']"
          :rules="[{ required: true, message: 'Key 不能为空' }]"
          class="kv-form-item"
      >
        <a-input v-model:value="item.key" :placeholder="keyPlaceholder" />
      </a-form-item>

      <!-- Value 输入框 -->
      <a-form-item
          :name="[fieldId, index, 'value']"
          :rules="[{ required: true, message: 'Value 不能为空' }]"
          class="kv-form-item"
      >
        <a-input v-model:value="item.value" :placeholder="valuePlaceholder" />
      </a-form-item>

      <!-- 删除按钮 -->
      <a-button type="text" danger @click="handleDelete(index)">
        <DeleteOutlined />
      </a-button>
    </div>
    <a-button type="dashed" block @click="handleAdd">
      <PlusOutlined /> 添加一项
    </a-button>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue';

const props = defineProps({
  value: { type: Array, default: () => [] },
  fieldId: { type: String, required: true },
  keyPlaceholder: { type: String, default: 'Key' },
  valuePlaceholder: { type: String, default: 'Value' },
});
const emit = defineEmits(['update:value']);

// 使用 v-model 语法糖
const localValue = computed({
  get: () => props.value || [],
  set: (val) => emit('update:value', val),
});

const handleAdd = () => {
  localValue.value = [...localValue.value, { key: '', value: '' }];
};

const handleDelete = (index) => {
  const newValue = [...localValue.value];
  newValue.splice(index, 1);
  localValue.value = newValue;
};
</script>

<style scoped>
.key-value-editor {
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 16px;
}
.kv-row {
  display: flex;
  align-items: flex-start; /* 使得删除按钮和输入框顶部对齐 */
  gap: 8px;
  margin-bottom: 16px;
}
.kv-row:last-of-type {
  margin-bottom: 8px; /* 调整最后一行和添加按钮的间距 */
}
.kv-form-item {
  flex: 1;
  margin-bottom: 0 !important; /* 覆盖默认的 antd 表单项边距 */
}
</style>