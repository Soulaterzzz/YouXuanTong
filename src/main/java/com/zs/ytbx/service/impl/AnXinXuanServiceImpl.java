package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.entity.*;
import com.zs.ytbx.mapper.*;
import com.zs.ytbx.service.AnXinXuanService;
import com.zs.ytbx.vo.anxinxuan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        AxxProductEntity product = axxProductMapper.selectById(request.getProductId());
        if (product == null) {
            throw new RuntimeException("产品不存在");
        }

        AccountBalanceEntity balance = accountBalanceMapper.selectOne(
            new LambdaQueryWrapper<AccountBalanceEntity>()
                .eq(AccountBalanceEntity::getUserId, userId));
        
        if (balance == null) {
            balance = new AccountBalanceEntity();
            balance.setUserId(userId);
            balance.setBalance(BigDecimal.ZERO);
            balance.setFrozenBalance(BigDecimal.ZERO);
            accountBalanceMapper.insert(balance);
        }

        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(request.getCount()));
        
        if (balance.getBalance().compareTo(totalAmount) < 0) {
            throw new RuntimeException("余额不足，请先充值");
        }

        String serialNo = generateSerialNo("EXP");
        String policyNo = "P" + System.currentTimeMillis();
        
        ExpenseRecordEntity expense = new ExpenseRecordEntity();
        expense.setSerialNo(serialNo);
        expense.setUserId(userId);
        expense.setProductId(product.getId());
        expense.setProductName(product.getProductName());
        expense.setContactName(request.getBeneficiaryName());
        expense.setExpenseStatus("COMPLETED");
        expense.setPolicyNo(policyNo);
        expense.setPremiumAmount(product.getPrice());
        expense.setQuantity(request.getCount());
        expense.setTotalAmount(totalAmount);
        expense.setEffectiveDate(LocalDate.now().plusDays(1));
        expense.setExpiryDate(LocalDate.now().plusYears(1));
        expense.setExportTime(LocalDateTime.now());
        expense.setCreateTime(LocalDateTime.now());
        expense.setUpdateTime(LocalDateTime.now());
        expenseRecordMapper.insert(expense);

        InsuranceRecordEntity insurance = new InsuranceRecordEntity();
        insurance.setExpenseId(expense.getId());
        insurance.setUserId(userId);
        insurance.setProductName(product.getProductName());
        insurance.setInsuredName(request.getBeneficiaryName());
        insurance.setInsuredIdNo(request.getBeneficiaryId());
        insurance.setBeneficiaryName(request.getBeneficiaryName());
        insurance.setBeneficiaryIdNo(request.getBeneficiaryId());
        insurance.setBeneficiaryJob(request.getBeneficiaryJob());
        insurance.setBeneficiaryAddress(request.getAddress());
        insurance.setAgentName(request.getAgent());
        insurance.setInsuranceStatus("ACTIVE");
        insurance.setPolicyNo(policyNo);
        insurance.setPremiumAmount(product.getPrice());
        insurance.setQuantity(request.getCount());
        insurance.setEffectiveDate(LocalDate.now().plusDays(1));
        insurance.setExpiryDate(LocalDate.now().plusYears(1));
        insurance.setExportTime(LocalDateTime.now());
        insurance.setCreateTime(LocalDateTime.now());
        insurance.setUpdateTime(LocalDateTime.now());
        insuranceRecordMapper.insert(insurance);

        BigDecimal balanceBefore = balance.getBalance();
        balance.setBalance(balanceBefore.subtract(totalAmount));
        accountBalanceMapper.updateById(balance);

        TransactionRecordEntity transaction = new TransactionRecordEntity();
        transaction.setSerialNo(generateSerialNo("TXN"));
        transaction.setUserId(userId);
        transaction.setTransType("CONSUME");
        transaction.setAmount(totalAmount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balance.getBalance());
        transaction.setDescription("购买保险：" + product.getProductName() + " x" + request.getCount());
        transaction.setRefType("EXPENSE");
        transaction.setRefId(expense.getId());
        transaction.setPaymentStatus("SUCCESS");
        transaction.setCreateTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());
        transactionRecordMapper.insert(transaction);
    }

    @Override
    public PageResponse<ExpenseVO> listExpenses(ExpenseQuery query, Long userId) {
        LambdaQueryWrapper<ExpenseRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseRecordEntity::getUserId, userId);
        
        if (query.getStatus() != null && !query.getStatus().isEmpty() && !"all".equals(query.getStatus())) {
            wrapper.eq(ExpenseRecordEntity::getExpenseStatus, query.getStatus().toUpperCase());
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
            wrapper.eq(InsuranceRecordEntity::getInsuranceStatus, query.getStatus().toUpperCase());
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
        AccountBalanceEntity balance = accountBalanceMapper.selectOne(
            new LambdaQueryWrapper<AccountBalanceEntity>()
                .eq(AccountBalanceEntity::getUserId, userId));
        
        if (balance == null) {
            balance = new AccountBalanceEntity();
            balance.setUserId(userId);
            balance.setBalance(BigDecimal.ZERO);
            balance.setFrozenBalance(BigDecimal.ZERO);
            accountBalanceMapper.insert(balance);
        }
        
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
                .eq(InsuranceRecordEntity::getInsuranceStatus, "ACTIVE"));
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
        return InsuranceVO.builder()
                .id(entity.getId())
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
        if (status == null) return "";
        switch (status.toUpperCase()) {
            case "COMPLETED":
            case "ACTIVE":
                return "已完成";
            case "PENDING":
                return "待处理";
            case "PROCESSING":
                return "处理中";
            case "CANCELLED":
            case "EXPIRED":
                return "已取消";
            default:
                return status;
        }
    }
}
