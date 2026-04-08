<template>
  <!-- 产品编辑弹窗 -->
  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose" v-model="productDialogVisible" :title="productDialogTitle" width="620px" :close-on-click-modal="false">
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
  Upload
} from '@element-plus/icons-vue'
import { useHomeDialogBridge } from '@/composables/home/useHomeDialogBridge.js'

export default {
  name: 'ProductDialogs',
  components: {
    Document,
    Upload,
    Plus,
    Delete
  },
  setup() {
    const { bridge } = useHomeDialogBridge(['productInsuranceBatchUploadRef', 'activeFormRef'])
    return bridge
  }
}
</script>
