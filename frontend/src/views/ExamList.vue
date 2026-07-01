<template>
  <el-card>
    <template #header><div class="card-header"><span>考试管理</span><el-button type="primary" size="small" @click="openDialog()">创建考试</el-button></div></template>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="examName" label="考试名称" />
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
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '进行中' : row.status === 2 ? '已结束' : '未发布' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="success" @click="$router.push(`/exam/take/${row.id}`)">参加考试</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:20px;justify-content:flex-end" v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="考试名称"><el-input v-model="form.examName" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.examType"><el-option label="随堂测验" :value="1" /><el-option label="章节测试" :value="2" /><el-option label="期末考试" :value="3" /></el-select></el-form-item>
        <el-form-item label="总分"><el-input-number v-model="form.totalScore" :min="0" /></el-form-item>
        <el-form-item label="及格线"><el-input-number v-model="form.passScore" :min="0" /></el-form-item>
        <el-form-item label="时长(分钟)"><el-input-number v-model="form.duration" :min="0" /></el-form-item>
        <el-form-item label="难度"><el-select v-model="form.difficulty"><el-option label="简单" :value="1" /><el-option label="中等" :value="2" /><el-option label="困难" :value="3" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { examAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const dialogVisible = ref(false)
const dialogTitle = ref('创建考试')
const form = reactive({ examName: '', examType: 1, totalScore: 100, passScore: 60, duration: 60, difficulty: 1 })

const fetchData = async () => { loading.value = true; const res = await examAPI.page(query); tableData.value = res.data.records; total.value = res.data.total; loading.value = false }

onMounted(fetchData)

const openDialog = (row) => {
  dialogTitle.value = row ? '编辑考试' : '创建考试'
  Object.assign(form, row || { examName: '', examType: 1, totalScore: 100, passScore: 60, duration: 60, difficulty: 1 })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (form.id) { await examAPI.update(form.id, form) } else { await examAPI.add(form) }
  ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
}

const handleDelete = async (id) => { await ElMessageBox.confirm('确认删除该考试？'); await examAPI.delete(id); ElMessage.success('删除成功'); fetchData() }
</script>

<style scoped>.card-header { display: flex; justify-content: space-between; align-items: center; }</style>
