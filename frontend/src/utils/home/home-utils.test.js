import { describe, expect, it } from 'vitest'
import { buildAdminAnalysisRange } from './date'
import { cloneNoticeList, createNoticeId, normalizeNoticeItem } from './notice'
import { formatMoney, formatStock, getCategoryLabel, getProductImage, getSaleStatusLabel } from './product'
import { buildQueryParams, resolveDownloadFileName } from './download'
import { validateProductImageFile, validateProductInsuranceBatchFile, validateProductTemplateFile } from './upload'
import { buildInsuranceTimeline, formatPolicyTime } from './insurance'

describe('home utils', () => {
  it('构建管理分析区间', () => {
    expect(buildAdminAnalysisRange('MONTH', new Date('2026-04-08T12:00:00Z'))).toEqual(['2026-04-01', '2026-04-08'])
    expect(buildAdminAnalysisRange('YEAR', new Date('2026-04-08T12:00:00Z'))[0]).toBe('2026-01-01')
  })

  it('归一化通知并生成克隆列表', () => {
    const item = normalizeNoticeItem({ title: '  标题 ', content: ' 内容 ', sortNo: 2, publishedAt: '2026-04-01' }, () => 'notice-1')
    expect(item).toEqual({
      id: 'notice-1',
      title: '标题',
      content: '内容',
      sortNo: 2,
      publishedAt: '2026-04-01'
    })
    expect(normalizeNoticeItem({ title: ' ', content: 'x' })).toBeNull()
    expect(cloneNoticeList([item])).toEqual([{ ...item }])
    expect(createNoticeId(1000)).toContain('1000-')
  })

  it('格式化产品信息', () => {
    expect(getProductImage({ id: 9 })).toBe('/api/images/product/9')
    expect(getCategoryLabel('child')).toBe('少儿医疗')
    expect(getSaleStatusLabel('ON_SALE')).toBe('已上架')
    expect(formatMoney('12.3')).toBe('12.30')
    expect(formatStock(8)).toBe('8 份')
  })

  it('构建下载查询和文件名', () => {
    expect(buildQueryParams({ plan: 'all', status: 'ACTIVE', serialNo: 'A-1' })).toBe('status=ACTIVE&serialNo=A-1')
    expect(resolveDownloadFileName("attachment; filename*=UTF-8''test%20file.pdf", 'fallback.pdf')).toBe('test file.pdf')
    expect(resolveDownloadFileName('attachment; filename="plain.pdf"', 'fallback.pdf')).toBe('plain.pdf')
  })

  it('校验上传文件', () => {
    expect(validateProductImageFile({ type: 'image/png', size: 1024 }, true)).toEqual({ valid: true, message: '' })
    expect(validateProductTemplateFile({ type: 'text/plain', size: 1024, name: 'a.txt' }, true)).toEqual({ valid: true, message: '' })
    expect(validateProductInsuranceBatchFile({ name: 'batch.xlsx', size: 1024 })).toEqual({ valid: true, message: '' })
  })

  it('构建保单时间线和格式化时间', () => {
    const timeline = buildInsuranceTimeline({
      statusCode: 'ACTIVE',
      createTime: '2026-04-01T08:00:00',
      submitTime: '2026-04-02T08:00:00',
      reviewTime: '2026-04-03T08:00:00',
      underwritingTime: '2026-04-04T08:00:00',
      activateTime: '2026-04-05T08:00:00',
      policyNo: 'P-001',
      reviewComment: '通过'
    })

    expect(timeline.length).toBeGreaterThan(0)
    expect(formatPolicyTime('2026-04-01T08:00:00')).toContain('2026/4/1')
  })
})
