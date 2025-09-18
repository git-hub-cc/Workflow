
<template>
  <div class="page-container">
    <a-page-header title="流程设计器" @back="() => $router.push({ name: 'admin-forms' })">
      <template #subTitle>
        为表单: <strong class="form-name-highlight">{{ formName }}</strong>
      </template>
      <template #extra>
        <a-space>
          <a-button @click="$router.push({ name: 'admin-forms' })">取消</a-button>
          <a-button @click="handleSaveDraft" :loading="saving">保存草稿</a-button>
          <a-button type="primary" @click="handleDeploy" :loading="deploying">部署</a-button>
        </a-space>
      </template>
    </a-page-header>

    <!-- 工具栏 (桌面端) -->
    <div class="toolbar" v-if="!isMobile">
      <a-space>
        <a-tooltip title="撤销 (Ctrl+Z)">
          <a-button @click="handleUndo" :disabled="!canUndo"><UndoOutlined /></a-button>
        </a-tooltip>
        <a-tooltip title="重做 (Ctrl+Y)">
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
          <a-button @click="triggerImport"><UploadOutlined /> 导入</a-button>
        </a-tooltip>
        <a-tooltip title="下载为 BPMN 文件">
          <a-button @click="downloadBpmn"><DownloadOutlined /> BPMN</a-button>
        </a-tooltip>
        <a-tooltip title="下载为 SVG 图像">
          <a-button @click="downloadSvg"><FileImageOutlined /> SVG</a-button>
        </a-tooltip>
      </a-space>
    </div>

    <!-- 设计器区域 -->
    <div class="designer-container" v-show="!loading">
      <div id="canvas" ref="canvasRef" class="canvas"></div>
      <SimplePropertiesPanel
          v-if="!isMobile"
          class="properties-panel"
          :selected-element="selectedElement"
          :modeler="modeler"
          :form-fields="formFields"
          :user-groups="availableGroups"
          @update="handlePropertiesUpdate"
      />
    </div>
    <div v-if="loading" class="loading-container">
      <a-spin size="large" tip="正在加载设计器..." />
    </div>

    <!-- 【核心新增】移动端悬浮操作按钮 -->
    <div v-if="isMobile" class="mobile-fab-group">
      <a-button type="primary" shape="circle" size="large" @click="propertiesDrawerVisible = true" :disabled="!selectedElement">
        <SettingOutlined />
      </a-button>
    </div>

    <!-- 【核心新增】移动端属性面板抽屉 -->
    <a-drawer
        v-if="isMobile"
        v-model:open="propertiesDrawerVisible"
        title="属性配置"
        placement="bottom"
        :height="'80%'"
    >
      <SimplePropertiesPanel
          :selected-element="selectedElement"
          :modeler="modeler"
          :form-fields="formFields"
          :user-groups="availableGroups"
          @update="handlePropertiesUpdate"
      />
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick, markRaw } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import { getFormById, deployWorkflow, getWorkflowTemplate, updateWorkflowTemplate, getGroupsForWorkflow } from '@/api';
import {
  UndoOutlined, RedoOutlined, ZoomInOutlined, ZoomOutOutlined,
  FullscreenOutlined, DownloadOutlined, FileImageOutlined, UploadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue';
import { flattenFields } from '@/utils/formUtils.js';

import BpmnModeler from 'bpmn-js/lib/Modeler.js';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';
import camundaModdleDescriptors from 'camunda-bpmn-moddle/resources/camunda.json';
import customTranslateModule from '@/utils/customTranslate';
import SimplePropertiesPanel from './workflow/SimplePropertiesPanel.vue';

const route = useRoute();
const router = useRouter();
const formId = route.params.formId;

const canvasRef = ref(null);
const fileInputRef = ref(null);
const formName = ref('加载中...');
const saving = ref(false);
const deploying = ref(false);
const loading = ref(true);
let modeler = null;

const canUndo = ref(false);
const canRedo = ref(false);

const availableGroups = ref([]);
const formFields = ref([]);
const selectedElement = ref(null);

const isMobile = ref(window.innerWidth < 768);
const propertiesDrawerVisible = ref(false);
const handleResize = () => { isMobile.value = window.innerWidth < 768; };
onBeforeUnmount(() => window.removeEventListener('resize', handleResize));

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
      additionalModules: [ customTranslateModule ],
      moddleExtensions: { camunda: camundaModdleDescriptors },
    });

    const eventBus = modeler.get('eventBus');

    eventBus.on('commandStack.changed', () => {
      canUndo.value = modeler.get('commandStack').canUndo();
      canRedo.value = modeler.get('commandStack').canRedo();
    });

    eventBus.on('selection.changed', (event) => {
      const { newSelection } = event;
      selectedElement.value = newSelection && newSelection.length > 0 ? markRaw(newSelection[0]) : null;
      if (isMobile.value && selectedElement.value) {
        propertiesDrawerVisible.value = true;
      }
    });

    eventBus.on('element.changed', (event) => {
      const { element } = event;
      if (selectedElement.value && selectedElement.value.id === element.id) {
        selectedElement.value = markRaw(element);
      }
    });

    const template = await getWorkflowTemplate(formId);
    await modeler.importXML(template.bpmnXml);
    handleFitViewport();
  } catch (error) {
    message.error('初始化设计器失败!');
    console.error('Designer Initialization Error:', error);
  }
}

