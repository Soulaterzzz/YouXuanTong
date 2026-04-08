export function formatAdminDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function buildAdminAnalysisRange(periodType, baseDate = new Date()) {
  const current = new Date(baseDate)
  current.setHours(0, 0, 0, 0)
  let start = new Date(current)

  switch (periodType) {
    case 'WEEK': {
      const day = current.getDay() === 0 ? 7 : current.getDay()
      start.setDate(current.getDate() - day + 1)
      break
    }
    case 'QUARTER': {
      const quarterStartMonth = Math.floor(current.getMonth() / 3) * 3
      start = new Date(current.getFullYear(), quarterStartMonth, 1)
      break
    }
    case 'YEAR':
      start = new Date(current.getFullYear(), 0, 1)
      break
    case 'MONTH':
    default:
      start = new Date(current.getFullYear(), current.getMonth(), 1)
      break
  }

  return [formatAdminDate(start), formatAdminDate(current)]
}
