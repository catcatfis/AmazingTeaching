<template>
  <el-card>
    <template #header><div class="card-header"><span>课程管理</span><el-button type="primary" size="small" @click="$router.push('/course-edit')">新增课程</el-button></div></template>

    <el-form :inline="true" :model="query">
      <el-form-item><el-input v-model="query.keyword" placeholder="课程名称" clearable /></el-form-item>
      <el-form-item><el-select v-model="query.category" placeholder="分类" clearable><el-option label="编程开发" value="编程开发" /><el-option label="前端开发" value="前端开发" /><el-option label="数据科学" value="数据科学" /><el-option label="计算机基础" value="计算机基础" /><el-option label="数据库" value="数据库" /></el-select></el-form-item>
      <el-form-item><el-select v-model="query.difficulty" placeholder="难度" clearable><el-option label="入门" :value="1" /><el-option label="初级" :value="2" /><el-option label="中级" :value="3" /><el-option label="高级" :value="4" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchData">查询</el-button></el-form-item>
    </el-form>

    <el-row :gutter="20">
      <el-col :span="8" v-for="course in tableData" :key="course.id" style="margin-bottom:20px">
        <el-card shadow="hover" @click="$router.push(`/course/${course.id}`)" style="cursor:pointer">
          <template #header>
            <div class="course-header">
              <span class="course-name">{{ course.courseName }}</span>
              <el-tag size="small">{{ course.category }}</el-tag>
            </div>
          </template>
          <p class="course-desc">{{ course.description?.substring(0, 80) }}...</p>
          <div class="course-footer">
            <span>👤 {{ course.teacherName || '未知教师' }}</span>
            <span>⭐ {{ course.rating }}/5</span>
            <span>👨‍🎓 {{ course.studentCount }}人</span>
          </div>
          <div class="course-actions" @click.stop>
            <el-button v-if="userStore.isAdmin() || course.teacherId === userStore.userInfo?.userId" size="small" @click="$router.push(`/course-edit/${course.id}`)">编辑</el-button>
            <el-button size="small" @click="$router.push(`/chapter/${course.id}`)">章节</el-button>
            <el-button v-if="course.status === 0 && (userStore.isAdmin() || course.teacherId === userStore.userInfo?.userId)" size="small" type="success" @click="handlePublish(course.id)">发布</el-button>
            <el-tag v-else-if="course.status === 1" size="small" type="success">已发布</el-tag>
            <el-button v-if="userStore.isAdmin() || course.teacherId === userStore.userInfo?.userId" size="small" type="danger" @click="handleDelete(course.id)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-pagination style="margin-top:20px;justify-content:center" v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { courseAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 9, keyword: '', category: '', difficulty: null })

const fetchData = async () => {
  const res = await courseAPI.page(query)
  tableData.value = res.data.records
  total.value = res.data.total
}

onMounted(fetchData)

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该课程？')
  await courseAPI.delete(id)
  ElMessage.success('删除成功')
  fetchData()
}

const handlePublish = async (id) => {
  await ElMessageBox.confirm('确认发布该课程？发布后将对学生可见', '发布确认', { type: 'success' })
  await courseAPI.publish(id)
  ElMessage.success('发布成功')
  fetchData()
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.course-header { display: flex; justify-content: space-between; align-items: center; }
.course-name { font-weight: bold; font-size: 16px; }
.course-desc { color: #909399; font-size: 13px; margin: 8px 0; }
.course-footer { display: flex; gap: 16px; color: #606266; font-size: 13px; margin-bottom: 8px; }
.course-actions { display: flex; gap: 8px; justify-content: flex-end; border-top: 1px solid #ebeef5; padding-top: 10px; }
</style>
