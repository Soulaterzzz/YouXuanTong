<template>
  <div class="page">
    <div class="filter-form">
      <div class="filter-header">
        <h3>{{ isAdmin ? '全部费用清单' : '费用筛选' }}</h3>
        <div class="filter-header-actions">
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
      <el-form :inline="true" :model="expenseFilter" class="demo-form-inline">
        <el-form-item label="方案">
          <el-select v-model="expenseFilter.plan" placeholder="请选择" clearable style="width: 150px;">
            <el-option label="所有方案" value="all"></el-option>
            <el-option label="国寿财意外险（1-3类）" value="guoshou-3"></el-option>
            <el-option label="平安财意外险" value="pingan-10"></el-option>
            <el-option label="少儿医疗险" value="child-med"></el-option>
            <el-option label="老年意外险" value="elder-acc"></el-option>
            <el-option label="旅游险" value="travel"></el-option>
            <el-option label="驾乘险" value="maternity"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="expenseFilter.status" placeholder="请选择" clearable style="width: 150px;">
            <el-option label="全部" value="all"></el-option>
            <el-option label="待审核" value="PENDING_REVIEW"></el-option>
            <el-option label="审核通过" value="APPROVED"></el-option>
            <el-option label="审核驳回" value="REVIEW_REJECTED"></el-option>
            <el-option label="承保中" value="UNDERWRITING"></el-option>
            <el-option label="已生效" value="ACTIVE"></el-option>
            <el-option label="已取消" value="cancelled"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="序列号">
          <el-input v-model="expenseFilter.serial" placeholder="请输入序列号" clearable style="width: 150px;"></el-input>
        </el-form-item>
        <el-form-item label="上传日期">
          <el-date-picker
            v-model="expenseFilter.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="$emit('query')">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table
      :data="expenseList"
      class="expense-table"
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
      <el-table-column prop="total" label="金额" width="96" align="right">
        <template #default="scope">¥{{ formatAmount(scope.row.total) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="72" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" @click="$emit('view-detail', scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="expenseTotal"
        :page-size="expensePageSize"
        @current-change="$emit('page-change', $event)"
      />
    </div>
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

defineEmits(['reset-filter', 'download-export', 'query', 'page-change', 'view-detail'])

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

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-form {
  padding: 22px 22px 18px;
  border-radius: 20px;
  border: 1px solid #e8edf4;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfd 100%);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.filter-header {
  gap: 12px;
  margin-bottom: 18px;
}

.filter-header h3 {
  margin: 0;
  font-size: 18px;
  line-height: 1.2;
  font-weight: 700;
  color: #172033;
}

.filter-header-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
  margin-right: 16px;
}

.filter-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #556072;
}

.filter-form :deep(.el-input__wrapper),
.filter-form :deep(.el-select__wrapper),
.filter-form :deep(.el-date-editor) {
  border-radius: 12px;
}

.expense-table {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #ebeff5;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.expense-table :deep(.el-table__cell) {
  padding-top: 8px;
  padding-bottom: 8px;
}

.expense-table :deep(.el-table__header-wrapper th) {
  background: #f7f9fc;
  color: #1f2937;
  font-weight: 700;
}

.expense-table :deep(.el-table__cell) {
  border-bottom-color: #eef2f7;
}

.expense-table :deep(.el-table__row:hover > td) {
  background: #f8fbff !important;
}

.expense-table :deep(.el-tag) {
  border-radius: 999px;
  padding: 0 8px;
  font-weight: 700;
  font-size: 12px;
  line-height: 22px;
}

.expense-table :deep(.el-button.is-link) {
  padding-left: 0;
  padding-right: 0;
  min-height: auto;
}

.pagination {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

@media screen and (max-width: 768px) {
  .filter-form {
    padding: 18px 16px 14px;
  }

  .filter-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .filter-header-actions {
    justify-content: flex-start;
  }

  .expense-table :deep(.el-table__cell) {
    padding-top: 12px;
    padding-bottom: 12px;
  }

  .pagination {
    justify-content: center;
  }
}
</style>
