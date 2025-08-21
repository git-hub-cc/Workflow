<template>
  <a-modal
      :open="open"
      title="选择图标"
      width="800px"
      @update:open="(val) => emit('update:open', val)"
      :footer="null"
  >
    <div class="icon-picker-body">
      <a-input-search
          v-model:value="searchQuery"
          placeholder="搜索图标 (例如: user, setting)"
          @search="filterIcons"
          style="margin-bottom: 16px;"
      />
      <div class="icon-grid">
        <div
            v-for="icon in filteredIconList"
            :key="icon.name"
            class="icon-item"
            @click="selectIcon(icon.name)"
        >
          <component :is="icon.component" style="font-size: 24px;" />
          <span class="icon-name">{{ icon.name }}</span>
        </div>
      </div>
      <a-empty v-if="filteredIconList.length === 0" description="未找到匹配的图标" />
    </div>
  </a-modal>
</template>

<script setup>
import { ref, computed } from 'vue';
// 【修复】从新的图标库文件导入精选的图标列表
import { iconList } from '@/utils/iconLibrary.js';

const props = defineProps({ open: Boolean });
const emit = defineEmits(['update:open', 'select']);

const searchQuery = ref('');

const filteredIconList = computed(() => {
  if (!searchQuery.value) {
    // 直接使用导入的列表
    return iconList;
  }
  const query = searchQuery.value.toLowerCase();
  // 在精选列表中进行过滤
  return iconList.filter(icon => icon.name.toLowerCase().includes(query));
});

const selectIcon = (iconName) => {
  emit('select', iconName);
  emit('update:open', false);
};

const filterIcons = () => {
  // computed 属性会自动更新，此方法仅用于触发搜索
};
</script>

<style scoped>
.icon-picker-body {
  max-height: 60vh;
  overflow-y: auto;
}
.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 16px;
}
.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.icon-item:hover {
  /* 【核心修改】使用 CSS 变量来应用主题色 */
  border-color: var(--ant-primary-color);
  color: var(--ant-primary-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}
.icon-name {
  margin-top: 8px;
  font-size: 12px;
  text-align: center;
  word-break: break-all;
}
</style>