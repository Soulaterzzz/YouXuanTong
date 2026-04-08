-- 为产品表补充管理员别名字段
ALTER TABLE axx_product
    ADD COLUMN alias VARCHAR(128) DEFAULT NULL COMMENT '产品别名' AFTER product_name;
