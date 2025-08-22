<template>
  <div>
    <a-form-item label="组件类型">
      <a-tag color="volcano">分割线 (Divider)</a-tag>
    </a-form-item>
    <a-form-item label="分割线文本">
      <a-input v-model:value="field.props.text" />
    </a-form-item>
    <a-form-item label="文本位置">
      <a-radio-group v-model:value="field.props.orientation" button-style="solid" size="small">
        <a-radio-button value="left">左</a-radio-button>
        <a-radio-button value="center">中</a-radio-button>
        <a-radio-button value="right">右</a-radio-button>
      </a-radio-group>
    </a-form-item>
    <a-form-item label="是否为虚线">
      <a-switch v-model:checked="field.props.dashed" />
    </a-form-item>
  </div>
</template>

<script setup>
defineProps(['field']);
</script>