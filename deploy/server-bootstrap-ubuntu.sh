#!/usr/bin/env bash
set -Eeuo pipefail

# Ubuntu 宿主机部署脚本
# - 前端：在宿主机用 Node.js 构建后由 nginx 托管
# - 后端：在宿主机用 Maven 构建后由 systemd 运行
# - 数据库：默认连接外部数据库；只有在需要时才启用 Docker MySQL

if [[ "${EUID}" -ne 0 ]]; then
  echo "请使用 sudo 运行：sudo bash deploy/server-bootstrap-ubuntu.sh [deploy|status|stop|uninstall]" >&2
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
DEPLOY_BASE_DIR="${DEPLOY_BASE_DIR:-/opt/ytbx}"
BACKEND_DIR="${BACKEND_DIR:-${DEPLOY_BASE_DIR}/backend}"
WEB_DIR="${WEB_DIR:-${DEPLOY_BASE_DIR}/web}"
SYSTEMD_SERVICE_FILE="/etc/systemd/system/ytbx-backend.service"
NGINX_CONF_FILE="/etc/nginx/conf.d/ytbx.conf"
MYSQL_COMPOSE_FILE="${PROJECT_ROOT}/deploy/docker-compose.yml"
SERVICE_USER="${SERVICE_USER:-ytbx}"
SERVICE_GROUP="${SERVICE_GROUP:-${SERVICE_USER}}"
APP_PORT="${APP_PORT:-8080}"
HOST_UPLOAD_DIR="${HOST_UPLOAD_DIR:-/data/ytbx/uploads}"
USE_EXTERNAL_DB="${USE_EXTERNAL_DB:-1}"
MYSQL_HOST="${MYSQL_HOST:-}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:-ytbx}"
MYSQL_USER="${MYSQL_USER:-ytbx}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
MYSQL_URL="${MYSQL_URL:-}"
AUTH_TOKEN_SECRET="${AUTH_TOKEN_SECRET:-}"
CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:5173,http://127.0.0.1:5173}"
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

log() {
  local level="$1"
  shift
  local message="$*"
  local timestamp

  timestamp="$(date '+%Y-%m-%d %H:%M:%S')"
  echo "[${timestamp}] [${level}] ${message}" | tee -a "${LOG_FILE}"
}

trap 'echo "[ERROR] 部署失败，请查看日志: ${LOG_FILE}" >&2' ERR

if [[ ! -f "${PROJECT_ROOT}/pom.xml" ]] || [[ ! -d "${PROJECT_ROOT}/frontend" ]]; then
  echo "未找到项目源码，请在仓库根目录内运行脚本。" >&2
  exit 1
fi

if [[ ! -f "${MYSQL_COMPOSE_FILE}" ]]; then
  echo "未找到 deploy/docker-compose.yml，请在项目根目录内运行脚本。" >&2
  exit 1
fi

if [[ ! -f "${PROJECT_ROOT}/frontend/package.json" ]] || [[ ! -f "${PROJECT_ROOT}/frontend/package-lock.json" ]]; then
  echo "前端依赖文件缺失，无法执行构建。" >&2
  exit 1
fi

if [[ "${USE_EXTERNAL_DB}" != "1" ]] && [[ ! -f "${PROJECT_ROOT}/init_db.sql" ]]; then
  echo "本地 MySQL 模式需要根目录下的 init_db.sql。" >&2
  exit 1
fi

check_package_installed() {
  local pkg="$1"
  dpkg -l "${pkg}" >/dev/null 2>&1
}

get_missing_packages() {
  local missing=()
  local pkg
  for pkg in "$@"; do
    if ! check_package_installed "${pkg}"; then
      missing+=("${pkg}")
    fi
  done
  echo "${missing[*]}"
}

configure_chinese_apt_mirrors() {
  log "INFO" "配置国内 APT 镜像源..."

  if [[ -f /etc/apt/sources.list ]] && [[ ! -f /etc/apt/sources.list.backup ]]; then
    cp /etc/apt/sources.list /etc/apt/sources.list.backup
  fi

  local codename="${UBUNTU_CODENAME:-${VERSION_CODENAME}}"
  if [[ -z "${codename}" ]]; then
    codename="$(lsb_release -cs 2>/dev/null || echo "jammy")"
  fi

  local mirrors=(
    "https://mirrors.aliyun.com/ubuntu"
    "https://mirrors.tuna.tsinghua.edu.cn/ubuntu"
    "https://mirrors.ustc.edu.cn/ubuntu"
  )
  local selected_mirror=""
  local mirror
  for mirror in "${mirrors[@]}"; do
    if curl -fsSL "${mirror}/dists/${codename}/Release" -o /dev/null 2>/dev/null; then
      selected_mirror="${mirror}"
      break
    fi
  done

  if [[ -n "${selected_mirror}" ]]; then
    cat > /etc/apt/sources.list <<EOF
deb ${selected_mirror}/ ${codename} main restricted universe multiverse
deb ${selected_mirror}/ ${codename}-updates main restricted universe multiverse
deb ${selected_mirror}/ ${codename}-backports main restricted universe multiverse
deb ${selected_mirror}/ ${codename}-security main restricted universe multiverse
EOF
    log "INFO" "国内 APT 镜像源配置完成"
    return 0
  fi

  log "WARN" "无法连接到国内镜像源，继续使用原始源"
  return 1
}

