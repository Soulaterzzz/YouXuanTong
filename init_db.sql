-- ================================================
-- 优选通数据库初始化脚本
-- 版本: 2.0.0
-- 说明: 包含完整表结构和全流程测试数据
-- ================================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS axx_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    mobile VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    user_type VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '用户类型：ADMIN-管理员，USER-普通用户',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常，DISABLED-禁用',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_mobile (mobile),
    KEY idx_user_type (user_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优选通用户表';

-- 2. 账户余额表
CREATE TABLE IF NOT EXISTS axx_account_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    balance DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    frozen_balance DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户余额表';

-- 3. 充值消费明细表
CREATE TABLE IF NOT EXISTS axx_transaction_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    serial_no VARCHAR(64) NOT NULL COMMENT '流水号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    trans_type VARCHAR(32) NOT NULL COMMENT '交易类型：RECHARGE-充值，EXPENSE-消费',
    amount DECIMAL(18,2) NOT NULL COMMENT '交易金额',
    balance_before DECIMAL(18,2) NOT NULL COMMENT '交易前余额',
    balance_after DECIMAL(18,2) NOT NULL COMMENT '交易后余额',
    description VARCHAR(500) DEFAULT NULL COMMENT '交易说明',
    ref_type VARCHAR(32) DEFAULT NULL COMMENT '关联类型',
    ref_id BIGINT DEFAULT NULL COMMENT '关联ID',
    payment_method VARCHAR(32) DEFAULT NULL COMMENT '支付方式',
    payment_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '支付状态',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_serial_no (serial_no),
    KEY idx_user_time (user_id, create_time),
    KEY idx_ref (ref_type, ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值消费明细表';

-- 4. 费用清单表（激活记录）
CREATE TABLE IF NOT EXISTS axx_expense_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    serial_no VARCHAR(64) NOT NULL COMMENT '序列号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人',
    contact_mobile VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    expense_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '状态：DRAFT-待提交，PENDING_REVIEW-待审核，APPROVED-审核通过，REVIEW_REJECTED-审核驳回，UNDERWRITING-承保中，ACTIVE-已生效，CANCELLED-已取消',
    policy_no VARCHAR(64) DEFAULT NULL COMMENT '电子保单号',
    premium_amount DECIMAL(18,2) NOT NULL COMMENT '保费金额',
    quantity INT NOT NULL DEFAULT 1 COMMENT '份数',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '总金额',
    effective_date DATE DEFAULT NULL COMMENT '起保日期',
    expiry_date DATE DEFAULT NULL COMMENT '结束日期',
    export_time DATETIME DEFAULT NULL COMMENT '导出时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_serial_no (serial_no),
    KEY idx_user_status (user_id, expense_status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用清单表';

-- 5. 保险清单表（被保人信息）
CREATE TABLE IF NOT EXISTS axx_insurance_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    expense_id BIGINT NOT NULL COMMENT '费用记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
    insured_name VARCHAR(64) NOT NULL COMMENT '投保人姓名',
    insured_id_no VARCHAR(64) DEFAULT NULL COMMENT '投保人证件号',
    insured_mobile VARCHAR(32) DEFAULT NULL COMMENT '投保人手机号',
    beneficiary_name VARCHAR(64) NOT NULL COMMENT '被保人姓名',
    beneficiary_id_no VARCHAR(64) DEFAULT NULL COMMENT '被保人证件号',
    beneficiary_mobile VARCHAR(32) DEFAULT NULL COMMENT '被保人手机号',
    beneficiary_job VARCHAR(64) DEFAULT NULL COMMENT '被保人职业',
    beneficiary_address VARCHAR(255) DEFAULT NULL COMMENT '被保人地址',
    agent_name VARCHAR(64) DEFAULT NULL COMMENT '业务员',
    insurance_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '状态：DRAFT-待提交，PENDING_REVIEW-待审核，APPROVED-审核通过，REVIEW_REJECTED-审核驳回，UNDERWRITING-承保中，ACTIVE-已生效，EXPIRED-已过期，CANCELLED-已取消',
    review_comment VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    reviewer_id BIGINT DEFAULT NULL COMMENT '审核人ID',
    reviewer_name VARCHAR(64) DEFAULT NULL COMMENT '审核人姓名',
    review_time DATETIME DEFAULT NULL COMMENT '审核时间',
    reject_reason VARCHAR(500) DEFAULT NULL COMMENT '驳回原因',
    submit_time DATETIME DEFAULT NULL COMMENT '提交审核时间',
    underwriting_time DATETIME DEFAULT NULL COMMENT '进入承保时间',
    activate_time DATETIME DEFAULT NULL COMMENT '保单生效操作时间',
    policy_no VARCHAR(64) DEFAULT NULL COMMENT '电子保单号',
    premium_amount DECIMAL(18,2) NOT NULL COMMENT '保费金额',
    quantity INT NOT NULL DEFAULT 1 COMMENT '份数',
    effective_date DATE DEFAULT NULL COMMENT '起保日期',
    expiry_date DATE DEFAULT NULL COMMENT '结束日期',
    export_time DATETIME DEFAULT NULL COMMENT '导出时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_expense_id (expense_id),
    KEY idx_user_status (user_id, insurance_status),
    KEY idx_beneficiary (beneficiary_name, beneficiary_id_no),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保险清单表';

-- 6. 产品表（优选通专用）
CREATE TABLE IF NOT EXISTS axx_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    product_code VARCHAR(64) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
    category_code VARCHAR(64) NOT NULL COMMENT '分类编码',
    company_name VARCHAR(128) DEFAULT NULL COMMENT '承保公司',
    description VARCHAR(500) DEFAULT NULL COMMENT '产品描述',
    features VARCHAR(500) DEFAULT NULL COMMENT '产品特点',
    price DECIMAL(18,2) NOT NULL COMMENT '价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    is_new TINYINT NOT NULL DEFAULT 0 COMMENT '是否新品',
    is_hot TINYINT NOT NULL DEFAULT 0 COMMENT '是否热门',
    sale_status VARCHAR(32) NOT NULL DEFAULT 'ON_SALE' COMMENT '销售状态',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    image_url VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
    image_path VARCHAR(500) DEFAULT NULL COMMENT '图片路径',
    image_content_type VARCHAR(100) DEFAULT NULL COMMENT '图片类型',
    image_size BIGINT DEFAULT NULL COMMENT '图片大小',
    template_file_name VARCHAR(255) DEFAULT NULL COMMENT '模板文件名',
    template_file_path VARCHAR(500) DEFAULT NULL COMMENT '模板文件路径',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_code_deleted (product_code, deleted),
    KEY idx_category (category_code),
    KEY idx_sale_status (sale_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优选通产品表';

-- 7. 通知公告表
CREATE TABLE IF NOT EXISTS axx_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    title VARCHAR(64) NOT NULL COMMENT '通知标题',
    content VARCHAR(1000) NOT NULL COMMENT '通知内容',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    published_at DATETIME NOT NULL COMMENT '发布时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_notice_sort_no (sort_no),
    KEY idx_notice_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页通知公告表';

-- ================================================
-- 测试数据
-- ================================================

-- 清空现有数据（按依赖顺序）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE axx_insurance_record;
TRUNCATE TABLE axx_expense_record;
TRUNCATE TABLE axx_transaction_record;
TRUNCATE TABLE axx_account_balance;
TRUNCATE TABLE axx_user;
TRUNCATE TABLE axx_product;
TRUNCATE TABLE axx_notice;
SET FOREIGN_KEY_CHECKS = 1;

-- ================================================
-- 1. 产品数据（8款保险产品）
-- ================================================
INSERT INTO axx_product (product_code, product_name, category_code, company_name, description, features, price, stock, is_new, is_hot, sale_status, sort_no, create_time, update_time) VALUES
('GS-001', '国寿财1-3类10+5+5+50（青柑）', '1-3', '国寿财险', '个单：18-63周岁；可投保：1-3类职业；T+1生效；每周一三五十点截单', '无门诊免赔额；承保动物伤害；意外医疗全额赔付', 55.00, 999, 1, 1, 'ON_SALE', 1, NOW(), NOW()),
('PA-001', '平安财意外10+1+50+10（含意保）', '1-3', '平安财险', '1-3类职业可投；每周一三五十十点截单', '限投一份；正常1+6生效；包含住院津贴', 60.00, 999, 0, 1, 'ON_SALE', 2, NOW(), NOW()),
('ZA-001', '少儿门诊医疗险', 'child', '众安保险', '0-17岁可投保；门急诊可报销；等待期短', '门急诊保障；住院医疗；特定疾病额外赔付；不限社保', 380.00, 500, 1, 0, 'ON_SALE', 3, NOW(), NOW()),
('RB-001', '老年意外险（尊享版）', 'elder', '人保财险', '50-80岁可投保；综合意外保障；高龄专属', '意外医疗；骨折津贴；救护车费用；住院护理', 200.00, 999, 0, 1, 'ON_SALE', 4, NOW(), NOW()),
('GS-002', '国寿财1-4类意外险', '1-4', '国寿财险', '1-4类职业可投；保障全面；性价比高', '意外身故伤残；意外医疗；住院津贴', 80.00, 999, 0, 0, 'ON_SALE', 5, NOW(), NOW()),
('PA-002', '平安财高危职业意外险', '5-6', '平安财险', '5-6类高危职业专属；保障力度强', '高危职业可投；意外身故伤残；意外医疗', 150.00, 500, 1, 0, 'ON_SALE', 6, NOW(), NOW()),
('TP-001', '太保百万医疗险', 'medical', '太平洋保险', '一般医疗+重疾医疗；保额高达600万', '重疾绿通；住院垫付；质子重离子', 300.00, 999, 0, 1, 'ON_SALE', 7, NOW(), NOW()),
('ZA-002', '众安出行意外险', 'travel', '众安保险', '出行保障；飞机/火车/轮船/汽车全覆盖', '交通工具意外；航班延误；行李丢失', 50.00, 999, 0, 0, 'ON_SALE', 8, NOW(), NOW());

-- ================================================
-- 2. 用户数据（1管理员 + 12普通用户）
-- ================================================
INSERT INTO axx_user (id, username, password, mobile, user_type, status, last_login_time, create_time, update_time) VALUES
-- 管理员账户
(1, 'admin', 'admin123', '13800000001', 'ADMIN', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),

-- 普通用户账户（不同活跃程度）
(2, 'zhangwei', 'user123', '13800010001', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 60 DAY), NOW()),
(3, 'liming', 'user123', '13800010002', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 45 DAY), NOW()),
(4, 'wangfang', 'user123', '13800010003', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(5, 'liuyang', 'user123', '13800010004', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
(6, 'chenxin', 'user123', '13800010005', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
(7, 'zhaolei', 'user123', '13800010006', 'USER', 'ACTIVE', NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(8, 'sunli', 'user123', '13800010007', 'USER', 'ACTIVE', NULL, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(9, 'zhoujun', 'user123', '13800010008', 'USER', 'ACTIVE', NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(10, 'wuqiang', 'user123', '13800010009', 'USER', 'DISABLED', NULL, DATE_SUB(NOW(), INTERVAL 90 DAY), NOW()),
(11, 'zhenghua', 'user123', '13800010010', 'USER', 'ACTIVE', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(12, 'huangping', 'user123', '13800010011', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 25 DAY), NOW()),
(13, 'linxiao', 'user123', '13800010012', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 40 DAY), NOW());

-- ================================================
-- 3. 账户余额数据
-- ================================================
INSERT INTO axx_account_balance (user_id, balance, frozen_balance, create_by, create_time, update_by, update_time) VALUES
-- 管理员账户
(1, 100000.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 30 DAY), 1, NOW()),

-- 普通用户账户余额
(2, 8500.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 60 DAY), 2, NOW()),
(3, 12380.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 45 DAY), 3, NOW()),
(4, 5620.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 30 DAY), 4, NOW()),
(5, 19800.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 20 DAY), 5, NOW()),
(6, 7450.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 15 DAY), 6, NOW()),
(7, 3000.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), 7, NOW()),
(8, 15000.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), 8, NOW()),
(9, 680.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), 9, NOW()),
(10, 2500.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 90 DAY), 10, NOW()),
(11, 9200.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), 11, NOW()),
(12, 4350.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 25 DAY), 12, NOW()),
(13, 11200.00, 0.00, 1, DATE_SUB(NOW(), INTERVAL 40 DAY), 13, NOW());

