<template>
  <div class="page-container" style="padding: 0; display: flex; flex-direction: column; height: calc(100vh - 64px);">
    <a-page-header title="流程设计器" @back="() => $router.push('/')">
      <template #subTitle>
        为表单: <strong style="color: #1890ff">{{ formName }}</strong>
      </template>
      <template #extra>
        <a-button @click="$router.push('/')">取消</a-button>
        <a-button type="primary" @click="saveAndDeploy" :loading="saving">保存并部署</a-button>
      </template>
    </a-page-header>

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

// --- Import 部分 (CSS 导入已被移除) ---
import BpmnModeler from 'bpmn-js/lib/Modeler';
import * as BpmnJSExtra from 'bpmn-js-properties-panel';
const {
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule,
  CamundaPlatformPropertiesProviderModule,
} = BpmnJSExtra;
import camundaModdleDescriptors from 'camunda-bpmn-moddle/resources/camunda.json';
// --- CSS 导入已从此文件移除 ---


const route = useRoute();
const router = useRouter();
const formId = route.params.formId;

const canvasRef = ref(null);
const propertiesPanelRef = ref(null);
const formName = ref('加载中...');
const saving = ref(false);
let modeler = null;

const defaultBpmnXml = (processId) => `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn" xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
  <bpmn:process id="${processId}" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="${processId}">
      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
        <dc:Bounds x="179" y="159" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

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
      ],
      moddleExtensions: {
        camunda: camundaModdleDescriptors,
      },
    });

    try {
      const template = await getWorkflowTemplate(formId);
      await modeler.importXML(template.bpmnXml);
    } catch (e) {
      const processId = `Process_Form_${formId}`;
      await modeler.importXML(defaultBpmnXml(processId));
    }

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
</script>

<style scoped>
.page-container {
  padding: 0;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  background-color: #fff;
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