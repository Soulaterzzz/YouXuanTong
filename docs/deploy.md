# 项目部署说明

## 1. 部署目标

当前仓库采用以下部署形态：

- `Nginx`：对外暴露 `80/443`，转发到应用容器。
- `ytbx-app`：Spring Boot 单容器，对外提供接口和前端静态资源。
- `mysql`：通过 `Docker Compose` 一并部署，首次启动时自动执行根目录 `init_db.sql`。
- `uploads`：服务器本地持久化目录，保存产品图片和模板文件。
- `GitHub Actions`：负责 CI；生产环境通过 `self-hosted runner` 执行 CD。

## 2. 本次落地文件

- `Dockerfile`
- `.dockerignore`
- `deploy/docker-compose.yml`
- `deploy/.env.example`
- `deploy/nginx/ytbx.conf.example`
- `.github/workflows/ci.yml`
- `.github/workflows/deploy.yml`

## 3. 代码侧已处理的部署点

- 前端接口基址改为 `VITE_API_BASE_URL` 或同源默认值，不再写死 `localhost`。
- 后端数据库密码不再保留仓库内默认值。
- 文件上传目录默认改为相对路径，便于本地开发和容器部署。
- 生产环境启用 `server.forward-headers-strategy=framework`，适配反向代理。

## 4. 服务器准备

### 4.1 基础环境

需要一台 Linux 服务器，并安装：

- Docker
- Docker Compose Plugin
- Git
- Nginx
- GitHub Actions self-hosted runner

### 4.2 推荐目录

```bash
sudo mkdir -p /opt/ytbx
sudo mkdir -p /data/ytbx/uploads/templates
```

### 4.3 服务器初始化清单

上线前建议逐项确认：

- 已确认服务器具备 `sudo` 权限。
- 已确认域名解析到当前服务器。
- 已开放 `80/443` 端口；若临时直连应用，还需开放 `8080`。
- 已安装 `Docker`、`Docker Compose Plugin`、`Git`、`Nginx`。
- 已安装 GitHub `self-hosted runner`，且 runner 用户可执行 `docker compose`。
- 已创建 `/opt/ytbx` 和 `/data/ytbx/uploads/templates` 目录。
- 已预留足够磁盘空间给镜像、数据库和上传文件。
- 已准备 HTTPS 证书方案，常见方式是 `Certbot` 或云厂商证书服务。

## 5. 手工部署步骤

### 5.1 拉取代码

```bash
cd /opt/ytbx
git clone <你的仓库地址> app
cd app
```

### 5.2 配置环境变量

```bash
cp deploy/.env.example deploy/.env
```

按实际环境修改 `deploy/.env`：

- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `HOST_UPLOAD_DIR`
- `APP_PORT`

### 5.3 启动服务

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
```

### 5.4 检查状态

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f ytbx-app
```

默认访问：

- 应用：`http://服务器IP:8080`
- 数据库：Compose 内部网络访问，不默认暴露到公网

### 5.5 首次上线命令清单

首次在新服务器上部署时，可按下面顺序执行：

```bash
cd /opt/ytbx
git clone <你的仓库地址> app
cd /opt/ytbx/app
cp deploy/.env.example deploy/.env
vi deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f ytbx-app
```

## 6. Nginx 反向代理

示例配置见 `deploy/nginx/ytbx.conf.example`。

典型流程：

```bash
sudo cp deploy/nginx/ytbx.conf.example /etc/nginx/conf.d/ytbx.conf
sudo nginx -t
sudo systemctl reload nginx
```

上线 HTTPS 时建议配合 `Certbot` 或云厂商证书服务。

## 7. GitHub Actions CI/CD

### 7.1 CI 行为

`/.github/workflows/ci.yml` 会在 `pull_request` 和 `main/develop` 推送时执行：

- 前端安装依赖
- 前端测试
- 前端构建
- 后端测试
- 后端打包
- Docker 镜像构建校验

### 7.2 CD 行为

`/.github/workflows/deploy.yml` 会在 `main` 分支推送或手动触发时执行：

- 在 `self-hosted runner` 所在服务器拉取最新代码
- 根据 GitHub Secrets / Variables 生成 `deploy/.env`
- 创建上传目录
- 执行 `docker compose up -d --build`

### 7.3 GitHub 需要配置的 Secrets

- `YTBX_MYSQL_PASSWORD`
- `YTBX_MYSQL_ROOT_PASSWORD`

### 7.4 GitHub 可选 Variables

- `YTBX_APP_PORT`
- `YTBX_HOST_UPLOAD_DIR`
- `YTBX_MYSQL_DATABASE`
- `YTBX_MYSQL_USER`
- `YTBX_PRODUCT_IMAGE_PATH`
- `YTBX_MAX_FILE_SIZE`
- `YTBX_ALLOWED_EXTENSIONS`
- `YTBX_SQL_INIT_MODE`

### 7.5 GitHub 配置检查清单

启用自动部署前建议确认：

- 仓库已配置 `YTBX_MYSQL_PASSWORD`。
- 仓库已配置 `YTBX_MYSQL_ROOT_PASSWORD`。
- 如需自定义端口、路径、数据库名，已配置对应 Variables。
- `self-hosted runner` 已在线，并绑定到正确仓库或组织。
- 生产环境使用的 GitHub Environment 已配置完成。

## 8. 发布与回滚

### 8.1 发布

- 合并代码到 `main`。
- 等待 CI 通过。
- 自动或手动触发 `Deploy`。
- 验证首页、登录、管理员页面、文件上传。

### 8.2 发布前检查清单

正式发布前建议逐项确认：

- 当前分支代码已合并，且 CI 全部通过。
- 数据库已完成备份。
- 上传目录已确认存在并可写。
- `deploy/.env` 或 GitHub Secrets/Variables 已更新到本次发布所需配置。
- 域名、Nginx、HTTPS 配置可用。
- 回滚提交号已确认，可在异常时快速切换。

### 8.3 回滚

如果本次发布异常：

```bash
cd /opt/ytbx/app
git log --oneline -n 5
git checkout <上一个稳定提交>
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
```

### 8.4 回滚检查清单

回滚后至少确认：

- 首页可访问。
- 登录成功。
- 管理员首页与产品列表可正常加载。
- 文件上传与下载功能恢复正常。
- 应用日志中无持续异常。

## 9. 发布后检查

至少检查以下内容：

- 登录成功，接口返回正常。
- 管理端用户、产品、通知可正常操作。
- 产品图片上传与模板下载正常。
- `uploads` 目录有持久化文件。
- 应用容器和 MySQL 容器状态为 `healthy/running`。
- Nginx 访问日志与应用日志无连续报错。

### 9.1 上线验收清单

发布完成后建议执行一轮完整验收：

- 页面验收：首页、登录页、管理员页均可正常打开。
- 功能验收：登录、用户管理、产品上下架、通知发布、产品激活、批量生效全部正常。
- 文件验收：图片上传、模板上传、模板下载正常。
- 数据验收：核心业务数据可查询，新增记录正常落库。
- 服务验收：应用重启后仍可正常访问，上传文件不丢失。
- 代理验收：Nginx 反向代理正常，客户端 IP 与协议头透传正常。

## 10. 当前已知限制

- 当前 token 存储在内存中，适合单机部署，不适合多实例水平扩容。
- 数据库首次初始化依赖 `init_db.sql`，已有生产库不要重复执行初始化。
- 如果未来需要蓝绿发布、滚动发布或多副本，需要先把认证态迁移到 Redis 等集中式存储。
