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