const handlePropertiesUpdate = (propertiesToUpdate) => {
  if (!selectedElement.value) return;
  const modeling = modeler.get('modeling');
  modeling.updateProperties(selectedElement.value, propertiesToUpdate);
};

onMounted(async () => {
  window.addEventListener('resize', handleResize);
  try {
    const [form, groups] = await Promise.all([
      getFormById(formId),
      getGroupsForWorkflow()
    ]);
    formName.value = form.name;
    availableGroups.value = groups;

    const schema = JSON.parse(form.schemaJson);
    const flattened = flattenFields(schema.fields);
    formFields.value = flattened
        .filter(f => !['GridRow', 'GridCol', 'Collapse', 'CollapsePanel', 'StaticText', 'DescriptionList', 'Divider'].includes(f.type))
        .map(f => ({ id: f.id, label: f.label || f.id, type: f.type }));

  } catch (err) {
    message.error('获取基础信息失败');
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  if (modeler) {
    modeler.destroy();
    modeler = null;
  }
});

async function getModelerContent() {
  if (!modeler) throw new Error('设计器未初始化！');
  const { xml } = await modeler.saveXML({ format: true });
  const processElement = modeler.get('elementRegistry').find(el => el.type === 'bpmn:Process');
  if (!processElement) throw new Error('无法在BPMN图中找到 <bpmn:process> 元素。');
  const processDefinitionKey = processElement.businessObject.id;
  if (!processDefinitionKey) throw new Error('流程 Process ID 不能为空，请在属性面板中设置。');
  return { xml, processDefinitionKey };
}

const handleSaveDraft = async () => {
  saving.value = true;
  try {
    const { xml, processDefinitionKey } = await getModelerContent();
    const payload = { bpmnXml: xml, processDefinitionKey };
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
        await handleSaveDraft();
        const { xml, processDefinitionKey } = await getModelerContent();
        const payload = {
          formDefinitionId: Number(formId),
          bpmnXml: xml,
          processDefinitionKey,
        };
        await deployWorkflow(payload);
        message.success('流程部署成功！');
        router.push({ name: 'admin-forms' });
      } catch (error) {
        message.error(`部署失败: ${error.message}`);
      } finally {
        deploying.value = false;
      }
    },
  });
};

const handleUndo = () => modeler.get('commandStack').undo();
const handleRedo = () => modeler.get('commandStack').redo();
let scale = 1;
const handleZoomIn = () => { scale += 0.1; modeler.get('canvas').zoom(scale); };
const handleZoomOut = () => { if (scale > 0.2) { scale -= 0.1; modeler.get('canvas').zoom(scale); } };
const handleFitViewport = () => { scale = 1; modeler.get('canvas').zoom('fit-viewport'); };

const triggerImport = () => fileInputRef.value?.click();

const handleFileImport = (event) => {
  const file = event.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (e) => {
    let xmlContent = e.target.result;
    const expectedProcessId = `Process_Form_${formId}`;

    const processIdRegex = /(<bpmn:process[^>]*id=")([^"]+)(")/;
    const match = xmlContent.match(processIdRegex);

    if (match && match[2] && match[2] !== expectedProcessId) {
      const oldProcessId = match[2];
      const escapedOldProcessId = oldProcessId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const planeElementRegexForReplace = new RegExp(`(<bpmndi:BPMNPlane[^>]*bpmnElement=")` + escapedOldProcessId + `(")`);

      xmlContent = xmlContent.replace(processIdRegex, `$1${expectedProcessId}$3`);
      xmlContent = xmlContent.replace(planeElementRegexForReplace, `$1${expectedProcessId}$2`);

      message.info(`导入的流程ID "${oldProcessId}" 已自动更新为 "${expectedProcessId}"`, 5);
    }

    modeler.importXML(xmlContent)
        .then(({ warnings }) => {
          if (warnings.length) console.warn('BPMN Import Warnings:', warnings);
          message.success('BPMN 文件导入成功！');
          handleFitViewport();
        })
        .catch(err => {
          console.error('BPMN Import Error:', err);
          message.error(`导入失败: ${err.message || '请检查文件格式。'}`);
        });
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
</script>

<style scoped>
.page-container {
  padding: 0;
  display: flex;
  flex-direction: column;
  height: 100%; /* Fallback for older browsers */
  height: calc(100vh - 64px); /* Assuming header height is 64px */
  background-color: #fff;
  overflow: hidden; /* 【样式完善】防止页面滚动 */
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
  min-height: 0;
}
.canvas {
  flex-grow: 1;
  background-color: #f9f9f9;
}
.properties-panel {
  width: 320px;
  flex-shrink: 0;
  background: #f8f8f8;
  border-left: 1px solid #e0e0e0;
  overflow-y: auto;
}
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}
.form-name-highlight {
  color: var(--ant-primary-color);
}
:deep(.bjs-powered-by) {
  display: none !important;
}
:deep(.djs-palette) {
  top: 20px;
  left: 20px;
}
.mobile-fab-group {
  position: fixed;
  bottom: 80px;
  right: 24px;
  z-index: 100;
}
/* 【样式完善】为FAB按钮添加阴影以增强可见性 */
.mobile-fab-group .ant-btn {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
</style>