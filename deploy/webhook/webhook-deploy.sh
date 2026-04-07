#!/usr/bin/env bash
set -Eeuo pipefail

# 脚本功能：Webhook 触发的部署执行脚本
# 使用方法：由 webhook-server.py 自动调用，也可手动执行
# 参数：$1=项目目录  $2=提交SHA  $3=推送者
# 环境变量：COMPOSE_FILE, ENV_FILE 可覆盖默认路径

PROJECT_DIR="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
COMMIT_SHA="${2:-unknown}"
PUSHER="${3:-webhook}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${DEPLOY_DIR}/.env"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.yml"
LOG_FILE="${DEPLOY_DIR}/deploy-webhook-$(date +%Y%m%d_%H%M%S).log"

# ============================================================================
# 日志和工具函数
# ============================================================================

log() {
  local level="$1"
  shift
  local timestamp
  timestamp="$(date '+%Y-%m-%d %H:%M:%S')"
  local msg="[${timestamp}] [${level}] $*"
  echo "${msg}" | tee -a "${LOG_FILE}"
}

# ============================================================================
# 前置检查
# ============================================================================

validate_env() {
  if [[ ! -f "${COMPOSE_FILE}" ]]; then
    log "ERROR" "docker-compose.yml 不存在: ${COMPOSE_FILE}"
    return 1
  fi

  if [[ ! -f "${ENV_FILE}" ]]; then
    log "ERROR" ".env 不存在: ${ENV_FILE}，请先运行 server-bootstrap-ubuntu.sh"
    return 1
  fi

  if ! command -v docker >/dev/null 2>&1; then
    log "ERROR" "Docker 未安装"
    return 1
  fi

  if ! docker compose version >/dev/null 2>&1; then
    log "ERROR" "Docker Compose 不可用"
    return 1
  fi

  if [[ ! -d "${PROJECT_DIR}/.git" ]]; then
    log "ERROR" "不是 Git 仓库: ${PROJECT_DIR}"
    return 1
  fi
}

# ============================================================================
# 部署流程
# ============================================================================

git_pull() {
  log "INFO" "拉取最新代码 (commit: ${COMMIT_SHA})..."
  cd "${PROJECT_DIR}"

  if git pull origin main >> "${LOG_FILE}" 2>&1; then
    log "INFO" "代码更新成功"
  else
    log "ERROR" "代码拉取失败"
    return 1
  fi
}

tag_current_image() {
  if docker image inspect ytbx-app:latest >/dev/null 2>&1; then
    docker tag ytbx-app:latest ytbx-app:rollback
    ROLLBACK_AVAILABLE=1
    log "INFO" "已标记当前镜像为 ytbx-app:rollback"
  else
    ROLLBACK_AVAILABLE=0
    log "INFO" "首次部署，无回滚镜像"
  fi
}

docker_deploy() {
  log "INFO" "构建并启动服务..."
  local start_time
  start_time=$(date +%s)

  if docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" \
      up -d --build --remove-orphans >> "${LOG_FILE}" 2>&1; then
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - start_time))
    log "INFO" "服务构建启动成功（耗时 ${duration} 秒）"
  else
    log "ERROR" "服务构建启动失败"
    return 1
  fi
}

health_check() {
  source "${ENV_FILE}"
  local app_port="${APP_PORT:-8080}"
  local health_url="http://127.0.0.1:${app_port}"
  local max_attempts=30
  local attempt=0

  log "INFO" "等待服务启动..."

  while [[ ${attempt} -lt ${max_attempts} ]]; do
    if curl -sf "${health_url}" >/dev/null 2>&1; then
      log "INFO" "服务健康检查通过 (等待 ${attempt} 次)"
      return 0
    fi
    attempt=$((attempt + 1))
    log "INFO" "等待中... (${attempt}/${max_attempts})"
    sleep 2
  done

  log "WARN" "服务健康检查超时"
  return 1
}

rollback() {
  if [[ "${ROLLBACK_AVAILABLE}" -ne 1 ]]; then
    log "ERROR" "无可用回滚镜像"
    return 1
  fi

  log "WARN" "开始回滚到上一版本..."

  if docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" \
      up -d ytbx-app >> "${LOG_FILE}" 2>&1; then
    docker tag ytbx-app:rollback ytbx-app:latest
    log "INFO" "回滚成功"
    return 0
  else
    log "ERROR" "回滚失败，请手动检查"
    return 1
  fi
}

cleanup() {
  log "INFO" "清理旧镜像..."
  docker image prune -f >> "${LOG_FILE}" 2>&1 || true
}

# ============================================================================
# 主流程
# ============================================================================

main() {
  log "INFO" "===== 部署开始 ====="
  log "INFO" "推送者: ${PUSHER}  提交: ${COMMIT_SHA}"

  validate_env || exit 1
  git_pull || exit 1
  tag_current_image

  if docker_deploy; then
    if health_check; then
      cleanup
      log "INFO" "===== 部署成功 ====="
      exit 0
    fi
  fi

  # 部署或健康检查失败，尝试回滚
  log "WARN" "部署异常，尝试回滚..."
  rollback || true
  log "INFO" "===== 部署结束（已回滚） ====="
  exit 1
}

main
