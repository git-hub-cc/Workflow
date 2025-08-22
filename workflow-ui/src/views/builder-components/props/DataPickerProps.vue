<template>
  <div>
    <!-- Generic Properties -->
    <GenericProps :field="field" :all-fields="allFields" />

    <a-divider>数据选择器配置</a-divider>

    <a-form-item label="弹窗标题">
      <a-input v-model:value="field.props.modalTitle" />
    </a-form-item>

    <!-- 【核心修改】允许用户输入自定义API或从预设中选择 -->
    <a-form-item label="数据接口" help="可从列表选择或手动输入自定义API地址">
      <a-auto-complete
          v-model:value="field.props.dataUrl"
          :options="predefinedApisForSelect"
          placeholder="/api/custom/endpoint"
          @select="onApiChange"
          @change="onApiChange"
      />
    </a-form-item>


    <a-form-item>
      <a-button @click="testApi" :loading="testingApi" :disabled="!field.props.dataUrl">
        <template #icon><ApiOutlined /></template>
        测试接口并自动填充
      </a-button>
    </a-form-item>

    <!-- Table Columns Configuration -->
    <a-form-item label="弹窗表格列">
      <div v-for="(col, index) in field.props.columns" :key="index" class="config-row">
        <a-input v-model:value="col.title" placeholder="列标题 (Title)" />
        <a-input v-model:value="col.dataIndex" placeholder="数据字段 (DataIndex)" />
        <a-button type="text" danger @click="removeColumn(index)">
          <DeleteOutlined />
        </a-button>
      </div>
      <a-button type="dashed" block @click="addColumn">
        <PlusOutlined /> 添加列
      </a-button>
    </a-form-item>

    <!-- Field Mappings Configuration -->
    <a-form-item label="回填字段映射" help="将选中行的源字段值，回填到当前表单的目标字段中">
      <div v-for="(mapping, index) in field.props.mappings" :key="index" class="config-row">
        <a-input v-model:value="mapping.sourceField" placeholder="源字段 (Source)" />
        <a-select v-model:value="mapping.targetField" placeholder="目标字段 (Target)">
          <a-select-option v-for="f in availableTargetFields" :key="f.id" :value="f.id">
            {{ f.label }} ({{ f.id }})
          </a-select-option>
        </a-select>
        <a-button type="text" danger @click="removeMapping(index)">
          <DeleteOutlined />
        </a-button>
      </div>
      <a-button type="dashed" block @click="addMapping">
        <PlusOutlined /> 添加映射
      </a-button>
    </a-form-item>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { DeleteOutlined, PlusOutlined, ApiOutlined } from '@ant-design/icons-vue';
import { flattenFields } from '@/utils/formUtils.js';
import GenericProps from './GenericProps.vue';
import { predefinedApis } from '@/utils/apiLibrary.js';
import { fetchTableData } from '@/api';
import { message } from 'ant-design-vue';

const props = defineProps(['field', 'allFields']);
const testingApi = ref(false);

const predefinedApisForSelect = computed(() =>
    predefinedApis.map(api => ({ value: api.url, label: api.name }))
);

const availableTargetFields = computed(() => {
  return flattenFields(props.allFields).filter(f => !['GridRow', 'GridCol', 'DataPicker', 'Subform'].includes(f.type));
});

let debounceTimer = null;
const onApiChange = () => {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    testApi();
  }, 1000); // 延迟1秒自动测试
};

const testApi = async () => {
  if (!props.field.props.dataUrl) {
    message.warn("请先选择或输入一个数据接口");
    return;
  }
  testingApi.value = true;
  try {
    const response = await fetchTableData(props.field.props.dataUrl, { page: 0, size: 1 });
    const dataList = Array.isArray(response) ? response : response.content;

    if (!dataList || dataList.length === 0) {
      message.warn('接口返回数据为空，无法自动填充配置。');
      props.field.props.columns = [];
      props.field.props.mappings = [];
      return;
    }

    const firstItem = dataList[0];
    const keys = Object.keys(firstItem);

    props.field.props.columns = keys.map(key => ({ title: key, dataIndex: key }));
    props.field.props.mappings = keys.map(key => ({ sourceField: key, targetField: '' }));

    message.success('接口测试成功，已自动填充列和映射配置！');

  } catch (error) {
    message.error('接口测试失败，请检查URL是否正确或联系管理员。');
  } finally {
    testingApi.value = false;
  }
};


// Columns management
const addColumn = () => {
  if (!props.field.props.columns) props.field.props.columns = [];
  props.field.props.columns.push({ title: '', dataIndex: '' });
};
const removeColumn = (index) => {
  props.field.props.columns.splice(index, 1);
};

// Mappings management
const addMapping = () => {
  if (!props.field.props.mappings) props.field.props.mappings = [];
  props.field.props.mappings.push({ sourceField: '', targetField: '' });
};
const removeMapping = (index) => {
  props.field.props.mappings.splice(index, 1);
};
</script>

<style scoped>
.config-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>