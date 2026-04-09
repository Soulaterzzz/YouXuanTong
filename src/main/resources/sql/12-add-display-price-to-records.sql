-- 为费用清单和保险清单添加显示价格字段（用户自定义分销价格）
ALTER TABLE axx_expense_record
    ADD COLUMN display_price DECIMAL(18,2) DEFAULT NULL COMMENT '显示价格（用户自定义的分销价格，为空时使用 premium_amount）' AFTER premium_amount;

ALTER TABLE axx_insurance_record
    ADD COLUMN display_price DECIMAL(18,2) DEFAULT NULL COMMENT '显示价格（用户自定义的分销价格，为空时使用 premium_amount）' AFTER premium_amount;
