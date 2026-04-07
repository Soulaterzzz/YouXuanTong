import { describe, expect, it } from 'vitest'
import { resolveNavigationTarget } from './guard'

describe('resolveNavigationTarget', () => {
  it('未登录访问鉴权页面时跳转登录页', () => {
    const redirectTarget = resolveNavigationTarget({ path: '/', meta: { requiresAuth: true } }, '', 'USER')

    expect(redirectTarget).toBe('/login')
  })

  it('已登录访问登录页时回到首页', () => {
    const redirectTarget = resolveNavigationTarget({ path: '/login', meta: { requiresAuth: false } }, 'token-1', 'USER')

    expect(redirectTarget).toBe('/')
  })

  it('非管理员访问管理员页面时回到首页', () => {
    const redirectTarget = resolveNavigationTarget({ path: '/admin', meta: { requiresAuth: true, requiresAdmin: true } }, 'token-2', 'USER')

    expect(redirectTarget).toBe('/')
  })

  it('满足条件时允许继续访问', () => {
    const redirectTarget = resolveNavigationTarget({ path: '/', meta: { requiresAuth: true } }, 'token-3', 'ADMIN')

    expect(redirectTarget).toBeNull()
  })
})
