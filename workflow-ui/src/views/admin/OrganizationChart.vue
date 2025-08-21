<template>
  <div class="page-container">
    <a-page-header title="组织架构图" sub-title="可视化展示公司部门与人员层级关系" />

    <div style="padding: 24px;">
      <a-spin :spinning="loading">
        <a-alert
            message="交互说明"
            description="您可以滚动鼠标滚轮进行缩放，按住画布拖动进行平移。"
            type="info"
            show-icon
            style="margin-bottom: 24px;"
        />
        <div ref="chartRef" class="chart-container"></div>
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { getOrganizationTree } from '@/api';
import { message } from 'ant-design-vue';
import * as echarts from 'echarts/core';
import { TreeChart } from 'echarts/charts';
import { TooltipComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
// 【核心修改】引入 system store 以获取主题色
import { useSystemStore } from '@/stores/system';

echarts.use([TreeChart, TooltipComponent, LegendComponent, CanvasRenderer]);

const loading = ref(true);
const chartRef = ref(null);
let chartInstance = null;
// 【核心修改】实例化 store
const systemStore = useSystemStore();

// 将后端返回的 DepartmentTreeNode 结构转换为 ECharts TreeChart 需要的格式
const transformDataForEcharts = (data) => {
  // 【核心修改】从 store 中获取主题色
  const themeColor = systemStore.settings.THEME_COLOR || '#1890ff';

  return data.map(node => {
    const isDept = node.type === 'department';
    return {
      name: node.title.split(' (')[0], // 只显示名字
      value: node.value,
      itemStyle: {
        // 【核心修改】动态设置部门节点的颜色
        color: isDept ? themeColor : '#bae7ff',
        borderColor: themeColor,
      },
      label: {
        position: isDept ? 'top' : 'bottom',
        verticalAlign: 'middle',
        align: 'center',
        fontSize: isDept ? 14 : 12,
        fontWeight: isDept ? 'bold' : 'normal',
      },
      children: node.children ? transformDataForEcharts(node.children) : [],
    };
  });
};

const initChart = async () => {
  try {
    const rawData = await getOrganizationTree();
    const echartsData = {
      name: '组织架构',
      itemStyle: { color: '#fa8c16', borderColor: '#fa8c16' },
      children: transformDataForEcharts(rawData),
    };

    await nextTick();

    if (chartRef.value) {
      chartInstance = echarts.init(chartRef.value);
      const option = {
        tooltip: {
          trigger: 'item',
          triggerOn: 'mousemove',
          formatter: (params) => {
            return params.name;
          }
        },
        series: [
          {
            type: 'tree',
            data: [echartsData],
            top: '5%', left: '10%', bottom: '5%', right: '20%',
            symbolSize: 10,
            symbol: 'circle',
            roam: true,
            label: { position: 'left', verticalAlign: 'middle', align: 'right' },
            leaves: { label: { position: 'right', verticalAlign: 'middle', align: 'left' } },
            emphasis: { focus: 'descendant' },
            expandAndCollapse: true,
            animationDuration: 550,
            animationDurationUpdate: 750,
          },
        ],
      };
      chartInstance.setOption(option);
    }
  } catch (error) {
    message.error('加载组织架构数据失败');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  initChart();
  window.addEventListener('resize', resizeChart);
});

onBeforeUnmount(() => {
  if (chartInstance) {
    chartInstance.dispose();
  }
  window.removeEventListener('resize', resizeChart);
});

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize();
  }
};
</script>

<style scoped>
.page-container {
  background-color: #fff;
}
.chart-container {
  width: 100%;
  height: 600px;
  border: 1px solid #f0f0f0;
}
</style>