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

SET @old_index_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'axx_product'
      AND index_name = 'uk_product_code'
);
SET @drop_old_index_sql := IF(@old_index_exists > 0, 'ALTER TABLE axx_product DROP INDEX uk_product_code', 'SELECT 1');
PREPARE drop_old_index_stmt FROM @drop_old_index_sql;
EXECUTE drop_old_index_stmt;
DEALLOCATE PREPARE drop_old_index_stmt;

SET @new_index_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'axx_product'
      AND index_name = 'uk_product_code_deleted'
);
SET @add_new_index_sql := IF(@new_index_exists = 0, 'ALTER TABLE axx_product ADD UNIQUE KEY uk_product_code_deleted (product_code, deleted)', 'SELECT 1');
PREPARE add_new_index_stmt FROM @add_new_index_sql;
EXECUTE add_new_index_stmt;
DEALLOCATE PREPARE add_new_index_stmt;
