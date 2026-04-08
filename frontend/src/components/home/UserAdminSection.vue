<template>
  <div class="records-page">
    <section class="records-shell surface-panel">
      <div class="records-head">
        <div class="records-head-copy">
          <span class="records-kicker">系统管理</span>
          <h2>用户管理</h2>
        </div>
        <div class="records-head-actions">
          <el-button text @click="$emit('reset-filter')">
            <el-icon><Refresh /></el-icon>
            重置筛选
          </el-button>
          <el-button type="primary" @click="$emit('open-user-dialog', 'create')">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
          <el-button v-if="isAdmin" type="success" @click="$emit('open-recharge-dialog')">
            <el-icon><Coin /></el-icon>
            账户充值
          </el-button>
        </div>
      </div>

      <el-form :model="userQuery" label-position="top" class="records-filter-grid user-filter-grid">
        <el-form-item label="用户名" class="records-field">
          <el-input v-model="userQuery.username" placeholder="请输入用户名" clearable class="records-control" />
        </el-form-item>
        <el-form-item label="用户类型" class="records-field">
          <el-select v-model="userQuery.userType" placeholder="请选择" clearable class="records-control">
            <el-option label="全部" value="" />
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" class="records-field">
          <el-select v-model="userQuery.status" placeholder="请选择" clearable class="records-control">
            <el-option label="全部" value="" />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="records-filter-actions">
        <el-button type="primary" @click="$emit('query')">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>

      <el-table
        :data="userList"
        class="records-table"
        style="width: 100%"
        v-loading="userLoading"
        stripe
        border
        size="small"
        highlight-current-row
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="mobile" label="手机号" width="130" />
        <el-table-column prop="userType" label="类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.userType === 'ADMIN' ? 'danger' : 'success'" size="small">
              {{ scope.row.userType === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="balance" label="余额" width="100" align="right">
          <template #default="scope">¥{{ scope.row.balance }}</template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="160" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <div class="records-row-actions">
              <el-button link type="primary" @click="$emit('open-user-dialog', 'edit', scope.row)">编辑</el-button>
              <el-button
                :type="scope.row.status === 'ACTIVE' ? 'warning' : 'success'"
                link
                @click="$emit('toggle-user-status', scope.row)"
              >
                {{ scope.row.status === 'ACTIVE' ? '禁用' : '启用' }}
              </el-button>
              <el-button
                v-if="scope.row.userType !== 'ADMIN'"
                link
                type="danger"
                @click="$emit('delete-user', scope.row.id)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="records-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="userTotal"
          :page-size="userPageSize"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="$emit('size-change', $event)"
          @current-change="$emit('page-change', $event)"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { Refresh, Plus, Search, Coin } from '@element-plus/icons-vue'

defineProps({
  isAdmin: { type: Boolean, default: false },
  userQuery: { type: Object, required: true },
  userList: { type: Array, default: () => [] },
  userLoading: { type: Boolean, default: false },
  userTotal: { type: Number, default: 0 },
  userPageSize: { type: Number, default: 20 },
  userCurrentPage: { type: Number, default: 1 }
})

defineEmits([
  'reset-filter',
  'open-user-dialog',
  'open-recharge-dialog',
  'query',
  'toggle-user-status',
  'delete-user',
  'page-change',
  'size-change'
])
</script>
