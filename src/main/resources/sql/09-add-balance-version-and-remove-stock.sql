-- 09-add-balance-version-and-remove-stock.sql
-- 为账户余额表添加乐观锁版本字段，移除产品表库存字段

-- 1. 为账户余额表添加version字段（乐观锁）
ALTER TABLE axx_account_balance ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '版本号，用于乐观锁';

-- 2. 产品表移除stock字段（库存功能已废弃）
ALTER TABLE axx_product DROP COLUMN stock;