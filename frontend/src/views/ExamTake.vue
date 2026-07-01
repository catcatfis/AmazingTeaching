<template>
  <el-card>
    <template #header><div class="card-header"><span>参加考试 - {{ exam?.examName }}</span><el-button @click="$router.back()">返回</el-button></div></template>

    <div v-if="exam">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="总分">{{ exam.totalScore }}分</el-descriptions-item>
        <el-descriptions-item label="及格线">{{ exam.passScore }}分</el-descriptions-item>
        <el-descriptions-item label="时长">{{ exam.duration }}分钟</el-descriptions-item>
      </el-descriptions>

      <h3 style="margin-top:24px">试题</h3>
      <div v-for="(q, i) in questions" :key="i" style="margin-bottom:20px;padding:16px;background:#f5f7fa;border-radius:8px">
        <p style="font-weight:bold">{{ i + 1 }}. {{ q.question }}</p>
        <el-radio-group v-model="answers[i]" style="display:flex;flex-direction:column;gap:8px;margin-top:8px">
          <el-radio v-for="(opt, j) in q.options" :key="j" :value="q.answerMap[j]">{{ opt }}</el-radio>
        </el-radio-group>
      </div>

      <el-button type="primary" size="large" @click="handleSubmit" style="margin-top:20px">提交试卷</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { examAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const exam = ref(null)
const answers = ref([])

const mockQuestions = [
  { question: 'Java中 == 和 equals() 的区别？', options: ['==比较引用，equals比较值', '两者完全相同', '==比较值，equals比较引用', '都不可用于字符串'], answerMap: ['A','B','C','D'] },
  { question: 'Spring Boot的核心特性不包括？', options: ['自动配置', '起步依赖', '内嵌服务器', '需要XML配置'], answerMap: ['A','B','C','D'] },
  { question: 'MySQL的默认端口号是？', options: ['3306', '8080', '1521', '27017'], answerMap: ['A','B','C','D'] },
  { question: 'Vue3中，以下哪个是组合式API的核心函数？', options: ['setup()', 'data()', 'methods()', 'created()'], answerMap: ['A','B','C','D'] },
  { question: 'RESTful API中，PUT方法通常用于？', options: ['更新资源', '删除资源', '查询资源', '创建资源'], answerMap: ['A','B','C','D'] }
]
const questions = ref(mockQuestions)

onMounted(async () => {
  const res = await examAPI.detail(route.params.id)
  exam.value = res.data
  answers.value = new Array(questions.value.length).fill(null)
})

const handleSubmit = async () => {
  const score = Math.floor(Math.random() * 30) + 70 // 模拟评分 70-100
  await examAPI.submit({
    examId: exam.value.id,
    studentId: userStore.userInfo?.userId || 4,
    answersJson: JSON.stringify(answers.value),
    score: score
  })
  ElMessage.success(`考试提交成功！得分：${score}分`)
  router.push('/exam')
}
</script>

<style scoped>.card-header { display: flex; justify-content: space-between; align-items: center; }</style>
