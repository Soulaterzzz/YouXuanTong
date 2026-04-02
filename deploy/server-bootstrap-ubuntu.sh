#!/usr/bin/env bash
set -Eeuo pipefail

# 脚本功能：Ubuntu 服务器自动化部署脚本，用于配置 Docker、Nginx 和应用服务
# 使用方法：sudo bash deploy/server-bootstrap-ubuntu.sh
# 环境要求：Ubuntu 22.04+/24.04+

# ============================================================================
# 全局变量和配置
# ============================================================================

if [[ "${EUID}" -ne 0 ]]; then
  echo "请使用 sudo 运行：sudo bash deploy/server-bootstrap-ubuntu.sh" >&2
  exit 1
fi

if [[ ! -f /etc/os-release ]]; then
  echo "无法识别当前系统，脚本仅支持 Ubuntu。" >&2
  exit 1
fi

. /etc/os-release
if [[ "${ID:-}" != "ubuntu" ]]; then
  echo "当前脚本仅支持 Ubuntu 22.04+/24.04+。" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DEPLOY_ENV_FILE="${PROJECT_ROOT}/deploy/.env"
LOG_FILE="${PROJECT_ROOT}/deploy/deploy-$(date +%Y%m%d_%H%M%S).log"
DOCKER_GPG_KEY="/etc/apt/keyrings/docker.asc"
DOCKER_SOURCES="/etc/apt/sources.list.d/docker.sources"

APP_PORT="${APP_PORT:-8080}"
HOST_UPLOAD_DIR="${HOST_UPLOAD_DIR:-/data/ytbx/uploads}"
MYSQL_DATABASE="${MYSQL_DATABASE:-ytbx}"
MYSQL_USER="${MYSQL_USER:-ytbx}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
PRODUCT_IMAGE_PATH="${PRODUCT_IMAGE_PATH:-products}"
MAX_FILE_SIZE="${MAX_FILE_SIZE:-10485760}"
ALLOWED_EXTENSIONS="${ALLOWED_EXTENSIONS:-jpg,jpeg,png,gif,webp}"
YTBX_SQL_INIT_MODE="${YTBX_SQL_INIT_MODE:-never}"
SERVER_NAME="${SERVER_NAME:-_}"
ENABLE_NGINX="${ENABLE_NGINX:-1}"
FORCE_WRITE_ENV="${FORCE_WRITE_ENV:-0}"
PRIMARY_USER="${SUDO_USER:-}"
LOCAL_IP="127.0.0.1"
INTERACTIVE_PROMPTS="0"
TTY_DEVICE="/dev/tty"
VALIDATION_ERROR=""

# 记录日志函数
log() {
  local level="$1"
  shift
  local message="$*"
  local timestamp="$(date '+%Y-%m-%d %H:%M:%S')"
  echo "[${timestamp}] [${level}] ${message}" | tee -a "${LOG_FILE}"
}

if [[ ! -f "${PROJECT_ROOT}/deploy/docker-compose.yml" ]]; then
  echo "未找到 deploy/docker-compose.yml，请在项目根目录内运行此脚本。" >&2
  exit 1
fi

# ============================================================================
# 包安装函数
# ============================================================================

# 检查单个包是否已安装
check_package_installed() {
  local pkg="$1"
  dpkg -l "${pkg}" >/dev/null 2>&1
}

# 一次性检查多个包，返回未安装的包列表
get_missing_packages() {
  local missing=()
  for pkg in "$@"; do
    if ! check_package_installed "${pkg}"; then
      missing+=("${pkg}")
    fi
  done
  echo "${missing[*]}"
}

# 优先配置国内 APT 源（防止国外源下载失败）
configure_chinese_apt_mirrors() {
  log "INFO" "配置国内 APT 镜像源..."

  # 备份原始 sources.list
  if [[ -f /etc/apt/sources.list ]] && [[ ! -f /etc/apt/sources.list.backup ]]; then
    cp /etc/apt/sources.list /etc/apt/sources.list.backup
    log "INFO" "已备份原始 sources.list"
  fi

  # 检测 Ubuntu 版本代号
  local codename="${UBUNTU_CODENAME:-${VERSION_CODENAME}}"
  if [[ -z "${codename}" ]]; then
    codename=$(lsb_release -cs 2>/dev/null || echo "jammy")
  fi

  # 国内镜像源列表（按优先级排序）
  local mirrors=(
    "https://mirrors.aliyun.com/ubuntu"
    "https://mirrors.tuna.tsinghua.edu.cn/ubuntu"
    "https://mirrors.ustc.edu.cn/ubuntu"
  )

  # 尝试使用可用的镜像源
  local selected_mirror=""
  for mirror in "${mirrors[@]}"; do
    if curl -fsSL "${mirror}/dists/${codename}/Release" -o /dev/null 2>&1; then
      selected_mirror="${mirror}"
      break
    fi
  done

  if [[ -n "${selected_mirror}" ]]; then
    log "INFO" "使用国内镜像源: ${selected_mirror}"

    # 写入新的 sources.list
    cat > /etc/apt/sources.list <<EOF
deb ${selected_mirror}/ ${codename} main restricted universe multiverse
deb ${selected_mirror}/ ${codename}-updates main restricted universe multiverse
deb ${selected_mirror}/ ${codename}-backports main restricted universe multiverse
deb ${selected_mirror}/ ${codename}-security main restricted universe multiverse
EOF
    log "INFO" "国内 APT 镜像源配置完成"
    return 0
  else
    log "WARN" "无法连接到国内镜像源，使用原始源"
    return 1
  fi
}

