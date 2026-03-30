package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.enums.InsuranceStatus;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.entity.*;
import com.zs.ytbx.mapper.*;
import com.zs.ytbx.service.AdminService;
import com.zs.ytbx.vo.anxinxuan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

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
        user.setPassword(request.getPassword());
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
            user.setPassword(request.getPassword());
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
        product.setPrice(request.getPrice());
        product.setStock(request.getStock() != null ? request.getStock() : 0);
        product.setIsNew(request.getIsNew() != null ? request.getIsNew() : 0);
        product.setIsHot(request.getIsHot() != null ? request.getIsHot() : 0);
        product.setSaleStatus(request.getSaleStatus() != null ? request.getSaleStatus() : "ON_SALE");
        product.setSortNo(request.getSortNo() != null ? request.getSortNo() : 0);
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
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            product.setStock(request.getStock());
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
        
        if (query.getStatus() != null && !query.getStatus().isEmpty() && !"all".equals(query.getStatus())) {
            applyExpenseStatusFilter(wrapper, query.getStatus());
        }
        
        if (query.getSerialNo() != null && !query.getSerialNo().isEmpty()) {
            wrapper.like(ExpenseRecordEntity::getSerialNo, query.getSerialNo());
        }
        
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
        if (query.getStatus() == null || query.getStatus().isEmpty() || "all".equals(query.getStatus())) {
            wrapper.ne(InsuranceRecordEntity::getInsuranceStatus, InsuranceStatus.DRAFT.getCode());
        }
        
        if (query.getStatus() != null && !query.getStatus().isEmpty() && !"all".equals(query.getStatus())) {
            applyInsuranceStatusFilter(wrapper, query.getStatus());
        }
        
        if (query.getSerialNo() != null && !query.getSerialNo().isEmpty()) {
            wrapper.like(InsuranceRecordEntity::getPolicyNo, query.getSerialNo());
        }
        
        if (query.getInsuredName() != null && !query.getInsuredName().isEmpty()) {
            wrapper.like(InsuranceRecordEntity::getInsuredName, query.getInsuredName());
        }
        
        if (query.getBeneficiaryName() != null && !query.getBeneficiaryName().isEmpty()) {
            wrapper.like(InsuranceRecordEntity::getBeneficiaryName, query.getBeneficiaryName());
        }
        
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
    @Transactional(rollbackFor = Exception.class)
    public void approveInsurance(Long insuranceId, InsuranceApproveRequest request, Long reviewerId, String reviewerName) {
        InsuranceRecordEntity insurance = requireInsurance(insuranceId);
        ensureTransition(insurance, InsuranceStatus.PENDING_REVIEW, InsuranceStatus.APPROVED);

        insurance.setReviewComment(request.getReviewComment().trim());
        insurance.setReviewerId(reviewerId);
        insurance.setReviewerName(reviewerName);
        insurance.setReviewTime(LocalDateTime.now());
        insurance.setRejectReason(null);
        insurance.setUpdateTime(LocalDateTime.now());

        boolean hasPolicyInfo = request.getPolicyNo() != null && !request.getPolicyNo().isBlank()
                && request.getEffectiveDate() != null && request.getExpiryDate() != null;

        if (hasPolicyInfo) {
            if (request.getExpiryDate().isBefore(request.getEffectiveDate())) {
                throw new BusinessException(ResultCode.INVALID_PARAM, "结束日期不能早于起保日期");
            }
            insurance.setInsuranceStatus(InsuranceStatus.ACTIVE.getCode());
            insurance.setPolicyNo(request.getPolicyNo().trim());
            insurance.setEffectiveDate(request.getEffectiveDate());
            insurance.setExpiryDate(request.getExpiryDate());
            insurance.setUnderwritingTime(LocalDateTime.now());
            insurance.setActivateTime(LocalDateTime.now());
            insuranceRecordMapper.updateById(insurance);
            syncExpenseStatus(insurance.getExpenseId(), InsuranceStatus.ACTIVE, insurance.getPolicyNo(), insurance.getEffectiveDate(), insurance.getExpiryDate());
        } else {
            insurance.setInsuranceStatus(InsuranceStatus.APPROVED.getCode());
            insurance.setUnderwritingTime(null);
            insurance.setActivateTime(null);
            insuranceRecordMapper.updateById(insurance);
            syncExpenseStatus(insurance.getExpenseId(), InsuranceStatus.APPROVED, insurance.getPolicyNo(), insurance.getEffectiveDate(), insurance.getExpiryDate());
        }
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
                .price(entity.getPrice())
                .stock(entity.getStock())
                .isNew(entity.getIsNew() == 1)
                .isHot(entity.getIsHot() == 1)
                .categoryCode(entity.getCategoryCode())
                .companyName(entity.getCompanyName())
                .saleStatus(entity.getSaleStatus())
                .sortNo(entity.getSortNo())
                .imageUrl(entity.getImageUrl())
                .templateFileName(entity.getTemplateFileName())
                .build();
    }

    private ProductDetailVO convertToProductDetailVO(AxxProductEntity entity) {
        return ProductDetailVO.builder()
                .id(entity.getId())
                .productCode(entity.getProductCode())
                .name(entity.getProductName())
                .description(entity.getDescription())
                .features(entity.getFeatures())
                .price(entity.getPrice())
                .stock(entity.getStock())
                .isNew(entity.getIsNew() == 1)
                .categoryCode(entity.getCategoryCode())
                .companyName(entity.getCompanyName())
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

    private RechargeVO convertToRechargeVO(TransactionRecordEntity entity) {
        return RechargeVO.builder()
                .id(entity.getId())
                .date(entity.getCreateTime().toLocalDate().toString())
                .amount(entity.getAmount())
                .type(entity.getTransType().toLowerCase())
                .description(entity.getDescription())
                .serial(entity.getSerialNo())
                .balance(entity.getBalanceAfter())
                .build();
    }
}
