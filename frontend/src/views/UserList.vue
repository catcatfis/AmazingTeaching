<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>用户管理</span>
        <el-button type="primary" size="small" @click="openDialog()" v-if="isAdmin">新增用户</el-button>
      </div>
    </template>

    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item><el-input v-model="query.keyword" placeholder="用户名/姓名/手机号" clearable /></el-form-item>
      <el-form-item><el-select v-model="query.userType" placeholder="用户类型" clearable><el-option label="管理员" :value="1" /><el-option label="教师" :value="2" /><el-option label="学生" :value="3" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchData">查询</el-button></el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="userType" label="用户类型" width="100">
        <template #default="{ row }">{{ ['', '管理员', '教师', '学生'][row.userType] }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="240" v-if="isAdmin">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="handleReset(row.id)">重置密码</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:20px;justify-content:flex-end" v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码" v-if="!form.id"><el-input v-model="form.password" type="password" /></el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="用户类型"><el-select v-model="form.userType"><el-option label="管理员" :value="1" /><el-option label="教师" :value="2" /><el-option label="学生" :value="3" /></el-select></el-form-item>
        <el-form-item label="角色"><el-select v-model="selectedRole" placeholder="请选择角色"><el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { userAPI, roleAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin())

const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', userType: null })

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const form = reactive({})
const selectedRole = ref(null)
const roles = ref([])

onMounted(async () => { await fetchData(); const r = await roleAPI.list(); roles.value = r.data })

const fetchData = async () => {
  loading.value = true
  const res = await userAPI.page(query)
  tableData.value = res.data.records
  total.value = res.data.total
  loading.value = false
}

const openDialog = (row) => {
  dialogTitle.value = row ? '编辑用户' : '新增用户'
  Object.assign(form, row ? { ...row, password: undefined } : { username: '', password: '', realName: '', email: '', phone: '', userType: 3 })
  selectedRole.value = null
  dialogVisible.value = true
}

const handleSave = async () => {
  if (form.id) {
    await userAPI.update(form.id, form, selectedRole.value)
  } else {
    await userAPI.add(form, selectedRole.value)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}

const handleReset = async (id) => { await ElMessageBox.confirm('确认重置密码为 123456？'); await userAPI.resetPassword(id); ElMessage.success('密码已重置') }

const handleDelete = async (id) => { await ElMessageBox.confirm('确认删除该用户？'); await userAPI.delete(id); ElMessage.success('删除成功'); fetchData() }
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 16px; }
</style>
