<template>
  <div class="page-container">
    <a-page-header title="仪表盘" sub-title="系统关键指标概览" />
    <div style="padding: 24px;">
      <a-spin :spinning="loading">
        <a-row :gutter="[24, 24]">
          <!-- 指标卡片 -->
          <a-col :xs="24" :sm="12" :md="6">
            <a-card>
              <a-statistic title="运行中的流程" :value="stats.runningInstances" />
            </a-card>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-card>
              <a-statistic title="本月已完成" :value="stats.completedThisMonth" />
            </a-card>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-card>
              <a-statistic title="本月已通过" :value="stats.approvedThisMonth" class="statistic-success" />
            </a-card>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-card>
              <a-statistic title="本月已拒绝" :value="stats.rejectedThisMonth" class="statistic-error" />
            </a-card>
          </a-col>

          <!-- ECharts 图表 -->
          <a-col :span="24">
            <a-card title="任务耗时 TOP 5 (瓶颈分析)">
              <!-- 3. 将 Ant Design Chart 的 <Column> 替换为 ECharts 的 <v-chart> -->
              <v-chart class="chart" :option="chartOption" autoresize />
            </a-card>
          </a-col>
        </a-row>
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { getDashboardStats } from '@/api';
import { message } from 'ant-design-vue';

// --- 1. 引入 vue-echarts 和 ECharts 核心模块 ---
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { BarChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
} from 'echarts/components';
import VChart from 'vue-echarts';

// 2. 注册 ECharts 组件
use([
  CanvasRenderer,
  BarChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
]);


const loading = ref(true);
const stats = ref({});

const fetchStats = async () => {
  try {
    stats.value = await getDashboardStats();
  } catch (error) {
    message.error('加载仪表盘数据失败');
  } finally {
    loading.value = false;
  }
};

onMounted(fetchStats);

// 4. 创建一个 computed 属性来生成 ECharts 需要的 option 对象
const chartOption = computed(() => {
  if (!stats.value?.taskBottlenecks) {
    return {};
  }
  const sourceData = stats.value.taskBottlenecks;
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
      formatter: '{b}: {c} 分钟' // 提示框格式
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: sourceData.map(item => `${item.taskName}\n(${item.processDefinitionKey.split(':')[0]})`), // X轴：任务名 + 流程名
      axisLabel: {
        interval: 0, // 强制显示所有标签
        rotate: 15   // 轻微旋转以防重叠
      }
    },
    yAxis: {
      type: 'value',
      name: '耗时 (分钟)',
    },
    series: [
      {
        name: '耗时',
        type: 'bar',
        barWidth: '60%',
        data: sourceData.map(item => parseFloat((item.durationMillis / 1000 / 60).toFixed(2))), // Y轴：转换为分钟
        itemStyle: {
          color: '#5470C6'
        }
      },
    ],
  };
});
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
:deep(.statistic-success .ant-statistic-content-value) {
  color: #52c41a;
}
:deep(.statistic-error .ant-statistic-content-value) {
  color: #f5222d;
}
.chart {
  height: 400px;
}
</style>