<template>
  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose"
    v-model="insuranceActivationDialogVisible"
    :title="insuranceActivationDialogTitle"
    width="960px"
    :close-on-click-modal="false"
  >
    <div class="insurance-activation-tip">
      请填写保单号、起保日期和结束日期，提交后将同步更新费用清单状态和保单起止日期。
    </div>
    <el-table :data="insuranceActivationItems" border>
      <el-table-column prop="product" label="产品名称" min-width="160" />
      <el-table-column prop="beneficiaryName" label="被保人" width="120" />
      <el-table-column label="保单号" min-width="200">
        <template #default="scope">
          <el-input v-model="scope.row.policyNo" placeholder="请输入保单号" />
        </template>
      </el-table-column>
      <el-table-column label="起保日期" width="170">
        <template #default="scope">
          <el-date-picker
            v-model="scope.row.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="起保日期"
            style="width: 100%"
          />
        </template>
      </el-table-column>
      <el-table-column label="结束日期" width="170">
        <template #default="scope">
          <el-date-picker
            v-model="scope.row.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="结束日期"
            style="width: 100%"
          />
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="insuranceActivationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="insuranceActivationSubmitting" @click="submitInsuranceActivation">
          确认生效
        </el-button>
      </span>
    </template>
  </el-dialog>

  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose"
    v-model="insuranceReviewDialogVisible"
    :title="insuranceReviewDialogTitle"
    width="520px"
    :close-on-click-modal="false"
  >
    <el-form label-width="88px">
      <el-form-item label="审核意见">
        <el-input
          v-model="insuranceReviewForm.reviewComment"
          type="textarea"
          :rows="4"
          placeholder="请输入审核意见"
        />
      </el-form-item>
      <el-form-item v-if="insuranceReviewAction === 'reject'" label="驳回原因">
        <el-input
          v-model="insuranceReviewForm.rejectReason"
          type="textarea"
          :rows="4"
          placeholder="请输入驳回原因"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="insuranceReviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="insuranceReviewSubmitting" @click="submitInsuranceReview">确认</el-button>
      </span>
    </template>
  </el-dialog>

  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose"
    v-model="expenseDetailDialogVisible"
    title="费用详情"
    width="680px"
    :close-on-click-modal="true"
    destroy-on-close
  >
    <div v-if="expenseDetailRow" class="detail-dialog-body">
      <div class="detail-dialog-hero">
        <div>
          <div class="detail-dialog-eyebrow">费用清单</div>
          <h3 class="detail-dialog-title">{{ expenseDetailRow.product || '保险产品' }}</h3>
          <p class="detail-dialog-subtitle">{{ expenseDetailRow.serial || '-' }}</p>
        </div>
        <el-tag :type="getStatusType(expenseDetailRow.status)" effect="dark">{{ expenseDetailRow.status }}</el-tag>
      </div>

      <div class="detail-summary-grid">
        <div class="detail-summary-card">
          <span class="label">联系人</span>
          <strong>{{ expenseDetailRow.contact || '-' }}</strong>
        </div>
        <div class="detail-summary-card">
          <span class="label">创建时间</span>
          <strong>{{ formatPolicyTime(expenseDetailRow.createTime) }}</strong>
        </div>
        <div class="detail-summary-card">
          <span class="label">投保份数</span>
          <strong>{{ expenseDetailRow.count || 0 }} 份</strong>
        </div>
        <div class="detail-summary-card">
          <span class="label">总金额</span>
          <strong>¥{{ expenseDetailRow.total ?? '-' }}</strong>
        </div>
      </div>

      <el-descriptions :column="2" border class="detail-meta-block">
        <el-descriptions-item label="序列号">{{ expenseDetailRow.serial || '-' }}</el-descriptions-item>
        <el-descriptions-item label="保单号">{{ expenseDetailRow.policyNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="起保日期">{{ expenseDetailRow.startDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ expenseDetailRow.endDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单价">¥{{ expenseDetailRow.price ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="小计">¥{{ expenseDetailRow.total ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </el-dialog>

  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose"
    v-model="insuranceDetailDialogVisible"
    title="保单详情"
    width="760px"
    :close-on-click-modal="true"
    destroy-on-close
  >
    <div v-if="insuranceDetailRow" class="detail-dialog-body">
      <div class="detail-dialog-hero">
        <div>
          <div class="detail-dialog-eyebrow">保单生命周期</div>
          <h3 class="detail-dialog-title">{{ insuranceDetailRow.product || '保险产品' }}</h3>
          <p class="detail-dialog-subtitle">{{ insuranceDetailRow.policyNo || '正式保单号待下发' }}</p>
        </div>
        <el-tag :type="getStatusType(insuranceDetailRow.status)" effect="dark">{{ insuranceDetailRow.status }}</el-tag>
      </div>

      <div class="detail-summary-grid">
        <div class="detail-summary-card">
          <span class="label">投保人</span>
          <strong>{{ insuranceDetailRow.insuredName || '-' }}</strong>
        </div>
        <div class="detail-summary-card">
          <span class="label">被保人</span>
          <strong>{{ insuranceDetailRow.beneficiaryName || '-' }}</strong>
        </div>
        <div class="detail-summary-card">
          <span class="label">业务员</span>
          <strong>{{ insuranceDetailRow.agent || '-' }}</strong>
        </div>
        <div class="detail-summary-card">
          <span class="label">保障期限</span>
          <strong>{{ insuranceDetailRow.startDate || '-' }} 至 {{ insuranceDetailRow.endDate || '-' }}</strong>
        </div>
      </div>

      <el-descriptions :column="2" border class="detail-meta-block">
        <el-descriptions-item label="审核人">{{ insuranceDetailRow.reviewerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ formatPolicyTime(insuranceDetailRow.reviewTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">{{ insuranceDetailRow.reviewComment || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="insuranceDetailRow.rejectReason" label="驳回原因" :span="2">{{ insuranceDetailRow.rejectReason }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section-block">
        <div class="detail-section-header">
          <h4>进度时间线</h4>
          <span>按业务节点查看当前保单处理进度</span>
        </div>
        <el-timeline class="policy-timeline">
          <el-timeline-item
            v-for="item in insuranceTimeline"
            :key="item.key"
            :timestamp="formatPolicyTime(item.time)"
            :type="item.type"
            placement="top"
          >
            <div class="policy-timeline-card">
              <div class="policy-timeline-title">{{ item.title }}</div>
              <p class="policy-timeline-content">{{ item.content }}</p>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { useHomeDialogBridge } from '@/composables/home/useHomeDialogBridge.js'

export default {
  name: 'InsuranceDialogs',
  setup() {
    const { bridge } = useHomeDialogBridge()
    return bridge
  }
}
</script>
