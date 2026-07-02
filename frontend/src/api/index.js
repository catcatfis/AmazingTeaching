import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => Promise.reject(error))

// 响应拦截器
api.interceptors.response.use(response => {
  const res = response.data
  if (res.code !== 200) {
    ElMessage.error(res.msg || '请求失败')
    if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
    }
    return Promise.reject(new Error(res.msg))
  }
  return res
}, error => {
  ElMessage.error('网络错误，请稍后重试')
  return Promise.reject(error)
})

// ========== 认证 API ==========
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data)
}

// ========== 用户 API ==========
export const userAPI = {
  page: (params) => api.get('/user/page', { params }),
  getById: (id) => api.get(`/user/${id}`),
  add: (data, roleId) => api.post('/user', data, { params: { roleId } }),
  update: (id, data, roleId) => api.put(`/user/${id}`, data, { params: { roleId } }),
  delete: (id) => api.delete(`/user/${id}`),
  resetPassword: (id) => api.put(`/user/${id}/reset-password`),
  changePassword: (params) => api.put('/user/change-password', null, { params }),
  current: (userId) => api.get('/user/current', { params: { userId } })
}

// ========== 角色 API ==========
export const roleAPI = {
  list: () => api.get('/role'),
  add: (data) => api.post('/role', data),
  update: (id, data) => api.put(`/role/${id}`, data),
  delete: (id) => api.delete(`/role/${id}`),
  getPermissions: (roleId) => api.get(`/role/${roleId}/permissions`),
  assignPermissions: (roleId, permIds) => api.post(`/role/${roleId}/permissions`, permIds)
}

// ========== 权限 API ==========
export const permissionAPI = {
  tree: () => api.get('/permission/tree'),
  list: () => api.get('/permission')
}

// ========== 课程 API ==========
export const courseAPI = {
  page: (params) => api.get('/course/page', { params }),
  detail: (id) => api.get(`/course/${id}`),
  add: (data) => api.post('/course', data),
  update: (id, data) => api.put(`/course/${id}`, data),
  delete: (id) => api.delete(`/course/${id}`),
  publish: (id) => api.put(`/course/${id}/publish`),
  recommend: (params) => api.get('/course/recommend', { params }),
  hot: (limit) => api.get('/course/hot', { params: { limit } }),
  enroll: (params) => api.post('/course/enroll', null, { params }),
  unenroll: (params) => api.delete('/course/unenroll', { params }),
  studentCourses: (studentId) => api.get('/course/student-courses', { params: { studentId } }),
  teacherCourses: (teacherId) => api.get('/course/teacher-courses', { params: { teacherId } })
}

// ========== 章节 API ==========
export const chapterAPI = {
  tree: (courseId) => api.get(`/chapter/course/${courseId}`),
  detail: (id) => api.get(`/chapter/${id}`),
  add: (data) => api.post('/chapter', data),
  update: (id, data) => api.put(`/chapter/${id}`, data),
  delete: (id) => api.delete(`/chapter/${id}`)
}

// ========== 考试 API ==========
export const examAPI = {
  page: (params) => api.get('/exam/page', { params }),
  detail: (id) => api.get(`/exam/${id}`),
  add: (data) => api.post('/exam', data),
  update: (id, data) => api.put(`/exam/${id}`, data),
  delete: (id) => api.delete(`/exam/${id}`),
  submit: (data) => api.post('/exam/submit', data),
  records: (params) => api.get('/exam/records', { params }),
  submittedExamIds: (studentId) => api.get('/exam/submitted', { params: { studentId } }),
  stats: () => api.get('/exam/stats')
}

// ========== 日志 API ==========
export const logAPI = {
  page: (params) => api.get('/log/page', { params }),
  detail: (id) => api.get(`/log/${id}`)
}

// ========== 仪表盘 API ==========
export const dashboardAPI = {
  overview: () => api.get('/dashboard/overview'),
  categoryPie: () => api.get('/dashboard/category-pie'),
  difficultyBar: () => api.get('/dashboard/difficulty-bar'),
  enrollmentTrend: () => api.get('/dashboard/enrollment-trend'),
  userTypePie: () => api.get('/dashboard/user-type-pie'),
  examPassTrend: () => api.get('/dashboard/exam-pass-trend'),
  topCourses: () => api.get('/dashboard/top-courses')
}

// ========== AI API ==========
export const aiAPI = {
  chat: (data) => api.post('/ai/chat', data),
  chatStream: (data) => {
    const token = localStorage.getItem('token')
    return fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      },
      body: JSON.stringify(data)
    })
  },
  studyAdvice: (params) => api.get('/ai/study-advice', { params }),
  generateQuestions: (data) => api.post('/ai/generate-questions', data),
  knowledgeGraph: (studentId) => api.get('/ai/knowledge-graph', { params: { studentId } }),
  learningPath: (params) => api.get('/ai/learning-path', { params })
}

export default api
