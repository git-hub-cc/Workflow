<template>
  <div class="field-injection-container">
    <a-divider>字段注入</a-divider>
    <div v-for="(field, index) in localFields" :key="index" class="field-row">
      <a-select v-model:value="field.type" style="width: 120px;">
        <a-select-option value="string">字符串</a-select-option>
        <a-select-option value="expression">表达式</a-select-option>
      </a-select>
      <a-input v-model:value="field.name" placeholder="字段名 (Name)" />
      <a-input v-model:value="field.value" placeholder="值 (Value)" />
      <a-button type="text" danger @click="removeField(index)">
        <DeleteOutlined />
      </a-button>
    </div>
    <a-button type="dashed" block @click="addField">
      <PlusOutlined /> 添加字段
    </a-button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';

const props = defineProps({
  fields: {
    type: Array,
    default: () => [],
  },
});
const emit = defineEmits(['update:fields']);

const localFields = ref([]);

// 解析从BPMN模型传入的字段
const parseFields = () => {
  if (props.fields && props.fields.length > 0) {
    localFields.value = props.fields.map(field => {
      let type = 'string';
      let value = '';
      if (field.string) {
        type = 'string';
        value = field.string;
      } else if (field.expression) {
        type = 'expression';
        value = field.expression;
      }
      return { name: field.name, type, value };
    });
  } else {
    localFields.value = [];
  }
};

watch(() => props.fields, parseFields, { immediate: true, deep: true });

const addField = () => {
  localFields.value.push({ name: '', type: 'string', value: '' });
  updateParent();
};

const removeField = (index) => {
  localFields.value.splice(index, 1);
  updateParent();
};

// 当本地字段列表变化时，转换为BPMN格式并通知父组件
const updateParent = () => {
  const bpmnFields = localFields.value.map(field => {
    const bpmnField = { name: field.name };
    if (field.type === 'string') {
      bpmnField.string = field.value;
    } else {
      bpmnField.expression = field.value;
    }
    return bpmnField;
  });
  emit('update:fields', bpmnFields);
};

// 监听 localFields 的变化以触发更新
watch(localFields, updateParent, { deep: true });

</script>

<style scoped>
.field-injection-container {
  margin-top: 16px;
}
.field-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>