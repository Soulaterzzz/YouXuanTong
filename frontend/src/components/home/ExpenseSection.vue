<template>
  <div class="records-page">
    <section class="records-shell surface-panel">
      <div class="records-head">
        <div class="records-head-copy">
          <span class="records-kicker">财务台账</span>
          <h2>{{ isAdmin ? '全部费用清单' : '费用筛选' }}</h2>
        </div>
        <div class="records-head-actions">
          <el-button text @click="$emit('reset-filter')">
            <el-icon><Refresh /></el-icon>
            重置筛选
          </el-button>
          <el-button type="success" plain @click="$emit('download-export')">
            <el-icon><Download /></el-icon>
            导出Excel
          </el-button>
        </div>
      </div>

      <el-form :model="expenseFilter" label-position="top" class="records-filter-grid expense-filter-grid">
        <el-form-item label="方案" class="records-field">
          <el-select v-model="expenseFilter.plan" placeholder="请选择" clearable class="records-control">
            <el-option label="所有方案" value="all" />
            <el-option label="国寿财意外险（1-3类）" value="guoshou-3" />
            <el-option label="平安财意外险" value="pingan-10" />
            <el-option label="少儿医疗险" value="child-med" />
            <el-option label="老年意外险" value="elder-acc" />
            <el-option label="旅游险" value="travel" />
            <el-option label="驾乘险" value="maternity" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" class="records-field">
          <el-select v-model="expenseFilter.status" placeholder="请选择" clearable class="records-control">
            <el-option label="全部" value="all" />
            <el-option label="待审核" value="PENDING_REVIEW" />
            <el-option label="审核通过" value="APPROVED" />
            <el-option label="审核驳回" value="REVIEW_REJECTED" />
            <el-option label="承保中" value="UNDERWRITING" />
            <el-option label="已生效" value="ACTIVE" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item label="序列号" class="records-field">
          <el-input v-model="expenseFilter.serial" placeholder="请输入序列号" clearable class="records-control" />
        </el-form-item>
        <el-form-item label="上传日期" class="records-field">
          <el-date-picker
            v-model="expenseFilter.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="records-control"
          />
        </el-form-item>
      </el-form>

      <div class="records-filter-actions">
        <el-button type="primary" @click="$emit('query')">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>

      <el-table
        :data="expenseList"
        class="records-table"
        style="width: 100%"
        v-loading="expenseLoading"
        stripe
        border
        size="small"
        highlight-current-row
      >
        <el-table-column prop="serial" label="序列号" width="150" show-overflow-tooltip />
        <el-table-column prop="product" label="产品名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="contact" label="联系人" width="100" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="128" />
        <el-table-column prop="status" label="状态" width="92" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" effect="light" round>
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="count" label="份数" width="68" align="center" />
        <el-table-column label="单价" width="100" align="right">
          <template #default="scope">¥{{ formatAmount(isAdmin ? scope.row.price : (scope.row.displayPrice || scope.row.price)) }}</template>
        </el-table-column>
        <el-table-column prop="total" label="金额" width="96" align="right">
          <template #default="scope">¥{{ formatAmount(scope.row.total) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="scope">
            <div class="records-row-actions">
              <el-button link type="primary" @click="$emit('view-detail', scope.row)">详情</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="records-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="expenseTotal"
          :page-size="expensePageSize"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="$emit('size-change', $event)"
          @current-change="$emit('page-change', $event)"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { Refresh, Download, Search } from '@element-plus/icons-vue'

defineProps({
  isAdmin: { type: Boolean, default: false },
  expenseFilter: { type: Object, required: true },
  expenseList: { type: Array, default: () => [] },
  expenseLoading: { type: Boolean, default: false },
  expenseTotal: { type: Number, default: 0 },
  expensePageSize: { type: Number, default: 20 }
})

defineEmits(['reset-filter', 'download-export', 'query', 'page-change', 'size-change', 'view-detail'])

const getStatusType = (status) => {
  const normalized = String(status ?? '').trim().toUpperCase()
  const typeMap = {
    DRAFT: 'info',
    待提交: 'info',
    PENDING_REVIEW: 'warning',
    待审核: 'warning',
    APPROVED: 'success',
    审核通过: 'success',
    REVIEW_REJECTED: 'danger',
    审核驳回: 'danger',
    UNDERWRITING: 'warning',
    承保中: 'warning',
    ACTIVE: 'success',
    已生效: 'success',
    EXPIRED: 'danger',
    已过期: 'danger',
    CANCELLED: 'info',
    已取消: 'info',
    COMPLETED: 'success',
    已完成: 'success',
    PENDING: 'warning',
    待处理: 'warning',
    PROCESSING: 'warning',
    处理中: 'warning',
    REJECTED: 'danger',
    已驳回: 'danger',
    INSURED: 'success'
  }
  return typeMap[normalized] || typeMap[status] || 'info'
}

const getStatusLabel = (status) => {
  const normalized = String(status ?? '').trim().toUpperCase()
  const labelMap = {
    DRAFT: '待提交',
    PENDING_REVIEW: '待审核',
    APPROVED: '审核通过',
    REVIEW_REJECTED: '审核驳回',
    UNDERWRITING: '承保中',
    ACTIVE: '已生效',
    EXPIRED: '已过期',
    CANCELLED: '已取消',
    COMPLETED: '已完成',
    PENDING: '待处理',
    PROCESSING: '处理中',
    REJECTED: '已驳回',
    INSURED: '已生效',
    待提交: '待提交',
    待审核: '待审核',
    审核通过: '审核通过',
    审核驳回: '审核驳回',
    承保中: '承保中',
    已生效: '已生效',
    已过期: '已过期',
    已取消: '已取消',
    已完成: '已完成',
    待处理: '待处理',
    处理中: '处理中',
    已驳回: '已驳回'
  }
  return labelMap[normalized] || labelMap[status] || status || '-'
}

const formatAmount = (value) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : String(value)
}
</script>
