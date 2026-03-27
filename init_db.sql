-- ================================================
-- 优选通数据库初始化脚本
-- 版本: 1.0.0
-- 说明: 包含完整表结构和测试数据
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
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 初始化产品数据
INSERT INTO axx_product (product_code, product_name, category_code, company_name, description, features, price, stock, is_new, is_hot, sale_status, sort_no, create_time, update_time) VALUES
('GS-001', '国寿财1-3类10+5+5+50（青柑）', '1-3', '国寿财险', '个单：18-63；可投保：1-3类；T+1；一三五十截单', '无门诊免赔；承保动物伤害', 55.00, 0, 1, 1, 'ON_SALE', 1, NOW(), NOW()),
('PA-001', '平安财意外10+1+50+10（含意保）', '1-3', '平安财险', '1-3类职业可投；每周一三五十十点截单', '限投一份；正常1+6生效；1:4新增住院地区和医院', 60.00, 100, 0, 0, 'ON_SALE', 2, NOW(), NOW()),
('SE-001', '少儿门诊医疗险', 'child', '众安保险', '0-17岁投保；门急诊可报；等待期短', '门急诊保障；住院医疗；特定疾病额外赔', 380.00, 50, 1, 0, 'ON_SALE', 3, NOW(), NOW()),
('EL-001', '老年意外险（尊享版）', 'elder', '人保财险', '50-80岁可投保；综合意外保障', '意外医疗；骨折津贴；救护车费用', 200.00, 200, 0, 1, 'ON_SALE', 4, NOW(), NOW()),
('GS-002', '国寿财1-4类意外险', '1-4', '国寿财险', '1-4类职业可投；保障全面', '意外身故伤残；意外医疗', 80.00, 150, 0, 0, 'ON_SALE', 5, NOW(), NOW());

-- 2. 初始化用户数据（ID从1开始）
INSERT INTO axx_user (id, username, password, mobile, user_type, status, create_time, update_time) VALUES
(1, 'admin', 'admin111', '13800138000', 'ADMIN', 'ACTIVE', NOW(), NOW()),
(2, 'user1', 'user123', '13800138001', 'USER', 'ACTIVE', NOW(), NOW()),
(3, 'user2', 'user123', '13800138002', 'USER', 'ACTIVE', NOW(), NOW()),
(4, 'user3', 'user123', '13800138003', 'USER', 'ACTIVE', NOW(), NOW()),
(5, 'user4', 'user123', '13800138004', 'USER', 'ACTIVE', NOW(), NOW()),
(6, 'user5', 'user123', '13800138005', 'USER', 'ACTIVE', NOW(), NOW()),
(7, 'user6', 'user123', '13800138006', 'USER', 'ACTIVE', NOW(), NOW()),
(8, 'user7', 'user123', '13800138007', 'USER', 'ACTIVE', NOW(), NOW()),
(9, 'user8', 'user123', '13800138008', 'USER', 'ACTIVE', NOW(), NOW()),
(10, 'user9', 'user123', '13800138009', 'USER', 'ACTIVE', NOW(), NOW());

-- 3. 初始化账户余额
INSERT INTO axx_account_balance (user_id, balance, frozen_balance, create_by, create_time, update_by, update_time) VALUES
(1, 50000.00, 0.00, 1, NOW(), 1, NOW()),
(2, 9945.00, 0.00, 1, NOW(), 1, NOW()),
(3, 14940.00, 0.00, 1, NOW(), 1, NOW()),
(4, 7620.00, 0.00, 1, NOW(), 1, NOW()),
(5, 19800.00, 0.00, 1, NOW(), 1, NOW()),
(6, 11920.00, 0.00, 1, NOW(), 1, NOW()),
(7, 18000.00, 0.00, 1, NOW(), 1, NOW()),
(8, 9000.00, 0.00, 1, NOW(), 1, NOW()),
(9, 16000.00, 0.00, 1, NOW(), 1, NOW()),
(10, 11000.00, 0.00, 1, NOW(), 1, NOW());

