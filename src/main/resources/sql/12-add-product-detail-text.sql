-- 为产品表补充独立详情正文字段
ALTER TABLE axx_product
    ADD COLUMN detail_text LONGTEXT DEFAULT NULL COMMENT '产品详情正文' AFTER features;
