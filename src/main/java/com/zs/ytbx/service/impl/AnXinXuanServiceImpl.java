package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.common.enums.InsuranceStatus;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.common.export.SimplePdfWriter;
import com.zs.ytbx.common.export.SimpleXlsxWriter;
import com.zs.ytbx.common.export.SimpleXlsxReader;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.entity.*;
import com.zs.ytbx.mapper.*;
import com.zs.ytbx.service.AnXinXuanService;
import com.zs.ytbx.vo.anxinxuan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AnXinXuanServiceImpl implements AnXinXuanService {

    private final AxxProductMapper axxProductMapper;
    private final ExpenseRecordMapper expenseRecordMapper;
    private final InsuranceRecordMapper insuranceRecordMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final AuthContext authContext;

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
    public ProductDetailVO getProductDetail(Long productId) {
        AxxProductEntity entity = axxProductMapper.selectById(productId);
        if (entity == null) {
            return null;
        }
        return convertToProductDetailVO(entity);
    }

    private static final int MAX_RETRY = 3;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateProduct(ActivateRequest request, Long userId) {
        AxxProductEntity product = requireProduct(request.getProductId());
        int quantity = normalizeQuantity(request.getCount());
        BigDecimal premiumAmount = resolvePremiumAmount(product);
        BigDecimal displayPrice = resolveDisplayPrice(request, product);
        BigDecimal totalAmount = calculateTotalAmount(premiumAmount, quantity);

        ExpenseRecordEntity expense = buildExpenseRecord(request, userId, product, premiumAmount, displayPrice, quantity, totalAmount, InsuranceStatus.PENDING_REVIEW);
        expenseRecordMapper.insert(expense);

        InsuranceRecordEntity insurance = buildInsuranceRecord(request, userId, product, premiumAmount, displayPrice, quantity, InsuranceStatus.PENDING_REVIEW, expense.getId());
        insuranceRecordMapper.insert(insurance);

        deductBalanceWithOptimisticLock(userId, totalAmount, expense.getId(),
                "提交投保审核：" + product.getProductName() + " x" + quantity);
    }

    private void deductBalanceWithOptimisticLock(Long userId, BigDecimal amount, Long expenseId, String description) {
        for (int i = 0; i < MAX_RETRY; i++) {
            AccountBalanceEntity balance = ensureBalance(userId);
            BigDecimal balanceBefore = balance.getBalance();

            if (balanceBefore.compareTo(amount) < 0) {
                throw new BusinessException(ResultCode.INVALID_PARAM, "余额不足，请先充值");
            }

            balance.setBalance(balanceBefore.subtract(amount));
            int updated = accountBalanceMapper.updateById(balance);
            if (updated > 0) {
                createConsumeTransaction(userId, amount, balanceBefore, balance.getBalance(), description, expenseId);
                return;
            }
        }
        throw new BusinessException(ResultCode.SYSTEM_ERROR, "余额更新失败，请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(ActivateRequest request, Long userId) {
        AxxProductEntity product = requireProduct(request.getProductId());
        int quantity = normalizeQuantity(request.getCount());
        BigDecimal premiumAmount = resolvePremiumAmount(product);
        BigDecimal displayPrice = resolveDisplayPrice(request, product);
        BigDecimal totalAmount = calculateTotalAmount(premiumAmount, quantity);

        ExpenseRecordEntity expense = buildExpenseRecord(request, userId, product, premiumAmount, displayPrice, quantity, totalAmount, InsuranceStatus.DRAFT);
        expenseRecordMapper.insert(expense);

        InsuranceRecordEntity insurance = buildInsuranceRecord(request, userId, product, premiumAmount, displayPrice, quantity, InsuranceStatus.DRAFT, expense.getId());
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
        LambdaQueryWrapper<ExpenseRecordEntity> wrapper = buildExpenseWrapper(query, userId);
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
        LambdaQueryWrapper<InsuranceRecordEntity> wrapper = buildInsuranceWrapper(query, userId);
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
    public byte[] exportExpenses(ExpenseQuery query) {
        Long userId = isAdmin() ? null : currentUserId();
        LambdaQueryWrapper<ExpenseRecordEntity> wrapper = buildExpenseWrapper(query, userId);
        wrapper.orderByDesc(ExpenseRecordEntity::getCreateTime);

        List<List<String>> rows = expenseRecordMapper.selectList(wrapper).stream()
                .map(this::toExpenseExportRow)
                .collect(Collectors.toList());
        return SimpleXlsxWriter.write("费用清单", expenseExportHeaders(), rows);
    }

    @Override
    public byte[] exportInsurances(InsuranceQuery query) {
        Long userId = isAdmin() ? null : currentUserId();
        LambdaQueryWrapper<InsuranceRecordEntity> wrapper = buildInsuranceWrapper(query, userId);
        wrapper.orderByDesc(InsuranceRecordEntity::getCreateTime);

        List<SimplePdfWriter.PolicyPage> pages = insuranceRecordMapper.selectList(wrapper).stream()
                .map(this::renderInsurancePolicyPage)
                .collect(Collectors.toList());
        if (pages.isEmpty()) {
            pages = List.of(new SimplePdfWriter.PolicyPage(
                    SimplePdfWriter.renderDetailPage(
                            "保险清单",
                            List.of(new SimplePdfWriter.FieldLine("提示", "当前筛选条件下没有保险记录")),
                            "请调整筛选条件后重试"),
                    List.of("当前筛选条件下没有保险记录", "请调整筛选条件后重试")));
        }
        return SimplePdfWriter.writePolicyPages(pages);
    }

    @Override
    public byte[] exportInsurancePdf(Long insuranceId) {
        InsuranceRecordEntity insurance = requireInsuranceVisible(insuranceId);
        SimplePdfWriter.PolicyPage page = renderInsurancePolicyPage(insurance);
        return SimplePdfWriter.writePolicyPages(List.of(page));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProductInsurancePreviewVO> previewProductInsurances(Long productId, MultipartFile file) {
        return processProductInsuranceTemplate(productId, file, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importProductInsurances(Long productId, MultipartFile file) {
        return processProductInsuranceTemplate(productId, file, false).size();
    }

    private List<ProductInsurancePreviewVO> processProductInsuranceTemplate(Long productId, MultipartFile file, boolean previewOnly) {
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

        AxxProductEntity product = requireProduct(productId);

        List<List<String>> rows;
        try {
            rows = SimpleXlsxReader.readFirstSheet(file.getInputStream());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "Excel文件解析失败：" + e.getMessage());
        }

        if (rows.size() <= 1) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "Excel中没有可导入的数据");
        }

        Map<String, Integer> columnIndexes = buildProductInsuranceColumnIndexes(rows.get(0));

        List<ProductInsurancePreviewVO> previewRows = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (shouldSkipProductInsuranceRow(row, columnIndexes)) {
                continue;
            }
            ActivateRequest request = buildInsuranceImportRequest(product, row, i + 1, columnIndexes);
            previewRows.add(buildProductInsurancePreviewRow(request, i + 1));
            if (!previewOnly) {
                activateProduct(request, currentUserId());
            }
        }
        if (!previewOnly && previewRows.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "Excel中没有可导入的数据，请先填写模板后再上传");
        }
        return previewRows;
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

    @Override
    public void updateDisplayPrice(Long insuranceId, BigDecimal displayPrice, Long userId) {
        InsuranceRecordEntity insurance = insuranceRecordMapper.selectById(insuranceId);
        if (insurance == null || !userId.equals(insurance.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "保险记录不存在");
        }
        insurance.setDisplayPrice(displayPrice);
        insurance.setUpdateTime(LocalDateTime.now());
        insuranceRecordMapper.updateById(insurance);

        // 同步更新关联的费用记录
        if (insurance.getExpenseId() != null) {
            ExpenseRecordEntity expense = expenseRecordMapper.selectById(insurance.getExpenseId());
            if (expense != null && userId.equals(expense.getUserId())) {
                expense.setDisplayPrice(displayPrice);
                expense.setUpdateTime(LocalDateTime.now());
                expenseRecordMapper.updateById(expense);
            }
        }
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
        newBalance.setVersion(0);
        newBalance.setCreateTime(LocalDateTime.now());
        newBalance.setUpdateTime(LocalDateTime.now());
        accountBalanceMapper.insert(newBalance);
        return accountBalanceMapper.selectById(newBalance.getId());
    }

    private int normalizeQuantity(Integer count) {
        return count == null || count <= 0 ? 1 : count;
    }

    private BigDecimal resolvePremiumAmount(AxxProductEntity product) {
        BigDecimal price = product.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "产品原始价格必须大于0");
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveDisplayPrice(ActivateRequest request, AxxProductEntity product) {
        BigDecimal displayPrice = request.getDisplayPrice();
        if (displayPrice == null) {
            displayPrice = product.getDisplayPrice() != null ? product.getDisplayPrice() : product.getPrice();
        }
        if (displayPrice == null || displayPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "显示价格必须大于0");
        }
        return displayPrice.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalAmount(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private ExpenseRecordEntity buildExpenseRecord(ActivateRequest request,
                                                   Long userId,
                                                   AxxProductEntity product,
                                                   BigDecimal premiumAmount,
                                                   BigDecimal displayPrice,
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
        expense.setPremiumAmount(premiumAmount);
        expense.setDisplayPrice(displayPrice);
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
                                                       BigDecimal premiumAmount,
                                                       BigDecimal displayPrice,
                                                       int quantity,
                                                       InsuranceStatus status,
                                                       Long expenseId) {
        LocalDateTime now = LocalDateTime.now();
        InsuranceRecordEntity insurance = new InsuranceRecordEntity();
        insurance.setExpenseId(expenseId);
        insurance.setUserId(userId);
        insurance.setProductName(product.getProductName());
        insurance.setInsuredName(request.getPolicyHolderName());
        insurance.setInsuredIdNo(request.getPolicyHolderId());
        insurance.setBeneficiaryName(request.getBeneficiaryName());
        insurance.setBeneficiaryIdNo(request.getBeneficiaryId());
        insurance.setBeneficiaryJob(request.getBeneficiaryJob());
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
        insurance.setPremiumAmount(premiumAmount);
        insurance.setDisplayPrice(displayPrice);
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

    private RechargeVO convertToRechargeVO(TransactionRecordEntity entity) {
        return RechargeVO.builder()
                .id(entity.getId())
                .username(authContext.requireCurrentUser().getUsername())
                .date(entity.getCreateTime().toLocalDate().toString())
                .amount(entity.getAmount())
                .type(entity.getTransType().toLowerCase())
                .description(entity.getDescription())
                .serial(entity.getSerialNo())
                .balance(entity.getBalanceAfter())
                .build();
    }

    private LambdaQueryWrapper<ExpenseRecordEntity> buildExpenseWrapper(ExpenseQuery query, Long userId) {
        LambdaQueryWrapper<ExpenseRecordEntity> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(ExpenseRecordEntity::getUserId, userId);
        }
        if (userId == null && (query == null || query.getStatus() == null || query.getStatus().isBlank() || "all".equalsIgnoreCase(query.getStatus()))) {
            wrapper.ne(ExpenseRecordEntity::getExpenseStatus, InsuranceStatus.DRAFT.getCode());
        }
        applyExpenseFilters(wrapper, query);
        return wrapper;
    }

    private LambdaQueryWrapper<InsuranceRecordEntity> buildInsuranceWrapper(InsuranceQuery query, Long userId) {
        LambdaQueryWrapper<InsuranceRecordEntity> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(InsuranceRecordEntity::getUserId, userId);
        }
        if (userId == null && (query == null || query.getStatus() == null || query.getStatus().isBlank() || "all".equalsIgnoreCase(query.getStatus()))) {
            wrapper.ne(InsuranceRecordEntity::getInsuranceStatus, InsuranceStatus.DRAFT.getCode());
        }
        applyInsuranceFilters(wrapper, query);
        return wrapper;
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
                                    Date startDate,
                                    Date endDate,
                                    SFunction<T, LocalDateTime> columnGetter) {
        if (startDate != null) {
            wrapper.ge(columnGetter, toStartOfDay(startDate));
        }
        if (endDate != null) {
            wrapper.le(columnGetter, toEndOfDay(endDate));
        }
    }

    private LocalDateTime toStartOfDay(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay();
    }

    private LocalDateTime toEndOfDay(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate().atTime(LocalTime.MAX);
    }

    private Long currentUserId() {
        return authContext.requireCurrentUser().getUserId();
    }

    private boolean isAdmin() {
        return "ADMIN".equals(authContext.requireCurrentUser().getUserType());
    }

    private InsuranceRecordEntity requireInsuranceVisible(Long insuranceId) {
        InsuranceRecordEntity insurance = insuranceRecordMapper.selectById(insuranceId);
        if (insurance == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "保险记录不存在");
        }
        if (!isAdmin() && !currentUserId().equals(insurance.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "保险记录不存在");
        }
        return insurance;
    }

    private List<String> expenseExportHeaders() {
        return List.of("分销商ID", "序列号", "产品名称", "联系人", "手机号", "状态", "保单号", "起保日期", "结束日期", "份数", "单价", "金额", "创建时间");
    }

    private List<String> toExpenseExportRow(ExpenseRecordEntity entity) {
        return List.of(
                String.valueOf(entity.getUserId()),
                defaultText(entity.getSerialNo()),
                defaultText(entity.getProductName()),
                defaultText(entity.getContactName()),
                defaultText(entity.getContactMobile()),
                defaultText(mapStatus(entity.getExpenseStatus())),
                policyNoText(entity.getPolicyNo(), entity.getExpenseStatus()),
                entity.getEffectiveDate() == null ? "" : entity.getEffectiveDate().toString(),
                entity.getExpiryDate() == null ? "" : entity.getExpiryDate().toString(),
                entity.getQuantity() == null ? "" : String.valueOf(entity.getQuantity()),
                entity.getPremiumAmount() == null ? "" : entity.getPremiumAmount().toPlainString(),
                entity.getTotalAmount() == null ? "" : entity.getTotalAmount().toPlainString(),
                entity.getCreateTime() == null ? "" : entity.getCreateTime().toString()
        );
    }

    private List<SimplePdfWriter.FieldLine> toInsurancePdfFields(InsuranceRecordEntity entity) {
        return List.of(
                new SimplePdfWriter.FieldLine("分销商ID", String.valueOf(entity.getUserId())),
                new SimplePdfWriter.FieldLine("保单号", policyNoText(entity.getPolicyNo(), entity.getInsuranceStatus())),
                new SimplePdfWriter.FieldLine("产品名称", defaultText(entity.getProductName())),
                new SimplePdfWriter.FieldLine("投保人", defaultText(entity.getInsuredName())),
                new SimplePdfWriter.FieldLine("投保人证件号", defaultText(entity.getInsuredIdNo())),
                new SimplePdfWriter.FieldLine("被保人", defaultText(entity.getBeneficiaryName())),
                new SimplePdfWriter.FieldLine("被保人证件号", defaultText(entity.getBeneficiaryIdNo())),
                new SimplePdfWriter.FieldLine("业务员", defaultText(entity.getAgentName())),
                new SimplePdfWriter.FieldLine("状态", defaultText(mapStatus(entity.getInsuranceStatus()))),
                new SimplePdfWriter.FieldLine("起保日期", entity.getEffectiveDate() == null ? "" : entity.getEffectiveDate().toString()),
                new SimplePdfWriter.FieldLine("结束日期", entity.getExpiryDate() == null ? "" : entity.getExpiryDate().toString()),
                new SimplePdfWriter.FieldLine("份数", entity.getQuantity() == null ? "" : String.valueOf(entity.getQuantity())),
                new SimplePdfWriter.FieldLine("显示价格", entity.getDisplayPrice() != null ? entity.getDisplayPrice().toPlainString() : (entity.getPremiumAmount() == null ? "" : entity.getPremiumAmount().toPlainString())),
                new SimplePdfWriter.FieldLine("审核人", defaultText(entity.getReviewerName())),
                new SimplePdfWriter.FieldLine("审核时间", entity.getReviewTime() == null ? "" : entity.getReviewTime().toString()),
                new SimplePdfWriter.FieldLine("承保时间", entity.getUnderwritingTime() == null ? "" : entity.getUnderwritingTime().toString()),
                new SimplePdfWriter.FieldLine("生效时间", entity.getActivateTime() == null ? "" : entity.getActivateTime().toString())
        );
    }

    private SimplePdfWriter.PolicyPage renderInsurancePolicyPage(InsuranceRecordEntity insurance) {
        ExpenseRecordEntity expense = insurance.getExpenseId() == null ? null : expenseRecordMapper.selectById(insurance.getExpenseId());
        AxxProductEntity product = expense == null || expense.getProductId() == null
                ? null
                : axxProductMapper.selectById(expense.getProductId());
        String policyNoText = policyNoText(insurance.getPolicyNo(), insurance.getInsuranceStatus());

        String title = firstNonBlank(
                product != null ? product.getProductName() : null,
                insurance.getProductName(),
                "保险清单");
        String subtitle = "";

        List<SimplePdfWriter.FieldLine> summaryFields = List.of(
                new SimplePdfWriter.FieldLine("保单号码", policyNoText),
                new SimplePdfWriter.FieldLine("保险期间", formatPolicyPeriod(insurance)),
                new SimplePdfWriter.FieldLine("被保险人姓名", defaultText(insurance.getBeneficiaryName())),
                new SimplePdfWriter.FieldLine("被保险人证件号码", defaultText(insurance.getBeneficiaryIdNo())),
                new SimplePdfWriter.FieldLine("投保人姓名", defaultText(insurance.getInsuredName())),
                new SimplePdfWriter.FieldLine("投保人证件号码", defaultText(insurance.getInsuredIdNo())),
                new SimplePdfWriter.FieldLine("受益人", defaultText(insurance.getBeneficiaryName())),
                new SimplePdfWriter.FieldLine("受益人证件号码", defaultText(insurance.getBeneficiaryIdNo())),
                new SimplePdfWriter.FieldLine("保费合计", formatMoney(firstNonNull(insurance.getDisplayPrice(), insurance.getPremiumAmount()))),
                new SimplePdfWriter.FieldLine("份数", insurance.getQuantity() == null ? "1" : String.valueOf(insurance.getQuantity()))
        );

        List<SimplePdfWriter.FieldLine> coverageRows = buildCoverageRows(product, insurance);
        List<String> notes = buildPolicyNotes(product, insurance);
        List<String> footers = List.of(
                "承保公司：" + firstNonBlank(product != null ? product.getCompanyName() : null, "-"),
                "服务电话：400-890-9999",
                "注意：仅需使用保单号报案及理赔即可"
        );

        String introText = buildPolicyIntroText(product, insurance);
        BufferedImage image = SimplePdfWriter.renderInsurancePolicyPage(
                title,
                subtitle,
                "尊敬的客户：",
                introText,
                summaryFields,
                coverageRows,
                notes,
                footers);
        return new SimplePdfWriter.PolicyPage(image, buildHiddenPolicyText(title, subtitle, introText, summaryFields, coverageRows, notes, footers));
    }

    private List<SimplePdfWriter.FieldLine> buildCoverageRows(AxxProductEntity product, InsuranceRecordEntity insurance) {
        String coverageText = product != null ? product.getFeatures() : null;
        List<SimplePdfWriter.FieldLine> rows = parseCoverageRows(coverageText);
        if (!rows.isEmpty()) {
            return rows;
        }
        return List.of(new SimplePdfWriter.FieldLine("保障内容", "请以产品详情页及正式保险条款为准"));
    }

    private List<String> buildPolicyNotes(AxxProductEntity product, InsuranceRecordEntity insurance) {
        List<String> notes = buildDynamicPolicyNotes(product);
        if (insurance.getReviewComment() != null && !insurance.getReviewComment().isBlank()) {
            notes.add("## 审核意见");
            notes.add(insurance.getReviewComment().trim());
        }
        if (insurance.getRejectReason() != null && !insurance.getRejectReason().isBlank()) {
            notes.add("## 驳回原因");
            notes.add(insurance.getRejectReason().trim());
        }
        return notes;
    }

    private List<String> buildDynamicPolicyNotes(AxxProductEntity product) {
        String detailText = firstNonBlank(product != null ? product.getDetailText() : null,
                product != null ? product.getDescription() : null);
        List<String> notes = normalizePolicyNoteLines(detailText);
        if (notes.isEmpty()) {
            return new ArrayList<>(List.of(
                    "## 其他说明",
                    "具体保障责任、特别约定和免除责任请以产品详情页及正式保险条款为准。"
            ));
        }
        boolean hasHeading = notes.stream().anyMatch(line -> line.startsWith("##"));
        if (!hasHeading) {
            notes.add(0, "## 产品说明");
        }
        return notes;
    }

    private String buildPolicyIntroText(AxxProductEntity product, InsuranceRecordEntity insurance) {
        String productName = firstNonBlank(product != null ? product.getProductName() : null, insurance.getProductName(), "本保险产品");
        String companyName = firstNonBlank(product != null ? product.getCompanyName() : null, "承保公司");
        String displayPrice = formatMoney(firstNonNull(insurance.getDisplayPrice(), insurance.getPremiumAmount()));
        return "感谢您委托我代为办理保险业务。为了保护您的合法权益，请知悉保险条款，重点关注保险责任、投保须知、责任免除、免赔额、健康保险产品等待期等内容，并可要求业务销售人员对上述内容进行详细讲解。\n"
                + "本保单由" + companyName + "承保，产品名称为“" + productName + "”，当前保费合计为" + displayPrice + "，请妥善保管本保单并以保险公司官网公示内容为准。";
    }

    private String policyNoText(String policyNo, String status) {
        if (InsuranceStatus.UNDERWRITING.getCode().equals(status)) {
            return "";
        }
        return defaultText(policyNo);
    }

    private List<SimplePdfWriter.FieldLine> parseCoverageRows(String text) {
        List<SimplePdfWriter.FieldLine> rows = new ArrayList<>();
        for (String line : extractTextLines(text)) {
            String normalized = line.replaceAll("^[0-9]+[、.．)]?\\s*", "").trim();
            if (normalized.isBlank()) {
                continue;
            }
            String[] tokens = normalized.split("[:：]", 2);
            if (tokens.length == 2) {
                rows.add(new SimplePdfWriter.FieldLine(tokens[0].trim(), tokens[1].trim()));
                continue;
            }
            int amountIndex = findAmountIndex(normalized);
            if (amountIndex > 0) {
                String label = normalized.substring(0, amountIndex).trim();
                String value = normalized.substring(amountIndex).trim();
                rows.add(new SimplePdfWriter.FieldLine(label.isBlank() ? normalized : label, value));
            } else {
                rows.add(new SimplePdfWriter.FieldLine(normalized, "-"));
            }
        }
        return rows;
    }

    private List<String> normalizePolicyNoteLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String raw : extractTextLines(text)) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("#")) {
                String heading = line.replaceFirst("^#+\\s*", "").trim();
                if (!heading.isEmpty()) {
                    lines.add("## " + heading);
                }
                continue;
            }
            String heading = normalizePolicyHeading(line);
            if (heading != null) {
                lines.add("## " + heading);
                continue;
            }
            lines.add(line);
        }
        return lines;
    }

    private String normalizePolicyHeading(String line) {
        if (line == null) {
            return null;
        }
        String normalized = line.trim().replaceAll("[：:]$", "");
        if (normalized.isEmpty() || normalized.length() > 24) {
            return null;
        }
        if (normalized.matches("^第[一二三四五六七八九十百零0-9]+[章节条].*")) {
            return normalized;
        }
        List<String> headings = Arrays.asList(
                "特别约定",
                "其他说明",
                "免除责任",
                "责任免除",
                "投保须知",
                "保险责任",
                "保障责任",
                "保障内容",
                "产品说明",
                "理赔说明",
                "理赔流程",
                "注意事项",
                "温馨提示",
                "特别提示",
                "免责条款"
        );
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String heading : headings) {
            if (normalized.equals(heading) || lower.equals(heading.toLowerCase(Locale.ROOT))) {
                return heading;
            }
        }
        return null;
    }

    private List<String> buildHiddenPolicyText(String title,
                                               String subtitle,
                                               String introText,
                                               List<SimplePdfWriter.FieldLine> summaryFields,
                                               List<SimplePdfWriter.FieldLine> coverageRows,
                                               List<String> notes,
                                               List<String> footerLines) {
        List<String> lines = new ArrayList<>();
        lines.add("__TITLE__:" + title);
        lines.add("__SUBTITLE__:" + subtitle);
        lines.add("__INTRO_LABEL__:尊敬的客户：");
        lines.addAll(extractTextLines(introText));
        lines.add("__SECTION_SUMMARY__");
        for (SimplePdfWriter.FieldLine field : summaryFields) {
            lines.add(field.label() + "：" + field.value());
        }
        lines.add("__SECTION_COVERAGE__");
        for (SimplePdfWriter.FieldLine row : coverageRows) {
            lines.add(row.label() + "：" + row.value());
        }
        lines.add("__SECTION_NOTES__");
        for (String note : notes) {
            lines.add(note);
        }
        lines.add("__SECTION_FOOTER__");
        for (String footer : footerLines) {
            lines.add(footer);
        }
        return lines;
    }

    private List<String> extractTextLines(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private int findAmountIndex(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private String formatPolicyPeriod(InsuranceRecordEntity insurance) {
        String start = insurance.getEffectiveDate() == null ? "-" : insurance.getEffectiveDate().toString();
        String end = insurance.getExpiryDate() == null ? "-" : insurance.getExpiryDate().toString();
        return "自 " + start + " 起至 " + end;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "元";
    }

    private BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private List<String> productInsuranceTemplateHeaders() {
        return List.of("方案名称", "被保人姓名", "被保人证件号", "被保人职业", "份数", "地址", "业务员");
    }

    private void validateProductInsuranceTemplateHeaders(List<String> headers) {
        buildProductInsuranceColumnIndexes(headers);
    }

    private Map<String, Integer> buildProductInsuranceColumnIndexes(List<String> headers) {
        Map<String, Integer> indexes = new HashMap<>();
        indexes.put("planName", findHeaderIndex(headers, "方案名称", "产品名称", "计划名称"));
        indexes.put("beneficiaryName", findHeaderIndex(headers, "被保人姓名", "被保险人姓名", "姓名"));
        indexes.put("beneficiaryId", findHeaderIndex(headers, "被保人证件号", "被保人证件号码", "证件号", "证件号码"));
        indexes.put("beneficiaryJobCategory", findHeaderIndex(headers, "被保人职业", "职业类别", "职业"));
        indexes.put("beneficiaryJobDetail", findHeaderIndex(headers, "具体职业", "职业明细"));
        indexes.put("count", findHeaderIndex(headers, "份数", "数量"));
        indexes.put("address", findHeaderIndex(headers, "地址", "联系地址"));
        indexes.put("agent", findHeaderIndex(headers, "业务员", "销售员", "代理人"));

        List<String> missing = new ArrayList<>();
        if (indexes.get("planName") < 0) {
            missing.add("方案名称");
        }
        if (indexes.get("beneficiaryName") < 0) {
            missing.add("被保人姓名");
        }
        if (indexes.get("beneficiaryId") < 0) {
            missing.add("被保人证件号");
        }
        if (indexes.get("count") < 0) {
            missing.add("份数");
        }
        if (indexes.get("agent") < 0) {
            missing.add("业务员");
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "Excel模板表头不正确，缺少：" + String.join("、", missing));
        }
        return indexes;
    }

    private int findHeaderIndex(List<String> headers, String... aliases) {
        if (headers == null || headers.isEmpty() || aliases == null || aliases.length == 0) {
            return -1;
        }
        for (int i = 0; i < headers.size(); i++) {
            String header = normalizeHeader(headers.get(i));
            if (header.isEmpty()) {
                continue;
            }
            for (String alias : aliases) {
                if (header.equals(normalizeHeader(alias))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private ActivateRequest buildInsuranceImportRequest(AxxProductEntity product,
                                                        List<String> row,
                                                        int rowNumber,
                                                        Map<String, Integer> columnIndexes) {
        ActivateRequest request = new ActivateRequest();
        request.setProductId(product.getId());
        String planName = normalizeCell(getCell(row, columnIndexes.get("planName")));
        request.setPlanName(planName.isBlank() ? product.getProductName() : planName);
        request.setBeneficiaryName(requireCell(row, columnIndexes.get("beneficiaryName"), rowNumber, "被保人姓名"));
        request.setBeneficiaryId(requireCell(row, columnIndexes.get("beneficiaryId"), rowNumber, "被保人证件号"));
        String beneficiaryJobCategory = normalizeCell(getCell(row, columnIndexes.get("beneficiaryJobCategory")));
        String beneficiaryJobDetail = normalizeCell(getCell(row, columnIndexes.get("beneficiaryJobDetail")));
        String beneficiaryJob = !beneficiaryJobDetail.isBlank()
                ? beneficiaryJobDetail
                : beneficiaryJobCategory;
        request.setBeneficiaryJob(beneficiaryJob.isBlank() ? "1" : beneficiaryJob);
        request.setCount(parseInteger(getCell(row, columnIndexes.get("count")), rowNumber, "份数", 1));
        request.setAgent(normalizeCell(getCell(row, columnIndexes.get("agent"))));
        return request;
    }

    private ProductInsurancePreviewVO buildProductInsurancePreviewRow(ActivateRequest request, int rowNumber) {
        return ProductInsurancePreviewVO.builder()
                .rowNumber(rowNumber)
                .planName(defaultText(request.getPlanName()))
                .beneficiaryName(defaultText(request.getBeneficiaryName()))
                .beneficiaryId(defaultText(request.getBeneficiaryId()))
                .beneficiaryJob(defaultText(request.getBeneficiaryJob()))
                .count(request.getCount())
                .agent(defaultText(request.getAgent()))
                .build();
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String requireCell(List<String> row, Integer index, int rowNumber, String fieldName) {
        if (index == null || index < 0) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "第" + rowNumber + "行【" + fieldName + "】列不存在");
        }
        String value = normalizeCell(getCell(row, index));
        if (value.isBlank()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "第" + rowNumber + "行【" + fieldName + "】不能为空");
        }
        return value;
    }

    private String getCell(List<String> row, Integer index) {
        if (index == null || index < 0) {
            return "";
        }
        if (row == null || index < 0 || index >= row.size()) {
            return "";
        }
        String value = row.get(index);
        return value == null ? "" : value;
    }

    private String normalizeHeader(String value) {
        return normalizeCell(value).replaceAll("\\s+", "");
    }

    private String normalizeCell(String value) {
        return value == null ? "" : value.trim();
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

    private boolean shouldSkipProductInsuranceRow(List<String> row, Map<String, Integer> columnIndexes) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        return isBlankValue(row, columnIndexes.get("beneficiaryName"))
                || isBlankValue(row, columnIndexes.get("beneficiaryId"));
    }

    private boolean isBlankValue(List<String> row, Integer index) {
        return normalizeCell(getCell(row, index)).isBlank();
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
