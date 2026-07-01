<template>
  <el-card>
    <template #header><div class="card-header"><span>章节管理 - {{ courseName }}</span><el-button type="primary" size="small" @click="openDialog()">新增章节</el-button><el-button @click="$router.back()">返回</el-button></div></template>

    <el-table :data="chapters" row-key="id" default-expand-all>
      <el-table-column prop="chapterName" label="章节名称" />
      <el-table-column prop="chapterType" label="类型" width="100">
        <template #default="{ row }">{{ ['', '视频', '文档', '测验'][row.chapterType] }}</template>
      </el-table-column>
      <el-table-column prop="duration" label="时长(分钟)" width="100" />
      <el-table-column prop="isFree" label="免费" width="80">
        <template #default="{ row }"><el-tag :type="row.isFree ? 'success' : 'info'">{{ row.isFree ? '是' : '否' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="章节名称"><el-input v-model="form.chapterName" /></el-form-item>
        <el-form-item label="父章节"><el-select v-model="form.parentId" clearable placeholder="不选为一级章节"><el-option v-for="c in chapters" :key="c.id" :label="c.chapterName" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.chapterType"><el-option label="视频" :value="1" /><el-option label="文档" :value="2" /><el-option label="测验" :value="3" /></el-select></el-form-item>
        <el-form-item label="时长(分钟)"><el-input-number v-model="form.duration" :min="0" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="免费试看"><el-switch v-model="form.isFree" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { chapterAPI, courseAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const courseId = route.params.courseId
const chapters = ref([])
const courseName = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('新增章节')
const form = reactive({ chapterName: '', courseId: Number(courseId), parentId: null, chapterType: 1, duration: 0, sortOrder: 0, isFree: 0, content: '' })

onMounted(async () => {
  const c = await courseAPI.detail(courseId)
  courseName.value = c.data.courseName
  fetchChapters()
})

const fetchChapters = async () => {
  const res = await chapterAPI.tree(courseId)
  chapters.value = res.data
}

const openDialog = (row) => {
  dialogTitle.value = row ? '编辑章节' : '新增章节'
  Object.assign(form, row ? { ...row } : { chapterName: '', courseId: Number(courseId), parentId: null, chapterType: 1, duration: 0, sortOrder: 0, isFree: 0, content: '' })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (form.id) { await chapterAPI.update(form.id, form) } else { await chapterAPI.add(form) }
  ElMessage.success('保存成功'); dialogVisible.value = false; fetchChapters()
}

const handleDelete = async (id) => { await ElMessageBox.confirm('确认删除该章节？'); await chapterAPI.delete(id); ElMessage.success('删除成功'); fetchChapters() }
</script>

<style scoped>.card-header { display: flex; gap: 12px; align-items: center; }</style>
