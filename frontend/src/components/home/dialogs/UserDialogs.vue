<template>
  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose" v-model="userDialogVisible" :title="userDialogTitle" width="450px" :close-on-click-modal="false">
    <el-form :model="userForm" :rules="userFormRules" ref="userFormRef" label-width="80px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="!!userForm.id" />
      </el-form-item>
      <el-form-item label="手机号" prop="mobile">
        <el-input v-model="userForm.mobile" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="密码" :prop="userForm.id ? '' : 'password'">
        <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        <span style="color: #999; font-size: 12px;" v-if="userForm.id">留空则不修改密码</span>
      </el-form-item>
      <el-form-item v-if="!userForm.id || userForm.password" label="确认密码">
        <el-input v-model="userForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUserForm">确认</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import { useHomeDialogBridge } from '@/composables/home/useHomeDialogBridge.js'

export default {
  name: 'UserDialogs',
  setup() {
    const { bridge } = useHomeDialogBridge(['userFormRef'])
    return bridge
  }
}
</script>
