<template>
  <div class="hero-banner" :style="{ backgroundImage: `url(${resolvedImageUrl})` }">
    <div class="hero-content">
      <h1 v-if="resolvedTitle" class="hero-title">{{ resolvedTitle }}</h1>
      <p v-if="resolvedSubtitle" class="hero-subtitle">{{ resolvedSubtitle }}</p>
      <div v-if="resolvedButtonText && resolvedButtonLink" class="hero-actions">
        <router-link :to="resolvedButtonLink">
          <a-button type="primary" size="large">{{ resolvedButtonText }}</a-button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { resolveBinding } from '@/utils/bindingResolver';

const props = defineProps({
  title: String,
  subtitle: String,
  imageUrl: String,
  buttonText: String,
  buttonLink: String,
  businessData: Object,
});

const resolvedTitle = computed(() => resolveBinding(props.title, props.businessData));
const resolvedSubtitle = computed(() => resolveBinding(props.subtitle, props.businessData));
const resolvedImageUrl = computed(() => resolveBinding(props.imageUrl, props.businessData));
const resolvedButtonText = computed(() => resolveBinding(props.buttonText, props.businessData));
const resolvedButtonLink = computed(() => resolveBinding(props.buttonLink, props.businessData));

</script>

<style scoped>
.hero-banner {
  position: relative;
  height: 60vh;
  min-height: 400px;
  background-size: cover;
  background-position: center;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

/* 添加一个遮罩层使文字更清晰 */
.hero-banner::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
}

.hero-content {
  position: relative;
  z-index: 1;
  padding: 20px;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
}

.hero-subtitle {
  font-size: 1.5rem;
  max-width: 600px;
  margin: 0 auto 1.5rem;
  font-weight: 300;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2.5rem;
  }
  .hero-subtitle {
    font-size: 1.2rem;
  }
}
</style>