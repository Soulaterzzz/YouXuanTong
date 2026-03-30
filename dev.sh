#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DEV_ENV_FILE="${PROJECT_ROOT}/.env.local"

declare -A CONFIG_VARS=(
  ["MYSQL_HOST"]="127.0.0.1"
  ["MYSQL_PORT"]="3306"
  ["MYSQL_USER"]="root"
  ["MYSQL_PASSWORD"]=""
  ["MYSQL_DATABASE"]="ytbx"
  ["APP_PORT"]="8080"
  ["FILE_STORAGE_PATH"]="./uploads"
  ["PRODUCT_IMAGE_PATH"]="products"
  ["TEMPLATE_FILE_PATH"]="./uploads/templates"
  ["MAX_FILE_SIZE"]="10485760"
  ["ALLOWED_EXTENSIONS"]="jpg,jpeg,png,gif,webp"
)

check_mysql_client() {
  if ! command -v mysql >/dev/null 2>&1; then
    echo "错误: 未找到 mysql 客户端"
    echo "请安装: apt install mysql-client 或 brew install mysql-client"
    return 1
  fi
  return 0
}

check_mysql_connection() {
  local host="${1:-${MYSQL_HOST}}"
  local port="${2:-${MYSQL_PORT}}"
  local user="${3:-${MYSQL_USER}}"
  local password="${4:-${MYSQL_PASSWORD}}"

  if mysql -h "${host}" -P "${port}" -u "${user}" -p"${password}" -e "SELECT 1" >/dev/null 2>&1; then
    return 0
  fi
  return 1
}

create_database() {
  echo "创建数据库 ${MYSQL_DATABASE}..."
  if mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" -p"${MYSQL_PASSWORD}" \
    -e "CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>/dev/null; then
    echo "数据库创建完成"
    return 0
  else
    echo "数据库创建失败，请检查连接信息"
    return 1
  fi
}

