-- 09-add-balance-version-and-remove-stock.sql
-- 为账户余额表添加乐观锁版本字段，移除产品表库存字段

-- 1. 为账户余额表添加version字段（乐观锁）
SET @version_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'axx_account_balance'
      AND column_name = 'version'
);
SET @sql := IF(@version_exists = 0,
    'ALTER TABLE axx_account_balance ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT ''版本号，用于乐观锁''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 产品表移除stock字段（库存功能已废弃）
SET @stock_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'axx_product'
      AND column_name = 'stock'
);
SET @sql := IF(@stock_exists > 0,
    'ALTER TABLE axx_product DROP COLUMN stock',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
