<template>
  <div class="page">
    <div class="filter-form">
      <div class="filter-header">
        <h3>用户管理</h3>
        <div class="filter-header-actions">
          <el-button text @click="$emit('reset-filter')">
            <el-icon><Refresh /></el-icon>
            重置筛选
          </el-button>
          <el-button type="primary" @click="$emit('open-user-dialog', 'create')">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </div>
      <el-form :inline="true" :model="userQuery" class="demo-form-inline">
        <el-form-item label="用户名">
          <el-input v-model="userQuery.username" placeholder="请输入用户名" clearable style="width: 150px;"></el-input>
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="userQuery.userType" placeholder="请选择" clearable style="width: 120px;">
            <el-option label="全部" value=""></el-option>
            <el-option label="普通用户" value="USER"></el-option>
            <el-option label="管理员" value="ADMIN"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="userQuery.status" placeholder="请选择" clearable style="width: 100px;">
            <el-option label="全部" value=""></el-option>
            <el-option label="启用" value="ACTIVE"></el-option>
            <el-option label="禁用" value="DISABLED"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="$emit('query')">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="userList" v-loading="userLoading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="mobile" label="手机号" width="130" />
        <el-table-column prop="userType" label="类型" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.userType === 'ADMIN' ? 'danger' : 'success'" size="small">
              {{ scope.row.userType === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button type="primary" size="small" text @click="$emit('open-user-dialog', 'edit', scope.row)">
                编辑
              </el-button>
              <el-button
                :type="scope.row.status === 'ACTIVE' ? 'warning' : 'success'"
                size="small"
                text
                @click="$emit('toggle-user-status', scope.row)"
              >
                {{ scope.row.status === 'ACTIVE' ? '禁用' : '启用' }}
              </el-button>
              <el-button
                v-if="scope.row.userType !== 'ADMIN'"
                type="danger"
                size="small"
                text
                @click="$emit('delete-user', scope.row.id)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="userTotal"
          :page-size="userPageSize"
          :current-page="userCurrentPage"
          @current-change="$emit('page-change', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { Refresh, Plus, Search } from '@element-plus/icons-vue'

defineProps({
  userQuery: { type: Object, required: true },
  userList: { type: Array, default: () => [] },
  userLoading: { type: Boolean, default: false },
  userTotal: { type: Number, default: 0 },
  userPageSize: { type: Number, default: 10 },
  userCurrentPage: { type: Number, default: 1 }
})

defineEmits([
  'reset-filter',
  'open-user-dialog',
  'query',
  'toggle-user-status',
  'delete-user',
  'page-change'
])
</script>
