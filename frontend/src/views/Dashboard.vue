<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" :body-style="{ padding: '20px' }">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: card.color }"><el-icon :size="28"><component :is="card.icon" /></el-icon></div>
            <div class="stat-info">
              <p class="stat-label">{{ card.label }}</p>
              <p class="stat-value">{{ card.value }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="16">
        <el-card>
          <template #header>📈 选课趋势（近12个月）</template>
          <div ref="trendChart" style="height:350px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>🍩 课程分类分布</template>
          <div ref="categoryChart" style="height:350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header>📊 用户类型分布</template>
          <div ref="userTypeChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>🔥 热门课程 Top5</template>
          <el-table :data="topCourses" stripe size="small">
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="name" label="课程名称" />
            <el-table-column prop="studentCount" label="学生数" width="100" />
            <el-table-column prop="rating" label="评分" width="80" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { dashboardAPI } from '@/api'

const statCards = ref([
  { label: '用户总数', value: 0, icon: 'User', color: '#409EFF' },
  { label: '课程总数', value: 0, icon: 'Reading', color: '#67C23A' },
  { label: '选课人次', value: 0, icon: 'Notebook', color: '#E6A23C' },
  { label: '考试平均分', value: 0, icon: 'EditPen', color: '#F56C6C' }
])

const topCourses = ref([])
const trendChart = ref(null)
const categoryChart = ref(null)
const userTypeChart = ref(null)

onMounted(async () => {
  const overview = await dashboardAPI.overview()
  const data = overview.data
  statCards.value[0].value = data.userStats?.totalUsers || 0
  statCards.value[1].value = data.totalCourses || 0
  statCards.value[2].value = data.totalEnrollments || 0
  statCards.value[3].value = data.avgScore || 0

  // 选课趋势
  const trendData = await dashboardAPI.enrollmentTrend()
  const trendMonths = trendData.data.map(i => i.month)
  const trendCounts = trendData.data.map(i => i.count)
  const trend = echarts.init(trendChart.value)
  trend.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { data: trendMonths },
    yAxis: {},
    series: [{ type: 'line', data: trendCounts, smooth: true, areaStyle: { color: 'rgba(64,158,255,0.2)' } }]
  })

  // 分类分布
  const catData = await dashboardAPI.categoryPie()
  const cat = echarts.init(categoryChart.value)
  cat.setOption({
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['40%', '70%'], data: catData.data, label: { formatter: '{b}\n{d}%' } }]
  })

  // 用户类型
  const userData = await dashboardAPI.userTypePie()
  const ut = echarts.init(userTypeChart.value)
  ut.setOption({
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '70%', data: userData.data, label: { formatter: '{b}: {c}' } }]
  })

  // 热门课程
  const topData = await dashboardAPI.topCourses()
  topCourses.value = topData.data
})
</script>

<style scoped>
.stats-row { margin-bottom: 0; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; }
.stat-label { color: #909399; font-size: 14px; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; margin-top: 4px; }
</style>
