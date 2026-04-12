package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.enums.InsuranceStatus;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.common.export.SimpleXlsxReader;
import com.zs.ytbx.common.export.SimpleXlsxWriter;
import com.zs.ytbx.common.util.PasswordUtils;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.entity.*;
import com.zs.ytbx.mapper.*;
import com.zs.ytbx.service.AdminService;
import com.zs.ytbx.vo.anxinxuan.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final AxxUserMapper axxUserMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final AxxProductMapper axxProductMapper;
    private final ExpenseRecordMapper expenseRecordMapper;
    private final InsuranceRecordMapper insuranceRecordMapper;
    private final TransactionRecordMapper transactionRecordMapper;

    @Override
    public PageResponse<UserVO> listUsers(UserQuery query) {
        LambdaQueryWrapper<AxxUserEntity> wrapper = new LambdaQueryWrapper<>();
        
        if (query.getUsername() != null && !query.getUsername().isEmpty()) {
            wrapper.like(AxxUserEntity::getUsername, query.getUsername());
        }
        if (query.getMobile() != null && !query.getMobile().isEmpty()) {
            wrapper.eq(AxxUserEntity::getMobile, query.getMobile());
        }
        if (query.getUserType() != null && !query.getUserType().isEmpty()) {
            wrapper.eq(AxxUserEntity::getUserType, query.getUserType());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(AxxUserEntity::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(AxxUserEntity::getCreateTime);
        
        IPage<AxxUserEntity> page = axxUserMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()), wrapper);
        
        List<UserVO> records = page.getRecords().stream()
            .map(this::convertToUserVO)
            .collect(Collectors.toList());
        
        return PageResponse.<UserVO>builder()
                .records(records)
                .pageNo(query.getPage())
                .pageSize(query.getSize())
                .total(page.getTotal())
                .totalPages(page.getPages())
                .build();
    }

    @Override
    public UserVO getUser(Long userId) {
        AxxUserEntity user = axxUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(RegisterRequest request) {
        LambdaQueryWrapper<AxxUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AxxUserEntity::getUsername, request.getUsername())
               .eq(AxxUserEntity::getDeleted, 0);
        
        if (axxUserMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        AxxUserEntity user = new AxxUserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtils.encode(request.getPassword()));
        user.setMobile(request.getMobile());
        user.setUserType("USER");
        user.setStatus("ACTIVE");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        axxUserMapper.insert(user);
        
        AccountBalanceEntity balance = new AccountBalanceEntity();
        balance.setUserId(user.getId());
        balance.setBalance(BigDecimal.ZERO);
        balance.setFrozenBalance(BigDecimal.ZERO);
        balance.setCreateTime(LocalDateTime.now());
        balance.setUpdateTime(LocalDateTime.now());
        accountBalanceMapper.insert(balance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UpdateUserRequest request) {
        AxxUserEntity user = axxUserMapper.selectById(request.getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(PasswordUtils.encode(request.getPassword()));
        }
        if (request.getMobile() != null) {
            user.setMobile(request.getMobile());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        
        user.setUpdateTime(LocalDateTime.now());
        axxUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        AxxUserEntity user = axxUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if ("ADMIN".equals(user.getUserType())) {
            throw new RuntimeException("不能删除管理员账号");
        }
        user.setDeleted(1);
        user.setUpdateTime(LocalDateTime.now());
        axxUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long userId) {
        AxxUserEntity user = axxUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus("ACTIVE");
        user.setUpdateTime(LocalDateTime.now());
        axxUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId) {
        AxxUserEntity user = axxUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if ("ADMIN".equals(user.getUserType())) {
            throw new RuntimeException("不能禁用管理员账号");
        }
        user.setStatus("DISABLED");
        user.setUpdateTime(LocalDateTime.now());
        axxUserMapper.updateById(user);
    }

    @Override
    public PageResponse<ProductVO> listProducts(ProductQuery query) {
        LambdaQueryWrapper<AxxProductEntity> wrapper = new LambdaQueryWrapper<>();

        if (query.getCategory() != null && !query.getCategory().isEmpty() && !"all".equals(query.getCategory())) {
            wrapper.eq(AxxProductEntity::getCategoryCode, query.getCategory());
        }

        if (query.getCompany() != null && !query.getCompany().isEmpty() && !"all".equals(query.getCompany())) {
            wrapper.like(AxxProductEntity::getCompanyName, query.getCompany());
        }

        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(AxxProductEntity::getProductName, query.getKeyword().trim());
        }

        wrapper.orderByAsc(AxxProductEntity::getSortNo);
        
        IPage<AxxProductEntity> page = axxProductMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()), wrapper);
        
        List<ProductVO> records = page.getRecords().stream()
            .map(this::convertToProductVO)
            .collect(Collectors.toList());
        
        return PageResponse.<ProductVO>builder()
                .records(records)
                .pageNo(query.getPage())
                .pageSize(query.getSize())
                .total(page.getTotal())
                .totalPages(page.getPages())
                .build();
    }

    @Override
    public ProductDetailVO getProduct(Long productId) {
        AxxProductEntity entity = axxProductMapper.selectById(productId);
        if (entity == null) {
            return null;
        }
        return convertToProductDetailVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProduct(ProductRequest request) {
        long exists = axxProductMapper.selectCount(new LambdaQueryWrapper<AxxProductEntity>()
                .eq(AxxProductEntity::getProductCode, request.getProductCode()));

        if (exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "产品编码已存在");
        }

        AxxProductEntity product = new AxxProductEntity();
        product.setProductCode(request.getProductCode());
        product.setProductName(request.getProductName());
        product.setCategoryCode(request.getCategoryCode());
        product.setCompanyName(request.getCompanyName());
        product.setDescription(request.getDescription());
        product.setFeatures(request.getFeatures());
        product.setDetailText(request.getDetailText());
        product.setPrice(request.getPrice());
        product.setDisplayPrice(request.getDisplayPrice());
        product.setIsNew(request.getIsNew() != null ? request.getIsNew() : 0);
        product.setIsHot(request.getIsHot() != null ? request.getIsHot() : 0);
        product.setSaleStatus(request.getSaleStatus() != null ? request.getSaleStatus() : "ON_SALE");
        product.setSortNo(request.getSortNo() != null ? request.getSortNo() : 0);
        product.setAlias(request.getAlias() == null ? null : request.getAlias().trim());
        product.setDeleted(0);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        if (axxProductMapper.insert(product) != 1) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "新增产品失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductRequest request) {
        AxxProductEntity product = axxProductMapper.selectById(request.getId());
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "产品不存在");
        }

        if (request.getProductCode() != null && !request.getProductCode().equals(product.getProductCode())) {
            long exists = axxProductMapper.selectCount(new LambdaQueryWrapper<AxxProductEntity>()
                    .eq(AxxProductEntity::getProductCode, request.getProductCode()));
            if (exists > 0) {
                throw new BusinessException(ResultCode.CONFLICT, "产品编码已存在");
            }
        }
        
        if (request.getProductCode() != null) {
            product.setProductCode(request.getProductCode());
        }
        if (request.getProductName() != null) {
            product.setProductName(request.getProductName());
        }
        if (request.getCategoryCode() != null) {
            product.setCategoryCode(request.getCategoryCode());
        }
        if (request.getCompanyName() != null) {
            product.setCompanyName(request.getCompanyName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getFeatures() != null) {
            product.setFeatures(request.getFeatures());
        }
        if (request.getDetailText() != null) {
            product.setDetailText(request.getDetailText());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getDisplayPrice() != null) {
            product.setDisplayPrice(request.getDisplayPrice());
        }
        if (request.getIsNew() != null) {
            product.setIsNew(request.getIsNew());
        }
        if (request.getIsHot() != null) {
            product.setIsHot(request.getIsHot());
        }
        if (request.getSaleStatus() != null) {
            product.setSaleStatus(request.getSaleStatus());
        }
        if (request.getSortNo() != null) {
            product.setSortNo(request.getSortNo());
        }
        if (request.getAlias() != null) {
            product.setAlias(request.getAlias().trim());
        }
        
        product.setUpdateTime(LocalDateTime.now());
        if (axxProductMapper.updateById(product) != 1) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "更新产品失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        AxxProductEntity product = axxProductMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "产品不存在");
        }

        if (axxProductMapper.deleteById(productId) != 1) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "删除产品失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onSale(Long productId) {
        AxxProductEntity product = axxProductMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("产品不存在");
        }
        product.setSaleStatus("ON_SALE");
        product.setUpdateTime(LocalDateTime.now());
        axxProductMapper.updateById(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offSale(Long productId) {
        AxxProductEntity product = axxProductMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("产品不存在");
        }
        product.setSaleStatus("OFF_SALE");
        product.setUpdateTime(LocalDateTime.now());
        axxProductMapper.updateById(product);
    }

    @Override
    public PageResponse<ExpenseVO> listAllExpenses(ExpenseQuery query) {
        LambdaQueryWrapper<ExpenseRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(ExpenseRecordEntity::getExpenseStatus, InsuranceStatus.DRAFT.getCode());
        applyExpenseFilters(wrapper, query);
        
        wrapper.orderByDesc(ExpenseRecordEntity::getCreateTime);
        
        IPage<ExpenseRecordEntity> page = expenseRecordMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()), wrapper);
        
        List<ExpenseVO> records = page.getRecords().stream()
            .map(this::convertToExpenseVO)
            .collect(Collectors.toList());
        
        return PageResponse.<ExpenseVO>builder()
                .records(records)
                .pageNo(query.getPage())
                .pageSize(query.getSize())
                .total(page.getTotal())
                .totalPages(page.getPages())
                .build();
    }

    @Override
    public PageResponse<InsuranceVO> listAllInsurances(InsuranceQuery query) {
        LambdaQueryWrapper<InsuranceRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(InsuranceRecordEntity::getInsuranceStatus, InsuranceStatus.DRAFT.getCode());
        applyInsuranceFilters(wrapper, query);

        wrapper.orderByDesc(InsuranceRecordEntity::getCreateTime);
        
        IPage<InsuranceRecordEntity> page = insuranceRecordMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()), wrapper);
        
        List<InsuranceVO> records = page.getRecords().stream()
            .map(this::convertToInsuranceVO)
            .collect(Collectors.toList());
        
        return PageResponse.<InsuranceVO>builder()
                .records(records)
                .pageNo(query.getPage())
                .pageSize(query.getSize())
                .total(page.getTotal())
                .totalPages(page.getPages())
                .build();
    }

    @Override
    public byte[] downloadProductImportTemplate() {
        List<String> headers = productImportHeaders();
        List<List<String>> rows = List.of(List.of(
                "P-20240401-001",
                "示例产品",
                "1-3",
                "示例承保公司",
                "这里填写产品描述",
                "这里填写产品特点",
                "99.00",
                "1",
                "0",
                "ON_SALE",
                "1"
        ));
        return SimpleXlsxWriter.write("产品导入模板", headers, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importProducts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "导入文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "导入文件不能为空");
        }
        String lowerFilename = filename.toLowerCase();
        if (!lowerFilename.endsWith(".xls") && !lowerFilename.endsWith(".xlsx")) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "仅支持导入 .xls 或 .xlsx 文件");
        }

        List<List<String>> rows;
        try {
            rows = SimpleXlsxReader.readFirstSheet(file.getInputStream());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "Excel文件解析失败：" + e.getMessage());
        }

        if (rows.size() <= 1) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "Excel中没有可导入的数据");
        }

        List<String> headers = rows.get(0);
        validateProductImportHeaders(headers);

        int importedCount = 0;
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }
            ProductRequest request = buildProductRequestFromRow(row, i + 1);
            createProduct(request);
            importedCount++;
        }
        return importedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveInsurance(Long insuranceId, InsuranceApproveRequest request, Long reviewerId, String reviewerName) {
        InsuranceRecordEntity insurance = requireInsurance(insuranceId);
        ensureTransition(insurance, InsuranceStatus.PENDING_REVIEW, InsuranceStatus.APPROVED);

        insurance.setInsuranceStatus(InsuranceStatus.APPROVED.getCode());
        insurance.setReviewComment(request.getReviewComment().trim());
        insurance.setReviewerId(reviewerId);
        insurance.setReviewerName(reviewerName);
        insurance.setReviewTime(LocalDateTime.now());
        insurance.setRejectReason(null);
        insurance.setUnderwritingTime(null);
        insurance.setActivateTime(null);
        insurance.setUpdateTime(LocalDateTime.now());
        insuranceRecordMapper.updateById(insurance);

        syncExpenseStatus(insurance.getExpenseId(), InsuranceStatus.APPROVED, insurance.getPolicyNo(), insurance.getEffectiveDate(), insurance.getExpiryDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectInsurance(Long insuranceId, InsuranceRejectRequest request, Long reviewerId, String reviewerName) {
        InsuranceRecordEntity insurance = requireInsurance(insuranceId);
        ensureTransition(insurance, InsuranceStatus.PENDING_REVIEW, InsuranceStatus.REVIEW_REJECTED);

        insurance.setInsuranceStatus(InsuranceStatus.REVIEW_REJECTED.getCode());
        insurance.setReviewComment(request.getReviewComment().trim());
        insurance.setReviewerId(reviewerId);
        insurance.setReviewerName(reviewerName);
        insurance.setReviewTime(LocalDateTime.now());
        insurance.setRejectReason(request.getRejectReason().trim());
        insurance.setUnderwritingTime(null);
        insurance.setActivateTime(null);
        insurance.setPolicyNo(null);
        insurance.setEffectiveDate(null);
        insurance.setExpiryDate(null);
        insurance.setUpdateTime(LocalDateTime.now());
        insuranceRecordMapper.updateById(insurance);

        syncExpenseStatus(insurance.getExpenseId(), InsuranceStatus.REVIEW_REJECTED, null, null, null);
        refundRejectedInsurance(insurance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchApproveInsurances(BatchInsuranceRequest request, Long reviewerId, String reviewerName) {
        for (Long insuranceId : request.getInsuranceIds()) {
            try {
                InsuranceApproveRequest approveRequest = new InsuranceApproveRequest();
                approveRequest.setReviewComment(request.getReviewComment() != null ? request.getReviewComment() : "批量审核通过");
                approveInsurance(insuranceId, approveRequest, reviewerId, reviewerName);
            } catch (Exception e) {
                log.error("批量审核失败 insuranceId={}", insuranceId, e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRejectInsurances(BatchInsuranceRequest request, Long reviewerId, String reviewerName) {
        for (Long insuranceId : request.getInsuranceIds()) {
            try {
                InsuranceRejectRequest rejectRequest = new InsuranceRejectRequest();
                rejectRequest.setReviewComment(request.getReviewComment() != null ? request.getReviewComment() : "批量审核拒绝");
                rejectRequest.setRejectReason(request.getRejectReason() != null ? request.getRejectReason() : "批量审核拒绝");
                rejectInsurance(insuranceId, rejectRequest, reviewerId, reviewerName);
            } catch (Exception e) {
                log.error("批量审核失败 insuranceId={}", insuranceId, e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startUnderwriting(Long insuranceId) {
        InsuranceRecordEntity insurance = requireInsurance(insuranceId);
        ensureTransition(insurance, InsuranceStatus.APPROVED, InsuranceStatus.UNDERWRITING);

        insurance.setInsuranceStatus(InsuranceStatus.UNDERWRITING.getCode());
        insurance.setUnderwritingTime(LocalDateTime.now());
        insurance.setActivateTime(null);
        insurance.setUpdateTime(LocalDateTime.now());
        insuranceRecordMapper.updateById(insurance);

        syncExpenseStatus(insurance.getExpenseId(), InsuranceStatus.UNDERWRITING, insurance.getPolicyNo(), insurance.getEffectiveDate(), insurance.getExpiryDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateInsurance(Long insuranceId, ActivateInsuranceRequest request) {
        activateInsuranceInternal(insuranceId, request.getPolicyNo(), request.getEffectiveDate(), request.getExpiryDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateInsurances(List<BatchActivateInsuranceRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("保单列表不能为空");
        }

        for (BatchActivateInsuranceRequest.Item item : items) {
            activateInsuranceInternal(item.getInsuranceId(), item.getPolicyNo(), item.getEffectiveDate(), item.getExpiryDate());
        }
    }

    private void activateInsuranceInternal(Long insuranceId, String policyNo, LocalDate effectiveDate, LocalDate expiryDate) {
        InsuranceRecordEntity insurance = requireInsurance(insuranceId);
        ensureTransition(insurance, InsuranceStatus.UNDERWRITING, InsuranceStatus.ACTIVE);

        if (policyNo == null || policyNo.trim().isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "保单号不能为空");
        }

        if (effectiveDate == null || expiryDate == null) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "起保日期和结束日期不能为空");
        }

        if (expiryDate.isBefore(effectiveDate)) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "结束日期不能早于起保日期");
        }

        insurance.setInsuranceStatus(InsuranceStatus.ACTIVE.getCode());
        insurance.setPolicyNo(policyNo.trim());
        insurance.setEffectiveDate(effectiveDate);
        insurance.setExpiryDate(expiryDate);
        insurance.setActivateTime(LocalDateTime.now());
        insurance.setUpdateTime(LocalDateTime.now());
        insuranceRecordMapper.updateById(insurance);

        syncExpenseStatus(insurance.getExpenseId(), InsuranceStatus.ACTIVE, policyNo.trim(), effectiveDate, expiryDate);
    }

    private InsuranceRecordEntity requireInsurance(Long insuranceId) {
        InsuranceRecordEntity insurance = insuranceRecordMapper.selectById(insuranceId);
        if (insurance == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "保险记录不存在");
        }
        return insurance;
    }

    private void ensureTransition(InsuranceRecordEntity insurance, InsuranceStatus expectedStatus, InsuranceStatus targetStatus) {
        InsuranceStatus currentStatus = InsuranceStatus.fromCode(insurance.getInsuranceStatus());
        if (currentStatus != expectedStatus) {
            throw new BusinessException(ResultCode.INVALID_PARAM,
                    "当前状态为“" + currentStatus.getLabel() + "”，不能执行“" + targetStatus.getLabel() + "”操作");
        }
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BusinessException(ResultCode.INVALID_PARAM,
                    "状态“" + currentStatus.getLabel() + "”不能流转到“" + targetStatus.getLabel() + "”");
        }
    }

    private void syncExpenseStatus(Long expenseId,
                                   InsuranceStatus status,
                                   String policyNo,
                                   LocalDate effectiveDate,
                                   LocalDate expiryDate) {
        if (expenseId == null) {
            return;
        }

        ExpenseRecordEntity expense = expenseRecordMapper.selectById(expenseId);
        if (expense == null) {
            return;
        }

        expense.setExpenseStatus(status.getCode());
        expense.setPolicyNo(policyNo);
        expense.setEffectiveDate(effectiveDate);
        expense.setExpiryDate(expiryDate);
        expense.setUpdateTime(LocalDateTime.now());
        expenseRecordMapper.updateById(expense);
    }

    private void refundRejectedInsurance(InsuranceRecordEntity insurance) {
        if (insurance.getExpenseId() == null) {
            return;
        }

        ExpenseRecordEntity expense = expenseRecordMapper.selectById(insurance.getExpenseId());
        if (expense == null || expense.getTotalAmount() == null) {
            return;
        }

        AccountBalanceEntity balance = accountBalanceMapper.selectOne(
                new LambdaQueryWrapper<AccountBalanceEntity>()
                        .eq(AccountBalanceEntity::getUserId, insurance.getUserId()));
        if (balance == null) {
            balance = new AccountBalanceEntity();
            balance.setUserId(insurance.getUserId());
            balance.setBalance(BigDecimal.ZERO);
            balance.setFrozenBalance(BigDecimal.ZERO);
            balance.setCreateTime(LocalDateTime.now());
            balance.setUpdateTime(LocalDateTime.now());
            accountBalanceMapper.insert(balance);
        }

        BigDecimal balanceBefore = balance.getBalance();
        BigDecimal refundAmount = expense.getTotalAmount();
        balance.setBalance(balanceBefore.add(refundAmount));
        balance.setUpdateTime(LocalDateTime.now());
        accountBalanceMapper.updateById(balance);

        TransactionRecordEntity transaction = new TransactionRecordEntity();
        transaction.setSerialNo(generateSerialNo("RFD"));
        transaction.setUserId(insurance.getUserId());
        transaction.setTransType("RECHARGE");
        transaction.setAmount(refundAmount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balance.getBalance());
        transaction.setDescription("投保审核驳回退款：" + insurance.getProductName());
        transaction.setRefType("EXPENSE");
        transaction.setRefId(expense.getId());
        transaction.setPaymentMethod("BALANCE");
        transaction.setPaymentStatus("SUCCESS");
        transaction.setCreateTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());
        transactionRecordMapper.insert(transaction);
    }

    private String generateSerialNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private UserVO convertToUserVO(AxxUserEntity entity) {
        AccountBalanceEntity balance = accountBalanceMapper.selectOne(
            new LambdaQueryWrapper<AccountBalanceEntity>()
                .eq(AccountBalanceEntity::getUserId, entity.getId()));
        
        return UserVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .mobile(entity.getMobile())
                .userType(entity.getUserType())
                .status(entity.getStatus())
                .balance(balance != null ? balance.getBalance() : BigDecimal.ZERO)
                .lastLoginTime(entity.getLastLoginTime())
                .createTime(entity.getCreateTime())
                .build();
    }

    private ProductVO convertToProductVO(AxxProductEntity entity) {
        return ProductVO.builder()
                .id(entity.getId())
                .productCode(entity.getProductCode())
                .name(entity.getProductName())
                .description(entity.getDescription())
                .features(entity.getFeatures())
                .detailText(entity.getDetailText())
                .price(entity.getPrice())
                .displayPrice(entity.getDisplayPrice())
                .isNew(entity.getIsNew() == 1)
                .isHot(entity.getIsHot() == 1)
                .categoryCode(entity.getCategoryCode())
                .companyName(entity.getCompanyName())
                .saleStatus(entity.getSaleStatus())
                .sortNo(entity.getSortNo())
                .imageUrl(entity.getImageUrl())
                .templateFileName(entity.getTemplateFileName())
                .alias(entity.getAlias())
                .build();
    }

    private ProductDetailVO convertToProductDetailVO(AxxProductEntity entity) {
        return ProductDetailVO.builder()
                .id(entity.getId())
                .productCode(entity.getProductCode())
                .name(entity.getProductName())
                .description(entity.getDescription())
                .features(entity.getFeatures())
                .detailText(entity.getDetailText())
                .price(entity.getPrice())
                .displayPrice(entity.getDisplayPrice())
                .isNew(entity.getIsNew() == 1)
                .categoryCode(entity.getCategoryCode())
                .companyName(entity.getCompanyName())
                .alias(entity.getAlias())
                .build();
    }

    private ExpenseVO convertToExpenseVO(ExpenseRecordEntity entity) {
        return ExpenseVO.builder()
                .id(entity.getId())
                .serial(entity.getSerialNo())
                .contact(entity.getContactName())
                .product(entity.getProductName())
                .createTime(entity.getCreateTime().toString())
                .exportTime(entity.getExportTime() != null ? entity.getExportTime().toString() : "")
                .status(mapStatus(entity.getExpenseStatus()))
                .policyNo(entity.getPolicyNo())
                .startDate(entity.getEffectiveDate() != null ? entity.getEffectiveDate().toString() : "")
                .endDate(entity.getExpiryDate() != null ? entity.getExpiryDate().toString() : "")
                .count(entity.getQuantity())
                .price(entity.getPremiumAmount())
                .displayPrice(entity.getDisplayPrice())
                .total(entity.getTotalAmount())
                .build();
    }

    private InsuranceVO convertToInsuranceVO(InsuranceRecordEntity entity) {
        String statusCode = InsuranceStatus.normalizeCode(entity.getInsuranceStatus());
        return InsuranceVO.builder()
                .id(entity.getId())
                .statusCode(statusCode)
                .product(entity.getProductName())
                .insuredName(entity.getInsuredName())
                .insuredId(entity.getInsuredIdNo())
                .beneficiaryName(entity.getBeneficiaryName())
                .beneficiaryId(entity.getBeneficiaryIdNo())
                .createTime(entity.getCreateTime().toString())
                .exportTime(entity.getExportTime() != null ? entity.getExportTime().toString() : "")
                .status(mapStatus(entity.getInsuranceStatus()))
                .agent(entity.getAgentName())
                .policyNo(entity.getPolicyNo())
                .startDate(entity.getEffectiveDate() != null ? entity.getEffectiveDate().toString() : "")
                .endDate(entity.getExpiryDate() != null ? entity.getExpiryDate().toString() : "")
                .reviewerName(entity.getReviewerName())
                .reviewTime(entity.getReviewTime() != null ? entity.getReviewTime().toString() : "")
                .reviewComment(entity.getReviewComment())
                .rejectReason(entity.getRejectReason())
                .submitTime(entity.getSubmitTime() != null ? entity.getSubmitTime().toString() : "")
                .underwritingTime(entity.getUnderwritingTime() != null ? entity.getUnderwritingTime().toString() : "")
                .activateTime(entity.getActivateTime() != null ? entity.getActivateTime().toString() : "")
                .count(entity.getQuantity())
                .premiumAmount(entity.getPremiumAmount())
                .displayPrice(entity.getDisplayPrice())
                .build();
    }

    private void applyExpenseStatusFilter(LambdaQueryWrapper<ExpenseRecordEntity> wrapper, String status) {
        String normalizedStatus = InsuranceStatus.normalizeCode(status);
        if (InsuranceStatus.ACTIVE.getCode().equals(normalizedStatus)) {
            wrapper.in(ExpenseRecordEntity::getExpenseStatus, InsuranceStatus.ACTIVE.getCode(), "COMPLETED");
            return;
        }
        if ("PENDING".equalsIgnoreCase(status)) {
            wrapper.in(ExpenseRecordEntity::getExpenseStatus,
                    InsuranceStatus.PENDING_REVIEW.getCode(),
                    InsuranceStatus.APPROVED.getCode(),
                    InsuranceStatus.UNDERWRITING.getCode());
            return;
        }
        wrapper.eq(ExpenseRecordEntity::getExpenseStatus, normalizedStatus);
    }

    private void applyInsuranceStatusFilter(LambdaQueryWrapper<InsuranceRecordEntity> wrapper, String status) {
        String normalizedStatus = InsuranceStatus.normalizeCode(status);
        if ("PENDING".equalsIgnoreCase(status)) {
            wrapper.in(InsuranceRecordEntity::getInsuranceStatus,
                    InsuranceStatus.PENDING_REVIEW.getCode(),
                    InsuranceStatus.APPROVED.getCode(),
                    InsuranceStatus.UNDERWRITING.getCode(),
                    "PENDING");
            return;
        }
        wrapper.eq(InsuranceRecordEntity::getInsuranceStatus, normalizedStatus);
    }

    private String mapStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        try {
            return InsuranceStatus.fromCode(status).getLabel();
        } catch (IllegalArgumentException ex) {
            return status;
        }
    }

    @Override
    public PageResponse<RechargeVO> listAllRecharges(RechargeQuery query) {
        LambdaQueryWrapper<TransactionRecordEntity> wrapper = new LambdaQueryWrapper<>();
        
        if (query.getUsername() != null && !query.getUsername().isEmpty()) {
            // 通过用户名查询用户ID
            AxxUserEntity user = axxUserMapper.selectOne(
                new LambdaQueryWrapper<AxxUserEntity>()
                    .eq(AxxUserEntity::getUsername, query.getUsername()));
            if (user != null) {
                wrapper.eq(TransactionRecordEntity::getUserId, user.getId());
            }
        }

        if (query.getType() != null && !query.getType().isEmpty()) {
            wrapper.eq(TransactionRecordEntity::getTransType, query.getType().toUpperCase());
        }

        if (query.getDescription() != null && !query.getDescription().isEmpty()) {
            wrapper.like(TransactionRecordEntity::getDescription, query.getDescription());
        }
        
        wrapper.orderByDesc(TransactionRecordEntity::getCreateTime);
        
        IPage<TransactionRecordEntity> page = transactionRecordMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()), wrapper);
        
        List<RechargeVO> records = page.getRecords().stream()
            .map(this::convertToRechargeVO)
            .collect(Collectors.toList());
        
        return PageResponse.<RechargeVO>builder()
                .records(records)
                .pageNo(query.getPage())
                .pageSize(query.getSize())
                .total(page.getTotal())
                .totalPages(page.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rechargeUser(Long userId, BigDecimal amount, String method, String remark) {
        AccountBalanceEntity balance = accountBalanceMapper.selectOne(
            new LambdaQueryWrapper<AccountBalanceEntity>()
                .eq(AccountBalanceEntity::getUserId, userId));
        
        if (balance == null) {
            balance = new AccountBalanceEntity();
            balance.setUserId(userId);
            balance.setBalance(BigDecimal.ZERO);
            balance.setFrozenBalance(BigDecimal.ZERO);
            balance.setCreateTime(LocalDateTime.now());
            balance.setUpdateTime(LocalDateTime.now());
            accountBalanceMapper.insert(balance);
        }

        BigDecimal balanceBefore = balance.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        balance.setBalance(balanceAfter);
        balance.setUpdateTime(LocalDateTime.now());
        accountBalanceMapper.updateById(balance);

        TransactionRecordEntity transaction = new TransactionRecordEntity();
        transaction.setSerialNo(generateSerialNo("RCH"));
        transaction.setUserId(userId);
        transaction.setTransType("RECHARGE");
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(remark != null ? remark : "管理员充值");
        transaction.setPaymentMethod(method);
        transaction.setPaymentStatus("SUCCESS");
        transaction.setCreateTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());
        transactionRecordMapper.insert(transaction);
    }

    @Override
    public Long getTodayNewOrders() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        return expenseRecordMapper.selectCount(
            new LambdaQueryWrapper<ExpenseRecordEntity>()
                .ne(ExpenseRecordEntity::getExpenseStatus, InsuranceStatus.DRAFT.getCode())
                .ge(ExpenseRecordEntity::getCreateTime, todayStart)
        );
    }

    @Override
    public Long getPendingOrders() {
        return expenseRecordMapper.selectCount(
            new LambdaQueryWrapper<ExpenseRecordEntity>()
                .in(ExpenseRecordEntity::getExpenseStatus,
                        InsuranceStatus.PENDING_REVIEW.getCode(),
                        InsuranceStatus.APPROVED.getCode(),
                        InsuranceStatus.UNDERWRITING.getCode())
        );
    }

    @Override
    public Long getMonthOrders() {
        LocalDate now = LocalDate.now();
        LocalDateTime monthStart = LocalDateTime.of(now.with(TemporalAdjusters.firstDayOfMonth()), LocalTime.MIN);
        return expenseRecordMapper.selectCount(
            new LambdaQueryWrapper<ExpenseRecordEntity>()
                .ne(ExpenseRecordEntity::getExpenseStatus, InsuranceStatus.DRAFT.getCode())
                .ge(ExpenseRecordEntity::getCreateTime, monthStart)
        );
    }

    @Override
    public List<ProductSalesAnalysisVO> getProductSalesRanking(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveStatsDateRange(startDate, endDate);
        LocalDateTime startDateTime = range[0].atStartOfDay();
        LocalDateTime endDateTime = LocalDateTime.of(range[1], LocalTime.MAX);

        List<ExpenseRecordEntity> expenses = expenseRecordMapper.selectList(new LambdaQueryWrapper<ExpenseRecordEntity>()
                .notIn(ExpenseRecordEntity::getExpenseStatus,
                        InsuranceStatus.DRAFT.getCode(),
                        InsuranceStatus.REVIEW_REJECTED.getCode(),
                        InsuranceStatus.CANCELLED.getCode())
                .ge(ExpenseRecordEntity::getCreateTime, startDateTime)
                .le(ExpenseRecordEntity::getCreateTime, endDateTime));

        Map<String, ProductSalesAccumulator> accumulatorMap = new HashMap<>();
        for (ExpenseRecordEntity expense : expenses) {
            String productName = expense.getProductName() == null || expense.getProductName().isBlank() ? "未命名产品" : expense.getProductName();
            ProductSalesAccumulator accumulator = accumulatorMap.computeIfAbsent(productName, key -> new ProductSalesAccumulator());
            accumulator.orderCount++;
            accumulator.salesQuantity += expense.getQuantity() == null ? 0 : expense.getQuantity();
            accumulator.salesAmount = accumulator.salesAmount.add(expense.getTotalAmount() == null ? BigDecimal.ZERO : expense.getTotalAmount());
        }

        Map<String, Long> activePolicyMap = insuranceRecordMapper.selectList(new LambdaQueryWrapper<InsuranceRecordEntity>()
                        .eq(InsuranceRecordEntity::getInsuranceStatus, InsuranceStatus.ACTIVE.getCode())
                        .ge(InsuranceRecordEntity::getCreateTime, startDateTime)
                        .le(InsuranceRecordEntity::getCreateTime, endDateTime))
                .stream()
                .collect(Collectors.groupingBy(item -> item.getProductName() == null || item.getProductName().isBlank() ? "未命名产品" : item.getProductName(), Collectors.counting()));

        List<Map.Entry<String, ProductSalesAccumulator>> sortedEntries = new ArrayList<>(accumulatorMap.entrySet());
        sortedEntries.sort(Comparator
                .comparing((Map.Entry<String, ProductSalesAccumulator> item) -> item.getValue().salesAmount).reversed()
                .thenComparing(item -> item.getValue().salesQuantity, Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey));

        List<ProductSalesAnalysisVO> result = new ArrayList<>();
        for (int index = 0; index < Math.min(sortedEntries.size(), 10); index++) {
            Map.Entry<String, ProductSalesAccumulator> entry = sortedEntries.get(index);
            ProductSalesAccumulator value = entry.getValue();
            result.add(ProductSalesAnalysisVO.builder()
                    .rankNo(index + 1)
                    .productName(entry.getKey())
                    .orderCount(value.orderCount)
                    .salesQuantity(value.salesQuantity)
                    .salesAmount(value.salesAmount)
                    .activePolicyCount(activePolicyMap.getOrDefault(entry.getKey(), 0L))
                    .build());
        }
        return result;
    }

    @Override
    public List<OrderTrendAnalysisVO> getOrderTrendAnalysis(LocalDate startDate, LocalDate endDate, String periodType) {
        LocalDate[] range = resolveStatsDateRange(startDate, endDate);
        LocalDateTime startDateTime = range[0].atStartOfDay();
        LocalDateTime endDateTime = LocalDateTime.of(range[1], LocalTime.MAX);

        Map<LocalDate, Long> orderCountMap = expenseRecordMapper.selectList(new LambdaQueryWrapper<ExpenseRecordEntity>()
                        .ne(ExpenseRecordEntity::getExpenseStatus, InsuranceStatus.DRAFT.getCode())
                        .ge(ExpenseRecordEntity::getCreateTime, startDateTime)
                        .le(ExpenseRecordEntity::getCreateTime, endDateTime))
                .stream()
                .collect(Collectors.groupingBy(item -> item.getCreateTime().toLocalDate(), Collectors.counting()));

        return switch (normalizePeriodType(periodType)) {
            case "YEAR" -> buildMonthlyTrend(range[0], range[1], orderCountMap);
            case "QUARTER" -> buildSemiMonthlyTrend(range[0], range[1], orderCountMap);
            case "MONTH" -> buildWeeklyTrend(range[0], range[1], orderCountMap);
            default -> buildDailyTrend(range[0], range[1], orderCountMap);
        };
    }

    private static class ProductSalesAccumulator {
        private long orderCount;
        private int salesQuantity;
        private BigDecimal salesAmount = BigDecimal.ZERO;
    }

    private LocalDate[] resolveStatsDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate resolvedStart = startDate == null ? resolvedEnd.minusDays(29) : startDate;
        if (resolvedStart.isAfter(resolvedEnd)) {
            LocalDate temp = resolvedStart;
            resolvedStart = resolvedEnd;
            resolvedEnd = temp;
        }
        return new LocalDate[]{resolvedStart, resolvedEnd};
    }

    private String normalizePeriodType(String periodType) {
        if (periodType == null || periodType.isBlank()) {
            return "MONTH";
        }
        return periodType.trim().toUpperCase();
    }

    private List<OrderTrendAnalysisVO> buildDailyTrend(LocalDate startDate,
                                                       LocalDate endDate,
                                                       Map<LocalDate, Long> orderCountMap) {
        List<OrderTrendAnalysisVO> result = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            result.add(OrderTrendAnalysisVO.builder()
                    .dateLabel(cursor.format(DateTimeFormatter.ofPattern("MM-dd")))
                    .orderCount(orderCountMap.getOrDefault(cursor, 0L))
                    .build());
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private List<OrderTrendAnalysisVO> buildWeeklyTrend(LocalDate startDate,
                                                        LocalDate endDate,
                                                        Map<LocalDate, Long> orderCountMap) {
        List<OrderTrendAnalysisVO> result = new ArrayList<>();
        LocalDate bucketStart = startDate;
        int weekIndex = 1;
        while (!bucketStart.isAfter(endDate)) {
            LocalDate bucketEnd = bucketStart.plusDays(6);
            if (bucketEnd.isAfter(endDate)) {
                bucketEnd = endDate;
            }
            result.add(OrderTrendAnalysisVO.builder()
                    .dateLabel(String.format("第%d周 %s~%s", weekIndex,
                            bucketStart.format(DateTimeFormatter.ofPattern("MM-dd")),
                            bucketEnd.format(DateTimeFormatter.ofPattern("MM-dd"))))
                    .orderCount(sumOrderCount(orderCountMap, bucketStart, bucketEnd))
                    .build());
            bucketStart = bucketEnd.plusDays(1);
            weekIndex++;
        }
        return result;
    }

    private List<OrderTrendAnalysisVO> buildSemiMonthlyTrend(LocalDate startDate,
                                                             LocalDate endDate,
                                                             Map<LocalDate, Long> orderCountMap) {
        List<OrderTrendAnalysisVO> result = new ArrayList<>();
        LocalDate cursor = LocalDate.of(startDate.getYear(), startDate.getMonth(), 1);
        while (!cursor.isAfter(endDate)) {
            LocalDate firstHalfStart = cursor;
            LocalDate firstHalfEnd = LocalDate.of(cursor.getYear(), cursor.getMonth(), Math.min(15, cursor.lengthOfMonth()));
            appendHalfMonthBucket(result, orderCountMap, startDate, endDate, firstHalfStart, firstHalfEnd, cursor.format(DateTimeFormatter.ofPattern("MM上")));

            LocalDate secondHalfStart = firstHalfEnd.plusDays(1);
            LocalDate secondHalfEnd = LocalDate.of(cursor.getYear(), cursor.getMonth(), cursor.lengthOfMonth());
            appendHalfMonthBucket(result, orderCountMap, startDate, endDate, secondHalfStart, secondHalfEnd, cursor.format(DateTimeFormatter.ofPattern("MM下")));

            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    private void appendHalfMonthBucket(List<OrderTrendAnalysisVO> result,
                                       Map<LocalDate, Long> orderCountMap,
                                       LocalDate rangeStart,
                                       LocalDate rangeEnd,
                                       LocalDate bucketStart,
                                       LocalDate bucketEnd,
                                       String label) {
        if (bucketStart.isAfter(rangeEnd) || bucketEnd.isBefore(rangeStart)) {
            return;
        }
        LocalDate actualStart = bucketStart.isBefore(rangeStart) ? rangeStart : bucketStart;
        LocalDate actualEnd = bucketEnd.isAfter(rangeEnd) ? rangeEnd : bucketEnd;
        result.add(OrderTrendAnalysisVO.builder()
                .dateLabel(label)
                .orderCount(sumOrderCount(orderCountMap, actualStart, actualEnd))
                .build());
    }

    private List<OrderTrendAnalysisVO> buildMonthlyTrend(LocalDate startDate,
                                                         LocalDate endDate,
                                                         Map<LocalDate, Long> orderCountMap) {
        List<OrderTrendAnalysisVO> result = new ArrayList<>();
        LocalDate cursor = LocalDate.of(startDate.getYear(), startDate.getMonth(), 1);
        while (!cursor.isAfter(endDate)) {
            LocalDate monthStart = cursor;
            LocalDate monthEnd = cursor.withDayOfMonth(cursor.lengthOfMonth());
            LocalDate actualStart = monthStart.isBefore(startDate) ? startDate : monthStart;
            LocalDate actualEnd = monthEnd.isAfter(endDate) ? endDate : monthEnd;
            result.add(OrderTrendAnalysisVO.builder()
                    .dateLabel(cursor.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .orderCount(sumOrderCount(orderCountMap, actualStart, actualEnd))
                    .build());
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    private Long sumOrderCount(Map<LocalDate, Long> orderCountMap, LocalDate startDate, LocalDate endDate) {
        long total = 0L;
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            total += orderCountMap.getOrDefault(cursor, 0L);
            cursor = cursor.plusDays(1);
        }
        return total;
    }

    private void applyExpenseFilters(LambdaQueryWrapper<ExpenseRecordEntity> wrapper, ExpenseQuery query) {
        if (query == null) {
            return;
        }

        if (query.getPlan() != null && !query.getPlan().isBlank() && !"all".equalsIgnoreCase(query.getPlan())) {
            wrapper.like(ExpenseRecordEntity::getProductName, resolvePlanKeyword(query.getPlan()));
        }
        if (query.getStatus() != null && !query.getStatus().isBlank() && !"all".equalsIgnoreCase(query.getStatus())) {
            applyExpenseStatusFilter(wrapper, query.getStatus());
        }
        if (query.getUsername() != null && !query.getUsername().isBlank()) {
            wrapper.like(ExpenseRecordEntity::getContactName, query.getUsername().trim());
        }
        if (query.getSerialNo() != null && !query.getSerialNo().isBlank()) {
            wrapper.like(ExpenseRecordEntity::getSerialNo, query.getSerialNo().trim());
        }
        applyDateRange(wrapper, query.getStartDate(), query.getEndDate(), ExpenseRecordEntity::getCreateTime);
    }

    private void applyInsuranceFilters(LambdaQueryWrapper<InsuranceRecordEntity> wrapper, InsuranceQuery query) {
        if (query == null) {
            return;
        }

        if (query.getPlan() != null && !query.getPlan().isBlank() && !"all".equalsIgnoreCase(query.getPlan())) {
            wrapper.like(InsuranceRecordEntity::getProductName, resolvePlanKeyword(query.getPlan()));
        }
        if (query.getStatus() != null && !query.getStatus().isBlank() && !"all".equalsIgnoreCase(query.getStatus())) {
            applyInsuranceStatusFilter(wrapper, query.getStatus());
        }
        if (query.getSerialNo() != null && !query.getSerialNo().isBlank()) {
            wrapper.like(InsuranceRecordEntity::getPolicyNo, query.getSerialNo().trim());
        }
        if (query.getInsuredName() != null && !query.getInsuredName().isBlank()) {
            wrapper.like(InsuranceRecordEntity::getInsuredName, query.getInsuredName().trim());
        }
        if (query.getInsuredId() != null && !query.getInsuredId().isBlank()) {
            wrapper.like(InsuranceRecordEntity::getInsuredIdNo, query.getInsuredId().trim());
        }
        if (query.getBeneficiaryName() != null && !query.getBeneficiaryName().isBlank()) {
            wrapper.like(InsuranceRecordEntity::getBeneficiaryName, query.getBeneficiaryName().trim());
        }
        if (query.getBeneficiaryId() != null && !query.getBeneficiaryId().isBlank()) {
            wrapper.like(InsuranceRecordEntity::getBeneficiaryIdNo, query.getBeneficiaryId().trim());
        }
        if (query.getAgent() != null && !query.getAgent().isBlank()) {
            wrapper.like(InsuranceRecordEntity::getAgentName, query.getAgent().trim());
        }
        applyDateRange(wrapper, query.getStartDate(), query.getEndDate(), InsuranceRecordEntity::getCreateTime);
    }

    private <T> void applyDateRange(LambdaQueryWrapper<T> wrapper,
                                    java.util.Date startDate,
                                    java.util.Date endDate,
                                    SFunction<T, LocalDateTime> columnGetter) {
        if (startDate != null) {
            wrapper.ge(columnGetter, toStartOfDay(startDate));
        }
        if (endDate != null) {
            wrapper.le(columnGetter, toEndOfDay(endDate));
        }
    }

    private LocalDateTime toStartOfDay(java.util.Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay();
    }

    private LocalDateTime toEndOfDay(java.util.Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate().atTime(LocalTime.MAX);
    }

    private List<String> productImportHeaders() {
        return List.of("产品编码", "产品名称", "分类编码", "承保公司", "产品描述", "产品特点", "价格", "是否新品", "是否热销", "上架状态", "排序号");
    }

    private void validateProductImportHeaders(List<String> headers) {
        List<String> expected = productImportHeaders();
        if (headers == null || headers.size() < expected.size()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "Excel模板表头不完整，请使用系统下载的模板");
        }
        for (int i = 0; i < expected.size(); i++) {
            String actual = normalizeCell(headers.get(i));
            if (!expected.get(i).equals(actual)) {
                throw new BusinessException(ResultCode.INVALID_PARAM, "Excel模板表头不正确，请使用系统下载的模板");
            }
        }
    }

    private ProductRequest buildProductRequestFromRow(List<String> row, int rowNumber) {
        ProductRequest request = new ProductRequest();
        request.setProductCode(requireCell(row, 0, rowNumber, "产品编码"));
        request.setProductName(requireCell(row, 1, rowNumber, "产品名称"));
        request.setCategoryCode(requireCell(row, 2, rowNumber, "分类编码"));
        request.setCompanyName(requireCell(row, 3, rowNumber, "承保公司"));
        request.setDescription(getCell(row, 4));
        request.setFeatures(getCell(row, 5));
        request.setPrice(parseBigDecimal(requireCell(row, 6, rowNumber, "价格"), rowNumber, "价格"));
        request.setIsNew(parseInteger(getCell(row, 7), rowNumber, "是否新品", 0));
        request.setIsHot(parseInteger(getCell(row, 8), rowNumber, "是否热销", 0));
        request.setSaleStatus(normalizeSaleStatus(getCell(row, 9)));
        request.setSortNo(parseInteger(getCell(row, 10), rowNumber, "排序号", 0));
        return request;
    }

    private String normalizeSaleStatus(String value) {
        String normalized = normalizeCell(value);
        if (normalized.isEmpty()) {
            return "ON_SALE";
        }
        String upper = normalized.toUpperCase();
        if (!List.of("ON_SALE", "OFF_SALE").contains(upper)) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "上架状态仅支持 ON_SALE 或 OFF_SALE");
        }
        return upper;
    }

    private String requireCell(List<String> row, int index, int rowNumber, String fieldName) {
        String value = getCell(row, index);
        if (value.isBlank()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "第" + rowNumber + "行【" + fieldName + "】不能为空");
        }
        return value;
    }

    private String getCell(List<String> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return "";
        }
        return normalizeCell(row.get(index));
    }

    private String normalizeCell(String value) {
        return value == null ? "" : value.trim();
    }

    private BigDecimal parseBigDecimal(String value, int rowNumber, String fieldName) {
        try {
            BigDecimal result = new BigDecimal(value.trim());
            if (result.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ResultCode.INVALID_PARAM, "第" + rowNumber + "行【" + fieldName + "】必须大于0");
            }
            return result;
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "第" + rowNumber + "行【" + fieldName + "】格式不正确");
        }
    }

    private Integer parseInteger(String value, int rowNumber, String fieldName, Integer defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "第" + rowNumber + "行【" + fieldName + "】格式不正确");
        }
    }

    private boolean isBlankRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String resolvePlanKeyword(String plan) {
        String normalized = plan == null ? "" : plan.trim().toLowerCase();
        if (normalized.contains("guoshou") || normalized.contains("国寿") || normalized.contains("1-3")) {
            return "国寿";
        }
        if (normalized.contains("pingan") || normalized.contains("平安")) {
            return "平安";
        }
        if (normalized.contains("child") || normalized.contains("少儿")) {
            return "少儿";
        }
        if (normalized.contains("elder") || normalized.contains("老年")) {
            return "老年";
        }
        if (normalized.contains("travel") || normalized.contains("旅游")) {
            return "旅游";
        }
        if (normalized.contains("maternity") || normalized.contains("驾乘")) {
            return "驾乘";
        }
        if (normalized.contains("zhonghua") || normalized.contains("中华")) {
            return "中华";
        }
        if (normalized.contains("taiping") || normalized.contains("太平")) {
            return "太平";
        }
        if (normalized.contains("renbao") || normalized.contains("人保")) {
            return "人保";
        }
        return plan.trim();
    }

    private RechargeVO convertToRechargeVO(TransactionRecordEntity entity) {
        // 查询用户名
        String username = null;
        if (entity.getUserId() != null) {
            AxxUserEntity user = axxUserMapper.selectById(entity.getUserId());
            if (user != null) {
                username = user.getUsername();
            }
        }
        return RechargeVO.builder()
                .id(entity.getId())
                .username(username)
                .date(entity.getCreateTime().toLocalDate().toString())
                .amount(entity.getAmount())
                .type(entity.getTransType().toLowerCase())
                .description(entity.getDescription())
                .serial(entity.getSerialNo())
                .balance(entity.getBalanceAfter())
                .build();
    }
}
