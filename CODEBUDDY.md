# CODEBUDDY.md This file provides guidance to CodeBuddy when working with code in this repository.

## 项目概述

优推保险系统（YTBX）是一个保险产品管理平台，包含产品展示、费用计算、保单管理、充值缴费等功能。

## 技术栈

**后端**
- Spring Boot 3.5.11
- MyBatis-Plus 3.5.15
- H2 内存数据库（默认）
- Session/Cookie 认证机制

**前端**
- Vue 3.5 + Vite
- Element Plus UI框架
- Axios HTTP客户端

## 常用命令

### 后端开发
```bash
# 编译项目
mvn clean compile

# 运行项目（默认使用Mock数据）
mvn spring-boot:run

# 打包
mvn clean package

# 运行测试
mvn test

# 使用真实数据库运行（需配置数据库连接）
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

### 前端开发
```bash
cd frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 生产构建
npm run build

# 预览构建结果
npm run preview
```

### Docker部署
```bash
# 构建镜像
docker build -t ytbx:latest .

# 运行容器
docker run -p 8080:8080 ytbx:latest
```

## 架构说明

### 整体架构
采用经典的三层架构模式：
```
Controller (API层) → Service (业务逻辑层) → Mapper (数据访问层)
```

### 双实现模式
系统支持Mock和数据库两种实现，通过配置切换：
```properties
# 使用Mock实现（默认）
ytbx.mock.enabled=true

# 使用数据库实现
ytbx.mock.enabled=false
```

实现类命名规则：
- Mock实现：`*ServiceImpl` （当`ytbx.mock.enabled=true`时生效）
- 数据库实现：`*DbServiceImpl` （当`ytbx.mock.enabled=false`时生效）

### 实体系统
项目包含两套实体系统：

1. **axx_*** 表（安心选业务）
   - `axx_product` - 产品信息
   - `axx_expense` - 费用记录
   - `axx_insurance` - 保单信息
   - `axx_recharge` - 充值记录
   - `axx_banner` - 轮播图
   - `axx_user` - 用户信息

2. **biz_*** 表（业务系统）
   - `biz_user` - 用户信息
   - `biz_product` - 产品信息
   - `biz_order` - 订单信息

### 前端架构
单页应用（SPA），核心业务逻辑集中在`frontend/src/views/Home.vue`（约5000行代码）。

页面路由：
- `/` - Home.vue（主业务页面）
- `/login` - Login.vue（登录页面）

### 认证机制
使用Session/Cookie认证，非JWT方式：
- 登录：`POST /api/auth/login`
- 登出：`POST /api/auth/logout`
- 注册：`POST /api/auth/register`

### 逻辑删除
使用MyBatis-Plus的逻辑删除功能，删除操作会设置`deleted`字段为1而非物理删除。

## 已实现的业务功能

### 用户管理
- 用户注册、登录、登出
- 用户信息查询和更新
- 管理员用户管理（CRUD）

### 产品管理
- 产品列表查询（支持分页、搜索、筛选）
- 产品详情查看
- 管理员产品管理（CRUD）
- 产品图片和模板管理

### 保单管理
- 保单创建和查询
- 保单导出功能
- 用户保单列表

### 费用管理
- 费用计算
- 费用记录查询
- 费用统计分析

### 充值缴费
- 用户充值
- 充值记录查询
- 余额管理

### 公告管理
- 公告列表查询
- 管理员公告管理（CRUD）

### 数据统计
- 首页统计数据
- 管理员数据统计面板

## 已清理的冗余代码

以下冗余代码已被删除（2026-04-01）：

### 后端Controller
- `PolicyController.java` - 保单相关API（未被前端调用）
- `PortalController.java` - 门户相关API（未被前端调用）
- `ProductController.java` - 产品相关API（未被前端调用）
- `SupportController.java` - 支持相关API（未被前端调用）
- `UserDashboardController.java` - 用户仪表盘API（未被前端调用）

### 前端组件
- `frontend/src/components/OptimizedProductCard.vue` - 未在任何页面导入使用

### 后端实体和Mapper
- `SysUserEntity.java` - 映射`sys_user`表，项目未使用该表
- `SysUserMapper.java` - 仅有自引用，无消费者

## 待优化问题

### 设计问题
1. `AnXinXuanController.java` 直接注入`AnXinXuanServiceImpl`实现类，违反接口编程原则
2. 存在两个`ProductQuery`类（dto包和query包各一个）
3. `AnXinXuanServiceImpl.exportInsurances()` 方法为空实现

## 开发注意事项

1. **API端点规范**：所有API路径以`/api`开头
2. **跨域配置**：开发环境已配置CORS，允许前端跨域访问
3. **文件上传**：上传文件存储在配置的目录中，默认为系统临时目录
4. **数据库初始化**：项目启动时会自动执行`init_db.sql`初始化数据库表结构
5. **Mock数据**：默认使用内存数据，重启后数据丢失

## 目录结构

```
ytbx/
├── src/main/java/com/zs/ytbx/
│   ├── controller/     # API控制器（12个）
│   ├── service/        # 业务接口和实现（15个实现类）
│   ├── mapper/         # MyBatis Mapper接口（21个）
│   ├── entity/         # 数据实体（22个）
│   ├── dto/            # 数据传输对象
│   ├── query/          # 查询对象
│   ├── config/         # 配置类
│   └── util/           # 工具类
├── frontend/
│   ├── src/
│   │   ├── views/      # 页面组件（4个Vue文件）
│   │   ├── router/     # 路由配置
│   │   └── main.js     # 应用入口
│   └── package.json
├── deploy/             # 部署相关配置
├── docs/               # 项目文档
├── init_db.sql         # 数据库初始化脚本
└── pom.xml             # Maven配置
```

## 关键文件

- `frontend/src/views/Home.vue` - 核心业务页面（约5000行）
- `src/main/java/com/zs/ytbx/controller/AnXinXuanController.java` - 主要业务API
- `src/main/java/com/zs/ytbx/controller/AdminController.java` - 管理后台API
- `src/main/java/com/zs/ytbx/controller/AuthController.java` - 认证相关API
- `application.yml` - 应用配置文件
