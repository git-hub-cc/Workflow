<template>
  <div>
    <!-- Generic Properties -->
    <GenericProps :field="field" :all-fields="allFields" />

    <a-divider>富文本工具栏配置</a-divider>

    <a-form-item label="基础样式">
      <a-checkbox-group v-model:value="field.props.toolbarOptions.basic">
        <a-checkbox value="bold">加粗</a-checkbox>
        <a-checkbox value="italic">斜体</a-checkbox>
        <a-checkbox value="underline">下划线</a-checkbox>
        <a-checkbox value="strike">删除线</a-checkbox>
      </a-checkbox-group>
    </a-form-item>

    <a-form-item label="标题与引用">
      <a-checkbox-group v-model:value="field.props.toolbarOptions.header">
        <a-checkbox value="header">标题</a-checkbox>
        <a-checkbox value="blockquote">引用</a-checkbox>
        <a-checkbox value="code-block">代码块</a-checkbox>
      </a-checkbox-group>
    </a-form-item>

    <a-form-item label="列表">
      <a-checkbox-group v-model:value="field.props.toolbarOptions.list">
        <a-checkbox value="list-ordered">有序列表</a-checkbox>
        <a-checkbox value="list-bullet">无序列表</a-checkbox>
      </a-checkbox-group>
    </a-form-item>

    <a-form-item label="其他">
      <a-checkbox-group v-model:value="field.props.toolbarOptions.extra">
        <a-checkbox value="link">链接</a-checkbox>
        <a-checkbox value="image">图片</a-checkbox>
        <a-checkbox value="clean">清除格式</a-checkbox>
      </a-checkbox-group>
    </a-form-item>

  </div>
</template>

<script setup>
import GenericProps from './GenericProps.vue';

const props = defineProps(['field', 'allFields']);

/**
 * 【加固】
 * 确保 toolbarOptions 对象及其所有子数组都存在，防止 v-model 绑定到 undefined。
 * 这个初始化逻辑对于新拖入的组件至关重要，并为它们提供了合理的默认值。
 */
const initToolbarOptions = () => {
  if (!props.field.props.toolbarOptions) {
    props.field.props.toolbarOptions = {};
  }
  const options = props.field.props.toolbarOptions;

  // 为每个分组设置默认值或确保其为数组
  if (!Array.isArray(options.basic)) {
    options.basic = ['bold', 'italic']; // 设置一个合理的默认值
  }
  if (!Array.isArray(options.header)) {
    options.header = [];
  }
  if (!Array.isArray(options.list)) {
    options.list = ['list-ordered', 'list-bullet'];
  }
  if (!Array.isArray(options.extra)) {
    options.extra = ['link'];
  }
};

// 立即执行初始化
initToolbarOptions();
</script>