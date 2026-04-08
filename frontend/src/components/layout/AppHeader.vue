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
        <el-dropdown trigger="click" @command="handleCommand" @visible-change="handleVisibleChange">
          <button
            ref="triggerRef"
            type="button"
            class="user-info-trigger"
            :aria-label="`用户菜单，当前用户 ${currentUser}`"
            aria-haspopup="menu"
            :aria-expanded="dropdownVisible ? 'true' : 'false'"
          >
            <el-icon><User /></el-icon>
            <span class="user-name">{{ currentUser }}</span>
            <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
          </button>
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
import { nextTick, ref } from 'vue'
import {
  House, Goods, Document, Collection, Wallet, User, Coin, SwitchButton, ArrowDown
} from '@element-plus/icons-vue'

defineProps({
  activeMenu: { type: String, required: true },
  isAdmin: { type: Boolean, default: false },
  currentUser: { type: String, default: '访客' },
  balance: { type: Number, default: 0 }
})

const emit = defineEmits(['select', 'logout'])
const dropdownVisible = ref(false)
const triggerRef = ref(null)

const handleCommand = (command) => {
  if (command === 'logout') {
    emit('logout')
  }
  dropdownVisible.value = false
}

const handleVisibleChange = (visible) => {
  dropdownVisible.value = visible
  if (!visible) {
    nextTick(() => {
      triggerRef.value?.focus?.()
    })
  }
}
</script>

<style scoped>
.user-info-trigger {
  appearance: none;
  border: 1px solid var(--color-border-lighter);
  background: var(--color-bg-surface);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: background-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease;
  font: inherit;
}

.user-info-trigger:hover {
  background: var(--color-border-lighter);
  border-color: var(--color-border);
}

.user-info-trigger:focus-visible {
  outline: 3px solid rgba(230, 0, 18, 0.16);
  outline-offset: 2px;
}

.user-info-trigger .user-name {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.user-info-trigger .dropdown-arrow {
  font-size: 12px;
  color: var(--color-text-secondary);
  transition: transform 0.2s ease;
}

.user-info-trigger[aria-expanded='true'] .dropdown-arrow {
  transform: rotate(180deg);
}

@media (pointer: coarse) {
  .user-info-trigger {
    min-height: 44px;
    padding: 10px 14px;
  }
}
</style>