-- 4. 初始化费用清单记录（关联用户ID 2-6）
INSERT INTO axx_expense_record (serial_no, user_id, product_id, product_name, contact_name, contact_mobile, expense_status, policy_no, premium_amount, quantity, total_amount, effective_date, expiry_date, create_by, create_time, update_by, update_time) VALUES
('EXP202401010001', 2, 1, '国寿财1-3类10+5+5+50（青柑）', '张三', '13800138001', 'COMPLETED', 'POL202401010001', 55.00, 1, 55.00, '2024-01-02', '2025-01-01', 2, NOW(), 2, NOW()),
('EXP202401010002', 3, 2, '平安财意外10+1+50+10（含意保）', '李四', '13800138002', 'COMPLETED', 'POL202401010002', 60.00, 1, 60.00, '2024-01-02', '2025-01-01', 3, NOW(), 3, NOW()),
('EXP202401010003', 4, 3, '少儿门诊医疗险', '王五', '13800138003', 'COMPLETED', 'POL202401010003', 380.00, 1, 380.00, '2024-01-02', '2025-01-01', 4, NOW(), 4, NOW()),
('EXP202401010004', 5, 4, '老年意外险（尊享版）', '赵六', '13800138004', 'COMPLETED', 'POL202401010004', 200.00, 1, 200.00, '2024-01-02', '2025-01-01', 5, NOW(), 5, NOW()),
('EXP202401010005', 6, 5, '国寿财1-4类意外险', '孙七', '13800138005', 'COMPLETED', 'POL202401010005', 80.00, 1, 80.00, '2024-01-02', '2025-01-01', 6, NOW(), 6, NOW());

-- 5. 初始化保险记录（关联费用记录和用户ID 2-6）
INSERT INTO axx_insurance_record (expense_id, user_id, product_name, insured_name, insured_id_no, insured_mobile, beneficiary_name, beneficiary_id_no, beneficiary_mobile, beneficiary_job, beneficiary_address, agent_name, insurance_status, policy_no, premium_amount, quantity, effective_date, expiry_date, create_by, create_time, update_by, update_time) VALUES
(1, 2, '国寿财1-3类10+5+5+50（青柑）', '张三', '110101199001011234', '13800138001', '张三', '110101199001011234', '13800138001', '办公室职员', '北京市朝阳区', '系统', 'ACTIVE', 'POL202401010001', 55.00, 1, '2024-01-02', '2025-01-01', 2, NOW(), 2, NOW()),
(2, 3, '平安财意外10+1+50+10（含意保）', '李四', '110101199002022345', '13800138002', '李四', '110101199002022345', '13800138002', '销售人员', '上海市浦东新区', '系统', 'ACTIVE', 'POL202401010002', 60.00, 1, '2024-01-02', '2025-01-01', 3, NOW(), 3, NOW()),
(3, 4, '少儿门诊医疗险', '王五', '110101198503033456', '13800138003', '王小五', '110101201004044567', '13800138003', '学生', '广州市天河区', '系统', 'ACTIVE', 'POL202401010003', 380.00, 1, '2024-01-02', '2025-01-01', 4, NOW(), 4, NOW()),
(4, 5, '老年意外险（尊享版）', '赵六', '110101198005055678', '13800138004', '赵老', '110101195006066789', '13800138004', '退休', '深圳市南山区', '系统', 'ACTIVE', 'POL202401010004', 200.00, 1, '2024-01-02', '2025-01-01', 5, NOW(), 5, NOW()),
(5, 6, '国寿财1-4类意外险', '孙七', '110101199207077890', '13800138005', '孙七', '110101199207077890', '13800138005', '技术工程师', '杭州市西湖区', '系统', 'ACTIVE', 'POL202401010005', 80.00, 1, '2024-01-02', '2025-01-01', 6, NOW(), 6, NOW());