update_package_index() {
  log "INFO" "更新软件包索引..."
  export DEBIAN_FRONTEND=noninteractive

  if apt-get update > >(tee -a "${LOG_FILE}") 2>&1; then
    return 0
  fi

  log "WARN" "apt-get update 失败，尝试切换国内镜像源"
  configure_chinese_apt_mirrors
  apt-get update > >(tee -a "${LOG_FILE}") 2>&1
}

configure_nodesource_repository() {
  log "INFO" "配置 NodeSource 仓库..."
  install -m 0755 -d /etc/apt/keyrings
  local key_file="/etc/apt/keyrings/nodesource.gpg"
  local source_file="/etc/apt/sources.list.d/nodesource.list"

  curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key \
    | gpg --dearmor -o "${key_file}"
  chmod a+r "${key_file}"

  cat > "${source_file}" <<EOF
deb [signed-by=${key_file}] https://deb.nodesource.com/node_22.x nodistro main
EOF
}

remove_old_docker_packages() {
  log "INFO" "移除旧的 Docker 包..."
  apt-get remove -y docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc 2>&1 || true
}

configure_docker_repository() {
  log "INFO" "配置 Docker APT 源..."

  install -m 0755 -d /etc/apt/keyrings
  local docker_key="/etc/apt/keyrings/docker.asc"
  local docker_sources="/etc/apt/sources.list.d/docker.sources"
  local docker_mirror="${DOCKER_MIRROR:-https://mirrors.aliyun.com}"

  if curl -fsSL "${docker_mirror}/docker-ce/linux/ubuntu/gpg" -o "${docker_key}" 2>/dev/null; then
    log "INFO" "使用 Docker 镜像源: ${docker_mirror}"
  else
    log "WARN" "Docker 镜像源不可用，切换官方源"
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o "${docker_key}"
  fi
  chmod a+r "${docker_key}"

  cat > "${docker_sources}" <<EOF
Types: deb
URIs: ${docker_mirror}/docker-ce/linux/ubuntu
Suites: ${UBUNTU_CODENAME:-${VERSION_CODENAME}}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: ${docker_key}
EOF
}

start_docker_service() {
  log "INFO" "启动 Docker 服务..."
  if [[ -d /run/systemd/system ]]; then
    systemctl enable --now docker
  else
    service docker start 2>/dev/null || dockerd &>/var/log/docker.log &
    sleep 2
  fi

  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    log "INFO" "Docker 服务启动成功"
    return 0
  fi

  log "ERROR" "Docker 服务启动失败"
  return 1
}

enable_interactive_prompts() {
  if [[ -r "${TTY_DEVICE}" && -w "${TTY_DEVICE}" ]]; then
    INTERACTIVE_PROMPTS="1"
  fi
}

