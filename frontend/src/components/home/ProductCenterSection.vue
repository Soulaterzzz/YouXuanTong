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
        <div class="product-search-row">
          <el-input
            :model-value="productSearchKeyword"
            class="product-search-input"
            placeholder="搜索产品名称"
            clearable
            @update:model-value="$emit('search-keyword-change', $event)"
            @keyup.enter="$emit('search-product')"
            @clear="$emit('clear-product-search')"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" plain @click="$emit('search-product')">
            搜索
          </el-button>
        </div>

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
            <el-button
              v-if="companyFilter !== 'all' || activeCategory !== 'all' || productSearchKeyword"
              text
              @click="$emit('reset-filter')"
            >
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
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button v-for="company in companyList" :key="company.code" :value="company.code">
              {{ company.name }}
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div class="product-stats">
        <span class="stats-text">共 <strong>{{ productTotal }}</strong> 个产品</span>
        <span class="stats-divider" v-if="filterText">|</span>
        <span class="filter-status" v-if="filterText">
          当前筛选：{{ filterText }}
        </span>
      </div>

      <div class="product-list" v-loading="productLoading">
        <OptimizedProductCard
          v-for="product in products"
          :key="product.id"
          v-memo="[isAdmin, product.id, product.name, product.alias, product.categoryCode, product.companyName, product.saleStatus, product.price, product.displayPrice, product.isNew, product.isHot, product.description, product.features, product.detailText, product.imageUrl, product.templateFileName]"
          :product="product"
          :is-admin="isAdmin"
          @preview-image="$emit('preview-image', $event)"
          @open-product-detail="$emit('open-product-detail', $event)"
          @open-product-dialog="$emit('open-product-dialog', $event)"
          @toggle-product-status="$emit('toggle-product-status', $event)"
          @delete-product="$emit('delete-product', $event)"
          @open-batch-dialog="$emit('open-batch-dialog', $event)"
          @download-template="$emit('download-template', $event)"
          @open-activate-dialog="$emit('open-activate-dialog', $event)"
        />
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
import { Sunny, Star, Present, Grid, Menu, Filter, Setting, Refresh, Plus, Search } from '@element-plus/icons-vue'
import OptimizedProductCard from '@/components/OptimizedProductCard.vue'

const props = defineProps({
  isAdmin: { type: Boolean, default: false },
  activeCategory: { type: String, default: 'all' },
  categoryList: { type: Array, default: () => [] },
  companyFilter: { type: String, default: 'all' },
  companyList: { type: Array, default: () => [] },
  filterText: { type: String, default: '' },
  productSearchKeyword: { type: String, default: '' },
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
  'search-keyword-change',
  'search-product',
  'clear-product-search',
  'preview-image',
  'open-product-detail',
  'open-product-dialog',
  'toggle-product-status',
  'delete-product',
  'open-batch-dialog',
  'download-template',
  'open-activate-dialog',
  'page-change',
  'size-change'
])

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
  min-width: 0;
  overflow-x: hidden;
}

.product-center-content .product-filter {
  padding: 22px 22px 18px;
  border-radius: 20px;
  border: 1px solid #e8edf4;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfd 100%);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.product-search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.product-search-input {
  flex: 1;
  min-width: 260px;
  max-width: 420px;
}

.product-search-row .el-button {
  min-height: 40px;
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
  color: var(--color-secondary);
  border-radius: 999px;
  padding: 4px 12px;
}

.product-center-content .product-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-center-content .pagination {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

@media screen and (max-width: 1200px) {
  .product-search-row {
    align-items: stretch;
  }

  .product-search-input {
    max-width: none;
  }

  .product-center-page {
    flex-direction: column;
    align-items: stretch;
  }

  .product-center-page .sidebar,
  .product-center-content {
    width: 100%;
  }

  .product-center-page .sidebar {
    position: static;
    top: auto;
  }
}

@media screen and (max-width: 768px) {
  .product-center-content .product-filter {
    padding: 18px 16px 14px;
  }

  .product-center-content .product-filter .filter-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-center-content .filter-header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .product-center-content .product-filter .filter-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-center-content .product-stats {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .product-center-content .filter-label {
    min-width: 0;
  }

  .product-center-content .product-company-group {
    width: 100%;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none;
    -ms-overflow-style: none;
  }

  .product-center-content .product-company-group::-webkit-scrollbar {
    display: none;
  }

  .product-center-content .product-company-group .el-radio-button {
    flex: 0 0 auto;
  }

  .product-center-content .pagination {
    justify-content: center;
  }
}

@media screen and (max-width: 480px) {
  .product-center-content .product-filter {
    padding: 16px 14px 12px;
  }

  .product-search-row {
    flex-direction: column;
  }

  .product-search-row .el-button {
    width: 100%;
    justify-content: center;
  }

  .product-center-content .filter-header-actions .el-button {
    width: 100%;
    justify-content: center;
  }
}
</style>
