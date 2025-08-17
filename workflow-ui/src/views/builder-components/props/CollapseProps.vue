<template>
  <div>
    <a-form-item label="组件类型">
      <a-tag color="geekblue">折叠面板容器 (Collapse)</a-tag>
    </a-form-item>
    <a-form-item label="手风琴模式">
      <a-switch v-model:checked="field.props.accordion" />
    </a-form-item>

    <a-divider>面板管理</a-divider>
    <div v-for="(panel, index) in field.panels" :key="panel.id" class="panel-config-row">
      <a-input v-model:value="panel.props.header" placeholder="面板标题" />
      <a-button type="text" danger @click="removePanel(index)">
        <DeleteOutlined />
      </a-button>
    </div>
    <a-button type="dashed" block @click="addPanel">
      <PlusOutlined /> 添加面板
    </a-button>
  </div>
</template>

<script setup>
import { v4 as uuidv4 } from 'uuid';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
const props = defineProps(['field']);

const addPanel = () => {
  if (!props.field.panels) {
    props.field.panels = [];
  }
  props.field.panels.push({
    id: `panel_${uuidv4().substring(0, 4)}`,
    type: 'CollapsePanel',
    props: { header: `新面板${props.field.panels.length + 1}` },
    fields: []
  });
};

const removePanel = (index) => {
  // 至少保留一个面板
  if (props.field.panels.length > 1) {
    props.field.panels.splice(index, 1);
  }
};
</script>

<style scoped>
.panel-config-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>