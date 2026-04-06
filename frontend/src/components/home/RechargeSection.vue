<template>
  <div class="page">
    <div class="filter-form">
      <div class="filter-header">
        <h3>消费筛选</h3>
        <el-button text @click="$emit('reset-filter')">
          <el-icon><Refresh /></el-icon>
          重置筛选
        </el-button>
      </div>
      <el-form :inline="true" :model="rechargeFilter" class="demo-form-inline">
        <el-form-item label="日期">
          <el-date-picker v-model="rechargeFilter.date" type="date" placeholder="请选择日期" style="width: 150px;"></el-date-picker>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="rechargeFilter.type" placeholder="请选择" clearable style="width: 150px;">
            <el-option label="全部" value=""></el-option>
            <el-option label="充值" value="recharge"></el-option>
            <el-option label="消费" value="consume"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="rechargeFilter.description" placeholder="请输入说明" clearable style="width: 150px;"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="$emit('query')">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-actions" v-if="isAdmin">
      <el-button type="primary" @click="$emit('open-recharge-dialog')">
        <el-icon><Coin /></el-icon>
        账户充值
      </el-button>
    </div>

    <el-table :data="rechargeList" style="width: 100%" v-loading="rechargeLoading" stripe>
      <el-table-column prop="date" label="日期" width="120"></el-table-column>
      <el-table-column prop="amount" label="金额" width="120" align="right">
        <template #default="scope">
          <span :class="{ 'amount-positive': scope.row.type === 'recharge', 'amount-negative': scope.row.type === 'consume' }">
            {{ scope.row.type === 'recharge' ? '+' : '-' }}¥{{ scope.row.amount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.type === 'recharge' ? 'success' : 'warning'" size="small">
            {{ scope.row.type === 'recharge' ? '充值' : '消费' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="200"></el-table-column>
      <el-table-column prop="serial" label="序列号" width="180"></el-table-column>
      <el-table-column prop="balance" label="余额" width="120" align="right">
        <template #default="scope">¥{{ scope.row.balance }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="rechargeTotal"
        :page-size="10"
        @current-change="$emit('page-change', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { Refresh, Search, Coin } from '@element-plus/icons-vue'

defineProps({
  isAdmin: { type: Boolean, default: false },
  rechargeFilter: { type: Object, required: true },
  rechargeList: { type: Array, default: () => [] },
  rechargeLoading: { type: Boolean, default: false },
  rechargeTotal: { type: Number, default: 0 }
})

defineEmits(['reset-filter', 'query', 'open-recharge-dialog', 'page-change'])
</script>
