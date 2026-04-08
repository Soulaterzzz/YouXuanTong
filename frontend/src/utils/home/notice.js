export function createNoticeId(now = Date.now()) {
  return `${now}-${Math.random().toString(16).slice(2, 8)}`
}

export function normalizeNoticeItem(item, idFactory = createNoticeId) {
  const title = String(item?.title || '').trim()
  const content = String(item?.content || '').trim()

  if (!title || !content) {
    return null
  }

  return {
    id: item?.id || idFactory(),
    title,
    content,
    sortNo: item?.sortNo ?? 0,
    publishedAt: item?.publishedAt || ''
  }
}

export function cloneNoticeList(list = []) {
  return list.map(item => ({ ...item }))
}

export function sortNoticeList(list = []) {
  return cloneNoticeList(list)
    .filter(Boolean)
    .sort((a, b) => {
      const sortA = Number.isFinite(Number(a?.sortNo)) ? Number(a.sortNo) : Number.MAX_SAFE_INTEGER
      const sortB = Number.isFinite(Number(b?.sortNo)) ? Number(b.sortNo) : Number.MAX_SAFE_INTEGER
      if (sortA !== sortB) {
        return sortA - sortB
      }

      const timeA = new Date(a?.publishedAt || 0).getTime()
      const timeB = new Date(b?.publishedAt || 0).getTime()
      if (timeA !== timeB) {
        return timeB - timeA
      }

      return String(b?.id || '').localeCompare(String(a?.id || ''))
    })
}

export function sortNoticeListByPublishedAtAsc(list = []) {
  return cloneNoticeList(list)
    .filter(Boolean)
    .sort((a, b) => {
      const timeA = new Date(a?.publishedAt || 0).getTime()
      const timeB = new Date(b?.publishedAt || 0).getTime()
      if (timeA !== timeB) {
        return timeA - timeB
      }

      const sortA = Number.isFinite(Number(a?.sortNo)) ? Number(a.sortNo) : Number.MAX_SAFE_INTEGER
      const sortB = Number.isFinite(Number(b?.sortNo)) ? Number(b.sortNo) : Number.MAX_SAFE_INTEGER
      if (sortA !== sortB) {
        return sortA - sortB
      }

      return String(a?.id || '').localeCompare(String(b?.id || ''))
    })
}

export function formatNoticeTime(value) {
  if (!value) {
    return '未知时间'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '未知时间'
  }

  return date.toLocaleString('zh-CN', { hour12: false })
}
