<template>
  <article class="optimized-product-card">
    <button type="button" class="product-image-wrapper" @click="$emit('preview-image', product)">
      <img
        :src="getProductImage(product)"
        :alt="`保险产品 ${product.name}`"
        class="product-image"
        loading="lazy"
      >
      <div class="image-overlay">
        <el-icon><ZoomIn /></el-icon>
        <span>点击放大</span>
      </div>
    </button>

    <div class="product-content">
      <div class="product-header">
        <div class="title-stack">
          <div class="product-title-line">
            <h3 class="product-name">
              <span class="product-name-main">{{ product.name }}</span>
              <span v-if="isAdmin && product.alias" class="product-alias">{{ product.alias }}</span>
            </h3>
            <span v-if="product.isHot" class="product-tag hot">热销产品</span>
            <span v-else-if="product.isNew" class="product-tag new">新品</span>
          </div>
          <p v-if="getProductSummary(product)" class="product-summary">
            {{ getProductSummary(product) }}
          </p>
        </div>
        <div class="product-price-badge">
          <span class="price-symbol">¥</span>
          <span class="price-amount">{{ formatMoney(isAdmin ? product.price : (product.displayPrice || product.price)) }}</span>
        </div>
      </div>

      <div class="product-description">
        <p class="description-line" v-if="product.features">
          <span class="description-label">产品特点：</span>
          <span class="description-value">{{ normalizeIntroText(product.features) }}</span>
        </p>
        <p class="description-line" v-if="getTimingText(product)">
          <span class="description-label">{{ getTimingLabel(product) }}：</span>
          <span class="description-value">{{ getTimingText(product) }}</span>
        </p>
      </div>

      <div class="product-actions">
        <el-button type="info" plain size="small" @click="$emit('open-product-detail', product)">
          <el-icon><Document /></el-icon>
          详情
        </el-button>
        <el-button v-if="isAdmin" type="primary" size="small" @click="$emit('open-product-dialog', product)">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button
          v-if="isAdmin"
          :type="product.saleStatus === 'ON_SALE' ? 'danger' : 'success'"
          size="small"
          @click="$emit('toggle-product-status', product)"
        >
          <el-icon><Switch /></el-icon>
          {{ product.saleStatus === 'ON_SALE' ? '下架' : '上架' }}
        </el-button>
        <el-button v-if="isAdmin" type="danger" plain size="small" @click="$emit('delete-product', product)">
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
        <el-button
          type="warning"
          plain
          size="small"
          :disabled="!product.templateFileName"
          @click="$emit('open-batch-dialog', product)"
        >
          <el-icon><Upload /></el-icon>
          批量激活
        </el-button>
        <el-button
          type="primary"
          plain
          size="small"
          :disabled="!product.templateFileName"
          @click="$emit('download-template', product)"
        >
          <el-icon><Download /></el-icon>
          下载模板
        </el-button>
        <el-button
          type="warning"
          size="small"
          :disabled="product.saleStatus && product.saleStatus !== 'ON_SALE'"
          @click="$emit('open-activate-dialog', product)"
        >
          <el-icon><Lightning /></el-icon>
          激活
        </el-button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { ZoomIn, Edit, Switch, Delete, Upload, Download, Lightning, Document } from '@element-plus/icons-vue'
import { formatMoney, getProductImage } from '@/utils/home/product.js'

const INTRO_SPLIT_RE = /[；;。！？!?，,]+/
const TIMING_HINT_RE = /(T\+\d+|截单|提盘|回盘|生效|起保|等待期|上午|下午|晚上|凌晨|时|点|周[一二三四五六日天12345])/

