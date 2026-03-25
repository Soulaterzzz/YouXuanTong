<template>
  <div class="app-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="logo">
        <span class="logo-text">优通选</span>
        <span class="logo-sub">保险服务平台</span>
      </div>
      <nav class="nav">
        <el-menu :default-active="activeMenu" class="el-menu-demo" mode="horizontal" @select="handleMenuSelect">
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
          <el-dropdown trigger="click" @command="handleUserCommand">
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

    <!-- 主体内容 -->
    <main class="main-content">
      <!-- 左侧产品类别菜单 -->
      <aside class="sidebar" v-if="activeMenu === 'product'">
        <div class="sidebar-section">
          <h3 class="sidebar-title">
            <el-icon><Grid /></el-icon>
            产品类别
          </h3>
          <el-menu :default-active="activeCategory" class="el-menu-vertical-demo" @select="handleCategorySelect">
            <el-menu-item index="all">
              <el-icon><Menu /></el-icon>
              全部产品
            </el-menu-item>
            <el-menu-item index="1-3">
              <el-icon><Sunny /></el-icon>
              1-3类意外
            </el-menu-item>
            <el-menu-item index="1-4">
              <el-icon><Sunny /></el-icon>
              1-4类意外
            </el-menu-item>
            <el-menu-item index="1-5">
              <el-icon><Sunny /></el-icon>
              1-5类意外
            </el-menu-item>
            <el-menu-item index="1-6">
              <el-icon><Sunny /></el-icon>
              1-6类意外
            </el-menu-item>
            <el-menu-item index="child">
              <el-icon><Star /></el-icon>
              少儿医疗
            </el-menu-item>
            <el-menu-item index="elder">
              <el-icon><Present /></el-icon>
              老年意外
            </el-menu-item>
            <el-menu-item index="travel">
              <el-icon><Present /></el-icon>
              旅游险
            </el-menu-item>
            <el-menu-item index="maternity">
              <el-icon><Present /></el-icon>
              驾乘险
            </el-menu-item>
          </el-menu>
        </div>
      </aside>

      <!-- 右侧主内容区域 -->
      <section class="content">
        <!-- 首页 -->
        <div v-if="activeMenu === 'home'" class="page home-page">
          <div class="welcome-card">
            <div class="welcome-icon">
              <el-icon :size="60"><CircleCheck /></el-icon>
            </div>
            <h1>欢迎使用优通选保险服务平台</h1>
            <p>为您的家庭保驾护航，提供专业、稳健、有温度的风险保障方案</p>
            <div class="quick-actions">
              <el-button type="primary" size="large" @click="activeMenu = 'product'">
                <el-icon><Goods /></el-icon>
                浏览产品
              </el-button>
              <el-button type="success" size="large" @click="activeMenu = 'insurance'">
                <el-icon><Document /></el-icon>
                查看保单
              </el-button>
            </div>
          </div>
          <div class="stats-cards" v-if="!isAdmin">
            <div class="stat-card">
              <div class="stat-icon blue">
                <el-icon><Goods /></el-icon>
              </div>
              <div class="stat-info">
                <span class="stat-number">{{ stats.totalProducts }}</span>
                <span class="stat-name">在售产品</span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon green">
                <el-icon><Collection /></el-icon>
              </div>
              <div class="stat-info">
                <span class="stat-number">{{ stats.totalPolicies }}</span>
                <span class="stat-name">有效保单</span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon orange">
                <el-icon><Coin /></el-icon>
              </div>
              <div class="stat-info">
                <span class="stat-number">¥{{ Number(stats.totalExpenses || 0).toFixed(2) }}</span>
                <span class="stat-name">累计消费</span>
              </div>
            </div>
          </div>
          
          <div class="stats-cards" v-else>
            <div class="stat-card">
              <div class="stat-icon blue">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="stat-info">
                <span class="stat-number">{{ stats.todayNewOrders }}</span>
                <span class="stat-name">今日新增订单</span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon green">
                <el-icon><Timer /></el-icon>
              </div>
              <div class="stat-info">
                <span class="stat-number">{{ stats.pendingOrders }}</span>
                <span class="stat-name">待处理订单</span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon orange">
                <el-icon><Notebook /></el-icon>
              </div>
              <div class="stat-info">
                <span class="stat-number">{{ stats.monthOrders }}</span>
                <span class="stat-name">本月订单数</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 产品中心 -->
        <div v-else-if="activeMenu === 'product'" class="page">
          <!-- 产品分类筛选 -->
          <div class="product-filter">
            <div class="filter-header">
              <h3><el-icon><Filter /></el-icon> 产品筛选</h3>
              <el-button text @click="resetProductFilter" v-if="companyFilter !== 'all'">
                <el-icon><Refresh /></el-icon>
                重置筛选
              </el-button>
            </div>
            <div class="filter-row">
              <span class="filter-label">承保公司：</span>
              <el-radio-group v-model="companyFilter" @change="handleCompanyChange" size="default">
                <el-radio-button label="all">全部</el-radio-button>
                <el-radio-button label="guoshou">国寿财险</el-radio-button>
                <el-radio-button label="pingan">平安财险</el-radio-button>
                <el-radio-button label="zhonghua">中华联合</el-radio-button>
                <el-radio-button label="taiping">太平财险</el-radio-button>
                <el-radio-button label="renbao">人保财险</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <!-- 产品统计 -->
          <div class="product-stats">
            <span class="stats-text">共 <strong>{{ productTotal }}</strong> 个产品</span>
            <span class="stats-divider" v-if="companyFilter !== 'all'">|</span>
            <span class="filter-status" v-if="companyFilter !== 'all'">
              当前筛选：{{ getFilterText() }}
            </span>
          </div>

          <!-- 产品列表 -->
          <div class="product-list" v-loading="productLoading">
            <div class="product-item" v-for="product in products" :key="product.id">
              <div class="product-card">
                <div class="product-image-wrapper" @click="previewImage(product)">
                  <img :src="getProductImage(product)" :alt="product.name" class="product-img">
                  <div class="image-overlay">
                    <el-icon><ZoomIn /></el-icon>
                    <span>点击放大</span>
                  </div>
                  <div class="product-badges">
                    <span class="badge new" v-if="product.isNew">新品</span>
                    <span class="badge hot" v-if="product.isHot">热门</span>
                  </div>
                </div>
                <div class="product-content">
                  <div class="product-header">
                    <h3 class="product-name">{{ product.name }}</h3>
                    <span
                      v-if="isAdmin"
                      class="status-badge"
                      :class="product.saleStatus === 'ON_SALE' ? 'on-sale' : 'off-sale'"
                    >
                      {{ product.saleStatus === 'ON_SALE' ? '已上架' : '已下架' }}
                    </span>
                  </div>
                  <div class="product-desc">
                    <p class="desc-text">{{ product.description }}</p>
                    <p class="features-text" v-if="product.features">
                      <el-icon><Star /></el-icon>
                      {{ product.features }}
                    </p>
                  </div>
                  <div class="product-footer">
                    <div class="price-info">
                      <span class="price-label">价格：</span>
                      <span class="price-value">¥{{ product.price }}</span>
                    </div>
                    <div class="stock-info">
                      <span class="stock-label">资源值：</span>
                      <span class="stock-value" :class="{ 'low-stock': product.stock < 10 }">
                        {{ product.stock > 0 ? product.stock : '充足' }}
                      </span>
                    </div>
                    <div class="product-actions">
                      <el-button v-if="isAdmin" type="primary" size="small" @click="openProductDialog(product)">
                        <el-icon><Edit /></el-icon>
                        编辑
                      </el-button>
                      <el-button
                        v-if="isAdmin"
                        :type="product.saleStatus === 'ON_SALE' ? 'danger' : 'success'"
                        size="small"
                        @click="toggleProductStatus(product)"
                      >
                        <el-icon><Switch /></el-icon>
                        {{ product.saleStatus === 'ON_SALE' ? '下架' : '上架' }}
                      </el-button>
                      <el-button type="primary" plain size="small" @click="previewImage(product)">
                        <el-icon><Picture /></el-icon>
                        图片
                      </el-button>
                      <el-button
                        type="success"
                        plain
                        size="small"
                        v-if="product.templateFileName"
                        @click="downloadProductTemplate(product)"
                      >
                        <el-icon><Download /></el-icon>
                        模板
                      </el-button>
                      <el-button
                        type="warning"
                        size="small"
                        :disabled="product.saleStatus && product.saleStatus !== 'ON_SALE'"
                        @click="openActivateDialog(product)"
                      >
                        <el-icon><Lightning /></el-icon>
                        激活
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <!-- 空状态 -->
            <el-empty v-if="products.length === 0 && !productLoading" description="暂无产品数据" />
          </div>

          <!-- 分页 -->
          <div class="pagination" v-if="products.length > 0">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="productTotal"
              :page-size="productPageSize"
              :current-page="productCurrentPage"
              :page-sizes="[5, 10, 20, 50]"
              @size-change="handleProductSizeChange"
              @current-change="handleProductPageChange"
            />
          </div>
        </div>

        <!-- 费用清单 -->
        <div v-else-if="activeMenu === 'expense'" class="page">
          <!-- 筛选条件 -->
          <div class="filter-form">
            <div class="filter-header">
              <h3>{{ isAdmin ? '全部费用清单' : '费用筛选' }}</h3>
              <el-button text @click="resetExpenseFilter">
                <el-icon><Refresh /></el-icon>
                重置筛选
              </el-button>
            </div>
            <el-form :inline="true" :model="expenseFilter" class="demo-form-inline">
              <el-form-item label="方案">
                <el-select v-model="expenseFilter.plan" placeholder="请选择" clearable style="width: 150px;">
                  <el-option label="所有方案" value="all"></el-option>
                  <el-option label="国寿财1-3类" value="guoshou-3"></el-option>
                  <el-option label="平安财意外" value="pingan"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="expenseFilter.status" placeholder="请选择" clearable style="width: 150px;">
                  <el-option label="全部" value="all"></el-option>
                  <el-option label="待处理" value="pending"></el-option>
                  <el-option label="已完成" value="completed"></el-option>
                  <el-option label="已取消" value="cancelled"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="序列号">
                <el-input v-model="expenseFilter.serial" placeholder="请输入序列号" clearable style="width: 150px;"></el-input>
              </el-form-item>
              <el-form-item label="上传日期">
                <el-date-picker v-model="expenseFilter.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 240px;"></el-date-picker>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="queryExpenses">
                  <el-icon><Search /></el-icon>
                  查询
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 操作按钮 -->
          <div class="table-actions">
            <el-button type="success" plain>
              <el-icon><Download /></el-icon>
              {{ isAdmin ? '导出全部费用清单' : '导出费用清单' }}
            </el-button>
          </div>

          <!-- 费用清单表格 -->
          <el-table :data="expenseList" style="width: 100%" v-loading="expenseLoading" stripe>
            <el-table-column prop="serial" label="序列号" width="150"></el-table-column>
            <el-table-column prop="contact" label="联系人" width="100"></el-table-column>
            <el-table-column prop="product" label="产品名称" min-width="150"></el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="160"></el-table-column>
            <el-table-column prop="exportTime" label="导出时间" width="160"></el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="policyNo" label="电子保单号" width="180"></el-table-column>
            <el-table-column prop="startDate" label="起保日期" width="120"></el-table-column>
            <el-table-column prop="endDate" label="结束日期" width="120"></el-table-column>
            <el-table-column prop="count" label="份数" width="80" align="center"></el-table-column>
            <el-table-column prop="price" label="价格" width="100" align="right">
              <template #default="scope">¥{{ scope.row.price }}</template>
            </el-table-column>
            <el-table-column prop="total" label="小计" width="100" align="right">
              <template #default="scope">¥{{ scope.row.total }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="viewExpenseDetail(scope.row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :total="expenseTotal"
              :page-size="10"
              @current-change="handleExpensePageChange"
            />
          </div>
        </div>

        <!-- 保险清单 -->
        <div v-else-if="activeMenu === 'insurance'" class="page">
          <!-- 筛选条件 -->
          <div class="filter-form">
            <div class="filter-header">
              <h3>{{ isAdmin ? '全部保险清单' : '保险筛选' }}</h3>
              <el-button text @click="resetInsuranceFilter">
                <el-icon><Refresh /></el-icon>
                重置筛选
              </el-button>
            </div>
            <el-form :inline="true" :model="insuranceFilter" class="demo-form-inline">
              <el-form-item label="方案">
                <el-select v-model="insuranceFilter.plan" placeholder="请选择" clearable style="width: 150px;">
                  <el-option label="所有方案" value="all"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="insuranceFilter.status" placeholder="请选择" clearable style="width: 150px;">
                  <el-option label="全部" value="all"></el-option>
                  <el-option label="有效" value="active"></el-option>
                  <el-option label="待生效" value="pending"></el-option>
                  <el-option label="已过期" value="expired"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="序列号">
                <el-input v-model="insuranceFilter.serial" placeholder="请输入序列号" clearable style="width: 150px;"></el-input>
              </el-form-item>
              <el-form-item label="上传日期">
                <el-date-picker v-model="insuranceFilter.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 240px;"></el-date-picker>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="queryInsurances">
                  <el-icon><Search /></el-icon>
                  查询
                </el-button>
              </el-form-item>
            </el-form>
            <div class="filter-form-row">
              <el-input v-model="insuranceFilter.insuredName" placeholder="投保人姓名" clearable style="width: 130px;"></el-input>
              <el-input v-model="insuranceFilter.insuredId" placeholder="投保人证件号" clearable style="width: 130px;"></el-input>
              <el-input v-model="insuranceFilter.beneficiaryName" placeholder="被保人姓名" clearable style="width: 130px;"></el-input>
              <el-input v-model="insuranceFilter.beneficiaryId" placeholder="被保人证件号" clearable style="width: 130px;"></el-input>
              <el-input v-model="insuranceFilter.agent" placeholder="业务员" clearable style="width: 130px;"></el-input>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="table-actions">
            <el-button type="primary" @click="queryInsurances">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button type="success">
              <el-icon><Download /></el-icon>
              数据导出(最多5000条)
            </el-button>
            <el-button type="info">
              <el-icon><Clock /></el-icon>
              到期查询(1个月)
            </el-button>
            <el-button type="warning">
              <el-icon><Document /></el-icon>
              批量导出凭证(前20条)
            </el-button>
          </div>

          <!-- 保险清单表格 -->
          <el-table :data="insuranceList" style="width: 100%" v-loading="insuranceLoading" stripe @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55"></el-table-column>
            <el-table-column prop="product" label="产品名称" min-width="150"></el-table-column>
            <el-table-column prop="insuredName" label="投保人" width="100"></el-table-column>
            <el-table-column prop="insuredId" label="投保人证件号" width="150"></el-table-column>
            <el-table-column prop="beneficiaryName" label="被保人" width="100"></el-table-column>
            <el-table-column prop="beneficiaryId" label="被保人证件号" width="150"></el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="160"></el-table-column>
            <el-table-column prop="exportTime" label="导出时间" width="160"></el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="agent" label="业务员" width="100"></el-table-column>
            <el-table-column prop="policyNo" label="电子保单号" width="180"></el-table-column>
            <el-table-column prop="startDate" label="起保日期" width="120"></el-table-column>
            <el-table-column prop="endDate" label="结束日期" width="120"></el-table-column>
            <el-table-column prop="count" label="份数" width="80" align="center"></el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="viewInsuranceDetail(scope.row)">详情</el-button>
                <el-button link type="success" @click="downloadPolicy(scope.row)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :total="insuranceTotal"
              :page-size="10"
              :page-sizes="[10, 20, 50, 100]"
              @size-change="handleInsuranceSizeChange"
              @current-change="handleInsurancePageChange"
            />
          </div>
        </div>

        <!-- 充值消费明细 -->
        <div v-else-if="activeMenu === 'recharge'" class="page">
          <!-- 筛选条件 -->
          <div class="filter-form">
            <div class="filter-header">
              <h3>消费筛选</h3>
              <el-button text @click="resetRechargeFilter">
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
                <el-button type="primary" @click="queryRecharges">
                  <el-icon><Search /></el-icon>
                  查询
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 操作按钮 -->
          <div class="table-actions" v-if="isAdmin">
            <el-select v-model="rechargeTargetUser" placeholder="选择用户" clearable style="width: 150px;">
              <el-option v-for="user in allUsers" :key="user.id" :label="user.username" :value="user.id" />
            </el-select>
            <el-button type="primary" @click="openRechargeDialog" :disabled="!rechargeTargetUser">
              <el-icon><Coin /></el-icon>
              账户充值
            </el-button>
          </div>

          <!-- 充值消费明细表格 -->
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

          <!-- 分页 -->
          <div class="pagination">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :total="rechargeTotal"
              :page-size="10"
              @current-change="handleRechargePageChange"
            />
          </div>
        </div>

        <!-- 用户管理 (仅管理员可见) -->
        <div v-else-if="activeMenu === 'userAdmin'" class="page">
          <!-- 筛选条件 -->
          <div class="filter-form">
            <div class="filter-header">
              <h3>用户管理</h3>
              <el-button text @click="resetUserQuery">
                <el-icon><Refresh /></el-icon>
                重置筛选
              </el-button>
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
                <el-button type="primary" @click="loadUsers">
                  <el-icon><Search /></el-icon>
                  查询
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 用户表格 -->
          <div class="table-card">
            <el-table :data="userList" v-loading="userLoading" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="username" label="用户名" width="120" />
              <el-table-column prop="phone" label="手机号" width="130" />
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
                    <el-button type="primary" size="small" text @click="openUserDialog('edit', scope.row)">
                      编辑
                    </el-button>
                    <el-button
                      :type="scope.row.status === 'ACTIVE' ? 'warning' : 'success'"
                      size="small"
                      text
                      @click="toggleUserStatus(scope.row)"
                    >
                      {{ scope.row.status === 'ACTIVE' ? '禁用' : '启用' }}
                    </el-button>
                    <el-button
                      v-if="scope.row.userType !== 'ADMIN'"
                      type="danger"
                      size="small"
                      text
                      @click="deleteUser(scope.row.id)"
                    >
                      删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="pagination">
              <el-pagination
                background
                layout="total, prev, pager, next"
                :total="userTotal"
                :page-size="userPageSize"
                :current-page="userCurrentPage"
                @current-change="handleUserPageChange"
              />
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- 激活弹窗 -->
    <el-dialog v-model="productDialogVisible" title="编辑产品" width="620px" :close-on-click-modal="false">
      <el-form :model="productForm" label-width="90px">
        <el-form-item label="产品编码">
          <el-input v-model="productForm.productCode" placeholder="请输入产品编码" />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="productForm.productName" placeholder="请输入产品名称" />
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
          <el-input v-model="productForm.companyName" placeholder="请输入承保公司" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="productForm.price" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="productForm.stock" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="productForm.sortNo" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="产品描述">
          <el-input v-model="productForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="产品特点">
          <el-input v-model="productForm.features" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="产品图片">
          <el-upload
            class="product-image-uploader"
            :action="`/api/images/product/${productForm.id}/upload`"
            :show-file-list="false"
            :on-success="handleImageUploadSuccess"
            :on-error="handleImageUploadError"
            :before-upload="beforeImageUpload"
            accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
            :with-credentials="true"
          >
            <img v-if="productForm.imageUrl" :src="productForm.imageUrl" class="product-image-preview" />
            <el-icon v-else class="product-image-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="image-upload-tip">
            <el-button size="small" type="danger" @click="deleteProductImage" v-if="productForm.imageUrl">
              删除图片
            </el-button>
            <span class="tip-text">支持 jpg、png、gif、webp 格式，大小不超过 10MB</span>
          </div>
        </el-form-item>
        <el-form-item label="模板文件">
          <div class="template-upload-container">
            <el-upload
              class="template-uploader"
              :action="`/api/images/product/${productForm.id}/template`"
              :show-file-list="false"
              :on-success="handleTemplateUploadSuccess"
              :on-error="handleTemplateUploadError"
              :before-upload="beforeTemplateUpload"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.txt"
              :with-credentials="true"
              method="post"
            >
              <el-button type="primary" size="small">
                <el-icon><Upload /></el-icon>
                上传模板
              </el-button>
            </el-upload>
            <div v-if="productForm.templateFileName" class="template-file-info">
              <el-icon><Document /></el-icon>
              <span class="template-file-name">{{ productForm.templateFileName }}</span>
              <el-button size="small" type="primary" link @click="downloadTemplate">下载</el-button>
              <el-button size="small" type="danger" link @click="deleteTemplate">删除</el-button>
            </div>
            <div class="template-upload-tip">
              支持 PDF、Word、Excel、TXT 格式，大小不超过 10MB
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="productDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="productSubmitting" @click="saveProduct">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 激活弹窗 -->
    <el-dialog v-model="activeDialogVisible" title="产品激活" width="550px" :close-on-click-modal="false">
      <div v-if="selectedProduct" class="activate-product-info">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="info-item">
              <span class="label">产品名称：</span>
              <span class="value">{{ selectedProduct.name }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <span class="label">单价：</span>
              <span class="value price">¥{{ selectedProduct.price }}</span>
            </div>
          </el-col>
        </el-row>
      </div>
      <el-form :model="activeForm" :rules="activeFormRules" ref="activeFormRef" label-width="100px">
        <el-form-item label="方案名称" prop="planName">
          <el-select v-model="activeForm.planName" placeholder="请选择方案名称" style="width: 100%;">
            <el-option v-if="selectedProduct" :label="selectedProduct.name" :value="String(selectedProduct.id)"></el-option>
            <el-option label="国寿财1-3类10+5+5+50（青柑）" value="guoshou-3"></el-option>
            <el-option label="平安财意外10+1+50+10（含意保）" value="pingan-10"></el-option>
            <el-option label="少儿医疗险" value="child-med"></el-option>
            <el-option label="老年意外险" value="elder-acc"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="被保人姓名" prop="beneficiaryName">
          <el-input v-model="activeForm.beneficiaryName" placeholder="请输入被保人姓名"></el-input>
        </el-form-item>
        <el-form-item label="被保人证件号" prop="beneficiaryId">
          <el-input v-model="activeForm.beneficiaryId" placeholder="请输入被保人证件号"></el-input>
        </el-form-item>
        <el-form-item label="被保人职业" prop="beneficiaryJob">
          <el-select v-model="activeForm.beneficiaryJob" placeholder="请选择被保人职业" style="width: 100%;">
            <el-option label="1类（低风险）" value="1"></el-option>
            <el-option label="2类" value="2"></el-option>
            <el-option label="3类" value="3"></el-option>
            <el-option label="4类" value="4"></el-option>
            <el-option label="5类" value="5"></el-option>
            <el-option label="6类（高风险）" value="6"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="份数" prop="count">
          <el-select v-model="activeForm.count" placeholder="请选择份数" style="width: 100%;">
            <el-option label="1份" :value="1"></el-option>
            <el-option label="2份" :value="2"></el-option>
            <el-option label="3份" :value="3"></el-option>
            <el-option label="5份" :value="5"></el-option>
            <el-option label="10份" :value="10"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="activeForm.address" placeholder="请输入地址"></el-input>
        </el-form-item>
        <el-form-item label="业务员">
          <el-input v-model="activeForm.agent" placeholder="请输入业务员"></el-input>
        </el-form-item>
        <el-divider />
        <div class="activate-summary">
          <div class="summary-row">
            <span>当前余额：</span>
            <span class="balance">¥{{ balance }}</span>
          </div>
          <div class="summary-row" v-if="selectedProduct">
            <span>应付金额：</span>
            <span class="amount">¥{{ (selectedProduct.price * activeForm.count).toFixed(2) }}</span>
          </div>
          <div class="summary-row" v-if="selectedProduct">
            <span>余额不足：</span>
            <span :class="['remain', balance >= selectedProduct.price * activeForm.count ? 'enough' : 'not-enough']">
              {{ balance >= selectedProduct.price * activeForm.count ? '否' : '是，请先充值' }}
            </span>
          </div>
        </div>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="activeDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="activeSubmitting" :disabled="selectedProduct && balance < selectedProduct.price * activeForm.count" @click="submitActiveForm">提交数据</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 充值弹窗 -->
    <el-dialog v-model="rechargeDialogVisible" title="账户充值" width="400px">
      <el-form :model="rechargeForm" :rules="rechargeFormRules" ref="rechargeFormRef" label-width="80px">
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

    <!-- 用户管理弹窗 -->
    <el-dialog v-model="userDialogVisible" :title="userDialogTitle" width="450px" :close-on-click-modal="false">
      <el-form :model="userForm" :rules="userFormRules" ref="userFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="!!userForm.id" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" :prop="userForm.id ? '' : 'password'">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
          <span style="color: #999; font-size: 12px;" v-if="userForm.id">留空则不修改密码</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="userDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitUserForm">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 图片预览弹窗 -->
    <el-dialog v-model="previewImageDialogVisible" :title="previewImageTitle" width="800px">
      <div class="image-preview-container">
        <img :src="previewImageUrl" :alt="previewImageTitle" class="preview-image">
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  House, Goods, Document, Collection, Wallet, User, Coin, SwitchButton,
  Grid, Menu, Sunny, Star, Present, OfficeBuilding,
  CircleCheck, Refresh, Search, Picture, Upload, Lightning, View, Download,
  Clock, Link, Edit, Switch, ZoomIn, Calendar, Timer, Notebook, Plus, Filter,
  Delete, ArrowDown
} from '@element-plus/icons-vue'

