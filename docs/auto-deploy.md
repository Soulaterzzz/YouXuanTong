# 自动部署配置指南

本项目支持两种自动部署方案，可根据实际需求选择其一或同时使用：

| 方案 | 适用场景 | 优势 |
|------|---------|------|
| **GitHub Actions Runner**（推荐） | 需要完整的 CI/CD 流水线 | 包含构建、测试、部署全流程，有 CI 门控保护 |
| **Webhook 自动部署** | 轻量部署、快速迭代 | 无需 Runner，服务器端直接 git pull + 重建 |

---

## 方案一：GitHub Actions self-hosted Runner

### 原理

```
push main → CI 流水线（构建+测试）→ Deploy 流水线（self-hosted Runner 执行部署）
```

### 前置条件

1. 服务器已通过 `deploy/server-bootstrap-ubuntu.sh` 完成初始化部署
2. 拥有 GitHub 仓库的管理权限

### 配置步骤

#### 1. 在服务器上安装 Runner

**交互模式**（推荐首次使用）：

```bash
sudo bash deploy/setup-runner.sh
```

按提示输入仓库地址和注册 Token。

**命令行模式**：

```bash
sudo bash deploy/setup-runner.sh \
  --repo OWNER/REPO \
  --token REGISTRATION_TOKEN \
  --user ubuntu
```

#### 2. 获取 Runner 注册 Token

**方法一：使用 PAT 自动获取**

```bash
curl -s -X POST \
  -H "Authorization: Bearer YOUR_PERSONAL_ACCESS_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/OWNER/REPO/actions/runners/registration-token \
  | jq -r .token
```

**方法二：GitHub 页面获取**

GitHub 仓库 → Settings → Actions → Runners → New self-hosted runner → 复制 Token

#### 3. 配置 GitHub Secrets

在 GitHub 仓库 Settings → Secrets and variables → Actions 中添加：

| Name | 说明 |
|------|------|
| `YTBX_MYSQL_PASSWORD` | MySQL 用户密码 |
| `YTBX_MYSQL_ROOT_PASSWORD` | MySQL Root 密码 |

在 Variables 中添加（可选）：

| Name | 默认值 | 说明 |
|------|--------|------|
| `YTBX_APP_PORT` | 8080 | 应用端口 |
| `YTBX_HOST_UPLOAD_DIR` | /data/ytbx/uploads | 上传目录 |
| `YTBX_MYSQL_DATABASE` | ytbx | 数据库名 |
| `YTBX_MYSQL_USER` | ytbx | MySQL 用户名 |

#### 4. 验证

访问 GitHub 仓库 Settings → Actions → Runners，确认 Runner 状态为 **Online**。

推送代码到 `main` 分支后，Actions 页面应自动触发 CI 和 Deploy 流水线。

### 管理命令

```bash
# 查看状态
cd /opt/actions-runner && ./svc.sh status

# 停止/启动
cd /opt/actions-runner && ./svc.sh stop
cd /opt/actions-runner && ./svc.sh start

# 卸载
cd /opt/actions-runner && ./svc.sh uninstall
```

---

## 方案二：Webhook 自动部署

### 原理

```
push main → GitHub 发送 Webhook → 服务器 Python 服务接收 → HMAC 验证 → git pull → docker compose up
```

### 前置条件

1. 服务器已通过 `deploy/server-bootstrap-ubuntu.sh` 完成初始化部署
2. 服务器有 Python3（Ubuntu 22.04+ 默认已安装）

### 配置步骤

#### 1. 在服务器上安装 Webhook 服务

**交互模式**：

```bash
sudo bash deploy/webhook/setup-webhook.sh
```

**命令行模式**：

```bash
sudo bash deploy/webhook/setup-webhook.sh \
  --secret "your_webhook_secret_here" \
  --port 9000
```

#### 2. 在 GitHub 配置 Webhook

1. 打开 GitHub 仓库 Settings → Webhooks → Add webhook
2. 填写配置：

| 字段 | 值 |
|------|-----|
| Payload URL | `http://<服务器IP>:9000` |
| Content type | `application/json` |
| Secret | 与 `--secret` 相同的值 |
| Which events | **Just the push event** |

3. 在 Active 下方，点击 **Add webhook**

#### 3. 防火墙配置（如需外网访问）

```bash
sudo ufw allow 9000/tcp
```

> 建议使用 Nginx 反向代理 + HTTPS 来暴露 Webhook 端点，避免直接暴露端口。

#### 4. 验证

在 GitHub Webhook 页面查看 Recent Deliveries，最新一条应显示 **200** 状态码。

### 管理命令

```bash
# 查看状态
systemctl status ytbx-webhook

# 查看日志
journalctl -u ytbx-webhook -f

# 停止/启动
systemctl stop ytbx-webhook
systemctl start ytbx-webhook

# 健康检查
curl http://127.0.0.1:9000
```

---

## 两种方案对比

| 特性 | Actions Runner | Webhook |
|------|---------------|---------|
| CI 测试门控 | 有 | 无 |
| 并发控制 | GitHub concurrency | flock 文件锁 |
| 健康检查 | 有 | 有 |
| 失败回滚 | 有 | 有 |
| 部署日志 | GitHub Actions 页面 + 服务器日志 | 服务器 deploy-*.log |
| 额外依赖 | Actions Runner (~200MB) | 无（Python3 标准库） |
| 配置复杂度 | 中等 | 低 |

**推荐**：日常使用 Actions Runner 方案，Webhook 作为快速验证或备用通道。

---

## 故障排查

### Actions Runner 不上线

```bash
# 检查服务状态
cd /opt/actions-runner && ./svc.sh status

# 查看详细日志
journalctl -u actions.runner.* -n 50

# 重新配置
cd /opt/actions-runner
./config.sh remove --token OLD_TOKEN
./config.sh --url https://github.com/OWNER/REPO --token NEW_TOKEN --unattended
./svc.sh start
```

### Webhook 未触发部署

```bash
# 检查服务状态
systemctl status ytbx-webhook

# 检查签名验证
journalctl -u ytbx-webhook -n 20

# 确认 Secret 一致
cat /etc/ytbx-webhook/env | grep WEBHOOK_SECRET
```

### 部署后服务异常

```bash
# 查看应用日志
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f ytbx-app

# 手动回滚
docker tag ytbx-app:rollback ytbx-app:latest
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d ytbx-app
```
