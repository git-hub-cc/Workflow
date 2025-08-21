<template>
  <div class="page-container">
    <a-page-header title="流程设计器" @back="() => $router.push('/')">
      <template #subTitle>
        为表单: <strong class="form-name-highlight">{{ formName }}</strong>
      </template>
      <template #extra>
        <a-space>
          <a-button @click="$router.push('/')">取消</a-button>
          <a-button @click="handleSaveDraft" :loading="saving">保存草稿</a-button>
          <a-button type="primary" @click="handleDeploy" :loading="deploying">部署</a-button>
        </a-space>
      </template>
    </a-page-header>

    <!-- 工具栏 -->
    <div class="toolbar">
      <a-space>
        <a-tooltip title="撤销">
          <a-button @click="handleUndo" :disabled="!canUndo"><UndoOutlined /></a-button>
        </a-tooltip>
        <a-tooltip title="重做">
          <a-button @click="handleRedo" :disabled="!canRedo"><RedoOutlined /></a-button>
        </a-tooltip>
        <a-divider type="vertical" />
        <a-tooltip title="放大">
          <a-button @click="handleZoomIn"><ZoomInOutlined /></a-button>
        </a-tooltip>
        <a-tooltip title="缩小">
          <a-button @click="handleZoomOut"><ZoomOutOutlined /></a-button>
        </a-tooltip>
        <a-tooltip title="适应屏幕">
          <a-button @click="handleFitViewport"><FullscreenOutlined /></a-button>
        </a-tooltip>
        <a-divider type="vertical" />
        <input type="file" ref="fileInputRef" @change="handleFileImport" style="display: none" accept=".bpmn, .xml" />
        <a-tooltip title="从本地文件导入">
          <a-button @click="triggerImport"><UploadOutlined /> 导入 BPMN</a-button>
        </a-tooltip>
        <a-tooltip title="下载为 BPMN 文件">
          <a-button @click="downloadBpmn"><DownloadOutlined /> BPMN</a-button>
        </a-tooltip>
        <a-tooltip title="下载为 SVG 图像">
          <a-button @click="downloadSvg"><FileImageOutlined /> SVG</a-button>
        </a-tooltip>
      </a-space>
    </div>

    <!-- 用户组辅助面板 -->
    <a-collapse ghost class="group-helper-collapse">
      <a-collapse-panel key="1" header="可用用户组 (点击复制ID)">
        <div class="group-helper-content">
          <a-input-search
              v-model:value="groupSearchText"
              placeholder="搜索用户组"
              size="small"
              style="margin-bottom: 8px;"
          />
          <a-spin :spinning="groupsLoading">
            <div class="group-list">
              <a-tooltip v-for="group in filteredGroups" :key="group.id" :title="group.description">
                <a-tag @click="copyToClipboard(group.name)" class="group-tag">
                  {{ group.name }}
                </a-tag>
              </a-tooltip>
              <a-empty v-if="filteredGroups.length === 0" :image-style="{ height: '40px' }" description="无匹配" />
            </div>
          </a-spin>
        </div>
      </a-collapse-panel>
    </a-collapse>

    <!-- 设计器区域 -->
    <div class="designer-container" v-show="!loading">
      <div id="canvas" ref="canvasRef"></div>
      <div id="properties-panel-container" ref="propertiesPanelRef"></div>
    </div>
    <div v-if="loading" class="loading-container">
      <a-spin size="large" tip="正在加载设计器..." />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import { getFormById, deployWorkflow, getWorkflowTemplate, updateWorkflowTemplate, getGroupsForWorkflow } from '@/api';
import {
  UndoOutlined, RedoOutlined, ZoomInOutlined, ZoomOutOutlined,
  FullscreenOutlined, DownloadOutlined, FileImageOutlined, UploadOutlined
} from '@ant-design/icons-vue';

