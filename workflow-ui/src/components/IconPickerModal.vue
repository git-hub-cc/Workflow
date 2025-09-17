<template>
  <a-modal
      :open="open"
      title="选择图标"
      :width="isMobile ? '95%' : '800px'"
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { iconList } from '@/utils/iconLibrary.js';

const props = defineProps({ open: Boolean });
const emit = defineEmits(['update:open', 'select']);

const searchQuery = ref('');

// --- 【核心新增】响应式断点逻辑 ---
const isMobile = ref(window.innerWidth < 768);
const handleResize = () => { isMobile.value = window.innerWidth < 768; };
onMounted(() => { window.addEventListener('resize', handleResize); });
onBeforeUnmount(() => { window.removeEventListener('resize', handleResize); });
// --- 响应式逻辑结束 ---

const filteredIconList = computed(() => {
  if (!searchQuery.value) {
    return iconList;
  }
  const query = searchQuery.value.toLowerCase();
  return iconList.filter(icon => icon.name.toLowerCase().includes(query));
});

const selectIcon = (iconName) => {
  emit('select', iconName);
  emit('update:open', false);
};

const filterIcons = () => {
  // computed property handles filtering
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