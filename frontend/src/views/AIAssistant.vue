<template>
  <el-card>
    <template #header><span>🤖 AI 智能助手</span></template>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="智能问答" name="chat">
        <div class="chat-container">
          <div class="chat-messages" ref="chatMessages">
            <div v-for="(msg, i) in chatHistory" :key="i" :class="['chat-msg', msg.role]">
              <div class="msg-content">{{ msg.content }}<span v-if="msg.role === 'ai' && isSending && i === chatHistory.length - 1" class="typing-cursor"></span></div>
              <div class="msg-time">{{ msg.time }}</div>
            </div>
          </div>
          <div class="chat-input">
            <el-input v-model="chatInput" placeholder="输入你的问题..." @keyup.enter="sendMessage" :disabled="isSending" />
            <el-button type="primary" @click="sendMessage" :loading="isSending" style="margin-left:8px">{{ isSending ? '回答中...' : '发送' }}</el-button>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="学习建议" name="advice">
        <el-button type="primary" @click="getAdvice">获取学习建议</el-button>
        <div v-if="advice" style="margin-top:16px;padding:16px;background:#f0f9eb;border-radius:8px">
          <p style="font-weight:bold;font-size:16px">📝 AI学习建议</p>
          <p style="margin-top:12px;line-height:1.8">{{ advice.advice }}</p>
          <h4 style="margin-top:16px">学习要点：</h4>
          <ul><li v-for="(item, i) in advice.focusPoints" :key="i">{{ item }}</li></ul>
        </div>
      </el-tab-pane>

      <el-tab-pane label="自动出题" name="questions">
        <el-form :inline="true">
          <el-form-item label="主题"><el-input v-model="genTopic" placeholder="例如：Java基础" /></el-form-item>
          <el-form-item label="数量"><el-input-number v-model="genCount" :min="1" :max="10" /></el-form-item>
          <el-form-item label="难度"><el-select v-model="genDifficulty" placeholder="请选择难度" style="width:120px"><el-option label="简单" value="简单" /><el-option label="中等" value="中等" /><el-option label="困难" value="困难" /></el-select></el-form-item>
          <el-form-item><el-button type="primary" @click="generateQuestions">生成试题</el-button></el-form-item>
        </el-form>
        <div v-for="(q, i) in generatedQs" :key="i" style="margin-bottom:16px;padding:12px;background:#f5f7fa;border-radius:8px">
          <p style="font-weight:bold">{{ i + 1 }}. {{ q.question }}</p>
          <div v-if="q.options" style="margin:8px 0;color:#606266;line-height:1.8">
            <p v-for="(opt, j) in q.options.split('\n')" :key="j" style="margin:0">{{ opt }}</p>
          </div>
          <p style="color:#409EFF;margin-top:4px;font-weight:bold">正确答案：{{ q.answer }}</p>
        </div>
      </el-tab-pane>

      <el-tab-pane label="知识图谱" name="graph">
        <div ref="graphChart" style="height:400px"></div>
      </el-tab-pane>

      <el-tab-pane label="学习路径" name="path">
        <el-form :inline="true">
          <el-form-item label="学习目标"><el-input v-model="pathGoal" placeholder="例如：成为Java高级工程师" /></el-form-item>
          <el-form-item><el-button type="primary" @click="getLearningPath">生成学习路径</el-button></el-form-item>
        </el-form>
        <el-timeline v-if="learningPath" style="margin-top:20px">
          <el-timeline-item v-for="stage in learningPath" :key="stage.stage"
            :timestamp="`第${stage.stage}阶段 - ${stage.name}（${stage.duration}）`" placement="top">
            <el-card>{{ stage.content }}</el-card>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { aiAPI } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activeTab = ref('chat')
const chatInput = ref('')
const chatHistory = ref([{ role: 'ai', content: '你好！我是 AmazingTeaching AI 智能助手，有什么可以帮你的？', time: new Date().toLocaleTimeString() }])
const chatMessages = ref(null)
const advice = ref(null)
const genTopic = ref('Java基础')
const genCount = ref(5)
const genDifficulty = ref('中等')
const generatedQs = ref([])
const graphChart = ref(null)
let chartInstance = null
const pathGoal = ref('')
const learningPath = ref(null)
const isSending = ref(false)

