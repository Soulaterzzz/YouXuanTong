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

## 项目结构

```text
ytbx/
├── src/main/java/com/zs/ytbx/
│   ├── common/                  # 通用返回、异常、鉴权常量
│   │   ├── api/                  # ApiResponse, PageResponse
│   │   ├── auth/                 # AuthContext, AuthTokenService, SessionConstants
│   │   ├── enums/                # InsuranceStatus, ResultCode
│   │   ├── exception/            # BusinessException
│   │   └── util/                 # PasswordUtils
│   ├── config/                   # Web / CORS / 文件存储 / 数据库迁移配置
│   ├── controller/               # 控制器
│   ├── dto/                      # 请求与响应 DTO
│   ├── entity/                   # 数据库实体
│   ├── mapper/                   # MyBatis-Plus Mapper
│   ├── query/                    # 查询条件对象
│   ├── service/                  # 服务接口
│   ├── service/impl/             # 服务实现
│   └── vo/                       # 视图对象
├── src/main/resources/
│   ├── application.properties     # 默认配置
│   ├── application-*.properties  # 环境配置（local/prod/demo）
│   └── static/                   # 前端构建产物
├── frontend/
│   ├── src/
│   │   ├── views/                # 页面视图
│   │   ├── router/               # 路由配置
│   │   ├── components/            # 公共组件
│   │   └── main.js               # 前端入口
│   └── package.json
├── deploy/                       # 部署配置
│   ├── docker-compose.yml        # Docker Compose 部署
│   ├── server-bootstrap-ubuntu.sh # 服务器初始化脚本
│   ├── nginx/                    # Nginx 配置示例
│   └── .env.example              # 环境变量示例
├── docs/                         # 文档
│   ├── deploy.md                 # 部署文档
│   └── qa-test-plan.md           # 测试计划
├── init_db.sql                   # 数据库初始化脚本
├── dev.sh                        # 本地开发脚本
├── Dockerfile                    # Docker 镜像构建
└── pom.xml                       # Maven 配置
```

---

## 快速开始

## 1. 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8.0+

## 2. 本地开发启动

项目提供了交互式开发脚本 `dev.sh`：

```bash
# 交互式启动（自动引导配置）
./dev.sh

# 或指定命令
./dev.sh init    # 初始化数据库
./dev.sh start   # 启动应用
./dev.sh stop    # 停止应用
./dev.sh status  # 查看状态
```

首次使用会自动提示配置数据库连接等信息。

## 3. 手动初始化数据库

```bash
mysql -u root -p
CREATE DATABASE IF NOT EXISTS ytbx DEFAULT CHARACTER SET utf8mb4;
exit

mysql -u root -p ytbx < init_db.sql
```

### 默认测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `admin123` |
| 普通用户 | `zhangwei` | `user123` |

> 普通用户账号由管理员在"用户管理"中创建。

## 4. 配置后端数据库

编辑 `src/main/resources/application-local.properties` 或使用环境变量：

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/ytbx?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=your_password
```

## 5. 启动后端

```bash
mvn spring-boot:run
```

默认地址：`http://localhost:8080`

## 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

---

## 部署指南

### Docker Compose 部署

```bash
cd deploy
cp .env.example .env
# 编辑 .env 填入实际配置
docker-compose up -d
```

### 服务器初始化

```bash
cd deploy
./server-bootstrap-ubuntu.sh
```

详细部署文档请参考 [docs/deploy.md](docs/deploy.md)。

---

## 数据库说明

### 初始化脚本

- `init_db.sql`：完整建表与初始化数据，包含：
  - `axx_user` - 用户表
  - `axx_account_balance` - 账户余额表
  - `axx_transaction_record` - 交易记录表
  - `axx_expense_record` - 费用清单表
  - `axx_insurance_record` - 保险清单表
  - `axx_product` - 产品表
  - `axx_notice` - 通知公告表

### 启动自动修复

项目启动时会自动执行数据库兼容性检查，补齐以下结构：

- `axx_notice` 表
- `axx_product.template_file_name`
- `axx_product.template_file_path`
- `(product_code, deleted)` 组合唯一索引

对应代码见：`src/main/java/com/zs/ytbx/config/DatabaseMigrationRunner.java`

---

## 常用接口

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 退出登录 |
| GET | `/api/auth/current` | 获取当前登录用户 |

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

## 当前已验证流程

以下流程已经在本地 MySQL 上完成实际验证：

- 管理员登录
- 新增产品写入数据库
- 删除产品写入逻辑删除字段
- 发布通知公告写入 `axx_notice`
- 用户端读取通知公告接口

---

## 说明

- 本地上传目录默认位于 `uploads/`
- 如果使用已有数据库，建议先备份，再执行初始化脚本
- 提交代码前请确保 `.gitignore` 已排除敏感信息
