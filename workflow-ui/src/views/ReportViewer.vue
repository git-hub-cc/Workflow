<template>
  <div class="page-container">
    <a-page-header :title="reportData.title || '报表'" @back="() => $router.go(-1)" />
    <div style="padding: 24px;">
      <a-spin :spinning="loading">
        <a-card v-if="reportData && reportData.type">
          <!-- ECharts 图表渲染器 -->
          <v-chart
              v-if="['pie', 'bar', 'line'].includes(reportData.type)"
              class="chart"
              :option="reportData.options"
              autoresize
          />

          <!-- 表格渲染器 -->
          <a-table
              v-if="reportData.type === 'table'"
              :columns="reportData.tableColumns"
              :data-source="reportData.tableData"
              row-key="id"
          />

        </a-card>
        <a-empty v-else-if="!loading" description="无法加载报表数据或报表类型不支持。" />
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { getReportData } from '@/api';
import { message } from 'ant-design-vue';

// --- ECharts 相关引入 ---
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart, BarChart, LineChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
} from 'echarts/components';
import VChart from 'vue-echarts';

// 注册 ECharts 组件
use([
  CanvasRenderer,
  PieChart,
  BarChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
]);


const props = defineProps({
  reportKey: {
    type: String,
    required: true,
  }
});

const loading = ref(true);
const reportData = ref({});

const fetchReport = async () => {
  if (!props.reportKey) {
    message.error("报表配置错误：未指定报表Key。");
    loading.value = false;
    return;
  }
  try {
    reportData.value = await getReportData(props.reportKey);
  } catch (error) {
    // 错误已由全局拦截器处理
  } finally {
    loading.value = false;
  }
};

onMounted(fetchReport);

</script>

<style scoped>
.page-container {
  background-color: #fff;
}
.chart {
  height: 500px;
}
</style>