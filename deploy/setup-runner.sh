#!/usr/bin/env bash
set -Eeuo pipefail

# 脚本功能：安装并配置 GitHub Actions self-hosted Runner
# 使用方法：sudo bash deploy/setup-runner.sh --repo OWNER/REPO --token TOKEN
#           或交互模式：sudo bash deploy/setup-runner.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

RUNNER_DIR="/opt/actions-runner"
RUNNER_SERVICE_NAME="actions.runner.ytbx"
REPO=""
RUNNER_TOKEN=""
RUNNER_USER=""
RUNNER_LABELS="self-hosted,linux,x64"
RUNNER_NAME=""

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
用法: sudo bash deploy/setup-runner.sh [选项]

选项:
  --repo OWNER/REPO    GitHub 仓库（必填，例如：myorg/ytbx）
  --token TOKEN        Runner 注册 Token（必填）
  --user USERNAME      运行 Runner 的系统用户（默认：当前 sudo 用户）
  --name NAME          Runner 显示名称（默认：主机名）
  --labels LABELS      Runner 标签，逗号分隔（默认：self-hosted,linux,x64）
  -h, --help           显示帮助

不带参数运行时进入交互模式。

获取注册 Token：
  方法一（推荐）：Personal Access Token (PAT) + 仓库管理权限
    curl -s -X POST -H "Authorization: Bearer <PAT>" \
      -H "Accept: application/vnd.github+json" \
      https://api.github.com/repos/OWNER/REPO/actions/runners/registration-token | jq -r .token

  方法二：GitHub 仓库页面 → Settings → Actions → Runners → New self-hosted runner → 获取 Token
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --repo)
        REPO="$2"
        shift 2
        ;;
      --token)
        RUNNER_TOKEN="$2"
        shift 2
        ;;
      --user)
        RUNNER_USER="$2"
        shift 2
        ;;
      --name)
        RUNNER_NAME="$2"
        shift 2
        ;;
      --labels)
        RUNNER_LABELS="$2"
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
  if [[ -n "${REPO}" && -n "${RUNNER_TOKEN}" ]]; then
    return
  fi

  echo "===== GitHub Actions Runner 配置 ====="
  echo ""

  if [[ -z "${REPO}" ]]; then
    read -rp "GitHub 仓库 (例如 myorg/ytbx): " REPO
  fi

  if [[ -z "${REPO}" ]]; then
    die "仓库地址不能为空"
  fi

  if [[ -z "${RUNNER_TOKEN}" ]]; then
    echo ""
    echo "获取注册 Token（方法一：PAT 自动获取，方法二：GitHub 页面手动获取）"
    read -rp "Runner 注册 Token: " RUNNER_TOKEN
  fi

  if [[ -z "${RUNNER_TOKEN}" ]]; then
    die "注册 Token 不能为空"
  fi
}

# ============================================================================
# 前置检查
# ============================================================================

validate_env() {
  if [[ "${EUID}" -ne 0 ]]; then
    die "请使用 sudo 运行此脚本"
  fi

  if [[ -z "${RUNNER_USER}" ]]; then
    RUNNER_USER="${SUDO_USER:-}"
  fi

  if [[ -z "${RUNNER_USER}" ]]; then
    die "无法确定运行用户，请使用 --user 指定"
  fi

  if ! id -u "${RUNNER_USER}" >/dev/null 2>&1; then
    die "用户 ${RUNNER_USER} 不存在"
  fi

  if ! command -v docker >/dev/null 2>&1; then
    die "Docker 未安装，请先运行 server-bootstrap-ubuntu.sh"
  fi

  if ! id -nG "${RUNNER_USER}" | grep -qw docker; then
    log "WARN" "用户 ${RUNNER_USER} 不在 docker 组中，Runner 可能无法执行 Docker 命令"
  fi

  # 检查是否已安装
  if [[ -d "${RUNNER_DIR}" ]]; then
    log "WARN" "Runner 已安装于 ${RUNNER_DIR}，将尝试重新配置"
  fi
}

# ============================================================================
# Runner 安装和配置
# ============================================================================

detect_arch() {
  local arch
  arch="$(uname -m)"
  case "${arch}" in
    x86_64) echo "x64" ;;
    aarch64|arm64) echo "arm64" ;;
    *) die "不支持的架构: ${arch}" ;;
  esac
}