-- ================================================
-- 4. 交易记录（充值记录）
-- ================================================
INSERT INTO axx_transaction_record (serial_no, user_id, trans_type, amount, balance_before, balance_after, description, ref_type, ref_id, payment_method, payment_status, create_by, create_time, update_by, update_time) VALUES
-- 用户2 张伟的充值记录
('TRX202501010001', 2, 'RECHARGE', 10000.00, 0.00, 10000.00, '账户首次充值', NULL, NULL, 'WECHAT', 'SUCCESS', 2, DATE_SUB(NOW(), INTERVAL 60 DAY), 2, DATE_SUB(NOW(), INTERVAL 60 DAY)),
('TRX202501150001', 2, 'RECHARGE', 5000.00, 10000.00, 15000.00, '账户充值-微信支付', NULL, NULL, 'WECHAT', 'SUCCESS', 2, DATE_SUB(NOW(), INTERVAL 45 DAY), 2, DATE_SUB(NOW(), INTERVAL 45 DAY)),

-- 用户3 李明的充值记录
('TRX202501010002', 3, 'RECHARGE', 15000.00, 0.00, 15000.00, '账户首次充值', NULL, NULL, 'ALIPAY', 'SUCCESS', 3, DATE_SUB(NOW(), INTERVAL 45 DAY), 3, DATE_SUB(NOW(), INTERVAL 45 DAY)),

