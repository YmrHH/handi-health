import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { recordFrontendAuditLog } from '@/audit/frontendAudit'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/home',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/Index.vue'),
        meta: { title: '数据管理 / 数据总览' }
      },
      {
        path: 'home/board',
        name: 'HomeBoard',
        component: () => import('@/views/report/Board.vue'),
        meta: { title: '数据管理 / AI数据分析' }
      },
      {
        path: 'home/export',
        name: 'HomeExport',
        component: () => import('@/views/report/Export.vue'),
        meta: { title: '数据管理 / 报表与导出' }
      },
      // 患者管理
      {
        path: 'patient',
        name: 'PatientList',
        component: () => import('@/views/patient/RiskList.vue'),
        meta: { title: '患者管理 / 患者档案管理' }
      },
      {
        path: 'patient/detail/:patientId',
        name: 'PatientDetail',
        component: () => import('@/views/patient/Detail.vue'),
        meta: { title: '患者管理 / 患者档案与画像' }
      },
      {
        path: 'patient/recommend',
        name: 'PatientRecommend',
        component: () => import('@/views/intervention/Recommend.vue'),
        meta: { title: '患者管理 / 健康建议下发' }
      },
      {
        path: 'patient/doctor-advice',
        name: 'PatientDoctorAdvice',
        component: () => import('@/views/patient/DoctorAdvice.vue'),
        meta: { title: '患者管理 / 医学建议数据留存' }
      },
      {
        path: 'patient/visit-plan',
        name: 'PatientVisitPlan',
        component: () => import('@/views/intervention/VisitPlan.vue'),
        meta: { title: '患者管理 / 复诊计划管理' }
      },
      // 预警管理
      {
        path: 'alert/patient',
        name: 'PatientAlert',
        component: () => import('@/views/alert/Alerts.vue'),
        meta: { title: '预警管理 / 健康数据异常预警' }
      },
      {
        path: 'alert/hardware',
        name: 'HardwareAlert',
        component: () => import('@/views/alert/Alerts.vue'),
        meta: { title: '预警管理 / 健康设备异常预警' }
      },
      // 旧预警路由重定向
      {
        path: 'alert/alerts',
        redirect: '/alert/patient'
      },
      // 随访管理
      {
        path: 'followup/workbench',
        name: 'FollowupWorkbench',
        component: () => import('@/views/alert/FollowupWorkbench.vue'),
        meta: { title: '随访管理 / 进行主动随访' }
      },
      {
        path: 'followup/pending/:id',
        name: 'PendingFollowupDetail',
        component: () => import('@/views/alert/PendingFollowupDetail.vue'),
        meta: { title: '随访管理 / 待随访详情' }
      },
      {
        path: 'followup/detail/:id',
        name: 'FollowupDetail',
        component: () => import('@/views/followup/FollowupDetail.vue'),
        meta: { title: '随访管理 / 随访记录详情' }
      },
      {
        path: 'followup/staff',
        name: 'FollowupStaffList',
        component: () => import('@/views/followup/StaffList.vue'),
        meta: { title: '随访管理 / 随访人员管理' }
      },
      {
        path: 'followup/staff/:id',
        name: 'FollowupStaffDetail',
        component: () => import('@/views/followup/StaffDetail.vue'),
        meta: { title: '随访管理 / 随访人员详情' }
      },
      {
        path: 'followup/task-assign',
        name: 'TaskAssign',
        component: () => import('@/views/intervention/HomeService.vue'),
        meta: { title: '随访管理 / 随访任务指派' }
      },
      // 旧随访路由重定向
      {
        path: 'alert/followup',
        redirect: '/followup/workbench'
      },
      {
        path: 'alert/task-assign',
        redirect: '/followup/task-assign'
      },
      {
        path: 'alert/followup-import',
        name: 'FollowupImport',
        component: () => import('@/views/alert/FollowupImport.vue'),
        meta: { title: '随访管理 / 导入随访数据' }
      },
      {
        path: 'alert/followup-export',
        name: 'FollowupExport',
        component: () => import('@/views/alert/FollowupExport.vue'),
        meta: { title: '随访管理 / 导出随访数据' }
      },
      // 干预与计划管理 - 重定向到患者管理子菜单
      {
        path: 'intervention/recommend',
        redirect: '/patient/recommend'
      },
      {
        path: 'intervention/visit-plan',
        redirect: '/patient/visit-plan'
      },
      // 统计看板与报表 - 重定向到首页子菜单
      {
        path: 'report/board',
        redirect: '/home/board'
      },
      {
        path: 'report/export',
        redirect: '/home/export'
      },
      // 系统与基础配置
      {
        path: 'system/data-log',
        name: 'SystemDataLog',
        component: () => import('@/views/system/DataLog.vue'),
        meta: { title: '日志审计' }
      },
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title as string} - 寒岐智护·慢性病随访健康预警管理平台`
  }

  // 检查是否需要登录
  if (to.meta.requiresAuth !== false) {
    if (!authStore.isAuthenticated) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
  } else {
    // 登录/注册页面，如果已登录则跳转到首页
    if (authStore.isAuthenticated && (to.name === 'Login' || to.name === 'Register')) {
      next({ name: 'Home' })
      return
    }
  }

  next()
})

router.afterEach((to, from) => {
  try {
    const authStore = useAuthStore()
    recordFrontendAuditLog({
      type: 'route',
      user: authStore.username,
      page: typeof window !== 'undefined' ? window.location.pathname : undefined,
      routeFrom: from.fullPath,
      routeTo: to.fullPath,
      action: 'route_change',
      success: true
    })
  } catch {
    // ignore
  }
})

export default router

