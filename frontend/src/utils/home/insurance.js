export function buildInsuranceTimeline(row) {
  const entries = [
    {
      key: 'draft',
      title: '投保资料创建',
      time: row.createTime,
      type: 'info',
      content: row.statusCode === 'DRAFT'
        ? '资料已暂存为待提交，用户仍可继续补充后再发起审核。'
        : '投保资料已创建，并进入后续业务流。'
    },
    {
      key: 'submit',
      title: '提交审核',
      time: row.submitTime || (row.statusCode !== 'DRAFT' ? row.createTime : ''),
      type: 'primary',
      content: '资料已提交给管理员审核，并进入内部处理队列。'
    },
    {
      key: 'review',
      title: row.statusCode === 'REVIEW_REJECTED' ? '审核驳回' : '审核完成',
      time: row.reviewTime,
      type: row.statusCode === 'REVIEW_REJECTED' ? 'danger' : 'success',
      content: row.statusCode === 'REVIEW_REJECTED'
        ? (row.rejectReason || row.reviewComment || '审核未通过，等待重新处理。')
        : (row.reviewComment || '管理员已审核通过，等待进入承保。')
    },
    {
      key: 'underwriting',
      title: '进入承保',
      time: row.underwritingTime,
      type: 'warning',
      content: '平台已推进到承保阶段，等待保险公司完成正式承保。'
    },
    {
      key: 'active',
      title: '保单生效',
      time: row.activateTime || row.startDate,
      type: 'success',
      content: row.policyNo
        ? `正式保单号已下发：${row.policyNo}`
        : '保单已生效，正式保单号待同步。'
    }
  ]

  return entries.filter(item => {
    if (item.key === 'draft') return !!row.createTime
    if (item.key === 'submit') return !!item.time && row.statusCode !== 'DRAFT'
    if (item.key === 'review') {
      return !!item.time && ['APPROVED', 'REVIEW_REJECTED', 'UNDERWRITING', 'ACTIVE', 'EXPIRED', 'CANCELLED'].includes(row.statusCode)
    }
    if (item.key === 'underwriting') {
      return !!item.time && ['UNDERWRITING', 'ACTIVE', 'EXPIRED', 'CANCELLED'].includes(row.statusCode)
    }
    if (item.key === 'active') {
      return !!item.time && ['ACTIVE', 'EXPIRED'].includes(row.statusCode)
    }
    return false
  })
}

export function formatPolicyTime(value) {
  if (!value) return '待更新'
  const normalized = typeof value === 'string' ? value.replace('T', ' ') : value
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return normalized
  return date.toLocaleString('zh-CN', { hour12: false })
}