// BPMN.js related imports...
import BpmnModeler from 'bpmn-js/lib/Modeler.js';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';
import '@bpmn-io/properties-panel/dist/assets/properties-panel.css';
import {
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule,
  CamundaPlatformPropertiesProviderModule
} from 'bpmn-js-properties-panel';
import camundaModdleDescriptors from 'camunda-bpmn-moddle/resources/camunda.json';
import customTranslateModule from '@/utils/customTranslate';

// --- 路由与状态 ---
const route = useRoute();
const router = useRouter();
const formId = route.params.formId;

const canvasRef = ref(null);
const propertiesPanelRef = ref(null);
const fileInputRef = ref(null);
const formName = ref('加载中...');
const saving = ref(false);
const deploying = ref(false);
const loading = ref(true);
let modeler = null;

const canUndo = ref(false);
const canRedo = ref(false);

// --- 用户组辅助功能状态 ---
const availableGroups = ref([]);
const groupsLoading = ref(false);
const groupSearchText = ref('');

const filteredGroups = computed(() => {
  if (!groupSearchText.value) return availableGroups.value;
  return availableGroups.value.filter(g =>
      g.name.toLowerCase().includes(groupSearchText.value.toLowerCase()) ||
      g.description.toLowerCase().includes(groupSearchText.value.toLowerCase())
  );
});

// 监听 loading，加载完成后再初始化 BPMN
watch(loading, async (val) => {
  if (!val) {
    await nextTick();
    await initModeler();
  }
});

async function initModeler() {
  try {
    modeler = new BpmnModeler({
      container: canvasRef.value,
      propertiesPanel: { parent: propertiesPanelRef.value },
      additionalModules: [
        BpmnPropertiesPanelModule,
        BpmnPropertiesProviderModule,
        CamundaPlatformPropertiesProviderModule,
        customTranslateModule
      ],
      moddleExtensions: { camunda: camundaModdleDescriptors },
    });

    const eventBus = modeler.get('eventBus');
    eventBus.on('commandStack.changed', () => {
      canUndo.value = modeler.get('commandStack').canUndo();
      canRedo.value = modeler.get('commandStack').canRedo();
    });

    const template = await getWorkflowTemplate(formId);
    await modeler.importXML(template.bpmnXml);
    handleFitViewport();
  } catch (error) {
    message.error('初始化设计器失败!');
    console.error('Designer Initialization Error:', error);
  }
}

// --- 页面挂载 ---
onMounted(async () => {
  groupsLoading.value = true;
  try {
    const [form, groups] = await Promise.all([
      getFormById(formId),
      getGroupsForWorkflow()
    ]);
    formName.value = form.name;
    availableGroups.value = groups;
  } catch (err) {
    message.error('获取基础信息失败');
    console.error(err);
  } finally {
    loading.value = false;
    groupsLoading.value = false;
  }
});

// --- 卸载清理 ---
onBeforeUnmount(() => {
  if (modeler) {
    modeler.destroy();
    modeler = null;
  }
});

// --- 辅助函数 ---
async function getModelerContent() {
  if (!modeler) throw new Error('设计器未初始化！');
  const { xml } = await modeler.saveXML({ format: true });
  const processElement = modeler.get('elementRegistry').find(el => el.type === 'bpmn:Process');
  if (!processElement) throw new Error('无法在BPMN图中找到 <bpmn:process> 元素。');
  const processDefinitionKey = processElement.businessObject.id;
  if (!processDefinitionKey) throw new Error('流程 Process ID 不能为空，请在属性面板中设置。');
  return { xml, processDefinitionKey };
}

// --- 核心操作 ---
const handleSaveDraft = async () => {
  saving.value = true;
  try {
    const { xml, processDefinitionKey } = await getModelerContent();
    const payload = {
      bpmnXml: xml,
      processDefinitionKey: processDefinitionKey,
    };
    await updateWorkflowTemplate(formId, payload);
    message.success('流程草稿保存成功！');
  } catch (error) {
    message.error(`保存失败: ${error.message}`);
  } finally {
    saving.value = false;
  }
};

