#!/usr/bin/env bash

# 宿主机部署指纹工具
# 当前部署脚本已经不依赖 Docker 应用镜像，但保留这组通用函数，
# 方便在需要时快速计算前后端与部署配置是否发生变化。

collect_deploy_fingerprint_candidates() {
  local project_root="${PROJECT_ROOT:?PROJECT_ROOT 未设置}"
  local candidate
  local dir

  for candidate in \
    "${project_root}/pom.xml" \
    "${project_root}/frontend/package.json" \
    "${project_root}/frontend/package-lock.json" \
    "${project_root}/frontend/index.html" \
    "${project_root}/frontend/vite.config.js" \
    "${project_root}/deploy/server-bootstrap-ubuntu.sh" \
    "${project_root}/deploy/docker-compose.yml" \
    "${project_root}/deploy/nginx/ytbx.conf.example" \
    "${project_root}/deploy/.env.example"; do
    [[ -f "${candidate}" ]] || continue
    printf '%s\n' "${candidate#${project_root}/}"
  done

  for dir in \
    "${project_root}/src" \
    "${project_root}/frontend/src" \
    "${project_root}/frontend/public"; do
    [[ -d "${dir}" ]] || continue
    find "${dir}" -type f \
      ! -path '*/node_modules/*' \
      ! -path '*/dist/*' \
      ! -path '*/target/*' \
      -print | while IFS= read -r file; do
        printf '%s\n' "${file#${project_root}/}"
      done
  done
}

compute_deploy_fingerprint() {
  local project_root="${PROJECT_ROOT:?PROJECT_ROOT 未设置}"
  local manifest
  local relative_path
  local absolute_path
  local file_hash
  local fingerprint

  manifest="$(mktemp)"

  while IFS= read -r relative_path; do
    [[ -n "${relative_path}" ]] || continue
    absolute_path="${project_root}/${relative_path}"
    [[ -f "${absolute_path}" ]] || continue
    file_hash="$(sha256sum "${absolute_path}" | awk '{print $1}')"
    printf '%s %s\n' "${relative_path}" "${file_hash}" >> "${manifest}"
  done < <(collect_deploy_fingerprint_candidates | LC_ALL=C sort)

  fingerprint="$(sha256sum "${manifest}" | awk '{print $1}')"
  rm -f "${manifest}"
  printf '%s\n' "${fingerprint}"
}
