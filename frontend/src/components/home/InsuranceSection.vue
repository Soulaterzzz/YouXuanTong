<template>
  <div class="records-page">
    <section class="records-shell surface-panel">
      <div class="records-head">
        <div class="records-head-copy">
          <span class="records-kicker">保单台账</span>
          <h2>{{ isAdmin ? '全部保险清单' : '保险筛选' }}</h2>
        </div>
        <div class="records-head-actions">
          <el-button text @click="$emit('reset-filter')">
            <el-icon><Refresh /></el-icon>
            重置筛选
          </el-button>
          <el-button type="success" plain @click="$emit('download-export')">
            <el-icon><Download /></el-icon>
            导出PDF
          </el-button>
        </div>
      </div>

      <el-form :model="insuranceFilter" label-position="top" class="records-filter-grid insurance-filter-grid">
        <el-form-item label="方案" class="records-field">
          <el-select v-model="insuranceFilter.plan" placeholder="请选择" clearable class="records-control">
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
          <el-select v-model="insuranceFilter.status" placeholder="请选择" clearable class="records-control">
            <el-option label="全部" value="all" />
            <el-option v-if="!isAdmin" label="待提交" value="DRAFT" />
            <el-option label="待审核" value="PENDING_REVIEW" />
            <el-option label="审核通过" value="APPROVED" />
            <el-option label="审核驳回" value="REVIEW_REJECTED" />
            <el-option label="承保中" value="UNDERWRITING" />
            <el-option label="已生效" value="ACTIVE" />
            <el-option label="已过期" value="EXPIRED" />
          </el-select>
        </el-form-item>
        <el-form-item label="投保人姓名" class="records-field">
          <el-input v-model="insuranceFilter.insuredName" placeholder="请输入投保人姓名" clearable class="records-control" />
        </el-form-item>
        <el-form-item label="被保人姓名" class="records-field">
          <el-input v-model="insuranceFilter.beneficiaryName" placeholder="请输入被保人姓名" clearable class="records-control" />
        </el-form-item>
        <el-form-item label="上传日期" class="records-field">
          <el-date-picker
            v-model="insuranceFilter.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="records-control"
          />
        </el-form-item>
        <el-form-item label="投保人证件号" class="records-field">
          <el-input v-model="insuranceFilter.insuredId" placeholder="请输入投保人证件号" clearable class="records-control" />
        </el-form-item>
        <el-form-item label="被保人证件号" class="records-field">
          <el-input v-model="insuranceFilter.beneficiaryId" placeholder="请输入被保人证件号" clearable class="records-control" />
        </el-form-item>
        <el-form-item label="业务员" class="records-field">
          <el-input v-model="insuranceFilter.agent" placeholder="请输入业务员" clearable class="records-control" />
        </el-form-item>
      </el-form>

      <div class="records-filter-actions">
        <el-button type="primary" @click="$emit('query')">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button
          v-if="isAdmin"
          type="warning"
          :disabled="!selectedInsurances.some(item => item.statusCode === 'UNDERWRITING')"
          @click="$emit('batch-activate', selectedInsurances.filter(item => item.statusCode === 'UNDERWRITING'))"
        >
          <el-icon><Lightning /></el-icon>
          批量生效
        </el-button>
      </div>

      <el-table
        :data="insuranceList"
        class="records-table insurance-records-table"
        style="width: 100%"
        v-loading="insuranceLoading"
        stripe
        border
        size="small"
        highlight-current-row
        @selection-change="$emit('selection-change', $event)"
      >
        <el-table-column v-if="isAdmin" type="selection" width="40" />
        <el-table-column prop="product" label="产品名称" min-width="110" show-overflow-tooltip />
        <el-table-column prop="insuredName" label="投保人" width="68" show-overflow-tooltip />
        <el-table-column prop="insuredId" label="投保人证件号" min-width="110" show-overflow-tooltip />
        <el-table-column prop="beneficiaryName" label="被保人" width="68" show-overflow-tooltip />
        <el-table-column prop="beneficiaryId" label="被保人证件号" min-width="110" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="92" show-overflow-tooltip />
        <el-table-column prop="exportTime" label="导出时间" width="92" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="76" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.statusCode || scope.row.status)" effect="light" round>
              {{ getStatusLabel(scope.row.statusCode || scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="agent" label="业务员" width="68" show-overflow-tooltip />
        <el-table-column prop="policyNo" label="电子保单号" min-width="110" show-overflow-tooltip />
        <el-table-column prop="startDate" label="起保日期" width="72" align="center" show-overflow-tooltip />
        <el-table-column prop="endDate" label="结束日期" width="72" align="center" show-overflow-tooltip />
        <el-table-column prop="count" label="份数" width="44" align="center" />
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="scope">
            <div class="records-row-actions">
              <el-button
                v-if="canDownloadPolicyPdf(scope.row.statusCode || scope.row.status)"
                link
                type="success"
                @click="$emit('download-policy', scope.row)"
              >
                下载PDF
              </el-button>
              <el-button
                v-if="!isAdmin && scope.row.statusCode === 'DRAFT'"
                link
                type="primary"
                @click="$emit('submit-draft', scope.row)"
              >
                提交审核
              </el-button>
              <el-button
                v-if="isAdmin && scope.row.statusCode === 'PENDING_REVIEW'"
                link
                type="success"
                @click="$emit('approve', scope.row)"
              >
                通过
              </el-button>
              <el-button
                v-if="isAdmin && scope.row.statusCode === 'PENDING_REVIEW'"
                link
                type="danger"
                @click="$emit('reject', scope.row)"
              >
                驳回
              </el-button>
              <el-button
                v-if="isAdmin && scope.row.statusCode === 'APPROVED'"
                link
                type="warning"
                @click="$emit('underwriting', scope.row)"
              >
                承保
              </el-button>
              <el-button
                v-if="isAdmin && scope.row.statusCode === 'UNDERWRITING'"
                link
                type="warning"
                @click="$emit('activate', scope.row)"
              >
                生效
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="records-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="insuranceTotal"
          :page-size="insurancePageSize"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="$emit('size-change', $event)"
          @current-change="$emit('page-change', $event)"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { Refresh, Download, Search, Lightning } from '@element-plus/icons-vue'

defineProps({
  isAdmin: { type: Boolean, default: false },
  insuranceFilter: { type: Object, required: true },
  insuranceList: { type: Array, default: () => [] },
  insuranceLoading: { type: Boolean, default: false },
  insuranceTotal: { type: Number, default: 0 },
  insurancePageSize: { type: Number, default: 20 },
  selectedInsurances: { type: Array, default: () => [] }
})

defineEmits([
  'reset-filter',
  'download-export',
  'query',
  'selection-change',
  'batch-activate',
  'download-policy',
  'submit-draft',
  'approve',
  'reject',
  'underwriting',
  'activate',
  'page-change',
  'size-change'
])

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

const canDownloadPolicyPdf = (status) => {
  const normalized = String(status ?? '').trim().toUpperCase()
  return normalized === 'UNDERWRITING' || normalized === 'ACTIVE' || normalized === 'EXPIRED' || normalized === 'INSURED' || status === '承保中' || status === '已生效' || status === '已过期'
}
</script>
