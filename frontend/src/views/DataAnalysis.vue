<template>
  <el-card>
    <template #header><span>📊 数据分析</span></template>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card><template #header>课程分类分布</template><div ref="catChart" style="height:350px"></div></el-card>
      </el-col>
      <el-col :span="12">
        <el-card><template #header>课程难度分布</template><div ref="diffChart" style="height:350px"></div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card><template #header>选课趋势（近12个月）</template><div ref="trendChart" style="height:350px"></div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card><template #header>考试通过率趋势</template><div ref="passChart" style="height:350px"></div></el-card>
      </el-col>
      <el-col :span="12">
        <el-card><template #header>用户类型分布</template><div ref="userChart" style="height:350px"></div></el-card>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { dashboardAPI } from '@/api'

const catChart = ref(null)
const diffChart = ref(null)
const trendChart = ref(null)
const passChart = ref(null)
const userChart = ref(null)

onMounted(async () => {
  // 分类分布 - 饼图
  const catData = await dashboardAPI.categoryPie()
  const cat = echarts.init(catChart.value)
  cat.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 10 },
    series: [{ type: 'pie', radius: ['40%', '70%'], data: catData.data, label: { formatter: '{b}: {c}' }, emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } } }]
  })

  // 难度分布 - 柱状图
  const diffData = await dashboardAPI.difficultyBar()
  const diff = echarts.init(diffChart.value)
  diff.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: diffData.data.map(d => d.name) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: diffData.data.map(d => d.value), itemStyle: { color: '#67C23A' }, barWidth: '50%' }]
  })

  // 选课趋势 - 折线图
  const trendData = await dashboardAPI.enrollmentTrend()
  const trend = echarts.init(trendChart.value)
  trend.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trendData.data.map(d => d.month) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', data: trendData.data.map(d => d.count), smooth: true, areaStyle: { color: 'rgba(64,158,255,0.2)' }, itemStyle: { color: '#409EFF' } }]
  })

  // 考试通过率 - 折线图
  const passData = await dashboardAPI.examPassTrend()
  const pass = echarts.init(passChart.value)
  pass.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: passData.data.map(d => d.month) },
    yAxis: { type: 'value', name: '通过率(%)' },
    series: [{ type: 'line', data: passData.data.map(d => d.passRate), smooth: true, itemStyle: { color: '#E6A23C' }, areaStyle: { color: 'rgba(230,162,60,0.2)' } }]
  })

  // 用户分布 - 饼图
  const userData = await dashboardAPI.userTypePie()
  const user = echarts.init(userChart.value)
  user.setOption({
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '70%', data: userData.data, label: { formatter: '{b}: {c}人' }, emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } } }]
  })
})
</script>
