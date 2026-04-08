<template>
  <div class="records-page">
    <section class="records-shell surface-panel">
      <div class="records-head">
        <div class="records-head-copy">
          <span class="records-kicker">账户流水</span>
          <h2>消费明细</h2>
        </div>
        <div class="records-head-actions">
          <el-button text @click="$emit('reset-filter')">
            <el-icon><Refresh /></el-icon>
            重置筛选
          </el-button>
        </div>
      </div>

      <el-form :model="rechargeFilter" label-position="top" class="records-filter-grid recharge-filter-grid">
        <el-form-item label="账户名" class="records-field">
          <el-input v-model="rechargeFilter.username" placeholder="请输入账户名" clearable class="records-control" />
        </el-form-item>
        <el-form-item label="日期" class="records-field">
          <el-date-picker
            v-model="rechargeFilter.date"
            type="date"
            placeholder="请选择日期"
            class="records-control"
          />
        </el-form-item>
        <el-form-item label="类型" class="records-field">
          <el-select v-model="rechargeFilter.type" placeholder="请选择" clearable class="records-control">
            <el-option label="全部" value="" />
            <el-option label="充值" value="recharge" />
            <el-option label="消费" value="consume" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明" class="records-field">
          <el-input v-model="rechargeFilter.description" placeholder="请输入说明" clearable class="records-control" />
        </el-form-item>
      </el-form>

      <div class="records-filter-actions">
        <el-button type="primary" @click="$emit('query')">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>

      <el-table
        :data="rechargeList"
        class="records-table"
        style="width: 100%"
        v-loading="rechargeLoading"
        stripe
        border
        size="small"
        highlight-current-row
      >
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="scope">
            <span :class="scope.row.type === 'recharge' ? 'records-amount--positive' : 'records-amount--negative'">
              {{ scope.row.type === 'recharge' ? '+' : '-' }}¥{{ formatAmount(scope.row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.type === 'recharge' ? 'success' : 'warning'" size="small">
              {{ scope.row.type === 'recharge' ? '充值' : '消费' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="username" label="账户名" width="120" show-overflow-tooltip />
        <el-table-column prop="balance" label="余额" width="120" align="right">
          <template #default="scope">¥{{ formatAmount(scope.row.balance) }}</template>
        </el-table-column>
      </el-table>

      <div class="records-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="rechargeTotal"
          :page-size="rechargePageSize"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="$emit('size-change', $event)"
          @current-change="$emit('page-change', $event)"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { Refresh, Search } from '@element-plus/icons-vue'

defineProps({
  isAdmin: { type: Boolean, default: false },
  rechargeFilter: { type: Object, required: true },
  rechargeList: { type: Array, default: () => [] },
  rechargeLoading: { type: Boolean, default: false },
  rechargeTotal: { type: Number, default: 0 },
  rechargePageSize: { type: Number, default: 20 }
})

defineEmits(['reset-filter', 'query', 'page-change', 'size-change'])

const formatAmount = (value) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }

  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : String(value)
}
</script>
