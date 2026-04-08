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
      <div class="product-badges">
        <span class="badge new" v-if="product.isNew">新品</span>
        <span class="badge hot" v-if="product.isHot">热门</span>
      </div>
    </button>

    <div class="product-content">
      <div class="product-header">
        <div class="title-stack">
          <h3 class="product-name">{{ product.name }}</h3>
          <span class="product-category">{{ getCategoryLabel(product.categoryCode) }}</span>
        </div>
        <span
          class="product-status"
          :class="product.saleStatus === 'ON_SALE' ? 'on-sale' : 'off-sale'"
        >
          {{ getSaleStatusLabel(product.saleStatus) }}
        </span>
      </div>

      <div class="product-description">
        <p class="description-text" v-if="product.description">{{ product.description }}</p>
        <p class="features-text" v-if="product.features">
          <el-icon><Star /></el-icon>
          {{ product.features }}
        </p>
      </div>

      <div class="product-info-row">
        <div class="info-item price-info">
          <span class="info-label">价格</span>
          <span class="info-value price-value">¥{{ formatMoney(product.price) }}</span>
        </div>
        <div class="info-item stock-info">
          <span class="info-label">库存</span>
          <span class="info-value stock-value" :class="{ 'low-stock': product.stock > 0 && product.stock < 10 }">
            {{ formatStock(product.stock) }}
          </span>
        </div>
      </div>

      <div class="product-actions">
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
import { ZoomIn, Star, Edit, Switch, Delete, Upload, Download, Lightning } from '@element-plus/icons-vue'
import { formatMoney, formatStock, getCategoryLabel, getProductImage, getSaleStatusLabel } from '@/utils/home/product.js'

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
  grid-template-columns: 112px minmax(0, 1fr);
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
  min-height: 100%;
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

.product-badges {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #fff;
}

.badge.new {
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff4d5a 100%);
}

.badge.hot {
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%);
}

.product-content {
  padding: 14px 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
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
}

.product-name {
  margin: 0;
  font-size: 16px;
  line-height: 1.35;
  font-weight: 800;
  color: #101828;
}

.product-category {
  display: inline-flex;
  margin-top: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(0, 59, 114, 0.08);
  color: var(--color-secondary);
  font-size: 11px;
  font-weight: 700;
}

.product-status {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.product-status.on-sale {
  background: rgba(46, 125, 50, 0.1);
  color: #2e7d32;
}

.product-status.off-sale {
  background: rgba(198, 40, 40, 0.1);
  color: #c62828;
}

.product-description {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.description-text {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #475467;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.features-text {
  margin: 0;
  padding: 6px 10px;
  border-radius: 10px;
  background: #f8fafc;
  color: #667085;
  font-size: 11px;
  line-height: 1.45;
  display: flex;
  gap: 6px;
  align-items: flex-start;
}

.features-text .el-icon {
  color: #f59e0b;
  margin-top: 2px;
}

.product-info-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.info-item {
  flex: 1 1 120px;
  padding: 8px 10px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: #fff;
}

.info-label {
  display: block;
  margin-bottom: 4px;
  font-size: 10px;
  color: #98a2b3;
}

.info-value {
  font-size: 14px;
  font-weight: 800;
}

.price-value {
  color: var(--color-primary);
}

.stock-value {
  color: #2e7d32;
}

.stock-value.low-stock {
  color: #e64545;
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
    grid-template-columns: 96px minmax(0, 1fr);
  }

  .product-content {
    padding: 12px;
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
    min-height: 140px;
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
