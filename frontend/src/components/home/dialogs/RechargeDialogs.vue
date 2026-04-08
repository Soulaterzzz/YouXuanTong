<template>
  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose" v-model="rechargeDialogVisible" title="账户充值" width="400px">
    <el-form :model="rechargeForm" :rules="rechargeFormRules" ref="rechargeFormRef" label-width="80px">
      <el-form-item label="用户" prop="userId">
        <el-select v-model="rechargeForm.userId" placeholder="请选择用户" filterable style="width: 100%;">
          <el-option v-for="user in allUsers" :key="user.id" :label="user.username" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="充值金额" prop="amount">
        <el-input-number v-model="rechargeForm.amount" :min="1" :max="100000" :step="100" style="width: 100%;"></el-input-number>
      </el-form-item>
      <el-form-item label="充值方式">
        <el-select v-model="rechargeForm.method" placeholder="请选择充值方式" style="width: 100%;">
          <el-option label="支付宝" value="alipay"></el-option>
          <el-option label="微信支付" value="wechat"></el-option>
          <el-option label="银行转账" value="bank"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="rechargeForm.remark" type="textarea" placeholder="请输入备注" :rows="3"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rechargeSubmitting" @click="submitRechargeForm">确认充值</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import { useHomeDialogBridge } from '@/composables/home/useHomeDialogBridge.js'

export default {
  name: 'RechargeDialogs',
  setup() {
    const { bridge } = useHomeDialogBridge(['rechargeFormRef'])
    return bridge
  }
}
</script>
