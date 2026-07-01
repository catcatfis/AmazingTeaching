<template>
  <el-card>
    <template #header><span>权限管理</span></template>
    <el-tree :data="treeData" node-key="id" default-expand-all :props="{ children: 'children', label: 'permName' }">
      <template #default="{ node, data }">
        <span>
          {{ data.permName }}
          <el-tag size="small" :type="data.permType === 1 ? 'info' : data.permType === 2 ? 'success' : 'warning'" style="margin-left:8px">
            {{ ['未知', '菜单', '按钮', '接口'][data.permType] }}
          </el-tag>
          <span style="color:#909399;margin-left:8px;font-size:12px">{{ data.permCode }}</span>
        </span>
      </template>
    </el-tree>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { permissionAPI } from '@/api'

const treeData = ref([])

onMounted(async () => {
  const res = await permissionAPI.tree()
  treeData.value = res.data
})
</script>
