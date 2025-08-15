<template>
  <div class="page-container">
    <a-page-header title="流程设计器" @back="() => $router.push('/')">
      <template #subTitle>
        为表单: <strong style="color: #1890ff">{{ formName }}</strong>
      </template>
      <template #extra>
        <a-button @click="$router.push('/')">取消</a-button>
        <a-button type="primary" @click="saveAndDeploy" :loading="saving">保存并部署</a-button>
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
        <a-tooltip title="下载为 BPMN 文件">
          <a-button @click="downloadBpmn"><DownloadOutlined /> BPMN</a-button>
        </a-tooltip>
        <a-tooltip title="下载为 SVG 图像">
          <a-button @click="downloadSvg"><FileImageOutlined /> SVG</a-button>
        </a-tooltip>
      </a-space>
    </div>

    <div class="designer-container">
      <div id="canvas" ref="canvasRef"></div>
      <div id="properties-panel-container" ref="propertiesPanelRef"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { getFormById, deployWorkflow, getWorkflowTemplate } from '@/api';
import {
  UndoOutlined, RedoOutlined, ZoomInOutlined, ZoomOutOutlined,
  FullscreenOutlined, DownloadOutlined, FileImageOutlined
} from '@ant-design/icons-vue';

// --- BPMN.js 相关导入 ---
import BpmnModeler from 'bpmn-js/lib/Modeler';
import * as BpmnJSExtra from 'bpmn-js-properties-panel';
const {
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule,
  CamundaPlatformPropertiesProviderModule,
} = BpmnJSExtra;
import camundaModdleDescriptors from 'camunda-bpmn-moddle/resources/camunda.json';
// 【新增】导入汉化模块
import customTranslate from '@/utils/customTranslate';

const route = useRoute();
const router = useRouter();
const formId = route.params.formId;

const canvasRef = ref(null);
const propertiesPanelRef = ref(null);
const formName = ref('加载中...');
const saving = ref(false);
let modeler = null;

const canUndo = ref(false);
const canRedo = ref(false);

onMounted(async () => {
  try {
    const form = await getFormById(formId);
    formName.value = form.name;

    modeler = new BpmnModeler({
      container: canvasRef.value,
      propertiesPanel: {
        parent: propertiesPanelRef.value,
      },
      additionalModules: [
        BpmnPropertiesPanelModule,
        BpmnPropertiesProviderModule,
        CamundaPlatformPropertiesProviderModule,
        // 【新增】加载汉化模块
        {
          translate: ['value', customTranslate]
        }
      ],
      moddleExtensions: {
        camunda: camundaModdleDescriptors,
      },
    });

    // 监听命令栈变化，更新撤销/重做按钮状态
    const eventBus = modeler.get('eventBus');
    eventBus.on('commandStack.changed', () => {
      canUndo.value = modeler.get('commandStack').canUndo();
      canRedo.value = modeler.get('commandStack').canRedo();
    });

    const template = await getWorkflowTemplate(formId);
    await modeler.importXML(template.bpmnXml);
    handleFitViewport(); // 初始加载后适应屏幕

  } catch (error) {
    message.error('初始化设计器失败! 请检查控制台获取详细信息。');
    console.error('Designer Initialization Error:', error);
  }
});

onBeforeUnmount(() => {
  if (modeler) {
    modeler.destroy();
    modeler = null;
  }
});

const saveAndDeploy = async () => {
  // ... (省略原有保存逻辑, 无需修改)
  if (!modeler) {
    message.error('设计器未初始化！');
    return;
  }
  saving.value = true;
  try {
    const { xml } = await modeler.saveXML({ format: true });

    const elementRegistry = modeler.get('elementRegistry');
    const processElement = elementRegistry.find(el => el.type === 'bpmn:Process');
    if (!processElement) {
      throw new Error('无法在BPMN图中找到 <bpmn:process> 元素。');
    }
    const processDefinitionKey = processElement.businessObject.id;
    if (!processDefinitionKey) {
      throw new Error('流程 Process ID 不能为空，请在属性面板中设置。');
    }

    const payload = {
      formDefinitionId: parseInt(formId, 10),
      bpmnXml: xml,
      processDefinitionKey: processDefinitionKey,
    };

    await deployWorkflow(payload);
    message.success('流程部署成功！');
    router.push('/');
  } catch (err) {
    message.error('保存或部署流程时出错: ' + err.message);
    console.error('Deploy Error:', err);
  } finally {
    saving.value = false;
  }
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

function downloadFile(filename, data, type) {
  const a = document.createElement('a');
  const url = URL.createObjectURL(new Blob([data], { type }));
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

const downloadBpmn = async () => {
  try {
    const { xml } = await modeler.saveXML({ format: true });
    downloadFile(`${formName.value || 'process'}.bpmn`, xml, 'application/xml');
  } catch (err) {
    message.error('下载 BPMN 文件失败');
  }
};

const downloadSvg = async () => {
  try {
    const { svg } = await modeler.saveSVG();
    downloadFile(`${formName.value || 'process'}.svg`, svg, 'image/svg+xml');
  } catch (err) {
    message.error('下载 SVG 图像失败');
  }
};
</script>

<style scoped>
.page-container {
  padding: 0;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  background-color: #fff;
}

.toolbar {
  padding: 8px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
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
}

:deep(.djs-palette) {
  top: 20px;
  left: 20px;
}
:deep(.bjs-powered-by) {
  display: none !important;
}
</style>