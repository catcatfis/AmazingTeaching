<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>参加考试 - {{ exam?.examName }}</span>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </template>

    <div v-if="exam && !showResult">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="总分">{{ exam.totalScore }}分</el-descriptions-item>
        <el-descriptions-item label="及格线">{{ exam.passScore }}分</el-descriptions-item>
        <el-descriptions-item label="时长">{{ exam.duration }}分钟</el-descriptions-item>
        <el-descriptions-item label="题数">{{ questions.length }}题</el-descriptions-item>
        <el-descriptions-item label="每题分值">{{ scorePerQuestion }}分</el-descriptions-item>
      </el-descriptions>

      <h3 style="margin-top:24px">试题</h3>
      <div v-for="(q, i) in questions" :key="i" class="question-item">
        <p class="question-title">{{ i + 1 }}. {{ q.question }} <span class="score-tag">（{{ scorePerQuestion }}分）</span></p>
        <el-radio-group v-model="answers[i]" style="display:flex;flex-direction:column;gap:8px;margin-top:8px">
          <el-radio v-for="(opt, j) in q.options" :key="j" :value="q.answerMap[j]">{{ opt }}</el-radio>
        </el-radio-group>
      </div>

      <el-button type="primary" size="large" @click="handleSubmit" style="margin-top:20px">提交试卷</el-button>
    </div>

    <!-- 考试结果展示 -->
    <div v-if="showResult">
      <el-result
        :icon="resultData.isPassed ? 'success' : 'warning'"
        :title="resultData.isPassed ? '恭喜，考试通过！' : '很遗憾，考试未通过'"
        :sub-title="`您的得分：${resultData.score}分 / ${exam.totalScore}分（及格线：${exam.passScore}分）`"
      >
        <template #extra>
          <el-button type="primary" @click="$router.push('/exam')">返回考试列表</el-button>
        </template>
      </el-result>

      <h3 style="margin-top:24px">答题详情</h3>
      <div v-for="(q, i) in questions" :key="i" class="question-item" :class="getQuestionClass(i)">
        <div class="question-header">
          <p class="question-title">{{ i + 1 }}. {{ q.question }} <span class="score-tag">（{{ scorePerQuestion }}分）</span></p>
          <el-tag :type="getAnswerStatus(i) ? 'success' : 'danger'" size="small">
            {{ getAnswerStatus(i) ? '✓ 正确' : '✗ 错误' }}
          </el-tag>
        </div>
        <div class="options-list">
          <p v-for="(opt, j) in q.options" :key="j" :class="getOptionClass(i, j)">
            {{ q.answerMap[j] }}. {{ opt }}
            <span v-if="q.answerMap[j] === q.answer" class="correct-mark">（正确答案）</span>
            <span v-if="answers[i] === q.answerMap[j] && q.answerMap[j] !== q.answer" class="wrong-mark">（您的答案）</span>
            <span v-if="answers[i] === q.answerMap[j] && q.answerMap[j] === q.answer" class="correct-mark">（您的答案）</span>
          </p>
        </div>
        <p class="score-result">得分：{{ getAnswerStatus(i) ? scorePerQuestion : 0 }}分</p>
      </div>

      <el-divider />
      <el-descriptions :column="2" border>
        <el-descriptions-item label="总题数">{{ questions.length }}题</el-descriptions-item>
        <el-descriptions-item label="答对题数">{{ correctCount }}题</el-descriptions-item>
        <el-descriptions-item label="您的得分"><span style="font-size:20px;font-weight:bold;color:#409EFF">{{ resultData.score }}分</span></el-descriptions-item>
        <el-descriptions-item label="正确率">{{ ((correctCount / questions.length) * 100).toFixed(1) }}%</el-descriptions-item>
      </el-descriptions>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { examAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const exam = ref(null)
const answers = ref([])
const showResult = ref(false)
const resultData = ref({ score: 0, isPassed: false })

const mockQuestions = [
  { question: 'Java中 == 和 equals() 的区别？', options: ['==比较引用，equals比较值', '两者完全相同', '==比较值，equals比较引用', '都不可用于字符串'], answerMap: ['A','B','C','D'], answer: 'A' },
  { question: 'Spring Boot的核心特性不包括？', options: ['自动配置', '起步依赖', '内嵌服务器', '需要XML配置'], answerMap: ['A','B','C','D'], answer: 'D' },
  { question: 'MySQL的默认端口号是？', options: ['3306', '8080', '1521', '27017'], answerMap: ['A','B','C','D'], answer: 'A' },
  { question: 'Vue3中，以下哪个是组合式API的核心函数？', options: ['setup()', 'data()', 'methods()', 'created()'], answerMap: ['A','B','C','D'], answer: 'A' },
  { question: 'RESTful API中，PUT方法通常用于？', options: ['更新资源', '删除资源', '查询资源', '创建资源'], answerMap: ['A','B','C','D'], answer: 'A' }
]
const questions = ref(mockQuestions)

// 计算每题分值
const scorePerQuestion = computed(() => {
  if (!exam.value || !questions.value.length) return 0
  return Math.round(exam.value.totalScore / questions.value.length)
})

// 计算正确题数
const correctCount = computed(() => {
  return questions.value.filter((q, i) => answers.value[i] === q.answer).length
})

onMounted(async () => {
  const res = await examAPI.detail(route.params.id)
  exam.value = res.data
  
  // 检查考试状态
  if (exam.value.status === 0) {
    ElMessage.warning('考试尚未开始')
    router.push('/exam')
    return
  }
  if (exam.value.status === 2) {
    ElMessage.warning('考试已结束')
    router.push('/exam')
    return
  }
  
  answers.value = new Array(questions.value.length).fill(null)
})

const getAnswerStatus = (index) => {
  return answers.value[index] === questions.value[index].answer
}

const getQuestionClass = (index) => {
  return getAnswerStatus(index) ? 'question-correct' : 'question-wrong'
}

const getOptionClass = (questionIndex, optionIndex) => {
  const q = questions.value[questionIndex]
  const selectedAnswer = answers.value[questionIndex]
  const optionValue = q.answerMap[optionIndex]
  
  if (optionValue === q.answer) return 'option-correct'
  if (optionValue === selectedAnswer && optionValue !== q.answer) return 'option-wrong'
  return ''
}

const handleSubmit = async () => {
  // 检查是否全部作答
  const unanswered = answers.value.filter(a => a === null).length
  if (unanswered > 0) {
    try {
      await ElMessageBox.confirm(`还有 ${unanswered} 道题未作答，确定要提交吗？`, '提示', { type: 'warning' })
    } catch {
      return
    }
  }
  
  // 计算得分（本地评分）
  let score = 0
  questions.value.forEach((q, i) => {
    if (answers.value[i] === q.answer) {
      score += scorePerQuestion.value
    }
  })
  
  const isPassed = score >= exam.value.passScore
  
  try {
    await examAPI.submit({
      examId: exam.value.id,
      studentId: userStore.userInfo?.userId || 4,
      answersJson: JSON.stringify(answers.value),
      score: score
    })
    
    resultData.value = { score, isPassed }
    showResult.value = true
    
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-item {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid #dcdfe6;
}

.question-correct {
  border-left-color: #67C23A;
}

.question-wrong {
  border-left-color: #F56C6C;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.question-title {
  font-weight: bold;
  margin: 0;
}

.score-tag {
  color: #909399;
  font-weight: normal;
  font-size: 14px;
}

.options-list p {
  margin: 6px 0;
}

.option-correct {
  color: #67C23A;
  font-weight: bold;
}

.option-wrong {
  color: #F56C6C;
  text-decoration: line-through;
}

.correct-mark {
  color: #67C23A;
  font-size: 14px;
}

.wrong-mark {
  color: #F56C6C;
  font-size: 14px;
}

.score-result {
  margin-top: 12px;
  font-weight: bold;
  color: #409EFF;
}
</style>
