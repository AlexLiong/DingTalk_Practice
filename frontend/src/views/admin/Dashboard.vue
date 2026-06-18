<template>
  <div class="page-card dashboard-page">
    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card" v-for="item in statCards" :key="item.key">
        <div class="stat-icon-box" :style="{ background: item.bg }">
          <component :is="item.icon" class="stat-icon" :style="{ color: item.color }" />
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ overview[item.key] ?? '-' }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-row">
      <div class="chart-box">
        <h3 class="chart-title">近7日消息趋势</h3>
        <div ref="barRef" class="chart-container"></div>
      </div>
      <div class="chart-box">
        <h3 class="chart-title">部门人数分布</h3>
        <div ref="pieRef" class="chart-container"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { apiDashOverview, apiDashDept, apiDashTrend } from '../../api'

// 统计卡片配置
const statCards = [
  { key: 'userCount', label: '用户数', icon: 'el-icon-user', bg: '#e6f7ff', color: '#1677ff' },
  { key: 'messageCount', label: '消息数', icon: 'el-icon-chat-dot-round', bg: '#fff7e6', color: '#fa8c16' },
  { key: 'sessionCount', label: '会话数', icon: 'el-icon-comment', bg: '#f6ffed', color: '#52c41a' },
  { key: 'todoCount', label: '待办数', icon: 'el-icon-document-checked', bg: '#fff1f0', color: '#ff4d4f' },
  { key: 'approvalCount', label: '审批数', icon: 'el-icon-stamp', bg: '#f9f0ff', color: '#722ed1' },
]

const overview = ref({})
const barRef = ref(null)
const pieRef = ref(null)
let barChart = null
let pieChart = null
let resizeObserver = null

onMounted(async () => {
  // 获取总览
  try { overview.value = await apiDashOverview() } catch { overview.value = {} }

  // 初始化图表
  barChart = echarts.init(barRef.value)
  pieChart = echarts.init(pieRef.value)

  // 消息趋势
  try {
    const trend = await apiDashTrend()
    const dates = Array.isArray(trend) ? trend.map(t => t.date || t.day) : []
    const counts = Array.isArray(trend) ? trend.map(t => t.count ?? t.value ?? 0) : []
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { top: 30, right: 20, bottom: 30, left: 50 },
      xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        type: 'bar',
        data: counts,
        itemStyle: { color: '#1677ff', borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 36,
      }],
    })
  } catch {
    barChart.setOption({ title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#ccc', fontSize: 14 } } })
  }

  // 部门分布
  try {
    const dept = await apiDashDept()
    const pieData = Array.isArray(dept) ? dept.map(d => ({ name: d.name || d.deptName, value: d.count ?? d.value ?? 0 })) : []
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0, type: 'scroll' },
      series: [{
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        label: { formatter: '{b}\n{d}%', fontSize: 11 },
        data: pieData,
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.2)' } },
      }],
    })
  } catch {
    pieChart.setOption({ title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#ccc', fontSize: 14 } } })
  }

  // 响应式
  resizeObserver = new ResizeObserver(() => {
    barChart?.resize()
    pieChart?.resize()
  })
  if (barRef.value) resizeObserver.observe(barRef.value)
  if (pieRef.value) resizeObserver.observe(pieRef.value)
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard-page { background: #fff; border-radius: 10px; padding: 20px; }

/* 统计卡片 */
.stat-row { display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
.stat-card {
  flex: 1;
  min-width: 150px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
}
.stat-icon-box {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-icon { width: 24px; height: 24px; }
.stat-num { font-size: 24px; font-weight: 700; color: #1d2129; }
.stat-label { font-size: 13px; color: #8c8c8c; margin-top: 2px; }

/* 图表 */
.chart-row { display: flex; gap: 20px; flex-wrap: wrap; }
.chart-box {
  flex: 1;
  min-width: 360px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 16px;
}
.chart-title { margin: 0 0 10px; font-size: 15px; font-weight: 600; color: #1d2129; }
.chart-container { width: 100%; height: 400px; }
</style>
