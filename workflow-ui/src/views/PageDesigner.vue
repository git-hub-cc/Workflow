<template>
  <div class="page-designer-container">
    <div class="designer-header">
      <div class="header-left">
        <a-page-header
            :title="isEditing ? '编辑页面' : '新建页面'"
            :ghost="false"
            @back="() => $router.push({ name: 'admin-pages' })"
        />
      </div>
      <div class="header-center">
        <a-input v-model:value="pageName" placeholder="页面名称" style="width: 200px; margin-right: 16px;" />
        <a-input v-model:value="pageKey" placeholder="页面路径 (Key)" style="width: 200px;" />
      </div>
      <div class="header-right">
        <a-space>
          <a-button @click="previewInNewTab">预览</a-button>
          <a-button type="primary" :loading="saving" @click="handleSave">保存</a-button>
        </a-space>
      </div>
    </div>
    <div ref="grapesjsEditor" class="editor-main"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import grapesjs from 'grapesjs';
import 'grapesjs/dist/css/grapes.min.css';
// 【权限修复】从 api/index.js 导入 API 函数
import { getComponentMetas, getPageSchemaById, createPageSchema, updatePageSchema } from '@/api';

const props = defineProps({
  schemaId: [String, Number],
});

const route = useRoute();
const router = useRouter();

const grapesjsEditor = ref(null);
let editor = null;
const saving = ref(false);

const pageName = ref('');
const pageKey = ref('');
const schemaJson = ref(null);

const isEditing = computed(() => !!props.schemaId);

// 初始化 GrapesJS 编辑器
const initializeEditor = async () => {
  if (!grapesjsEditor.value) return;

  // 1. 获取可用组件元数据
  // 【权限修复】使用导入的 API 函数
  const componentMetas = await getComponentMetas();

  editor = grapesjs.init({
    container: grapesjsEditor.value,
    fromElement: true,
    height: '100%',
    width: 'auto',
    storageManager: false, // 我们自己通过API管理存储
    plugins: [],
    // 配置 Block Manager (左侧组件面板)
    blockManager: {
      appendTo: '#blocks', // 这是一个虚拟ID，我们将在下面动态添加
      blocks: componentMetas.map(meta => ({
        id: meta.type,
        label: meta.name,
        category: meta.category,
        content: `<div data-gjs-type="${meta.type}"></div>`, // 使用自定义组件类型
      })),
    },
  });

  // 2. 将元数据中的组件注册为 GrapesJS 组件
  componentMetas.forEach(meta => {
    editor.DomComponents.addType(meta.type, {
      model: {
        defaults: {
          // 将元数据中的默认属性设置到GrapesJS组件模型中
          attributes: meta.defaultProps,
          // 定义哪些属性是可以在属性面板中编辑的
          traits: Object.keys(meta.defaultProps).map(propKey => ({
            name: propKey,
            label: propKey, // 简单起见，label和name相同
          })),
        }
      },
      // 视图定义了组件在画布中的外观
      view: {
        // 简单显示一个占位符
        onRender({ el }) {
          el.innerHTML = `
                <div style="padding: 20px; border: 1px dashed #ccc; text-align: center; background: #f9f9f9;">
                    <strong>${meta.name}</strong>
                    <div style="font-size: 12px; color: #888;">组件占位符</div>
                </div>
            `;
        }
      }
    });
  });

  // 如果是编辑模式，加载现有数据
  if (isEditing.value) {
    try {
      // 【权限修复】使用导入的 API 函数
      const data = await getPageSchemaById(props.schemaId);
      pageName.value = data.name;
      pageKey.value = data.pageKey;
      schemaJson.value = JSON.parse(data.schemaJson);
      // GrapesJS 提供了多种加载方式，这里使用 setComponents
      if (schemaJson.value.components) {
        editor.setComponents(schemaJson.value.components);
      }
    } catch (error) {
      message.error('加载页面数据失败！');
      router.push({ name: 'admin-pages' });
    }
  }
};

const handleSave = async () => {
  if (!pageName.value || !pageKey.value) {
    message.warn('页面名称和路径不能为空！');
    return;
  }
  saving.value = true;
  try {
    // 获取 GrapesJS 项目数据
    const projectData = {
      // GrapesJS 默认的数据结构包含 assets, styles, pages, ...
      // 我们简化为只保存核心的 components 结构
      components: editor.getComponents().map(c => c.toJSON()),
    };

    const payload = {
      name: pageName.value,
      pageKey: pageKey.value,
      schemaJson: JSON.stringify(projectData),
    };

    if (isEditing.value) {
      // 【权限修复】使用导入的 API 函数
      await updatePageSchema(props.schemaId, payload);
    } else {
      // 【权限修复】使用导入的 API 函数
      await createPageSchema(payload);
    }
    message.success('保存成功！');
    router.push({ name: 'admin-pages' });
  } catch (error) {
    // 错误已由全局拦截器处理
  } finally {
    saving.value = false;
  }
};

const previewInNewTab = () => {
  if (!pageKey.value) {
    message.warn('请先设置页面路径(Key)才能预览');
    return;
  }
  // 假设渲染端运行在 3000 端口
  const previewUrl = `http://localhost:3000/${pageKey.value}`;
  window.open(previewUrl, '_blank');
}

onMounted(() => {
  initializeEditor();
});

onBeforeUnmount(() => {
  if (editor) {
    editor.destroy();
    editor = null;
  }
});
</script>

<style scoped>
.page-designer-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px); /* 减去顶部导航栏高度123 */
  overflow: hidden;
}

.designer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fff;
  flex-shrink: 0;
}

.header-left {
  flex: 1;
}
.header-left :deep(.ant-page-header) {
  padding: 8px 24px;
}

.header-center {
  flex: 2;
  text-align: center;
}

.header-right {
  flex: 1;
  text-align: right;
  padding-right: 24px;
}

.editor-main {
  flex-grow: 1;
  min-height: 0;
}

/* GrapesJS 默认UI的一些样式覆盖 */
:deep(.gjs-cv-canvas) {
  background-color: #fff;
  top: 0;
  width: 100%;
  height: 100%;
}
</style>