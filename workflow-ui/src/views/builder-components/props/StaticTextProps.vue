<template>
  <div>
    <a-form-item label="组件类型">
      <a-tag color="orange">静态文本 (StaticText)</a-tag>
    </a-form-item>

    <a-form-item label="组件ID">
      <a-input :value="field.id" disabled />
    </a-form-item>

    <a-form-item label="HTML 标签类型">
      <a-radio-group v-model:value="field.props.tag" button-style="solid" size="small">
        <a-radio-button value="h1">H1</a-radio-button>
        <a-radio-button value="h2">H2</a-radio-button>
        <a-radio-button value="h3">H3</a-radio-button>
        <a-radio-button value="p">段落</a-radio-button>
      </a-radio-group>
    </a-form-item>

    <a-form-item label="文本内容 (支持HTML)">
      <a-textarea v-model:value="field.props.content" :rows="6" />
    </a-form-item>

  </div>
</template>

<script setup>
defineProps(['field']);
</script>