# 更新包索引（只执行一次）
update_package_index() {
  log "INFO" "更新软件包索引..."
  export DEBIAN_FRONTEND=noninteractive
  if apt-get update > >(tee -a "${LOG_FILE}") 2>&1; then
    log "INFO" "软件包索引更新成功"
    return 0
  else
    log "ERROR" "软件包索引更新失败，尝试配置国内源..."
    configure_chinese_apt_mirrors
    if apt-get update > >(tee -a "${LOG_FILE}") 2>&1; then
      log "INFO" "使用国内源后软件包索引更新成功"
      return 0
    fi
    return 1
  fi
}

# 安装基础软件包和 Docker（合并为一次安装）
install_all_packages() {
  local base_packages="ca-certificates curl git gnupg nginx openssl"
  local docker_packages="docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin"

  # 检查需要安装的包
  local missing_base=($(get_missing_packages ${base_packages}))
  local missing_packages=("${missing_base[@]}")

  # 检查 Docker 是否已安装
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    log "INFO" "Docker 已安装，跳过 Docker 安装"
  else
    log "INFO" "Docker 未安装或版本不兼容"
    missing_packages+=(${docker_packages})
  fi

  if [[ ${#missing_packages[@]} -eq 0 ]]; then
    log "INFO" "所有依赖包已安装"
    return 0
  fi

  log "INFO" "即将安装以下软件包: ${missing_packages[*]}"
  if apt-get install -y "${missing_packages[@]}" > >(tee -a "${LOG_FILE}") 2>&1; then
    log "INFO" "软件包安装成功"
    return 0
  else
    log "ERROR" "软件包安装失败"
    return 1
  fi
}

enable_interactive_prompts() {
  if [[ -r "${TTY_DEVICE}" && -w "${TTY_DEVICE}" ]]; then
    INTERACTIVE_PROMPTS="1"
  fi
}

load_existing_env_defaults() {
  if [[ ! -f "${DEPLOY_ENV_FILE}" ]]; then
    return
  fi

  . "${DEPLOY_ENV_FILE}"
}

print_notice() {
  local message="$1"

  if [[ "${INTERACTIVE_PROMPTS}" == "1" ]]; then
    echo "${message}" > "${TTY_DEVICE}"
  else
    echo "${message}" >&2
  fi
}

print_validation_error() {
  local label="$1"

  print_notice "${label}无效：${VALIDATION_ERROR}"
}

validate_flag() {
  local value="$1"

  if [[ "${value}" =~ ^[01]$ ]]; then
    return 0
  fi

  VALIDATION_ERROR="请输入 0 或 1"
  return 1
}

validate_app_port() {
  local value="$1"

  if [[ ! "${value}" =~ ^[0-9]+$ ]]; then
    VALIDATION_ERROR="请输入 1-65535 之间的整数"
    return 1
  fi

  if (( 10#${value} < 1 || 10#${value} > 65535 )); then
    VALIDATION_ERROR="端口范围必须在 1-65535 之间"
    return 1
  fi

  # 检查端口是否被占用
  if command -v ss >/dev/null 2>&1 && ss -tuln | grep -q ":${value} "; then
    VALIDATION_ERROR="端口 ${value} 已被占用，请选择其他端口"
    return 1
  fi

  return 0
}

validate_host_upload_dir() {
  local value="$1"

  if [[ -z "${value}" ]]; then
    VALIDATION_ERROR="上传目录不能为空"
    return 1
  fi

  if [[ "${value}" != /* ]]; then
    VALIDATION_ERROR="上传目录必须是绝对路径，例如 /data/ytbx/uploads"
    return 1
  fi

  if [[ "${value}" =~ [[:space:]] ]]; then
    VALIDATION_ERROR="上传目录不能包含空白字符"
    return 1
  fi

  return 0
}

validate_mysql_database() {
  local value="$1"

  if [[ ! "${value}" =~ ^[A-Za-z0-9_]+$ ]]; then
    VALIDATION_ERROR="仅支持字母、数字和下划线"
    return 1
  fi

  return 0
}

validate_mysql_user() {
  local value="$1"

  if [[ ! "${value}" =~ ^[A-Za-z0-9_]+$ ]]; then
    VALIDATION_ERROR="仅支持字母、数字和下划线"
    return 1
  fi

  if (( ${#value} > 32 )); then
    VALIDATION_ERROR="用户名长度不能超过 32 个字符"
    return 1
  fi

  return 0
}

validate_mysql_password() {
  local value="$1"
  local allow_empty="${2:-0}"

  if [[ -z "${value}" ]]; then
    if [[ "${allow_empty}" == "1" ]]; then
      return 0
    fi

    VALIDATION_ERROR="密码不能为空；如需自动生成，请选择重写 deploy/.env"
    return 1
  fi

  if (( ${#value} < 8 )); then
    VALIDATION_ERROR="密码长度至少 8 位；留空可自动生成随机密码"
    return 1
  fi

  if [[ "${value}" =~ [[:space:]] ]]; then
    VALIDATION_ERROR="密码不能包含空白字符"
    return 1
  fi

  return 0
}

validate_product_image_path() {
  local value="$1"

  if [[ -z "${value}" ]]; then
    VALIDATION_ERROR="商品图片子目录不能为空"
    return 1
  fi

  if [[ "${value}" == /* ]]; then
    VALIDATION_ERROR="商品图片子目录应为相对路径，例如 products"
    return 1
  fi

  if [[ "${value}" == *".."* ]]; then
    VALIDATION_ERROR="商品图片子目录不能包含 .."
    return 1
  fi

  if [[ ! "${value}" =~ ^[A-Za-z0-9._/-]+$ ]]; then
    VALIDATION_ERROR="仅支持字母、数字、点、下划线、中划线和斜杠"
    return 1
  fi

  return 0
}

validate_max_file_size() {
  local value="$1"

  if [[ ! "${value}" =~ ^[0-9]+$ ]]; then
    VALIDATION_ERROR="请输入大于 0 的整数，单位为字节（如：10485760 表示 10MB）"
    return 1
  fi

  if (( 10#${value} <= 0 )); then
    VALIDATION_ERROR="最大上传大小必须大于 0"
    return 1
  fi

  return 0
}

validate_allowed_extensions() {
  local value="$1"

  if [[ ! "${value}" =~ ^[A-Za-z0-9]+(,[A-Za-z0-9]+)*$ ]]; then
    VALIDATION_ERROR="请使用逗号分隔扩展名，例如 jpg,jpeg,png"
    return 1
  fi

  return 0
}

validate_sql_init_mode() {
  local value="$1"

  case "${value}" in
    never|embedded|always)
      return 0
      ;;
    *)
      VALIDATION_ERROR="仅支持 never、embedded 或 always"
      return 1
      ;;
  esac
}

validate_server_name() {
  local value="$1"
  local invalid_pattern='[;$\\]'

  if [[ -z "${value}" ]]; then
    VALIDATION_ERROR="Nginx server_name 不能为空，可填写 _"
    return 1
  fi

  if [[ "${value}" =~ ${invalid_pattern} || "${value}" == *$'\n'* ]]; then
    VALIDATION_ERROR="不能包含 ;、$、反斜杠或换行符"
    return 1
  fi

  return 0
}

validate_value() {
  local validator="$1"
  local value="$2"

  shift 2
  VALIDATION_ERROR=""
  "${validator}" "${value}" "$@"
}

ensure_valid_value() {
  local label="$1"
  local value="$2"
  local validator="$3"

  shift 3
  if validate_value "${validator}" "${value}" "$@"; then
    return 0
  fi

  echo "${label}无效：${VALIDATION_ERROR}" >&2
  return 1
}

prompt_text_input() {
  local var_name="$1"
  local prompt_label="$2"
  local validator="${3:-}"
  local default_value="${!var_name:-}"
  local user_input=""
  local candidate_value=""

  if [[ "${INTERACTIVE_PROMPTS}" != "1" ]]; then
    return
  fi

  while true; do
    if [[ -n "${default_value}" ]]; then
      printf "%s [%s]: " "${prompt_label}" "${default_value}" > "${TTY_DEVICE}"
    else
      printf "%s: " "${prompt_label}" > "${TTY_DEVICE}"
    fi

    if ! IFS= read -r user_input < "${TTY_DEVICE}"; then
      print_notice "读取输入失败，继续使用当前值。"
      return
    fi

    if [[ -z "${user_input}" ]]; then
      candidate_value="${default_value}"
    else
      candidate_value="${user_input}"
    fi

    if [[ -z "${candidate_value}" ]]; then
      print_notice "${prompt_label}不能为空，请重新输入。"
      continue
    fi

    if [[ -n "${validator}" ]] && ! validate_value "${validator}" "${candidate_value}"; then
      print_validation_error "${prompt_label}"
      continue
    fi

    printf -v "${var_name}" '%s' "${candidate_value}"
    return
  done
}

# 统一的输入读取函数，提高代码复用性
read_input_safely() {
  local prompt="$1"
  local silent="${2:-0}"
  local user_input=""

  if [[ "${silent}" == "1" ]]; then
    if ! IFS= read -r -s user_input < "${TTY_DEVICE}"; then
      printf "\n读取输入失败，继续使用当前值。\n" > "${TTY_DEVICE}"
      return 1
    fi
    printf "\n" > "${TTY_DEVICE}"
  else
    if ! IFS= read -r user_input < "${TTY_DEVICE}"; then
      print_notice "读取输入失败，继续使用当前值。"
      return 1
    fi
  fi

  echo "${user_input}"
  return 0
}

prompt_secret_input() {
  local var_name="$1"
  local prompt_label="$2"
  local validator="${3:-}"
  local default_value="${!var_name:-}"
  local user_input=""
  local candidate_value=""

  if [[ "${INTERACTIVE_PROMPTS}" != "1" ]]; then
    return
  fi

  while true; do
    if [[ -n "${default_value}" ]]; then
      printf "%s（直接回车保留当前值）: " "${prompt_label}" > "${TTY_DEVICE}"
    else
      printf "%s（留空自动生成）: " "${prompt_label}" > "${TTY_DEVICE}"
    fi

    if ! IFS= read -r -s user_input < "${TTY_DEVICE}"; then
      printf "\n读取输入失败，继续使用当前值。\n" > "${TTY_DEVICE}"
      return
    fi

    printf "\n" > "${TTY_DEVICE}"

    if [[ -z "${user_input}" ]]; then
      candidate_value="${default_value}"
    else
      candidate_value="${user_input}"
    fi

    if [[ -n "${candidate_value}" && -n "${validator}" ]] && ! validate_value "${validator}" "${candidate_value}"; then
      print_validation_error "${prompt_label}"
      continue
    fi

    printf -v "${var_name}" '%s' "${candidate_value}"
    return
  done
}

prompt_yes_no() {
  local var_name="$1"
  local prompt_label="$2"
  local default_value="${!var_name:-0}"
  local default_hint="y/N"
  local default_answer="n"
  local user_input=""

  if [[ "${INTERACTIVE_PROMPTS}" != "1" ]]; then
    return
  fi

  if [[ "${default_value}" == "1" ]]; then
    default_hint="Y/n"
    default_answer="y"
  fi

  while true; do
    printf "%s [%s]: " "${prompt_label}" "${default_hint}" > "${TTY_DEVICE}"
    if ! IFS= read -r user_input < "${TTY_DEVICE}"; then
      print_notice "读取输入失败，继续使用当前值。"
      return
    fi

    user_input="${user_input:-${default_answer}}"
    case "${user_input,,}" in
      y|yes|1)
        printf -v "${var_name}" '1'
        return
        ;;
      n|no|0)
        printf -v "${var_name}" '0'
        return
        ;;
      *)
        print_notice "请输入 y 或 n。"
        ;;
    esac
  done
}

validate_configuration() {
  local allow_empty_passwords="0"

  if [[ ! -f "${DEPLOY_ENV_FILE}" || "${FORCE_WRITE_ENV}" == "1" ]]; then
    allow_empty_passwords="1"
  fi

  ensure_valid_value "应用端口 APP_PORT" "${APP_PORT}" validate_app_port
  ensure_valid_value "上传目录 HOST_UPLOAD_DIR" "${HOST_UPLOAD_DIR}" validate_host_upload_dir
  ensure_valid_value "MySQL 数据库名 MYSQL_DATABASE" "${MYSQL_DATABASE}" validate_mysql_database
  ensure_valid_value "MySQL 用户名 MYSQL_USER" "${MYSQL_USER}" validate_mysql_user
  ensure_valid_value "MySQL 用户密码 MYSQL_PASSWORD" "${MYSQL_PASSWORD}" validate_mysql_password "${allow_empty_passwords}"
  ensure_valid_value "MySQL Root 密码 MYSQL_ROOT_PASSWORD" "${MYSQL_ROOT_PASSWORD}" validate_mysql_password "${allow_empty_passwords}"
  ensure_valid_value "商品图片子目录 PRODUCT_IMAGE_PATH" "${PRODUCT_IMAGE_PATH}" validate_product_image_path
  ensure_valid_value "最大上传大小 MAX_FILE_SIZE" "${MAX_FILE_SIZE}" validate_max_file_size
  ensure_valid_value "允许扩展名 ALLOWED_EXTENSIONS" "${ALLOWED_EXTENSIONS}" validate_allowed_extensions
  ensure_valid_value "SQL 初始化模式 YTBX_SQL_INIT_MODE" "${YTBX_SQL_INIT_MODE}" validate_sql_init_mode
  ensure_valid_value "是否启用 Nginx ENABLE_NGINX" "${ENABLE_NGINX}" validate_flag
  ensure_valid_value "是否覆盖 .env FORCE_WRITE_ENV" "${FORCE_WRITE_ENV}" validate_flag

  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    ensure_valid_value "Nginx 域名 SERVER_NAME" "${SERVER_NAME}" validate_server_name
  fi
}

prompt_for_configuration() {
  if [[ -f "${DEPLOY_ENV_FILE}" && "${FORCE_WRITE_ENV}" != "1" ]]; then
    load_existing_env_defaults
    if [[ "${INTERACTIVE_PROMPTS}" != "1" ]]; then
      return
    fi
    echo > "${TTY_DEVICE}"
    echo "检测到现有配置，继续使用现有值。" > "${TTY_DEVICE}"
    echo "如需重新配置，请设置 FORCE_WRITE_ENV=1" > "${TTY_DEVICE}"
    return
  fi

  if [[ "${INTERACTIVE_PROMPTS}" != "1" ]]; then
    return
  fi

  echo > "${TTY_DEVICE}"
  echo "开始配置部署变量，直接回车可保留默认值。" > "${TTY_DEVICE}"

  if [[ -f "${DEPLOY_ENV_FILE}" && "${FORCE_WRITE_ENV}" != "1" ]]; then
    prompt_yes_no FORCE_WRITE_ENV "检测到 deploy/.env，是否重写该文件"
  fi

  if [[ ! -f "${DEPLOY_ENV_FILE}" || "${FORCE_WRITE_ENV}" == "1" ]]; then
    prompt_text_input APP_PORT "应用端口（1-65535）" validate_app_port
    prompt_text_input HOST_UPLOAD_DIR "上传目录（绝对路径）" validate_host_upload_dir
    prompt_text_input MYSQL_DATABASE "MySQL 数据库名（字母/数字/下划线）" validate_mysql_database
    prompt_text_input MYSQL_USER "MySQL 用户名（字母/数字/下划线）" validate_mysql_user
    prompt_secret_input MYSQL_PASSWORD "MySQL 用户密码" validate_mysql_password
    prompt_secret_input MYSQL_ROOT_PASSWORD "MySQL Root 密码" validate_mysql_password
    prompt_text_input PRODUCT_IMAGE_PATH "商品图片子目录（相对路径）" validate_product_image_path
    prompt_text_input MAX_FILE_SIZE "最大上传大小（字节）" validate_max_file_size
    prompt_text_input ALLOWED_EXTENSIONS "允许扩展名（逗号分隔）" validate_allowed_extensions
    prompt_text_input YTBX_SQL_INIT_MODE "SQL 初始化模式（never/embedded/always）" validate_sql_init_mode
  else
    echo "保留现有 deploy/.env 配置。" > "${TTY_DEVICE}"
  fi

  prompt_yes_no ENABLE_NGINX "是否配置 Nginx 反向代理"
  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    prompt_text_input SERVER_NAME "Nginx server_name（域名、IP 或 _）" validate_server_name
  fi
}

# ============================================================================
# 密码和网络配置
# ============================================================================

# 生成随机密码
generate_password() {
  local length="${1:-16}"
  openssl rand -hex "${length}"
}

# 获取本机 IP 地址
get_local_ip() {
  if command -v hostname >/dev/null 2>&1; then
    local ip
    ip=$(hostname -I 2>/dev/null | awk '{print $1}')
    echo "${ip:-127.0.0.1}"
  else
    echo "127.0.0.1"
  fi
}

hydrate_defaults() {
  if [[ -z "${MYSQL_PASSWORD}" ]]; then
    MYSQL_PASSWORD="$(generate_password 12)"
    log "INFO" "已自动生成 MySQL 用户密码"
  fi
  if [[ -z "${MYSQL_ROOT_PASSWORD}" ]]; then
    MYSQL_ROOT_PASSWORD="$(generate_password 16)"
    log "INFO" "已自动生成 MySQL Root 密码"
  fi
  LOCAL_IP="$(get_local_ip)"
}

# ============================================================================
# Docker 安装和配置
# ============================================================================

# 移除旧的 Docker 包
remove_old_docker_packages() {
  log "INFO" "移除旧的 Docker 包..."
  apt-get remove -y docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc 2>&1 || true
}

# 配置 Docker APT 源
configure_docker_repository() {
  log "INFO" "配置 Docker APT 源..."

  # 创建密钥目录
  install -m 0755 -d /etc/apt/keyrings

  # 优先使用国内镜像源
  DOCKER_MIRROR="${DOCKER_MIRROR:-https://mirrors.aliyun.com}"

  # 检查镜像源是否可用
  if curl -fsSL "${DOCKER_MIRROR}/docker-ce/linux/ubuntu/gpg" -o "${DOCKER_GPG_KEY}" 2>/dev/null; then
    log "INFO" "使用国内镜像源: ${DOCKER_MIRROR}"
  else
    log "WARN" "国内镜像源不可用，使用官方源"
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o "${DOCKER_GPG_KEY}" 2>/dev/null
  fi

  chmod a+r "${DOCKER_GPG_KEY}"

  # 写入 APT 源配置
  cat > "${DOCKER_SOURCES}" <<EOF_REPO
Types: deb
URIs: ${DOCKER_MIRROR}/docker-ce/linux/ubuntu
Suites: ${UBUNTU_CODENAME:-${VERSION_CODENAME}}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: ${DOCKER_GPG_KEY}
EOF_REPO

  log "INFO" "Docker APT 源配置完成"
}

# 启动 Docker 服务
start_docker_service() {
  log "INFO" "启动 Docker 服务..."
  if [[ -d /run/systemd/system ]]; then
    systemctl enable --now docker
  else
    service docker start 2>/dev/null || dockerd &>/var/log/docker.log &
    sleep 2
  fi

  # 验证 Docker 是否启动成功
  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    log "INFO" "Docker 服务启动成功"
    return 0
  else
    log "ERROR" "Docker 服务启动失败"
    return 1
  fi
}

# ============================================================================
# 用户和目录配置
# ============================================================================

configure_user_group() {
  if [[ -n "${PRIMARY_USER}" ]] && id -u "${PRIMARY_USER}" >/dev/null 2>&1; then
    if usermod -aG docker "${PRIMARY_USER}" 2>/dev/null; then
      log "INFO" "已将用户 ${PRIMARY_USER} 加入 docker 组"
    else
      log "WARN" "将用户 ${PRIMARY_USER} 加入 docker 组失败"
    fi
  fi
}

prepare_directories() {
  log "INFO" "准备必要的目录..."
  mkdir -p /opt/ytbx
  mkdir -p "${HOST_UPLOAD_DIR}/templates"

  # 使用更安全的权限设置（775 而不是 777）
  chmod -R 775 "${HOST_UPLOAD_DIR}"

  # 确保目录所有者正确
  if [[ -n "${PRIMARY_USER}" ]]; then
    chown -R "${PRIMARY_USER}:${PRIMARY_USER}" "${HOST_UPLOAD_DIR}" 2>/dev/null || true
  fi

  log "INFO" "目录准备完成"
}

prepare_directories() {
  mkdir -p /opt/ytbx
  mkdir -p "${HOST_UPLOAD_DIR}/templates"
  chmod -R 777 "${HOST_UPLOAD_DIR}"
}

write_env_file() {
  if [[ -f "${DEPLOY_ENV_FILE}" && "${FORCE_WRITE_ENV}" != "1" ]]; then
    chmod 600 "${DEPLOY_ENV_FILE}"
    return
  fi

  cat > "${DEPLOY_ENV_FILE}" <<EOF_ENV
APP_PORT=${APP_PORT}
HOST_UPLOAD_DIR=${HOST_UPLOAD_DIR}
MYSQL_DATABASE=${MYSQL_DATABASE}
MYSQL_USER=${MYSQL_USER}
MYSQL_PASSWORD=${MYSQL_PASSWORD}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
PRODUCT_IMAGE_PATH=${PRODUCT_IMAGE_PATH}
MAX_FILE_SIZE=${MAX_FILE_SIZE}
ALLOWED_EXTENSIONS=${ALLOWED_EXTENSIONS}
YTBX_SQL_INIT_MODE=${YTBX_SQL_INIT_MODE}
EOF_ENV
  chmod 600 "${DEPLOY_ENV_FILE}"
}

load_env_file() {
  set -a
  . "${DEPLOY_ENV_FILE}"
  set +a
}

# ============================================================================
# Nginx 配置
# ============================================================================

configure_nginx() {
  if [[ "${ENABLE_NGINX}" != "1" ]]; then
    log "INFO" "跳过 Nginx 配置"
    return
  fi

  log "INFO" "配置 Nginx 反向代理..."

  # 计算最大上传大小（MB）
  local max_size_mb=$((MAX_FILE_SIZE / 1024 / 1024))
  [[ ${max_size_mb} -lt 1 ]] && max_size_mb=1

  cat > /etc/nginx/conf.d/ytbx.conf <<EOF_NGINX
server {
    listen 80;
    server_name ${SERVER_NAME};
    client_max_body_size ${max_size_mb}m;

    location / {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_read_timeout 86400;
    }
}
EOF_NGINX

  # 移除默认配置
  rm -f /etc/nginx/sites-enabled/default /etc/nginx/conf.d/default.conf || true

  # 测试并重载 Nginx
  if nginx -t 2>&1 | tee -a "${LOG_FILE}"; then
    systemctl enable --now nginx 2>&1 | tee -a "${LOG_FILE}"
    systemctl reload nginx 2>&1 | tee -a "${LOG_FILE}"
    log "INFO" "Nginx 配置完成"
  else
    log "ERROR" "Nginx 配置测试失败"
    return 1
  fi
}

# ============================================================================
# 服务启动和健康检查
# ============================================================================

start_services() {
  log "INFO" "启动 Docker 服务..."
  cd "${PROJECT_ROOT}"

  if docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build > >(tee -a "${LOG_FILE}") 2>&1; then
    log "INFO" "服务启动成功"
  else
    log "ERROR" "服务启动失败"
    return 1
  fi
}

# 简单的健康检查
check_service_health() {
  local max_attempts=30
  local attempt=0
  local health_url="http://127.0.0.1:${APP_PORT}"

  log "INFO" "检查服务健康状态..."

  if ! command -v curl >/dev/null 2>&1; then
    log "WARN" "curl 命令不可用，跳过健康检查"
    return 0
  fi

  while [[ ${attempt} -lt ${max_attempts} ]]; do
    if curl -sf "${health_url}" >/dev/null 2>&1; then
      log "INFO" "服务健康检查通过"
      return 0
    fi

    attempt=$((attempt + 1))
    log "INFO" "等待服务启动... (${attempt}/${max_attempts})"
    sleep 2
  done

  log "WARN" "服务健康检查超时，请手动检查服务状态"
  return 1
}

# ============================================================================
# 服务管理命令
# ============================================================================

# 检查服务状态
cmd_status() {
  echo "========================================="
  echo "YTBX 服务状态"
  echo "========================================="
  echo ""

  # Docker 容器状态
  echo "【Docker 容器】"
  if command -v docker >/dev/null 2>&1; then
    cd "${PROJECT_ROOT}"
    if docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps 2>/dev/null; then
      :
    else
      echo "  提示：无法获取容器状态，请检查 Docker 是否运行"
    fi
  else
    echo "  Docker 未安装"
  fi
  echo ""

  # Nginx 状态
  echo "【Nginx 状态】"
  if command -v nginx >/dev/null 2>&1; then
    if systemctl is-active --quiet nginx 2>/dev/null; then
      echo "  Nginx: 运行中"
    else
      echo "  Nginx: 未运行"
    fi
  else
    echo "  Nginx: 未安装"
  fi
  echo ""

  # Docker 服务状态
  echo "【Docker 服务状态】"
  if command -v docker >/dev/null 2>&1; then
    if systemctl is-active --quiet docker 2>/dev/null; then
      echo "  Docker: 运行中"
    else
      echo "  Docker: 未运行"
    fi
  else
    echo "  Docker: 未安装"
  fi
  echo ""

  # 端口监听状态
  echo "【端口监听状态】"
  if command -v ss >/dev/null 2>&1; then
    echo "  应用端口 (${APP_PORT}):"
    if ss -tuln 2>/dev/null | grep -q ":${APP_PORT} "; then
      echo "    状态: 监听中"
    else
      echo "    状态: 未监听"
    fi
  elif command -v netstat >/dev/null 2>&1; then
    echo "  应用端口 (${APP_PORT}):"
    if netstat -tuln 2>/dev/null | grep -q ":${APP_PORT} "; then
      echo "    状态: 监听中"
    else
      echo "    状态: 未监听"
    fi
  fi
  echo ""

  echo "========================================="
}

# 停止服务
cmd_stop() {
  echo "========================================="
  echo "停止 YTBX 服务"
  echo "========================================="
  echo ""

  # 停止 Docker 容器
  echo "【停止 Docker 容器】"
  cd "${PROJECT_ROOT}"
  if docker compose --env-file deploy/.env -f deploy/docker-compose.yml down >/dev/null 2>&1; then
    echo "  Docker 容器已停止"
  else
    echo "  无法停止 Docker 容器，可能未运行"
  fi
  echo ""

  # 停止 Nginx
  echo "【停止 Nginx】"
  if command -v nginx >/dev/null 2>&1; then
    if systemctl is-active --quiet nginx 2>/dev/null; then
      systemctl stop nginx 2>/dev/null && echo "  Nginx 已停止" || echo "  Nginx 停止失败"
    else
      echo "  Nginx 未运行"
    fi
  fi
  echo ""

  # 停止自动部署服务
  echo "【停止自动部署服务】"
  if [[ -f /opt/actions-runner/svc.sh ]]; then
    cd /opt/actions-runner && ./svc.sh stop 2>/dev/null && echo "  Actions Runner 已停止" || echo "  Actions Runner 停止失败/未运行"
  fi

  if systemctl is-active --quiet ytbx-webhook 2>/dev/null; then
    systemctl stop ytbx-webhook 2>/dev/null && echo "  Webhook 服务已停止" || echo "  Webhook 服务停止失败"
  fi
  echo ""

  echo "========================================="
  echo "服务已停止"
  echo "========================================="
}

# 卸载服务
cmd_uninstall() {
  echo "========================================="
  echo "卸载 YTBX 服务"
  echo "========================================="
  echo ""
  echo "警告：此操作将执行以下操作："
  echo "  1. 停止并删除所有 Docker 容器"
  echo "  2. 删除 Docker 镜像（可选）"
  echo "  3. 移除 Nginx 配置"
  echo "  4. 移除应用数据目录"
  echo "  5. 移除自动部署服务"
  echo ""

  local confirm=""
  read -p "确认卸载? (输入 'yes' 确认): " confirm
  if [[ "${confirm}" != "yes" ]]; then
    echo "取消卸载操作"
    return 0
  fi

  # 停止所有服务
  cmd_stop
  echo ""

  # 删除 Docker 镜像
  echo "【删除 Docker 镜像】"
  local remove_images=""
  read -p "是否删除 Docker 镜像? (yes/no, 建议选 yes): " remove_images
  if [[ "${remove_images}" == "yes" ]]; then
    cd "${PROJECT_ROOT}"
    docker compose --env-file deploy/.env -f deploy/docker-compose.yml down --rmi local 2>/dev/null || true
    echo "  Docker 镜像已删除"
  fi
  echo ""

  # 删除应用数据
  echo "【删除应用数据】"
  local remove_data=""
  read -p "是否删除应用数据 (${HOST_UPLOAD_DIR})? (yes/no, 建议选 no): " remove_data
  if [[ "${remove_data}" == "yes" ]]; then
    rm -rf "${HOST_UPLOAD_DIR}" && echo "  应用数据已删除" || echo "  应用数据删除失败"
  fi
  echo ""

  # 移除 Nginx 配置
  echo "【移除 Nginx 配置】"
  rm -f /etc/nginx/conf.d/ytbx.conf
  systemctl reload nginx 2>/dev/null || true
  echo "  Nginx 配置已移除"
  echo ""

  # 移除自动部署服务
  echo "【移除自动部署服务】"
  if [[ -d /opt/actions-runner ]]; then
    if [[ -f /opt/actions-runner/svc.sh ]]; then
      cd /opt/actions-runner && ./svc.sh uninstall 2>/dev/null || true
    fi
    rm -rf /opt/actions-runner
    echo "  Actions Runner 已移除"
  fi

  if systemctl is-active --quiet ytbx-webhook 2>/dev/null || systemctl is-enabled --quiet ytbx-webhook 2>/dev/null; then
    systemctl stop ytbx-webhook 2>/dev/null || true
    systemctl disable ytbx-webhook 2>/dev/null || true
    rm -f /etc/systemd/system/ytbx-webhook.service
    systemctl daemon-reload
    echo "  Webhook 服务已移除"
  fi
  echo ""

  # 移除部署文件
  echo "【移除部署文件】"
  rm -f "${DEPLOY_ENV_FILE}"
  rm -f /opt/ytbx/docker-compose.yml
  echo "  部署配置文件已移除"
  echo ""

  echo "========================================="
  echo "卸载完成"
  echo ""
  echo "注意：Docker 和 Nginx 如需保留可手动卸载："
  echo "  Docker: apt-get remove docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin"
  echo "  Nginx: apt-get remove nginx"
  echo "========================================="
}

# ============================================================================
# 部署完成总结
# ============================================================================

print_summary() {
  echo
  echo "========================================="
  echo "部署完成"
  echo "========================================="
  echo "项目目录: ${PROJECT_ROOT}"
  echo "环境文件: ${DEPLOY_ENV_FILE}"
  echo "日志文件: ${LOG_FILE}"
  echo ""
  echo "配置信息："
  echo "  应用端口: ${APP_PORT}"
  echo "  上传目录: ${HOST_UPLOAD_DIR}"
  echo "  MySQL 数据库: ${MYSQL_DATABASE}"
  echo "  MySQL 用户: ${MYSQL_USER}"
  echo "  MySQL 密码: ${MYSQL_PASSWORD}"
  echo "  MySQL Root 密码: ${MYSQL_ROOT_PASSWORD}"
  echo ""
  echo "访问地址:"
  echo "  http://${LOCAL_IP}:${APP_PORT}"
  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    echo "  Nginx 域名: ${SERVER_NAME}"
  fi
  echo ""
  echo "管理命令："
  echo "  查看状态: docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps"
  echo "  查看日志: docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f ytbx-app"
  echo "  停止服务: docker compose --env-file deploy/.env -f deploy/docker-compose.yml down"
  echo ""
  if [[ -d /opt/actions-runner ]]; then
    echo "自动部署（Actions Runner）："
    echo "  Runner 状态: cd /opt/actions-runner && ./svc.sh status"
    echo "  Runner 管理: cd /opt/actions-runner && ./svc.sh [start|stop]"
  fi
  if systemctl is-active --quiet ytbx-webhook 2>/dev/null; then
    echo "自动部署（Webhook）："
    echo "  服务状态: systemctl status ytbx-webhook"
    echo "  服务日志: journalctl -u ytbx-webhook -f"
  fi
  if [[ -d /opt/actions-runner ]] || systemctl is-active --quiet ytbx-webhook 2>/dev/null; then
    echo ""
  fi
  if [[ -n "${PRIMARY_USER}" ]]; then
    echo "注意: 已将用户 ${PRIMARY_USER} 加入 docker 组，请重新登录后生效"
  fi
  echo "========================================="
}

# ============================================================================
# 主流程
# ============================================================================

main() {
  # 加载已有配置
  load_env_file 2>/dev/null || true

  # 显示主菜单
  while true; do
    echo ""
    echo "========================================="
    echo "  YTBX 服务器管理"
    echo "========================================="
    echo ""
    echo "  请选择操作："
    echo ""
    echo "  [1] 部署服务"
    echo "  [2] 查看服务状态"
    echo "  [3] 停止服务"
    echo "  [4] 卸载服务"
    echo "  [5] 退出"
    echo ""
    echo "========================================="

    local choice=""
    read -p "请输入选项 [1-5]: " choice

    case "${choice}" in
      1)
        # 执行部署流程
        break
        ;;
      2)
        cmd_status
        echo ""
        read -p "按回车键继续..."
        continue
        ;;
      3)
        cmd_stop
        echo ""
        read -p "按回车键继续..."
        continue
        ;;
      4)
        cmd_uninstall
        echo ""
        read -p "按回车键继续..."
        continue
        ;;
      5)
        echo "已退出"
        exit 0
        ;;
      *)
        echo "无效选项，请输入 1-5"
        echo ""
        read -p "按回车键继续..."
        continue
        ;;
    esac
    break
  done

  log "INFO" "开始部署流程..."
  log "INFO" "工作目录: ${PROJECT_ROOT}"

  # 初始化
  enable_interactive_prompts
  prompt_for_configuration
  validate_configuration
  hydrate_defaults

  # 安装依赖（先尝试配置国内源）
  configure_chinese_apt_mirrors
  update_package_index
  remove_old_docker_packages
  configure_docker_repository
  install_all_packages
  start_docker_service

  # 配置环境
  configure_user_group
  prepare_directories
  write_env_file
  load_env_file
  configure_nginx

  # 启动服务
  start_services
  check_service_health

  # 完成总结
  log "INFO" "部署流程完成"
  print_summary
}

# 执行主流程
main
