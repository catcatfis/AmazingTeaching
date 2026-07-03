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
      <el-table-column label="评分" width="180">
        <template #default="{ row }">
          <div v-if="row.rating">
            <el-rate v-model="row.rating" disabled text-color="#ff9900" />
            <span v-if="row.ratingContent" style="font-size:12px;color:#909399;">{{ row.ratingContent }}</span>
          </div>
          <span v-else style="color:#c0c4cc;">未评分</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" type="warning" @click="openRateDialog(row)">评分</el-button>
          <el-button size="small" type="danger" @click="handleUnenroll(row)">退课</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 评分对话框 -->
    <el-dialog title="课程评分" v-model="rateDialogVisible" width="400px">
      <p style="margin-bottom:16px;">课程名称：{{ currentCourse.courseName }}</p>
      <el-form label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="rateForm.rating" show-text :texts="['很差', '较差', '一般', '不错', '非常好']" />
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="rateForm.ratingContent" type="textarea" :rows="3" placeholder="请输入您的评价（可选）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRating">确定</el-button>
      </template>
    </el-dialog>

    <h3 style="margin-top:24px">推荐课程</h3>
    <el-row :gutter="16">
      <el-col :span="6" v-for="c in recommendCourses" :key="c.id">
        <el-card shadow="hover">
          <p style="font-weight:bold">{{ c.courseName }}</p>
          <p style="color:#909399;font-size:14px">{{ c.category }} | ⭐{{ c.rating }}</p>
          <el-button v-if="enrolledCourseIds.includes(c.id)" size="small" type="info" disabled style="margin-top:8px">已选</el-button>
          <el-button v-else size="small" type="primary" style="margin-top:8px" @click="handleEnroll(c.id)">选课</el-button>
        </el-card>
      </el-col>
    </el-row>

    <h3 style="margin-top:24px">所有课程</h3>
    <el-row :gutter="16">
      <el-col :span="6" v-for="c in allCourses" :key="c.id">
        <el-card shadow="hover">
          <p style="font-weight:bold">{{ c.courseName }}</p>
          <p style="color:#909399;font-size:14px">{{ c.category }} | ⭐{{ c.rating }} | {{ c.teacherName || '未知教师' }}</p>
          <el-button v-if="enrolledCourseIds.includes(c.id)" size="small" type="info" disabled style="margin-top:8px">已选</el-button>
          <el-button v-else size="small" type="primary" style="margin-top:8px" @click="handleEnroll(c.id)">选课</el-button>
        </el-card>
      </el-col>
    </el-row>
    <el-pagination
      style="margin-top:16px;justify-content:center;"
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next, jumper"
      @current-change="handlePageChange"
    />
  </el-card>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { courseAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const studentId = ref(userStore.userInfo?.userId || 4)
const enrollments = ref([])
const recommendCourses = ref([])
const allCourses = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
// 只包含有效选课状态（status!=0）的课程ID
const enrolledCourseIds = computed(() => enrollments.value.filter(e => e.status !== 0).map(e => e.courseId))

// 评分相关状态
const rateDialogVisible = ref(false)
const currentCourse = ref({})
const rateForm = ref({
  rating: 3,
  ratingContent: ''
})

onMounted(async () => { await fetchEnrollments(); const rec = await courseAPI.recommend({ studentId: studentId.value, limit: 4 }); recommendCourses.value = rec.data; await fetchAllCourses() })

const fetchEnrollments = async () => {
  if (!studentId.value) {
    ElMessage.error('输入ID为空，请重新输入ID')
    return
  }
  const res = await courseAPI.studentCourses(studentId.value)
  enrollments.value = res.data
}

const fetchAllCourses = async () => {
  const res = await courseAPI.page({
    page: currentPage.value,
    size: pageSize.value,
    status: 1 // 只查询已发布的课程
  })
  allCourses.value = res.data.records
  total.value = res.data.total
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchAllCourses()
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
  // 刷新所有相关数据（退课会清除评分，需要更新课程平均评分）
  fetchEnrollments()
  fetchRecommendCourses()
  fetchAllCourses()
}

// 打开评分对话框
const openRateDialog = (row) => {
  currentCourse.value = row
  rateForm.value = {
    rating: row.rating || 3,
    ratingContent: row.ratingContent || ''
  }
  rateDialogVisible.value = true
}

// 提交评分
const submitRating = async () => {
  if (!rateForm.value.rating) {
    ElMessage.warning('请选择评分')
    return
  }
  
  await courseAPI.rate({
    studentId: studentId.value,
    courseId: currentCourse.value.courseId,
    rating: rateForm.value.rating,
    ratingContent: rateForm.value.ratingContent
  })
  
  ElMessage.success('评分成功')
  rateDialogVisible.value = false
  // 刷新所有相关数据
  fetchEnrollments()
  fetchRecommendCourses()
  fetchAllCourses()
}

// 刷新推荐课程
const fetchRecommendCourses = async () => {
  const rec = await courseAPI.recommend({ studentId: studentId.value, limit: 4 })
  recommendCourses.value = rec.data
}
</script>
