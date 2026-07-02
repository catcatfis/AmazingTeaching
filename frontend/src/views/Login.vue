<template>
  <div class="login-page">
    <!-- Waves 开场动画 -->
    <div class="startwaves" ref="startwavesRef">
      <div class="waves">
        <img :src="wave1" />
        <img :src="wave2" />
        <img :src="wave3" />
        <img :src="wave4" />
        <img :src="wave5" />
      </div>
    </div>

    <!-- 视频背景 -->
    <div class="videobg">
      <video ref="video1Ref" class="videos" muted playsinline preload="auto">
        <source :src="fvMovie1" type="video/mp4" />
      </video>
      <video ref="video2Ref" class="videos" muted playsinline preload="auto" loop>
        <source :src="fvMovie2" type="video/mp4" />
      </video>

      <!-- 登录卡片 -->
      <div class="login-card">
        <h1 class="login-title">AmazingTeaching</h1>
        <p class="login-subtitle">智能教学平台</p>
        <el-form :model="form" :rules="rules" ref="formRef" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" style="width:100%" @click="handleLogin">登 录</el-button>
          </el-form-item>
        </el-form>
        <div class="login-footer">
          <span></span>
          <el-button type="primary" link @click="showRegister = true">注册账号</el-button>
        </div>
      </div>
    </div>

    <!-- 注册对话框 -->
    <el-dialog v-model="showRegister" title="注册账号" width="400px" :close-on-click-modal="false">
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select v-model="registerForm.userType" placeholder="请选择用户类型" style="width: 100%">
            <el-option label="学生" :value="3" />
            <el-option label="教师" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { authAPI } from '@/api'
import { ElMessage } from 'element-plus'

// 导入 waves 资源
import wave1 from '@/waves/wave-1.svg'
import wave2 from '@/waves/wave-2.svg'
import wave3 from '@/waves/wave-3.svg'
import wave4 from '@/waves/wave-4.svg'
import wave5 from '@/waves/wave-5.svg'

// 导入视频资源
import fvMovie1 from '@/video/fv_movie1.mp4'
import fvMovie2 from '@/video/fv_movie2.mp4'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const showRegister = ref(false)
const registerFormRef = ref(null)
const registerLoading = ref(false)

// waves 和视频相关引用
const startwavesRef = ref(null)
const video1Ref = ref(null)
const video2Ref = ref(null)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  email: '',
  phone: '',
  userType: 3 // 默认学生
})

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }]
}

// waves 动画是否已执行过（防止 animationend 重复触发）
let wavesHandled = false

// waves 动画结束后的处理
const handleWavesEnd = () => {
  if (wavesHandled) return
  wavesHandled = true

  const startwaves = startwavesRef.value
  const video1 = video1Ref.value
  const video2 = video2Ref.value
  if (!startwaves || !video1 || !video2) return

  // 1. 隐藏 waves 遮罩
  startwaves.style.display = 'none'

  // 2. 卡片淡入（与 movie1 同时）
  const card = document.querySelector('.login-card')
  if (card) card.classList.add('card-visible')

  // 3. movie1 播完 → movie1 渐隐 + movie2 渐显（交叉淡入淡出）
  video1.addEventListener('ended', () => {
    // movie1 渐隐
    video1.style.transition = 'opacity 0.6s ease-out'
    video1.style.opacity = '0'
    // movie2 同时渐显
    video2.style.opacity = '1'
    video2.play()
    // movie1 渐隐结束后彻底隐藏
    video1.addEventListener('transitionend', () => {
      video1.style.display = 'none'
    }, { once: true })
  })

  // 4. 尝试播放 movie1
  video1.style.opacity = '1'
  const playPromise = video1.play()
  if (playPromise !== undefined) {
    playPromise.catch(() => {
      // 浏览器拒绝自动播放，直接跳到 movie2
      console.warn('movie1 autoplay blocked, falling back to movie2')
      video1.style.display = 'none'
      video2.style.opacity = '1'
      video2.play()
    })
  }
}

onMounted(() => {
  const startwaves = startwavesRef.value
  if (startwaves) {
    startwaves.addEventListener('animationend', handleWavesEnd)
  }
})

onBeforeUnmount(() => {
  const startwaves = startwavesRef.value
  if (startwaves) {
    startwaves.removeEventListener('animationend', handleWavesEnd)
  }
})

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  registerLoading.value = true
  try {
    await authAPI.register(registerForm)
    ElMessage.success('注册成功，请登录')
    showRegister.value = false
    // 清空注册表单
    Object.assign(registerForm, { username: '', password: '', confirmPassword: '', realName: '', email: '', phone: '', userType: 3 })
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
/* 页面整体 */
.login-page {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  position: relative;
  background-color: #000;
}

/* ==================== Waves 开场动画 ==================== */
.startwaves {
  position: fixed;
  display: flex;
  justify-content: center;
  height: 120vh;
  width: 100%;
  background-color: #469ce5;
  transform: translateY(-10vh);
  animation-name: wavesDown;
  animation-duration: 1.2s;
  animation-delay: 0.3s;
  animation-timing-function: ease-out;
  z-index: 999;
}

@keyframes wavesDown {
  0% {
    transform: translateY(-10vh);
  }
  100% {
    transform: translateY(110vh);
  }
}

/* ==================== Waves 层叠动画 ==================== */
.waves {
  display: block;
  position: absolute;
  bottom: 120vh;
  width: 100%;
}

.waves img {
  position: absolute;
  left: 0;
  width: 100%;
}

.waves img:nth-child(1) {
  opacity: 0.8;
  bottom: -0.1vw;
  animation: waveMove1 3s infinite;
}

.waves img:nth-child(2) {
  opacity: 0.7;
  bottom: 0.5vw;
  animation: waveMove2 2.5s infinite;
}

.waves img:nth-child(3) {
  opacity: 0.6;
  bottom: 0.3vw;
  animation: waveMove3 2s infinite;
}

.waves img:nth-child(4) {
  opacity: 0.5;
  bottom: 0.1vw;
  animation: waveMove4 1.8s infinite;
}

.waves img:nth-child(5) {
  position: absolute;
  left: 0;
  bottom: -1vw;
}

@keyframes waveMove1 {
  50% { transform: translateY(35px); }
}

@keyframes waveMove2 {
  50% { transform: translateY(25px); }
}

@keyframes waveMove3 {
  50% { transform: translateY(20px); }
}

@keyframes waveMove4 {
  50% { transform: translateY(20px); }
}

/* ==================== 视频背景 ==================== */
.videobg {
  width: 100%;
  height: 100%;
  position: relative;
}

.videos {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: absolute;
  top: 0;
  left: 0;
  opacity: 0;
  transition: opacity 0.6s ease-in;
}

/* ==================== 登录卡片 ==================== */
.login-card {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -45%);
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.4);
  z-index: 10;
  opacity: 0;
  transition: opacity 1.2s ease-out, transform 1.2s ease-out;
}

.login-card.card-visible {
  opacity: 1;
  transform: translate(-50%, -50%);
}

.login-title {
  text-align: center;
  font-size: 28px;
  color: #303133;
  margin-bottom: 8px;
}

.login-subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 30px;
  font-size: 14px;
}

.login-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

/* ==================== 注册对话框 ==================== */
:deep(.el-dialog) {
  border-radius: 12px;
}
</style>
