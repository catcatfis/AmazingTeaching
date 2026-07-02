<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="card-header">
        <span>课程详情</span>
        <div>
          <el-button v-if="course && course.status === 0" type="success" @click="handlePublish">发布课程</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </div>
    </template>

    <div v-if="course">
      <h1>{{ course.courseName }}</h1>
      <el-descriptions :column="3" border style="margin-top:20px">
        <el-descriptions-item label="授课教师">{{ course.teacherName }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ course.category }}</el-descriptions-item>
        <el-descriptions-item label="难度">{{ ['', '入门', '初级', '中级', '高级'][course.difficulty] }}</el-descriptions-item>
        <el-descriptions-item label="价格">¥{{ course.price }}</el-descriptions-item>
        <el-descriptions-item label="总课时">{{ course.totalHours }}小时</el-descriptions-item>
        <el-descriptions-item label="评分">⭐ {{ course.rating }}/5</el-descriptions-item>
        <el-descriptions-item label="学生数">{{ course.studentCount }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="course.status === 1 ? 'success' : 'info'">{{ course.status === 1 ? '已发布' : '草稿' }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ course.publishTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h3 style="margin-top:24px">课程简介</h3>
      <p style="color:#606266;line-height:1.8">{{ course.description }}</p>

      <h3 style="margin-top:24px">推荐课程</h3>
      <el-row :gutter="16">
        <el-col :span="6" v-for="c in recommendCourses" :key="c.id">
          <el-card shadow="hover" @click="$router.push(`/course/${c.id}`)" style="cursor:pointer">
            <p style="font-weight:bold">{{ c.courseName }}</p>
            <p style="color:#909399;font-size:14px">{{ c.category }} | ⭐{{ c.rating }}</p>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { courseAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()
const course = ref(null)
const recommendCourses = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  const res = await courseAPI.detail(route.params.id)
  course.value = res.data

  const recRes = await courseAPI.recommend({ studentId: userStore.userInfo?.userId || 4, limit: 4 })
  recommendCourses.value = recRes.data.filter(c => c.id !== course.value.id)
  loading.value = false
})

const handlePublish = async () => {
  await ElMessageBox.confirm('确认发布该课程？发布后将对学生可见', '发布确认', { type: 'success' })
  await courseAPI.publish(course.value.id)
  ElMessage.success('发布成功')
  course.value.status = 1
  course.value.publishTime = new Date().toISOString().replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>.card-header { display: flex; justify-content: space-between; align-items: center; }</style>
