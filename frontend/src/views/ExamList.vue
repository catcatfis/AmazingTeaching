<template>
  <el-card>
    <template #header><div class="card-header"><span>{{ userStore.isStudent() ? '参加考试' : '考试管理' }}</span><el-button v-if="!userStore.isStudent()" type="primary" size="small" @click="openDialog()">创建考试</el-button></div></template>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="examName" label="考试名称" width="180" />
      <el-table-column prop="courseName" label="所属课程" width="150">
        <template #default="{ row }">{{ row.courseName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="examType" label="类型" width="100">
        <template #default="{ row }">{{ ['', '随堂测验', '章节测试', '期末考试'][row.examType] }}</template>
      </el-table-column>
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column prop="passScore" label="及格线" width="80" />
      <el-table-column prop="duration" label="时长(分)" width="100" />
      <el-table-column prop="difficulty" label="难度" width="80">
        <template #default="{ row }">{{ ['', '简单', '中等', '困难'][row.difficulty] }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '进行中' : row.status === 2 ? '已结束' : '未开始' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button v-if="userStore.isAdmin() || row.courseId && courses.some(c => c.id === row.courseId)" size="small" @click="openDialog(row)">编辑</el-button>
          <template v-if="userStore.isStudent()">
            <template v-if="isSubmitted(row.id)">
              <el-tag size="small" :type="examScores[row.id]?.isPassed === 1 ? 'success' : 'danger'">
                得分: {{ examScores[row.id]?.score ?? '-' }}/{{ row.totalScore }}
              </el-tag>
            </template>
            <el-button v-else-if="row.status === 1" size="small" type="success" @click="$router.push(`/exam/take/${row.id}`)">参加考试</el-button>
            <el-button v-else-if="row.status === 0" size="small" type="info" disabled>未开始</el-button>
            <el-button v-else size="small" type="info" disabled>已结束</el-button>
          </template>
          <el-button v-if="userStore.isAdmin() || row.courseId && courses.some(c => c.id === row.courseId)" size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:20px;justify-content:flex-end" v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="所属课程" required>
          <el-select v-model="form.courseId" placeholder="请选择课程" style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试名称"><el-input v-model="form.examName" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.examType"><el-option label="随堂测验" :value="1" /><el-option label="章节测试" :value="2" /><el-option label="期末考试" :value="3" /></el-select></el-form-item>
        <el-form-item label="总分"><el-input-number v-model="form.totalScore" :min="0" /></el-form-item>
        <el-form-item label="及格线"><el-input-number v-model="form.passScore" :min="0" /></el-form-item>
        <el-form-item label="时长(分钟)"><el-input-number v-model="form.duration" :min="0" /></el-form-item>
        <el-form-item label="难度"><el-select v-model="form.difficulty"><el-option label="简单" :value="1" /><el-option label="中等" :value="2" /><el-option label="困难" :value="3" /></el-select></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item label="结束时间"><el-input :value="endTimeDisplay" disabled /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { examAPI, courseAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const dialogVisible = ref(false)
const dialogTitle = ref('创建考试')
const form = reactive({ examName: '', examType: 1, totalScore: 100, passScore: 60, duration: 60, difficulty: 1, startTime: null, courseId: null })
const submittedExamIds = ref([]) // 学生已提交的考试ID列表
const examScores = ref({}) // 学生考试分数 { examId: { score, isPassed } }
const courses = ref([]) // 课程列表
const courseMap = ref({}) // 课程ID->课程名称映射

// 计算结束时间显示
const endTimeDisplay = computed(() => {
  if (!form.startTime || !form.duration) return ''
  const start = new Date(form.startTime)
  if (isNaN(start.getTime())) return ''
  const end = new Date(start.getTime() + form.duration * 60 * 1000)
  const year = end.getFullYear()
  const month = String(end.getMonth() + 1).padStart(2, '0')
  const day = String(end.getDate()).padStart(2, '0')
  const hours = String(end.getHours()).padStart(2, '0')
  const minutes = String(end.getMinutes()).padStart(2, '0')
  const seconds = String(end.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
})

// 加载课程列表
const loadCourses = async () => {
  try {
    // 管理员可以创建任何课程的考试，教师只能创建自己课程的考试
    if (userStore.isAdmin()) {
      // 管理员：加载所有课程
      const res = await courseAPI.page({ page: 1, size: 100 })
      courses.value = res.data.records || []
    } else if (userStore.isTeacher()) {
      // 教师：只加载自己的课程
      const teacherId = userStore.userInfo?.userId
      if (teacherId) {
        const res = await courseAPI.teacherCourses(teacherId)
        courses.value = res.data || []
      }
    }
    // 构建课程ID到课程名称的映射
    courseMap.value = {}
    courses.value.forEach(c => { courseMap.value[c.id] = c.courseName })
  } catch (e) {
    console.error('加载课程列表失败:', e)
  }
}

const fetchData = async () => {
  loading.value = true

  // 教师只获取自己课程的考试
  if (userStore.isTeacher()) {
    const teacherCourseIds = courses.value.map(c => c.id)
    const allExams = []
    for (const cid of teacherCourseIds) {
      try {
        const examRes = await examAPI.page({ page: 1, size: 100, courseId: cid })
        allExams.push(...(examRes.data.records || []))
      } catch (e) {
        console.error('获取课程考试失败:', e)
      }
    }
    // 按创建时间倒序排列
    allExams.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
    tableData.value = allExams.slice((query.page - 1) * query.size, query.page * query.size)
    total.value = allExams.length
  } else {
    // 管理员获取所有考试
    const res = await examAPI.page(query)
    tableData.value = res.data.records
    total.value = res.data.total
  }

  // 填充课程名称
  tableData.value.forEach(exam => {
    exam.courseName = courseMap.value[exam.courseId] || '-'
  })

  // 如果是学生，加载已提交的考试ID列表和分数
  if (userStore.isStudent()) {
    try {
      const studentId = userStore.userInfo?.userId
      if (studentId) {
        const submittedRes = await examAPI.submittedExamIds(studentId)
        submittedExamIds.value = submittedRes.data || []
        // 获取考试记录详情（包含分数）
        const recordsRes = await examAPI.records({ studentId: studentId })
        const records = recordsRes.data || []
        // 构建 examId -> { score, isPassed } 映射
        examScores.value = {}
        records.forEach(record => {
          examScores.value[record.examId] = {
            score: record.score,
            isPassed: record.isPassed
          }
        })
      }
    } catch (e) {
      console.error('获取已提交考试失败:', e)
    }
  }
  loading.value = false
}

onMounted(async () => {
  await loadCourses()
  fetchData()
})

// 检查考试是否已提交
const isSubmitted = (examId) => submittedExamIds.value.includes(examId)

const openDialog = (row) => {
  dialogTitle.value = row ? '编辑考试' : '创建考试'
  Object.assign(form, row || { examName: '', examType: 1, totalScore: 100, passScore: 60, duration: 60, difficulty: 1, startTime: null, courseId: null })
  dialogVisible.value = true
}

const handleSave = async () => {
  // 计算结束时间
  const dataToSend = { ...form }
  if (dataToSend.startTime && dataToSend.duration) {
    const start = new Date(dataToSend.startTime)
    if (!isNaN(start.getTime())) {
      const end = new Date(start.getTime() + dataToSend.duration * 60 * 1000)
      const year = end.getFullYear()
      const month = String(end.getMonth() + 1).padStart(2, '0')
      const day = String(end.getDate()).padStart(2, '0')
      const hours = String(end.getHours()).padStart(2, '0')
      const minutes = String(end.getMinutes()).padStart(2, '0')
      const seconds = String(end.getSeconds()).padStart(2, '0')
      dataToSend.endTime = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }
  }
  
  // 只发送必要的字段，避免不必要的字段导致问题
  const dataToSave = {
    id: dataToSend.id,
    examName: dataToSend.examName || '',
    examType: dataToSend.examType || 1,
    totalScore: dataToSend.totalScore || 100,
    passScore: dataToSend.passScore || 60,
    duration: dataToSend.duration || 60,
    difficulty: dataToSend.difficulty || 1,
    startTime: dataToSend.startTime || null,
    endTime: dataToSend.endTime || null,
    courseId: dataToSend.courseId || null,
    chapterId: dataToSend.chapterId || null,
    questionsJson: dataToSend.questionsJson || null,
    status: dataToSend.status || 0
  }
  
  try {
    if (form.id) { await examAPI.update(form.id, dataToSave) } else { await examAPI.add(dataToSave) }
    ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败，请检查网络连接或联系管理员')
  }
}

const handleDelete = async (id) => { await ElMessageBox.confirm('确认删除该考试？'); await examAPI.delete(id); ElMessage.success('删除成功'); fetchData() }
</script>

<style scoped>.card-header { display: flex; justify-content: space-between; align-items: center; }</style>
