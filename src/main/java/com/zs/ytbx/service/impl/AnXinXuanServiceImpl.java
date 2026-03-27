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
import com.zs.ytbx.service.AnXinXuanService;
import com.zs.ytbx.vo.anxinxuan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnXinXuanServiceImpl implements AnXinXuanService {

    private final AxxProductMapper axxProductMapper;
    private final ExpenseRecordMapper expenseRecordMapper;
    private final InsuranceRecordMapper insuranceRecordMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final AccountBalanceMapper accountBalanceMapper;

    @Override
    public PageResponse<ProductVO> listProducts(ProductQuery query) {
        LambdaQueryWrapper<AxxProductEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AxxProductEntity::getSaleStatus, "ON_SALE");

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
    public ProductDetailVO getProductDetail(Long productId) {
        AxxProductEntity entity = axxProductMapper.selectById(productId);
        if (entity == null) {
            return null;
        }
        return convertToProductDetailVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateProduct(ActivateRequest request, Long userId) {
        AxxProductEntity product = requireProduct(request.getProductId());
        AccountBalanceEntity balance = ensureBalance(userId);
        int quantity = normalizeQuantity(request.getCount());
        BigDecimal totalAmount = calculateTotalAmount(product, quantity);
        BigDecimal balanceBefore = balance.getBalance();

        if (balanceBefore.compareTo(totalAmount) < 0) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "余额不足，请先充值");
        }

        ExpenseRecordEntity expense = buildExpenseRecord(request, userId, product, quantity, totalAmount, InsuranceStatus.PENDING_REVIEW);
        expenseRecordMapper.insert(expense);

        InsuranceRecordEntity insurance = buildInsuranceRecord(request, userId, product, quantity, InsuranceStatus.PENDING_REVIEW, expense.getId());
        insuranceRecordMapper.insert(insurance);

        balance.setBalance(balanceBefore.subtract(totalAmount));
        accountBalanceMapper.updateById(balance);

        createConsumeTransaction(userId, totalAmount, balanceBefore, balance.getBalance(),
                "提交投保审核：" + product.getProductName() + " x" + quantity, expense.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(ActivateRequest request, Long userId) {
        AxxProductEntity product = requireProduct(request.getProductId());
        int quantity = normalizeQuantity(request.getCount());
        BigDecimal totalAmount = calculateTotalAmount(product, quantity);

        ExpenseRecordEntity expense = buildExpenseRecord(request, userId, product, quantity, totalAmount, InsuranceStatus.DRAFT);
        expenseRecordMapper.insert(expense);

        InsuranceRecordEntity insurance = buildInsuranceRecord(request, userId, product, quantity, InsuranceStatus.DRAFT, expense.getId());
        insuranceRecordMapper.insert(insurance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDraft(Long insuranceId, Long userId) {
        InsuranceRecordEntity insurance = insuranceRecordMapper.selectById(insuranceId);
        if (insurance == null || !userId.equals(insurance.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "投保记录不存在");
        }

        InsuranceStatus currentStatus = InsuranceStatus.fromCode(insurance.getInsuranceStatus());
        if (currentStatus != InsuranceStatus.DRAFT) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "仅待提交记录可提交审核");
        }

        ExpenseRecordEntity expense = expenseRecordMapper.selectById(insurance.getExpenseId());
        if (expense == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "费用记录不存在");
        }

        AccountBalanceEntity balance = ensureBalance(userId);
        BigDecimal totalAmount = expense.getTotalAmount();
        BigDecimal balanceBefore = balance.getBalance();
        if (balanceBefore.compareTo(totalAmount) < 0) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "余额不足，请先充值");
        }

        insurance.setInsuranceStatus(InsuranceStatus.PENDING_REVIEW.getCode());
        insurance.setReviewComment(null);
        insurance.setReviewerId(null);
        insurance.setReviewerName(null);
        insurance.setReviewTime(null);
        insurance.setRejectReason(null);
        insurance.setSubmitTime(LocalDateTime.now());
        insurance.setUnderwritingTime(null);
        insurance.setActivateTime(null);
        insurance.setPolicyNo(null);
        insurance.setEffectiveDate(null);
        insurance.setExpiryDate(null);
        insurance.setUpdateTime(LocalDateTime.now());
        insuranceRecordMapper.updateById(insurance);

        expense.setExpenseStatus(InsuranceStatus.PENDING_REVIEW.getCode());
        expense.setPolicyNo(null);
        expense.setEffectiveDate(null);
        expense.setExpiryDate(null);
        expense.setUpdateTime(LocalDateTime.now());
        expenseRecordMapper.updateById(expense);

        balance.setBalance(balanceBefore.subtract(totalAmount));
        accountBalanceMapper.updateById(balance);

        createConsumeTransaction(userId, totalAmount, balanceBefore, balance.getBalance(),
                "提交投保审核：" + insurance.getProductName() + " x" + insurance.getQuantity(), expense.getId());
    }

    @Override
    public PageResponse<ExpenseVO> listExpenses(ExpenseQuery query, Long userId) {
        LambdaQueryWrapper<ExpenseRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseRecordEntity::getUserId, userId)
                .ne(ExpenseRecordEntity::getExpenseStatus, InsuranceStatus.DRAFT.getCode());

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
    public PageResponse<InsuranceVO> listInsurances(InsuranceQuery query, Long userId) {
        LambdaQueryWrapper<InsuranceRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InsuranceRecordEntity::getUserId, userId);

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
    public void exportInsurances(ExportRequest request) {
    }

    @Override
    public PageResponse<RechargeVO> listRecharges(RechargeQuery query, Long userId) {
        LambdaQueryWrapper<TransactionRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionRecordEntity::getUserId, userId);

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
    public void createRecharge(CreateRechargeRequest request, Long userId) {
        AccountBalanceEntity balance = ensureBalance(userId);

        BigDecimal balanceBefore = balance.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.getAmount());
        balance.setBalance(balanceAfter);
        accountBalanceMapper.updateById(balance);

        TransactionRecordEntity transaction = new TransactionRecordEntity();
        transaction.setSerialNo(generateSerialNo("RCH"));
        transaction.setUserId(userId);
        transaction.setTransType("RECHARGE");
        transaction.setAmount(request.getAmount());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(request.getRemark() != null ? request.getRemark() : "账户充值");
        transaction.setPaymentMethod(request.getMethod());
        transaction.setPaymentStatus("SUCCESS");
        transaction.setCreateTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());
        transactionRecordMapper.insert(transaction);
    }

    public BigDecimal getBalance(Long userId) {
        AccountBalanceEntity balance = accountBalanceMapper.selectOne(
                new LambdaQueryWrapper<AccountBalanceEntity>()
                        .eq(AccountBalanceEntity::getUserId, userId));
        return balance != null ? balance.getBalance() : BigDecimal.ZERO;
    }

    public Long getTotalPolicies(Long userId) {
        return insuranceRecordMapper.selectCount(
                new LambdaQueryWrapper<InsuranceRecordEntity>()
                        .eq(InsuranceRecordEntity::getUserId, userId)
                        .eq(InsuranceRecordEntity::getInsuranceStatus, InsuranceStatus.ACTIVE.getCode()));
    }

    public BigDecimal getTotalExpenses(Long userId) {
        LambdaQueryWrapper<TransactionRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionRecordEntity::getUserId, userId)
                .eq(TransactionRecordEntity::getTransType, "CONSUME");
        List<TransactionRecordEntity> records = transactionRecordMapper.selectList(wrapper);
        return records.stream()
                .map(TransactionRecordEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getTotalProducts() {
        return axxProductMapper.selectCount(
                new LambdaQueryWrapper<AxxProductEntity>()
                        .eq(AxxProductEntity::getSaleStatus, "ON_SALE"));
    }

    private AxxProductEntity requireProduct(Long productId) {
        AxxProductEntity product = axxProductMapper.selectById(productId);
        if (product == null || !"ON_SALE".equals(product.getSaleStatus())) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private AccountBalanceEntity ensureBalance(Long userId) {
        AccountBalanceEntity balance = accountBalanceMapper.selectOne(
                new LambdaQueryWrapper<AccountBalanceEntity>()
                        .eq(AccountBalanceEntity::getUserId, userId));
        if (balance != null) {
            return balance;
        }

        AccountBalanceEntity newBalance = new AccountBalanceEntity();
        newBalance.setUserId(userId);
        newBalance.setBalance(BigDecimal.ZERO);
        newBalance.setFrozenBalance(BigDecimal.ZERO);
        newBalance.setCreateTime(LocalDateTime.now());
        newBalance.setUpdateTime(LocalDateTime.now());
        accountBalanceMapper.insert(newBalance);
        return newBalance;
    }

    private int normalizeQuantity(Integer count) {
        return count == null || count <= 0 ? 1 : count;
    }

    private BigDecimal calculateTotalAmount(AxxProductEntity product, int quantity) {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    private ExpenseRecordEntity buildExpenseRecord(ActivateRequest request,
                                                   Long userId,
                                                   AxxProductEntity product,
                                                   int quantity,
                                                   BigDecimal totalAmount,
                                                   InsuranceStatus status) {
        LocalDateTime now = LocalDateTime.now();
        ExpenseRecordEntity expense = new ExpenseRecordEntity();
        expense.setSerialNo(generateSerialNo("EXP"));
        expense.setUserId(userId);
        expense.setProductId(product.getId());
        expense.setProductName(product.getProductName());
        expense.setContactName(request.getBeneficiaryName());
        expense.setExpenseStatus(status.getCode());
        expense.setPolicyNo(null);
        expense.setPremiumAmount(product.getPrice());
        expense.setQuantity(quantity);
        expense.setTotalAmount(totalAmount);
        expense.setEffectiveDate(null);
        expense.setExpiryDate(null);
        expense.setExportTime(null);
        expense.setCreateTime(now);
        expense.setUpdateTime(now);
        return expense;
    }

    private InsuranceRecordEntity buildInsuranceRecord(ActivateRequest request,
                                                       Long userId,
                                                       AxxProductEntity product,
                                                       int quantity,
                                                       InsuranceStatus status,
                                                       Long expenseId) {
        LocalDateTime now = LocalDateTime.now();
        InsuranceRecordEntity insurance = new InsuranceRecordEntity();
        insurance.setExpenseId(expenseId);
        insurance.setUserId(userId);
        insurance.setProductName(product.getProductName());
        insurance.setInsuredName(request.getBeneficiaryName());
        insurance.setInsuredIdNo(request.getBeneficiaryId());
        insurance.setBeneficiaryName(request.getBeneficiaryName());
        insurance.setBeneficiaryIdNo(request.getBeneficiaryId());
        insurance.setBeneficiaryJob(request.getBeneficiaryJob());
        insurance.setBeneficiaryAddress(request.getAddress());
        insurance.setAgentName(request.getAgent());
        insurance.setInsuranceStatus(status.getCode());
        insurance.setReviewComment(null);
        insurance.setReviewerId(null);
        insurance.setReviewerName(null);
        insurance.setReviewTime(null);
        insurance.setRejectReason(null);
        insurance.setSubmitTime(status == InsuranceStatus.DRAFT ? null : now);
        insurance.setUnderwritingTime(null);
        insurance.setActivateTime(null);
        insurance.setPolicyNo(null);
        insurance.setPremiumAmount(product.getPrice());
        insurance.setQuantity(quantity);
        insurance.setEffectiveDate(null);
        insurance.setExpiryDate(null);
        insurance.setExportTime(null);
        insurance.setCreateTime(now);
        insurance.setUpdateTime(now);
        return insurance;
    }

    private void createConsumeTransaction(Long userId,
                                          BigDecimal amount,
                                          BigDecimal balanceBefore,
                                          BigDecimal balanceAfter,
                                          String description,
                                          Long expenseId) {
        TransactionRecordEntity transaction = new TransactionRecordEntity();
        transaction.setSerialNo(generateSerialNo("TXN"));
        transaction.setUserId(userId);
        transaction.setTransType("CONSUME");
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setRefType("EXPENSE");
        transaction.setRefId(expenseId);
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
}
