-- 为产品表添加图片字段
ALTER TABLE axx_product ADD COLUMN image_url VARCHAR(500) COMMENT '图片URL' AFTER sort_no;
ALTER TABLE axx_product ADD COLUMN image_path VARCHAR(500) COMMENT '图片本地存储路径' AFTER image_url;
ALTER TABLE axx_product ADD COLUMN image_content_type VARCHAR(100) COMMENT '图片内容类型' AFTER image_path;
ALTER TABLE axx_product ADD COLUMN image_size BIGINT COMMENT '图片大小(字节)' AFTER image_content_type;

-- 创建图片存储目录（在实际部署时需要创建）
-- mkdir -p /var/ytbx/images/products
