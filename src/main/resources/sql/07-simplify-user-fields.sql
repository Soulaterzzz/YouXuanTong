-- 简化用户表结构，移除 real_name 和 email 字段
-- 只保留：用户名、手机号、密码

ALTER TABLE axx_user
    DROP COLUMN IF EXISTS real_name,
    DROP COLUMN IF EXISTS email;
