<template>
  <div>
    <a-divider>数据源</a-divider>

    <!-- 数据源类型选择 -->
    <a-form-item label="数据源类型">
      <a-radio-group v-model:value="localField.dataSource.type" button-style="solid" size="small">
        <a-radio-button value="static">静态数据</a-radio-button>
        <a-radio-button value="api">API (列表)</a-radio-button>
        <!-- 【新增】树形API选项 -->
        <a-radio-button v-if="localField.type === 'TreeSelect'" value="api-tree">API (树形)</a-radio-button>
        <a-radio-button v-if="localField.type === 'UserPicker'" value="system-users" disabled>系统用户</a-radio-button>
      </a-radio-group>
    </a-form-item>

    <!-- 静态数据配置 -->
    <div v-if="localField.dataSource.type === 'static'">
      <!-- 【修改】为树形选择器增加静态树数据配置 -->
      <template v-if="localField.type === 'TreeSelect'">
        <a-textarea v-model:value="staticTreeData" :rows="6" placeholder="请输入JSON格式的树形数据" @change="parseStaticTreeData" />
        <p style="font-size: 12px; color: #888; margin-top: 4px;">
          格式: <code>[{"title":"a", "value":"a", "children": [...]}]</code>
        </p>
      </template>
      <template v-else>
        <div v-for="(option, index) in localField.dataSource.options" :key="index" class="static-option-row">
          <a-input v-model:value="option.label" placeholder="显示文本 (Label)" />
          <a-input v-model:value="option.value" placeholder="实际值 (Value)" />
          <a-button type="text" danger @click="removeStaticOption(index)">
            <DeleteOutlined />
          </a-button>
        </div>
        <a-button type="dashed" block @click="addStaticOption">
          <PlusOutlined /> 添加选项
        </a-button>
      </template>
    </div>

    <!-- API列表接口配置 -->
    <div v-if="localField.dataSource.type === 'api'">
      <a-form-item label="接口URL">
        <a-input v-model:value="localField.dataSource.url" placeholder="/api/your/data/endpoint" />
      </a-form-item>
      <a-form-item label="值字段 (Value Key)">
        <a-input v-model:value="localField.dataSource.valueKey" placeholder="例如: id" />
      </a-form-item>
      <a-form-item label="文本字段 (Label Key)">
        <a-input v-model:value="localField.dataSource.labelKey" placeholder="例如: name" />
      </a-form-item>
    </div>

    <!-- 【新增】API树形接口配置 -->
    <div v-if="localField.dataSource.type === 'api-tree'">
      <a-form-item label="数据源标识 (Source)">
        <a-input v-model:value="localField.dataSource.source" placeholder="例如: departments" />
        <p style="font-size: 12px; color: #888; margin-top: 4px;">
          此标识将作为参数 <code>?source=departments</code> 传递给后端通用树形数据接口。
        </p>
      </a-form-item>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { message } from "ant-design-vue";

const props = defineProps({
  field: { type: Object, required: true },
});
const emit = defineEmits(['update:field']);

const localField = computed({
  get: () => props.field,
  set: (newField) => emit('update:field', newField),
});

// For TreeSelect static data
const staticTreeData = ref('');
watch(() => localField.value.dataSource, (newDs) => {
  if (newDs && newDs.type === 'static' && localField.value.type === 'TreeSelect') {
    staticTreeData.value = JSON.stringify(newDs.options || [], null, 2);
  }
}, { immediate: true, deep: true });

const parseStaticTreeData = () => {
  try {
    localField.value.dataSource.options = JSON.parse(staticTreeData.value);
  } catch (e) {
    message.error('JSON格式不正确', 2);
  }
};


const addStaticOption = () => {
  if (!localField.value.dataSource.options) {
    localField.value.dataSource.options = [];
  }
  const newIndex = localField.value.dataSource.options.length + 1;
  localField.value.dataSource.options.push({ label: `选项${newIndex}`, value: `选项${newIndex}` });
};

const removeStaticOption = (index) => {
  localField.value.dataSource.options.splice(index, 1);
};
</script>

<style scoped>
.static-option-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>