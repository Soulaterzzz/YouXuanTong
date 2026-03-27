#!/usr/bin/env bash
set -Eeuo pipefail

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

if [[ ! -f "${PROJECT_ROOT}/deploy/docker-compose.yml" ]]; then
  echo "未找到 deploy/docker-compose.yml，请在项目根目录内运行此脚本。" >&2
  exit 1
fi

install_base_packages() {
  export DEBIAN_FRONTEND=noninteractive
  apt-get update
  apt-get install -y ca-certificates curl git gnupg nginx openssl
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

hydrate_defaults() {
  if [[ -z "${MYSQL_PASSWORD}" ]]; then
    MYSQL_PASSWORD="$(openssl rand -hex 12)"
  fi
  if [[ -z "${MYSQL_ROOT_PASSWORD}" ]]; then
    MYSQL_ROOT_PASSWORD="$(openssl rand -hex 16)"
  fi
  if hostname -I >/dev/null 2>&1; then
    LOCAL_IP="$(hostname -I | awk '{print $1}')"
    LOCAL_IP="${LOCAL_IP:-127.0.0.1}"
  fi
}

install_docker() {
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    systemctl enable --now docker
    return
  fi

  apt-get remove -y docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc || true
  install -m 0755 -d /etc/apt/keyrings

  DOCKER_MIRROR="${DOCKER_MIRROR:-https://mirrors.aliyun.com}"
  if curl -fsSL "${DOCKER_MIRROR}/docker-ce/linux/ubuntu/gpg" -o /etc/apt/keyrings/docker.asc 2>/dev/null; then
    chmod a+r /etc/apt/keyrings/docker.asc
    cat > /etc/apt/sources.list.d/docker.sources <<EOF_REPO
Types: deb
URIs: ${DOCKER_MIRROR}/docker-ce/linux/ubuntu
Suites: ${UBUNTU_CODENAME:-${VERSION_CODENAME}}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF_REPO
  else
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
    cat > /etc/apt/sources.list.d/docker.sources <<EOF_REPO
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: ${UBUNTU_CODENAME:-${VERSION_CODENAME}}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF_REPO
  fi

  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  systemctl enable --now docker
}

configure_user_group() {
  if [[ -n "${PRIMARY_USER}" ]] && id -u "${PRIMARY_USER}" >/dev/null 2>&1; then
    usermod -aG docker "${PRIMARY_USER}" || true
  fi
}

prepare_directories() {
  mkdir -p /opt/ytbx
  mkdir -p "${HOST_UPLOAD_DIR}/templates"
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

configure_nginx() {
  if [[ "${ENABLE_NGINX}" != "1" ]]; then
    return
  fi

  cat > /etc/nginx/conf.d/ytbx.conf <<EOF_NGINX
server {
    listen 80;
    server_name ${SERVER_NAME};
    client_max_body_size 20m;

    location / {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_http_version 1.1;
    }
}
EOF_NGINX

  rm -f /etc/nginx/sites-enabled/default /etc/nginx/conf.d/default.conf || true
  nginx -t
  systemctl enable --now nginx
  systemctl reload nginx
}

start_services() {
  cd "${PROJECT_ROOT}"
  docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
}

print_summary() {
  echo
  echo "部署完成。"
  echo "项目目录: ${PROJECT_ROOT}"
  echo "环境文件: ${DEPLOY_ENV_FILE}"
  echo "应用端口: ${APP_PORT}"
  echo "上传目录: ${HOST_UPLOAD_DIR}"
  echo "MySQL 数据库: ${MYSQL_DATABASE}"
  echo "MySQL 用户: ${MYSQL_USER}"
  echo "MySQL 密码: ${MYSQL_PASSWORD}"
  echo "MySQL Root 密码: ${MYSQL_ROOT_PASSWORD}"
  echo "访问地址: http://${LOCAL_IP}:${APP_PORT}"
  if [[ "${ENABLE_NGINX}" == "1" ]]; then
    echo "Nginx 域名: ${SERVER_NAME}"
  fi
  if [[ -n "${PRIMARY_USER}" ]]; then
    echo "已尝试将用户 ${PRIMARY_USER} 加入 docker 组，重新登录后生效。"
  fi
  echo "查看状态: docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps"
  echo "查看日志: docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f ytbx-app"
}

enable_interactive_prompts
prompt_for_configuration
validate_configuration
hydrate_defaults
install_base_packages
install_docker
configure_user_group
prepare_directories
write_env_file
load_env_file
configure_nginx
start_services
print_summary
