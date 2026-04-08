import { defineAsyncComponent } from 'vue'

import AppHeader from '@/components/layout/AppHeader.vue'
import HomeOverviewSection from '@/components/home/HomeOverviewSection.vue'
import { buildAdminAnalysisRange as buildAdminAnalysisRangeUtil, formatAdminDate as formatAdminDateUtil } from '@/utils/home/date.js'
import { cloneNoticeList as cloneNoticeListUtil, createNoticeId as createNoticeIdUtil, normalizeNoticeItem as normalizeNoticeItemUtil } from '@/utils/home/notice.js'
import { buildQueryParams as buildQueryParamsUtil, resolveDownloadFileName as resolveDownloadFileNameUtil } from '@/utils/home/download.js'
import { buildInsuranceTimeline as buildInsuranceTimelineUtil, formatPolicyTime as formatPolicyTimeUtil } from '@/utils/home/insurance.js'
import { getProductImage as getProductImageUtil } from '@/utils/home/product.js'
import { validateProductImageFile as validateProductImageFileUtil, validateProductInsuranceBatchFile as validateProductInsuranceBatchFileUtil, validateProductTemplateFile as validateProductTemplateFileUtil } from '@/utils/home/upload.js'

const ProductCenterSection = defineAsyncComponent(() => import('@/components/home/ProductCenterSection.vue'))
const ExpenseSection = defineAsyncComponent(() => import('@/components/home/ExpenseSection.vue'))
const InsuranceSection = defineAsyncComponent(() => import('@/components/home/InsuranceSection.vue'))
const RechargeSection = defineAsyncComponent(() => import('@/components/home/RechargeSection.vue'))
const UserAdminSection = defineAsyncComponent(() => import('@/components/home/UserAdminSection.vue'))
const HomeDialogs = defineAsyncComponent(() => import('@/components/home/dialogs/HomeDialogs.vue'))

export default {
  name: 'Home',
  components: {
    AppHeader,
    HomeOverviewSection,
    ProductCenterSection,
    ExpenseSection,
    InsuranceSection,
    RechargeSection,
    UserAdminSection,
    HomeDialogs
  },
  provide() {
    return {
      homeDialogContext: this
    }
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
      companyFilter: 'all',
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
      companyList: [],
      dialogRefs: {}
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

    getDialogRef(refName) {
      return this.dialogRefs?.[refName] || this.$refs?.[refName] || null
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
      return buildAdminAnalysisRangeUtil(periodType)
    },

    formatAdminDate(date) {
      return formatAdminDateUtil(date)
    },

    applyAdminAnalysisPreset(periodType) {
      this.adminAnalysisPeriodType = periodType
      this.adminAnalysisRange = this.buildAdminAnalysisRange(periodType)
      this.loadStats()
    },

    createNoticeId() {
      return createNoticeIdUtil()
    },

    normalizeNoticeItem(item) {
      return normalizeNoticeItemUtil(item, createNoticeIdUtil)
    },

    cloneNoticeList(list = []) {
      return cloneNoticeListUtil(list)
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
      this.companyFilter = value
      this.productCurrentPage = 1
      this.loadProducts()
    },

    resetProductFilter() {
      this.companyFilter = 'all'
      this.activeCategory = 'all'
      this.productCurrentPage = 1
      this.loadProducts()
    },

    normalizeProductFilters() {
      let shouldReload = false

      if (this.activeCategory !== 'all' && !this.categoryList.some(cat => cat.code === this.activeCategory)) {
        this.activeCategory = 'all'
        shouldReload = true
      }

      if (this.companyFilter !== 'all' && !this.companyList.some(comp => comp.code === this.companyFilter)) {
        this.companyFilter = 'all'
        shouldReload = true
      }

      if (shouldReload) {
        this.productCurrentPage = 1
        this.loadProducts()
      }
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
        this.normalizeProductFilters()
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
        this.normalizeProductFilters()
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
        this.getDialogRef('productInsuranceBatchUploadRef')?.clearFiles?.()
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
        this.getDialogRef('productInsuranceBatchUploadRef')?.clearFiles?.()
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
          this.getDialogRef('productInsuranceBatchUploadRef')?.clearFiles?.()
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
      const result = validateProductInsuranceBatchFileUtil(file)
      if (!result.valid) {
        this.$message.error(result.message)
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
      this.getDialogRef('activeFormRef')?.validate(async (valid) => {
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
      this.getDialogRef('activeFormRef')?.validate(async (valid) => {
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
      return buildQueryParamsUtil(source)
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
      return resolveDownloadFileNameUtil(disposition, fallbackFileName)
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
      return buildInsuranceTimelineUtil(row)
    },

    formatPolicyTime(value) {
      return formatPolicyTimeUtil(value)
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
      this.getDialogRef('userFormRef')?.validate(async (valid) => {
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
      this.getDialogRef('rechargeFormRef')?.validate(async (valid) => {
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
      return getProductImageUtil(product)
    },

    // 预览图片
    previewImage(product) {
      this.previewImageUrl = this.getProductImage(product)
      this.previewImageTitle = product.name
      this.previewImageDialogVisible = true
    },

    // 图片上传前验证
    beforeImageUpload(file) {
      const result = validateProductImageFileUtil(file, !!this.productForm.id)
      if (!result.valid) {
        this.$message.error(result.message)
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
      const result = validateProductTemplateFileUtil(file, !!this.productForm.id)
      if (!result.valid) {
        this.$message.error(result.message)
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
