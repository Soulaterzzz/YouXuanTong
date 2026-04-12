import { formatMoney, getCategoryLabel, getSaleStatusLabel } from './product.js'

const COMPANY_HOTLINES = {
  '中国人民财产保险股份有限公司': '95518',
  '人保财险': '95518',
  '中国人保财险': '95518',
  '平安财险': '95511',
  '平安保险': '95511',
  '平安人寿': '95511',
  '众安保险': '1010-9955',
  '太平洋保险': '95500',
  '国寿财险': '95519',
  '中国人寿财险': '95519'
}

const DETAIL_SPLIT_RE = /[；;。！？!?\n]+/

function normalizeText(value) {
  if (value === null || value === undefined) {
    return ''
  }

  return String(value)
    .replace(/[\r\n]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function normalizeMultilineText(value) {
  if (value === null || value === undefined) {
    return ''
  }

  return String(value)
    .replace(/\r\n?/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

export function splitDetailClauses(value) {
  const text = normalizeText(value)
  if (!text) {
    return []
  }

  return text
    .split(DETAIL_SPLIT_RE)
    .map((part) => part.trim())
    .filter(Boolean)
}

export function getCompanyHotline(companyName) {
  const normalizedName = normalizeText(companyName)
  return COMPANY_HOTLINES[normalizedName] || '请以承保公司官方客服为准'
}

export function buildProductDetailCopy(product) {
  const descriptionClauses = splitDetailClauses(product?.description)
  const featureClauses = splitDetailClauses(product?.features)
  const companyName = normalizeText(product?.companyName) || '承保公司待配置'
  const categoryLabel = getCategoryLabel(product?.categoryCode)
  const saleStatusLabel = getSaleStatusLabel(product?.saleStatus)
  const hotline = getCompanyHotline(product?.companyName)
  const basePrice = formatMoney(product?.price)
  const displayPrice = formatMoney(product?.displayPrice ?? product?.price)
  const detailText = normalizeMultilineText(product?.detailText) || [
    normalizeText(product?.description) ? `产品描述：${normalizeText(product?.description)}` : '',
    normalizeText(product?.features) ? `产品特点：${normalizeText(product?.features)}` : ''
  ].filter(Boolean).join('\n\n') || '当前产品详情正在补充中，请联系管理员完善正文内容。'

  const highlights = []
  if (descriptionClauses.length > 0) {
    highlights.push(descriptionClauses[0])
  }
  if (descriptionClauses.length > 1) {
    highlights.push(descriptionClauses[1])
  }
  if (featureClauses.length > 0) {
    highlights.push(featureClauses[0])
  }
  if (featureClauses.length > 1) {
    highlights.push(featureClauses[1])
  }

  while (highlights.length < 4) {
    highlights.push([
      '具体承保范围、等待期和免责责任以正式条款为准。',
      '如页面展示价格为空，将回落到基础价格。',
      '年龄、职业、区域和份数限制以投保时系统校验为准。',
      '报案、理赔与变更信息请以保单号联系承保公司官方客服。'
    ][highlights.length])
  }

  const coverageItems = (featureClauses.length ? featureClauses : descriptionClauses).map((text, index) => ({
    title: `保障 ${index + 1}`,
    value: text
  }))

  if (!coverageItems.length) {
    coverageItems.push({
      title: '保障内容',
      value: '请以正式条款和投保页面展示为准。'
    })
  }

  const planNotes = [
    `本页面展示的是 ${product?.name || '当前产品'} 的核心配置，具体承保范围、等待期、投保年龄、职业限制和免责责任以正式条款为准。`,
    `展示价格优先使用显示价格；如显示价格为空，则回落到基础价格。`,
    `实际投保时请以系统校验结果为准，若年龄、职业、区域或份数不满足条件，系统会提示无法投保。`,
    `如需报案、理赔或补充材料，请保留保单号并联系承保公司官方客服。`
  ]

  const facts = [
    { label: '承保公司', value: companyName },
    { label: '客服电话', value: hotline },
    { label: '产品编码', value: normalizeText(product?.productCode) || '-' },
    { label: '产品分类', value: categoryLabel },
    { label: '基础价格', value: `¥${basePrice}` },
    { label: '展示价格', value: `¥${displayPrice}` },
    { label: '销售状态', value: saleStatusLabel }
  ]

  const introText = [
    normalizeText(product?.description),
    normalizeText(product?.features)
  ].filter(Boolean).join(' · ') || '产品详情正在补充中，请以投保页面和正式条款为准。'

  return {
    title: normalizeText(product?.alias) || normalizeText(product?.name) || '产品详情',
    companyName,
    categoryLabel,
    saleStatusLabel,
    hotline,
    basePrice,
    displayPrice,
    introText,
    highlights,
    coverageItems,
    planNotes,
    facts,
    detailText,
  }
}
