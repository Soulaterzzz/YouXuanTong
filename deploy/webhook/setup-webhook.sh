#!/usr/bin/env bash
set -Eeuo pipefail

# 脚本功能：安装并配置 Webhook 自动部署服务（systemd）
# 使用方法：sudo bash deploy/webhook/setup-webhook.sh [选项]
# 参数：
#   --secret SECRET    GitHub Webhook Secret（必填）
#   --port PORT        监听端口（默认 9000）

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
WEBHOOK_SERVER="${SCRIPT_DIR}/webhook-server.py"
SERVICE_NAME="ytbx-webhook"
ENV_DIR="/etc/${SERVICE_NAME}"
ENV_FILE="${ENV_DIR}/env"
SERVICE_UNIT="/etc/systemd/system/${SERVICE_NAME}.service"

WEBHOOK_SECRET=""
WEBHOOK_PORT=9000

# ============================================================================
# 日志和工具函数
# ============================================================================

log() {
  local level="$1"
  shift
  local timestamp
  timestamp="$(date '+%Y-%m-%d %H:%M:%S')"
  echo "[${timestamp}] [${level}] $*"
}

die() {
  log "ERROR" "$1"
  exit 1
}

# ============================================================================
# 参数解析
# ============================================================================

show_usage() {
  cat <<EOF
用法: sudo bash deploy/webhook/setup-webhook.sh [选项]

选项:
  --secret SECRET    GitHub Webhook Secret（必填）
  --port PORT        监听端口（默认 9000）
  -h, --help         显示帮助

不带参数运行时进入交互模式。

配置完成后，在 GitHub 仓库 Settings → Webhooks → Add webhook 中：
  - Payload URL: http://<服务器IP>:9000
  - Content type: application/json
  - Secret: 与 --secret 相同的值
  - Events: Push events（仅 main 分支）
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --secret)
        WEBHOOK_SECRET="$2"
        shift 2
        ;;
      --port)
        WEBHOOK_PORT="$2"
        shift 2
        ;;
      -h|--help)
        show_usage
        exit 0
        ;;
      *)
        die "未知参数: $1（使用 -h 查看帮助）"
        ;;
    esac
  done
}

# ============================================================================
# 交互式输入
# ============================================================================

prompt_interactive() {
  if [[ -n "${WEBHOOK_SECRET}" ]]; then
    return
  fi

  echo "===== Webhook 自动部署配置 ====="
  echo ""

  read -rp "GitHub Webhook Secret（在 GitHub 仓库 Settings → Webhooks 中生成）: " WEBHOOK_SECRET

  if [[ -z "${WEBHOOK_SECRET}" ]]; then
    die "Webhook Secret 不能为空"
  fi

  read -rp "监听端口 [9000]: " input_port
  WEBHOOK_PORT="${input_port:-9000}"
}

# ============================================================================
# 前置检查和安装
# ============================================================================

validate_env() {
  if [[ "${EUID}" -ne 0 ]]; then
    die "请使用 sudo 运行此脚本"
  fi

  if ! command -v python3 >/dev/null 2>&1; then
    die "Python3 未安装，请先运行: apt install python3"
  fi

  if ! command -v docker >/dev/null 2>&1; then
    die "Docker 未安装，请先运行 server-bootstrap-ubuntu.sh"
  fi

  if [[ ! -f "${WEBHOOK_SERVER}" ]]; then
    die "webhook-server.py 不存在: ${WEBHOOK_SERVER}"
  fi

  if [[ ! -f "${PROJECT_ROOT}/deploy/docker-compose.yml" ]]; then
    die "docker-compose.yml 不存在"
  fi
}

# ============================================================================
# 配置服务
# ============================================================================

configure_service() {
  log "INFO" "创建配置目录..."
  mkdir -p "${ENV_DIR}"
  chmod 700 "${ENV_DIR}"

  log "INFO" "写入环境配置..."
  cat > "${ENV_FILE}" <<EOF_ENV
WEBHOOK_SECRET=${WEBHOOK_SECRET}
WEBHOOK_PORT=${WEBHOOK_PORT}
PROJECT_DIR=${PROJECT_ROOT}
DEPLOY_SCRIPT=${SCRIPT_DIR}/webhook-deploy.sh
LOG_FILE=${PROJECT_ROOT}/deploy/webhook-$(date +%Y%m%d).log
EOF_ENV
  chmod 600 "${ENV_FILE}"

  log "INFO" "创建 systemd 服务单元..."
  cat > "${SERVICE_UNIT}" <<EOF_UNIT
[Unit]
Description=YTBX Webhook Auto-Deploy Service
After=network.target docker.service
Requires=docker.service

[Service]
Type=simple
User=root
WorkingDirectory=${PROJECT_ROOT}
EnvironmentFile=${ENV_FILE}
ExecStart=/usr/bin/python3 ${WEBHOOK_SERVER}
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF_UNIT

  systemctl daemon-reload
}

start_service() {
  log "INFO" "启用并启动 Webhook 服务..."

  systemctl enable "${SERVICE_NAME}"
  systemctl restart "${SERVICE_NAME}"

  sleep 2

  if systemctl is-active --quiet "${SERVICE_NAME}"; then
    log "INFO" "Webhook 服务已启动"
  else
    log "ERROR" "Webhook 服务启动失败"
    journalctl -u "${SERVICE_NAME}" --no-pager -n 20
    exit 1
  fi
}

print_summary() {
  local ip
  ip="$(hostname -I 2>/dev/null | awk '{print $1}')"
  ip="${ip:-<服务器IP>}"

  echo ""
  echo "========================================="
  echo "Webhook 自动部署服务安装完成"
  echo "========================================="
  echo "服务名称: ${SERVICE_NAME}"
  echo "监听端口: ${WEBHOOK_PORT}"
  echo "项目目录: ${PROJECT_ROOT}"
  echo "环境配置: ${ENV_FILE}"
  echo ""
  echo "管理命令："
  echo "  查看状态: systemctl status ${SERVICE_NAME}"
  echo "  查看日志: journalctl -u ${SERVICE_NAME} -f"
  echo "  停止服务: systemctl stop ${SERVICE_NAME}"
  echo "  启动服务: systemctl start ${SERVICE_NAME}"
  echo ""
  echo "GitHub Webhook 配置："
  echo "  1. 打开 https://github.com/<OWNER>/<REPO>/settings/hooks/new"
  echo "  2. Payload URL: http://${ip}:${WEBHOOK_PORT}"
  echo "  3. Content type: application/json"
  echo "  4. Secret: ${WEBHOOK_SECRET}"
  echo "  5. Which events: Just the push event → Active → 仅 main 分支"
  echo ""
  echo "防火墙配置（如需外网访问）："
  echo "  sudo ufw allow ${WEBHOOK_PORT}/tcp"
  echo "========================================="
}

# ============================================================================
# 主流程
# ============================================================================

main() {
  parse_args "$@"
  prompt_interactive
  validate_env
  configure_service
  start_service
  print_summary
}

main "$@"
