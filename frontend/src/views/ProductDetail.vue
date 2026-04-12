<template>
  <div class="app-container product-detail-route">
    <AppHeader
      :active-menu="'product'"
      :is-admin="isAdmin"
      :current-user="currentUser"
      :balance="balance"
      @select="handleMenuSelect"
      @logout="handleLogout"
    />

    <main class="main-content product-detail-main">
      <section class="detail-paper surface-panel" v-loading="loading">
        <div class="detail-page-toolbar">
          <el-button text class="detail-back-button" @click="backToProductCenter">
            <el-icon><ArrowLeft /></el-icon>
            返回产品中心
          </el-button>
          <el-button type="primary" plain @click="goHomeProductCenter">
            产品中心
          </el-button>
        </div>

        <template v-if="product">
          <div class="detail-doc-head">
            <div class="detail-kicker">产品独立详情页</div>
            <h1 class="detail-title">{{ detailCopy.title }}</h1>
            <p class="detail-subtitle">
              {{ detailCopy.companyName }} · {{ detailCopy.categoryLabel }} · {{ product.productCode || '未配置编码' }}
            </p>
            <div class="detail-meta-line">
              <span>承保公司:{{ detailCopy.companyName }}</span>
              <span>客服电话:{{ detailCopy.hotline }}</span>
              <span>状态:{{ detailCopy.saleStatusLabel }}</span>
            </div>
          </div>

          <div class="detail-text-body" v-text="detailCopy.detailText"></div>
        </template>

        <div v-else-if="!loading" class="detail-empty-wrap">
          <el-empty description="暂无产品详情" />
          <el-button type="primary" @click="goHomeProductCenter">返回产品中心</el-button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ArrowLeft } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import { buildProductDetailCopy } from '@/utils/home/product-detail.js'

const route = useRoute()
const router = useRouter()

const currentUser = ref(sessionStorage.getItem('username') || '访客')
const isAdmin = ref(sessionStorage.getItem('userType') === 'ADMIN')
const balance = ref(0)
const loading = ref(false)
const product = ref(null)

const isSuccess = (res) => res && res.data && res.data.code === '00000'

const detailCopy = computed(() => buildProductDetailCopy(product.value))

const loadBalance = async () => {
  try {
    const res = await axios.get('/api/anxinxuan/balance')
    if (isSuccess(res)) {
      balance.value = Number(res.data.data || 0)
    }
  } catch (error) {
    console.error('读取余额失败:', error)
  }
}

const loadProduct = async () => {
  const productId = Number(route.params.id)
  if (!Number.isFinite(productId) || productId <= 0) {
    product.value = null
    return
  }

  loading.value = true
  try {
    const res = await axios.get(`/api/anxinxuan/products/${productId}`)
    if (isSuccess(res)) {
      product.value = res.data.data || null
      return
    }
    product.value = null
  } catch (error) {
    console.error('读取产品详情失败:', error)
    product.value = null
  } finally {
    loading.value = false
  }
}

const goHomeProductCenter = () => {
  router.replace({ path: '/', query: { menu: 'product' } })
}

const backToProductCenter = () => {
  goHomeProductCenter()
}

const handleMenuSelect = (key) => {
  const query = key === 'home' ? {} : { menu: key }
  router.replace({ path: '/', query })
}

const handleLogout = () => {
  sessionStorage.removeItem('authToken')
  sessionStorage.removeItem('isLoggedIn')
  sessionStorage.removeItem('userId')
  sessionStorage.removeItem('userType')
  sessionStorage.removeItem('username')
  router.push('/login')
}

watch(
  () => route.params.id,
  () => {
    loadProduct()
  },
  { immediate: true }
)

onMounted(() => {
  loadBalance()
})
</script>

<style scoped>
.product-detail-route {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(230, 0, 18, 0.06), transparent 26%),
    linear-gradient(180deg, #f6f8fc 0%, #eef2f7 100%);
}

.product-detail-main {
  padding: 24px 0 36px;
}

.detail-paper {
  width: min(1120px, calc(100vw - 48px));
  margin: 0 auto;
  padding: 24px 28px 28px;
  border-radius: 24px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
}

.detail-page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.detail-back-button {
  padding-left: 0;
  font-weight: 700;
}

.detail-doc-head {
  padding: 2px 0 18px;
  border-left: 3px solid rgba(230, 0, 18, 0.2);
  padding-left: 18px;
}

.detail-kicker {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(230, 0, 18, 0.08);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.detail-title {
  margin: 14px 0 10px;
  font-size: clamp(28px, 3vw, 42px);
  line-height: 1.1;
  color: #20232a;
}

.detail-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.detail-meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin-top: 14px;
  font-size: 14px;
  color: #344054;
}

.detail-notice {
  margin: 12px 0 0;
  font-size: 14px;
  color: #111827;
  font-weight: 600;
}

.detail-text-body {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
  font-size: 16px;
  line-height: 1.9;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 0 8px;
}

@media screen and (max-width: 960px) {
  .detail-paper {
    width: min(100vw - 32px, 1120px);
    padding: 20px 20px 24px;
  }
}

@media screen and (max-width: 640px) {
  .product-detail-main {
    padding: 16px 0 24px;
  }

  .detail-paper {
    width: calc(100vw - 24px);
    padding: 16px 14px 18px;
    border-radius: 18px;
  }

  .detail-meta-line {
    gap: 6px 12px;
  }

  .detail-text-body {
    font-size: 15px;
    line-height: 1.85;
  }
}
</style>