const normalizeIntroSource = (value) => {
  if (value === null || value === undefined) {
    return ''
  }

  return String(value)
    .replace(/[\r\n]+/g, ' ')
    .replace(/[：:]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const normalizeIntroText = (value) => {
  return normalizeIntroSource(value)
    .replace(/[；;]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const splitIntroClauses = (value) => {
  const text = normalizeIntroSource(value)
  if (!text) {
    return []
  }

  return text
    .split(INTRO_SPLIT_RE)
    .map((part) => part.trim())
    .filter(Boolean)
}

const isTimingClause = (value) => TIMING_HINT_RE.test(value)

const getProductSummary = (product) => {
  const sourceClauses = splitIntroClauses(product.description || product.features || product.companyName || '')
  if (!sourceClauses.length) {
    return normalizeIntroText(getCategoryFallback(product.categoryCode))
  }

  const summaryClauses = sourceClauses.filter((clause) => !isTimingClause(clause))
  const pickedClauses = summaryClauses.length ? summaryClauses : sourceClauses
  const summaryText = pickedClauses.slice(0, 2).join(' ')

  return summaryText || normalizeIntroText(getCategoryFallback(product.categoryCode))
}

const getTimingText = (product) => {
  const sourceClauses = splitIntroClauses([product.description, product.features].filter(Boolean).join(' '))
  const timingClauses = sourceClauses.filter((clause) => isTimingClause(clause))

  if (timingClauses.length) {
    return timingClauses.join(' ')
  }

  if (product.companyName) {
    return normalizeIntroText(product.companyName)
  }

  return normalizeIntroText(getCategoryFallback(product.categoryCode))
}

const getTimingLabel = (product) => {
  const sourceClauses = splitIntroClauses([product.description, product.features].filter(Boolean).join(' '))
  const hasTimingInfo = sourceClauses.some((clause) => isTimingClause(clause))

  if (hasTimingInfo) {
    return '投保时间'
  }

  if (product.companyName) {
    return '承保公司'
  }

  return '产品分类'
}

const getCategoryFallback = (categoryCode) => {
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

  return categoryMap[categoryCode] || categoryCode || ''
}

defineProps({
  product: {
    type: Object,
    required: true
  },
  isAdmin: {
    type: Boolean,
    default: false
  }
})

defineEmits([
  'preview-image',
  'open-product-detail',
  'open-product-dialog',
  'toggle-product-status',
  'delete-product',
  'open-batch-dialog',
  'download-template',
  'open-activate-dialog'
])
</script>

<style scoped>
.optimized-product-card {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 0;
  align-items: stretch;
  overflow: hidden;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.optimized-product-card:hover {
  transform: translateY(-2px);
  border-color: rgba(230, 0, 18, 0.18);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
}

.product-image-wrapper {
  position: relative;
  width: 100%;
  min-height: 160px;
  border: 0;
  padding: 0;
  cursor: pointer;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #e6ebf2 100%);
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.product-image-wrapper:hover .product-image {
  transform: scale(1.04);
}

.image-overlay {
  position: absolute;
  inset: auto 0 0;
  padding: 14px 12px 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  background: linear-gradient(180deg, transparent 0%, rgba(15, 23, 42, 0.76) 100%);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.product-image-wrapper:hover .image-overlay {
  opacity: 1;
}

.product-content {
  padding: 18px 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.product-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title-stack {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.product-name {
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px;
  font-size: 20px;
  line-height: 1.18;
  font-weight: 800;
  color: #101828;
}

.product-name-main {
  min-width: 0;
  overflow-wrap: anywhere;
}

.product-alias {
  flex-shrink: 0;
  color: #e60012;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

.product-tag {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.product-tag.hot {
  border: 1px solid #c8dbff;
  background: #eaf2ff;
  color: #2f62c9;
}

.product-tag.new {
  border: 1px solid #f7d79f;
  background: #fff6e4;
  color: #c67a00;
}

.product-summary {
  margin: 0;
  font-size: 17px;
  line-height: 1.5;
  font-weight: 700;
  color: #1f2937;
  letter-spacing: 0.01em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-price-badge {
  flex-shrink: 0;
  display: flex;
  align-items: baseline;
  gap: 2px;
  margin-top: 2px;
  color: var(--color-primary);
  font-weight: 800;
}

.price-symbol {
  font-size: 14px;
  line-height: 1;
}

.price-amount {
  font-size: 22px;
  line-height: 1;
  letter-spacing: -0.02em;
}

.product-description {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.description-line {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  color: #8c6b5c;
  display: flex;
  gap: 0;
  flex-wrap: wrap;
}

.description-label {
  flex-shrink: 0;
  font-weight: 700;
  color: #9a7667;
}

.description-value {
  flex: 1;
  min-width: 0;
  overflow-wrap: anywhere;
}

.product-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  align-items: center;
}

.product-actions .el-button {
  flex: 0 0 auto;
  border-radius: 10px;
}

@media screen and (max-width: 768px) {
  .optimized-product-card {
    grid-template-columns: 110px minmax(0, 1fr);
  }

  .product-image-wrapper {
    min-height: 130px;
  }

  .product-content {
    padding: 12px;
  }

  .product-name {
    font-size: 18px;
  }

  .product-summary {
    font-size: 15px;
  }

  .price-amount {
    font-size: 18px;
  }

  .description-line {
    font-size: 13px;
  }

  .product-actions .el-button {
    min-height: 40px;
  }
}

@media screen and (max-width: 480px) {
  .optimized-product-card {
    grid-template-columns: 1fr;
  }

  .product-image-wrapper {
    min-height: 160px;
  }

  .product-actions .el-button {
    width: 100%;
    justify-content: center;
  }
}

@media (pointer: coarse) {
  .product-actions .el-button {
    min-height: 44px;
  }
}
</style>