init_database() {
  echo "初始化数据库..."

  if [[ ! -f "${PROJECT_ROOT}/init_db.sql" ]]; then
    echo "错误: 未找到 init_db.sql 文件"
    return 1
  fi

  if ! create_database; then
    return 1
  fi

  echo "导入数据库结构..."
  if mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" \
    < "${PROJECT_ROOT}/init_db.sql" 2>/dev/null; then
    echo "数据库导入成功"
  else
    echo "数据库导入失败"
    return 1
  fi

  if [[ -d "${PROJECT_ROOT}/src/main/resources/sql" ]]; then
    echo "执行增量迁移脚本..."
    for sql_file in "${PROJECT_ROOT}"/src/main/resources/sql/*.sql; do
      if [[ -f "${sql_file}" ]]; then
        echo "  执行: $(basename "${sql_file}")"
        mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" \
          < "${sql_file}" 2>/dev/null || true
      fi
    done
  fi

  echo "数据库初始化完成"
}

get_app_pid() {
  lsof -ti :"${APP_PORT}" 2>/dev/null | head -1 || true
}

is_app_running() {
  local pid
  pid=$(get_app_pid)
  [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null
}

start_app() {
  echo "启动应用..."

  if is_app_running; then
    echo "应用已在运行中 (端口 ${APP_PORT})"
    return 0
  fi

  cd "${PROJECT_ROOT}"

  local mysql_url="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}?useUnicode=true\&characterEncoding=utf8\&serverTimezone=Asia/Shanghai"

  export MYSQL_URL="${mysql_url}"
  export MYSQL_USERNAME="${MYSQL_USER}"
  export MYSQL_PASSWORD="${MYSQL_PASSWORD}"
  export MYSQL_HOST="${MYSQL_HOST}"
  export MYSQL_PORT="${MYSQL_PORT}"
  export MYSQL_DATABASE="${MYSQL_DATABASE}"
  export FILE_STORAGE_PATH="${FILE_STORAGE_PATH}"
  export TEMPLATE_FILE_PATH="${TEMPLATE_FILE_PATH}"
  export PRODUCT_IMAGE_PATH="${PRODUCT_IMAGE_PATH}"
  export MAX_FILE_SIZE="${MAX_FILE_SIZE}"
  export ALLOWED_EXTENSIONS="${ALLOWED_EXTENSIONS}"
  export SPRING_PROFILES_ACTIVE="local"

  mkdir -p "${PROJECT_ROOT}/uploads/templates" 2>/dev/null || true

  if [[ -f "${PROJECT_ROOT}/mvnw" ]]; then
    echo "使用 Maven Wrapper 启动..."
    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=${APP_PORT}"
  elif command -v mvn >/dev/null 2>&1; then
    echo "使用 mvn 启动..."
    mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=${APP_PORT}"
  elif [[ -f "${PROJECT_ROOT}/target"/*.jar ]]; then
    local jar_file
    jar_file=$(find "${PROJECT_ROOT}/target" -maxdepth 1 -name "*.jar" ! -name "*original*" 2>/dev/null | head -1)
    if [[ -n "${jar_file}" ]]; then
      echo "启动 JAR: ${jar_file}"
      java -jar "${jar_file}" --server.port="${APP_PORT}"
    else
      echo "错误: 未找到 JAR 文件，请先执行 mvn package"
      return 1
    fi
  else
    echo "错误: 未找到 Java/Maven/JAR"
    return 1
  fi
}

stop_app() {
  echo "停止应用..."

  local pid
  pid=$(get_app_pid)

  if [[ -z "${pid}" ]]; then
    echo "应用未在运行"
    return 0
  fi

  echo "终止进程 PID: ${pid}"
  kill "${pid}" 2>/dev/null || true
  sleep 2

  if kill -0 "${pid}" 2>/dev/null; then
    echo "强制终止..."
    kill -9 "${pid}" 2>/dev/null || true
  fi

  echo "应用已停止"
}

show_status() {
  echo
  echo "=============================================="
  echo "  项目本地开发状态"
  echo "=============================================="
  echo "项目路径: ${PROJECT_ROOT}"
  echo "环境文件: ${DEV_ENV_FILE}"
  echo
  echo "MySQL 配置:"
  echo "  主机: ${MYSQL_HOST}:${MYSQL_PORT}"
  echo "  用户: ${MYSQL_USER}"
  echo "  数据库: ${MYSQL_DATABASE}"
  echo "  密码: ${MYSQL_PASSWORD:+****}"
  echo
  echo "应用配置:"
  echo "  端口: ${APP_PORT}"
  echo "  上传目录: ${FILE_STORAGE_PATH}"
  echo

  if check_mysql_connection; then
    echo "MySQL 连接: ✅ 正常"
  else
    echo "MySQL 连接: ❌ 失败"
  fi

  if is_app_running; then
    echo "应用状态: ✅ 运行中 (端口 ${APP_PORT})"
    local pid
    pid=$(get_app_pid)
    [[ -n "${pid}" ]] && echo "进程 PID: ${pid}"
  else
    echo "应用状态: ❌ 未运行"
  fi

  echo "----------------------------------------------"
  echo "访问地址: http://127.0.0.1:${APP_PORT}"
  echo "----------------------------------------------"
  echo
}

load_env_file() {
  if [[ ! -f "${DEV_ENV_FILE}" ]]; then
    return
  fi

  while IFS='=' read -r key value; do
    [[ -z "${key}" || "${key}" =~ ^# ]] && continue
    value=$(echo "${value}" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    [[ -n "${value}" ]] && declare "${key}=${value}"
  done < "${DEV_ENV_FILE}"
}

save_env_file() {
  cat > "${DEV_ENV_FILE}" <<EOF
MYSQL_HOST=${MYSQL_HOST}
MYSQL_PORT=${MYSQL_PORT}
MYSQL_USER=${MYSQL_USER}
MYSQL_PASSWORD=${MYSQL_PASSWORD}
MYSQL_DATABASE=${MYSQL_DATABASE}
APP_PORT=${APP_PORT}
FILE_STORAGE_PATH=${FILE_STORAGE_PATH}
PRODUCT_IMAGE_PATH=${PRODUCT_IMAGE_PATH}
TEMPLATE_FILE_PATH=${TEMPLATE_FILE_PATH}
MAX_FILE_SIZE=${MAX_FILE_SIZE}
ALLOWED_EXTENSIONS=${ALLOWED_EXTENSIONS}
EOF
  chmod 600 "${DEV_ENV_FILE}"
  echo "配置已保存到 ${DEV_ENV_FILE}"
}

prompt_config() {
  echo
  echo "=============================================="
  echo "  配置 MySQL 连接"
  echo "=============================================="
  echo

  local keys=("MYSQL_HOST" "MYSQL_PORT" "MYSQL_USER" "MYSQL_PASSWORD" "MYSQL_DATABASE" "APP_PORT")

  for key in "${keys[@]}"; do
    local default="${!key}"
    local input
    local label="${key}"

    if [[ "${key}" == "MYSQL_PASSWORD" ]]; then
      printf "%-20s [%s]: " "${label}" "${default}"
      read -r -s input
      echo
    else
      printf "%-20s [%s]: " "${label}" "${default}"
      read -r input
    fi

    if [[ -n "${input}" ]]; then
      declare "${key}=${input}"
    elif [[ -z "${!key}" ]]; then
      declare "${key}=${CONFIG_VARS[${key}]}"
    fi
  done

  echo
  echo "测试 MySQL 连接..."
  if check_mysql_connection; then
    echo "✅ 连接成功"
  else
    echo "❌ 连接失败，请检查配置"
  fi
  echo
}

show_menu() {
  echo
  echo "=============================================="
  echo "  优选通 - 本地开发工具"
  echo "=============================================="
  echo "  1. 配置 MySQL 连接"
  echo "  2. 初始化数据库"
  echo "  3. 启动应用"
  echo "  4. 停止应用"
  echo "  5. 查看状态"
  echo "  6. 保存配置"
  echo "  0. 退出"
  echo "=============================================="
  echo
}

main() {
  load_env_file

  for key in "${!CONFIG_VARS[@]}"; do
    if [[ -z "${!key:-}" ]]; then
      declare "${key}=${CONFIG_VARS[${key}]}"
    fi
  done

  if [[ $# -gt 0 ]]; then
    case "$1" in
      init)
        check_mysql_client || exit 1
        if check_mysql_connection; then
          init_database
        else
          echo "错误: 无法连接 MySQL，请先配置正确的连接信息"
          exit 1
        fi
        exit 0
        ;;
      start)
        if check_mysql_connection; then
          start_app
        else
          echo "错误: 无法连接 MySQL，请检查配置"
          exit 1
        fi
        exit 0
        ;;
      stop)
        stop_app
        exit 0
        ;;
      status)
        show_status
        exit 0
        ;;
      restart)
        stop_app
        start_app
        exit 0
        ;;
      *)
        echo "用法: $0 {init|start|stop|status|restart}"
        exit 1
        ;;
    esac
  fi

  while true; do
    show_menu
    printf "请选择操作: "
    read -r choice
    echo

    case "${choice}" in
      1)
        prompt_config
        echo "配置已更新（未保存）"
        echo
        read -p "按回车键继续..."
        ;;
      2)
        check_mysql_client || { read -p "按回车键继续..."; continue; }
        if check_mysql_connection; then
          init_database
        else
          echo "错误: 无法连接 MySQL，请先配置正确的连接信息"
        fi
        echo
        read -p "按回车键继续..."
        ;;
      3)
        if check_mysql_connection; then
          start_app
        else
          echo "错误: 无法连接 MySQL，请检查配置"
        fi
        echo
        read -p "按回车键继续..."
        ;;
      4)
        stop_app
        echo
        read -p "按回车键继续..."
        ;;
      5)
        show_status
        echo
        read -p "按回车键继续..."
        ;;
      6)
        save_env_file
        echo
        read -p "按回车键继续..."
        ;;
      0)
        echo "再见!"
        exit 0
        ;;
      *)
        echo "无效选项，请输入 0-6"
        echo
        read -p "按回车键继续..."
        ;;
    esac
  done
}

main "$@"
