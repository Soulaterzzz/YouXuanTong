<template>
  <div class="app-container">
    <a href="#main-content" class="skip-link">跳转到主要内容</a>

    <AppHeader
      :active-menu="activeMenu"
      :is-admin="isAdmin"
      :current-user="currentUser"
      :balance="balance"
      @select="handleMenuSelect"
      @logout="handleUserCommand('logout')"
    />

    <main id="main-content" class="main-content">
      <ProductCenterSection
        v-if="activeMenu === 'product'"
        :is-admin="isAdmin"
        :active-category="activeCategory"
        :category-list="categoryList"
        :company-filter="companyFilter"
        :company-list="companyList"
        :filter-text="getFilterText()"
        :products="products"
        :product-loading="productLoading"
        :product-total="productTotal"
        :product-page-size="productPageSize"
        :product-current-page="productCurrentPage"
        @category-select="handleCategorySelect"
        @company-change="handleCompanyChange"
        @reset-filter="resetProductFilter"
        @open-category-dialog="openCategoryDialog"
        @open-company-dialog="openCompanyDialog"
        @add-product="openProductDialog()"
        @preview-image="previewImage"
        @open-product-dialog="openProductDialog"
        @toggle-product-status="toggleProductStatus"
        @delete-product="deleteProduct"
        @open-batch-dialog="openProductInsuranceBatchDialog"
        @download-template="downloadProductTemplate"
        @open-activate-dialog="openActivateDialog"
        @page-change="handleProductPageChange"
        @size-change="handleProductSizeChange"
      />

      <section v-else class="content">
        <HomeOverviewSection
          v-if="activeMenu === 'home'"
          :is-admin="isAdmin"
          :stats="stats"
          :admin-analysis-period-type="adminAnalysisPeriodType"
          :admin-analysis-range-text="adminAnalysisRangeText"
          :admin-order-trend-range-text="adminOrderTrendRangeText"
          :admin-order-trend-mode-label="adminOrderTrendModeLabel"
          :admin-sales-ranking="adminSalesRanking"
          :admin-order-trend="adminOrderTrend"
          :published-notice-time-text="publishedNoticeTimeText"
          :published-notice-list="publishedNoticeList"
          @goto-product="activeMenu = 'product'"
          @goto-insurance="activeMenu = 'insurance'"
          @apply-admin-analysis-preset="applyAdminAnalysisPreset"
          @open-notice-publish="openNoticePublishDialog"
          @open-notice-detail="openNoticeDetail"
        />

        <ExpenseSection
          v-else-if="activeMenu === 'expense'"
          :is-admin="isAdmin"
          :expense-filter="expenseFilter"
          :expense-list="expenseList"
          :expense-loading="expenseLoading"
          :expense-total="expenseTotal"
          :expense-page-size="expensePageSize"
          @reset-filter="resetExpenseFilter"
          @download-export="downloadExpenseExport"
          @query="queryExpenses"
          @page-change="handleExpensePageChange"
          @view-detail="viewExpenseDetail"
        />

        <InsuranceSection
          v-else-if="activeMenu === 'insurance'"
          :is-admin="isAdmin"
          :insurance-filter="insuranceFilter"
          :insurance-list="insuranceList"
          :insurance-loading="insuranceLoading"
          :insurance-total="insuranceTotal"
          :insurance-page-size="insurancePageSize"
          :selected-insurances="selectedInsurances"
          @reset-filter="resetInsuranceFilter"
          @download-export="downloadInsuranceExport"
          @query="queryInsurances"
          @selection-change="handleSelectionChange"
          @batch-activate="openInsuranceActivationDialog"
          @view-detail="viewInsuranceDetail"
          @download-policy="downloadPolicy"
          @submit-draft="submitInsuranceDraft"
          @approve="openInsuranceReviewDialog($event, 'approve')"
          @reject="openInsuranceReviewDialog($event, 'reject')"
          @underwriting="startInsuranceUnderwriting"
          @activate="activateInsuranceRecord"
          @page-change="handleInsurancePageChange"
          @size-change="handleInsuranceSizeChange"
        />

        <RechargeSection
          v-else-if="activeMenu === 'recharge'"
          :is-admin="isAdmin"
          :recharge-filter="rechargeFilter"
          :recharge-list="rechargeList"
          :recharge-loading="rechargeLoading"
          :recharge-total="rechargeTotal"
          @reset-filter="resetRechargeFilter"
          @query="queryRecharges"
          @open-recharge-dialog="openRechargeDialog"
          @page-change="handleRechargePageChange"
        />

        <UserAdminSection
          v-else-if="activeMenu === 'userAdmin'"
          :user-query="userQuery"
          :user-list="userList"
          :user-loading="userLoading"
          :user-total="userTotal"
          :user-page-size="userPageSize"
          :user-current-page="userCurrentPage"
          @reset-filter="resetUserQuery"
          @open-user-dialog="openUserDialog"
          @query="loadUsers"
          @toggle-user-status="toggleUserStatus"
          @delete-user="deleteUser"
          @page-change="handleUserPageChange"
        />
      </section>
    </main>

    <HomeDialogs />
  </div>
</template>

<script src="./home/home-controller.js"></script>
<style src="./home/home.css"></style>