-- 6. 初始化交易记录（充值记录）
INSERT INTO axx_transaction_record (serial_no, user_id, trans_type, amount, balance_before, balance_after, description, ref_type, ref_id, payment_method, payment_status, create_by, create_time, update_by, update_time) VALUES
-- user1 充值记录
('TRX202401010001', 2, 'RECHARGE', 10000.00, 0.00, 10000.00, '初始充值', NULL, NULL, 'WECHAT', 'SUCCESS', 1, NOW(), 1, NOW()),
-- user2 充值记录
('TRX202401010002', 3, 'RECHARGE', 15000.00, 0.00, 15000.00, '初始充值', NULL, NULL, 'ALIPAY', 'SUCCESS', 1, NOW(), 1, NOW()),
-- user3 充值记录
('TRX202401010003', 4, 'RECHARGE', 8000.00, 0.00, 8000.00, '初始充值', NULL, NULL, 'WECHAT', 'SUCCESS', 1, NOW(), 1, NOW()),
-- user4 充值记录
('TRX202401010004', 5, 'RECHARGE', 20000.00, 0.00, 20000.00, '初始充值', NULL, NULL, 'ALIPAY', 'SUCCESS', 1, NOW(), 1, NOW()),
-- user5 充值记录
('TRX202401010005', 6, 'RECHARGE', 12000.00, 0.00, 12000.00, '初始充值', NULL, NULL, 'WECHAT', 'SUCCESS', 1, NOW(), 1, NOW()),
-- user6-user10 充值记录
('TRX202401010006', 7, 'RECHARGE', 18000.00, 0.00, 18000.00, '初始充值', NULL, NULL, 'ALIPAY', 'SUCCESS', 1, NOW(), 1, NOW()),
('TRX202401010007', 8, 'RECHARGE', 9000.00, 0.00, 9000.00, '初始充值', NULL, NULL, 'WECHAT', 'SUCCESS', 1, NOW(), 1, NOW()),
('TRX202401010008', 9, 'RECHARGE', 16000.00, 0.00, 16000.00, '初始充值', NULL, NULL, 'ALIPAY', 'SUCCESS', 1, NOW(), 1, NOW()),
('TRX202401010009', 10, 'RECHARGE', 11000.00, 0.00, 11000.00, '初始充值', NULL, NULL, 'WECHAT', 'SUCCESS', 1, NOW(), 1, NOW());

-- 7. 初始化交易记录（消费记录）
INSERT INTO axx_transaction_record (serial_no, user_id, trans_type, amount, balance_before, balance_after, description, ref_type, ref_id, payment_method, payment_status, create_by, create_time, update_by, update_time) VALUES
-- user1 消费记录
('TRX202401020001', 2, 'EXPENSE', 55.00, 10000.00, 9945.00, '购买保险：国寿财1-3类10+5+5+50（青柑）', 'EXPENSE', 1, 'BALANCE', 'SUCCESS', 2, NOW(), 2, NOW()),
-- user2 消费记录
('TRX202401020002', 3, 'EXPENSE', 60.00, 15000.00, 14940.00, '购买保险：平安财意外10+1+50+10（含意保）', 'EXPENSE', 2, 'BALANCE', 'SUCCESS', 3, NOW(), 3, NOW()),
-- user3 消费记录
('TRX202401020003', 4, 'EXPENSE', 380.00, 8000.00, 7620.00, '购买保险：少儿门诊医疗险', 'EXPENSE', 3, 'BALANCE', 'SUCCESS', 4, NOW(), 4, NOW()),
-- user4 消费记录
('TRX202401020004', 5, 'EXPENSE', 200.00, 20000.00, 19800.00, '购买保险：老年意外险（尊享版）', 'EXPENSE', 4, 'BALANCE', 'SUCCESS', 5, NOW(), 5, NOW()),
-- user5 消费记录
('TRX202401020005', 6, 'EXPENSE', 80.00, 12000.00, 11920.00, '购买保险：国寿财1-4类意外险', 'EXPENSE', 5, 'BALANCE', 'SUCCESS', 6, NOW(), 6, NOW());

-- ================================================
-- 初始化完成
-- 测试账号：admin/admin111 (管理员)
--          user1-9/user123 (普通用户)
-- ================================================
