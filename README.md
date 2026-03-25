# 优选通保险服务平台

<div align="center">

![Java](https://img.shields.io/badge/Java-17+-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green.svg)
![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

**专业、稳健、有温度的保险服务管理平台**

[功能介绍](#功能介绍) · [技术栈](#技术栈) · [快速开始](#快速开始) · [项目结构](#项目结构) · [部署指南](#部署指南) · [许可证](#许可证)

</div>

---

## 功能介绍

### 客户端功能

| 模块 | 功能描述 |
|------|----------|
| **产品中心** | 浏览保险产品、分类筛选、产品详情查看 |
| **费用清单** | 查看保险购买记录、订单状态跟踪 |
| **保险清单** | 管理被保险人信息、保单查询 |
| **账户充值** | 在线充值、支付记录查询 |
| **个人中心** | 账户管理、余额查询、消费明细 |

### 管理端功能

| 模块 | 功能描述 |
|------|----------|
| **数据概览** | 用户统计、订单统计、收入概览 |
| **用户管理** | 用户CRUD、状态管理、余额管理 |
| **产品管理** | 产品上架、下架、编辑 |
| **订单管理** | 订单处理、状态更新 |

---

## 技术栈

### 后端技术

| 技术 | 说明 |
|------|------|
| **Java 17** | 主要开发语言 |
| **Spring Boot 3.5** | 核心框架 |
| **MyBatis-Plus** | ORM 持久层框架 |
| **MySQL 8.0** | 关系型数据库 |
| **JWT** | 身份认证 |
| **Spring Security** | 安全框架 |

### 前端技术

| 技术 | 说明 |
|------|------|
| **Vue 3.4** | 渐进式 JavaScript 框架 |
| **Vite** | 下一代前端构建工具 |
| **Element Plus** | UI 组件库 |
| **Vue Router** | 前端路由管理 |
| **Axios** | HTTP 请求库 |

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+

### 1. 克隆项目

```bash
git clone https://github.com/your-username/ytbx.git
cd ytbx
```

### 2. 配置数据库

```bash
# 登录 MySQL 创建数据库
mysql -u root -p
CREATE DATABASE IF NOT EXISTS ytbx DEFAULT CHARACTER SET utf8mb4;
EXIT;

# 初始化数据库表和数据
mysql -u root -p ytbx < init_db.sql
```

### 3. 配置后端

编辑 `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ytbx?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=your_password
```

### 4. 启动后端

```bash
mvn spring-boot:run
```

后端启动后运行在 http://localhost:8080

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动后访问 http://localhost:5173

### 6. 登录测试

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin111 |
| 普通用户 | user1 | user123 |

---

## 项目结构

```
ytbx/
├── src/
│   └── main/
│       ├── java/com/zs/ytbx/
│       │   ├── controller/     # 控制器层
│       │   ├── service/       # 服务层
│       │   ├── mapper/        # 数据访问层
│       │   ├── entity/        # 实体类
│       │   ├── dto/           # 数据传输对象
│       │   ├── config/        # 配置类
│       │   └── common/        # 公共组件
│       └── resources/
│           └── application.properties
├── frontend/
│   ├── src/
│   │   ├── views/           # 页面组件
│   │   ├── router/          # 路由配置
│   │   ├── assets/          # 静态资源
│   │   └── App.vue          # 根组件
│   ├── public/              # 公共资源
│   ├── package.json
│   └── vite.config.js
├── init_db.sql              # 数据库初始化脚本
├── pom.xml                  # Maven 配置
└── DEPLOY.md                # 部署文档
```

---

## API 接口

### 认证接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/logout` | POST | 用户登出 |
| `/api/auth/currentUser` | GET | 获取当前用户 |

### 客户端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/anxinxuan/products` | GET | 获取产品列表 |
| `/api/anxinxuan/expenses` | GET | 获取费用清单 |
| `/api/anxinxuan/insurances` | GET | 获取保险清单 |
| `/api/anxinxuan/recharges` | GET | 获取充值记录 |
| `/api/anxinxuan/balance` | GET | 获取账户余额 |

### 管理端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/users` | GET | 获取用户列表 |
| `/api/admin/users` | POST | 创建用户 |
| `/api/admin/users/{id}` | PUT | 更新用户 |
| `/api/admin/users/{id}/disable` | PUT | 禁用用户 |
| `/api/admin/users/{id}/enable` | PUT | 启用用户 |
| `/api/admin/products` | GET/POST | 产品管理 |
| `/api/admin/stats` | GET | 统计数据 |

---

## 部署指南

详细部署说明请参考 [DEPLOY.md](DEPLOY.md)

### 生产环境部署

```bash
# 1. 打包后端
mvn clean package -DskipTests

# 2. 构建前端
cd frontend && npm run build && cd ..

# 3. 使用 Nginx 部署前端静态文件
# 4. 使用 java -jar 启动后端
```

### ⚠️ 重要配置变量

部署前请务必检查并修改以下配置：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `FILE_STORAGE_PATH` | 文件存储根目录 | `/Users/zhao/IdeaProjects/ytbx/uploads` |
| `TEMPLATE_FILE_PATH` | 模板文件存储路径 | `/Users/zhao/IdeaProjects/ytbx/uploads/templates` |
| `MYSQL_URL` | 数据库连接地址 | `jdbc:mysql://127.0.0.1:3306/ytbx` |
| `MYSQL_PASSWORD` | 数据库密码 | - |

可通过环境变量或 `application.properties` 修改：
```bash
export FILE_STORAGE_PATH=/your/path/uploads
export TEMPLATE_FILE_PATH=/your/path/uploads/templates
```

---

## 许可证

本项目采用 MIT 许可证开源，详情请参阅 [LICENSE](LICENSE) 文件。

---

<div align="center">

**如果这个项目对您有帮助，请给我们一个 ⭐️**

Design with ❤️ by 优选通团队

</div>
