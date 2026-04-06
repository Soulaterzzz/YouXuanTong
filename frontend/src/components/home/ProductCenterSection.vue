<template>
  <div class="page product-center-page">
    <aside class="sidebar">
      <div class="sidebar-section">
        <h3 class="sidebar-title">
          <el-icon><Grid /></el-icon>
          产品类别
        </h3>
        <el-menu
          ref="categoryMenuRef"
          :default-active="activeCategory"
          class="el-menu-vertical-demo"
          @select="$emit('category-select', $event)"
        >
          <el-menu-item index="all">
            <el-icon><Menu /></el-icon>
            全部产品
          </el-menu-item>
          <el-menu-item v-for="category in categoryList" :key="category.code" :index="category.code">
            <el-icon><component :is="getCategoryIcon(category.code)" /></el-icon>
            {{ category.name }}
          </el-menu-item>
        </el-menu>
      </div>
    </aside>

    <section class="content product-center-content">
      <div class="product-filter">
        <div class="filter-header">
          <h3><el-icon><Filter /></el-icon> 产品筛选</h3>
          <div class="filter-header-actions">
            <el-button v-if="isAdmin" type="warning" plain @click="$emit('open-category-dialog')">
              <el-icon><Setting /></el-icon>
              管理类别
            </el-button>
            <el-button v-if="isAdmin" type="success" plain @click="$emit('open-company-dialog')">
              <el-icon><Setting /></el-icon>
              管理公司
            </el-button>
            <el-button text @click="$emit('reset-filter')" v-if="companyFilter !== 'all' || activeCategory !== 'all'">
              <el-icon><Refresh /></el-icon>
              重置筛选
            </el-button>
            <el-button v-if="isAdmin" type="primary" @click="$emit('add-product')">
              <el-icon><Plus /></el-icon>
              添加产品
            </el-button>
          </div>
        </div>
        <div class="filter-row">
          <span class="filter-label">承保公司：</span>
          <el-radio-group :model-value="companyFilter" @change="$emit('company-change', $event)" size="default" class="product-company-group">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button v-for="company in companyList" :key="company.code" :label="company.code">
              {{ company.name }}
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div class="product-stats">
        <span class="stats-text">共 <strong>{{ productTotal }}</strong> 个产品</span>
        <span class="stats-divider" v-if="companyFilter !== 'all'">|</span>
        <span class="filter-status" v-if="companyFilter !== 'all'">
          当前筛选：{{ filterText }}
        </span>
      </div>

      <div class="product-list" v-loading="productLoading">
        <div class="product-item" v-for="product in products" :key="product.id">
          <div class="product-card pc-product-card">
            <div class="product-image-wrapper pc-product-media" @click="$emit('preview-image', product)">
              <img :src="getProductImage(product)" :alt="product.name" class="product-img">
              <div class="image-overlay">
                <el-icon><ZoomIn /></el-icon>
                <span>点击放大</span>
              </div>
              <div class="product-badges">
                <span class="badge new" v-if="product.isNew">新品</span>
                <span class="badge hot" v-if="product.isHot">热门</span>
              </div>
            </div>
            <div class="product-content pc-product-body">
              <div class="product-header">
                <h3 class="product-name">{{ product.name }}</h3>
                <span
                  class="status-badge"
                  :class="product.saleStatus === 'ON_SALE' ? 'on-sale' : 'off-sale'"
                >
                  {{ getSaleStatusLabel(product.saleStatus) }}
                </span>
              </div>
              <div class="product-meta">
                <span class="meta-chip meta-chip-primary">
                  {{ product.companyName || '未配置公司' }}
                </span>
                <span class="meta-chip">
                  {{ getCategoryLabel(product.categoryCode) }}
                </span>
                <span class="meta-chip" :class="product.templateFileName ? 'meta-chip-success' : 'meta-chip-muted'">
                  {{ product.templateFileName ? '模板已就绪' : '待上传模板' }}
                </span>
              </div>
              <div class="product-desc">
                <p class="desc-text">{{ product.description }}</p>
                <p class="features-text" v-if="product.features">
                  <el-icon><Star /></el-icon>
                  {{ product.features }}
                </p>
              </div>
              <div class="product-footer">
                <div class="product-stats-row">
                  <div class="price-info">
                    <span class="price-label">价格</span>
                    <span class="price-value">¥{{ formatMoney(product.price) }}</span>
                  </div>
                  <div class="stock-info">
                    <span class="stock-label">库存</span>
                    <span class="stock-value" :class="{ 'low-stock': product.stock > 0 && product.stock < 10 }">
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
            </div>
          </div>
        </div>
        <el-empty v-if="products.length === 0 && !productLoading" description="暂无产品数据" />
      </div>

      <div class="pagination" v-if="products.length > 0">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="productTotal"
          :page-size="productPageSize"
          :current-page="productCurrentPage"
          :page-sizes="[5, 10, 20, 50]"
          @size-change="$emit('size-change', $event)"
          @current-change="$emit('page-change', $event)"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { Sunny, Star, Present, Grid, Menu, Filter, Setting, Refresh, Plus, ZoomIn, Edit, Switch, Delete, Upload, Download, Lightning } from '@element-plus/icons-vue'

