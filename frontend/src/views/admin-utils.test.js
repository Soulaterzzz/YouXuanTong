import { describe, expect, it } from 'vitest'
import { buildInsuranceBatchPayload, getStatusType, validateInsuranceBatchItems } from './admin-utils'

describe('admin-utils', () => {
  it('状态标签映射正确', () => {
    expect(getStatusType('待生效')).toBe('warning')
    expect(getStatusType('已完成')).toBe('success')
    expect(getStatusType('未知状态')).toBe('')
  })

  it('批量生效列表为空时返回提示', () => {
    expect(validateInsuranceBatchItems([])).toBe('暂无可更新保单')
  })

  it('批量生效缺少必填字段时返回提示', () => {
    expect(validateInsuranceBatchItems([
      { insuranceId: 1, policyNo: '', startDate: '2026-03-01', endDate: '2026-03-31' }
    ])).toBe('请完整填写保单号、起保日期、到期日期')
  })

  it('批量生效日期逆序时返回提示', () => {
    expect(validateInsuranceBatchItems([
      { insuranceId: 1, policyNo: 'P-001', startDate: '2026-03-31', endDate: '2026-03-01' }
    ])).toBe('到期日期不能早于起保日期')
  })

  it('批量生效载荷映射正确', () => {
    expect(buildInsuranceBatchPayload([
      { insuranceId: 9, policyNo: 'BX-20260327001', startDate: '2026-03-27', endDate: '2027-03-26' }
    ])).toEqual({
      items: [
        {
          insuranceId: 9,
          policyNo: 'BX-20260327001',
          effectiveDate: '2026-03-27',
          expiryDate: '2027-03-26'
        }
      ]
    })
  })
})