const handleDeploy = () => {
  Modal.confirm({
    title: '确认部署',
    content: '部署后，新的流程实例将按照此版本执行。确定要部署吗？',
    onOk: async () => {
      deploying.value = true;
      try {
        const { xml, processDefinitionKey } = await getModelerContent();
        const payload = {
          formDefinitionId: Number(formId),
          bpmnXml: xml,
          processDefinitionKey: processDefinitionKey,
        };
        await deployWorkflow(payload);
        message.success('流程部署成功！');
        router.push('/');
      } catch (error) {
        message.error(`部署失败: ${error.message}`);
      } finally {
        deploying.value = false;
      }
    },
  });
};

// --- 工具栏方法 ---
const handleUndo = () => modeler.get('commandStack').undo();
const handleRedo = () => modeler.get('commandStack').redo();
let scale = 1;
const handleZoomIn = () => { scale += 0.1; modeler.get('canvas').zoom(scale); };
const handleZoomOut = () => { if (scale > 0.2) { scale -= 0.1; modeler.get('canvas').zoom(scale); } };
const handleFitViewport = () => { scale = 1; modeler.get('canvas').zoom('fit-viewport'); };

// --- 文件导入/导出 ---
const triggerImport = () => fileInputRef.value?.click();

const handleFileImport = (event) => {
  const file = event.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (e) => {
    modeler.importXML(e.target.result)
        .then(() => message.success('BPMN 文件导入成功！'))
        .catch(err => message.error('导入失败，请检查文件格式。'));
  };
  reader.readAsText(file);
  event.target.value = '';
};

function downloadFile(filename, data, type) {
  const blob = new Blob([data], { type });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.download = filename;
  a.href = url;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

const downloadBpmn = async () => {
  try {
    const { xml } = await modeler.saveXML({ format: true });
    downloadFile(`${formName.value}_workflow.bpmn`, xml, 'application/xml');
  } catch (error) {
    message.error('导出BPMN失败');
  }
};

const downloadSvg = async () => {
  try {
    const { svg } = await modeler.saveSVG();
    downloadFile(`${formName.value}_workflow.svg`, svg, 'image/svg+xml');
  } catch (error) {
    message.error('导出SVG失败');
  }
};

// --- 复制到剪贴板 ---
const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text);
    message.success(`用户组ID "${text}" 已复制到剪贴板!`);
  } catch (err) {
    message.error('复制失败!');
  }
};
</script>

<style scoped>
.page-container {
  padding: 0;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
  background-color: #fff;
}
.toolbar {
  padding: 8px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  flex-shrink: 0;
}
.designer-container {
  display: flex;
  flex-grow: 1;
  border-top: 1px solid #f0f0f0;
  min-height: 0;
}
#canvas { flex-grow: 1; background-color: #f9f9f9; }
#properties-panel-container {
  width: 320px;
  background: #f8f8f8;
  overflow-y: auto;
  border-left: 1px solid #e0e0e0;
  flex-shrink: 0;
}
.loading-container { display: flex; justify-content: center; align-items: center; height: 100%; }
:deep(.bjs-powered-by) { display: none !important; }
:deep(.djs-palette) { top: 20px; left: 20px; }

.group-helper-collapse {
  border-bottom: 1px solid #f0f0f0;
}
.group-helper-content {
  padding: 0 16px;
}
.group-list {
  max-height: 120px;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.group-tag {
  cursor: pointer;
  transition: all 0.2s;
}
.group-tag:hover {
  transform: translateY(-2px);
  /* 【核心修改】使用 CSS 变量来应用主题色 */
  color: var(--ant-primary-color);
  border-color: var(--ant-primary-color);
}
.form-name-highlight {
  /* 【核心修改】使用 CSS 变量来应用主题色 */
  color: var(--ant-primary-color);
}
</style>