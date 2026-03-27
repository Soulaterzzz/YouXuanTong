import { createRouter, createWebHistory } from 'vue-router'
import { resolveNavigationTarget } from './guard'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const redirectTarget = resolveNavigationTarget(
    to,
    sessionStorage.getItem('authToken'),
    sessionStorage.getItem('userType')
  )

  if (redirectTarget) {
    next(redirectTarget)
    return
  }

  next()
})

export default router