const props = defineProps({
  isAdmin: { type: Boolean, default: false },
  activeCategory: { type: String, default: 'all' },
  categoryList: { type: Array, default: () => [] },
  companyFilter: { type: String, default: 'all' },
  companyList: { type: Array, default: () => [] },
  filterText: { type: String, default: '' },
  products: { type: Array, default: () => [] },
  productLoading: { type: Boolean, default: false },
  productTotal: { type: Number, default: 0 },
  productPageSize: { type: Number, default: 10 },
  productCurrentPage: { type: Number, default: 1 }
})

defineEmits([
  'category-select',
  'company-change',
  'reset-filter',
  'open-category-dialog',
  'open-company-dialog',
  'add-product',
  'preview-image',
  'open-product-dialog',
  'toggle-product-status',
  'delete-product',
  'open-batch-dialog',
  'download-template',
  'open-activate-dialog',
  'page-change',
  'size-change'
])

const getProductImage = (product) => `/api/images/product/${product.id}`

const categoryMenuRef = ref(null)

const syncCategoryMenu = async (value) => {
  await nextTick()
  const menu = categoryMenuRef.value
  if (!menu) return
  if (typeof menu.updateActiveIndex === 'function') {
    menu.updateActiveIndex(value)
    return
  }
  menu.activeIndex = value
}

watch(
  () => props.activeCategory,
  (value) => {
    syncCategoryMenu(value)
  },
  { immediate: true }
)

const getCategoryIcon = (categoryCode) => {
  if (/^1-/.test(categoryCode)) {
    return Sunny
  }
  if (categoryCode === 'child') {
    return Star
  }
  return Present
}

const getCategoryLabel = (categoryCode) => {
  const categoryMap = {
    medical: '医疗险',
    critical: '重疾险',
    accident: '意外险',
    life: '人寿险',
    car: '车险'
  }
  return categoryMap[categoryCode] || categoryCode || '未分类'
}

const getSaleStatusLabel = (saleStatus) => {
  const statusMap = {
    ON_SALE: '已上架',
    OFF_SALE: '已下架'
  }
  return statusMap[saleStatus] || saleStatus || '未知状态'
}

const formatMoney = (value) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : String(value)
}

const formatStock = (stock) => {
  if (stock === null || stock === undefined || stock === '') {
    return '充足'
  }
  const num = Number(stock)
  if (!Number.isFinite(num)) {
    return String(stock)
  }
  if (num <= 0) {
    return '充足'
  }
  return `${num} 份`
}
</script>

<style scoped>
.product-center-page {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  width: 100%;
  min-width: 0;
  flex: 1;
}

.product-center-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-center-content .product-filter {
  padding: 22px 22px 18px;
  border-radius: 20px;
  border: 1px solid #e8edf4;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfd 100%);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.product-center-content .product-filter .filter-header {
  margin-bottom: 18px;
}

.product-center-content .product-filter .filter-header h3 {
  font-size: 18px;
  line-height: 1.2;
  font-weight: 700;
  color: #172033;
}

.product-center-content .product-filter .filter-row {
  gap: 12px;
}

.product-center-content .product-stats {
  margin-bottom: 8px;
  padding: 0 4px;
  font-size: 13px;
  color: #6b7280;
}

.product-center-content .product-stats .stats-text strong {
  color: #003b72;
}

.product-center-content .product-stats .filter-status {
  background: rgba(0, 59, 114, 0.08);
  color: #003b72;
  border-radius: 999px;
  padding: 4px 12px;
}

.product-center-content .product-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-center-content .product-item {
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid #ebeff5;
  background: #fff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.product-center-content .product-item:hover {
  transform: translateY(-2px);
  border-color: #d9e2ef;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.09);
}

.product-center-content .pc-product-card {
  display: grid !important;
  grid-template-columns: 108px minmax(0, 1fr) !important;
  column-gap: 12px;
  align-items: stretch;
  min-height: 118px !important;
}

@media screen and (max-width: 1200px) {
  .product-center-page {
    flex-direction: column;
    align-items: stretch;
  }

  .product-center-content .pc-product-card {
    grid-template-columns: 100px minmax(0, 1fr) !important;
  }
}

