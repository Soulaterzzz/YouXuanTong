<template>
  <!-- 产品编辑弹窗 -->
  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose" v-model="productDialogVisible" :title="productDialogTitle" width="680px" :close-on-click-modal="false" destroy-on-close>
    <div class="product-edit-form">
      <!-- 基本信息区 -->
      <div class="form-section">
        <div class="form-section-title">
          <el-icon><InfoFilled /></el-icon>
          基本信息
        </div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="产品名称" required>
              <el-input v-model="productForm.productName" placeholder="请输入产品名称" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品编码">
              <el-input v-model="productForm.productCode" placeholder="请输入产品编码" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="产品别名">
              <el-input v-model="productForm.alias" placeholder="仅管理员可见的产品别名" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="产品类别" required>
              <el-select v-model="productForm.categoryCode" style="width: 100%" placeholder="请选择产品类别" clearable>
                <el-option v-for="category in categoryList" :key="category.code" :label="category.name" :value="category.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="承保公司" required>
              <el-select v-model="productForm.companyCode" style="width: 100%" placeholder="请选择承保公司" clearable>
                <el-option v-for="company in companyList" :key="company.code" :label="company.name" :value="company.code" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="职业表">
              <div class="profession-upload-block">
                <el-upload
                  class="profession-table-uploader"
                  :action="`/api/images/profession/${productForm.companyCode}`"
                  :disabled="!productForm.companyCode"
                  :show-file-list="false"
                  :before-upload="beforeProfessionUpload"
                  :on-success="handleProfessionUploadSuccess"
                  :on-error="handleProfessionUploadError"
                  accept=".xls,.xlsx"
                  :headers="uploadHeaders"
                  method="post"
                >
                  <el-button type="success" plain :disabled="!productForm.companyCode">
                    <el-icon><Upload /></el-icon>
                    上传职业表
                  </el-button>
                </el-upload>
                <div class="upload-tip">
                  {{ productForm.companyCode
                    ? `将保存到 /uploads/profession/${productForm.companyCode}.xls 或 .xlsx`
                    : '请先选择承保公司后再上传职业表' }}
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="价格" required>
              <el-input-number v-model="productForm.price" :min="0.01" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="显示价格">
              <el-input-number v-model="productForm.displayPrice" :min="0.01" :precision="2" style="width: 100%" placeholder="对客户展示的价格" />
              <div style="margin-top: 2px; color: #909399; font-size: 12px;">留空则使用价格</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="productForm.sortNo" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="销售状态">
              <el-select v-model="productForm.saleStatus" style="width: 100%">
                <el-option label="上架" value="ON_SALE" />
                <el-option label="下架" value="OFF_SALE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <!-- 描述信息区 -->
      <div class="form-section">
        <div class="form-section-title">
          <el-icon><Document /></el-icon>
          描述信息
        </div>
        <el-form-item label="产品描述">
          <el-input v-model="productForm.description" type="textarea" :rows="2" placeholder="请输入产品描述" />
        </el-form-item>
        <el-form-item label="产品特点">
          <el-input v-model="productForm.features" type="textarea" :rows="2" placeholder="请输入产品特点，如：保额高、保障全、理赔快" />
        </el-form-item>
        <el-form-item label="详情正文">
          <el-input
            v-model="productForm.detailText"
            type="textarea"
            :rows="12"
            placeholder="请输入产品独立详情正文，支持换行。建议按“承保公司 / 客服电话 / 重点提示 / 保障内容 / 方案说明”分段。"
          />
          <div style="margin-top: 2px; color: #909399; font-size: 12px;">该内容会显示在独立详情页中，支持复制截图里的长文案样式。</div>
        </el-form-item>
      </div>

      <!-- 图片与模板区 -->
      <div class="form-section">
        <div class="form-section-title">
          <el-icon><Picture /></el-icon>
          图片与模板
        </div>
        <div class="upload-row">
          <div class="upload-item">
            <div class="upload-label">产品图片</div>
            <el-upload
              class="product-image-uploader"
              :action="`/api/images/product/${productForm.id}/upload`"
              :disabled="!productForm.id"
              :show-file-list="false"
              :on-success="handleImageUploadSuccess"
              :on-error="handleImageUploadError"
              :before-upload="beforeImageUpload"
              accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
              :headers="uploadHeaders"
            >
              <div v-if="productForm.imageUrl" class="image-preview-wrapper">
                <img :src="productForm.imageUrl" class="product-image-preview" />
                <div class="image-overlay">
                  <el-icon><ZoomIn /></el-icon>
                  <span>点击更换</span>
                </div>
              </div>
              <div v-else class="image-placeholder">
                <el-icon class="placeholder-icon"><Plus /></el-icon>
                <span>{{ productForm.id ? '上传图片' : '保存后上传' }}</span>
              </div>
            </el-upload>
            <div class="upload-actions" v-if="productForm.id && productForm.imageUrl">
              <el-button size="small" type="danger" @click="deleteProductImage">删除图片</el-button>
            </div>
          </div>
          <div class="upload-item">
            <div class="upload-label">模板文件</div>
            <el-upload
              class="template-uploader"
              :action="`/api/images/product/${productForm.id}/template`"
              :disabled="!productForm.id"
              :show-file-list="false"
              :on-success="handleTemplateUploadSuccess"
              :on-error="handleTemplateUploadError"
              :before-upload="beforeTemplateUpload"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.txt"
              :headers="uploadHeaders"
              method="post"
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon>
                {{ productForm.templateFileName ? '更换模板' : '上传模板' }}
              </el-button>
            </el-upload>
            <div v-if="productForm.templateFileName" class="template-file-info">
              <el-icon><Document /></el-icon>
              <span class="template-file-name">{{ productForm.templateFileName }}</span>
              <el-button size="small" type="primary" link @click="downloadTemplate">下载</el-button>
              <el-button size="small" type="danger" link @click="deleteTemplate">删除</el-button>
            </div>
            <div class="upload-tip">
              支持 PDF、Word、Excel、TXT
            </div>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="productDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="productSubmitting" @click="saveProduct">
          <el-icon><Check /></el-icon>
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 批量激活上传弹窗 -->
  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose"
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

  <!-- 批量激活确认弹窗 -->
  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose"
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
        <el-table-column type="index" label="序号" width="80" :index="index => index + 1" />
        <el-table-column prop="planName" label="方案名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="beneficiaryName" label="被保人姓名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="beneficiaryId" label="证件号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="beneficiaryJob" label="职业" min-width="120" show-overflow-tooltip />
        <el-table-column prop="count" label="份数" width="80" />
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
  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose" v-model="categoryDialogVisible" title="产品类别管理" width="700px" :close-on-click-modal="false">
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
  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose" v-model="companyDialogVisible" title="承保公司管理" width="700px" :close-on-click-modal="false">
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

  <!-- 产品激活弹窗 -->
  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose" v-model="activeDialogVisible" title="产品激活" width="550px" :close-on-click-modal="false">
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
            <span class="label">原始单价：</span>
            <span class="value price">¥{{ Number(selectedProduct.price || 0).toFixed(2) }}</span>
            <span class="label" style="margin-left: 12px;">显示价格：</span>
            <el-input-number
              v-model="activeForm.displayPrice"
              :min="0.01"
              :precision="2"
              :step="0.01"
              controls-position="right"
              style="width: 110px;"
              size="small"
            />
            <el-tooltip content="打印 PDF 凭证时将使用此价格" placement="top">
              <el-icon style="margin-left: 4px; color: #909399; cursor: help;"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
        </el-col>
      </el-row>
    </div>
    <el-form :model="activeForm" :rules="activeFormRules" ref="activeFormRef" label-width="100px">
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
          <span class="amount">¥{{ getActiveTotalAmount().toFixed(2) }}</span>
          <span style="color: #909399; font-size: 12px; margin-left: 4px;">（按原始单价 ¥{{ Number(selectedProduct.price || 0).toFixed(2) }} × {{ activeForm.count || 1 }} 份）</span>
        </div>
        <div class="summary-row" v-if="selectedProduct">
          <span>余额不足：</span>
          <span :class="['remain', balance >= getActiveTotalAmount() ? 'enough' : 'not-enough']">
            {{ balance >= getActiveTotalAmount() ? '否' : '是，请先充值' }}
          </span>
        </div>
      </div>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="activeDialogVisible = false">取消</el-button>
        <el-button :loading="draftSubmitting" @click="saveDraftForm">保存待提交</el-button>
        <el-button type="primary" :loading="activeSubmitting" :disabled="selectedProduct && balance < getActiveTotalAmount()" @click="submitActiveForm">提交审核</el-button>
      </span>
    </template>
  </el-dialog>

  <!-- 图片预览弹窗 -->
  <el-dialog
    ref="dialogRef"
    @open="handleDialogOpen"
    @closed="handleDialogClose"
    v-model="previewImageDialogVisible"
    :title="previewImageTitle"
    width="800px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="image-preview-container">
      <img :src="previewImageUrl" :alt="previewImageTitle" class="preview-image">
    </div>
  </el-dialog>
</template>

<script>
import {
  Document,
  Delete,
  Plus,
  Upload,
  InfoFilled,
  Picture,
  ZoomIn,
  Check
} from '@element-plus/icons-vue'
import { useHomeDialogBridge } from '@/composables/home/useHomeDialogBridge.js'
import { computed } from 'vue'

export default {
  name: 'ProductDialogs',
  components: {
    Document,
    Upload,
    Plus,
    Delete,
    InfoFilled,
    Picture,
    ZoomIn,
    Check
  },
  setup() {
    const { bridge } = useHomeDialogBridge(['productInsuranceBatchUploadRef', 'activeFormRef'])
    bridge.uploadHeaders = computed(() => {
      const token = sessionStorage.getItem('authToken')
      return token ? { Authorization: `Bearer ${token}` } : {}
    })
    return bridge
  }
}
</script>
