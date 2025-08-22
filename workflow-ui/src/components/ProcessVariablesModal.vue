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
                    v-else-if="record.type === 'json' || record.type === 'object'"
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
                <template v-if="record.type === 'json' || record.type === 'object'">
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
    // 全局处理器已处理
  } finally {
    loading.value = false;
  }
};

onMounted(fetchVariables);

const isNumeric = (type) => ['integer', 'long', 'double', 'short'].includes(type.toLowerCase());

const formatJson = (value) => {
  if (value === null || value === undefined) return '';
  try {
    // 如果值本身就是对象或数组，直接格式化
    if (typeof value === 'object') {
      return JSON.stringify(value, null, 2);
    }
    // 如果是字符串，尝试解析为JSON再格式化
    const obj = JSON.parse(value.toString());
    return JSON.stringify(obj, null, 2);
  } catch (e) {
    // 如果解析失败，说明它可能不是一个有效的JSON字符串，直接返回原值
    return value.toString();
  }
};

const edit = (key) => {
  const target = variables.value.find(item => key === item.name);
  if (target) {
    const clonedTarget = cloneDeep(target);
    // 对于JSON/Object类型，在编辑时格式化一下，方便阅读和修改
    if (clonedTarget.type === 'json' || clonedTarget.type === 'object') {
      clonedTarget.value = formatJson(clonedTarget.value);
    }
    editableData[key] = clonedTarget;
  }
};


const save = async (key) => {
  const editedData = editableData[key];
  const payload = { ...editedData };

  // 在发送前，对于 boolean 类型，确保它的值是 true/false 而不是 1/0
  if (payload.type === 'boolean') {
    payload.value = !!payload.value;
  }

  try {
    await updateProcessVariable(props.instanceId, payload);
    const target = variables.value.find(item => key === item.name);
    if (target) {
      Object.assign(target, editableData[key]);
    }
    delete editableData[key];
    message.success(`变量 "${key}" 已更新`);
  } catch (error) {
    // 错误信息已由全局拦截器显示
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
  white-space: pre-wrap;
  word-break: break-all;
}
</style>