export function resolveNavigationTarget(to, authToken, userType) {
  if (to.meta?.requiresAuth && !authToken) {
    return '/login'
  }

  if (to.path === '/login' && authToken) {
    return '/'
  }

  if (to.meta?.requiresAdmin && userType !== 'ADMIN') {
    return '/'
  }

  return null
}
