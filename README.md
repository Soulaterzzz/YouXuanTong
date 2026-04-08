# 优选通保险服务平台

<div align="center">

![Java](https://img.shields.io/badge/Java-17+-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green.svg)
![Vue](https://img.shields.io/badge/Vue-3.5-brightgreen.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange.svg)

**面向保险产品、订单、用户与通知公告的一体化管理平台**

</div>

---

## 项目简介

本项目包含：

- `Spring Boot + MyBatis-Plus` 后端服务
- `Vue 3 + Element Plus + Vite` 前端单页应用
- `MySQL` 业务数据存储

系统分为**用户端**和**管理员端**两类使用场景：

- 用户端：浏览产品、激活产品、查看费用清单/保险清单/消费明细
- 管理端：管理用户、管理产品、发布首页通知公告、查看统计数据

当前认证方式为 **Session / Cookie**，不是 JWT。

---

## 主要功能

### 用户端

| 模块 | 说明 |
|------|------|
| 首页 | 展示欢迎信息、业务统计、通知公告列表 |
| 产品中心 | 按类别/承保公司筛选产品，查看图片、下载模板、执行激活 |
| 费用清单 | 查看费用记录、筛选状态、导出数据 |
| 保险清单 | 查看保单信息、筛选状态、导出数据 |
| 消费明细 | 查看账户余额与交易记录 |

### 管理端

| 模块 | 说明 |
|------|------|
| 首页 | 查看统计数据，编辑并发布通知公告 |
| 用户管理 | 查询、新增、编辑、启用、禁用、删除用户 |
| 产品中心 | 新增、编辑、上架、下架、删除产品，上传图片与模板 |
| 费用/保险/充值管理 | 查看全量业务记录 |

### 通知公告

- 管理员首页可编辑并发布通知
- 用户首页与管理员首页展示同一份通知列表
- 通知数据已落库到 `axx_notice` 表

---

## 技术栈

### 后端

| 技术 | 说明 |
|------|------|
| Java 17 | 运行环境 |
| Spring Boot 3.5 | Web 应用框架 |
| MyBatis-Plus 3.5 | ORM 与逻辑删除支持 |
| MySQL 8 | 主业务数据库 |
| HikariCP | 数据库连接池 |
| Jakarta Validation | 参数校验 |

### 前端

| 技术 | 说明 |
|------|------|
| Vue 3.5 | 前端框架 |
| Vite | 构建工具 |
| Element Plus | UI 组件库 |
| Vue Router | 路由管理 |
| Axios | HTTP 请求 |

---

## 快速开始

## 1. 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8.0+

## 2. 初始化数据库

```bash
mysql -u root -p
CREATE DATABASE IF NOT EXISTS ytbx DEFAULT CHARACTER SET utf8mb4;
exit

mysql -u root -p ytbx < init_db.sql
```

初始化脚本会创建以下核心表：

- `axx_user`
- `axx_account_balance`
- `axx_transaction_record`
- `axx_expense_record`
- `axx_insurance_record`
- `axx_product`
- `axx_notice`

### 默认测试账号

初始化脚本会写入管理员和普通用户种子账号，账号名如下：

| 角色 | 用户名 |
|------|--------|
| 管理员 | `admin` |
| 普通用户 | `user` |

> 密码不再以明文写在仓库文档里；请按 `init_db.sql` 或部署环境变量完成初始化后再登录。

## 3. 配置后端数据库

默认启动使用真实数据库模式，需要先启动 MySQL 并导入初始化脚本：

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/ytbx?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=your_password
auth.token.secret=${AUTH_TOKEN_SECRET}
app.cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5173
```

## 4. 启动后端

```bash
mvn spring-boot:run
```

默认地址：`http://localhost:8080`

如果是首次启动，请先创建 `ytbx` 数据库并导入 `init_db.sql`，再启动应用。
如果本地 MySQL 暂时不可用，启动会在数据库连接阶段失败，请先确认 MySQL 服务、账号密码和数据库名都正确。

## 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

## 6. 运行测试

### 后端测试

```bash
mvn test
```

### 前端测试

```bash
cd frontend
npm test
```

后端回归以 `mvn test` 为准，前端回归以前端项目自己的测试命令为准。

---

## 数据库说明

### 初始化脚本

- `init_db.sql`：完整建表与初始化数据
- `src/main/resources/sql/08-add-home-notice-and-product-code-index.sql`：补充通知公告表与产品唯一索引迁移脚本

### 启动自动修复

项目启动时会自动执行数据库兼容性检查，补齐以下结构：

- `axx_notice` 表
- `axx_product.template_file_name`
- `axx_product.template_file_path`
- `(product_code, deleted)` 组合唯一索引

对应代码见：

- `src/main/java/com/zs/ytbx/config/DatabaseMigrationRunner.java`

---

## 常用接口

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 退出登录 |
| GET | `/api/auth/current` | 获取当前登录用户 |
| POST | `/api/auth/register` | 注册用户接口（UI 默认不暴露） |

### 用户端接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/anxinxuan/stats` | 首页统计 |
| GET | `/api/anxinxuan/products` | 产品列表 |
| POST | `/api/anxinxuan/products/activate` | 激活产品 |
| GET | `/api/anxinxuan/expenses` | 费用清单 |
| GET | `/api/anxinxuan/insurances` | 保险清单 |
| GET | `/api/anxinxuan/recharges` | 消费明细 |
| GET | `/api/notices` | 首页通知公告 |

### 管理端接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users` | 用户列表 |
| POST | `/api/admin/users` | 新增用户 |
| PUT | `/api/admin/users` | 编辑用户 |
| DELETE | `/api/admin/users/{userId}` | 删除用户 |
| PUT | `/api/admin/users/{userId}/enable` | 启用用户 |
| PUT | `/api/admin/users/{userId}/disable` | 禁用用户 |
| GET | `/api/admin/products` | 产品列表 |
| POST | `/api/admin/products` | 新增产品 |
| PUT | `/api/admin/products` | 编辑产品 |
| DELETE | `/api/admin/products/{productId}` | 删除产品 |
| PUT | `/api/admin/products/{productId}/on-sale` | 上架产品 |
| PUT | `/api/admin/products/{productId}/off-sale` | 下架产品 |
| GET | `/api/admin/stats` | 首页统计 |
| POST | `/api/admin/notices/publish` | 发布通知公告 |

---

## 项目结构

```text
ytbx/
├── src/main/java/com/zs/ytbx/
│   ├── common/                  # 通用返回、异常、鉴权常量
│   ├── config/                  # Web / CORS / 文件存储 / 数据库迁移配置
│   ├── controller/              # 控制器
│   ├── dto/                     # 请求与响应 DTO
│   ├── entity/                  # 数据库实体
│   ├── mapper/                  # MyBatis-Plus Mapper
│   ├── query/                   # 查询条件对象
│   ├── service/                 # 服务接口
│   └── service/impl/            # 服务实现
├── src/main/resources/
│   ├── application.properties   # 默认配置
│   └── sql/                     # SQL 迁移脚本
├── frontend/
│   ├── src/views/               # 页面视图
│   ├── src/router/              # 路由
│   ├── src/main.js              # 前端入口
│   └── package.json             # 前端依赖与脚本
├── init_db.sql                  # 数据库初始化脚本
└── pom.xml                      # Maven 配置
```

---

## 当前已验证流程

以下流程已经在本地 MySQL 上完成实际验证：

- 管理员登录
- 新增产品写入数据库
- 删除产品写入逻辑删除字段
- 发布通知公告写入 `axx_notice`
- 用户端读取通知公告接口

---

## 说明

- `target/` 下的编译产物不建议手动提交
- 本地上传目录默认位于 `uploads/`
- 如果使用已有数据库，建议先备份，再执行初始化脚本或迁移脚本
- 后续开发规范文档见：`docs/development-guide.md`