export default {
  name: 'Home',
  components: {
    House, Goods, Document, Collection, Wallet, User, Coin, SwitchButton,
    Grid, Menu, Sunny, Star, Present, OfficeBuilding,
    CircleCheck, Refresh, Search, Picture, Upload, Lightning, View, Download,
    Clock, Link, Edit, Switch, ZoomIn, Calendar, Timer, Notebook, Plus, Filter,
    ArrowDown
  },
  data() {
    return {
      isAdmin: sessionStorage.getItem('userType') === 'ADMIN',
      currentUser: sessionStorage.getItem('username') || '访客',
      balance: 0,
      activeMenu: 'home',
      activeCategory: 'all',
      stats: {
        totalProducts: 0,
        totalPolicies: 0,
        totalExpenses: 0,
        totalInsurances: 0,
        todayNewOrders: 0,
        pendingOrders: 0,
        monthOrders: 0
      },
      productLoading: false,
      products: [],
      productTotal: 0,
      productPageSize: 10,
      productCurrentPage: 1,
      companyFilter: 'all',
      categoryFilter: 'all',
      selectedCompanies: [],
      productDialogVisible: false,
      productSubmitting: false,
      productForm: {
        id: null,
        productCode: '',
        productName: '',
        categoryCode: '1-3',
        companyName: '',
        description: '',
        features: '',
        price: 0.01,
        stock: 0,
        isNew: 0,
        isHot: 0,
        saleStatus: 'ON_SALE',
        sortNo: 0,
        imageUrl: '',
        templateFileName: ''
      },
      expenseLoading: false,
      expenseList: [],
      expenseTotal: 0,
      expenseCurrentPage: 1,
      expenseFilter: {
        plan: 'all',
        status: 'all',
        serial: '',
        dateRange: null
      },
      insuranceLoading: false,
      insuranceList: [],
      insuranceTotal: 0,
      insuranceCurrentPage: 1,
      insuranceFilter: {
        plan: 'all',
        status: 'all',
        serial: '',
        dateRange: null,
        insuredName: '',
        insuredId: '',
        beneficiaryName: '',
        beneficiaryId: '',
        agent: ''
      },
      selectedInsurances: [],
      rechargeLoading: false,
      rechargeList: [],
      rechargeTotal: 0,
      rechargeCurrentPage: 1,
      rechargeFilter: {
        date: '',
        type: '',
        description: ''
      },
      rechargeTargetUser: null,
      allUsers: [],
      userLoading: false,
      userList: [],
      userTotal: 0,
      userCurrentPage: 1,
      userPageSize: 10,
      userQuery: {
        username: '',
        userType: '',
        status: ''
      },
      userDialogVisible: false,
      userDialogTitle: '',
      userForm: {
        id: null,
        username: '',
        phone: '',
        password: ''
      },
      userFormRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
      },
      selectedProduct: null,
      activeDialogVisible: false,
      activeSubmitting: false,
      activeForm: {
        planName: '',
        beneficiaryName: '',
        beneficiaryId: '',
        beneficiaryJob: '',
        count: 1,
        address: '',
        agent: ''
      },
      activeFormRules: {
        planName: [{ required: true, message: '请选择方案名称', trigger: 'change' }],
        beneficiaryName: [{ required: true, message: '请输入被保人姓名', trigger: 'blur' }],
        beneficiaryId: [{ required: true, message: '请输入被保人证件号', trigger: 'blur' }],
        beneficiaryJob: [{ required: true, message: '请选择被保人职业', trigger: 'change' }]
      },
      rechargeDialogVisible: false,
      rechargeSubmitting: false,
      rechargeForm: {
        amount: 1000,
        method: 'alipay',
        remark: ''
      },
      rechargeFormRules: {
        amount: [{ required: true, message: '请输入充值金额', trigger: 'blur' }]
      },
      // 图片预览相关
      previewImageDialogVisible: false,
      previewImageUrl: '',
      previewImageTitle: ''
    }
  },
  mounted() {
    if (!sessionStorage.getItem('isLoggedIn')) {
      this.$router.push('/login')
      return
    }
    this.loadStats()
    this.loadProducts()
    this.loadBalance()
  },
  methods: {
    isSuccess(res) {
      return res && res.data && res.data.code === '00000'
    },

    async loadBalance() {
      try {
        const res = await this.$axios.get('/api/anxinxuan/balance')
        if (this.isSuccess(res)) {
          this.balance = Number(res.data.data || 0)
        }
      } catch (error) {
        console.error('加载余额失败:', error)
      }
    },

    async loadStats() {
      try {
        if (this.isAdmin) {
          const [productsRes, expensesRes, insurancesRes, statsRes] = await Promise.all([
            this.$axios.get('/api/admin/products', { params: { page: 1, size: 1 } }),
            this.$axios.get('/api/admin/expenses', { params: { page: 1, size: 1 } }),
            this.$axios.get('/api/admin/insurances', { params: { page: 1, size: 1 } }),
            this.$axios.get('/api/admin/stats')
          ])
          
          const adminStats = this.isSuccess(statsRes) ? statsRes.data.data || {} : {}
          
          this.stats = {
            totalProducts: this.isSuccess(productsRes) ? productsRes.data.data.total || 0 : 0,
            totalPolicies: 0,
            totalExpenses: this.isSuccess(expensesRes) ? expensesRes.data.data.total || 0 : 0,
            totalInsurances: this.isSuccess(insurancesRes) ? insurancesRes.data.data.total || 0 : 0,
            todayNewOrders: adminStats.todayNewOrders || 0,
            pendingOrders: adminStats.pendingOrders || 0,
            monthOrders: adminStats.monthOrders || 0
          }
          return
        }

        const res = await this.$axios.get('/api/anxinxuan/stats')
        if (this.isSuccess(res)) {
          const data = res.data.data || {}
          this.stats = {
            totalProducts: data.totalProducts || 0,
            totalPolicies: data.totalPolicies || 0,
            totalExpenses: Number(data.totalExpenses || 0),
            totalInsurances: 0
          }
          this.balance = Number(data.balance || 0)
        }
      } catch (error) {
        console.error('加载统计数据失败:', error)
        this.stats = {
          totalProducts: 0,
          totalPolicies: 0,
          totalExpenses: 0,
          totalInsurances: 0,
          todayNewOrders: 0,
          pendingOrders: 0,
          monthOrders: 0
        }
      }
    },

    handleMenuSelect(key) {
      this.activeMenu = key
      switch (key) {
        case 'home':
          this.loadStats()
          break
        case 'product':
          this.loadProducts()
          break
        case 'expense':
          this.loadExpenses()
          break
        case 'insurance':
          this.loadInsurances()
          break
        case 'recharge':
          this.loadRecharges()
          if (this.isAdmin) {
            this.loadAllUsers()
          }
          break
        case 'userAdmin':
          this.loadUsers()
          break
        default:
          break
      }
    },

    handleCategorySelect(key) {
      this.activeCategory = key
      this.productCurrentPage = 1
      this.loadProducts()
    },

    handleCompanyChange(value) {
      this.productCurrentPage = 1
      this.loadProducts()
    },

    resetProductFilter() {
      this.companyFilter = 'all'
      this.selectedCompanies = []
      this.activeCategory = 'all'
      this.productCurrentPage = 1
      this.loadProducts()
    },

    getFilterText() {
      const companyMap = {
        'guoshou': '国寿财险',
        'pingan': '平安财险',
        'zhonghua': '中华联合',
        'taiping': '太平财险',
        'renbao': '人保财险'
      }
      if (this.companyFilter !== 'all') {
        return companyMap[this.companyFilter] || this.companyFilter
      }
      return ''
    },

    async loadProducts() {
      this.productLoading = true
      try {
        const params = {
          page: this.productCurrentPage,
          size: this.productPageSize,
          company: this.companyFilter === 'all' ? '' : this.companyFilter
        }
        const endpoint = this.isAdmin ? '/api/admin/products' : '/api/anxinxuan/products'
        const res = await this.$axios.get(endpoint, { params })
        if (this.isSuccess(res)) {
          const data = res.data.data || {}
          this.products = data.records || []
          this.productTotal = data.total || 0
        } else {
          this.products = []
          this.productTotal = 0
        }
      } catch (error) {
        console.error('加载产品失败:', error)
        this.products = []
        this.productTotal = 0
      } finally {
        this.productLoading = false
      }
    },

    handleProductPageChange(page) {
      this.productCurrentPage = page
      this.loadProducts()
    },

    handleProductSizeChange(size) {
      this.productPageSize = size
      this.productCurrentPage = 1
      this.loadProducts()
    },

    openProductDialog(product) {
      this.productForm = {
        id: product.id,
        productCode: product.productCode || '',
        productName: product.name || '',
        categoryCode: product.categoryCode || '1-3',
        companyName: product.companyName || '',
        description: product.description || '',
        features: product.features || '',
        price: product.price || 0.01,
        stock: product.stock || 0,
        isNew: product.isNew ? 1 : 0,
        isHot: product.isHot ? 1 : 0,
        saleStatus: product.saleStatus || 'ON_SALE',
        sortNo: product.sortNo || 0,
        imageUrl: product.imageUrl || '',
        templateFileName: product.templateFileName || ''
      }
      this.productDialogVisible = true
    },

    async saveProduct() {
      if (!this.productForm.productCode || !this.productForm.productName) {
        this.$message.warning('请填写产品编码和产品名称')
        return
      }
      this.productSubmitting = true
      try {
        await this.$axios.put('/api/admin/products', this.productForm)
        this.$message.success('产品更新成功')
        this.productDialogVisible = false
        this.loadProducts()
        this.loadStats()
      } catch (error) {
        console.error('更新产品失败:', error)
        this.$message.error(error.response?.data?.message || '更新产品失败')
      } finally {
        this.productSubmitting = false
      }
    },

    async toggleProductStatus(product) {
      try {
        if (product.saleStatus === 'ON_SALE') {
          await this.$axios.put(`/api/admin/products/${product.id}/off-sale`)
          this.$message.success('产品已下架')
        } else {
          await this.$axios.put(`/api/admin/products/${product.id}/on-sale`)
          this.$message.success('产品已上架')
        }
        this.loadProducts()
        this.loadStats()
      } catch (error) {
        console.error('切换产品状态失败:', error)
        this.$message.error(error.response?.data?.message || '切换产品状态失败')
      }
    },

    openActivateDialog(product) {
      if (product.saleStatus && product.saleStatus !== 'ON_SALE') {
        this.$message.warning('下架产品不可激活')
        return
      }
      this.selectedProduct = product
      this.activeForm = {
        productId: product.id,
        planName: '',
        beneficiaryName: '',
        beneficiaryId: '',
        beneficiaryJob: '',
        count: 1,
        address: '',
        agent: ''
      }
      this.activeDialogVisible = true
    },

    async submitActiveForm() {
      this.$refs.activeFormRef.validate(async (valid) => {
        if (!valid) {
          return
        }
        this.activeSubmitting = true
        try {
          const res = await this.$axios.post('/api/anxinxuan/products/activate', this.activeForm)
          if (this.isSuccess(res)) {
            this.$message.success('激活成功')
            this.activeDialogVisible = false
            this.loadProducts()
            this.loadStats()
            this.loadBalance()
          } else {
            this.$message.error(res.data.message || '激活失败')
          }
        } catch (error) {
          console.error('激活失败:', error)
          this.$message.error(error.response?.data?.message || '激活失败，请稍后重试')
        } finally {
          this.activeSubmitting = false
        }
      })
    },

    resetExpenseFilter() {
      this.expenseFilter = {
        plan: 'all',
        status: 'all',
        serial: '',
        dateRange: null
      }
      this.expenseCurrentPage = 1
      this.loadExpenses()
    },

    async loadExpenses() {
      this.expenseLoading = true
      try {
        const params = {
          page: this.expenseCurrentPage,
          size: 20,
          status: this.expenseFilter.status === 'all' ? '' : this.expenseFilter.status,
          serialNo: this.expenseFilter.serial
        }
        const endpoint = this.isAdmin ? '/api/admin/expenses' : '/api/anxinxuan/expenses'
        const res = await this.$axios.get(endpoint, { params })
        if (this.isSuccess(res)) {
          const data = res.data.data || {}
          this.expenseList = data.records || []
          this.expenseTotal = data.total || 0
        } else {
          this.expenseList = []
          this.expenseTotal = 0
        }
      } catch (error) {
        console.error('加载费用清单失败:', error)
        this.expenseList = []
        this.expenseTotal = 0
      } finally {
        this.expenseLoading = false
      }
    },

    queryExpenses() {
      this.expenseCurrentPage = 1
      this.loadExpenses()
    },

    handleExpensePageChange(page) {
      this.expenseCurrentPage = page
      this.loadExpenses()
    },

    viewExpenseDetail(row) {
      this.$message.info(`查看费用详情：${row.serial}`)
    },

    resetInsuranceFilter() {
      this.insuranceFilter = {
        plan: 'all',
        status: 'all',
        serial: '',
        dateRange: null,
        insuredName: '',
        insuredId: '',
        beneficiaryName: '',
        beneficiaryId: '',
        agent: ''
      }
      this.insuranceCurrentPage = 1
      this.loadInsurances()
    },

    async loadInsurances() {
      this.insuranceLoading = true
      try {
        const params = {
          page: this.insuranceCurrentPage,
          size: 20,
          status: this.insuranceFilter.status === 'all' ? '' : this.insuranceFilter.status,
          serialNo: this.insuranceFilter.serial,
          insuredName: this.insuranceFilter.insuredName,
          beneficiaryName: this.insuranceFilter.beneficiaryName
        }
        const endpoint = this.isAdmin ? '/api/admin/insurances' : '/api/anxinxuan/insurances'
        const res = await this.$axios.get(endpoint, { params })
        if (this.isSuccess(res)) {
          const data = res.data.data || {}
          this.insuranceList = data.records || []
          this.insuranceTotal = data.total || 0
        } else {
          this.insuranceList = []
          this.insuranceTotal = 0
        }
      } catch (error) {
        console.error('加载保险清单失败:', error)
        this.insuranceList = []
        this.insuranceTotal = 0
      } finally {
        this.insuranceLoading = false
      }
    },

    queryInsurances() {
      this.insuranceCurrentPage = 1
      this.loadInsurances()
    },

    handleInsurancePageChange(page) {
      this.insuranceCurrentPage = page
      this.loadInsurances()
    },

    handleInsuranceSizeChange() {
      this.insuranceCurrentPage = 1
      this.loadInsurances()
    },

    handleSelectionChange(selection) {
      this.selectedInsurances = selection
    },

    viewInsuranceDetail(row) {
      this.$message.info(`查看保险详情：${row.policyNo}`)
    },

    downloadPolicy(row) {
      this.$message.success(`开始下载保单：${row.policyNo}`)
    },

    resetRechargeFilter() {
      this.rechargeFilter = {
        date: '',
        type: '',
        description: ''
      }
      this.rechargeCurrentPage = 1
      this.loadRecharges()
    },

    async loadRecharges() {
      this.rechargeLoading = true
      try {
        const params = {
          page: this.rechargeCurrentPage,
          size: 20,
          type: this.rechargeFilter.type,
          description: this.rechargeFilter.description
        }
        const endpoint = this.isAdmin ? '/api/admin/recharges' : '/api/anxinxuan/recharges'
        const res = await this.$axios.get(endpoint, { params })
        if (this.isSuccess(res)) {
          const data = res.data.data || {}
          this.rechargeList = data.records || []
          this.rechargeTotal = data.total || 0
          if (this.rechargeList.length > 0) {
            this.balance = Number(this.rechargeList[0].balance || 0)
          }
        } else {
          this.rechargeList = []
          this.rechargeTotal = 0
        }
      } catch (error) {
        console.error('加载充值消费明细失败:', error)
        this.rechargeList = []
        this.rechargeTotal = 0
      } finally {
        this.rechargeLoading = false
      }
    },

    queryRecharges() {
      this.rechargeCurrentPage = 1
      this.loadRecharges()
    },

    handleRechargePageChange(page) {
      this.rechargeCurrentPage = page
      this.loadRecharges()
    },

    async loadUsers() {
      this.userLoading = true
      try {
        const params = {
          page: this.userCurrentPage,
          size: this.userPageSize,
          username: this.userQuery.username || undefined,
          userType: this.userQuery.userType || undefined,
          status: this.userQuery.status || undefined
        }
        const res = await this.$axios.get('/api/admin/users', { params })
        if (this.isSuccess(res)) {
          this.userList = res.data.data.records || res.data.data || []
          this.userTotal = res.data.data.total || 0
        }
      } catch (error) {
        console.error('加载用户列表失败:', error)
      } finally {
        this.userLoading = false
      }
    },

    resetUserQuery() {
      this.userQuery = { username: '', userType: '', status: '' }
      this.userCurrentPage = 1
      this.loadUsers()
    },

    async loadAllUsers() {
      try {
        const res = await this.$axios.get('/api/admin/users', { params: { page: 1, size: 1000 } })
        if (this.isSuccess(res)) {
          this.allUsers = res.data.data.records || res.data.data || []
        }
      } catch (error) {
        console.error('加载所有用户失败:', error)
      }
    },

    handleUserPageChange(page) {
      this.userCurrentPage = page
      this.loadUsers()
    },

    openUserDialog(type, user = null) {
      this.userDialogTitle = type === 'create' ? '新增用户' : '编辑用户'
      if (type === 'edit' && user) {
        this.userForm = {
          id: user.id,
          username: user.username,
          phone: user.phone,
          password: ''
        }
      } else {
        this.userForm = {
          id: null,
          username: '',
          phone: '',
          password: ''
        }
      }
      this.userDialogVisible = true
    },

    async submitUserForm() {
      this.$refs.userFormRef.validate(async (valid) => {
        if (!valid) return
        try {
          const isEdit = !!this.userForm.id
          const submitData = { ...this.userForm }
          if (submitData.password === '') {
            delete submitData.password
          }
          let res
          if (isEdit) {
            res = await this.$axios.put(`/api/admin/users/${submitData.id}`, submitData)
          } else {
            res = await this.$axios.post('/api/admin/users', submitData)
          }
          if (this.isSuccess(res)) {
            this.$message.success(isEdit ? '更新成功' : '创建成功')
            this.userDialogVisible = false
            this.loadUsers()
          }
        } catch (error) {
          console.error('保存用户失败:', error)
          this.$message.error('保存失败')
        }
      })
    },

    async toggleUserStatus(user) {
      const action = user.status === 1 ? '禁用' : '启用'
      try {
        const res = await this.$axios.put(`/api/admin/users/${user.id}/${action === '启用' ? 'enable' : 'disable'}`)
        if (this.isSuccess(res)) {
          this.$message.success(`${action}成功`)
          this.loadUsers()
        }
      } catch (error) {
        console.error(`${action}用户失败:`, error)
        this.$message.error(`${action}失败`)
      }
    },

    async deleteUser(userId) {
      try {
        await this.$confirm('确定要删除该用户吗？此操作不可恢复。', '删除确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const res = await this.$axios.delete(`/api/admin/users/${userId}`)
        if (this.isSuccess(res)) {
          this.$message.success('删除成功')
          this.loadUsers()
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除用户失败:', error)
          this.$message.error('删除失败')
        }
      }
    },

    openRechargeDialog() {
      this.rechargeForm = {
        userId: this.rechargeTargetUser,
        amount: 1000,
        method: 'alipay',
        remark: ''
      }
      this.rechargeDialogVisible = true
    },

    async submitRechargeForm() {
      this.$refs.rechargeFormRef.validate(async (valid) => {
        if (!valid) {
          return
        }
        this.rechargeSubmitting = true
        try {
          const res = await this.$axios.post('/api/anxinxuan/recharges', this.rechargeForm)
          if (this.isSuccess(res)) {
            this.$message.success('充值成功')
            this.rechargeDialogVisible = false
            this.loadRecharges()
            this.loadStats()
            this.loadBalance()
          } else {
            this.$message.error(res.data.message || '充值失败')
          }
        } catch (error) {
          console.error('充值失败:', error)
          this.$message.error(error.response?.data?.message || '充值失败，请稍后重试')
        } finally {
          this.rechargeSubmitting = false
        }
      })
    },

    getStatusType(status) {
      const typeMap = {
        已完成: 'success',
        有效: 'success',
        待处理: 'warning',
        待生效: 'warning',
        处理中: 'warning',
        已取消: 'info',
        已过期: 'danger'
      }
      return typeMap[status] || 'info'
    },

    handleUserCommand(command) {
      if (command === 'logout') {
        this.logout()
      }
    },

    logout() {
      this.$confirm('确定要退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await this.$axios.post('/api/auth/logout')
        } catch (error) {
          console.error('退出登录失败:', error)
        } finally {
          sessionStorage.clear()
          this.$message.success('已退出系统')
          this.$router.push('/login')
        }
      }).catch(() => {})
    },

    // 获取产品图片
    getProductImage(product) {
      return `/api/images/product/${product.id}`
    },

    // 预览图片
    previewImage(product) {
      this.previewImageUrl = this.getProductImage(product)
      this.previewImageTitle = product.name
      this.previewImageDialogVisible = true
    },

    // 下载产品模板
    downloadProductTemplate(product) {
      const link = document.createElement('a')
      link.href = `/api/images/product/${product.id}/template`
      link.download = product.templateFileName || 'template'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    },

    // 图片上传前验证
    beforeImageUpload(file) {
      const isImage = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
      const isLt10M = file.size / 1024 / 1024 < 10

      if (!isImage) {
        this.$message.error('只能上传 JPG/PNG/GIF/WEBP 格式的图片!')
        return false
      }
      if (!isLt10M) {
        this.$message.error('图片大小不能超过 10MB!')
        return false
      }
      return true
    },

    // 图片上传成功
    handleImageUploadSuccess(response, file) {
      if (response.code === '00000') {
        this.productForm.imageUrl = response.data.url
        this.$message.success('图片上传成功')
        this.loadProducts()
      } else {
        this.$message.error(response.message || '图片上传失败')
      }
    },

    // 图片上传失败
    handleImageUploadError(error, file) {
      console.error('图片上传失败:', error)
      this.$message.error('图片上传失败，请重试')
    },

    // 删除产品图片
    async deleteProductImage() {
      try {
        await this.$axios.delete(`/api/images/product/${this.productForm.id}`)
        this.productForm.imageUrl = ''
        this.$message.success('图片删除成功')
        this.loadProducts()
      } catch (error) {
        console.error('删除图片失败:', error)
        this.$message.error(error.response?.data?.message || '删除图片失败')
      }
    },

    // 模板文件上传前验证
    beforeTemplateUpload(file) {
      const allowedTypes = [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'text/plain'
      ]
      const isAllowed = allowedTypes.includes(file.type) || 
        /\.(pdf|doc|docx|xls|xlsx|txt)$/i.test(file.name)
      const isLt10M = file.size / 1024 / 1024 < 10

      if (!isAllowed) {
        this.$message.error('只能上传 PDF、Word、Excel、TXT 格式的文件!')
        return false
      }
      if (!isLt10M) {
        this.$message.error('文件大小不能超过 10MB!')
        return false
      }
      return true
    },

    // 模板文件上传成功
    handleTemplateUploadSuccess(response, file) {
      if (response.code === '00000') {
        this.productForm.templateFileName = response.data.fileName
        this.$message.success('模板文件上传成功')
        this.loadProducts()
      } else {
        this.$message.error(response.message || '模板文件上传失败')
      }
    },

    // 模板文件上传失败
    handleTemplateUploadError(error, file) {
      console.error('模板文件上传失败:', error)
      this.$message.error('模板文件上传失败，请重试')
    },

    // 下载模板文件
    downloadTemplate() {
      const link = document.createElement('a')
      link.href = `/api/images/product/${this.productForm.id}/template`
      link.download = this.productForm.templateFileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    },

    // 删除模板文件
    async deleteTemplate() {
      try {
        await this.$confirm('确定要删除该模板文件吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await this.$axios.delete(`/api/images/product/${this.productForm.id}/template`)
        this.productForm.templateFileName = ''
        this.$message.success('模板文件删除成功')
        this.loadProducts()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除模板文件失败:', error)
          this.$message.error(error.response?.data?.message || '删除模板文件失败')
        }
      }
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  font-size: 14px;
  color: #333;
  background-color: #f5f7fa;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.header {
  background-color: #fff;
  border-bottom: 3px solid #e60012;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 70px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
  width: 100%;
  box-sizing: border-box;
}

