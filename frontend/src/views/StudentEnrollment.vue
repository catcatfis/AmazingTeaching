<template>
  <el-card>
    <template #header><span>选课管理</span></template>

    <el-form :inline="true" v-if="userStore.isAdmin()">
      <el-form-item><el-input v-model="studentId" placeholder="输入学生ID" /></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchEnrollments">查询</el-button></el-form-item>
    </el-form>
    <div v-else-if="userStore.isStudent()" style="margin-bottom:16px;color:#606266;">
      当前查看的是您自己的选课记录
    </div>

    <el-table :data="enrollments" stripe>
      <el-table-column prop="courseName" label="课程名称" />
      <el-table-column prop="progress" label="进度" width="150">
        <template #default="{ row }"><el-progress :percentage="Number(row.progress || 0)" /></template>
      </el-table-column>
      <el-table-column prop="enrollTime" label="报名时间" width="180" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'warning' : row.status === 2 ? 'success' : 'info'">{{ {1:'学习中',2:'已完成'}[row.status] || '学习中' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="score" label="成绩" width="80" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleUnenroll(row)">退课</el-button>
        </template>
      </el-table-column>
    </el-table>

    <h3 style="margin-top:24px">推荐课程</h3>
    <el-row :gutter="16">
      <el-col :span="6" v-for="c in recommendCourses" :key="c.id">
        <el-card shadow="hover">
          <p style="font-weight:bold">{{ c.courseName }}</p>
          <p style="color:#909399;font-size:14px">{{ c.category }} | ⭐{{ c.rating }}</p>
          <el-button size="small" type="primary" style="margin-top:8px" @click="handleEnroll(c.id)">选课</el-button>
        </el-card>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { courseAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const studentId = ref(userStore.userInfo?.userId || 4)
const enrollments = ref([])
const recommendCourses = ref([])

onMounted(async () => { await fetchEnrollments(); const rec = await courseAPI.recommend({ studentId: studentId.value, limit: 4 }); recommendCourses.value = rec.data })

const fetchEnrollments = async () => {
  if (!studentId.value) {
    ElMessage.error('输入ID为空，请重新输入ID')
    return
  }
  const res = await courseAPI.studentCourses(studentId.value)
  enrollments.value = res.data
}

const handleEnroll = async (courseId) => {
  await courseAPI.enroll({ studentId: studentId.value, courseId })
  ElMessage.success('选课成功')
  fetchEnrollments()
}

const handleUnenroll = async (row) => {
  await ElMessageBox.confirm(`确定要退课「${row.courseName}」吗？`, '提示', { type: 'warning' })
  await courseAPI.unenroll({ studentId: studentId.value, courseId: row.courseId })
  ElMessage.success('退课成功')
  fetchEnrollments()
}
</script>
