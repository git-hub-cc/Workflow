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

// 注册 ECharts 组件
echarts.use([TreeChart, TooltipComponent, LegendComponent, CanvasRenderer]);

const loading = ref(true);
const chartRef = ref(null);
let chartInstance = null;

// 将后端返回的组织树数据转换为 ECharts 需要的格式
const transformDataForEcharts = (data) => {
  return data.map(dept => ({
    name: dept.title,
    value: dept.key, // Using key as value for uniqueness
    // 部门节点的样式
    itemStyle: {
      color: '#1890ff',
      borderColor: '#1890ff',
    },
    label: {
      position: 'top',
      verticalAlign: 'middle',
      align: 'center',
      fontSize: 14,
      fontWeight: 'bold',
    },
    children: (dept.children || []).map(user => ({
      name: user.title.split(' (')[0], // 只显示名字
      value: user.value, // The actual user ID
      // 用户节点的样式
      itemStyle: {
        color: '#bae7ff',
        borderColor: '#1890ff',
      },
      label: {
        position: 'bottom',
        verticalAlign: 'middle',
        align: 'center',
      },
    })),
  }));
};

const initChart = async () => {
  try {
    const rawData = await getOrganizationTree();
    const echartsData = {
      name: '组织架构',
      // No value property for the root node
      itemStyle: {
        color: '#fa8c16',
        borderColor: '#fa8c16',
      },
      children: transformDataForEcharts(rawData),
    };

    // 等待DOM渲染完成
    await nextTick();

    if (chartRef.value) {
      chartInstance = echarts.init(chartRef.value);
      const option = {
        tooltip: {
          trigger: 'item',
          triggerOn: 'mousemove',
          // --- 【核心修正】 ---
          formatter: (params) => {
            const nodeData = params.data;
            // 检查 nodeData.value 是否存在且为字符串
            if (nodeData && nodeData.value && typeof nodeData.value === 'string') {
              // 检查是否为用户节点（value 不以 'dept_' 开头）
              if (!nodeData.value.startsWith('dept_')) {
                return `${nodeData.name}<br/>ID: ${nodeData.value}`;
              }
            }
            // 对于根节点和部门节点，只显示名称
            return params.name;
          }
        },
        series: [
          {
            type: 'tree',
            data: [echartsData],
            top: '5%',
            left: '10%',
            bottom: '5%',
            right: '20%',
            symbolSize: 10,
            symbol: 'circle',
            roam: true, // 开启缩放和平移
            label: {
              position: 'left',
              verticalAlign: 'middle',
              align: 'right',
            },
            leaves: {
              label: {
                position: 'right',
                verticalAlign: 'middle',
                align: 'left',
              },
            },
            emphasis: {
              focus: 'descendant',
            },
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
  // 添加窗口大小变化的监听
  window.addEventListener('resize', resizeChart);
});

onBeforeUnmount(() => {
  // 销毁图表实例并移除监听
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
  height: 600px; /* 或者 calc(100vh - ...px) */
  border: 1px solid #f0f0f0;
}
</style>