export function getProductImage(product, noCache = true) {
  const apiBaseUrl = getApiBaseUrl()
  let url = `${apiBaseUrl}/api/images/product/${product.id}`
  if (noCache) {
    url += `?v=${Date.now()}`
  }
  return url
}

function getApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL
  if (configuredBaseUrl) {
    return configuredBaseUrl.replace(/\/$/, '')
  }

  if (typeof window !== 'undefined' && window.location?.origin) {
    return window.location.origin.replace(/\/$/, '')
  }

  return 'http://127.0.0.1:8080'
}

export function getCategoryLabel(categoryCode) {
  const categoryMap = {
    medical: '医疗险',
    critical: '重疾险',
    accident: '意外险',
    life: '人寿险',
    car: '车险',
    '1-3': '1-3类意外',
    '1-4': '1-4类意外',
    '1-5': '1-5类意外',
    '1-6': '1-6类意外',
    child: '少儿医疗',
    elder: '老年意外',
    travel: '旅游险',
    maternity: '驾乘险'
  }

  return categoryMap[categoryCode] || categoryCode || '未分类'
}

export function getSaleStatusLabel(saleStatus) {
  const statusMap = {
    ON_SALE: '已上架',
    OFF_SALE: '已下架'
  }

  return statusMap[saleStatus] || saleStatus || '未知状态'
}

export function formatMoney(value) {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : String(value)
}