.logo {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  flex-shrink: 0;
  margin-right: 30px;
}

.logo-text {
  font-size: 22px;
  font-weight: bold;
  color: #e60012;
}

.logo-sub {
  font-size: 11px;
  color: #999;
}

.nav {
  flex: 1;
  display: flex;
  justify-content: center;
}

.nav .el-menu {
  border-bottom: none;
  display: flex;
  justify-content: flex-start;
  background: transparent;
}

.nav .el-menu-item {
  height: 68px;
  line-height: 68px;
  font-size: 14px;
  padding: 0 15px;
  min-width: 100px;
}

.nav .el-menu-item .el-icon {
  margin-right: 6px;
}

.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  margin-left: 30px;
}

.user-stats {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
}

.stat-item.balance {
  padding: 6px 12px;
  background: linear-gradient(135deg, #fff5f5 0%, #ffeaea 100%);
  border-radius: 20px;
  color: #e60012;
  font-weight: 600;
}

.stat-label {
  color: #999;
}

.stat-value {
  color: #333;
  font-weight: 500;
}

.balance .stat-value {
  color: #e60012;
}

.user-info-dropdown {
  display: flex;
  align-items: center;
}

.user-info-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.user-info-trigger:hover {
  background: #e8eaf0;
}

.user-info-trigger .user-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.user-info-trigger .dropdown-arrow {
  font-size: 12px;
  color: #999;
  transition: transform 0.3s;
}

.user-info-dropdown:hover .dropdown-arrow {
  transform: rotate(180deg);
}

.dropdown-user-info {
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  margin: 0 -12px;
}

.dropdown-balance {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #333;
}

.dropdown-balance .el-icon {
  color: #e60012;
}

.dropdown-balance span {
  font-weight: 600;
  color: #e60012;
}

/* 主体内容 */
.main-content {
  flex: 1;
  display: flex;
  padding: 20px;
  gap: 20px;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

/* 左侧边栏 */
.sidebar {
  width: 220px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 16px;
  flex-shrink: 0;
  height: fit-content;
  position: sticky;
  top: 90px;
}

.sidebar-section {
  margin-bottom: 24px;
}

.sidebar-section:last-child {
  margin-bottom: 0;
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}

.sidebar-title .el-icon {
  color: #e60012;
}

.sidebar .el-menu {
  border-right: none;
}

.sidebar .el-menu-item {
  height: 42px;
  line-height: 42px;
  border-radius: 6px;
  margin-bottom: 4px;
}

.sidebar .el-menu-item:hover {
  background-color: #fff5f5;
}

.sidebar .el-menu-item.is-active {
  background-color: #e60012;
  color: #fff;
}

.company-list {
  max-height: 280px;
  overflow-y: auto;
}

.company-list .el-checkbox {
  display: flex;
  margin-bottom: 8px;
  width: 100%;
}

/* 右侧内容区域 */
.content {
  flex: 1;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 24px;
  min-height: calc(100vh - 110px);
  overflow-x: auto;
}

/* 表格样式 */
.content .el-table {
  width: 100%;
  min-width: 800px;
}

.content .el-table__header-wrapper,
.content .el-table__body-wrapper {
  width: 100%;
}

/* 表格列宽调整 */
.content .el-table-column {
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  flex-wrap: nowrap;
  gap: 4px;
  align-items: center;
}

/* 页面通用样式 */
.page h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

/* 首页样式 */
.home-page {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.welcome-card {
  background: linear-gradient(135deg, #e60012 0%, #ff4d5a 100%);
  border-radius: 12px;
  padding: 48px;
  color: #fff;
  text-align: center;
}

.welcome-icon {
  margin-bottom: 20px;
}

.welcome-card h1 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 12px;
}

.welcome-card p {
  font-size: 15px;
  opacity: 0.9;
  margin-bottom: 30px;
}

.quick-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.quick-actions .el-button {
  padding: 14px 32px;
  font-size: 15px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  color: #1976d2;
}

.stat-icon.green {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  color: #388e3c;
}

.stat-icon.orange {
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  color: #f57c00;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 26px;
  font-weight: 700;
  color: #333;
}

.stat-name {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

/* 筛选表单样式 */
.filter-form {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.filter-header h3 {
  margin-bottom: 0;
}

.filter-form-row {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* 产品筛选样式 */
.product-filter {
  margin-bottom: 24px;
  padding: 20px 24px;
  background-color: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.filter-header h3 {
  margin-bottom: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-row {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-row:last-child {
  margin-bottom: 0;
}

.filter-label {
  font-weight: 600;
  min-width: 80px;
  color: #666;
  font-size: 14px;
}

/* 产品统计样式 */
.product-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 0 4px;
  font-size: 13px;
  color: #666;
}

.product-stats .stats-text strong {
  color: #e60012;
  font-size: 15px;
}

.product-stats .stats-divider {
  color: #d9d9d9;
}

.product-stats .filter-status {
  color: #1890ff;
  background: #e6f7ff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

/* 产品列表样式 */
.product-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-item {
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  transition: all 0.3s ease;
  border: 1px solid #eee;
}

.product-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  border-color: #e60012;
}

.product-card {
  display: flex;
  flex-direction: row;
  height: 100%;
}

.product-image-wrapper {
  position: relative;
  width: 200px;
  min-width: 200px;
  height: 150px;
  cursor: pointer;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.product-image-wrapper .product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.product-image-wrapper:hover .product-img {
  transform: scale(1.05);
}

.product-image-wrapper .image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.75));
  color: #fff;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.product-image-wrapper:hover .image-overlay {
  opacity: 1;
}

.product-badges {
  position: absolute;
  top: 8px;
  left: 8px;
  display: flex;
  gap: 6px;
}

.badge {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 20px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.badge.new {
  background: linear-gradient(135deg, #ff4d5a 0%, #e60012 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(230, 0, 18, 0.4);
}

.badge.hot {
  background: linear-gradient(135deg, #ffa726 0%, #ff9800 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(255, 152, 0, 0.4);
}

.product-content {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.product-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.4;
  flex: 1;
  padding-right: 12px;
}

.status-badge {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.status-badge.on-sale {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-badge.off-sale {
  background: #ffebee;
  color: #c62828;
}

.product-desc {
  margin-bottom: 12px;
  flex: 1;
}

.desc-text {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin: 0 0 6px 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.features-text {
  font-size: 12px;
  color: #888;
  margin: 0;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  background: #f8f9fa;
  padding: 6px 10px;
  border-radius: 6px;
}

.features-text .el-icon {
  color: #ffa726;
  margin-top: 2px;
}

.product-footer {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 8px 0;
  border-top: 1px solid #f0f0f0;
  margin-top: auto;
}

.price-info, .stock-info {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.price-label, .stock-label {
  font-size: 12px;
  color: #999;
}

.price-value {
  font-size: 18px;
  font-weight: 700;
  color: #e60012;
}

.stock-value {
  font-size: 14px;
  font-weight: 600;
  color: #52c41a;
}

.stock-value.low-stock {
  color: #ff4d4d;
}

.product-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.product-actions .el-button {
  padding: 6px 12px;
}

/* 图片预览样式 */
.image-preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.preview-image {
  max-width: 100%;
  max-height: 600px;
  object-fit: contain;
  border-radius: 8px;
}

/* 表格操作按钮 */
.table-actions {
  margin-bottom: 16px;
  display: flex;
  gap: 10px;
}

/* 分页样式 */
.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* 金额样式 */
.amount-positive {
  color: #52c41a;
  font-weight: 600;
}

.amount-negative {
  color: #ff4d4d;
  font-weight: 600;
}

/* 对话框样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式设计 - 移动端优先 */
@media screen and (max-width: 1024px) {
  .header {
    height: auto;
    min-height: 60px;
    padding: 0 16px;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
  }

  .header-left {
    flex: 1;
    gap: 16px;
    min-width: 0;
    align-items: center;
  }

  .logo {
    min-width: auto;
    flex-shrink: 0;
  }

  .logo-text {
    font-size: 18px;
  }

  .logo-sub {
    font-size: 10px;
  }

  .header-right {
    flex-shrink: 0;
  }

  .main-content {
    padding: 16px;
    gap: 16px;
  }

  .sidebar {
    width: 100%;
    position: static;
    padding: 12px;
  }

  .sidebar-title {
    font-size: 14px;
  }

  .sidebar .el-menu-item {
    height: 44px;
    line-height: 44px;
    font-size: 14px;
  }

  .stats-cards {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .product-card {
    flex-direction: column;
  }

  .product-image-wrapper {
    width: 100%;
    height: 160px;
  }

  .product-footer {
    flex-wrap: wrap;
  }

  .product-actions {
    width: 100%;
    margin-left: 0;
    margin-top: 12px;
    justify-content: flex-start;
  }

  .filter-form .el-form-item {
    margin-bottom: 12px;
  }

  .filter-form .el-select {
    width: 100% !important;
  }

  .filter-form .el-input {
    width: 100% !important;
  }

  .table-actions {
    flex-wrap: wrap;
    gap: 8px;
  }

  .table-actions .el-button {
    width: 100%;
  }
}

@media screen and (max-width: 768px) {
  .header {
    padding: 0 12px;
  }

  .logo-text {
    font-size: 16px;
  }

  .logo-sub {
    font-size: 9px;
  }

  .welcome-card {
    padding: 24px 16px;
  }

  .welcome-icon .el-icon {
    font-size: 48px !important;
  }

  .welcome-card h1 {
    font-size: 20px;
  }

  .welcome-card p {
    font-size: 14px;
  }

  .quick-actions {
    flex-direction: column;
    gap: 12px;
  }

  .quick-actions .el-button {
    width: 100%;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }

  .stat-card {
    flex-direction: row;
    padding: 16px;
    gap: 12px;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
  }

  .stat-info {
    flex: 1;
  }

  .product-filter {
    padding: 16px;
  }

  .filter-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-row .el-radio-group {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .product-stats {
    padding: 12px 16px;
    font-size: 13px;
  }

  .product-content {
    padding: 12px;
  }

  .product-name {
    font-size: 15px;
  }

  .product-desc {
    font-size: 13px;
  }

  .pagination {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media screen and (max-width: 480px) {
  .header {
    padding: 0 10px;
  }

  .logo-text {
    font-size: 15px;
  }

  .logo-sub {
    display: none;
  }

  .main-content {
    padding: 12px;
  }

  .stat-card {
    padding: 12px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
  }

  .stat-number {
    font-size: 20px;
  }

  .welcome-card {
    padding: 20px 12px;
  }

  .welcome-icon .el-icon {
    font-size: 40px !important;
  }

  .welcome-card h1 {
    font-size: 18px;
  }

  .filter-header h3 {
    font-size: 14px;
  }
}

/* Element Plus 覆盖样式 */
.el-menu-item {
  display: flex;
  align-items: center;
}

.el-menu-item .el-icon {
  margin-right: 4px;
}

.activate-product-info {
  background: #f5f7fa;
  padding: 12px 16px;
  border-radius: 4px;
  margin-bottom: 18px;
}

.activate-product-info .info-item {
  line-height: 28px;
}

.activate-product-info .label {
  color: #909399;
  font-size: 14px;
}

.activate-product-info .value {
  color: #303133;
  font-size: 14px;
}

.activate-product-info .price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.activate-summary {
  padding: 10px 0;
}

.activate-summary .summary-row {
  display: flex;
  justify-content: space-between;
  line-height: 28px;
  font-size: 14px;
}

.activate-summary .balance {
  color: #409eff;
  font-weight: bold;
}

.activate-summary .amount {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.activate-summary .remain.enough {
  color: #67c23a;
}

.activate-summary .remain.not-enough {
  color: #f56c6c;
}

/* 图片上传组件样式 */
.product-image-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 178px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.3s;
}

.product-image-uploader:hover {
  border-color: #409eff;
}

.product-image-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
  line-height: 178px;
}

.product-image-preview {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

.image-upload-tip {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.image-upload-tip .tip-text {
  font-size: 12px;
  color: #909399;
}

/* 模板文件上传样式 */
.template-upload-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.template-uploader {
  display: inline-block;
}

.template-file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.template-file-info .el-icon {
  font-size: 18px;
  color: #409eff;
}

.template-file-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.template-upload-tip {
  font-size: 12px;
  color: #909399;
}
</style>
