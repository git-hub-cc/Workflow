<template>
  <div>
    <a-divider>数据源</a-divider>

    <!-- 数据源类型选择 -->
    <a-form-item label="数据源类型">
      <a-radio-group v-model:value="localField.dataSource.type" button-style="solid" size="small">
        <a-radio-button value="static">静态数据</a-radio-button>
        <a-radio-button value="api">API (列表)</a-radio-button>
        <a-radio-button v-if="localField.type === 'TreeSelect'" value="api-tree">API (树形)</a-radio-button>
        <!-- 【核心修改】扩展 UserPicker 的数据源 -->
        <a-radio-group v-if="localField.type === 'UserPicker'" v-model:value="localField.dataSource.type" button-style="solid" size="small">
          <a-radio-button value="system-users-global">全局搜索</a-radio-button>
          <a-radio-button value="system-users-dept">按部门</a-radio-button>
          <a-radio-button value="system-users-role">按角色</a-radio-button>
        </a-radio-group>
      </a-radio-group>
    </a-form-item>

    <!-- 【核心新增】UserPicker 按部门/角色配置 -->
    <a-form-item v-if="localField.dataSource.type === 'system-users-dept'" label="选择部门">
      <a-tree-select v-model:value="localField.dataSource.departmentId" :tree-data="departmentTree" placeholder="请选择部门" tree-default-expand-all />
    </a-form-item>
    <a-form-item v-if="localField.dataSource.type === 'system-users-role'" label="选择角色">
      <!-- 【核心修复】绑定到本地的 allRoles，而不是 userStore.allRoles -->
      <a-select v-model:value="localField.dataSource.roleName" :options="allRoles.map(r => ({label: r.description, value: r.name}))" placeholder="请选择角色" />
    </a-form-item>

    <!-- 静态数据配置 -->
    <div v-if="localField.dataSource.type === 'static'">
      <template v-if="localField.type === 'TreeSelect'">
        <a-textarea v-model:value="staticTreeData" :rows="6" placeholder="请输入JSON格式的树形数据" @change="parseStaticTreeData" />
        <p style="font-size: 12px; color: #888; margin-top: 4px;">
          格式: <code>[{"title":"a", "value":"a", "children": [...]}]</code>
        </p>
      </template>
      <template v-else>
        <div v-for="(option, index) in localField.dataSource.options" :key="index" class="static-option-row">
          <a-input v-model:value="option.label" placeholder="显示文本 (Label)" />
          <a-input v-model:value="option.value" placeholder="实际值 (Value)" />
          <a-button type="text" danger @click="removeStaticOption(index)">
            <DeleteOutlined />
          </a-button>
        </div>
        <a-button type="dashed" block @click="addStaticOption">
          <PlusOutlined /> 添加选项
        </a-button>
      </template>
    </div>

    <!-- API列表接口配置 -->
    <div v-if="localField.dataSource.type === 'api'">
      <a-form-item label="接口URL">
        <a-input v-model:value="localField.dataSource.url" placeholder="/api/your/data/endpoint" />
      </a-form-item>
      <a-form-item label="值字段 (Value Key)">
        <a-input v-model:value="localField.dataSource.valueKey" placeholder="例如: id" />
      </a-form-item>
      <a-form-item label="文本字段 (Label Key)">
        <a-input v-model:value="localField.dataSource.labelKey" placeholder="例如: name" />
      </a-form-item>

      <!-- 【核心新增】级联配置 -->
      <a-divider>级联配置 (可选)</a-divider>
      <a-form-item label="监听字段">
        <a-select v-model:value="localField.dataSource.listensTo" placeholder="选择一个父级字段" allow-clear>
          <a-select-option v-for="f in parentFieldsForCascading" :key="f.id" :value="f.id">
            {{ f.label }} ({{ f.id }})
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="请求参数名" help="父级字段的值将作为此参数发送请求">
        <a-input v-model:value="localField.dataSource.paramName" placeholder="例如: parentId" />
      </a-form-item>
    </div>

    <!-- API树形接口配置 -->
    <div v-if="localField.dataSource.type === 'api-tree'">
      <a-form-item label="数据源标识 (Source)">
        <a-input v-model:value="localField.dataSource.source" placeholder="例如: departments" />
        <p style="font-size: 12px; color: #888; margin-top: 4px;">
          此标识将作为参数 <code>?source=departments</code> 传递给后端通用树形数据接口。
        </p>
      </a-form-item>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { message } from "ant-design-vue";
// 【核心修复】移除不再使用的 useUserStore
import { getDepartmentTree, getRoles } from '@/api'; // 【核心修复】引入 getRoles API
import { flattenFields } from '@/utils/formUtils.js';

const props = defineProps({
  field: { type: Object, required: true },
  allFields: { type: Array, required: true },
});
const emit = defineEmits(['update:field']);

// 【核心修复】移除 userStore 实例
const departmentTree = ref([]);
const allRoles = ref([]); // 【核心修复】创建本地 state 用于存储角色

const localField = computed({
  get: () => props.field,
  set: (newField) => emit('update:field', newField),
});

onMounted(async () => {
  // 【核心修复】组件自己获取角色和部门数据
  try {
    const [rolesRes, deptData] = await Promise.all([
      getRoles({ page: 0, size: 1000 }),
      getDepartmentTree()
    ]);
    allRoles.value = rolesRes.content;
    departmentTree.value = transformDeptTree(deptData);
  } catch (e) {
    message.error("加载配置所需数据失败");
  }
});

const transformDeptTree = (nodes) => {
  return nodes.map(node => ({
    title: node.name,
    value: node.id,
    children: node.children ? transformDeptTree(node.children) : []
  }));
};

const parentFieldsForCascading = computed(() => {
  return flattenFields(props.allFields).filter(f =>
      f.id !== props.field.id && ['Select', 'TreeSelect', 'RadioGroup'].includes(f.type)
  );
});

// For TreeSelect static data
const staticTreeData = ref('');
watch(() => localField.value.dataSource, (newDs) => {
  if (newDs && newDs.type === 'static' && localField.value.type === 'TreeSelect') {
    staticTreeData.value = JSON.stringify(newDs.options || [], null, 2);
  }
}, { immediate: true, deep: true });

const parseStaticTreeData = () => {
  try {
    localField.value.dataSource.options = JSON.parse(staticTreeData.value);
  } catch (e) {
    message.error('JSON格式不正确', 2);
  }
};

const addStaticOption = () => {
  if (!localField.value.dataSource.options) {
    localField.value.dataSource.options = [];
  }
  const newIndex = localField.value.dataSource.options.length + 1;
  localField.value.dataSource.options.push({ label: `选项${newIndex}`, value: `选项${newIndex}` });
};

const removeStaticOption = (index) => {
  localField.value.dataSource.options.splice(index, 1);
};
</script>

<style scoped>
.static-option-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>