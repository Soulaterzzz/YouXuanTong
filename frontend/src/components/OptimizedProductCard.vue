<template>
  <div class="optimized-product-card">
    <!-- 产品图片区域 -->
    <div class="product-image-wrapper" @click="previewImage(product)">
      <img :src="getProductImage(product)" :alt="product.name" class="product-image">
      <div class="image-overlay">
        <el-icon><ZoomIn /></el-icon>
        <span>点击放大</span>
      </div>
      <!-- 标签 -->
      <div class="product-badges">
        <span class="badge new" v-if="product.isNew">新品</span>
        <span class="badge hot" v-if="product.isHot">热门</span>
      </div>
    </div>

    <!-- 产品内容区域 -->
    <div class="product-content">
      <!-- 标题和类别 -->
      <div class="product-header">
        <h3 class="product-name">{{ product.name }}</h3>
        <span class="product-category">{{ product.category }}</span>
      </div>

      <!-- 描述信息 -->
      <div class="product-description">
        <p class="description-text" v-if="product.description">{{ product.description }}</p>
        <p class="features-text" v-if="product.features">
          <el-icon><Star /></el-icon>
          {{ product.features }}
        </p>
      </div>

      <!-- 价格和库存信息 - 优化布局 -->
      <div class="product-info-row">
        <!-- 价格信息 -->
        <div class="info-item price-info">
          <span class="info-label">价格：</span>
          <span class="info-value price-value">¥{{ product.price }}</span>
        </div>
        <!-- 库存信息 -->
        <div class="info-item stock-info">
          <span class="info-label">库存：</span>
          <span class="info-value stock-value" :class="{ 'low-stock': product.stock < 10 }">
            {{ product.stock > 0 ? product.stock : '充足' }}
          </span>
        </div>
      </div>

      <!-- 操作按钮区域 -->
      <div class="product-actions" v-if="isAdmin">
        <el-button type="primary" size="small" @click="editProduct(product)">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button :type="product.saleStatus === 'ON_SALE' ? 'danger' : 'success'"
                   size="small"
                   @click="toggleProductStatus(product)">
          <el-icon><Switch /></el-icon>
          {{ product.saleStatus === 'ON_SALE' ? '下架' : '上架' }}
        </el-button>
        <el-button type="danger" size="small" @click="deleteProduct(product)">
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { ZoomIn, Star, Edit, Switch, Delete } from '@element-plus/icons-vue'

export default {
  name: 'OptimizedProductCard',
  components: {
    ZoomIn, Star, Edit, Switch, Delete
  },
  props: {
    product: {
      type: Object,
      required: true
    },
    isAdmin: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    previewImage(product) {
      // 预览图片逻辑
    },
    editProduct(product) {
      // 编辑产品逻辑
    },
    toggleProductStatus(product) {
      // 切换产品状态逻辑
    },
    deleteProduct(product) {
      // 删除产品逻辑
    },
    getProductImage(product) {
      // 获取产品图片逻辑
      return product.imageUrl || 'placeholder.png'
    }
  }
}
</script>

<style scoped>
.optimized-product-card {
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  height: 100%;
}

.optimized-product-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

/* 产品图片区域 */
.product-image-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f5f7fa;
  cursor: pointer;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.3s ease;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  color: #ffffff;
  gap: 8px;
}

.product-image-wrapper:hover .image-overlay {
  opacity: 1;
}

.product-image-wrapper:hover .product-image {
  transform: scale(1.05);
}

/* 产品标签 */
.product-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.badge.new {
  background: #67c23a;
  color: #ffffff;
}

.badge.hot {
  background: #e6a23c;
  color: #ffffff;
}

/* 产品内容区域 */
.product-content {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

/* 产品标题和类别 */
.product-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.product-name {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-category {
  font-size: 12px;
  padding: 4px 8px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  white-space: nowrap;
}

/* 产品描述 */
.product-description {
  margin-bottom: 12px;
  flex: 1;
  overflow: hidden;
}

.description-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.features-text {
  font-size: 13px;
  color: #909399;
  line-height: 1.4;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 价格和库存信息行 - 优化布局 */
.product-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-label {
  font-size: 13px;
  color: #909399;
}

.info-value {
  font-size: 15px;
  font-weight: 600;
}

.price-value {
  color: #f56c6c;
}

.stock-value {
  color: #67c23a;
}

.stock-value.low-stock {
  color: #e6a23c;
}

/* 操作按钮区域 */
.product-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.product-actions .el-button {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .optimized-product-card {
    flex-direction: row;
    height: auto;
  }

  .product-image-wrapper {
    width: 120px;
    height: 120px;
    flex-shrink: 0;
  }

  .product-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-name {
    font-size: 14px;
  }

  .product-info-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .product-actions {
    flex-direction: column;
  }
}
</style>