download_runner() {
  # 检查是否已安装
  if [[ -d "${RUNNER_DIR}" ]] && [[ -f "${RUNNER_DIR}/config.sh" ]]; then
    log "INFO" "Runner 已安装于 ${RUNNER_DIR}，跳过下载"
    return 0
  fi

  local arch
  arch="$(detect_arch)"
  local runner_os="linux"

  # 使用固定版本号（避免 API 请求卡住）
  local latest_version="v2.333.1"
  log "INFO" "使用 Actions Runner 版本 ${latest_version}"

  local pkg_name="actions-runner-${runner_os}-${arch}-${latest_version#v}.tar.gz"
  local original_url="https://github.com/actions/runner/releases/download/${latest_version}/${pkg_name}"

  # 腾讯云 GitHub 加速镜像（https://mirrors.cloud.tencent.com/github）
  local tencent_mirror="https://mirrors.cloud.tencent.com/github"
  local download_url="${tencent_mirror}/actions/runner/releases/download/${latest_version}/${pkg_name}"

  # 备用镜像列表
  local backup_mirrors=(
    "${original_url}"
  )

  log "INFO" "下载 Actions Runner ${latest_version#v} (${arch})..."

  mkdir -p "${RUNNER_DIR}"

  # 尝试主镜像（腾讯云）
  log "INFO" "尝试使用腾讯云加速镜像..."
  if curl -fsSL --max-time 300 -o "/tmp/${pkg_name}" "${download_url}"; then
    log "INFO" "下载成功"
  else
    # 尝试备用镜像
    local success=0
    for mirror in "${backup_mirrors[@]}"; do
      log "INFO" "尝试备用镜像: ${mirror}"
      if curl -fsSL --max-time 300 -o "/tmp/${pkg_name}" "${mirror}"; then
        log "INFO" "下载成功"
        success=1
        break
      fi
    done

    if [[ "${success}" -eq 0 ]]; then
      die "下载 Runner 失败，所有镜像源均无法访问"
    fi
  fi

  log "INFO" "解压 Runner 到 ${RUNNER_DIR}..."
  tar xzf "/tmp/${pkg_name}" -C "${RUNNER_DIR}"
  rm -f "/tmp/${pkg_name}"
  log "INFO" "Runner 下载并解压完成"
}

configure_runner() {
  log "INFO" "配置 Runner..."

  local runner_name="${RUNNER_NAME:-$(hostname)}"

  cd "${RUNNER_DIR}"

  if [[ -f ".runner" ]]; then
    log "INFO" "Runner 已配置过，执行卸载..."
    ./svc.sh uninstall || true
    rm -f ".runner"
  fi

  # 配置 Runner
  if ! ./config.sh \
    --url "https://github.com/${REPO}" \
    --token "${RUNNER_TOKEN}" \
    --name "${runner_name}" \
    --labels "${RUNNER_LABELS}" \
    --unattended \
    --replace \
    --ephemeral; then
    die "Runner 配置失败，请检查 Token 和仓库地址是否正确"
  fi

  log "INFO" "Runner 配置完成"
}

install_service() {
  log "INFO" "安装 systemd 服务..."

  cd "${RUNNER_DIR}"

  # 安装为 systemd 服务
  ./svc.sh install "${RUNNER_USER}" || die "systemd 服务安装失败"

  # 启动服务
  ./svc.sh start || die "Runner 服务启动失败"

  log "INFO" "Runner 服务已启动"
}

verify_runner() {
  log "INFO" "验证 Runner 状态..."

  # 等待服务启动
  sleep 3

  if ./svc.sh status >/dev/null 2>&1; then
    log "INFO" "Runner 运行正常"
  else
    log "WARN" "Runner 状态检查失败，请手动检查: cd ${RUNNER_DIR} && ./svc.sh status"
  fi
}

print_summary() {
  echo ""
  echo "========================================="
  echo "GitHub Actions Runner 安装完成"
  echo "========================================="
  echo "仓库: ${REPO}"
  echo "安装目录: ${RUNNER_DIR}"
  echo "运行用户: ${RUNNER_USER}"
  echo "标签: ${RUNNER_LABELS}"
  echo ""
  echo "管理命令："
  echo "  查看状态: cd ${RUNNER_DIR} && ./svc.sh status"
  echo "  停止:     cd ${RUNNER_DIR} && ./svc.sh stop"
  echo "  启动:     cd ${RUNNER_DIR} && ./svc.sh start"
  echo "  卸载:     cd ${RUNNER_DIR} && ./svc.sh uninstall"
  echo ""
  echo "配置完成后，请前往 GitHub 仓库页面确认 Runner 已上线："
  echo "  https://github.com/${REPO}/settings/actions/runners"
  echo "========================================="
}

# ============================================================================
# 主流程
# ============================================================================

main() {
  parse_args "$@"
  prompt_interactive
  validate_env
  download_runner
  configure_runner
  install_service
  verify_runner
  print_summary
}

main "$@"
