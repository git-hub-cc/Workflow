<template>
  <div>
    <a-form-item label="列间距 (Gutter)">
      <a-slider v-model:value="field.props.gutter" :min="0" :max="48" />
    </a-form-item>
    <a-form-item label="列数">
      <a-slider v-model:value="columnCount" :min="1" :max="4" @change="onColumnCountChange" />
    </a-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
const props = defineProps(['field']);

// 内部状态，用于同步滑块和实际列数
const columnCount = ref(props.field.columns.length);

// 监听外部变化（例如撤销/重做），更新滑块位置
watch(() => props.field.columns.length, (newLength) => {
  columnCount.value = newLength;
});

const onColumnCountChange = (newCount) => {
  const currentColumns = props.field.columns;
  const currentCount = currentColumns.length;

  if (newCount > currentCount) {
    // 如果增加列数，则在末尾添加新列
    for (let i = 0; i < newCount - currentCount; i++) {
      currentColumns.push({ type: 'GridCol', props: { span: 12 }, fields: [] });
    }
  } else if (newCount < currentCount) {
    // 如果减少列数，则将多余列中的组件移动到最后一个保留的列中
    const lastRemainingColumn = currentColumns[newCount - 1];
    if (lastRemainingColumn) {
      const itemsToMove = currentColumns.slice(newCount).flatMap(col => col.fields);
      lastRemainingColumn.fields.push(...itemsToMove);
    }
    // 移除多余的列
    currentColumns.splice(newCount);
  }

  // 重新计算所有列的 span 以便均匀分布
  const baseSpan = Math.floor(24 / newCount);
  const remainder = 24 % newCount;
  currentColumns.forEach((col, index) => {
    // 将余数均匀分配给前面的列
    col.props.span = baseSpan + (index < remainder ? 1 : 0);
  });
};
</script>