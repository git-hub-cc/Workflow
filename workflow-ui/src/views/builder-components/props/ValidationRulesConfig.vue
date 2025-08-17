<template>
  <div>
    <a-divider>校验规则</a-divider>

    <div v-for="(rule, index) in localRules" :key="index" class="validation-rule-card">
      <div class="rule-header">
        <strong>规则 {{ index + 1 }}</strong>
        <!-- 不允许删除第一条（必填）规则 -->
        <a-button v-if="index > 0" type="text" danger size="small" @click="removeRule(index)">
          <DeleteOutlined />
        </a-button>
      </div>
      <div class="rule-body">
        <!-- 必填规则 (第一条规则) -->
        <a-checkbox v-if="'required' in rule" v-model:checked="rule.required">必填</a-checkbox>

        <!-- 其他规则类型 -->
        <template v-else>
          <a-select v-model:value="rule.type" placeholder="规则类型" style="width: 120px;">
            <a-select-option value="string">文本</a-select-option>
            <a-select-option value="number">数字</a-select-option>
            <a-select-option value="email">邮箱</a-select-option>
            <a-select-option value="url">网址</a-select-option>
          </a-select>

          <!-- 文本长度 -->
          <template v-if="rule.type === 'string'">
            <a-input-number v-model:value="rule.min" placeholder="最小长度" style="width: 100px;" />
            <a-input-number v-model:value="rule.max" placeholder="最大长度" style="width: 100px;" />
          </template>
        </template>

        <a-input v-model:value="rule.message" placeholder="自定义错误提示" style="margin-top: 8px;" />
      </div>
    </div>

    <a-dropdown>
      <a-button type="dashed" block>
        <PlusOutlined /> 添加校验规则
      </a-button>
      <template #overlay>
        <a-menu @click="handleAddRule">
          <a-menu-item key="string">长度校验</a-menu-item>
          <a-menu-item key="number">数字校验</a-menu-item>
          <a-menu-item key="email">邮箱校验</a-menu-item>
          <a-menu-item key="url">网址校验</a-menu-item>
          <a-menu-item key="pattern">正则校验 (请手动编辑)</a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>

  </div>
</template>

<script setup>
import { computed } from 'vue';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';

const props = defineProps({
  rules: {
    type: Array,
    required: true,
  },
});
const emit = defineEmits(['update:rules']);

const localRules = computed({
  get: () => props.rules,
  set: (newRules) => {
    emit('update:rules', newRules);
  }
});


const handleAddRule = ({ key }) => {
  let newRule = { type: key, message: '' };
  switch (key) {
    case 'string': newRule.message = '文本格式不正确'; break;
    case 'number': newRule.message = '请输入有效的数字'; break;
    case 'email': newRule.message = '请输入有效的邮箱地址'; break;
    case 'url': newRule.message = '请输入有效的网址'; break;
    case 'pattern':
      newRule.pattern = '';
      newRule.message = '不符合正则表达式规则';
      break;
  }
  // 创建一个新数组来触发更新，而不是直接push
  localRules.value = [...localRules.value, newRule];
};

const removeRule = (index) => {
  if (index > 0) {
    const newRules = [...localRules.value];
    newRules.splice(index, 1);
    localRules.value = newRules;
  }
};
</script>

<style scoped>
.validation-rule-card {
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 8px;
  margin-bottom: 12px;
}
.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.rule-body {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>