.product-center-content .pc-product-media {
  position: relative;
  min-height: 118px !important;
  width: 108px;
  min-width: 108px;
  height: 100%;
  cursor: pointer;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.product-center-content .pc-product-media .product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.product-center-content .pc-product-media:hover .product-img {
  transform: scale(1.05);
}

.product-center-content .pc-product-media .image-overlay {
  position: absolute;
  inset: auto 0 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  color: #fff;
  padding: 14px 14px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.product-center-content .pc-product-media:hover .image-overlay {
  opacity: 1;
}

.product-center-content .product-badges {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 6px;
}

.product-center-content .badge {
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 999px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.product-center-content .badge.new {
  background: rgba(230, 0, 18, 0.92);
  color: #fff;
  box-shadow: 0 4px 12px rgba(230, 0, 18, 0.28);
}

.product-center-content .badge.hot {
  background: rgba(255, 152, 0, 0.92);
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 152, 0, 0.26);
}

.product-center-content .pc-product-body {
  padding: 11px 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
}

.product-center-content .product-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 0;
}

.product-center-content .product-name {
  margin: 0;
  flex: 1;
  padding-right: 0;
  font-size: 15px;
  line-height: 1.2;
  font-weight: 700;
  color: #152033;
}

.product-center-content .status-badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 0.04em;
}

.product-center-content .status-badge.on-sale {
  background: rgba(46, 125, 50, 0.1);
  color: #2e7d32;
}

.product-center-content .status-badge.off-sale {
  background: rgba(198, 40, 40, 0.1);
  color: #c62828;
}

.product-center-content .product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.product-center-content .meta-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 7px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  border: 1px solid #e6ebf2;
  background: #f7f9fc;
  color: #526071;
}

.product-center-content .meta-chip-primary {
  background: rgba(0, 59, 114, 0.08);
  border-color: rgba(0, 59, 114, 0.12);
  color: #003b72;
}

.product-center-content .meta-chip-success {
  background: rgba(46, 125, 50, 0.08);
  border-color: rgba(46, 125, 50, 0.12);
  color: #2e7d32;
}

.product-center-content .meta-chip-muted {
  background: #f4f6f8;
  border-color: #e7ecf3;
  color: #7a8495;
}

.product-center-content .product-desc {
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.product-center-content .desc-text {
  margin: 0;
  font-size: 12px;
  line-height: 1.45;
  color: #4f596b;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-center-content .features-text {
  margin: 0;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 5px 8px;
  border-radius: 9px;
  background: #f7f9fc;
  color: #556072;
  font-size: 11px;
  line-height: 1.35;
}

.product-center-content .features-text .el-icon {
  color: #e6a23c;
  margin-top: 2px;
}

.product-center-content .product-footer {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 8px;
  margin-top: auto;
  border-top: 1px solid #eef2f7;
}

.product-center-content .product-stats-row {
  display: flex;
  align-items: stretch;
  gap: 6px;
  flex-wrap: wrap;
}

.product-center-content .price-info,
.product-center-content .stock-info {
  flex: 1 1 120px;
  padding: 7px 8px;
  border-radius: 10px;
  border: 1px solid #e7eef9;
  background: #f8fbff;
}

.product-center-content .price-label,
.product-center-content .stock-label {
  display: block;
  margin-bottom: 3px;
  font-size: 10px;
  color: #7a8496;
}

.product-center-content .price-value {
  font-size: 14px;
  line-height: 1.1;
  font-weight: 800;
  color: #e60012;
}

.product-center-content .stock-value {
  font-size: 12px;
  font-weight: 700;
  color: #2e7d32;
}

.product-center-content .stock-value.low-stock {
  color: #e64545;
}

.product-center-content .product-actions {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 4px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.product-center-content .product-actions .el-button {
  flex: 0 0 auto;
  border-radius: 10px;
  padding: 3px 7px;
  min-height: 26px;
  font-size: 10px;
  white-space: nowrap;
}

.product-center-content .pagination {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

@media screen and (max-width: 768px) {
  .product-center-content .product-filter {
    padding: 18px 16px 14px;
  }

  .product-center-content .product-filter .filter-header,
  .product-center-content .product-filter .filter-row {
    align-items: flex-start;
  }

  .product-center-content .product-filter .filter-header {
    flex-direction: column;
  }

  .product-center-content .pc-product-card {
    grid-template-columns: 92px minmax(0, 1fr) !important;
    min-height: 112px !important;
  }

  .product-center-content .pc-product-media {
    min-height: 112px !important;
    width: 92px;
    min-width: 92px;
  }

  .product-center-content .pc-product-body {
    padding: 10px 10px 8px;
  }

  .product-center-content .product-name {
    font-size: 14px;
  }

  .product-center-content .product-stats-row {
    gap: 10px;
  }

  .product-center-content .product-actions {
    gap: 4px;
  }

  .product-center-content .product-actions .el-button {
    flex: 0 0 auto;
  }

  .product-center-content .pagination {
    justify-content: center;
  }
}

@media screen and (max-width: 480px) {
  .product-center-content .pc-product-card {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .product-center-content .pc-product-media {
    min-height: 116px;
    width: 100%;
    min-width: 0;
  }

  .product-center-content .price-info,
  .product-center-content .stock-info {
    flex-basis: 100%;
  }

  .product-center-content .product-actions .el-button {
    flex-basis: 100%;
  }
}
</style>
