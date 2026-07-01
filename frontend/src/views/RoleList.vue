<template>
  <el-card>
    <template #header><div class="card-header"><span>角色管理</span><el-button type="primary" size="small" @click="openDialog()">新增角色</el-button></div></template>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roleName" label="角色名称" />
      <el-table-column prop="roleCode" label="角色编码" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="openPermDialog(row)">分配权限</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="form.roleCode" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>

    <el-dialog title="分配权限" v-model="permDialogVisible" width="400px">
      <el-tree :data="permTree" show-checkbox node-key="id" ref="permTreeRef" default-expand-all />
      <template #footer><el-button @click="permDialogVisible = false">取消</el-button><el-button type="primary" @click="handleAssignPerms">确定</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { roleAPI, permissionAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const form = reactive({})
const permDialogVisible = ref(false)
const permTree = ref([])
const permTreeRef = ref(null)
const currentRoleId = ref(null)

const fetchData = async () => { loading.value = true; const r = await roleAPI.list(); tableData.value = r.data; loading.value = false }

onMounted(fetchData)

const openDialog = (row) => {
  dialogTitle.value = row ? '编辑角色' : '新增角色'
  Object.assign(form, row || { roleName: '', roleCode: '', description: '', sortOrder: 0 })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (form.id) { await roleAPI.update(form.id, form) } else { await roleAPI.add(form) }
  ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
}

const handleDelete = async (id) => { await ElMessageBox.confirm('确认删除该角色？'); await roleAPI.delete(id); ElMessage.success('删除成功'); fetchData() }

const openPermDialog = async (row) => {
  currentRoleId.value = row.id
  const treeRes = await permissionAPI.tree()
  permTree.value = treeRes.data
  const permRes = await roleAPI.getPermissions(row.id)
  permDialogVisible.value = true
  setTimeout(() => permTreeRef.value.setCheckedKeys(permRes.data), 100)
}

const handleAssignPerms = async () => {
  const keys = permTreeRef.value.getCheckedKeys()
  await roleAPI.assignPermissions(currentRoleId.value, keys)
  ElMessage.success('权限分配成功'); permDialogVisible.value = false
}
</script>

<style scoped>.card-header { display: flex; justify-content: space-between; align-items: center; }</style>
