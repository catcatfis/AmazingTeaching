<template>
  <el-card>
    <template #header><span>操作日志</span></template>

    <el-form :inline="true" :model="query">
      <el-form-item><el-input v-model="query.username" placeholder="操作用户" clearable /></el-form-item>
      <el-form-item><el-input v-model="query.module" placeholder="操作模块" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchData">查询</el-button></el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="操作用户" width="120" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="operation" label="操作类型" width="100" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="requestUrl" label="请求URL" width="180" />
      <el-table-column prop="costTime" label="耗时(ms)" width="100" />
      <el-table-column prop="result" label="结果" width="80">
        <template #default="{ row }"><el-tag :type="row.result === 1 ? 'success' : 'danger'">{{ row.result === 1 ? '成功' : '失败' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createTime" label="操作时间" width="180" />
      <el-table-column label="详情" width="80">
        <template #default="{ row }"><el-button size="small" @click="viewDetail(row)">查看</el-button></template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:20px;justify-content:flex-end" v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />

    <el-dialog title="日志详情" v-model="detailVisible" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="请求方法">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="请求IP">{{ detail.requestIp }}</el-descriptions-item>
        <el-descriptions-item label="请求参数"><pre style="max-height:200px;overflow:auto">{{ detail.requestParams }}</pre></el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="detail.errorMsg">{{ detail.errorMsg }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { logAPI } from '@/api'

const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const query = reactive({ page: 1, size: 10, username: '', module: '' })
const detailVisible = ref(false)
const detail = ref({})

const fetchData = async () => { loading.value = true; const res = await logAPI.page(query); tableData.value = res.data.records; total.value = res.data.total; loading.value = false }

onMounted(fetchData)

const viewDetail = async (row) => {
  const res = await logAPI.detail(row.id)
  detail.value = res.data
  detailVisible.value = true
}
</script>
