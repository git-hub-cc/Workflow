<template>
  <a-modal
      :open="open"
      title="管理流程变量"
      width="800px"
      :footer="null"
      @update:open="(val) => $emit('update:open', val)"
      destroyOnClose
  >
    <a-spin :spinning="loading">
      <a-table
          :columns="columns"
          :data-source="variables"
          row-key="name"
          :pagination="false"
          size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'value'">
            <!-- 可编辑单元格 -->
            <div class="editable-cell">
              <div v-if="editableData[record.name]" class="edit-container">
                <!-- 根据变量类型显示不同的编辑器 -->
                <a-input-number
                    v-if="isNumeric(record.type)"
                    v-model:value="editableData[record.name].value"
                    @pressEnter="save(record.name)"
                    style="width: 100%;"
                />
                <a-switch
                    v-else-if="record.type === 'boolean'"
                    v-model:checked="editableData[record.name].value"
                />
                <a-textarea
                    v-else-if="record.type === 'json'"
                    v-model:value="editableData[record.name].value"
                    :rows="4"
                    auto-size
                />
                <a-input
                    v-else
                    v-model:value="editableData[record.name].value"
                    @pressEnter="save(record.name)"
                />
              </div>
              <!-- 显示单元格 -->
              <div v-else class="display-container">
                <template v-if="record.type === 'json'">
                  <pre class="json-pre">{{ formatJson(record.value) }}</pre>
                </template>
                <template v-else-if="record.type === 'boolean'">
                  <a-tag :color="record.value ? 'green' : 'red'">{{ record.value }}</a-tag>
                </template>
                <template v-else>
                  <span class="value-text">{{ record.value }}</span>
                </template>
              </div>
            </div>
          </template>

          <template v-if="column.key === 'actions'">
            <div class="editable-row-operations">
              <span v-if="editableData[record.name]">
                <a @click="save(record.name)">保存</a>
                <a-popconfirm title="确定要取消吗?" @confirm="cancel(record.name)">
                  <a>取消</a>
                </a-popconfirm>
              </span>
              <span v-else>
                <a @click="edit(record.name)">编辑</a>
              </span>
            </div>
          </template>
        </template>
      </a-table>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { getProcessVariables, updateProcessVariable } from '@/api';
import { message } from 'ant-design-vue';
import { cloneDeep } from 'lodash-es';

const props = defineProps({
  open: Boolean,
  instanceId: {
    type: String,
    required: true,
  },
});
defineEmits(['update:open']);

const loading = ref(true);
const variables = ref([]);
const editableData = reactive({});

const columns = [
  { title: '变量名', dataIndex: 'name', key: 'name', width: '25%' },
  { title: '类型', dataIndex: 'type', key: 'type', width: '15%' },
  { title: '值', dataIndex: 'value', key: 'value', width: '45%' },
  { title: '操作', key: 'actions', width: '15%', align: 'center' },
];

const fetchVariables = async () => {
  try {
    variables.value = await getProcessVariables(props.instanceId);
  } catch (error) {
    // global handler
  } finally {
    loading.value = false;
  }
};

onMounted(fetchVariables);

const isNumeric = (type) => ['integer', 'long', 'double'].includes(type.toLowerCase());

const formatJson = (value) => {
  try {
    const obj = JSON.parse(value);
    return JSON.stringify(obj, null, 2);
  } catch (e) {
    return value;
  }
};

const edit = (key) => {
  editableData[key] = cloneDeep(variables.value.filter(item => key === item.name)[0]);
};

const save = async (key) => {
  const payload = editableData[key];
  try {
    await updateProcessVariable(props.instanceId, payload);
    Object.assign(variables.value.filter(item => key === item.name)[0], editableData[key]);
    delete editableData[key];
    message.success(`变量 "${key}" 已更新`);
  } catch (error) {
    message.error(`变量 "${key}" 更新失败`);
  }
};

const cancel = (key) => {
  delete editableData[key];
};
</script>

<style scoped>
.editable-row-operations a {
  margin-right: 8px;
}
.json-pre {
  background-color: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  max-height: 150px;
  overflow: auto;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
.editable-cell {
  position: relative;
}
.value-text {
  max-height: 100px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>