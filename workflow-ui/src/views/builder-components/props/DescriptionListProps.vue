<template>
  <div>
    <!-- Generic Properties (only label and visibility) -->
    <a-form-item label="组件ID">
      <a-input :value="field.id" disabled />
    </a-form-item>
    <a-form-item label="标题 (Label)">
      <a-input v-model:value="field.label" />
    </a-form-item>

    <a-divider>描述列表配置</a-divider>

    <!-- Style Properties -->
    <a-form-item label="列数 (Columns)">
      <a-slider v-model:value="field.props.column" :min="1" :max="4" />
    </a-form-item>
    <a-form-item label="尺寸 (Size)">
      <a-radio-group v-model:value="field.props.size" button-style="solid" size="small">
        <a-radio-button value="default">默认</a-radio-button>
        <a-radio-button value="middle">中</a-radio-button>
        <a-radio-button value="small">小</a-radio-button>
      </a-radio-group>
    </a-form-item>
    <a-form-item label="是否带边框">
      <a-switch v-model:checked="field.props.bordered" />
    </a-form-item>

    <a-divider>描述项</a-divider>
    <div v-for="(item, index) in field.props.items" :key="index" class="config-row">
      <a-input v-model:value="item.label" placeholder="标签" />
      <a-select v-model:value="item.fieldId" placeholder="关联字段">
        <a-select-option v-for="f in availableTargetFields" :key="f.id" :value="f.id">
          {{ f.label }} ({{ f.id }})
        </a-select-option>
      </a-select>
      <a-button type="text" danger @click="removeItem(index)">
        <DeleteOutlined />
      </a-button>
    </div>
    <a-button type="dashed" block @click="addItem">
      <PlusOutlined /> 添加描述项
    </a-button>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { flattenFields } from '@/views/viewer-components/formUtils';

const props = defineProps(['field', 'allFields']);

const availableTargetFields = computed(() => {
  // 描述列表不能关联自身或布局组件
  return flattenFields(props.allFields).filter(f =>
      f.id !== props.field.id && !['GridRow', 'GridCol', 'Collapse', 'CollapsePanel', 'DescriptionList'].includes(f.type)
  );
});

// Items management
const addItem = () => {
  if (!props.field.props.items) props.field.props.items = [];
  props.field.props.items.push({ label: '新标签', fieldId: '' });
};
const removeItem = (index) => {
  props.field.props.items.splice(index, 1);
};
</script>

<style scoped>
.config-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>