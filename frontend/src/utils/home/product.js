export function getProductImage(product) {
  return `/api/images/product/${product.id}`
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

export function formatStock(stock) {
  if (stock === null || stock === undefined || stock === '') {
    return '充足'
  }
  const num = Number(stock)
  if (!Number.isFinite(num)) {
    return String(stock)
  }
  if (num <= 0) {
    return '充足'
  }
  return `${num} 份`
}
