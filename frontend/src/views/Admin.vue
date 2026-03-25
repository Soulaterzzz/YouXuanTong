<template>
  <div class="admin-container">
    <aside class="sidebar" :class="{ 'sidebar-open': sidebarOpen }">
      <div class="sidebar-header">
        <h2>优选通</h2>
        <p>管理后台</p>
      </div>
      <nav class="sidebar-menu">
        <div class="menu-item" :class="{ active: activeMenu === 'dashboard' }" @click="handleMenuClick('dashboard')">
          <span class="menu-icon">📊</span> <span class="menu-text">数据概览</span>
        </div>
        <div class="menu-item" :class="{ active: activeMenu === 'users' }" @click="handleMenuClick('users')">
          <span class="menu-icon">👥</span> <span class="menu-text">用户管理</span>
        </div>
        <div class="menu-item" :class="{ active: activeMenu === 'products' }" @click="handleMenuClick('products')">
          <span class="menu-icon">📦</span> <span class="menu-text">产品管理</span>
        </div>
        <div class="menu-item" :class="{ active: activeMenu === 'expenses' }" @click="handleMenuClick('expenses')">
          <span class="menu-icon">📋</span> <span class="menu-text">费用清单</span>
        </div>
        <div class="menu-item" :class="{ active: activeMenu === 'insurances' }" @click="handleMenuClick('insurances')">
          <span class="menu-icon">🛡️</span> <span class="menu-text">保险清单</span>
        </div>
        <div class="menu-item" @click="logout">
          <span class="menu-icon">🚪</span> <span class="menu-text">退出登录</span>
        </div>
      </nav>
    </aside>
    <div class="sidebar-overlay" v-if="sidebarOpen" @click="sidebarOpen = false"></div>

    <main class="main-content">
      <header class="header">
        <button class="mobile-menu-btn" @click="sidebarOpen = !sidebarOpen" :class="{ active: sidebarOpen }">
          <span class="hamburger-line"></span>
          <span class="hamburger-line"></span>
          <span class="hamburger-line"></span>
        </button>
        <div class="header-left">
          <span class="header-title">{{ menuTitle }}</span>
        </div>
        <div class="header-right">
          <el-button type="primary" size="small" @click="handleMenuClick('users')" class="quick-btn">
            <span>👥</span> 用户管理
          </el-button>
          <div class="user-info">
            <span class="user-name">👤 {{ username }}</span>
            <span class="user-role">角色：管理员</span>
          </div>
          <el-button type="danger" size="small" @click="logout" class="logout-btn">退出</el-button>
        </div>
      </header>

      <div class="content">
        <!-- 数据概览 -->
        <div v-if="activeMenu === 'dashboard'">
          <h1 class="page-title">数据概览</h1>
          <div class="stats-row">
            <div class="stat-card">
              <div class="stat-icon blue">👥</div>
              <div class="stat-info">
                <h3>{{ stats.totalUsers }}</h3>
                <p>用户总数</p>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon green">📦</div>
              <div class="stat-info">
                <h3>{{ stats.totalProducts }}</h3>
                <p>产品总数</p>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon orange">📋</div>
              <div class="stat-info">
                <h3>{{ stats.totalExpenses }}</h3>
                <p>订单总数</p>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon red">🛡️</div>
              <div class="stat-info">
                <h3>{{ stats.totalInsurances }}</h3>
                <p>保单总数</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 用户管理 -->
        <div v-if="activeMenu === 'users'">
          <h1 class="page-title">用户管理</h1>
          <div class="filter-card">
            <div class="filter-form">
              <div class="filter-item">
                <label>用户名：</label>
                <el-input v-model="userQuery.username" placeholder="请输入用户名" style="width: 150px" clearable />
              </div>
              <div class="filter-item">
                <label>用户类型：</label>
                <el-select v-model="userQuery.userType" placeholder="全部" style="width: 120px" clearable>
                  <el-option label="普通用户" value="USER" />
                  <el-option label="管理员" value="ADMIN" />
                </el-select>
              </div>
              <div class="filter-item">
                <label>状态：</label>
                <el-select v-model="userQuery.status" placeholder="全部" style="width: 120px" clearable>
                  <el-option label="正常" value="ACTIVE" />
                  <el-option label="禁用" value="DISABLED" />
                </el-select>
              </div>
              <el-button type="primary" @click="loadUsers">查询</el-button>
              <el-button @click="resetUserQuery">重置</el-button>
              <el-button type="success" @click="openUserDialog('create')">新增用户</el-button>
            </div>
          </div>
          <div class="table-card">
            <el-table :data="users" style="width: 100%" v-loading="userLoading">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="username" label="用户名" width="120" />
              <el-table-column prop="realName" label="真实姓名" width="100" />
              <el-table-column prop="mobile" label="手机号" width="120" />
              <el-table-column prop="email" label="邮箱" width="180" />
              <el-table-column prop="userType" label="类型" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.userType === 'ADMIN' ? 'danger' : 'success'">
                    {{ scope.row.userType === 'ADMIN' ? '管理员' : '普通用户' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'danger'">
                    {{ scope.row.status === 'ACTIVE' ? '正常' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="balance" label="余额" width="120">
                <template #default="scope">¥{{ scope.row.balance || 0 }}</template>
              </el-table-column>
              <el-table-column prop="lastLoginTime" label="最后登录" width="160" />
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="scope">
                  <el-button size="small" type="primary" @click="openUserDialog('edit', scope.row)">编辑</el-button>
                  <el-button size="small" :type="scope.row.status === 'ACTIVE' ? 'warning' : 'success'" 
                    @click="toggleUserStatus(scope.row)">
                    {{ scope.row.status === 'ACTIVE' ? '禁用' : '启用' }}
                  </el-button>
                  <el-button size="small" type="danger" @click="deleteUser(scope.row)" v-if="scope.row.userType !== 'ADMIN'">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination">
              <el-pagination background layout="total, prev, pager, next" :total="userTotal" :page-size="10" :current-page="userCurrentPage" @current-change="handleUserPageChange" />
            </div>
          </div>
        </div>

        <!-- 产品管理 -->
        <div v-if="activeMenu === 'products'">
          <h1 class="page-title">产品管理</h1>
          <div class="filter-card">
            <div class="filter-form">
              <div class="filter-item">
                <label>产品类别：</label>
                <el-select v-model="productQuery.category" placeholder="全部" style="width: 150px" clearable>
                  <el-option label="1-3类意外" value="1-3" />
                  <el-option label="1-4类意外" value="1-4" />
                  <el-option label="1-5类意外" value="1-5" />
                  <el-option label="1-6类意外" value="1-6" />
                  <el-option label="少儿医疗" value="child" />
                  <el-option label="老年意外" value="elder" />
                </el-select>
              </div>
              <el-button type="primary" @click="loadProducts">查询</el-button>
              <el-button @click="resetProductQuery">重置</el-button>
              <el-button type="success" @click="openProductDialog('create')">新增产品</el-button>
            </div>
          </div>
          <div class="table-card">
            <el-table :data="products" style="width: 100%" v-loading="productLoading">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="name" label="产品名称" min-width="150" />
              <el-table-column prop="categoryCode" label="类别" width="100" />
              <el-table-column prop="companyName" label="承保公司" width="120" />
              <el-table-column prop="price" label="价格" width="100">
                <template #default="scope">¥{{ scope.row.price }}</template>
              </el-table-column>
              <el-table-column prop="stock" label="库存" width="80" />
              <el-table-column prop="saleStatus" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.saleStatus === 'ON_SALE' ? 'success' : 'danger'">
                    {{ scope.row.saleStatus === 'ON_SALE' ? '上架' : '下架' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280" fixed="right">
                <template #default="scope">
                  <el-button size="small" type="primary" @click="openProductDialog('edit', scope.row)">编辑</el-button>
                  <el-button size="small" :type="scope.row.saleStatus !== 'ON_SALE' ? 'success' : 'warning'" 
                    @click="toggleProductStatus(scope.row)">
                    {{ scope.row.saleStatus !== 'ON_SALE' ? '上架' : '下架' }}
                  </el-button>
                  <el-button size="small" type="danger" @click="deleteProduct(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination">
              <el-pagination background layout="total, prev, pager, next" :total="productTotal" :page-size="10" :current-page="productCurrentPage" @current-change="handleProductPageChange" />
            </div>
          </div>
        </div>

        <!-- 费用清单 -->
        <div v-if="activeMenu === 'expenses'">
          <h1 class="page-title">费用清单</h1>
          <div class="table-card">
            <el-table :data="expenses" style="width: 100%" v-loading="expenseLoading">
              <el-table-column prop="serial" label="序列号" width="180" />
              <el-table-column prop="contact" label="联系人" width="100" />
              <el-table-column prop="product" label="产品名称" min-width="150" />
              <el-table-column prop="createTime" label="创建时间" width="160" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="policyNo" label="保单号" width="150" />
              <el-table-column prop="total" label="金额" width="100">
                <template #default="scope">¥{{ scope.row.total }}</template>
              </el-table-column>
            </el-table>
            <div class="pagination">
              <el-pagination background layout="total, prev, pager, next" :total="expenseTotal" :page-size="10" :current-page="expenseCurrentPage" @current-change="handleExpensePageChange" />
            </div>
          </div>
        </div>

        <!-- 保险清单 -->
        <div v-if="activeMenu === 'insurances'">
          <h1 class="page-title">保险清单</h1>
          <div class="table-card">
            <el-table :data="insurances" style="width: 100%" v-loading="insuranceLoading">
              <el-table-column prop="policyNo" label="保单号" width="150" />
              <el-table-column prop="product" label="产品名称" min-width="150" />
              <el-table-column prop="insuredName" label="被保险人" width="100" />
              <el-table-column prop="beneficiaryName" label="受益人" width="100" />
              <el-table-column prop="createTime" label="创建时间" width="160" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="premiumAmount" label="保费" width="100">
                <template #default="scope">¥{{ scope.row.premiumAmount }}</template>
              </el-table-column>
              <el-table-column prop="startDate" label="生效日期" width="120" />
              <el-table-column prop="endDate" label="到期日期" width="120" />
            </el-table>
            <div class="pagination">
              <el-pagination background layout="total, prev, pager, next" :total="insuranceTotal" :page-size="10" :current-page="insuranceCurrentPage" @current-change="handleInsurancePageChange" />
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 用户编辑对话框 -->
    <el-dialog v-model="userDialogVisible" :title="userDialogTitle" width="500px">
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" :disabled="!!userForm.id" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="userForm.password" type="password" :placeholder="userForm.id ? '留空则不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.mobile" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="userDialogLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 产品编辑对话框 -->
    <el-dialog v-model="productDialogVisible" :title="productDialogTitle" width="600px">
      <el-form label-width="80px">
        <el-form-item label="产品名称">
          <el-input v-model="productForm.productName" />
        </el-form-item>
        <el-form-item label="产品类别">
          <el-select v-model="productForm.categoryCode" style="width: 100%">
            <el-option label="1-3类意外" value="1-3" />
            <el-option label="1-4类意外" value="1-4" />
            <el-option label="1-5类意外" value="1-5" />
            <el-option label="1-6类意外" value="1-6" />
            <el-option label="少儿医疗" value="child" />
            <el-option label="老年意外" value="elder" />
          </el-select>
        </el-form-item>
        <el-form-item label="承保公司">
          <el-input v-model="productForm.companyName" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="productForm.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="productForm.stock" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="产品描述">
          <el-input v-model="productForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="产品特点">
          <el-input v-model="productForm.features" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProduct" :loading="productDialogLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

export default {
  name: 'Admin',
  setup() {
    const router = useRouter()
    const activeMenu = ref('dashboard')
    const sidebarOpen = ref(false)
    const username = ref(sessionStorage.getItem('username') || 'admin')

    const handleMenuClick = (menu) => {
      activeMenu.value = menu
      sidebarOpen.value = false
    }

    const menuTitles = {
      dashboard: '数据概览',
      users: '用户管理',
      products: '产品管理',
      expenses: '费用清单',
      insurances: '保险清单'
    }
    const menuTitle = computed(() => menuTitles[activeMenu.value])

    const checkAuth = () => {
      const isLoggedIn = sessionStorage.getItem('isLoggedIn')
      const userType = sessionStorage.getItem('userType')
      if (!isLoggedIn || userType !== 'ADMIN') {
        router.push('/login')
      }
    }

    const stats = reactive({ totalUsers: 0, totalProducts: 0, totalExpenses: 0, totalInsurances: 0 })

    const loadStats = async () => {
      try {
        const [usersRes, productsRes, expensesRes, insurancesRes] = await Promise.all([
          axios.get('/admin/users?page=1&size=1'),
          axios.get('/admin/products?page=1&size=1'),
          axios.get('/admin/expenses?page=1&size=1'),
          axios.get('/admin/insurances?page=1&size=1')
        ])
        if (usersRes.data.code === '00000') stats.totalUsers = usersRes.data.data.total
        if (productsRes.data.code === '00000') stats.totalProducts = productsRes.data.data.total
        if (expensesRes.data.code === '00000') stats.totalExpenses = expensesRes.data.data.total
        if (insurancesRes.data.code === '00000') stats.totalInsurances = insurancesRes.data.data.total
      } catch (error) {
        console.error('Failed to load stats:', error)
      }
    }

    const users = ref([])
    const userLoading = ref(false)
    const userTotal = ref(0)
    const userCurrentPage = ref(1)
    const userQuery = reactive({ username: '', userType: '', status: '' })
    const userDialogVisible = ref(false)
    const userDialogTitle = ref('新增用户')
    const userDialogLoading = ref(false)
    const userForm = reactive({ id: null, username: '', password: '', realName: '', mobile: '', email: '' })

    const loadUsers = async () => {
      userLoading.value = true
      try {
        const res = await axios.get('/admin/users', { params: { page: userCurrentPage.value, size: 10, ...userQuery } })
        if (res.data.code === '00000') {
          users.value = res.data.data.records
          userTotal.value = res.data.data.total
        }
      } catch (error) {
        ElMessage.error('加载用户列表失败')
      } finally {
        userLoading.value = false
      }
    }

    const resetUserQuery = () => {
      userQuery.username = ''
      userQuery.userType = ''
      userQuery.status = ''
      loadUsers()
    }

    const handleUserPageChange = (page) => {
      userCurrentPage.value = page
      loadUsers()
    }

    const openUserDialog = (type, row = null) => {
      if (type === 'create') {
        userDialogTitle.value = '新增用户'
        Object.assign(userForm, { id: null, username: '', password: '', realName: '', mobile: '', email: '' })
      } else {
        userDialogTitle.value = '编辑用户'
        Object.assign(userForm, { id: row.id, username: row.username, password: '', realName: row.realName, mobile: row.mobile, email: row.email })
      }
      userDialogVisible.value = true
    }

    const saveUser = async () => {
      userDialogLoading.value = true
      try {
        if (userForm.id) {
          await axios.put('/admin/users', userForm)
          ElMessage.success('更新成功')
        } else {
          if (!userForm.password) {
            ElMessage.warning('请输入密码')
            userDialogLoading.value = false
            return
          }
          await axios.post('/admin/users', userForm)
          ElMessage.success('创建成功')
        }
        userDialogVisible.value = false
        loadUsers()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      } finally {
        userDialogLoading.value = false
      }
    }

    const toggleUserStatus = async (row) => {
      try {
        if (row.status === 'ACTIVE') {
          await axios.put(`/admin/users/${row.id}/disable`)
          ElMessage.success('禁用成功')
        } else {
          await axios.put(`/admin/users/${row.id}/enable`)
          ElMessage.success('启用成功')
        }
        loadUsers()
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }

    const deleteUser = async (row) => {
      try {
        await ElMessageBox.confirm('确定要删除该用户吗？', '提示', { type: 'warning' })
        await axios.delete(`/admin/users/${row.id}`)
        ElMessage.success('删除成功')
        loadUsers()
      } catch (error) {
        if (error !== 'cancel') ElMessage.error('删除失败')
      }
    }

    const products = ref([])
    const productLoading = ref(false)
    const productTotal = ref(0)
    const productCurrentPage = ref(1)
    const productQuery = reactive({ category: '' })
    const productDialogVisible = ref(false)
    const productDialogTitle = ref('新增产品')
    const productDialogLoading = ref(false)
    const productForm = reactive({ id: null, productName: '', categoryCode: '1-3', companyName: '', price: 0, stock: 100, description: '', features: '' })

    const loadProducts = async () => {
      productLoading.value = true
      try {
        const res = await axios.get('/admin/products', { params: { page: productCurrentPage.value, size: 10, category: productQuery.category } })
        if (res.data.code === '00000') {
          products.value = res.data.data.records
          productTotal.value = res.data.data.total
        }
      } catch (error) {
        ElMessage.error('加载产品列表失败')
      } finally {
        productLoading.value = false
      }
    }

    const resetProductQuery = () => {
      productQuery.category = ''
      loadProducts()
    }

    const handleProductPageChange = (page) => {
      productCurrentPage.value = page
      loadProducts()
    }

    const openProductDialog = (type, row = null) => {
      if (type === 'create') {
        productDialogTitle.value = '新增产品'
        Object.assign(productForm, { id: null, productName: '', categoryCode: '1-3', companyName: '', price: 0, stock: 100, description: '', features: '' })
      } else {
        productDialogTitle.value = '编辑产品'
        Object.assign(productForm, { id: row.id, productName: row.name, categoryCode: row.categoryCode, companyName: row.companyName, price: row.price, stock: row.stock, description: row.description, features: row.features })
      }
      productDialogVisible.value = true
    }

    const saveProduct = async () => {
      productDialogLoading.value = true
      try {
        if (productForm.id) {
          await axios.put('/admin/products', productForm)
          ElMessage.success('更新成功')
        } else {
          await axios.post('/admin/products', productForm)
          ElMessage.success('创建成功')
        }
        productDialogVisible.value = false
        loadProducts()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      } finally {
        productDialogLoading.value = false
      }
    }

    const toggleProductStatus = async (row) => {
      try {
        if (row.saleStatus !== 'ON_SALE') {
          await axios.put(`/admin/products/${row.id}/on-sale`)
          ElMessage.success('上架成功')
        } else {
          await axios.put(`/admin/products/${row.id}/off-sale`)
          ElMessage.success('下架成功')
        }
        loadProducts()
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }

    const deleteProduct = async (row) => {
      try {
        await ElMessageBox.confirm('确定要删除该产品吗？', '提示', { type: 'warning' })
        await axios.delete(`/admin/products/${row.id}`)
        ElMessage.success('删除成功')
        loadProducts()
      } catch (error) {
        if (error !== 'cancel') ElMessage.error('删除失败')
      }
    }

    const expenses = ref([])
    const expenseLoading = ref(false)
    const expenseTotal = ref(0)
    const expenseCurrentPage = ref(1)

    const loadExpenses = async () => {
      expenseLoading.value = true
      try {
        const res = await axios.get('/admin/expenses', { params: { page: expenseCurrentPage.value, size: 10 } })
        if (res.data.code === '00000') {
          expenses.value = res.data.data.records
          expenseTotal.value = res.data.data.total
        }
      } catch (error) {
        ElMessage.error('加载费用清单失败')
      } finally {
        expenseLoading.value = false
      }
    }

    const handleExpensePageChange = (page) => {
      expenseCurrentPage.value = page
      loadExpenses()
    }

    const insurances = ref([])
    const insuranceLoading = ref(false)
    const insuranceTotal = ref(0)
    const insuranceCurrentPage = ref(1)

    const loadInsurances = async () => {
      insuranceLoading.value = true
      try {
        const res = await axios.get('/admin/insurances', { params: { page: insuranceCurrentPage.value, size: 10 } })
        if (res.data.code === '00000') {
          insurances.value = res.data.data.records
          insuranceTotal.value = res.data.data.total
        }
      } catch (error) {
        ElMessage.error('加载保险清单失败')
      } finally {
        insuranceLoading.value = false
      }
    }

    const handleInsurancePageChange = (page) => {
      insuranceCurrentPage.value = page
      loadInsurances()
    }

    const getStatusType = (status) => {
      const map = { '已完成': 'success', '有效': 'success', '待处理': 'warning', '待生效': 'warning', '已取消': 'danger', '已过期': 'danger' }
      return map[status] || ''
    }

    const logout = () => {
      sessionStorage.clear()
      router.push('/login')
    }

    watch(activeMenu, (val) => {
      if (val === 'dashboard') loadStats()
      else if (val === 'users') loadUsers()
      else if (val === 'products') loadProducts()
      else if (val === 'expenses') loadExpenses()
      else if (val === 'insurances') loadInsurances()
    })

    onMounted(() => {
      checkAuth()
      loadStats()
    })

    return {
      activeMenu, username, menuTitle, stats,
      users, userLoading, userTotal, userCurrentPage, userQuery, userDialogVisible, userDialogTitle, userDialogLoading, userForm,
      products, productLoading, productTotal, productCurrentPage, productQuery, productDialogVisible, productDialogTitle, productDialogLoading, productForm,
      expenses, expenseLoading, expenseTotal, expenseCurrentPage,
      insurances, insuranceLoading, insuranceTotal, insuranceCurrentPage,
      loadStats, loadUsers, resetUserQuery, handleUserPageChange, openUserDialog, saveUser, toggleUserStatus, deleteUser,
      loadProducts, resetProductQuery, handleProductPageChange, openProductDialog, saveProduct, toggleProductStatus, deleteProduct,
      loadExpenses, handleExpensePageChange, loadInsurances, handleInsurancePageChange,
      getStatusType, logout
    }
  }
}
</script>

<style scoped>
.admin-container { display: flex; min-height: 100vh; }

.sidebar { width: 220px; background: #304156; color: #fff; position: fixed; height: 100vh; z-index: 200; transition: transform 0.3s ease; }

.sidebar-header { padding: 20px; text-align: center; border-bottom: 1px solid #3d4a5c; }
.sidebar-header h2 { font-size: 18px; color: #fff; margin: 0; }
.sidebar-header p { font-size: 12px; color: #8a9ab5; margin-top: 4px; }

.sidebar-menu { padding: 10px 0; }

.menu-item { padding: 14px 20px; cursor: pointer; display: flex; align-items: center; color: #bfcbd9; transition: all 0.3s; }
.menu-item:hover { background: #3d4a5c; color: #fff; }
.menu-item.active { background: #667eea; color: #fff; }
.menu-item .menu-icon { margin-right: 10px; font-size: 16px; }

.main-content { margin-left: 220px; flex: 1; transition: margin 0.3s ease; }

.header { background: #fff; padding: 0 20px; height: 60px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 1px 4px rgba(0,0,0,0.1); position: sticky; top: 0; z-index: 100; }
.header-title { font-size: 16px; font-weight: 600; color: #333; }

.mobile-menu-btn { display: none; }

.user-info { display: flex; align-items: center; gap: 15px; color: #666; font-size: 14px; }
.user-info .user-name,
.user-info .user-role { display: inline; }

.logout-btn { display: inline-flex; align-items: center; gap: 4px; }
.quick-btn { display: inline-flex; align-items: center; gap: 4px; margin-right: 8px; }

.content { padding: 20px; }
.page-title { font-size: 20px; font-weight: 600; color: #333; margin-bottom: 20px; }

.stats-row { display: flex; gap: 20px; margin-bottom: 20px; }
.stat-card { flex: 1; background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); display: flex; align-items: center; }
.stat-icon { width: 60px; height: 60px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 28px; margin-right: 15px; }
.stat-icon.blue { background: #e6f0ff; }
.stat-icon.green { background: #e6f7f1; }
.stat-icon.orange { background: #fff3e6; }
.stat-icon.red { background: #fee; }
.stat-info h3 { font-size: 24px; font-weight: 600; color: #333; margin: 0; }
.stat-info p { font-size: 14px; color: #999; margin: 4px 0 0; }

.filter-card { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); }
.filter-form { display: flex; gap: 15px; flex-wrap: wrap; align-items: center; }
.filter-item { display: flex; align-items: center; gap: 8px; }
.filter-item label { font-size: 14px; color: #666; }

.table-card { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); overflow-x: auto; }

.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }

.sidebar-overlay { display: none; }

/* Responsive Styles */
@media screen and (max-width: 1024px) {
  .mobile-menu-btn {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    width: 40px;
    height: 40px;
    padding: 8px;
    background: none;
    border: none;
    cursor: pointer;
    z-index: 300;
    gap: 5px;
    margin-right: 12px;
  }

  .mobile-menu-btn .hamburger-line {
    display: block;
    width: 22px;
    height: 2px;
    background: #333;
    transition: all 0.3s;
    border-radius: 1px;
  }

  .mobile-menu-btn.active .hamburger-line:nth-child(1) {
    transform: translateY(7px) rotate(45deg);
  }

  .mobile-menu-btn.active .hamburger-line:nth-child(2) {
    opacity: 0;
  }

  .mobile-menu-btn.active .hamburger-line:nth-child(3) {
    transform: translateY(-7px) rotate(-45deg);
  }

  .sidebar {
    transform: translateX(-100%);
  }

  .sidebar.sidebar-open {
    transform: translateX(0);
  }

  .sidebar-overlay {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 199;
  }

  .main-content {
    margin-left: 0;
  }

  .header {
    padding: 0 16px;
  }

  .header-left {
    display: flex;
    align-items: center;
  }

  .user-info {
    gap: 10px;
    font-size: 13px;
  }

  .user-info .user-role {
    display: none;
  }

  .content {
    padding: 16px;
  }

  .stats-row {
    flex-wrap: wrap;
    gap: 12px;
  }

  .stat-card {
    flex: 1 1 calc(50% - 12px);
    min-width: 160px;
    padding: 16px;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    font-size: 24px;
    margin-right: 12px;
  }

  .stat-info h3 {
    font-size: 20px;
  }

  .filter-card {
    padding: 16px;
  }

  .filter-form {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .filter-item {
    width: 100%;
  }

  .filter-item label {
    min-width: 70px;
  }

  .filter-item .el-input,
  .filter-item .el-select {
    flex: 1;
  }

  .table-card {
    padding: 12px;
  }
}

@media screen and (max-width: 768px) {
  .header {
    padding: 0 12px;
  }

  .page-title {
    font-size: 18px;
    margin-bottom: 16px;
  }

  .stat-card {
    flex: 1 1 100%;
    padding: 14px;
  }

  .stat-icon {
    width: 44px;
    height: 44px;
    font-size: 20px;
  }

  .stat-info h3 {
    font-size: 18px;
  }

  .stat-info p {
    font-size: 12px;
  }

  .filter-item label {
    min-width: 60px;
    font-size: 13px;
  }

  .table-card {
    padding: 10px;
  }

  .pagination {
    justify-content: center;
    flex-wrap: wrap;
    gap: 10px;
  }
}

@media screen and (max-width: 480px) {
  .sidebar {
    width: 260px;
  }

  .sidebar-header {
    padding: 16px;
  }

  .sidebar-header h2 {
    font-size: 16px;
  }

  .menu-item {
    padding: 12px 16px;
    font-size: 14px;
  }

  .menu-item .menu-icon {
    font-size: 14px;
    margin-right: 8px;
  }

  .header {
    height: 56px;
    padding: 0 10px;
  }

  .mobile-menu-btn {
    width: 36px;
    height: 36px;
    padding: 6px;
    margin-right: 8px;
  }

  .mobile-menu-btn .hamburger-line {
    width: 18px;
    height: 2px;
  }

  .header-title {
    font-size: 14px;
  }

  .user-info {
    gap: 8px;
    font-size: 12px;
  }

  .logout-btn {
    padding: 6px 10px;
    font-size: 12px;
  }

  .content {
    padding: 12px;
  }

  .page-title {
    font-size: 16px;
    margin-bottom: 12px;
  }

  .stat-card {
    padding: 12px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
    margin-right: 10px;
  }

  .stat-icon .el-icon {
    font-size: 18px;
  }

  .stat-info h3 {
    font-size: 18px;
  }

  .filter-card {
    padding: 12px;
  }

  .filter-item {
    flex-wrap: wrap;
  }

  .filter-item label {
    width: 100%;
    margin-bottom: 4px;
  }

  .table-card {
    padding: 8px;
  }

  .el-table {
    font-size: 12px;
  }

  .el-table .el-button {
    padding: 4px 8px;
    font-size: 11px;
  }

  .pagination {
    margin-top: 12px;
  }
}
</style>
