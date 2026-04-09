-- 为产品表补充显示价格字段（分销商对外展示价格）
ALTER TABLE axx_product
    ADD COLUMN display_price DECIMAL(18,2) DEFAULT NULL COMMENT '显示价格（分销商对外展示价格，为空时使用 price）' AFTER price;