load_existing_env_defaults() {
  if [[ -f "${DEPLOY_ENV_FILE}" ]]; then
    # shellcheck disable=SC1090
    . "${DEPLOY_ENV_FILE}"
  fi
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

validate_mysql_host() {
  local value="$1"

  if [[ -z "${value}" ]]; then
    VALIDATION_ERROR="数据库主机不能为空"
    return 1
  fi

  if [[ ! "${value}" =~ ^[A-Za-z0-9._-]+$ ]]; then
    VALIDATION_ERROR="仅支持字母、数字、点、下划线和中划线"
    return 1
  fi

  if [[ "${value}" == *:* ]]; then
    VALIDATION_ERROR="数据库主机请不要包含端口，端口单独填写在 MYSQL_PORT 中"
    return 1
  fi

  return 0
}

validate_mysql_port() {
  local value="$1"

  if [[ ! "${value}" =~ ^[0-9]+$ ]]; then
    VALIDATION_ERROR="请输入 1-65535 之间的整数"
    return 1
  fi

  if (( 10#${value} < 1 || 10#${value} > 65535 )); then
    VALIDATION_ERROR="端口范围必须在 1-65535 之间"
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

    VALIDATION_ERROR="密码不能为空"
    return 1
  fi

  if (( ${#value} < 8 )); then
    VALIDATION_ERROR="密码长度至少 8 位"
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
    VALIDATION_ERROR="请输入大于 0 的整数，单位为字节"
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

  if [[ -z "${value}" ]]; then
    VALIDATION_ERROR="Nginx server_name 不能为空，可填写 _"
    return 1
  fi

  if [[ "${value}" =~ [[:space:]] ]]; then
    VALIDATION_ERROR="server_name 不能包含空白字符"
    return 1
  fi

  if [[ ! "${value}" =~ ^[A-Za-z0-9._-]+$ ]]; then
    VALIDATION_ERROR="仅支持字母、数字、点、下划线和中划线"
    return 1
  fi

  if [[ "${value}" == *$'\n'* || "${value}" == *';'* || "${value}" == *'\\'* || "${value}" == *'$'* ]]; then
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

prompt_secret_input() {
  local var_name="$1"
  local prompt_label="$2"
  local validator="${3:-}"
  local allow_generate="${4:-1}"
  local default_value="${!var_name:-}"
  local user_input=""
  local candidate_value=""

  if [[ "${INTERACTIVE_PROMPTS}" != "1" ]]; then
    return
  fi

  while true; do
    if [[ -n "${default_value}" ]]; then
      printf "%s（直接回车保留当前值）: " "${prompt_label}" > "${TTY_DEVICE}"
    elif [[ "${allow_generate}" == "1" ]]; then
      printf "%s（留空自动生成）: " "${prompt_label}" > "${TTY_DEVICE}"
    else
      printf "%s: " "${prompt_label}" > "${TTY_DEVICE}"
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

    if [[ -z "${candidate_value}" && "${allow_generate}" != "1" ]]; then
      print_notice "${prompt_label}不能为空，请重新输入。"
      continue
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

generate_password() {
  local length="${1:-16}"
  openssl rand -hex "${length}"
}

get_local_ip() {
  if command -v hostname >/dev/null 2>&1; then
    local ip
    ip="$(hostname -I 2>/dev/null | awk '{print $1}')"
    echo "${ip:-127.0.0.1}"
  else
    echo "127.0.0.1"
  fi
}

build_mysql_url() {
  local host="${MYSQL_HOST:-127.0.0.1}"
  local port="${MYSQL_PORT:-3306}"
  local query="useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
  printf 'jdbc:mysql://%s:%s/%s?%s\n' "${host}" "${port}" "${MYSQL_DATABASE}" "${query}"
}

get_access_url() {
  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    if [[ -n "${SERVER_NAME}" && "${SERVER_NAME}" != "_" ]]; then
      printf 'http://%s\n' "${SERVER_NAME}"
    else
      printf 'http://%s\n' "${LOCAL_IP}"
    fi
    return
  fi

  printf 'http://%s:%s\n' "${LOCAL_IP}" "${APP_PORT}"
}

hydrate_defaults() {
  if [[ "${USE_EXTERNAL_DB}" == "1" ]]; then
    MYSQL_ROOT_PASSWORD=""
  else
    MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"

    if [[ -z "${MYSQL_PASSWORD}" ]]; then
      MYSQL_PASSWORD="$(generate_password 12)"
      log "INFO" "已自动生成 MySQL 用户密码"
    fi

    if [[ -z "${MYSQL_ROOT_PASSWORD}" ]]; then
      MYSQL_ROOT_PASSWORD="$(generate_password 16)"
      log "INFO" "已自动生成 MySQL Root 密码"
    fi
  fi

  if [[ -z "${AUTH_TOKEN_SECRET}" ]]; then
    AUTH_TOKEN_SECRET="$(generate_password 32)"
    log "INFO" "已自动生成认证令牌密钥"
  fi

  if [[ -z "${MYSQL_URL}" ]]; then
    MYSQL_URL="$(build_mysql_url)"
  fi

  LOCAL_IP="$(get_local_ip)"
}

validate_configuration() {
  local allow_empty_local_passwords="0"

  ensure_valid_value "应用端口 APP_PORT" "${APP_PORT}" validate_app_port
  ensure_valid_value "上传目录 HOST_UPLOAD_DIR" "${HOST_UPLOAD_DIR}" validate_host_upload_dir
  ensure_valid_value "是否使用外部数据库 USE_EXTERNAL_DB" "${USE_EXTERNAL_DB}" validate_flag
  ensure_valid_value "MySQL 数据库名 MYSQL_DATABASE" "${MYSQL_DATABASE}" validate_mysql_database
  ensure_valid_value "MySQL 用户名 MYSQL_USER" "${MYSQL_USER}" validate_mysql_user

  if [[ "${USE_EXTERNAL_DB}" == "1" ]]; then
    ensure_valid_value "数据库主机 MYSQL_HOST" "${MYSQL_HOST}" validate_mysql_host
    ensure_valid_value "数据库端口 MYSQL_PORT" "${MYSQL_PORT}" validate_mysql_port
    ensure_valid_value "MySQL 用户密码 MYSQL_PASSWORD" "${MYSQL_PASSWORD}" validate_mysql_password 0
  else
    if [[ ! -f "${DEPLOY_ENV_FILE}" || "${FORCE_WRITE_ENV}" == "1" ]]; then
      allow_empty_local_passwords="1"
    fi

    ensure_valid_value "数据库端口 MYSQL_PORT" "${MYSQL_PORT}" validate_mysql_port
    ensure_valid_value "MySQL 用户密码 MYSQL_PASSWORD" "${MYSQL_PASSWORD}" validate_mysql_password "${allow_empty_local_passwords}"
    ensure_valid_value "MySQL Root 密码 MYSQL_ROOT_PASSWORD" "${MYSQL_ROOT_PASSWORD}" validate_mysql_password "${allow_empty_local_passwords}"
  fi

  ensure_valid_value "商品图片子目录 PRODUCT_IMAGE_PATH" "${PRODUCT_IMAGE_PATH}" validate_product_image_path
  ensure_valid_value "最大上传大小 MAX_FILE_SIZE" "${MAX_FILE_SIZE}" validate_max_file_size
  ensure_valid_value "允许扩展名 ALLOWED_EXTENSIONS" "${ALLOWED_EXTENSIONS}" validate_allowed_extensions
  ensure_valid_value "SQL 初始化模式 YTBX_SQL_INIT_MODE" "${YTBX_SQL_INIT_MODE}" validate_sql_init_mode
  ensure_valid_value "是否启用 Nginx ENABLE_NGINX" "${ENABLE_NGINX}" validate_flag
  ensure_valid_value "是否覆盖 .env FORCE_WRITE_ENV" "${FORCE_WRITE_ENV}" validate_flag

  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    ensure_valid_value "Nginx 域名 SERVER_NAME" "${SERVER_NAME}" validate_server_name
    if [[ "${APP_PORT}" == "80" ]]; then
      echo "应用端口 APP_PORT 无效：启用 nginx 时不能使用 80 端口，请改用其他端口（例如 8080）。" >&2
      return 1
    fi
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
    prompt_yes_no USE_EXTERNAL_DB "是否使用外部数据库"

    if [[ "${USE_EXTERNAL_DB}" == "1" ]]; then
      MYSQL_HOST=""
      prompt_text_input MYSQL_HOST "外部数据库主机（容器可访问的 IP 或域名）" validate_mysql_host
      prompt_text_input MYSQL_PORT "外部数据库端口" validate_mysql_port
      prompt_text_input MYSQL_DATABASE "MySQL 数据库名（字母/数字/下划线）" validate_mysql_database
      prompt_text_input MYSQL_USER "MySQL 用户名（字母/数字/下划线）" validate_mysql_user
      prompt_secret_input MYSQL_PASSWORD "MySQL 用户密码" validate_mysql_password 0
      MYSQL_ROOT_PASSWORD=""
      print_notice "外部数据库模式提示：如果目标库是空库，请将 SQL 初始化模式设置为 always。"
    else
      MYSQL_HOST="127.0.0.1"
      prompt_text_input MYSQL_PORT "本地 MySQL 容器端口（映射到宿主机）" validate_mysql_port
      prompt_text_input MYSQL_DATABASE "MySQL 数据库名（字母/数字/下划线）" validate_mysql_database
      prompt_text_input MYSQL_USER "MySQL 用户名（字母/数字/下划线）" validate_mysql_user
      prompt_secret_input MYSQL_PASSWORD "MySQL 用户密码" validate_mysql_password 1
      prompt_secret_input MYSQL_ROOT_PASSWORD "MySQL Root 密码" validate_mysql_password 1
    fi

    prompt_text_input APP_PORT "后端服务端口（nginx 反代到该端口）" validate_app_port
    prompt_text_input HOST_UPLOAD_DIR "上传目录（绝对路径）" validate_host_upload_dir
    prompt_text_input PRODUCT_IMAGE_PATH "商品图片子目录（相对路径）" validate_product_image_path
    prompt_text_input MAX_FILE_SIZE "最大上传大小（字节）" validate_max_file_size
    prompt_text_input ALLOWED_EXTENSIONS "允许扩展名（逗号分隔）" validate_allowed_extensions
    prompt_text_input YTBX_SQL_INIT_MODE "SQL 初始化模式（never/embedded/always）" validate_sql_init_mode
  else
    echo "保留现有 deploy/.env 配置。" > "${TTY_DEVICE}"
  fi

  prompt_yes_no ENABLE_NGINX "是否配置宿主机 Nginx 反向代理"
  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    prompt_text_input SERVER_NAME "Nginx server_name（域名、IP 或 _）" validate_server_name
  fi
}

quote_env_value() {
  local value="${1:-}"
  local escaped_value

  escaped_value="$(printf '%s' "${value}" | sed "s/'/'\"'\"'/g")"
  printf "'%s'" "${escaped_value}"
}

write_env_file() {
  cat > "${DEPLOY_ENV_FILE}" <<EOF
APP_PORT=$(quote_env_value "${APP_PORT}")
HOST_UPLOAD_DIR=$(quote_env_value "${HOST_UPLOAD_DIR}")
USE_EXTERNAL_DB=$(quote_env_value "${USE_EXTERNAL_DB}")
MYSQL_HOST=$(quote_env_value "${MYSQL_HOST}")
MYSQL_PORT=$(quote_env_value "${MYSQL_PORT}")
MYSQL_DATABASE=$(quote_env_value "${MYSQL_DATABASE}")
MYSQL_USER=$(quote_env_value "${MYSQL_USER}")
MYSQL_PASSWORD=$(quote_env_value "${MYSQL_PASSWORD}")
MYSQL_ROOT_PASSWORD=$(quote_env_value "${MYSQL_ROOT_PASSWORD}")
MYSQL_URL=$(quote_env_value "${MYSQL_URL}")
AUTH_TOKEN_SECRET=$(quote_env_value "${AUTH_TOKEN_SECRET}")
CORS_ALLOWED_ORIGINS=$(quote_env_value "${CORS_ALLOWED_ORIGINS}")
PRODUCT_IMAGE_PATH=$(quote_env_value "${PRODUCT_IMAGE_PATH}")
MAX_FILE_SIZE=$(quote_env_value "${MAX_FILE_SIZE}")
ALLOWED_EXTENSIONS=$(quote_env_value "${ALLOWED_EXTENSIONS}")
YTBX_SQL_INIT_MODE=$(quote_env_value "${YTBX_SQL_INIT_MODE}")
ENABLE_NGINX=$(quote_env_value "${ENABLE_NGINX}")
SERVER_NAME=$(quote_env_value "${SERVER_NAME}")
EOF
  chmod 600 "${DEPLOY_ENV_FILE}"
}

load_env_file() {
  if [[ -f "${DEPLOY_ENV_FILE}" ]]; then
    set -a
    # shellcheck disable=SC1090
    . "${DEPLOY_ENV_FILE}"
    set +a
  fi
}

get_node_major_version() {
  if ! command -v node >/dev/null 2>&1; then
    echo 0
    return
  fi

  node -p "process.versions.node.split('.')[0]" 2>/dev/null || echo 0
}

install_required_packages() {
  local bootstrap_packages=()
  local base_packages=(ca-certificates curl git gnupg openssl)
  local install_packages=()
  local need_node="0"
  local need_docker="0"
  local node_major
  local docker_packages=(docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin)
  local missing_base=()

  for pkg in ca-certificates curl gnupg; do
    if ! check_package_installed "${pkg}"; then
      bootstrap_packages+=("${pkg}")
    fi
  done

  if [[ ${#bootstrap_packages[@]} -gt 0 ]]; then
    update_package_index
    log "INFO" "先安装基础引导包: ${bootstrap_packages[*]}"
    apt-get install -y "${bootstrap_packages[@]}" > >(tee -a "${LOG_FILE}") 2>&1
  fi

  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    base_packages+=(nginx)
  fi

  base_packages+=(openjdk-17-jdk maven)
  missing_base=($(get_missing_packages "${base_packages[@]}"))
  install_packages+=("${missing_base[@]}")

  node_major="$(get_node_major_version)"
  if (( node_major < 20 )) || ! command -v npm >/dev/null 2>&1; then
    need_node="1"
    configure_nodesource_repository
    install_packages+=(nodejs)
  fi

  if [[ "${USE_EXTERNAL_DB}" != "1" ]]; then
    if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
      need_docker="1"
      remove_old_docker_packages
      configure_docker_repository
      install_packages+=("${docker_packages[@]}")
    else
      local missing_docker
      missing_docker=($(get_missing_packages "${docker_packages[@]}"))
      if [[ ${#missing_docker[@]} -gt 0 ]]; then
        need_docker="1"
        configure_docker_repository
        install_packages+=("${missing_docker[@]}")
      fi
    fi
  fi

  if [[ ${#install_packages[@]} -eq 0 ]]; then
    log "INFO" "运行依赖已全部安装，跳过 apt 安装"
    return 0
  fi

  update_package_index
  log "INFO" "即将安装以下软件包: ${install_packages[*]}"
  apt-get install -y "${install_packages[@]}" > >(tee -a "${LOG_FILE}") 2>&1

  if [[ "${need_node}" == "1" ]] && ! command -v npm >/dev/null 2>&1; then
    log "ERROR" "Node.js 已安装，但 npm 不可用，请检查 NodeSource 安装结果"
    return 1
  fi

  if [[ "${need_docker}" == "1" ]]; then
    start_docker_service
  fi
}

ensure_service_user() {
  if ! getent group "${SERVICE_GROUP}" >/dev/null 2>&1; then
    groupadd --system "${SERVICE_GROUP}" >/dev/null 2>&1 || true
  fi

  if id -u "${SERVICE_USER}" >/dev/null 2>&1; then
    return 0
  fi

  log "INFO" "创建系统用户 ${SERVICE_USER}"
  useradd --system --no-create-home --shell /usr/sbin/nologin --gid "${SERVICE_GROUP}" "${SERVICE_USER}"
}

prepare_directories() {
  log "INFO" "准备部署目录..."
  mkdir -p "${DEPLOY_BASE_DIR}" "${BACKEND_DIR}" "${WEB_DIR}" "${HOST_UPLOAD_DIR}/templates"
  chmod 755 "${DEPLOY_BASE_DIR}" "${BACKEND_DIR}" "${WEB_DIR}" 2>/dev/null || true
  chmod 775 "${HOST_UPLOAD_DIR}" "${HOST_UPLOAD_DIR}/templates" 2>/dev/null || true
  chown -R "${SERVICE_USER}:${SERVICE_GROUP}" "${DEPLOY_BASE_DIR}" "${HOST_UPLOAD_DIR}" 2>/dev/null || true
}

build_frontend_assets() {
  log "INFO" "构建前端..."
  (cd "${PROJECT_ROOT}/frontend" && npm ci --prefer-offline --no-audit && npm run build) > >(tee -a "${LOG_FILE}") 2>&1
}

sync_frontend_assets() {
  local dist_dir="${PROJECT_ROOT}/frontend/dist"
  if [[ ! -d "${dist_dir}" ]]; then
    echo "前端构建产物不存在：${dist_dir}" >&2
    return 1
  fi

  rm -rf "${WEB_DIR}"
  mkdir -p "${WEB_DIR}"
  cp -a "${dist_dir}/." "${WEB_DIR}/"
  chown -R "${SERVICE_USER}:${SERVICE_GROUP}" "${WEB_DIR}" 2>/dev/null || true
}

build_backend_package() {
  log "INFO" "构建后端..."
  (cd "${PROJECT_ROOT}" && mvn -s "${PROJECT_ROOT}/deploy/maven-settings.xml" -B -DskipTests package) > >(tee -a "${LOG_FILE}") 2>&1

  local jar_file
  jar_file="$(find "${PROJECT_ROOT}/target" -maxdepth 1 -name '*.jar' ! -name 'original-*' | sort | tail -n1)"
  if [[ -z "${jar_file}" || ! -f "${jar_file}" ]]; then
    echo "未找到后端构建产物" >&2
    return 1
  fi

  rm -f "${BACKEND_DIR}/ytbx.jar"
  cp "${jar_file}" "${BACKEND_DIR}/ytbx.jar"
  chown "${SERVICE_USER}:${SERVICE_GROUP}" "${BACKEND_DIR}/ytbx.jar" 2>/dev/null || true
}

write_backend_runtime_files() {
  cat > "${BACKEND_DIR}/ytbx.env" <<EOF
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=$(quote_env_value "${APP_PORT}")
MYSQL_URL=$(quote_env_value "${MYSQL_URL}")
MYSQL_USERNAME=$(quote_env_value "${MYSQL_USER}")
MYSQL_PASSWORD=$(quote_env_value "${MYSQL_PASSWORD}")
AUTH_TOKEN_SECRET=$(quote_env_value "${AUTH_TOKEN_SECRET}")
CORS_ALLOWED_ORIGINS=$(quote_env_value "${CORS_ALLOWED_ORIGINS}")
FILE_STORAGE_PATH=$(quote_env_value "${HOST_UPLOAD_DIR}")
TEMPLATE_FILE_PATH=$(quote_env_value "${HOST_UPLOAD_DIR}/templates")
PRODUCT_IMAGE_PATH=$(quote_env_value "${PRODUCT_IMAGE_PATH}")
MAX_FILE_SIZE=$(quote_env_value "${MAX_FILE_SIZE}")
ALLOWED_EXTENSIONS=$(quote_env_value "${ALLOWED_EXTENSIONS}")
YTBX_SQL_INIT_MODE=$(quote_env_value "${YTBX_SQL_INIT_MODE}")
JAVA_OPTS=$(quote_env_value "-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication")
TZ=$(quote_env_value "Asia/Shanghai")
EOF
  chmod 600 "${BACKEND_DIR}/ytbx.env"

  cat > "${BACKEND_DIR}/run.sh" <<'EOF'
#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/ytbx.env"
JAR_FILE="${SCRIPT_DIR}/ytbx.jar"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  . "${ENV_FILE}"
  set +a
fi

JAVA_BIN="${JAVA_BIN:-$(command -v java)}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication}"

exec "${JAVA_BIN}" ${JAVA_OPTS} -jar "${JAR_FILE}"
EOF
  chmod 755 "${BACKEND_DIR}/run.sh"
  chown "${SERVICE_USER}:${SERVICE_GROUP}" "${BACKEND_DIR}/ytbx.env" "${BACKEND_DIR}/run.sh" 2>/dev/null || true
}

write_systemd_service_file() {
  local docker_dependency=""
  if [[ "${USE_EXTERNAL_DB}" != "1" ]]; then
    docker_dependency=$'After=docker.service\nRequires=docker.service'
  fi

  cat > "${SYSTEMD_SERVICE_FILE}" <<EOF
[Unit]
Description=YTBX Backend Service
After=network-online.target
Wants=network-online.target
${docker_dependency}

[Service]
Type=simple
User=${SERVICE_USER}
Group=${SERVICE_GROUP}
WorkingDirectory=${BACKEND_DIR}
ExecStart=${BACKEND_DIR}/run.sh
Restart=always
RestartSec=5
SuccessExitStatus=143
UMask=0027
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF

  chmod 644 "${SYSTEMD_SERVICE_FILE}"
  systemctl daemon-reload
}

configure_nginx() {
  if [[ "${ENABLE_NGINX}" != "1" ]]; then
    log "INFO" "跳过 Nginx 配置"
    return 0
  fi

  log "INFO" "配置宿主机 Nginx..."

  local max_size_mb=$(( (MAX_FILE_SIZE + 1048575) / 1048576 ))
  [[ "${max_size_mb}" -lt 1 ]] && max_size_mb=1

  cat > "${NGINX_CONF_FILE}" <<EOF
server {
    listen 80;
    server_name ${SERVER_NAME};
    client_max_body_size ${max_size_mb}m;
    root ${WEB_DIR};
    index index.html;

    location ^~ /api/ {
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

    location ^~ /files/ {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location ^~ /agreements/ {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }
}
EOF

  rm -f /etc/nginx/sites-enabled/default /etc/nginx/conf.d/default.conf || true

  if nginx -t > >(tee -a "${LOG_FILE}") 2>&1; then
    systemctl enable --now nginx > >(tee -a "${LOG_FILE}") 2>&1
    systemctl reload nginx > >(tee -a "${LOG_FILE}") 2>&1
  else
    log "ERROR" "Nginx 配置测试失败"
    return 1
  fi
}

wait_for_mysql_container() {
  local max_attempts=30
  local attempt=0
  local status=""

  while [[ ${attempt} -lt ${max_attempts} ]]; do
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' ytbx-mysql 2>/dev/null || true)"
    case "${status}" in
      healthy)
        log "INFO" "MySQL 容器健康检查通过"
        return 0
        ;;
      unhealthy)
        log "WARN" "MySQL 容器健康状态异常，继续等待... (${attempt + 1}/${max_attempts})"
        ;;
      running|starting|created|"")
        log "INFO" "等待 MySQL 容器就绪... (${attempt + 1}/${max_attempts})"
        ;;
      *)
        log "INFO" "MySQL 当前状态: ${status}，继续等待... (${attempt + 1}/${max_attempts})"
        ;;
    esac

    attempt=$((attempt + 1))
    sleep 2
  done

  log "WARN" "MySQL 容器健康检查超时"
  return 1
}

start_mysql_container() {
  if [[ "${USE_EXTERNAL_DB}" == "1" ]]; then
    return 0
  fi

  if ! command -v docker >/dev/null 2>&1; then
    echo "本地 MySQL 模式需要 Docker，但当前未安装。" >&2
    return 1
  fi

  log "INFO" "启动本地 MySQL 容器..."
  (
    cd "${PROJECT_ROOT}" &&
      MYSQL_DATABASE="${MYSQL_DATABASE}" \
      MYSQL_USER="${MYSQL_USER}" \
      MYSQL_PASSWORD="${MYSQL_PASSWORD}" \
      MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD}" \
      MYSQL_PORT="${MYSQL_PORT}" \
      docker compose -f "${MYSQL_COMPOSE_FILE}" up -d mysql
  ) > >(tee -a "${LOG_FILE}") 2>&1

  wait_for_mysql_container
}

start_backend_service() {
  log "INFO" "启动后端 systemd 服务..."
  systemctl enable ytbx-backend > >(tee -a "${LOG_FILE}") 2>&1
  systemctl restart ytbx-backend > >(tee -a "${LOG_FILE}") 2>&1
}

check_service_health() {
  local max_attempts=30
  local attempt=0
  local backend_health_url="http://127.0.0.1:${APP_PORT}/api/notices"

  log "INFO" "检查后端健康状态..."
  while [[ ${attempt} -lt ${max_attempts} ]]; do
    if curl -fsS "${backend_health_url}" >/dev/null 2>&1; then
      log "INFO" "后端健康检查通过"
      break
    fi

    attempt=$((attempt + 1))
    log "INFO" "等待后端启动... (${attempt}/${max_attempts})"
    sleep 2
  done

  if [[ ${attempt} -ge ${max_attempts} ]]; then
    log "WARN" "后端健康检查超时"
    return 1
  fi

  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    local nginx_url="http://127.0.0.1/"
    if [[ "${SERVER_NAME}" != "_" && -n "${SERVER_NAME}" ]]; then
      nginx_url="http://127.0.0.1/"
      if ! curl -fsS -H "Host: ${SERVER_NAME}" "${nginx_url}" >/dev/null 2>&1; then
        log "WARN" "Nginx 健康检查失败"
        return 1
      fi
    else
      if ! curl -fsS "${nginx_url}" >/dev/null 2>&1; then
        log "WARN" "Nginx 健康检查失败"
        return 1
      fi
    fi
    log "INFO" "Nginx 健康检查通过"
  fi

  return 0
}

cmd_status() {
  echo "========================================="
  echo "YTBX 服务状态"
  echo "========================================="
  echo ""

  echo "【后端服务】"
  if systemctl is-active --quiet ytbx-backend 2>/dev/null; then
    echo "  ytbx-backend: 运行中"
  else
    echo "  ytbx-backend: 未运行"
  fi
  echo ""

  echo "【Nginx 状态】"
  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    if systemctl is-active --quiet nginx 2>/dev/null; then
      echo "  nginx: 运行中"
    else
      echo "  nginx: 未运行"
    fi
    echo "  配置: ${NGINX_CONF_FILE}"
  else
    echo "  nginx: 未启用"
  fi
  echo ""

  echo "【数据库模式】"
  if [[ "${USE_EXTERNAL_DB}" == "1" ]]; then
    echo "  模式: 外部数据库"
  else
    echo "  模式: 本地 MySQL 容器"
  fi
  echo "  地址: ${MYSQL_HOST:-127.0.0.1}:${MYSQL_PORT}/${MYSQL_DATABASE}"
  echo "  用户: ${MYSQL_USER}"
  echo ""

  echo "【端口监听】"
  if command -v ss >/dev/null 2>&1; then
    if ss -tuln 2>/dev/null | grep -q ":${APP_PORT} "; then
      echo "  应用端口 ${APP_PORT}: 监听中"
    else
      echo "  应用端口 ${APP_PORT}: 未监听"
    fi
  fi
  echo ""

  if [[ "${USE_EXTERNAL_DB}" != "1" ]]; then
    echo "【MySQL 容器】"
    if command -v docker >/dev/null 2>&1; then
      (cd "${PROJECT_ROOT}" && docker compose -f "${MYSQL_COMPOSE_FILE}" ps) 2>/dev/null || echo "  无法获取 MySQL 容器状态"
    else
      echo "  Docker 未安装"
    fi
    echo ""
  fi

  echo "访问地址: $(get_access_url)"
  echo "========================================="
}

cmd_stop() {
  echo "========================================="
  echo "停止 YTBX 服务"
  echo "========================================="
  echo ""

  echo "【停止后端服务】"
  if systemctl is-active --quiet ytbx-backend 2>/dev/null; then
    systemctl stop ytbx-backend >/dev/null 2>&1 && echo "  后端已停止" || echo "  后端停止失败"
  else
    echo "  后端未运行"
  fi
  echo ""

  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    echo "【停止 Nginx】"
    if systemctl is-active --quiet nginx 2>/dev/null; then
      systemctl stop nginx >/dev/null 2>&1 && echo "  Nginx 已停止" || echo "  Nginx 停止失败"
    else
      echo "  Nginx 未运行"
    fi
    echo ""
  fi

  if [[ "${USE_EXTERNAL_DB}" != "1" ]]; then
    echo "【停止 MySQL 容器】"
    if command -v docker >/dev/null 2>&1; then
      (cd "${PROJECT_ROOT}" && docker compose -f "${MYSQL_COMPOSE_FILE}" down >/dev/null 2>&1) \
        && echo "  MySQL 容器已停止" || echo "  MySQL 容器未运行或停止失败"
    else
      echo "  Docker 未安装"
    fi
    echo ""
  fi

  echo "========================================="
  echo "服务已停止"
  echo "========================================="
}

cmd_uninstall() {
  echo "========================================="
  echo "卸载 YTBX 服务"
  echo "========================================="
  echo ""
  echo "警告：此操作将执行以下操作："
  echo "  1. 停止后端服务"
  echo "  2. 移除 nginx 配置"
  echo "  3. 删除 /opt/ytbx 部署目录"
  echo "  4. 删除 systemd 服务"
  if [[ "${USE_EXTERNAL_DB}" != "1" ]]; then
    echo "  5. 停止并删除本地 MySQL 容器（可选）"
  fi
  echo ""

  local confirm=""
  read -r -p "确认卸载? (输入 'yes' 确认): " confirm
  if [[ "${confirm}" != "yes" ]]; then
    echo "取消卸载操作"
    return 0
  fi

  cmd_stop
  echo ""

  echo "【移除 systemd 服务】"
  systemctl disable --now ytbx-backend >/dev/null 2>&1 || true
  rm -f "${SYSTEMD_SERVICE_FILE}"
  systemctl daemon-reload
  echo "  systemd 服务已移除"
  echo ""

  echo "【移除 Nginx 配置】"
  rm -f "${NGINX_CONF_FILE}"
  systemctl reload nginx >/dev/null 2>&1 || true
  echo "  Nginx 配置已移除"
  echo ""

  if [[ "${USE_EXTERNAL_DB}" != "1" ]] && command -v docker >/dev/null 2>&1; then
    echo "【移除 MySQL 容器】"
    (cd "${PROJECT_ROOT}" && docker compose -f "${MYSQL_COMPOSE_FILE}" down -v >/dev/null 2>&1) || true
    echo "  MySQL 容器与数据卷已移除"
    echo ""
  fi

  echo "【移除部署目录】"
  rm -rf "${DEPLOY_BASE_DIR}"
  rm -f "${DEPLOY_ENV_FILE}"
  echo "  部署目录已移除"
  echo ""

  echo "========================================="
  echo "卸载完成"
  echo "========================================="
}

print_summary() {
  local access_url
  local database_mode
  local nginx_state
  access_url="$(get_access_url)"
  if [[ "${USE_EXTERNAL_DB}" == "1" ]]; then
    database_mode="外部数据库"
  else
    database_mode="本地 MySQL 容器"
  fi

  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    nginx_state="已启用"
  else
    nginx_state="未启用"
  fi

  echo
  echo "========================================="
  echo "部署完成"
  echo "========================================="
  echo "项目目录: ${PROJECT_ROOT}"
  echo "环境文件: ${DEPLOY_ENV_FILE}"
  echo "部署目录: ${DEPLOY_BASE_DIR}"
  echo "后端目录: ${BACKEND_DIR}"
  echo "前端目录: ${WEB_DIR}"
  echo "日志文件: ${LOG_FILE}"
  echo ""
  echo "配置信息："
  echo "  应用端口: ${APP_PORT}"
  echo "  上传目录: ${HOST_UPLOAD_DIR}"
  echo "  数据库模式: ${database_mode}"
  echo "  数据库地址: ${MYSQL_HOST:-127.0.0.1}:${MYSQL_PORT}/${MYSQL_DATABASE}"
  echo "  数据库用户: ${MYSQL_USER}"
  echo "  Nginx: ${nginx_state}"
  echo "  Nginx 绑定: ${SERVER_NAME}"
  echo ""
  echo "访问地址:"
  echo "  ${access_url}"
  echo ""
  echo "管理命令："
  echo "  查看状态: sudo bash deploy/server-bootstrap-ubuntu.sh status"
  echo "  停止服务: sudo bash deploy/server-bootstrap-ubuntu.sh stop"
  echo "  卸载服务: sudo bash deploy/server-bootstrap-ubuntu.sh uninstall"
  echo "  查看后端日志: journalctl -u ytbx-backend -f"
  if [[ "${USE_EXTERNAL_DB}" != "1" ]]; then
    echo "  查看 MySQL 容器: docker compose -f deploy/docker-compose.yml ps"
  fi
  echo "========================================="
}

run_deploy() {
  log "INFO" "开始部署流程..."
  log "INFO" "工作目录: ${PROJECT_ROOT}"

  enable_interactive_prompts
  prompt_for_configuration
  validate_configuration
  hydrate_defaults

  install_required_packages
  ensure_service_user
  prepare_directories
  write_env_file
  write_backend_runtime_files

  build_frontend_assets
  sync_frontend_assets
  build_backend_package
  write_systemd_service_file
  configure_nginx

  if [[ "${USE_EXTERNAL_DB}" != "1" ]]; then
    start_mysql_container
  fi

  start_backend_service
  check_service_health

  log "INFO" "部署流程完成"
  print_summary
}

interactive_menu() {
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
    if ! read -r -p "请输入选项 [1-5]: " choice; then
      echo "输入结束，退出。"
      return 0
    fi

    case "${choice}" in
      1)
        run_deploy
        return 0
        ;;
      2)
        load_env_file
        cmd_status
        echo ""
        read -r -p "按回车键继续..."
        ;;
      3)
        load_env_file
        cmd_stop
        echo ""
        read -r -p "按回车键继续..."
        ;;
      4)
        load_env_file
        cmd_uninstall
        echo ""
        read -r -p "按回车键继续..."
        ;;
      5)
        echo "已退出"
        return 0
        ;;
      *)
        echo "无效选项，请输入 1-5"
        echo ""
        read -r -p "按回车键继续..."
        ;;
    esac
  done
}

main() {
  load_env_file

  case "${1:-}" in
    deploy)
      run_deploy
      ;;
    status)
      cmd_status
      ;;
    stop)
      cmd_stop
      ;;
    uninstall)
      cmd_uninstall
      ;;
    "" )
      if [[ -t 0 && -r "${TTY_DEVICE}" && -w "${TTY_DEVICE}" ]]; then
        interactive_menu
      else
        run_deploy
      fi
      ;;
    *)
      echo "用法: sudo bash deploy/server-bootstrap-ubuntu.sh [deploy|status|stop|uninstall]" >&2
      exit 1
      ;;
  esac
}

main "$@"