const sendMessage = async () => {
  if (!chatInput.value.trim() || isSending.value) return
  const q = chatInput.value
  chatHistory.value.push({ role: 'user', content: q, time: new Date().toLocaleTimeString() })
  chatInput.value = ''
  isSending.value = true
  await nextTick(); chatMessages.value.scrollTop = chatMessages.value.scrollHeight

  // 添加 AI 消息占位符（流式填充）
  const aiMsgIndex = chatHistory.value.length
  chatHistory.value.push({ role: 'ai', content: '', time: new Date().toLocaleTimeString() })

  try {
    const response = await aiAPI.chatStream({ question: q })
    if (!response.ok) {
      chatHistory.value[aiMsgIndex].content = 'AI 服务请求失败，请稍后再试。'
      isSending.value = false
      return
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let streamDone = false

    while (!streamDone) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() // 保留不完整的行

      for (const line of lines) {
        if (line.startsWith('event:')) {
          continue
        }
        if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          if (data === '[DONE]') {
            streamDone = true
            break
          }
          try {
            chatHistory.value[aiMsgIndex].content += data
            await nextTick()
            chatMessages.value.scrollTop = chatMessages.value.scrollHeight
          } catch (e) { /* skip */ }
        }
      }
    }
    // 处理缓冲区剩余内容
    if (buffer.trim()) {
      const lines = buffer.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          if (data !== '[DONE]') {
            chatHistory.value[aiMsgIndex].content += data
          }
        }
      }
    }
    if (!chatHistory.value[aiMsgIndex].content) {
      chatHistory.value[aiMsgIndex].content = 'AI 未能生成回复，请重新提问。'
    }
  } catch (err) {
    console.error('SSE 流式请求失败:', err)
    // 回退到同步请求
    try {
      const res = await aiAPI.chat({ question: q })
      chatHistory.value[aiMsgIndex].content = res.data.answer
    } catch (e) {
      chatHistory.value[aiMsgIndex].content = 'AI 服务异常，请稍后再试。'
    }
  } finally {
    isSending.value = false
    await nextTick()
    chatMessages.value.scrollTop = chatMessages.value.scrollHeight
  }
}

const getAdvice = async () => {
  const res = await aiAPI.studyAdvice({ studentId: userStore.userInfo?.userId || 4 })
  advice.value = res.data
}

const generateQuestions = async () => {
  const res = await aiAPI.generateQuestions({ topic: genTopic.value, count: genCount.value, difficulty: genDifficulty.value })
  generatedQs.value = res.data.questions
}

const getLearningPath = async () => {
  const res = await aiAPI.learningPath({ studentId: userStore.userInfo?.userId || 4, goal: pathGoal.value })
  learningPath.value = res.data.stages
}

watch(activeTab, async (tab) => {
  if (tab === 'graph') {
    await nextTick()
    // 先销毁旧实例，避免重复初始化警告
    if (chartInstance) { chartInstance.dispose(); chartInstance = null }
    const res = await aiAPI.knowledgeGraph(userStore.userInfo?.userId || 4)
    chartInstance = echarts.init(graphChart.value)
    
    // 节点分类映射：后端->0, 数据库->1, 前端->2, 计算机基础->3
    const categoryMap = { '后端': 0, '数据库': 1, '前端': 2, '计算机基础': 3 }
    const nodes = res.data.nodes.map(n => ({
      name: n.name,
      category: categoryMap[n.category] ?? 3,
      symbolSize: 30 + (n.mastery - 60) / 40 * 40, // 30-70，差异更明显
      value: n.mastery
    }))
    const edges = res.data.edges.map(e => ({
      source: nodes[e.source - 1]?.name,
      target: nodes[e.target - 1]?.name,
      lineStyle: { curveness: 0.2 }
    }))
    chartInstance.setOption({
      tooltip: { formatter: (p) => p.dataType === 'node' ? `${p.name}<br/>掌握度: ${p.data.value}%` : '' },
      legend: [{ data: ['后端', '数据库', '前端', '计算机基础'] }],
      series: [{
        type: 'graph', layout: 'force', roam: true,
        label: { show: true, fontSize: 12 },
        categories: [
          { name: '后端' },
          { name: '数据库' },
          { name: '前端' },
          { name: '计算机基础' }
        ],
        data: nodes, edges: edges,
        force: { repulsion: 300, edgeLength: [120, 200] },
        emphasis: { focus: 'adjacency' },
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: [0, 10]
      }]
    })
  }
})
</script>

<style scoped>
.chat-container { border: 1px solid #e6e6e6; border-radius: 8px; overflow: hidden; }
.chat-messages { height: 400px; overflow-y: auto; padding: 16px; background: #fafafa; }
.chat-msg { margin-bottom: 16px; display: flex; flex-direction: column; }
.chat-msg.user { align-items: flex-end; }
.chat-msg.ai { align-items: flex-start; }
.chat-msg.user .msg-content { background: #409EFF; color: #fff; }
.chat-msg.ai .msg-content { background: #fff; border: 1px solid #e6e6e6; }
.msg-content { max-width: 70%; padding: 10px 14px; border-radius: 12px; line-height: 1.6; white-space: pre-wrap; }
.msg-time { font-size: 14px; color: #c0c4cc; margin-top: 4px; }
.chat-input { display: flex; padding: 12px; background: #fff; border-top: 1px solid #e6e6e6; }
.typing-cursor { display: inline-block; width: 2px; height: 1em; background: #409EFF; margin-left: 2px; vertical-align: text-bottom; animation: blink 0.8s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }
</style>
