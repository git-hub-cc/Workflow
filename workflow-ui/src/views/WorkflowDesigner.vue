<template>
  <div class="page-container">
    <a-page-header title="流程设计器" @back="() => $router.push('/')">
      <template #subTitle>
        为表单: <strong style="color: #1890ff">{{ formName }}</strong>
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
        <!-- 【新增】导入功能 -->
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
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import { getFormById, deployWorkflow, getWorkflowTemplate, updateWorkflowTemplate } from '@/api';
import {
  UndoOutlined, RedoOutlined, ZoomInOutlined, ZoomOutOutlined,
  FullscreenOutlined, DownloadOutlined, FileImageOutlined, UploadOutlined
} from '@ant-design/icons-vue';

// --- BPMN.js 核心 ---
import BpmnModeler from 'bpmn-js/lib/Modeler.js';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';
import '@bpmn-io/properties-panel/dist/assets/properties-panel.css';

// --- 属性面板相关 ---
import {
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule,
  CamundaPlatformPropertiesProviderModule
} from 'bpmn-js-properties-panel';
import camundaModdleDescriptors from 'camunda-bpmn-moddle/resources/camunda.json';

// 汉化
import customTranslateModule from '@/utils/customTranslate';

// --- 路由与状态 ---
const route = useRoute();
const router = useRouter();
const formId = route.params.formId;

const canvasRef = ref(null);
const propertiesPanelRef = ref(null);
const fileInputRef = ref(null); // 【新增】文件输入引用
const formName = ref('加载中...');
const saving = ref(false);
const deploying = ref(false); // 【修改】分离状态
const loading = ref(true);
let modeler = null;

const canUndo = ref(false);
const canRedo = ref(false);

// 监听 loading，加载完成后再初始化 BPMN
watch(loading, async (val) => {
  if (!val) {
    await nextTick(); // 确保 DOM 尺寸已计算
    await initModeler();
  }
});

async function initModeler() {
  try {
    modeler = new BpmnModeler({
      container: canvasRef.value,
      propertiesPanel: {
        parent: propertiesPanelRef.value,
      },
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
    message.error('初始化设计器失败! 请检查控制台获取详细信息。');
    console.error('Designer Initialization Error:', error);
  }
}

// --- 页面挂载 ---
onMounted(async () => {
  try {
    const form = await getFormById(formId);
    formName.value = form.name;
  } catch (err) {
    message.error('获取表单信息失败');
    console.error(err);
  } finally {
    loading.value = false;
  }
});

// --- 卸载清理 ---
onBeforeUnmount(() => {
  if (modeler) {
    modeler.destroy();
    modeler = null;
  }
});


// --- 【私有辅助函数】 ---
async function getModelerContent() {
  if (!modeler) throw new Error('设计器未初始化！');

  const { xml } = await modeler.saveXML({ format: true });
  const elementRegistry = modeler.get('elementRegistry');
  const processElement = elementRegistry.find(el => el.type === 'bpmn:Process');
  if (!processElement) throw new Error('无法在BPMN图中找到 <bpmn:process> 元素。');

  const processDefinitionKey = processElement.businessObject.id;
  if (!processDefinitionKey) throw new Error('流程 Process ID 不能为空，请在属性面板中设置。');

  return { xml, processDefinitionKey };
}


// --- 【核心修改】操作分离 ---

/**
 * 保存草稿
 */
const handleSaveDraft = async () => {
  saving.value = true;
  try {
    const { xml, processDefinitionKey } = await getModelerContent();
    const payload = {
      bpmnXml: xml,
      processDefinitionKey
    };
    await updateWorkflowTemplate(formId, payload);
    message.success('流程草稿保存成功！');
  } catch (err) {
    message.error('保存草稿失败: ' + err.message);
    console.error('Save Draft Error:', err);
  } finally {
    saving.value = false;
  }
};

/**
 * 部署流程
 */
const handleDeploy = () => {
  Modal.confirm({
    title: '确认部署',
    content: '部署操作将更新线上正在运行的流程版本，新的申请将按照此版本执行。是否继续？',
    okText: '确认部署',
    cancelText: '取消',
    onOk: async () => {
      deploying.value = true;
      try {
        const { xml, processDefinitionKey } = await getModelerContent();
        const payload = {
          formDefinitionId: parseInt(formId, 10),
          bpmnXml: xml,
          processDefinitionKey
        };
        await deployWorkflow(payload);
        message.success('流程部署成功！');
        router.push('/');
      } catch (err) {
        message.error('部署流程时出错: ' + err.message);
        console.error('Deploy Error:', err);
      } finally {
        deploying.value = false;
      }
    }
  });
};


// --- 工具栏方法 ---
const handleUndo = () => modeler.get('commandStack').undo();
const handleRedo = () => modeler.get('commandStack').redo();

let scale = 1;
const handleZoomIn = () => {
  scale += 0.1;
  modeler.get('canvas').zoom(scale);
};
const handleZoomOut = () => {
  if (scale > 0.2) {
    scale -= 0.1;
    modeler.get('canvas').zoom(scale);
  }
};
const handleFitViewport = () => {
  scale = 1;
  modeler.get('canvas').zoom('fit-viewport');
};

// --- 文件导入/导出 ---
const triggerImport = () => {
  fileInputRef.value?.click();
};

const handleFileImport = (event) => {
  const file = event.target.files[0];
  if (file && modeler) {
    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        await modeler.importXML(e.target.result);
        message.success('BPMN 文件导入成功！');
      } catch (err) {
        message.error('导入失败，文件格式可能无效。');
        console.error('Import Error:', err);
      }
    };
    reader.readAsText(file);
  }
  // 清空 input 的值，以便可以再次上传同一个文件
  event.target.value = '';
};


function downloadFile(filename, data, type) {
  const a = document.createElement('a');
  const url = URL.createObjectURL(new Blob([data], { type }));
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

const downloadBpmn = async () => {
  try {
    const { xml } = await modeler.saveXML({ format: true });
    downloadFile(`${formName.value || 'process'}.bpmn`, xml, 'application/xml');
  } catch {
    message.error('下载 BPMN 文件失败');
  }
};

const downloadSvg = async () => {
  try {
    const { svg } = await modeler.saveSVG();
    downloadFile(`${formName.value || 'process'}.svg`, svg, 'image/svg+xml');
  } catch {
    message.error('下载 SVG 图像失败');
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

#canvas {
  flex-grow: 1;
  background-color: #f9f9f9;
}

#properties-panel-container {
  width: 320px;
  background: #f8f8f8;
  overflow-y: auto;
  border-left: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

:deep(.bjs-powered-by) {
  display: none !important;
}

:deep(.djs-palette) {
  top: 20px;
  left: 20px;
}
</style>