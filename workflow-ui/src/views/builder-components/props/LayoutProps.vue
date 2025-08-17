<template>
  <div>
    <a-form-item label="列数">
      <a-slider v-model:value="columnCount" :min="1" :max="4" @change="onColumnCountChange" />
    </a-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
const props = defineProps(['field']);
const columnCount = ref(props.field.props.columns.length);

watch(() => props.field.props.columns.length, (newLength) => {
  columnCount.value = newLength;
});

const onColumnCountChange = (count) => {
  const currentCount = props.field.props.columns.length;
  if (count > currentCount) {
    // Add new columns
    for (let i = 0; i < count - currentCount; i++) {
      props.field.props.columns.push([]);
    }
  } else if (count < currentCount) {
    // Remove columns, move items from deleted columns to the last remaining column
    const lastColumn = props.field.props.columns[count - 1];
    const itemsToMove = props.field.props.columns.slice(count).flat();
    lastColumn.push(...itemsToMove);
    props.field.props.columns.splice(count);
  }
};

</script>