-- 用户4 王芳的充值记录
('TRX202501010003', 4, 'RECHARGE', 8000.00, 0.00, 8000.00, '账户首次充值', NULL, NULL, 'WECHAT', 'SUCCESS', 4, DATE_SUB(NOW(), INTERVAL 30 DAY), 4, DATE_SUB(NOW(), INTERVAL 30 DAY)),

-- 用户5 刘洋的充值记录
('TRX202501010004', 5, 'RECHARGE', 20000.00, 0.00, 20000.00, '账户首次充值', NULL, NULL, 'BANK', 'SUCCESS', 5, DATE_SUB(NOW(), INTERVAL 20 DAY), 5, DATE_SUB(NOW(), INTERVAL 20 DAY)),

-- 用户6 陈欣的充值记录
('TRX202501010005', 6, 'RECHARGE', 10000.00, 0.00, 10000.00, '账户首次充值', NULL, NULL, 'ALIPAY', 'SUCCESS', 6, DATE_SUB(NOW(), INTERVAL 15 DAY), 6, DATE_SUB(NOW(), INTERVAL 15 DAY)),

-- 用户7 赵磊的充值记录
('TRX202501010006', 7, 'RECHARGE', 5000.00, 0.00, 5000.00, '账户首次充值', NULL, NULL, 'WECHAT', 'SUCCESS', 7, DATE_SUB(NOW(), INTERVAL 10 DAY), 7, DATE_SUB(NOW(), INTERVAL 10 DAY)),

-- 用户8 孙丽的充值记录
('TRX202501010007', 8, 'RECHARGE', 20000.00, 0.00, 20000.00, '账户首次充值', NULL, NULL, 'ALIPAY', 'SUCCESS', 8, DATE_SUB(NOW(), INTERVAL 8 DAY), 8, DATE_SUB(NOW(), INTERVAL 8 DAY)),

