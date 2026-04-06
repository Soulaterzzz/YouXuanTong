<template>
  <header class="header">
    <div class="logo">
      <span class="logo-text">优选通</span>
      <span class="logo-sub">保险服务平台</span>
    </div>
    <nav class="nav">
      <el-menu
        :default-active="activeMenu"
        class="el-menu-demo top-nav-menu"
        mode="horizontal"
        :ellipsis="false"
        @select="$emit('select', $event)"
      >
        <el-menu-item index="home">
          <el-icon><House /></el-icon>
          <span class="nav-text">首页</span>
        </el-menu-item>
        <el-menu-item index="product">
          <el-icon><Goods /></el-icon>
          <span class="nav-text">产品中心</span>
        </el-menu-item>
        <el-menu-item index="expense">
          <el-icon><Document /></el-icon>
          <span class="nav-text">费用清单</span>
        </el-menu-item>
        <el-menu-item index="insurance">
          <el-icon><Collection /></el-icon>
          <span class="nav-text">保险清单</span>
        </el-menu-item>
        <el-menu-item index="recharge">
          <el-icon><Wallet /></el-icon>
          <span class="nav-text">消费明细</span>
        </el-menu-item>
        <el-menu-item index="userAdmin" v-if="isAdmin">
          <el-icon><User /></el-icon>
          <span class="nav-text">用户管理</span>
        </el-menu-item>
      </el-menu>
    </nav>
    <div class="header-right">
      <div class="user-info-dropdown">
        <el-dropdown trigger="click" @command="$emit('logout')">
          <div class="user-info-trigger">
            <el-icon><User /></el-icon>
            <span class="user-name">{{ currentUser }}</span>
            <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <div class="dropdown-user-info">
                <div class="dropdown-balance">
                  <el-icon><Coin /></el-icon>
                  <span>余额：¥{{ balance.toFixed(2) }}</span>
                </div>
              </div>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup>
import {
  House, Goods, Document, Collection, Wallet, User, Coin, SwitchButton, ArrowDown
} from '@element-plus/icons-vue'

defineProps({
  activeMenu: { type: String, required: true },
  isAdmin: { type: Boolean, default: false },
  currentUser: { type: String, default: '访客' },
  balance: { type: Number, default: 0 }
})

defineEmits(['select', 'logout'])
</script>
