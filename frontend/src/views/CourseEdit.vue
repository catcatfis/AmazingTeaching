<template>
  <el-card>
    <template #header><div class="card-header"><span>{{ isEdit ? '编辑课程' : '新增课程' }}</span><el-button @click="$router.back()">返回</el-button></div></template>

    <el-form :model="form" label-width="100px" style="max-width:600px">
      <el-form-item label="课程名称"><el-input v-model="form.courseName" /></el-form-item>
      <el-form-item label="授课教师"><el-select v-model="form.teacherId"><el-option v-for="t in teachers" :key="t.id" :label="t.realName" :value="t.id" /></el-select></el-form-item>
      <el-form-item label="课程分类"><el-select v-model="form.category"><el-option label="编程开发" value="编程开发" /><el-option label="前端开发" value="前端开发" /><el-option label="数据科学" value="数据科学" /><el-option label="计算机基础" value="计算机基础" /><el-option label="数据库" value="数据库" /></el-select></el-form-item>
      <el-form-item label="难度"><el-select v-model="form.difficulty"><el-option label="入门" :value="1" /><el-option label="初级" :value="2" /><el-option label="中级" :value="3" /><el-option label="高级" :value="4" /></el-select></el-form-item>
      <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
      <el-form-item label="总课时"><el-input-number v-model="form.totalHours" :min="0" /></el-form-item>
      <el-form-item label="课程简介"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
      <el-form-item label="封面URL"><el-input v-model="form.coverUrl" /></el-form-item>
      <el-form-item><el-button type="primary" @click="handleSave">保存</el-button></el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { courseAPI, userAPI } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const isEdit = ref(!!route.params.id)
const teachers = ref([])
const form = reactive({
  courseName: '', teacherId: null, category: '', difficulty: 1, price: 0,
  totalHours: 0, description: '', coverUrl: ''
})

onMounted(async () => {
  const res = await userAPI.page({ page: 1, size: 100, userType: 2 })
  teachers.value = res.data.records
  if (isEdit.value) {
    const detail = await courseAPI.detail(route.params.id)
    Object.assign(form, detail.data)
  }
})

const handleSave = async () => {
  if (isEdit.value) {
    await courseAPI.update(route.params.id, form)
  } else {
    await courseAPI.add(form)
  }
  ElMessage.success('保存成功')
  router.push('/course')
}
</script>

<style scoped>.card-header { display: flex; justify-content: space-between; align-items: center; }</style>
