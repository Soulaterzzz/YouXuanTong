const STATUS_TYPE_MAP = {
  已完成: 'success',
  有效: 'success',
  待处理: 'warning',
  待生效: 'warning',
  已取消: 'danger',
  已过期: 'danger'
}

export function getStatusType(status) {
  return STATUS_TYPE_MAP[status] || ''
}

export function validateInsuranceBatchItems(items) {
  if (!Array.isArray(items) || items.length === 0) {
    return '暂无可更新保单'
  }

  for (const item of items) {
    if (!item.policyNo || !item.startDate || !item.endDate) {
      return '请完整填写保单号、起保日期、到期日期'
    }

    if (item.endDate < item.startDate) {
      return '到期日期不能早于起保日期'
    }
  }

  return ''
}

export function buildInsuranceBatchPayload(items) {
  return {
    items: items.map(item => ({
      insuranceId: item.insuranceId,
      policyNo: item.policyNo,
      effectiveDate: item.startDate,
      expiryDate: item.endDate
    }))
  }
}
