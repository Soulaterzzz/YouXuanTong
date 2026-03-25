package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.ytbx.common.api.PageResponse;
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
import java.time.temporal.TemporalAdjusters;
import java.util.List;
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
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        axxProductMapper.insert(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductRequest request) {
        AxxProductEntity product = axxProductMapper.selectById(request.getId());
        if (product == null) {
            throw new RuntimeException("产品不存在");
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
        axxProductMapper.updateById(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        AxxProductEntity product = axxProductMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("产品不存在");
        }
        product.setDeleted(1);
        product.setUpdateTime(LocalDateTime.now());
        axxProductMapper.updateById(product);
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
    public PageResponse<InsuranceVO> listAllInsurances(InsuranceQuery query) {
        LambdaQueryWrapper<InsuranceRecordEntity> wrapper = new LambdaQueryWrapper<>();
        
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
    public Long getTodayNewOrders() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        return expenseRecordMapper.selectCount(
            new LambdaQueryWrapper<ExpenseRecordEntity>()
                .ge(ExpenseRecordEntity::getCreateTime, todayStart)
        );
    }

    @Override
    public Long getPendingOrders() {
        return expenseRecordMapper.selectCount(
            new LambdaQueryWrapper<ExpenseRecordEntity>()
                .eq(ExpenseRecordEntity::getExpenseStatus, "PENDING")
        );
    }

    @Override
    public Long getMonthOrders() {
        LocalDate now = LocalDate.now();
        LocalDateTime monthStart = LocalDateTime.of(now.with(TemporalAdjusters.firstDayOfMonth()), LocalTime.MIN);
        return expenseRecordMapper.selectCount(
            new LambdaQueryWrapper<ExpenseRecordEntity>()
                .ge(ExpenseRecordEntity::getCreateTime, monthStart)
        );
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
