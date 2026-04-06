<template>
  <div class="page home-page">
    <div class="welcome-card">
      <div class="welcome-icon">
        <el-icon :size="60"><CircleCheck /></el-icon>
      </div>
      <h1>欢迎使用优选通保险服务平台</h1>
      <p>为您的家庭保驾护航，提供专业、稳健、有温度的风险保障方案</p>
      <div class="quick-actions">
        <el-button type="primary" size="large" @click="$emit('goto-product')">
          <el-icon><Goods /></el-icon>
          浏览产品
        </el-button>
        <el-button type="success" size="large" @click="$emit('goto-insurance')">
          <el-icon><Document /></el-icon>
          查看保单
        </el-button>
      </div>
    </div>

    <div class="stats-cards" v-if="!isAdmin">
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon><Goods /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.totalProducts }}</span>
          <span class="stat-name">在售产品</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon><Collection /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.totalPolicies }}</span>
          <span class="stat-name">有效保单</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">¥{{ Number(stats.totalExpenses || 0).toFixed(2) }}</span>
          <span class="stat-name">累计消费</span>
        </div>
      </div>
    </div>

    <div class="stats-cards" v-else>
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon><Calendar /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.todayNewOrders }}</span>
          <span class="stat-name">今日新增订单</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.pendingOrders }}</span>
          <span class="stat-name">待处理订单</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <el-icon><Notebook /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.monthOrders }}</span>
          <span class="stat-name">本月订单数</span>
        </div>
      </div>
    </div>

    <div v-if="isAdmin" class="admin-analysis-toolbar">
      <div class="admin-analysis-filter">
        <div class="admin-analysis-filter-meta">
          <h3>经营分析</h3>
          <p>可按时间范围查看销量排行与订单总量走势</p>
        </div>
        <div class="admin-analysis-filter-actions">
          <el-button :type="adminAnalysisPeriodType === 'WEEK' ? 'primary' : 'default'" @click="$emit('apply-admin-analysis-preset', 'WEEK')">本周</el-button>
          <el-button :type="adminAnalysisPeriodType === 'MONTH' ? 'primary' : 'default'" @click="$emit('apply-admin-analysis-preset', 'MONTH')">本月</el-button>
          <el-button :type="adminAnalysisPeriodType === 'QUARTER' ? 'primary' : 'default'" @click="$emit('apply-admin-analysis-preset', 'QUARTER')">季度</el-button>
          <el-button :type="adminAnalysisPeriodType === 'YEAR' ? 'primary' : 'default'" @click="$emit('apply-admin-analysis-preset', 'YEAR')">全年</el-button>
        </div>
      </div>
    </div>

    <div v-if="isAdmin" class="admin-analysis-grid admin-analysis-stack">
      <div class="admin-analysis-card">
        <div class="admin-analysis-header">
          <div>
            <h3>产品销量排行</h3>
            <p>{{ adminAnalysisRangeText }}</p>
          </div>
          <el-tag type="success" effect="plain">TOP 10</el-tag>
        </div>
        <el-table v-if="adminSalesRanking.length" :data="adminSalesRanking" size="small" stripe>
          <el-table-column prop="rankNo" label="排名" width="70" align="center" />
          <el-table-column prop="productName" label="产品名称" min-width="220" show-overflow-tooltip />
          <el-table-column prop="orderCount" label="订单数" width="90" align="center" />
          <el-table-column prop="salesQuantity" label="销量份数" width="100" align="center" />
          <el-table-column label="销售金额" width="120" align="right">
            <template #default="scope">¥{{ Number(scope.row.salesAmount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="activePolicyCount" label="生效保单" width="100" align="center" />
        </el-table>
        <el-empty v-else description="当前时间范围暂无销量数据" :image-size="88">
          <el-button type="primary" @click="$emit('apply-admin-analysis-preset', 'MONTH')">查看本月</el-button>
        </el-empty>
      </div>

      <div class="admin-analysis-card">
        <div class="admin-analysis-header">
          <div>
            <h3>总订单量表</h3>
            <p>{{ adminOrderTrendRangeText }}</p>
          </div>
          <el-tag type="warning" effect="plain">{{ adminOrderTrendModeLabel }}</el-tag>
        </div>
        <el-table v-if="adminOrderTrend.length" :data="adminOrderTrend" size="small" stripe>
          <el-table-column prop="dateLabel" label="时间段" min-width="180" align="center" />
          <el-table-column prop="orderCount" label="订单量" min-width="120" align="center" />
        </el-table>
        <el-empty v-else description="当前时间范围暂无订单趋势数据" :image-size="88">
          <el-button type="primary" @click="$emit('apply-admin-analysis-preset', 'MONTH')">查看本月</el-button>
        </el-empty>
      </div>
    </div>

    <div class="notice-board-card">
      <div class="notice-board-header">
        <div class="notice-board-meta">
          <h3>通知公告</h3>
          <p>管理员首页和用户首页展示同一份通知列表。</p>
        </div>
        <div class="notice-board-actions">
          <el-tag type="info" effect="plain">{{ publishedNoticeTimeText }}</el-tag>
          <el-button v-if="isAdmin" type="primary" @click="$emit('open-notice-publish')">
            <el-icon><Plus /></el-icon>
            发布通知
          </el-button>
        </div>
      </div>

      <div v-if="publishedNoticeList.length" class="notice-list notice-published-list">
        <div
          v-for="(item, index) in publishedNoticeList"
          :key="item.id || index"
          class="notice-list-item notice-published-item"
          @click="$emit('open-notice-detail', item)"
        >
          <div class="notice-list-order">{{ index + 1 }}</div>
          <div class="notice-list-content">
            <h4>{{ item.title }}</h4>
            <p>{{ item.content }}</p>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无通知公告" :image-size="90" />
    </div>
  </div>
</template>

<script setup>
import { CircleCheck, Goods, Document, Collection, Coin, Calendar, Timer, Notebook, Plus } from '@element-plus/icons-vue'

defineProps({
  isAdmin: { type: Boolean, default: false },
  stats: { type: Object, required: true },
  adminAnalysisPeriodType: { type: String, default: 'MONTH' },
  adminAnalysisRangeText: { type: String, default: '' },
  adminOrderTrendRangeText: { type: String, default: '' },
  adminOrderTrendModeLabel: { type: String, default: '' },
  adminSalesRanking: { type: Array, default: () => [] },
  adminOrderTrend: { type: Array, default: () => [] },
  publishedNoticeTimeText: { type: String, default: '' },
  publishedNoticeList: { type: Array, default: () => [] }
})

defineEmits([
  'goto-product',
  'goto-insurance',
  'apply-admin-analysis-preset',
  'open-notice-publish',
  'open-notice-detail'
])
</script>
