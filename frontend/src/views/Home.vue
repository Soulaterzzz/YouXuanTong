<template>
  <div class="app-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="logo">
        <span class="logo-text">优选通</span>
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
            <h1>欢迎使用优选通保险服务平台</h1>
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

          <div v-if="isAdmin" class="admin-analysis-toolbar">
            <div class="admin-analysis-filter">
              <div class="admin-analysis-filter-meta">
                <h3>经营分析</h3>
                <p>可按时间范围查看销量排行与订单总量走势</p>
              </div>
              <div class="admin-analysis-filter-actions">
                <el-button :type="adminAnalysisPeriodType === 'WEEK' ? 'primary' : 'default'" @click="applyAdminAnalysisPreset('WEEK')">本周</el-button>
                <el-button :type="adminAnalysisPeriodType === 'MONTH' ? 'primary' : 'default'" @click="applyAdminAnalysisPreset('MONTH')">本月</el-button>
                <el-button :type="adminAnalysisPeriodType === 'QUARTER' ? 'primary' : 'default'" @click="applyAdminAnalysisPreset('QUARTER')">季度</el-button>
                <el-button :type="adminAnalysisPeriodType === 'YEAR' ? 'primary' : 'default'" @click="applyAdminAnalysisPreset('YEAR')">全年</el-button>
              </div>
            </div>
          </div>

          <div v-if="isAdmin" class="admin-analysis-grid admin-analysis-stack">
            <div class="admin-analysis-card">
              <div class="admin-analysis-header">
                <div>
                  <h3>产品销量排行</h3>
                  <p>{{ adminAnalysisRangeText }}</p>
                </div>
                <el-tag type="success" effect="plain">TOP 10</el-tag>
              </div>
              <el-table v-if="adminSalesRanking.length" :data="adminSalesRanking" size="small" stripe>
                <el-table-column prop="rankNo" label="排名" width="70" align="center" />
                <el-table-column prop="productName" label="产品名称" min-width="220" show-overflow-tooltip />
                <el-table-column prop="orderCount" label="订单数" width="90" align="center" />
                <el-table-column prop="salesQuantity" label="销量份数" width="100" align="center" />
                <el-table-column label="销售金额" width="120" align="right">
                  <template #default="scope">¥{{ Number(scope.row.salesAmount || 0).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column prop="activePolicyCount" label="生效保单" width="100" align="center" />
              </el-table>
              <el-empty v-else description="当前时间范围暂无销量数据" :image-size="88">
                <el-button type="primary" @click="applyAdminAnalysisPreset('MONTH')">查看本月</el-button>
              </el-empty>
            </div>

            <div class="admin-analysis-card">
              <div class="admin-analysis-header">
                <div>
                  <h3>总订单量表</h3>
                  <p>{{ adminOrderTrendRangeText }}</p>
                </div>
                <el-tag type="warning" effect="plain">{{ adminOrderTrendModeLabel }}</el-tag>
              </div>
              <el-table v-if="adminOrderTrend.length" :data="adminOrderTrend" size="small" stripe>
                <el-table-column prop="dateLabel" label="时间段" min-width="180" align="center" />
                <el-table-column prop="orderCount" label="订单量" min-width="120" align="center" />
              </el-table>
              <el-empty v-else description="当前时间范围暂无订单趋势数据" :image-size="88">
                <el-button type="primary" @click="applyAdminAnalysisPreset('MONTH')">查看本月</el-button>
              </el-empty>
            </div>
          </div>

          <div class="notice-board-card">
            <div class="notice-board-header">
              <div class="notice-board-meta">
                <h3>通知公告</h3>
                <p>管理员首页和用户首页展示同一份通知列表。</p>
              </div>
              <div class="notice-board-actions">
                <el-tag type="info" effect="plain">{{ publishedNoticeTimeText }}</el-tag>
                <el-button v-if="isAdmin" type="primary" @click="openNoticePublishDialog">
                  <el-icon><Plus /></el-icon>
                  发布通知
                </el-button>
              </div>
            </div>

            <div v-if="publishedNoticeList.length" class="notice-list notice-published-list">
              <div v-for="(item, index) in publishedNoticeList" :key="item.id || index" 
                   class="notice-list-item notice-published-item" 
                   @click="openNoticeDetail(item)">
                <div class="notice-list-order">{{ index + 1 }}</div>
                <div class="notice-list-content">
                  <h4>{{ item.title }}</h4>
                  <p>{{ item.content }}</p>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无通知公告" :image-size="90" />
          </div>
        </div>

        <!-- 产品中心 -->
        <div v-else-if="activeMenu === 'product'" class="page">
          <!-- 产品分类筛选 -->
          <div class="product-filter">
            <div class="filter-header">
              <h3><el-icon><Filter /></el-icon> 产品筛选</h3>
              <div class="filter-header-actions">
                <el-button v-if="isAdmin" type="warning" plain @click="openCategoryDialog">
                  <el-icon><Setting /></el-icon>
                  管理类别
                </el-button>
                <el-button v-if="isAdmin" type="success" plain @click="openCompanyDialog">
                  <el-icon><Setting /></el-icon>
                  管理公司
                </el-button>
                <el-button text @click="resetProductFilter" v-if="companyFilter !== 'all' || activeCategory !== 'all'">
                  <el-icon><Refresh /></el-icon>
                  重置筛选
                </el-button>
                <el-button v-if="isAdmin" type="primary" @click="openProductDialog()">
                  <el-icon><Plus /></el-icon>
                  添加产品
                </el-button>
              </div>
            </div>
            <div class="filter-row">
              <span class="filter-label">承保公司：</span>
              <el-radio-group v-model="companyFilter" @change="handleCompanyChange" size="default" class="product-company-group">
                <el-radio-button label="all">全部</el-radio-button>
                <el-radio-button v-for="company in companyList" :key="company.code" :label="company.code">
                  {{ company.name }}
                </el-radio-button>
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
                    <div class="product-stats-row">
                      <div class="price-info">
                        <span class="price-label">价格：</span>
                        <span class="price-value">¥{{ product.price }}</span>
                      </div>
                      <div class="stock-info">
                        <span class="stock-label">库存：</span>
                        <span class="stock-value" :class="{ 'low-stock': product.stock < 10 }">
                          {{ product.stock > 0 ? product.stock : '充足' }}
                        </span>
                      </div>
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
                      <el-button v-if="isAdmin" type="danger" plain size="small" @click="deleteProduct(product)">
                        <el-icon><Delete /></el-icon>
                        删除
                      </el-button>
                      <el-button
                        type="warning"
                        plain
                        size="small"
                        :disabled="!product.templateFileName"
                        @click="openProductInsuranceBatchDialog(product)"
                      >
                        <el-icon><Upload /></el-icon>
                        批量激活
                      </el-button>
                      <el-button
                        type="primary"
                        plain
                        size="small"
                        :disabled="!product.templateFileName"
                        @click="downloadProductTemplate(product)"
                      >
                        <el-icon><Download /></el-icon>
                        下载模板
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
              <div class="filter-header-actions">
                <el-button text @click="resetExpenseFilter">
                  <el-icon><Refresh /></el-icon>
                  重置筛选
                </el-button>
                <el-button type="success" plain @click="downloadExpenseExport">
                  <el-icon><Download /></el-icon>
                  导出Excel
                </el-button>
              </div>
            </div>
            <el-form :inline="true" :model="expenseFilter" class="demo-form-inline">
              <el-form-item label="方案">
                <el-select v-model="expenseFilter.plan" placeholder="请选择" clearable style="width: 150px;">
                  <el-option label="所有方案" value="all"></el-option>
                  <el-option label="国寿财意外险（1-3类）" value="guoshou-3"></el-option>
                  <el-option label="平安财意外险" value="pingan-10"></el-option>
                  <el-option label="少儿医疗险" value="child-med"></el-option>
                  <el-option label="老年意外险" value="elder-acc"></el-option>
                  <el-option label="旅游险" value="travel"></el-option>
                  <el-option label="驾乘险" value="maternity"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="expenseFilter.status" placeholder="请选择" clearable style="width: 150px;">
                  <el-option label="全部" value="all"></el-option>
                  <el-option label="待审核" value="PENDING_REVIEW"></el-option>
                  <el-option label="审核通过" value="APPROVED"></el-option>
                  <el-option label="审核驳回" value="REVIEW_REJECTED"></el-option>
                  <el-option label="承保中" value="UNDERWRITING"></el-option>
                  <el-option label="已生效" value="ACTIVE"></el-option>
                  <el-option label="已取消" value="cancelled"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="序列号">
                <el-input v-model="expenseFilter.serial" placeholder="请输入序列号" clearable style="width: 150px;"></el-input>
              </el-form-item>
              <el-form-item label="上传日期">
                <el-date-picker
                  v-model="expenseFilter.dateRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  style="width: 240px;"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="queryExpenses">
                  <el-icon><Search /></el-icon>
                  查询
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 费用清单表格 -->
          <el-table :data="expenseList" style="width: 100%" v-loading="expenseLoading" stripe>
            <el-table-column prop="serial" label="序列号" width="180"></el-table-column>
            <el-table-column prop="product" label="产品名称" min-width="220"></el-table-column>
            <el-table-column prop="contact" label="联系人" width="120"></el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170"></el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="count" label="份数" width="90" align="center"></el-table-column>
            <el-table-column prop="total" label="金额" width="120" align="right">
              <template #default="scope">¥{{ scope.row.total }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
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
              :page-size="expensePageSize"
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
              <div class="filter-header-actions">
                <el-button text @click="resetInsuranceFilter">
                  <el-icon><Refresh /></el-icon>
                  重置筛选
                </el-button>
                <el-button type="success" plain @click="downloadInsuranceExport">
                  <el-icon><Download /></el-icon>
                  导出PDF
                </el-button>
              </div>
            </div>
            <el-form :inline="true" :model="insuranceFilter" class="demo-form-inline">
              <el-form-item label="方案">
                <el-select v-model="insuranceFilter.plan" placeholder="请选择" clearable style="width: 150px;">
                  <el-option label="所有方案" value="all"></el-option>
                  <el-option label="国寿财意外险（1-3类）" value="guoshou-3"></el-option>
                  <el-option label="平安财意外险" value="pingan-10"></el-option>
                  <el-option label="少儿医疗险" value="child-med"></el-option>
                  <el-option label="老年意外险" value="elder-acc"></el-option>
                  <el-option label="旅游险" value="travel"></el-option>
                  <el-option label="驾乘险" value="maternity"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="insuranceFilter.status" placeholder="请选择" clearable style="width: 150px;">
                  <el-option label="全部" value="all"></el-option>
                  <el-option v-if="!isAdmin" label="待提交" value="DRAFT"></el-option>
                  <el-option label="待审核" value="PENDING_REVIEW"></el-option>
                  <el-option label="审核通过" value="APPROVED"></el-option>
                  <el-option label="审核驳回" value="REVIEW_REJECTED"></el-option>
                  <el-option label="承保中" value="UNDERWRITING"></el-option>
                  <el-option label="已生效" value="ACTIVE"></el-option>
                  <el-option label="已过期" value="EXPIRED"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="序列号">
                <el-input v-model="insuranceFilter.serial" placeholder="请输入序列号" clearable style="width: 150px;"></el-input>
              </el-form-item>
              <el-form-item label="上传日期">
                <el-date-picker
                  v-model="insuranceFilter.dateRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  style="width: 240px;"
                />
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
          <div v-if="isAdmin" class="table-actions">
            <el-button
              type="warning"
              :disabled="!selectedInsurances.some(item => item.statusCode === 'UNDERWRITING')"
              @click="openInsuranceActivationDialog(selectedInsurances.filter(item => item.statusCode === 'UNDERWRITING'))"
            >
              <el-icon><Lightning /></el-icon>
              批量生效
            </el-button>
          </div>

          <!-- 保险清单表格 -->
          <el-table :data="insuranceList" style="width: 100%" v-loading="insuranceLoading" stripe @selection-change="handleSelectionChange">
            <el-table-column v-if="isAdmin" type="selection" width="55"></el-table-column>
            <el-table-column prop="product" label="产品名称" min-width="220"></el-table-column>
            <el-table-column prop="insuredName" label="投保人" width="120"></el-table-column>
            <el-table-column prop="beneficiaryName" label="被保人" width="120"></el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="policyNo" label="保单号" min-width="200" show-overflow-tooltip></el-table-column>
            <el-table-column prop="startDate" label="起保日期" width="120"></el-table-column>
            <el-table-column prop="endDate" label="结束日期" width="120"></el-table-column>
            <el-table-column v-if="isAdmin" prop="agent" label="业务员" width="110"></el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="viewInsuranceDetail(scope.row)">详情</el-button>
                <el-button link type="success" @click="downloadPolicy(scope.row)">导出PDF</el-button>
                <el-button
                  v-if="!isAdmin && scope.row.statusCode === 'DRAFT'"
                  link
                  type="primary"
                  @click="submitInsuranceDraft(scope.row)"
                >
                  提交审核
                </el-button>
                <el-button
                  v-if="isAdmin && scope.row.statusCode === 'PENDING_REVIEW'"
                  link
                  type="success"
                  @click="openInsuranceReviewDialog(scope.row, 'approve')"
                >
                  通过
                </el-button>
                <el-button
                  v-if="isAdmin && scope.row.statusCode === 'PENDING_REVIEW'"
                  link
                  type="danger"
                  @click="openInsuranceReviewDialog(scope.row, 'reject')"
                >
                  驳回
                </el-button>
                <el-button
                  v-if="isAdmin && scope.row.statusCode === 'APPROVED'"
                  link
                  type="warning"
                  @click="startInsuranceUnderwriting(scope.row)"
                >
                  承保
                </el-button>
                <el-button
                  v-if="isAdmin && scope.row.statusCode === 'UNDERWRITING'"
                  link
                  type="warning"
                  @click="openInsuranceActivationDialog([scope.row])"
                >
                  生效
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :total="insuranceTotal"
              :page-size="insurancePageSize"
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
            <el-button type="primary" @click="openRechargeDialog">
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
              <div class="filter-header-actions">
                <el-button text @click="resetUserQuery">
                  <el-icon><Refresh /></el-icon>
                  重置筛选
                </el-button>
                <el-button type="primary" @click="openUserDialog('create')">
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
    <el-dialog v-model="productDialogVisible" :title="productDialogTitle" width="620px" :close-on-click-modal="false">
      <el-form :model="productForm" label-width="90px">
        <el-form-item label="产品编码">
          <el-input v-model="productForm.productCode" placeholder="请输入产品编码" />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="productForm.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="产品类别">
          <el-select v-model="productForm.categoryCode" style="width: 100%" placeholder="请选择产品类别">
            <el-option v-for="category in categoryList" :key="category.code" :label="category.name" :value="category.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="承保公司">
          <el-select v-model="productForm.companyCode" style="width: 100%" placeholder="请选择承保公司">
            <el-option v-for="company in companyList" :key="company.code" :label="company.name" :value="company.code" />
          </el-select>
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
            :disabled="!productForm.id"
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
            <el-button size="small" type="danger" @click="deleteProductImage" v-if="productForm.id && productForm.imageUrl">
              删除图片
            </el-button>
            <span class="tip-text">{{ productForm.id ? '支持 jpg、png、gif、webp 格式，大小不超过 10MB' : '请先保存产品后再上传图片' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="模板文件">
          <div class="template-upload-container">
            <el-upload
              class="template-uploader"
              :action="`/api/images/product/${productForm.id}/template`"
              :disabled="!productForm.id"
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
              {{ productForm.id ? '支持 PDF、Word、Excel、TXT 格式，大小不超过 10MB' : '请先保存产品后再上传模板文件' }}
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

    <el-dialog
      v-model="productInsuranceBatchDialogVisible"
      :title="productInsuranceBatchDialogTitle"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
      @close="closeProductInsuranceBatchDialog"
    >
      <div class="product-batch-dialog">
        <div class="product-batch-summary" v-if="productInsuranceBatchProduct">
          <div class="product-batch-name">{{ productInsuranceBatchProduct.name }}</div>
          <div class="product-batch-subtitle">
            请先下载当前产品模板，填写后再上传。系统会按行读取投保信息。
          </div>
        </div>
        <el-upload
          ref="productInsuranceBatchUploadRef"
          class="product-batch-upload"
          drag
          action="#"
          :auto-upload="false"
          :limit="1"
          :show-file-list="true"
          accept=".xls,.xlsx"
          :before-upload="beforeProductInsuranceBatchUpload"
          :on-change="handleProductInsuranceBatchFileChange"
          :on-remove="handleProductInsuranceBatchFileRemove"
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">拖拽 .xls / .xlsx 文件到这里，或 <em>点击选择文件</em></div>
          <template #tip>
            <div class="el-upload__tip">仅支持 Excel .xls / .xlsx 文件，大小不超过 10MB</div>
          </template>
        </el-upload>
        <div v-if="productInsuranceBatchPreviewLoading" class="product-batch-preview-tip">
          正在预览导入条数...
        </div>
        <div v-else-if="productInsuranceBatchPreviewCount !== null" class="product-batch-preview-tip">
          预计导入 {{ productInsuranceBatchPreviewCount }} 条投保信息
        </div>
        <div v-else class="product-batch-preview-tip product-batch-preview-tip--muted">
          选择文件后将自动预览导入条数
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeProductInsuranceBatchDialog">取消</el-button>
          <el-button
            type="primary"
            :loading="productInsuranceBatchPreviewLoading"
            :disabled="!productInsuranceBatchFile || productInsuranceBatchPreviewLoading || !productInsuranceBatchPreviewRows.length"
            @click="openProductInsuranceBatchConfirmDialog"
          >
            查看确认页
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="productInsuranceBatchConfirmDialogVisible"
      :title="`批量激活确认 - ${productInsuranceBatchProduct?.name || '产品'}`"
      width="980px"
      :close-on-click-modal="false"
      destroy-on-close
      @close="closeProductInsuranceBatchConfirmDialog"
    >
      <div class="product-batch-confirm">
        <div class="product-batch-summary" v-if="productInsuranceBatchProduct">
          <div class="product-batch-name">{{ productInsuranceBatchProduct.name }}</div>
          <div class="product-batch-subtitle">
            已读取 {{ productInsuranceBatchPreviewRows.length }} 条投保数据，请确认无误后再提交激活。
          </div>
        </div>
        <el-table
          class="product-batch-preview-table"
          :data="productInsuranceBatchPreviewRows"
          border
          stripe
          height="420"
          empty-text="暂无可确认的数据"
        >
          <el-table-column prop="rowNumber" label="行号" width="80" />
          <el-table-column prop="planName" label="方案名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="beneficiaryName" label="被保人姓名" min-width="120" show-overflow-tooltip />
          <el-table-column prop="beneficiaryId" label="证件号" min-width="180" show-overflow-tooltip />
          <el-table-column prop="beneficiaryJob" label="职业" min-width="120" show-overflow-tooltip />
          <el-table-column prop="count" label="份数" width="80" />
          <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
          <el-table-column prop="agent" label="业务员" min-width="120" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeProductInsuranceBatchConfirmDialog">返回修改</el-button>
          <el-button
            type="primary"
            :loading="productInsuranceBatchSubmitting"
            :disabled="!productInsuranceBatchPreviewRows.length"
            @click="submitProductInsuranceBatch"
          >
            确认提交激活
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 产品类别管理对话框 -->
    <el-dialog v-model="categoryDialogVisible" title="产品类别管理" width="700px" :close-on-click-modal="false">
      <div class="category-management">
        <div class="category-form">
          <el-form :inline="true" :model="categoryForm" label-width="80px">
            <el-form-item label="类别编码">
              <el-input v-model="categoryForm.code" placeholder="如：1-3" style="width: 150px" />
            </el-form-item>
            <el-form-item label="类别名称">
              <el-input v-model="categoryForm.name" placeholder="如：1-3类意外" style="width: 200px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="addCategory">
                <el-icon><Plus /></el-icon>
                添加
              </el-button>
              <el-button @click="resetCategoryForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="category-list">
          <el-table :data="categoryList" border stripe>
            <el-table-column prop="code" label="类别编码" width="120" />
            <el-table-column prop="name" label="类别名称" />
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button type="danger" size="small" @click="deleteCategory(scope.row)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>

    <!-- 承保公司管理对话框 -->
    <el-dialog v-model="companyDialogVisible" title="承保公司管理" width="700px" :close-on-click-modal="false">
      <div class="company-management">
        <div class="company-form">
          <el-form :inline="true" :model="companyForm" label-width="80px">
            <el-form-item label="公司编码">
              <el-input v-model="companyForm.code" placeholder="如：guoshou" style="width: 150px" />
            </el-form-item>
            <el-form-item label="公司名称">
              <el-input v-model="companyForm.name" placeholder="如：国寿财险" style="width: 200px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="addCompany">
                <el-icon><Plus /></el-icon>
                添加
              </el-button>
              <el-button @click="resetCompanyForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="company-list">
          <el-table :data="companyList" border stripe>
            <el-table-column prop="code" label="公司编码" width="150" />
            <el-table-column prop="name" label="公司名称" />
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button type="danger" size="small" @click="deleteCompany(scope.row)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
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
            <el-option label="国寿财意外险（1-3类）" value="guoshou-3"></el-option>
            <el-option label="平安财意外险" value="pingan-10"></el-option>
            <el-option label="少儿医疗险" value="child-med"></el-option>
            <el-option label="老年意外险" value="elder-acc"></el-option>
            <el-option label="旅游险" value="travel"></el-option>
            <el-option label="驾乘险" value="maternity"></el-option>
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
          <el-button :loading="draftSubmitting" @click="saveDraftForm">保存待提交</el-button>
          <el-button type="primary" :loading="activeSubmitting" :disabled="selectedProduct && balance < selectedProduct.price * activeForm.count" @click="submitActiveForm">提交审核</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 充值弹窗 -->
    <el-dialog v-model="rechargeDialogVisible" title="账户充值" width="400px">
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

    <el-dialog
      v-model="insuranceActivationDialogVisible"
      :title="insuranceActivationDialogTitle"
      width="960px"
      :close-on-click-modal="false"
    >
      <div class="insurance-activation-tip">
        请填写保单号、起保日期和结束日期，提交后将同步更新费用清单状态和保单起止日期。
      </div>
      <el-table :data="insuranceActivationItems" border>
        <el-table-column prop="product" label="产品名称" min-width="160" />
        <el-table-column prop="beneficiaryName" label="被保人" width="120" />
        <el-table-column label="保单号" min-width="200">
          <template #default="scope">
            <el-input v-model="scope.row.policyNo" placeholder="请输入保单号" />
          </template>
        </el-table-column>
        <el-table-column label="起保日期" width="170">
          <template #default="scope">
            <el-date-picker
              v-model="scope.row.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="起保日期"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="结束日期" width="170">
          <template #default="scope">
            <el-date-picker
              v-model="scope.row.endDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="结束日期"
              style="width: 100%"
            />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="insuranceActivationDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="insuranceActivationSubmitting" @click="submitInsuranceActivation">
            确认生效
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="insuranceReviewDialogVisible"
      :title="insuranceReviewDialogTitle"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form label-width="88px">
        <el-form-item label="审核意见">
          <el-input
            v-model="insuranceReviewForm.reviewComment"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见"
          />
        </el-form-item>
        <el-form-item v-if="insuranceReviewAction === 'reject'" label="驳回原因">
          <el-input
            v-model="insuranceReviewForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="insuranceReviewDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="insuranceReviewSubmitting" @click="submitInsuranceReview">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="expenseDetailDialogVisible"
      title="费用详情"
      width="680px"
      :close-on-click-modal="true"
      destroy-on-close
    >
      <div v-if="expenseDetailRow" class="detail-dialog-body">
        <div class="detail-dialog-hero">
          <div>
            <div class="detail-dialog-eyebrow">费用清单</div>
            <h3 class="detail-dialog-title">{{ expenseDetailRow.product || '保险产品' }}</h3>
            <p class="detail-dialog-subtitle">{{ expenseDetailRow.serial || '-' }}</p>
          </div>
          <el-tag :type="getStatusType(expenseDetailRow.status)" effect="dark">{{ expenseDetailRow.status }}</el-tag>
        </div>

        <div class="detail-summary-grid">
          <div class="detail-summary-card">
            <span class="label">联系人</span>
            <strong>{{ expenseDetailRow.contact || '-' }}</strong>
          </div>
          <div class="detail-summary-card">
            <span class="label">创建时间</span>
            <strong>{{ formatPolicyTime(expenseDetailRow.createTime) }}</strong>
          </div>
          <div class="detail-summary-card">
            <span class="label">投保份数</span>
            <strong>{{ expenseDetailRow.count || 0 }} 份</strong>
          </div>
          <div class="detail-summary-card">
            <span class="label">总金额</span>
            <strong>¥{{ expenseDetailRow.total ?? '-' }}</strong>
          </div>
        </div>

        <el-descriptions :column="2" border class="detail-meta-block">
          <el-descriptions-item label="序列号">{{ expenseDetailRow.serial || '-' }}</el-descriptions-item>
          <el-descriptions-item label="保单号">{{ expenseDetailRow.policyNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="起保日期">{{ expenseDetailRow.startDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ expenseDetailRow.endDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="单价">¥{{ expenseDetailRow.price ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="小计">¥{{ expenseDetailRow.total ?? '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <el-dialog
      v-model="insuranceDetailDialogVisible"
      title="保单详情"
      width="760px"
      :close-on-click-modal="true"
      destroy-on-close
    >
      <div v-if="insuranceDetailRow" class="detail-dialog-body">
        <div class="detail-dialog-hero">
          <div>
            <div class="detail-dialog-eyebrow">保单生命周期</div>
            <h3 class="detail-dialog-title">{{ insuranceDetailRow.product || '保险产品' }}</h3>
            <p class="detail-dialog-subtitle">{{ insuranceDetailRow.policyNo || '正式保单号待下发' }}</p>
          </div>
          <el-tag :type="getStatusType(insuranceDetailRow.status)" effect="dark">{{ insuranceDetailRow.status }}</el-tag>
        </div>

        <div class="detail-summary-grid">
          <div class="detail-summary-card">
            <span class="label">投保人</span>
            <strong>{{ insuranceDetailRow.insuredName || '-' }}</strong>
          </div>
          <div class="detail-summary-card">
            <span class="label">被保人</span>
            <strong>{{ insuranceDetailRow.beneficiaryName || '-' }}</strong>
          </div>
          <div class="detail-summary-card">
            <span class="label">业务员</span>
            <strong>{{ insuranceDetailRow.agent || '-' }}</strong>
          </div>
          <div class="detail-summary-card">
            <span class="label">保障期限</span>
            <strong>{{ insuranceDetailRow.startDate || '-' }} 至 {{ insuranceDetailRow.endDate || '-' }}</strong>
          </div>
        </div>

        <el-descriptions :column="2" border class="detail-meta-block">
          <el-descriptions-item label="审核人">{{ insuranceDetailRow.reviewerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ formatPolicyTime(insuranceDetailRow.reviewTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">{{ insuranceDetailRow.reviewComment || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="insuranceDetailRow.rejectReason" label="驳回原因" :span="2">{{ insuranceDetailRow.rejectReason }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-block">
          <div class="detail-section-header">
            <h4>进度时间线</h4>
            <span>按业务节点查看当前保单处理进度</span>
          </div>
          <el-timeline class="policy-timeline">
            <el-timeline-item
              v-for="item in insuranceTimeline"
              :key="item.key"
              :timestamp="formatPolicyTime(item.time)"
              :type="item.type"
              placement="top"
            >
              <div class="policy-timeline-card">
                <div class="policy-timeline-title">{{ item.title }}</div>
                <p class="policy-timeline-content">{{ item.content }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-dialog>

    <!-- 用户管理弹窗 -->
    <el-dialog v-model="userDialogVisible" :title="userDialogTitle" width="450px" :close-on-click-modal="false">
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

    <!-- 图片预览弹窗 -->
    <el-dialog v-model="previewImageDialogVisible" :title="previewImageTitle" width="800px">
      <div class="image-preview-container">
        <img :src="previewImageUrl" :alt="previewImageTitle" class="preview-image">
      </div>
    </el-dialog>

    <!-- 通知详情弹窗 -->
    <el-dialog v-model="noticeDetailDialogVisible" :title="noticeDetail.title" width="600px">
      <div class="notice-detail-content">
        <div class="notice-detail-meta">
          <el-icon><Clock /></el-icon>
          <span>发布时间：{{ noticeDetail.publishedAt ? new Date(noticeDetail.publishedAt).toLocaleString('zh-CN', { hour12: false }) : '未知' }}</span>
        </div>
        <el-divider />
        <div class="notice-detail-body">
          {{ noticeDetail.content }}
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="noticeDetailDialogVisible = false">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="noticePopupDialogVisible"
      title="首页通知栏"
      width="980px"
      :close-on-click-modal="false"
      class="notice-popup-dialog"
    >
      <div class="notice-popup-shell">
        <aside class="notice-popup-list">
          <div class="notice-popup-list-header">
            <span>最新通知</span>
            <el-tag type="info" effect="plain">{{ publishedNoticeList.length }}</el-tag>
          </div>
          <div v-if="publishedNoticeList.length" class="notice-popup-items">
            <button
              v-for="(item, index) in publishedNoticeList"
              :key="item.id || index"
              type="button"
              class="notice-popup-item"
              :class="{ active: index === noticePopupSelectedIndex }"
              @click="selectNoticePopupItem(index)"
            >
              <div class="notice-popup-index">{{ index + 1 }}</div>
              <div class="notice-popup-item-body">
                <h4>{{ item.title }}</h4>
                <p>{{ item.content }}</p>
              </div>
            </button>
          </div>
          <el-empty v-else description="暂无通知公告" :image-size="84" />
        </aside>

        <section class="notice-popup-detail" v-if="noticePopupCurrent">
          <div class="notice-popup-eyebrow">首页弹窗公告</div>
          <h3>{{ noticePopupCurrent.title }}</h3>
          <div class="notice-popup-meta">
            <el-icon><Clock /></el-icon>
            <span>发布时间：{{ noticePopupCurrent.publishedAt ? new Date(noticePopupCurrent.publishedAt).toLocaleString('zh-CN', { hour12: false }) : '未知' }}</span>
          </div>
          <div class="notice-popup-content">
            {{ noticePopupCurrent.content }}
          </div>
        </section>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeNoticePopup">关闭</el-button>
          <el-button
            type="primary"
            :disabled="!noticePopupCurrent"
            @click="openNoticeFromPopup"
          >
            查看详情
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="noticePublishDialogVisible"
      title="发布通知"
      width="780px"
      :close-on-click-modal="false"
      class="notice-publish-dialog"
    >
      <div class="notice-admin-layout">
        <div class="notice-editor-panel">
          <el-form label-position="top">
            <el-form-item label="通知标题">
              <el-input
                v-model="noticeDraft.title"
                maxlength="30"
                show-word-limit
                placeholder="例如：系统升级提醒"
              />
            </el-form-item>
            <el-form-item label="通知内容">
              <el-input
                v-model="noticeDraft.content"
                type="textarea"
                :rows="5"
                maxlength="200"
                show-word-limit
                placeholder="请输入首页要展示的通知内容"
              />
            </el-form-item>
          </el-form>
          <div class="notice-editor-actions">
            <el-button @click="resetNoticeDraft">重置输入</el-button>
            <el-button type="primary" @click="saveNoticeDraftItem">
              {{ editingNoticeIndex > -1 ? '更新通知' : '加入列表' }}
            </el-button>
          </div>
        </div>

        <div class="notice-list-panel">
          <div class="notice-list-header">
            <span>待发布通知（{{ noticeDraftList.length }}）</span>
            <div class="notice-list-actions">
              <el-button text @click="clearNoticeDraftList" :disabled="!noticeDraftList.length">
                清空列表
              </el-button>
            </div>
          </div>

          <div v-if="noticeDraftList.length" class="notice-list">
            <div v-for="(item, index) in noticeDraftList" :key="item.id || index" class="notice-list-item">
              <div class="notice-list-order">{{ index + 1 }}</div>
              <div class="notice-list-content">
                <h4>{{ item.title }}</h4>
                <p>{{ item.content }}</p>
              </div>
              <div class="notice-list-item-actions">
                <el-button text type="primary" @click="editNoticeItem(index)">编辑</el-button>
                <el-button text type="danger" @click="removeNoticeItem(index)">删除</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无待发布通知" :image-size="80" />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="noticePublishDialogVisible = false">取消</el-button>
          <el-button type="success" @click="publishNotices" :disabled="!noticeDraftList.length">发布通知</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  House, Goods, Document, Collection, Wallet, User, Coin, SwitchButton,
  Grid, Menu, Sunny, Star, Present, OfficeBuilding,
  CircleCheck, Refresh, Search, Picture, Upload, Lightning, View, Download,
  Clock, Link, Edit, Switch, ZoomIn, Calendar, Timer, Notebook, Plus, Filter,
  ArrowDown, Delete, Bell, Setting
} from '@element-plus/icons-vue'

export default {
  name: 'Home',
  components: {
    House, Goods, Document, Collection, Wallet, User, Coin, SwitchButton,
    Grid, Menu, Sunny, Star, Present, OfficeBuilding,
    CircleCheck, Refresh, Search, Picture, Upload, Lightning, View, Download,
    Clock, Link, Edit, Switch, ZoomIn, Calendar, Timer, Notebook, Plus, Filter,
    ArrowDown, Delete, Bell, Setting
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
      adminSalesRanking: [],
      adminOrderTrend: [],
      adminAnalysisRange: [],
      adminAnalysisPeriodType: 'MONTH',
      productLoading: false,
      products: [],
      productTotal: 0,
      productPageSize: 10,
      productCurrentPage: 1,
      productImportLoading: false,
      companyFilter: 'all',
      categoryFilter: 'all',
      selectedCompanies: [],
      productDialogVisible: false,
      productDialogTitle: '编辑产品',
      productSubmitting: false,
      productInsuranceBatchDialogVisible: false,
      productInsuranceBatchDialogTitle: '批量激活',
      productInsuranceBatchSubmitting: false,
      productInsuranceBatchProduct: null,
      productInsuranceBatchFile: null,
      productInsuranceBatchPreviewLoading: false,
      productInsuranceBatchPreviewCount: null,
      productInsuranceBatchPreviewRows: [],
      productInsuranceBatchConfirmDialogVisible: false,
      productInsuranceBatchPreviewRequestId: 0,
      productForm: {
        id: null,
        productCode: '',
        productName: '',
        categoryCode: '',
        companyCode: '',
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
      expensePageSize: 20,
      expenseCurrentPage: 1,
      expenseFilter: {
        plan: 'all',
        status: 'all',
        serial: '',
        dateRange: []
      },
      insuranceLoading: false,
      insuranceList: [],
      insuranceTotal: 0,
      insurancePageSize: 20,
      insuranceCurrentPage: 1,
      insuranceFilter: {
        plan: 'all',
        status: 'all',
        serial: '',
        dateRange: [],
        insuredName: '',
        insuredId: '',
        beneficiaryName: '',
        beneficiaryId: '',
        agent: ''
      },
      selectedInsurances: [],
      insuranceActivationDialogVisible: false,
      insuranceActivationDialogTitle: '保单生效',
      insuranceActivationSubmitting: false,
      insuranceActivationItems: [],
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
        mobile: '',
        password: '',
        confirmPassword: ''
      },
      userFormRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        mobile: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ]
      },
      noticePublishDialogVisible: false,
      noticePublishedAt: '',
      publishedNoticeList: [],
      noticeDraftList: [],
      noticeDraft: {
        id: null,
        title: '',
        content: ''
      },
      editingNoticeIndex: -1,
      noticeDetailDialogVisible: false,
      noticeDetail: {
        title: '',
        content: '',
        publishedAt: ''
      },
      selectedProduct: null,
      activeDialogVisible: false,
      activeSubmitting: false,
      draftSubmitting: false,
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
      insuranceReviewDialogVisible: false,
      insuranceReviewDialogTitle: '保单审核',
      insuranceReviewSubmitting: false,
      insuranceReviewAction: 'approve',
      insuranceReviewForm: {
        insuranceId: null,
        reviewComment: '',
        rejectReason: ''
      },
      expenseDetailDialogVisible: false,
      expenseDetailRow: null,
      insuranceDetailDialogVisible: false,
      insuranceDetailRow: null,
      insuranceTimeline: [],
      rechargeDialogVisible: false,
      rechargeSubmitting: false,
      rechargeForm: {
        userId: null,
        amount: 1000,
        method: 'alipay',
        remark: ''
      },
      rechargeFormRules: {
        userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
        amount: [{ required: true, message: '请输入充值金额', trigger: 'blur' }]
      },
      // 图片预览相关
      previewImageDialogVisible: false,
      previewImageUrl: '',
      previewImageTitle: '',
      // 通知公告弹窗
      noticePopupDialogVisible: false,
      noticePopupData: null,
      noticePopupSelectedIndex: 0,
      // 产品类别管理
      categoryDialogVisible: false,
      categoryForm: {
        code: '',
        name: ''
      },
      categoryList: [],
      // 承保公司管理
      companyDialogVisible: false,
      companyForm: {
        code: '',
        name: ''
      },
      companyList: []
    }
  },
  computed: {
    publishedNoticeTimeText() {
      if (!this.noticePublishedAt) {
        return '最近发布：未发布'
      }

      return `最近发布：${new Date(this.noticePublishedAt).toLocaleString('zh-CN', { hour12: false })}`
    },
    adminAnalysisRangeText() {
      if (!this.adminAnalysisRange || this.adminAnalysisRange.length !== 2) {
        return '默认展示本月的数据表现'
      }
      return `${this.adminAnalysisRange[0]} 至 ${this.adminAnalysisRange[1]}`
    },
    adminOrderTrendRangeText() {
      return `${this.adminAnalysisRangeText} · ${this.adminOrderTrendModeLabel}`
    },
    adminOrderTrendModeLabel() {
      const labelMap = {
        WEEK: '按日统计',
        MONTH: '按周统计',
        QUARTER: '按半月统计',
        YEAR: '按月统计'
      }
      return `${labelMap[this.adminAnalysisPeriodType] || '按周统计'} · 共 ${this.adminOrderTrendTotal} 单`
    },
    adminOrderTrendTotal() {
      return this.adminOrderTrend.reduce((sum, item) => sum + Number(item.orderCount || 0), 0)
    },
    noticePopupCurrent() {
      return this.publishedNoticeList[this.noticePopupSelectedIndex] || this.publishedNoticeList[0] || null
    },
  },
  mounted() {
    if (!sessionStorage.getItem('authToken')) {
      this.$router.push('/login')
      return
    }
    if (this.isAdmin) {
      this.adminAnalysisRange = this.buildAdminAnalysisRange('MONTH')
    }
    this.loadStats()
    this.loadProducts()
    this.loadBalance()
    this.initializeNoticeCenter()
    this.loadCategories()
    this.loadCompanies()
  },
  methods: {
    isSuccess(res) {
      return res && res.data && res.data.code === '00000'
    },

    getAdminStatsParams() {
      if (!this.adminAnalysisRange || this.adminAnalysisRange.length !== 2) {
        return { periodType: this.adminAnalysisPeriodType }
      }
      return {
        periodType: this.adminAnalysisPeriodType,
        startDate: this.adminAnalysisRange[0],
        endDate: this.adminAnalysisRange[1]
      }
    },

    buildAdminAnalysisRange(periodType) {
      const current = new Date()
      current.setHours(0, 0, 0, 0)
      let start = new Date(current)

      switch (periodType) {
        case 'WEEK': {
          const day = current.getDay() === 0 ? 7 : current.getDay()
          start.setDate(current.getDate() - day + 1)
          break
        }
        case 'QUARTER': {
          const quarterStartMonth = Math.floor(current.getMonth() / 3) * 3
          start = new Date(current.getFullYear(), quarterStartMonth, 1)
          break
        }
        case 'YEAR':
          start = new Date(current.getFullYear(), 0, 1)
          break
        case 'MONTH':
        default:
          start = new Date(current.getFullYear(), current.getMonth(), 1)
          break
      }

      return [this.formatAdminDate(start), this.formatAdminDate(current)]
    },

    formatAdminDate(date) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },

    applyAdminAnalysisPreset(periodType) {
      this.adminAnalysisPeriodType = periodType
      this.adminAnalysisRange = this.buildAdminAnalysisRange(periodType)
      this.loadStats()
    },

    createNoticeId() {
      return `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
    },

    normalizeNoticeItem(item) {
      const title = String(item?.title || '').trim()
      const content = String(item?.content || '').trim()

      if (!title || !content) {
        return null
      }

      return {
        id: item?.id || this.createNoticeId(),
        title,
        content,
        sortNo: item?.sortNo ?? 0,
        publishedAt: item?.publishedAt || ''
      }
    },

    cloneNoticeList(list = []) {
      return list.map(item => ({ ...item }))
    },

    async refreshPublishedNotices() {
      try {
        const res = await this.$axios.get('/api/notices')
        if (!this.isSuccess(res)) {
          this.publishedNoticeList = []
          this.noticePublishedAt = ''
          return
        }

        const notices = Array.isArray(res.data.data)
          ? res.data.data.map(item => this.normalizeNoticeItem(item)).filter(Boolean)
          : []

        this.publishedNoticeList = this.cloneNoticeList(notices)
        this.noticePublishedAt = notices[0]?.publishedAt || ''
      } catch (error) {
        console.error('读取通知失败:', error)
        this.publishedNoticeList = []
        this.noticePublishedAt = ''
      }
    },

    initializeNoticeCenter() {
      this.refreshPublishedNotices().then(() => {
        this.maybeOpenNoticePopup()
      })
    },

    maybeOpenNoticePopup() {
      if (!this.publishedNoticeList.length) {
        return
      }

      const latestVersion = this.publishedNoticeList[0]?.publishedAt || ''
      if (!latestVersion) {
        return
      }

      const seenVersion = sessionStorage.getItem('noticePopupVersion')
      if (seenVersion === latestVersion) {
        return
      }

      this.noticePopupSelectedIndex = 0
      this.noticePopupDialogVisible = true
      sessionStorage.setItem('noticePopupVersion', latestVersion)
    },

    selectNoticePopupItem(index) {
      this.noticePopupSelectedIndex = index
    },

    openNoticeFromPopup() {
      if (!this.noticePopupCurrent) {
        return
      }
      this.openNoticeDetail(this.noticePopupCurrent)
      this.noticePopupDialogVisible = false
    },

    closeNoticePopup() {
      this.noticePopupDialogVisible = false
    },

    async openNoticePublishDialog() {
      await this.refreshPublishedNotices()
      this.noticeDraftList = this.cloneNoticeList(this.publishedNoticeList)
      this.resetNoticeDraft()
      this.noticePublishDialogVisible = true
    },

    openNoticeDetail(item) {
      this.noticeDetail = {
        title: item.title || '',
        content: item.content || '',
        publishedAt: item.publishedAt || ''
      }
      this.noticeDetailDialogVisible = true
    },

    resetNoticeDraft() {
      this.noticeDraft = {
        id: null,
        title: '',
        content: ''
      }
      this.editingNoticeIndex = -1
    },

    saveNoticeDraftItem() {
      const notice = this.normalizeNoticeItem(this.noticeDraft)
      if (!notice) {
        this.$message.warning('请先填写完整的通知标题和内容')
        return
      }

      if (this.editingNoticeIndex > -1) {
        this.noticeDraftList.splice(this.editingNoticeIndex, 1, notice)
      } else {
        this.noticeDraftList.push(notice)
      }

      this.$message.success(this.editingNoticeIndex > -1 ? '通知已更新' : '通知已加入待发布列表')
      this.resetNoticeDraft()
    },

    editNoticeItem(index) {
      const target = this.noticeDraftList[index]
      if (!target) {
        return
      }

      this.noticeDraft = { ...target }
      this.editingNoticeIndex = index
    },

    removeNoticeItem(index) {
      this.noticeDraftList.splice(index, 1)

      if (this.editingNoticeIndex === index) {
        this.resetNoticeDraft()
      } else if (this.editingNoticeIndex > index) {
        this.editingNoticeIndex -= 1
      }
    },

    clearNoticeDraftList() {
      this.noticeDraftList = []
      this.resetNoticeDraft()
    },

    async publishNotices() {
      if (!this.noticeDraftList.length) {
        this.$message.warning('请先添加至少一条通知')
        return
      }

      const notices = this.noticeDraftList
        .map((item, index) => {
          const normalized = this.normalizeNoticeItem(item)
          return normalized ? { ...normalized, sortNo: index + 1 } : null
        })
        .filter(Boolean)

      try {
        const res = await this.$axios.post('/api/admin/notices/publish', { notices })
        if (!this.isSuccess(res)) {
          this.$message.error(res.data?.message || '通知发布失败')
          return
        }

        await this.refreshPublishedNotices()
        this.noticePublishDialogVisible = false
        this.$message.success('通知已发布')
      } catch (error) {
        console.error('发布通知失败:', error)
        this.$message.error(error.response?.data?.message || '通知发布失败')
      }
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
            this.$axios.get('/api/admin/stats', { params: this.getAdminStatsParams() })
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
          this.adminSalesRanking = adminStats.productSalesRanking || []
          this.adminOrderTrend = adminStats.orderTrendAnalysis || []
          return
        }

        this.adminSalesRanking = []
        this.adminOrderTrend = []
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
        this.adminSalesRanking = []
        this.adminOrderTrend = []
      }
    },

    async handleMenuSelect(key) {
      this.activeMenu = key

      switch (key) {
        case 'home':
          await this.loadStats()
          await this.refreshPublishedNotices()
          this.maybeOpenNoticePopup()
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
        case 'notice':
          // 通知公告页面，切换到首页显示通知列表
          this.activeMenu = 'home'
          await this.loadStats()
          await this.refreshPublishedNotices()
          this.maybeOpenNoticePopup()
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
      if (this.companyFilter !== 'all') {
        return this.getCompanyName(this.companyFilter) || this.companyFilter
      }
      return ''
    },

    async loadProducts() {
      this.productLoading = true
      try {
        const company = this.companyFilter !== 'all' ? this.getCompanyName(this.companyFilter) : ''
        const params = {
          page: this.productCurrentPage,
          size: this.productPageSize,
          category: this.activeCategory === 'all' ? '' : this.activeCategory,
          company: company
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

    // 产品类别管理
    async loadCategories() {
      try {
        // 从本地存储加载，如果没有则使用默认类别
        const storedCategories = localStorage.getItem('productCategories')
        if (storedCategories) {
          this.categoryList = JSON.parse(storedCategories)
        } else {
          // 默认类别列表
          this.categoryList = [
            { code: '1-3', name: '1-3类意外' },
            { code: '1-4', name: '1-4类意外' },
            { code: '1-5', name: '1-5类意外' },
            { code: '1-6', name: '1-6类意外' },
            { code: 'child', name: '少儿医疗' },
            { code: 'elder', name: '老年意外' },
            { code: 'travel', name: '旅游险' },
            { code: 'maternity', name: '驾乘险' }
          ]
          localStorage.setItem('productCategories', JSON.stringify(this.categoryList))
        }
      } catch (error) {
        console.error('加载产品类别失败:', error)
      }
    },

    openCategoryDialog() {
      this.categoryDialogVisible = true
    },

    addCategory() {
      if (!this.categoryForm.code || !this.categoryForm.name) {
        this.$message.warning('请填写类别编码和类别名称')
        return
      }

      // 检查编码是否重复
      if (this.categoryList.some(cat => cat.code === this.categoryForm.code)) {
        this.$message.warning('类别编码已存在')
        return
      }

      this.categoryList.push({
        code: this.categoryForm.code,
        name: this.categoryForm.name
      })

      // 保存到本地存储
      localStorage.setItem('productCategories', JSON.stringify(this.categoryList))

      this.$message.success('添加成功')
      this.resetCategoryForm()
    },

    deleteCategory(category) {
      this.$confirm(`确定删除类别"${category.name}"吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.categoryList = this.categoryList.filter(cat => cat.code !== category.code)
        localStorage.setItem('productCategories', JSON.stringify(this.categoryList))
        this.$message.success('删除成功')
      }).catch(() => {
        // 用户取消删除
      })
    },

    resetCategoryForm() {
      this.categoryForm = {
        code: '',
        name: ''
      }
    },

    // 承保公司管理
    async loadCompanies() {
      try {
        // 从本地存储加载，如果没有则使用默认公司列表
        const storedCompanies = localStorage.getItem('insuranceCompanies')
        if (storedCompanies) {
          this.companyList = JSON.parse(storedCompanies)
        } else {
          // 默认公司列表
          this.companyList = [
            { code: 'guoshou', name: '国寿财险' },
            { code: 'pingan', name: '平安财险' },
            { code: 'zhonghua', name: '中华联合' },
            { code: 'taiping', name: '太平财险' },
            { code: 'renbao', name: '人保财险' }
          ]
          localStorage.setItem('insuranceCompanies', JSON.stringify(this.companyList))
        }
      } catch (error) {
        console.error('加载承保公司失败:', error)
      }
    },

    openCompanyDialog() {
      this.companyDialogVisible = true
    },

    addCompany() {
      if (!this.companyForm.code || !this.companyForm.name) {
        this.$message.warning('请填写公司编码和公司名称')
        return
      }

      // 检查编码是否重复
      if (this.companyList.some(company => company.code === this.companyForm.code)) {
        this.$message.warning('公司编码已存在')
        return
      }

      this.companyList.push({
        code: this.companyForm.code,
        name: this.companyForm.name
      })

      // 保存到本地存储
      localStorage.setItem('insuranceCompanies', JSON.stringify(this.companyList))

      this.$message.success('添加成功')
      this.resetCompanyForm()
    },

    deleteCompany(company) {
      this.$confirm(`确定删除公司"${company.name}"吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.companyList = this.companyList.filter(comp => comp.code !== company.code)
        localStorage.setItem('insuranceCompanies', JSON.stringify(this.companyList))
        this.$message.success('删除成功')
      }).catch(() => {
        // 用户取消删除
      })
    },

    resetCompanyForm() {
      this.companyForm = {
        code: '',
        name: ''
      }
    },

    getCompanyName(companyCode) {
      if (!companyCode) return ''
      const company = this.companyList.find(comp => comp.code === companyCode)
      return company ? company.name : ''
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

    getDefaultProductForm() {
      return {
        id: null,
        productCode: '',
        productName: '',
        categoryCode: this.activeCategory !== 'all' ? this.activeCategory : (this.categoryList[0]?.code || '1-3'),
        companyCode: this.companyFilter !== 'all' ? this.companyFilter : '',
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
      }
    },

    openProductDialog(product = null) {
      if (product) {
        this.productDialogTitle = '编辑产品'
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
      } else {
        this.productDialogTitle = '新增产品'
        this.productForm = this.getDefaultProductForm()
      }
      this.productDialogVisible = true
    },

    async saveProduct() {
      if (!this.productForm.productCode || !this.productForm.productName) {
        this.$message.warning('请填写产品编码和产品名称')
        return
      }

      const isEdit = !!this.productForm.id
      const submitData = {
        id: this.productForm.id,
        productCode: this.productForm.productCode,
        productName: this.productForm.productName,
        categoryCode: this.productForm.categoryCode,
        companyCode: this.productForm.companyCode,
        companyName: this.getCompanyName(this.productForm.companyCode),
        description: this.productForm.description,
        features: this.productForm.features,
        price: this.productForm.price,
        stock: this.productForm.stock,
        isNew: this.productForm.isNew,
        isHot: this.productForm.isHot,
        saleStatus: this.productForm.saleStatus,
        sortNo: this.productForm.sortNo
      }

      this.productSubmitting = true
      try {
        if (isEdit) {
          await this.$axios.put('/api/admin/products', submitData)
        } else {
          delete submitData.id
          await this.$axios.post('/api/admin/products', submitData)
        }
        this.$message.success(isEdit ? '产品更新成功' : '产品新增成功')
        this.productDialogVisible = false
        this.loadProducts()
        this.loadStats()
      } catch (error) {
        console.error(isEdit ? '更新产品失败:' : '新增产品失败:', error)
        this.$message.error(error.response?.data?.message || (isEdit ? '更新产品失败' : '新增产品失败'))
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

    async deleteProduct(product) {
      try {
        await this.$confirm(`确定删除产品“${product.name}”吗？此操作不可恢复。`, '删除确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await this.$axios.delete(`/api/admin/products/${product.id}`)
        this.$message.success('产品删除成功')
        this.loadProducts()
        this.loadStats()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除产品失败:', error)
          this.$message.error(error.response?.data?.message || '删除产品失败')
        }
      }
    },

    openProductInsuranceBatchDialog(product) {
      if (!product || !product.id) {
        this.$message.warning('请选择需要批量激活的产品')
        return
      }
      if (product.saleStatus && product.saleStatus !== 'ON_SALE') {
        this.$message.warning('下架产品不可批量激活')
        return
      }
      if (!product.templateFileName) {
        this.$message.warning('该产品暂无模板文件，请先在编辑产品中上传模板文件')
        return
      }
      this.productInsuranceBatchProduct = { ...product }
      this.productInsuranceBatchDialogTitle = `批量激活 - ${product.name || '产品'}`
      this.productInsuranceBatchFile = null
      this.productInsuranceBatchPreviewCount = null
      this.productInsuranceBatchPreviewRows = []
      this.productInsuranceBatchPreviewLoading = false
      this.productInsuranceBatchConfirmDialogVisible = false
      this.productInsuranceBatchPreviewRequestId += 1
      this.productInsuranceBatchDialogVisible = true
      this.$nextTick(() => {
        this.$refs.productInsuranceBatchUploadRef?.clearFiles?.()
      })
    },

    closeProductInsuranceBatchDialog() {
      this.productInsuranceBatchDialogVisible = false
      this.productInsuranceBatchConfirmDialogVisible = false
      this.productInsuranceBatchFile = null
      this.productInsuranceBatchProduct = null
      this.productInsuranceBatchPreviewCount = null
      this.productInsuranceBatchPreviewRows = []
      this.productInsuranceBatchPreviewLoading = false
      this.productInsuranceBatchPreviewRequestId += 1
      this.$nextTick(() => {
        this.$refs.productInsuranceBatchUploadRef?.clearFiles?.()
      })
    },

    beforeProductInsuranceBatchUpload(file) {
      const isXlsx = /\.(xls|xlsx)$/i.test(file.name)
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isXlsx) {
        this.$message.error('只能上传 .xls 或 .xlsx 格式的Excel文件')
        return false
      }
      if (!isLt10M) {
        this.$message.error('文件大小不能超过 10MB')
        return false
      }
      return true
    },

    handleProductInsuranceBatchFileChange(uploadFile) {
      const rawFile = uploadFile?.raw || null
      if (!rawFile) {
        this.productInsuranceBatchFile = null
        this.productInsuranceBatchPreviewCount = null
        this.productInsuranceBatchPreviewRows = []
        return
      }
      if (!this.validateProductInsuranceBatchFile(rawFile)) {
        this.productInsuranceBatchFile = null
        this.productInsuranceBatchPreviewCount = null
        this.productInsuranceBatchPreviewRows = []
        this.$nextTick(() => {
          this.$refs.productInsuranceBatchUploadRef?.clearFiles?.()
        })
        return
      }
      this.productInsuranceBatchFile = rawFile
      this.productInsuranceBatchPreviewRows = []
      this.productInsuranceBatchConfirmDialogVisible = false
      this.previewProductInsuranceBatchCount(rawFile)
    },

    handleProductInsuranceBatchFileRemove() {
      this.productInsuranceBatchFile = null
      this.productInsuranceBatchPreviewCount = null
      this.productInsuranceBatchPreviewRows = []
      this.productInsuranceBatchPreviewLoading = false
      this.productInsuranceBatchConfirmDialogVisible = false
      this.productInsuranceBatchPreviewRequestId += 1
    },

    validateProductInsuranceBatchFile(file) {
      if (!file) {
        return false
      }
      const isExcel = /\.(xls|xlsx)$/i.test(file.name)
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isExcel) {
        this.$message.error('只能上传 .xls 或 .xlsx 格式的Excel文件')
        return false
      }
      if (!isLt10M) {
        this.$message.error('文件大小不能超过 10MB')
        return false
      }
      return true
    },

    async previewProductInsuranceBatchCount(file) {
      if (!this.productInsuranceBatchProduct?.id || !file) {
        this.productInsuranceBatchPreviewCount = null
        this.productInsuranceBatchPreviewRows = []
        return
      }

      const requestId = ++this.productInsuranceBatchPreviewRequestId
      this.productInsuranceBatchPreviewLoading = true
      this.productInsuranceBatchPreviewCount = null
      this.productInsuranceBatchPreviewRows = []

      try {
        const formData = new FormData()
        formData.append('file', file)
        const res = await this.$axios.post(
          `/api/anxinxuan/products/${this.productInsuranceBatchProduct.id}/insurance-template/preview`,
          formData,
          { headers: { 'Content-Type': 'multipart/form-data' } }
        )
        if (requestId !== this.productInsuranceBatchPreviewRequestId) {
          return
        }
        if (this.isSuccess(res)) {
          this.productInsuranceBatchPreviewCount = res.data.data?.previewCount ?? 0
          this.productInsuranceBatchPreviewRows = res.data.data?.previewRows || []
        } else {
          this.$message.error(res.data?.message || '预览导入条数失败')
        }
      } catch (error) {
        if (requestId !== this.productInsuranceBatchPreviewRequestId) {
          return
        }
        console.error('预览导入条数失败:', error)
        this.$message.error(error.response?.data?.message || '预览导入条数失败')
      } finally {
        if (requestId === this.productInsuranceBatchPreviewRequestId) {
          this.productInsuranceBatchPreviewLoading = false
        }
      }
    },

    openProductInsuranceBatchConfirmDialog() {
      if (!this.productInsuranceBatchFile) {
        this.$message.warning('请选择需要上传的Excel文件')
        return
      }
      if (this.productInsuranceBatchPreviewLoading) {
        this.$message.warning('正在预览文件，请稍后再操作')
        return
      }
      if (!this.productInsuranceBatchPreviewRows.length) {
        this.$message.warning('当前没有可确认的数据，请检查模板内容')
        return
      }
      this.productInsuranceBatchConfirmDialogVisible = true
    },

    closeProductInsuranceBatchConfirmDialog() {
      this.productInsuranceBatchConfirmDialogVisible = false
    },

    async submitProductInsuranceBatch() {
      if (!this.productInsuranceBatchProduct?.id) {
        this.$message.warning('请选择需要批量激活的产品')
        return
      }
      if (!this.productInsuranceBatchFile) {
        this.$message.warning('请选择需要上传的Excel文件')
        return
      }
      if (this.productInsuranceBatchPreviewLoading) {
        this.$message.warning('正在预览文件，请稍后再提交')
        return
      }
      if (!this.productInsuranceBatchPreviewRows.length) {
        this.$message.warning('请先查看确认页')
        return
      }

      this.productInsuranceBatchSubmitting = true
      try {
        const formData = new FormData()
        formData.append('file', this.productInsuranceBatchFile)
        const res = await this.$axios.post(
          `/api/anxinxuan/products/${this.productInsuranceBatchProduct.id}/insurance-template/import`,
          formData,
          { headers: { 'Content-Type': 'multipart/form-data' } }
        )
        if (this.isSuccess(res)) {
          const count = res.data.data?.importedCount ?? 0
          this.$message.success(res.data.data?.message || `已处理 ${count} 条投保信息`)
          this.closeProductInsuranceBatchDialog()
          this.loadExpenses()
          this.loadInsurances()
          this.loadStats()
        } else {
          this.$message.error(res.data?.message || '批量激活失败')
        }
      } catch (error) {
        console.error('批量激活失败:', error)
        this.$message.error(error.response?.data?.message || '批量激活失败，请检查模板后重试')
      } finally {
        this.productInsuranceBatchSubmitting = false
      }
    },

    async downloadProductTemplate(product) {
      if (!product?.id) {
        this.$message.warning('请选择需要下载模板的产品')
        return
      }
      if (!product.templateFileName) {
        this.$message.warning('该产品暂无可下载的模板文件')
        return
      }

      try {
        await this.downloadFile(
          `/api/images/template/${product.id}`,
          product.templateFileName
        )
        this.$message.success('模板文件下载已开始')
      } catch (error) {
        console.error('下载产品模板失败:', error)
        this.$message.error(error.response?.data?.message || '模板文件下载失败')
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
            this.$message.success('提交审核成功')
            this.activeDialogVisible = false
            this.loadProducts()
            this.loadStats()
            this.loadBalance()
            this.loadInsurances()
          } else {
            this.$message.error(res.data.message || '提交审核失败')
          }
        } catch (error) {
          console.error('提交审核失败:', error)
          this.$message.error(error.response?.data?.message || '提交审核失败，请稍后重试')
        } finally {
          this.activeSubmitting = false
        }
      })
    },

    async saveDraftForm() {
      this.$refs.activeFormRef.validate(async (valid) => {
        if (!valid) {
          return
        }
        this.draftSubmitting = true
        try {
          const res = await this.$axios.post('/api/anxinxuan/products/save-draft', this.activeForm)
          if (this.isSuccess(res)) {
            this.$message.success('已保存为待提交')
            this.activeDialogVisible = false
            this.loadInsurances()
          } else {
            this.$message.error(res.data.message || '保存草稿失败')
          }
        } catch (error) {
          console.error('保存草稿失败:', error)
          this.$message.error(error.response?.data?.message || '保存草稿失败，请稍后重试')
        } finally {
          this.draftSubmitting = false
        }
      })
    },

    resetExpenseFilter() {
      this.expenseFilter = {
        plan: 'all',
        status: 'all',
        serial: '',
        dateRange: []
      }
      this.expenseCurrentPage = 1
      this.loadExpenses()
    },

    async loadExpenses() {
      this.expenseLoading = true
      try {
        const [startDate, endDate] = Array.isArray(this.expenseFilter.dateRange) ? this.expenseFilter.dateRange : []
        const params = {
          page: this.expenseCurrentPage,
          size: this.expensePageSize,
          plan: this.expenseFilter.plan === 'all' ? '' : this.expenseFilter.plan,
          status: this.expenseFilter.status === 'all' ? '' : this.expenseFilter.status,
          serialNo: this.expenseFilter.serial,
          startDate,
          endDate
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
      this.expenseDetailRow = { ...row }
      this.expenseDetailDialogVisible = true
    },

    resetInsuranceFilter() {
      this.insuranceFilter = {
        plan: 'all',
        status: 'all',
        serial: '',
        dateRange: [],
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
        const [startDate, endDate] = Array.isArray(this.insuranceFilter.dateRange) ? this.insuranceFilter.dateRange : []
        const params = {
          page: this.insuranceCurrentPage,
          size: this.insurancePageSize,
          plan: this.insuranceFilter.plan === 'all' ? '' : this.insuranceFilter.plan,
          status: this.insuranceFilter.status === 'all' ? '' : this.insuranceFilter.status,
          serialNo: this.insuranceFilter.serial,
          startDate,
          endDate,
          insuredName: this.insuranceFilter.insuredName,
          insuredId: this.insuranceFilter.insuredId,
          beneficiaryName: this.insuranceFilter.beneficiaryName,
          beneficiaryId: this.insuranceFilter.beneficiaryId,
          agent: this.insuranceFilter.agent
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

    handleInsuranceSizeChange(size) {
      this.insurancePageSize = size
      this.insuranceCurrentPage = 1
      this.loadInsurances()
    },

    buildQueryParams(source) {
      const params = new URLSearchParams()
      Object.entries(source || {}).forEach(([key, value]) => {
        if (value === null || value === undefined || value === '' || value === 'all') {
          return
        }
        params.append(key, value)
      })
      return params.toString()
    },

    async downloadFile(url, fallbackFileName = 'download.bin') {
      try {
        const response = await this.$axios.get(url, { responseType: 'blob' })
        const blob = response.data instanceof Blob ? response.data : new Blob([response.data])
        const disposition = response.headers?.['content-disposition'] || response.headers?.['Content-Disposition'] || ''
        const fileName = this.resolveDownloadFileName(disposition, fallbackFileName)
        const objectUrl = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = objectUrl
        link.download = fileName
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.setTimeout(() => window.URL.revokeObjectURL(objectUrl), 1000)
      } catch (error) {
        console.error('文件下载失败:', error)
        throw error
      }
    },

    resolveDownloadFileName(disposition, fallbackFileName) {
      if (!disposition) {
        return fallbackFileName
      }

      const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
      if (utf8Match?.[1]) {
        try {
          return decodeURIComponent(utf8Match[1])
        } catch (error) {
          return utf8Match[1]
        }
      }

      const asciiMatch = disposition.match(/filename="?([^";]+)"?/i)
      if (asciiMatch?.[1]) {
        return asciiMatch[1]
      }

      return fallbackFileName
    },

    async downloadExpenseExport() {
      const [startDate, endDate] = Array.isArray(this.expenseFilter.dateRange) ? this.expenseFilter.dateRange : []
      const query = this.buildQueryParams({
        plan: this.expenseFilter.plan,
        status: this.expenseFilter.status,
        serialNo: this.expenseFilter.serial,
        startDate,
        endDate
      })

      try {
        await this.downloadFile(`/api/anxinxuan/expenses/export${query ? `?${query}` : ''}`, 'expense-export.xlsx')
        this.$message.success('费用清单导出已开始')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '费用清单导出失败')
      }
    },

    async downloadInsuranceExport() {
      const [startDate, endDate] = Array.isArray(this.insuranceFilter.dateRange) ? this.insuranceFilter.dateRange : []
      const query = this.buildQueryParams({
        plan: this.insuranceFilter.plan,
        status: this.insuranceFilter.status,
        serialNo: this.insuranceFilter.serial,
        startDate,
        endDate,
        insuredName: this.insuranceFilter.insuredName,
        insuredId: this.insuranceFilter.insuredId,
        beneficiaryName: this.insuranceFilter.beneficiaryName,
        beneficiaryId: this.insuranceFilter.beneficiaryId,
        agent: this.insuranceFilter.agent
      })

      try {
        await this.downloadFile(`/api/anxinxuan/insurances/export${query ? `?${query}` : ''}`, 'insurance-export.pdf')
        this.$message.success('保险清单导出已开始')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '保险清单导出失败')
      }
    },

    handleSelectionChange(selection) {
      this.selectedInsurances = selection
    },

    viewInsuranceDetail(row) {
      this.insuranceDetailRow = { ...row }
      this.insuranceTimeline = this.buildInsuranceTimeline(row)
      this.insuranceDetailDialogVisible = true
    },

    buildInsuranceTimeline(row) {
      const entries = [
        {
          key: 'draft',
          title: '投保资料创建',
          time: row.createTime,
          type: 'info',
          content: row.statusCode === 'DRAFT'
            ? '资料已暂存为待提交，用户仍可继续补充后再发起审核。'
            : '投保资料已创建，并进入后续业务流。'
        },
        {
          key: 'submit',
          title: '提交审核',
          time: row.submitTime || (row.statusCode !== 'DRAFT' ? row.createTime : ''),
          type: 'primary',
          content: '资料已提交给管理员审核，并进入内部处理队列。'
        },
        {
          key: 'review',
          title: row.statusCode === 'REVIEW_REJECTED' ? '审核驳回' : '审核完成',
          time: row.reviewTime,
          type: row.statusCode === 'REVIEW_REJECTED' ? 'danger' : 'success',
          content: row.statusCode === 'REVIEW_REJECTED'
            ? (row.rejectReason || row.reviewComment || '审核未通过，等待重新处理。')
            : (row.reviewComment || '管理员已审核通过，等待进入承保。')
        },
        {
          key: 'underwriting',
          title: '进入承保',
          time: row.underwritingTime,
          type: 'warning',
          content: '平台已推进到承保阶段，等待保险公司完成正式承保。'
        },
        {
          key: 'active',
          title: '保单生效',
          time: row.activateTime || row.startDate,
          type: 'success',
          content: row.policyNo
            ? `正式保单号已下发：${row.policyNo}`
            : '保单已生效，正式保单号待同步。'
        }
      ]

      return entries.filter(item => {
        if (item.key === 'draft') return !!row.createTime
        if (item.key === 'submit') return !!item.time && row.statusCode !== 'DRAFT'
        if (item.key === 'review') return !!item.time && ['APPROVED', 'REVIEW_REJECTED', 'UNDERWRITING', 'ACTIVE', 'EXPIRED', 'CANCELLED'].includes(row.statusCode)
        if (item.key === 'underwriting') return !!item.time && ['UNDERWRITING', 'ACTIVE', 'EXPIRED', 'CANCELLED'].includes(row.statusCode)
        if (item.key === 'active') return !!item.time && ['ACTIVE', 'EXPIRED'].includes(row.statusCode)
        return false
      })
    },

    formatPolicyTime(value) {
      if (!value) return '待更新'
      const normalized = typeof value === 'string' ? value.replace('T', ' ') : value
      const date = new Date(normalized)
      if (Number.isNaN(date.getTime())) return normalized
      return date.toLocaleString('zh-CN', { hour12: false })
    },

    openInsuranceActivationDialog(rows) {
      const pendingRows = (rows || []).filter(item => item.statusCode === 'UNDERWRITING')
      if (!pendingRows.length) {
        this.$message.warning('请选择承保中的保单')
        return
      }

      this.insuranceActivationDialogTitle = pendingRows.length > 1 ? `批量生效（${pendingRows.length} 条）` : '保单生效'
      this.insuranceActivationItems = pendingRows.map(item => ({
        id: item.id,
        product: item.product,
        beneficiaryName: item.beneficiaryName,
        policyNo: item.policyNo || '',
        startDate: item.startDate || '',
        endDate: item.endDate || ''
      }))
      this.insuranceActivationDialogVisible = true
    },

    activateInsuranceRecord(row) {
      this.openInsuranceActivationDialog([row])
    },

    batchActivateInsuranceRecords() {
      this.openInsuranceActivationDialog(this.selectedInsurances)
    },

    async submitInsuranceActivation() {
      if (!this.insuranceActivationItems.length) {
        this.$message.warning('暂无承保中的保单')
        return
      }

      for (const item of this.insuranceActivationItems) {
        if (!item.policyNo || !item.startDate || !item.endDate) {
          this.$message.warning('请完整填写保单号、起保日期和结束日期')
          return
        }
        if (item.endDate < item.startDate) {
          this.$message.warning('结束日期不能早于起保日期')
          return
        }
      }

      this.insuranceActivationSubmitting = true
      try {
        const items = this.insuranceActivationItems.map(item => ({
          insuranceId: item.id,
          policyNo: item.policyNo,
          effectiveDate: item.startDate,
          expiryDate: item.endDate
        }))
        const endpoint = items.length === 1
          ? `/api/admin/insurances/${items[0].insuranceId}/activate`
          : '/api/admin/insurances/activate-batch'
        const payload = items.length === 1
          ? {
              policyNo: items[0].policyNo,
              effectiveDate: items[0].effectiveDate,
              expiryDate: items[0].expiryDate
            }
          : { items }
        const method = items.length === 1 ? 'put' : 'post'
        const res = await this.$axios[method](endpoint, payload)
        if (this.isSuccess(res)) {
          this.$message.success(`已生效 ${items.length} 条保单，费用清单状态已同步`)
          this.insuranceActivationDialogVisible = false
          this.insuranceActivationItems = []
          this.selectedInsurances = []
          this.loadInsurances()
          this.loadExpenses()
          this.loadStats()
        } else {
          this.$message.error(res.data?.message || '设置生效失败')
        }
      } catch (error) {
        console.error('设置保险生效失败:', error)
        this.$message.error(error.response?.data?.message || '设置生效失败')
      } finally {
        this.insuranceActivationSubmitting = false
      }
    },

    async downloadPolicy(row) {
      if (!row || !row.id) {
        this.$message.warning('请选择需要导出的保险记录')
        return
      }
      try {
        await this.downloadFile(`/api/anxinxuan/insurances/${row.id}/pdf`, `insurance-${row.id}.pdf`)
        this.$message.success('已开始导出PDF')
      } catch (error) {
        this.$message.error(error.response?.data?.message || 'PDF导出失败')
      }
    },

    async submitInsuranceDraft(row) {
      try {
        await this.$confirm('确认提交该投保资料进入审核流程吗？提交后将扣减账户余额。', '提交审核', {
          confirmButtonText: '确认提交',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const res = await this.$axios.put(`/api/anxinxuan/insurances/${row.id}/submit`)
        if (this.isSuccess(res)) {
          this.$message.success('已提交审核')
          this.loadInsurances()
          this.loadExpenses()
          this.loadBalance()
          this.loadStats()
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('提交审核失败:', error)
          this.$message.error(error.response?.data?.message || '提交审核失败')
        }
      }
    },

    openInsuranceReviewDialog(row, action) {
      this.insuranceReviewAction = action
      this.insuranceReviewDialogTitle = action === 'approve' ? '审核通过' : '审核驳回'
      this.insuranceReviewForm = {
        insuranceId: row.id,
        reviewComment: row.reviewComment || '',
        rejectReason: ''
      }
      this.insuranceReviewDialogVisible = true
    },

    async submitInsuranceReview() {
      if (!this.insuranceReviewForm.reviewComment.trim()) {
        this.$message.warning('请输入审核意见')
        return
      }
      if (this.insuranceReviewAction === 'reject' && !this.insuranceReviewForm.rejectReason.trim()) {
        this.$message.warning('请输入驳回原因')
        return
      }

      this.insuranceReviewSubmitting = true
      try {
        const url = this.insuranceReviewAction === 'approve'
          ? `/api/admin/insurances/${this.insuranceReviewForm.insuranceId}/approve`
          : `/api/admin/insurances/${this.insuranceReviewForm.insuranceId}/reject`
        const payload = this.insuranceReviewAction === 'approve'
          ? { reviewComment: this.insuranceReviewForm.reviewComment }
          : {
              reviewComment: this.insuranceReviewForm.reviewComment,
              rejectReason: this.insuranceReviewForm.rejectReason
            }
        const res = await this.$axios.put(url, payload)
        if (this.isSuccess(res)) {
          this.$message.success(this.insuranceReviewAction === 'approve' ? '审核已通过' : '已驳回并退款')
          this.insuranceReviewDialogVisible = false
          this.loadInsurances()
          this.loadExpenses()
          this.loadRecharges()
          this.loadStats()
        } else {
          this.$message.error(res.data?.message || '审核操作失败')
        }
      } catch (error) {
        console.error('审核操作失败:', error)
        this.$message.error(error.response?.data?.message || '审核操作失败')
      } finally {
        this.insuranceReviewSubmitting = false
      }
    },

    async startInsuranceUnderwriting(row) {
      try {
        await this.$confirm('确认将该保单推进到承保中吗？', '进入承保', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const res = await this.$axios.put(`/api/admin/insurances/${row.id}/underwriting`)
        if (this.isSuccess(res)) {
          this.$message.success('已进入承保中')
          this.loadInsurances()
          this.loadExpenses()
          this.loadStats()
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('进入承保失败:', error)
          this.$message.error(error.response?.data?.message || '进入承保失败')
        }
      }
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
          mobile: user.mobile,
          password: '',
          confirmPassword: ''
        }
      } else {
        this.userForm = {
          id: null,
          username: '',
          mobile: '',
          password: '',
          confirmPassword: ''
        }
      }
      this.userDialogVisible = true
    },

    async submitUserForm() {
      this.$refs.userFormRef.validate(async (valid) => {
        if (!valid) return
        try {
          const isEdit = !!this.userForm.id

          if (!isEdit && !this.userForm.password) {
            this.$message.error('请输入密码')
            return
          }

          if (this.userForm.password && this.userForm.password.length < 6) {
            this.$message.error('密码长度不能少于6位')
            return
          }

          if ((!isEdit || this.userForm.password) && this.userForm.password !== this.userForm.confirmPassword) {
            this.$message.error('两次输入的密码不一致')
            return
          }

          const submitData = { ...this.userForm }
          delete submitData.confirmPassword
          if (submitData.password === '') {
            delete submitData.password
          }

          let res
          if (isEdit) {
            res = await this.$axios.put('/api/admin/users', submitData)
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
        userId: null,
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
          const res = await this.$axios.post('/api/admin/recharges', this.rechargeForm)
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
        有效: 'success',
        已完成: 'success',
        已生效: 'success',
        待提交: 'info',
        待审核: 'warning',
        审核通过: 'success',
        审核驳回: 'danger',
        承保中: 'warning',
        待处理: 'warning',
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
          sessionStorage.removeItem('authToken')
          sessionStorage.removeItem('isLoggedIn')
          sessionStorage.removeItem('userId')
          sessionStorage.removeItem('userType')
          sessionStorage.removeItem('username')
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

    // 图片上传前验证
    beforeImageUpload(file) {
      if (!this.productForm.id) {
        this.$message.warning('请先保存产品后再上传图片')
        return false
      }

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
      if (!this.productForm.id) {
        this.$message.warning('请先保存产品后再上传模板文件')
        return false
      }

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
    async downloadTemplate() {
      if (!this.productForm.id) {
        this.$message.warning('请先保存产品后再下载模板文件')
        return
      }

      try {
        await this.downloadFile(
          `/api/images/product/${this.productForm.id}/template`,
          this.productForm.templateFileName || 'template'
        )
        this.$message.success('模板文件下载已开始')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '模板文件下载失败')
      }
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
  align-items: flex-start;
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
  min-width: 0;
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

.notice-board-card {
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  padding: 24px;
  background: linear-gradient(180deg, #fff7f7 0%, #ffffff 100%);
  box-shadow: 0 8px 28px rgba(230, 0, 18, 0.06);
}

.notice-board-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.notice-board-meta h3 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #1f2937;
}

.notice-board-meta p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #6b7280;
}

.notice-board-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.notice-admin-layout {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(320px, 1.2fr);
  gap: 20px;
}

.notice-editor-panel,
.notice-list-panel {
  background: #fff;
  border: 1px solid #f3f4f6;
  border-radius: 14px;
  padding: 20px;
}

.notice-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.notice-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.notice-list-actions,
.filter-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-list-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px;
  background: #fafafa;
  border: 1px solid #f3f4f6;
  border-radius: 12px;
}

.notice-list-order {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff1f0;
  color: #e60012;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notice-list-content {
  flex: 1;
  min-width: 0;
}

.notice-list-content h4 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #1f2937;
}

.notice-list-content p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #6b7280;
  white-space: pre-line;
}

.notice-list-item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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

.product-company-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.product-company-group .el-radio-button {
  margin: 0;
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
  flex-direction: column;
  gap: 12px;
  padding: 14px 0 0;
  border-top: 1px solid #f0f0f0;
  margin-top: auto;
}

.product-stats-row {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
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
  flex-wrap: wrap;
}

.product-actions .el-button {
  padding: 6px 12px;
}

.product-actions .el-button {
  flex: 0 0 auto;
}

.product-batch-dialog {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.product-batch-summary {
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(230, 0, 18, 0.08), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(230, 0, 18, 0.12);
}

.product-batch-name {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 6px;
}

.product-batch-subtitle {
  font-size: 13px;
  line-height: 1.6;
  color: #6b7280;
}

.product-batch-upload {
  width: 100%;
}

.product-batch-upload .el-upload {
  width: 100%;
}

.product-batch-upload .el-upload-dragger {
  width: 100%;
  border-radius: 16px;
  border: 1px dashed rgba(230, 0, 18, 0.28);
  background: #fffdfd;
}

.product-batch-preview-tip {
  font-size: 13px;
  line-height: 1.6;
  color: #1f2937;
  padding: 0 4px;
}

.product-batch-preview-tip--muted {
  color: #6b7280;
}

.product-batch-confirm {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-batch-preview-table {
  width: 100%;
}

@media screen and (max-width: 1200px) {
  .main-content {
    flex-direction: column;
    align-items: stretch;
  }

  .sidebar,
  .content {
    width: 100%;
  }

  .sidebar {
    position: static;
    top: auto;
  }

  .product-filter .filter-header,
  .product-filter .filter-row {
    align-items: flex-start;
  }
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

.insurance-activation-tip {
  margin-bottom: 16px;
  padding: 12px 14px;
  background: #fff7e6;
  border: 1px solid #ffe7ba;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: #8c6218;
}

.notice-publish-dialog .el-dialog {
  border-radius: 20px;
  overflow: hidden;
}

.notice-publish-dialog .el-dialog__body {
  padding-top: 8px;
}

.notice-popup-dialog .el-dialog {
  border-radius: 22px;
  overflow: hidden;
}

.notice-popup-dialog .el-dialog__body {
  padding-top: 12px;
}

.notice-popup-shell {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 18px;
}

.notice-popup-list,
.notice-popup-detail {
  border-radius: 18px;
  border: 1px solid #eef1f6;
  background: #fff;
}

.notice-popup-list {
  padding: 16px;
  max-height: 60vh;
  overflow: auto;
}

.notice-popup-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.notice-popup-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-popup-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 12px;
  border: 1px solid #eef1f6;
  border-radius: 14px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
  appearance: none;
  font: inherit;
  color: inherit;
}

.notice-popup-item:hover,
.notice-popup-item.active {
  border-color: #e60012;
  box-shadow: 0 8px 20px rgba(230, 0, 18, 0.08);
  transform: translateY(-1px);
}

.notice-popup-index {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: #fff1f0;
  color: #e60012;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notice-popup-item-body {
  min-width: 0;
  flex: 1;
}

.notice-popup-item-body h4 {
  margin-bottom: 6px;
  font-size: 15px;
  color: #111827;
  line-height: 1.4;
}

.notice-popup-item-body p {
  font-size: 12px;
  line-height: 1.6;
  color: #6b7280;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-popup-detail {
  padding: 22px;
  background: linear-gradient(180deg, #fff7f7 0%, #ffffff 100%);
}

.notice-popup-eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fff1f0;
  color: #e60012;
  font-size: 12px;
  font-weight: 700;
}

.notice-popup-detail h3 {
  margin: 14px 0 12px;
  font-size: 24px;
  line-height: 1.35;
  color: #111827;
}

.notice-popup-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 18px;
}

.notice-popup-content {
  min-height: 240px;
  padding: 18px 20px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid #eef1f6;
  color: #1f2937;
  font-size: 15px;
  line-height: 1.9;
  white-space: pre-wrap;
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
    top: auto;
    padding: 12px;
  }

  .sidebar-section {
    display: block;
  }

  .sidebar-section .el-menu {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }

  .sidebar-section .el-menu-item {
    height: 36px;
    line-height: 36px;
    font-size: 13px;
    margin-bottom: 0;
    padding: 0 12px !important;
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

  .notice-admin-layout {
    grid-template-columns: 1fr;
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

  .product-stats-row {
    gap: 12px;
  }

  .product-actions {
    width: 100%;
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

  .content {
    width: 100%;
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
  .product-card {
    flex-direction: column;
  }

  .product-image-wrapper {
    width: 100%;
    height: 180px;
  }

  .product-footer {
    flex-wrap: wrap;
    gap: 12px;
  }

  .product-stats-row {
    gap: 12px;
  }

  .price-info, .stock-info {
    flex: 1;
    min-width: calc(50% - 6px);
  }

  .product-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
    gap: 6px;
  }

  .product-actions .el-button {
    flex: 1;
    min-width: 80px;
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

  .notice-board-card {
    padding: 20px 16px;
  }

  .notice-board-header {
    flex-direction: column;
  }

  .filter-header,
  .filter-header-actions,
  .notice-list-header,
  .notice-list-actions,
  .notice-board-actions {
    flex-wrap: wrap;
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

  .filter-row .el-radio-button {
    margin-bottom: 4px;
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

  .notice-list-item {
    flex-direction: column;
  }

  .notice-list-item-actions {
    width: 100%;
    justify-content: flex-end;
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

  .product-image-wrapper {
    height: 140px;
  }

  .product-footer {
    gap: 8px;
  }

  .price-info, .stock-info {
    min-width: 100%;
  }

  .product-actions .el-button {
    min-width: 70px;
    padding: 4px 8px;
    font-size: 12px;
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

.admin-analysis-toolbar {
  margin-bottom: 16px;
}

.admin-analysis-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.admin-analysis-filter-meta h3 {
  margin: 0 0 6px;
  font-size: 18px;
  color: #303133;
}

.admin-analysis-filter-meta p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.admin-analysis-filter-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.admin-analysis-grid {
  display: grid;
  gap: 20px;
  margin-bottom: 24px;
}

.admin-analysis-stack {
  grid-template-columns: 1fr;
}

.admin-analysis-card {
  padding: 20px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.admin-analysis-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.admin-analysis-header h3 {
  margin: 0 0 6px;
  font-size: 18px;
  color: #303133;
}

.admin-analysis-header p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #909399;
}

.detail-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-right: 6px;
}

.detail-dialog-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: #f8fafc;
}

.detail-dialog-eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.detail-dialog-title {
  margin: 0;
  font-size: 22px;
  line-height: 1.4;
  color: #303133;
}

.detail-dialog-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #606266;
  word-break: break-all;
}

.detail-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-summary-card {
  padding: 16px 18px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fff;
}

.detail-summary-card .label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.detail-summary-card strong {
  font-size: 15px;
  color: #303133;
  word-break: break-word;
}

.detail-section-block {
  padding: 18px 20px;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: #fff;
}

.detail-section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-section-header h4 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.detail-section-header span {
  font-size: 12px;
  color: #909399;
}

.policy-timeline {
  padding-top: 6px;
}

.policy-timeline-card {
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fcfcfd;
}

.policy-timeline-title {
  margin-bottom: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.policy-timeline-content {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}

@media screen and (max-width: 768px) {
  .admin-analysis-filter,
  .admin-analysis-grid,
  .detail-summary-grid {
    grid-template-columns: 1fr;
  }

  .admin-analysis-filter {
    flex-direction: column;
    align-items: stretch;
  }
}

/* 产品类别管理样式 */
.category-management {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.category-form {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.category-list {
  max-height: 400px;
  overflow-y: auto;
}

/* 承保公司管理样式 */
.company-management {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.company-form {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.company-list {
  max-height: 400px;
  overflow-y: auto;
}
</style>