-- 用户9 周军的充值记录
('TRX202501010008', 9, 'RECHARGE', 3000.00, 0.00, 3000.00, '账户首次充值', NULL, NULL, 'WECHAT', 'SUCCESS', 9, DATE_SUB(NOW(), INTERVAL 5 DAY), 9, DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- 用户10 吴强的充值记录（已禁用用户）
('TRX202501010009', 10, 'RECHARGE', 5000.00, 0.00, 5000.00, '账户首次充值', NULL, NULL, 'ALIPAY', 'SUCCESS', 10, DATE_SUB(NOW(), INTERVAL 90 DAY), 10, DATE_SUB(NOW(), INTERVAL 90 DAY)),

-- 用户11 郑华的充值记录
('TRX202501010010', 11, 'RECHARGE', 12000.00, 0.00, 12000.00, '账户首次充值', NULL, NULL, 'WECHAT', 'SUCCESS', 11, DATE_SUB(NOW(), INTERVAL 3 DAY), 11, DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- 用户12 黄平的充值记录
('TRX202501010011', 12, 'RECHARGE', 8000.00, 0.00, 8000.00, '账户首次充值', NULL, NULL, 'ALIPAY', 'SUCCESS', 12, DATE_SUB(NOW(), INTERVAL 25 DAY), 12, DATE_SUB(NOW(), INTERVAL 25 DAY)),

-- 用户13 林晓的充值记录
('TRX202501010012', 13, 'RECHARGE', 15000.00, 0.00, 15000.00, '账户首次充值', NULL, NULL, 'WECHAT', 'SUCCESS', 13, DATE_SUB(NOW(), INTERVAL 40 DAY), 13, DATE_SUB(NOW(), INTERVAL 40 DAY));

-- ================================================
-- 5. 费用清单记录（不同状态的保单）
-- ================================================
INSERT INTO axx_expense_record (serial_no, user_id, product_id, product_name, contact_name, contact_mobile, expense_status, policy_no, premium_amount, quantity, total_amount, effective_date, expiry_date, create_by, create_time, update_by, update_time) VALUES
-- 已生效的保单
('EXP202501010001', 2, 1, '国寿财1-3类10+5+5+50（青柑）', '张伟', '13800010001', 'ACTIVE', 'POL202501010001', 55.00, 2, 110.00, DATE_SUB(CURDATE(), INTERVAL 59 DAY), DATE_ADD(CURDATE(), INTERVAL 305 DAY), 2, DATE_SUB(NOW(), INTERVAL 60 DAY), 2, DATE_SUB(NOW(), INTERVAL 58 DAY)),
('EXP202501020001', 3, 2, '平安财意外10+1+50+10（含意保）', '李明', '13800010002', 'ACTIVE', 'POL202501020001', 60.00, 1, 60.00, DATE_SUB(CURDATE(), INTERVAL 44 DAY), DATE_ADD(CURDATE(), INTERVAL 320 DAY), 3, DATE_SUB(NOW(), INTERVAL 45 DAY), 3, DATE_SUB(NOW(), INTERVAL 43 DAY)),
('EXP202501030001', 4, 3, '少儿门诊医疗险', '王芳', '13800010003', 'ACTIVE', 'POL202501030001', 380.00, 1, 380.00, DATE_SUB(CURDATE(), INTERVAL 29 DAY), DATE_ADD(CURDATE(), INTERVAL 335 DAY), 4, DATE_SUB(NOW(), INTERVAL 30 DAY), 4, DATE_SUB(NOW(), INTERVAL 28 DAY)),
('EXP202501040001', 5, 4, '老年意外险（尊享版）', '刘洋', '13800010004', 'ACTIVE', 'POL202501040001', 200.00, 2, 400.00, DATE_SUB(CURDATE(), INTERVAL 19 DAY), DATE_ADD(CURDATE(), INTERVAL 345 DAY), 5, DATE_SUB(NOW(), INTERVAL 20 DAY), 5, DATE_SUB(NOW(), INTERVAL 18 DAY)),
('EXP202501050001', 6, 5, '国寿财1-4类意外险', '陈欣', '13800010005', 'ACTIVE', 'POL202501050001', 80.00, 1, 80.00, DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 350 DAY), 6, DATE_SUB(NOW(), INTERVAL 15 DAY), 6, DATE_SUB(NOW(), INTERVAL 13 DAY)),
('EXP202501060001', 8, 6, '平安财高危职业意外险', '孙丽', '13800010007', 'ACTIVE', 'POL202501060001', 150.00, 3, 450.00, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 357 DAY), 8, DATE_SUB(NOW(), INTERVAL 8 DAY), 8, DATE_SUB(NOW(), INTERVAL 6 DAY)),
('EXP202501070001', 11, 7, '太保百万医疗险', '郑华', '13800010010', 'ACTIVE', 'POL202501070001', 300.00, 1, 300.00, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 362 DAY), 11, DATE_SUB(NOW(), INTERVAL 3 DAY), 11, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 待审核的保单
('EXP202501080001', 12, 1, '国寿财1-3类10+5+5+50（青柑）', '黄平', '13800010011', 'PENDING_REVIEW', NULL, 55.00, 1, 55.00, NULL, NULL, 12, DATE_SUB(NOW(), INTERVAL 1 DAY), 12, NOW()),

