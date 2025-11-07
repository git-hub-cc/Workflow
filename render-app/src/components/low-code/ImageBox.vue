<template>
  <div class="image-box-container">
    <img :src="resolvedSrc" :alt="resolvedAlt" />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { resolveBinding } from '@/utils/bindingResolver';

const props = defineProps({
  src: String,
  alt: String,
  businessData: Object,
});

const resolvedSrc = computed(() => resolveBinding(props.src, props.businessData));
const resolvedAlt = computed(() => resolveBinding(props.alt, props.businessData));

</script>

<style scoped>
.image-box-container {
  padding: 20px;
  text-align: center;
}
.image-box-container img {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
</style>