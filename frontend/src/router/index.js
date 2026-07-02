import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import MainLayout from '@/layout/MainLayout.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login, meta: { title: '登录' } },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '数据看板', icon: 'DataAnalysis', roles: [1, 2, 3] } },
      { path: 'user', name: 'UserList', component: () => import('@/views/UserList.vue'), meta: { title: '用户管理', icon: 'User', roles: [1] } },
      { path: 'role', name: 'RoleList', component: () => import('@/views/RoleList.vue'), meta: { title: '角色管理', icon: 'UserFilled', roles: [1] } },
      { path: 'permission', name: 'PermissionList', component: () => import('@/views/PermissionList.vue'), meta: { title: '权限管理', icon: 'Lock', roles: [1] } },
      { path: 'course', name: 'CourseList', component: () => import('@/views/CourseList.vue'), meta: { title: '课程管理', icon: 'Reading', roles: [1, 2] } },
      { path: 'course/:id', name: 'CourseDetail', component: () => import('@/views/CourseDetail.vue'), meta: { title: '课程详情', hidden: true, roles: [1, 2, 3] } },
      { path: 'course-edit', name: 'CourseEdit', component: () => import('@/views/CourseEdit.vue'), meta: { title: '课程编辑', hidden: true, roles: [1, 2] } },
      { path: 'course-edit/:id', name: 'CourseEditId', component: () => import('@/views/CourseEdit.vue'), meta: { title: '课程编辑', hidden: true, roles: [1, 2] } },
      { path: 'chapter/:courseId', name: 'ChapterList', component: () => import('@/views/ChapterList.vue'), meta: { title: '章节管理', hidden: true, roles: [1, 2] } },
      { path: 'exam', name: 'ExamList', component: () => import('@/views/ExamList.vue'), meta: { title: '考试', icon: 'EditPen', roles: [1, 2, 3] } },
      { path: 'exam/take/:id', name: 'ExamTake', component: () => import('@/views/ExamTake.vue'), meta: { title: '参加考试', hidden: true, roles: [3] } },
      { path: 'enrollment', name: 'StudentEnrollment', component: () => import('@/views/StudentEnrollment.vue'), meta: { title: '选课管理', icon: 'Notebook', roles: [1, 3] } },
      { path: 'log', name: 'LogList', component: () => import('@/views/LogList.vue'), meta: { title: '操作日志', icon: 'Document', roles: [1] } },
      { path: 'ai', name: 'AIAssistant', component: () => import('@/views/AIAssistant.vue'), meta: { title: 'AI智能助手', icon: 'MagicStick', roles: [1, 2, 3] } },
      { path: 'analysis', name: 'DataAnalysis', component: () => import('@/views/DataAnalysis.vue'), meta: { title: '数据分析', icon: 'TrendCharts', roles: [1, 2] } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