-- 审核通过的保单（待承保）
('EXP202501090001', 13, 2, '平安财意外10+1+50+10（含意保）', '林晓', '13800010012', 'APPROVED', NULL, 60.00, 2, 120.00, NULL, NULL, 13, DATE_SUB(NOW(), INTERVAL 2 DAY), 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 审核驳回的保单
('EXP202501100001', 7, 3, '少儿门诊医疗险', '赵磊', '13800010006', 'REVIEW_REJECTED', NULL, 380.00, 1, 380.00, NULL, NULL, 7, DATE_SUB(NOW(), INTERVAL 5 DAY), 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- 承保中的保单
('EXP202501110001', 9, 8, '众安出行意外险', '周军', '13800010008', 'UNDERWRITING', NULL, 50.00, 2, 100.00, NULL, NULL, 9, DATE_SUB(NOW(), INTERVAL 3 DAY), 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- 已取消的保单
('EXP202501120001', 2, 4, '老年意外险（尊享版）', '张伟', '13800010001', 'CANCELLED', NULL, 200.00, 1, 200.00, NULL, NULL, 2, DATE_SUB(NOW(), INTERVAL 50 DAY), 2, DATE_SUB(NOW(), INTERVAL 48 DAY));

-- ================================================
-- 6. 保险清单记录（被保人详细信息）
-- ================================================
INSERT INTO axx_insurance_record (expense_id, user_id, product_name, insured_name, insured_id_no, insured_mobile, beneficiary_name, beneficiary_id_no, beneficiary_mobile, beneficiary_job, beneficiary_address, agent_name, insurance_status, review_comment, reviewer_id, reviewer_name, review_time, reject_reason, submit_time, underwriting_time, activate_time, policy_no, premium_amount, quantity, effective_date, expiry_date, create_by, create_time, update_by, update_time) VALUES
-- 费用清单1的保险记录（已生效）
(1, 2, '国寿财1-3类10+5+5+50（青柑）', '张伟', '110101199001011234', '13800010001', '张伟', '110101199001011234', '13800010001', '软件工程师', '北京市朝阳区望京街道', '系统管理员', 'ACTIVE', '审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 58 DAY), NULL, DATE_SUB(NOW(), INTERVAL 59 DAY), DATE_SUB(NOW(), INTERVAL 58 DAY), DATE_SUB(NOW(), INTERVAL 58 DAY), 'POL202501010001-01', 55.00, 1, DATE_SUB(CURDATE(), INTERVAL 59 DAY), DATE_ADD(CURDATE(), INTERVAL 305 DAY), 2, DATE_SUB(NOW(), INTERVAL 60 DAY), 2, DATE_SUB(NOW(), INTERVAL 58 DAY)),
(1, 2, '国寿财1-3类10+5+5+50（青柑）', '张伟', '110101199001011234', '13800010001', '张小妹', '110101201505052345', '13800010002', '学生', '北京市朝阳区望京街道', '系统管理员', 'ACTIVE', '审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 58 DAY), NULL, DATE_SUB(NOW(), INTERVAL 59 DAY), DATE_SUB(NOW(), INTERVAL 58 DAY), DATE_SUB(NOW(), INTERVAL 58 DAY), 'POL202501010001-02', 55.00, 1, DATE_SUB(CURDATE(), INTERVAL 59 DAY), DATE_ADD(CURDATE(), INTERVAL 305 DAY), 2, DATE_SUB(NOW(), INTERVAL 60 DAY), 2, DATE_SUB(NOW(), INTERVAL 58 DAY)),

-- 费用清单2的保险记录（已生效）
(2, 3, '平安财意外10+1+50+10（含意保）', '李明', '310101199202023456', '13800010002', '李明', '310101199202023456', '13800010002', '销售经理', '上海市浦东新区陆家嘴', '系统管理员', 'ACTIVE', '审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 43 DAY), NULL, DATE_SUB(NOW(), INTERVAL 44 DAY), DATE_SUB(NOW(), INTERVAL 43 DAY), DATE_SUB(NOW(), INTERVAL 43 DAY), 'POL202501020001', 60.00, 1, DATE_SUB(CURDATE(), INTERVAL 44 DAY), DATE_ADD(CURDATE(), INTERVAL 320 DAY), 3, DATE_SUB(NOW(), INTERVAL 45 DAY), 3, DATE_SUB(NOW(), INTERVAL 43 DAY)),

-- 费用清单3的保险记录（已生效）
(3, 4, '少儿门诊医疗险', '王芳', '440101198503033456', '13800010003', '王小宝', '440101201808084321', '13800010003', '学生', '广州市天河区珠江新城', '系统管理员', 'ACTIVE', '审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 28 DAY), NULL, DATE_SUB(NOW(), INTERVAL 29 DAY), DATE_SUB(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 28 DAY), 'POL202501030001', 380.00, 1, DATE_SUB(CURDATE(), INTERVAL 29 DAY), DATE_ADD(CURDATE(), INTERVAL 335 DAY), 4, DATE_SUB(NOW(), INTERVAL 30 DAY), 4, DATE_SUB(NOW(), INTERVAL 28 DAY)),

-- 费用清单4的保险记录（已生效，2份）
(4, 5, '老年意外险（尊享版）', '刘洋', '330101198005055678', '13800010004', '刘父', '330101195506066789', '13800010004', '退休', '杭州市西湖区文三路', '系统管理员', 'ACTIVE', '审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 18 DAY), NULL, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), 'POL202501040001-01', 200.00, 1, DATE_SUB(CURDATE(), INTERVAL 19 DAY), DATE_ADD(CURDATE(), INTERVAL 345 DAY), 5, DATE_SUB(NOW(), INTERVAL 20 DAY), 5, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(4, 5, '老年意外险（尊享版）', '刘洋', '330101198005055678', '13800010004', '刘母', '330101195808088901', '13800010005', '退休', '杭州市西湖区文三路', '系统管理员', 'ACTIVE', '审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 18 DAY), NULL, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), 'POL202501040001-02', 200.00, 1, DATE_SUB(CURDATE(), INTERVAL 19 DAY), DATE_ADD(CURDATE(), INTERVAL 345 DAY), 5, DATE_SUB(NOW(), INTERVAL 20 DAY), 5, DATE_SUB(NOW(), INTERVAL 18 DAY)),

-- 费用清单5的保险记录（已生效）
(5, 6, '国寿财1-4类意外险', '陈欣', '350101199207077890', '13800010005', '陈欣', '350101199207077890', '13800010005', '建筑工程师', '福州市鼓楼区五四路', '系统管理员', 'ACTIVE', '审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 13 DAY), NULL, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY), 'POL202501050001', 80.00, 1, DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 350 DAY), 6, DATE_SUB(NOW(), INTERVAL 15 DAY), 6, DATE_SUB(NOW(), INTERVAL 13 DAY)),

-- 费用清单6的保险记录（已生效，3份）
(6, 8, '平安财高危职业意外险', '孙丽', '510101199309099012', '13800010007', '孙丽', '510101199309099012', '13800010007', '电焊工', '成都市武侯区人民南路', '系统管理员', 'ACTIVE', '审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 'POL202501060001-01', 150.00, 1, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 357 DAY), 8, DATE_SUB(NOW(), INTERVAL 8 DAY), 8, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, 8, '平安财高危职业意外险', '孙丽', '510101199309099012', '13800010007', '孙强', '510101199501011234', '13800010008', '高空作业员', '成都市武侯区人民南路', '系统管理员', 'ACTIVE', '审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 'POL202501060001-02', 150.00, 1, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 357 DAY), 8, DATE_SUB(NOW(), INTERVAL 8 DAY), 8, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, 8, '平安财高危职业意外险', '孙丽', '510101199309099012', '13800010007', '孙梅', '510101199803033456', '13800010009', '化工操作员', '成都市武侯区人民南路', '系统管理员', 'ACTIVE', '审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 'POL202501060001-03', 150.00, 1, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 357 DAY), 8, DATE_SUB(NOW(), INTERVAL 8 DAY), 8, DATE_SUB(NOW(), INTERVAL 6 DAY)),

-- 费用清单7的保险记录（已生效）
(7, 11, '太保百万医疗险', '郑华', '420101199104044567', '13800010010', '郑华', '420101199104044567', '13800010010', '医生', '武汉市江汉区解放大道', '系统管理员', 'ACTIVE', '审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'POL202501070001', 300.00, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 362 DAY), 11, DATE_SUB(NOW(), INTERVAL 3 DAY), 11, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 费用清单8的保险记录（待审核）
(8, 12, '国寿财1-3类10+5+5+50（青柑）', '黄平', '610101199206066789', '13800010011', '黄平', '610101199206066789', '13800010011', '教师', '西安市雁塔区小寨路', NULL, 'PENDING_REVIEW', NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL, 55.00, 1, NULL, NULL, 12, DATE_SUB(NOW(), INTERVAL 1 DAY), 12, NOW()),

-- 费用清单9的保险记录（审核通过，待承保）
(9, 13, '平安财意外10+1+50+10（含意保）', '林晓', '370101199408088901', '13800010012', '林晓', '370101199408088901', '13800010012', '设计师', '青岛市市南区香港中路', NULL, 'APPROVED', '资料齐全，审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL, 60.00, 1, NULL, NULL, 13, DATE_SUB(NOW(), INTERVAL 2 DAY), 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(9, 13, '平安财意外10+1+50+10（含意保）', '林晓', '370101199408088901', '13800010012', '林小妹', '370101201909099012', '13800010013', '学生', '青岛市市南区香港中路', NULL, 'APPROVED', '资料齐全，审核通过', 1, 'admin', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL, 60.00, 1, NULL, NULL, 13, DATE_SUB(NOW(), INTERVAL 2 DAY), 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 费用清单10的保险记录（审核驳回）
(10, 7, '少儿门诊医疗险', '赵磊', '210101199105055678', '13800010006', '赵小宝', '210101202203033456', '13800010006', '婴幼儿', '沈阳市和平区中山路', NULL, 'REVIEW_REJECTED', NULL, 1, 'admin', DATE_SUB(NOW(), INTERVAL 4 DAY), '被保人年龄需满30天，当前年龄不符合投保要求', DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, NULL, NULL, 380.00, 1, NULL, NULL, 7, DATE_SUB(NOW(), INTERVAL 5 DAY), 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- 费用清单11的保险记录（承保中）
(11, 9, '众安出行意外险', '周军', '430101199507077890', '13800010008', '周军', '430101199507077890', '13800010008', '商务经理', '长沙市岳麓区麓谷大道', NULL, 'UNDERWRITING', '资料齐全，已提交承保', 1, 'admin', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, 50.00, 1, NULL, NULL, 9, DATE_SUB(NOW(), INTERVAL 3 DAY), 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(11, 9, '众安出行意外险', '周军', '430101199507077890', '13800010008', '周婷', '430101199808088901', '13800010009', '行政助理', '长沙市岳麓区麓谷大道', NULL, 'UNDERWRITING', '资料齐全，已提交承保', 1, 'admin', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, 50.00, 1, NULL, NULL, 9, DATE_SUB(NOW(), INTERVAL 3 DAY), 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- 费用清单12的保险记录（已取消）
(12, 2, '老年意外险（尊享版）', '张伟', '110101199001011234', '13800010001', '张父', '110101195003033456', '13800010003', '退休', '北京市朝阳区望京街道', NULL, 'CANCELLED', NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 50 DAY), NULL, NULL, NULL, 200.00, 1, NULL, NULL, 2, DATE_SUB(NOW(), INTERVAL 50 DAY), 2, DATE_SUB(NOW(), INTERVAL 48 DAY));

-- ================================================
-- 7. 交易记录（消费记录）
-- ================================================
INSERT INTO axx_transaction_record (serial_no, user_id, trans_type, amount, balance_before, balance_after, description, ref_type, ref_id, payment_method, payment_status, create_by, create_time, update_by, update_time) VALUES
-- 用户2 张伟的消费记录
('TRX202501010101', 2, 'EXPENSE', 110.00, 15000.00, 14890.00, '购买保险：国寿财1-3类10+5+5+50（青柑）x2', 'EXPENSE', 1, 'BALANCE', 'SUCCESS', 2, DATE_SUB(NOW(), INTERVAL 60 DAY), 2, DATE_SUB(NOW(), INTERVAL 60 DAY)),
('TRX202501010102', 2, 'EXPENSE', 200.00, 14890.00, 14690.00, '购买保险：老年意外险（尊享版）- 已取消', 'EXPENSE', 12, 'BALANCE', 'REFUNDED', 2, DATE_SUB(NOW(), INTERVAL 50 DAY), 2, DATE_SUB(NOW(), INTERVAL 48 DAY)),
('TRX202501010103', 2, 'RECHARGE', 200.00, 14690.00, 14890.00, '订单取消退款', 'REFUND', 12, 'BALANCE', 'SUCCESS', 2, DATE_SUB(NOW(), INTERVAL 48 DAY), 2, DATE_SUB(NOW(), INTERVAL 48 DAY)),

-- 用户3 李明的消费记录
('TRX202501010201', 3, 'EXPENSE', 60.00, 15000.00, 14940.00, '购买保险：平安财意外10+1+50+10（含意保）', 'EXPENSE', 2, 'BALANCE', 'SUCCESS', 3, DATE_SUB(NOW(), INTERVAL 45 DAY), 3, DATE_SUB(NOW(), INTERVAL 45 DAY)),

-- 用户4 王芳的消费记录
('TRX202501010301', 4, 'EXPENSE', 380.00, 8000.00, 7620.00, '购买保险：少儿门诊医疗险', 'EXPENSE', 3, 'BALANCE', 'SUCCESS', 4, DATE_SUB(NOW(), INTERVAL 30 DAY), 4, DATE_SUB(NOW(), INTERVAL 30 DAY)),
('TRX202501010302', 4, 'RECHARGE', 2000.00, 7620.00, 9620.00, '账户充值-微信支付', NULL, NULL, 'WECHAT', 'SUCCESS', 4, DATE_SUB(NOW(), INTERVAL 15 DAY), 4, DATE_SUB(NOW(), INTERVAL 15 DAY)),

-- 用户5 刘洋的消费记录
('TRX202501010401', 5, 'EXPENSE', 400.00, 20000.00, 19600.00, '购买保险：老年意外险（尊享版）x2', 'EXPENSE', 4, 'BALANCE', 'SUCCESS', 5, DATE_SUB(NOW(), INTERVAL 20 DAY), 5, DATE_SUB(NOW(), INTERVAL 20 DAY)),

-- 用户6 陈欣的消费记录
('TRX202501010501', 6, 'EXPENSE', 80.00, 10000.00, 9920.00, '购买保险：国寿财1-4类意外险', 'EXPENSE', 5, 'BALANCE', 'SUCCESS', 6, DATE_SUB(NOW(), INTERVAL 15 DAY), 6, DATE_SUB(NOW(), INTERVAL 15 DAY)),
('TRX202501010502', 6, 'RECHARGE', 3000.00, 9920.00, 12920.00, '账户充值-支付宝', NULL, NULL, 'ALIPAY', 'SUCCESS', 6, DATE_SUB(NOW(), INTERVAL 10 DAY), 6, DATE_SUB(NOW(), INTERVAL 10 DAY)),

-- 用户7 赵磊的消费记录（驳回后退费）
('TRX202501010601', 7, 'EXPENSE', 380.00, 5000.00, 4620.00, '购买保险：少儿门诊医疗险', 'EXPENSE', 10, 'BALANCE', 'REFUNDED', 7, DATE_SUB(NOW(), INTERVAL 5 DAY), 7, DATE_SUB(NOW(), INTERVAL 4 DAY)),
('TRX202501010602', 7, 'RECHARGE', 380.00, 4620.00, 5000.00, '审核驳回退款', 'REFUND', 10, 'BALANCE', 'SUCCESS', 7, DATE_SUB(NOW(), INTERVAL 4 DAY), 7, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- 用户8 孙丽的消费记录
('TRX202501010701', 8, 'EXPENSE', 450.00, 20000.00, 19550.00, '购买保险：平安财高危职业意外险 x3', 'EXPENSE', 6, 'BALANCE', 'SUCCESS', 8, DATE_SUB(NOW(), INTERVAL 8 DAY), 8, DATE_SUB(NOW(), INTERVAL 8 DAY)),

-- 用户9 周军的消费记录
('TRX202501010801', 9, 'EXPENSE', 100.00, 3000.00, 2900.00, '购买保险：众安出行意外险 x2', 'EXPENSE', 11, 'BALANCE', 'SUCCESS', 9, DATE_SUB(NOW(), INTERVAL 3 DAY), 9, DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- 用户10 吴强的消费记录（已禁用）
('TRX202501010901', 10, 'EXPENSE', 2500.00, 5000.00, 2500.00, '购买保险：国寿财1-4类意外险', 'EXPENSE', NULL, 'BALANCE', 'SUCCESS', 10, DATE_SUB(NOW(), INTERVAL 85 DAY), 10, DATE_SUB(NOW(), INTERVAL 85 DAY)),

-- 用户11 郑华的消费记录
('TRX202501011001', 11, 'EXPENSE', 300.00, 12000.00, 11700.00, '购买保险：太保百万医疗险', 'EXPENSE', 7, 'BALANCE', 'SUCCESS', 11, DATE_SUB(NOW(), INTERVAL 3 DAY), 11, DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- 用户12 黄平的消费记录（待审核，预扣款）
('TRX202501011101', 12, 'EXPENSE', 55.00, 8000.00, 7945.00, '购买保险：国寿财1-3类10+5+5+50（青柑）', 'EXPENSE', 8, 'BALANCE', 'PENDING', 12, DATE_SUB(NOW(), INTERVAL 1 DAY), 12, NOW()),

-- 用户13 林晓的消费记录
('TRX202501011201', 13, 'EXPENSE', 120.00, 15000.00, 14880.00, '购买保险：平安财意外10+1+50+10（含意保）x2', 'EXPENSE', 9, 'BALANCE', 'SUCCESS', 13, DATE_SUB(NOW(), INTERVAL 2 DAY), 13, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ================================================
-- 8. 通知公告数据
-- ================================================
INSERT INTO axx_notice (title, content, sort_no, published_at, create_by, create_time, update_by, update_time) VALUES
('欢迎使用优选通保险服务平台', '优选通保险服务平台已正式上线，欢迎各位用户使用。如有任何问题，请联系管理员。', 1, DATE_SUB(NOW(), INTERVAL 60 DAY), 1, DATE_SUB(NOW(), INTERVAL 60 DAY), 1, NOW()),
('春节假期投保提醒', '春节期间（1月28日-2月4日）提交的投保申请将于节后统一处理，请各位用户知悉。', 2, DATE_SUB(NOW(), INTERVAL 30 DAY), 1, DATE_SUB(NOW(), INTERVAL 30 DAY), 1, NOW()),
('新产品上线通知', '众安出行意外险现已上线，出行保障更全面，欢迎选购！', 3, DATE_SUB(NOW(), INTERVAL 10 DAY), 1, DATE_SUB(NOW(), INTERVAL 10 DAY), 1, NOW()),
('系统维护通知', '系统将于本周六凌晨2:00-4:00进行例行维护，届时服务可能短暂中断，请提前做好准备。', 4, DATE_SUB(NOW(), INTERVAL 2 DAY), 1, DATE_SUB(NOW(), INTERVAL 2 DAY), 1, NOW());

-- ================================================
-- 初始化完成
-- ================================================
-- 账户说明:
-- 管理员: admin / admin123
-- 普通用户: zhangwei, liming, wangfang, liuyang, chenxin, zhaolei, sunli, zhoujun, zhenghua, huangping, linxiao
-- 密码统一为: user123
-- 
-- 测试场景覆盖:
-- 1. 已生效保单 (ACTIVE) - 7个费用清单，多个被保人
-- 2. 待审核保单 (PENDING_REVIEW) - 1个
-- 3. 审核通过待承保 (APPROVED) - 1个
-- 4. 审核驳回保单 (REVIEW_REJECTED) - 1个
-- 5. 承保中保单 (UNDERWRITING) - 1个
-- 6. 已取消保单 (CANCELLED) - 1个
-- 7. 禁用用户 - 1个 (wuqiang)
-- ================================================
