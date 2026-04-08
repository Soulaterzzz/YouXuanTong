#!/usr/bin/env bash

# 仅提供函数，不设置 shell 选项，避免影响调用者。

DEPLOY_IMAGE_LABEL="${DEPLOY_IMAGE_LABEL:-com.zs.ytbx.deploy-fingerprint}"
DEPLOY_IMAGE_NAME="${DEPLOY_IMAGE_NAME:-ytbx-app:latest}"

collect_deploy_fingerprint_candidates() {
  local project_root="${PROJECT_ROOT:?PROJECT_ROOT 未设置}"
  local candidate
  local dir

  for candidate in \
    "${project_root}/Dockerfile" \
    "${project_root}/.dockerignore" \
    "${project_root}/deploy/maven-settings.xml" \
    "${project_root}/pom.xml" \
    "${project_root}/frontend/package.json" \
    "${project_root}/frontend/package-lock.json" \
    "${project_root}/frontend/index.html" \
    "${project_root}/frontend/vite.config.js"; do
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

get_deploy_image_fingerprint() {
  local fingerprint

  fingerprint="$(docker image inspect -f "{{ index .Config.Labels \"${DEPLOY_IMAGE_LABEL}\" }}" "${DEPLOY_IMAGE_NAME}" 2>/dev/null || true)"

  case "${fingerprint}" in
    "<no value>"|"map[]")
      fingerprint=""
      ;;
  esac

  printf '%s\n' "${fingerprint}"
}

deploy_image_needs_rebuild() {
  local desired_fingerprint="${1:-}"
  local current_fingerprint

  if [[ -z "${desired_fingerprint}" ]]; then
    return 0
  fi

  current_fingerprint="$(get_deploy_image_fingerprint)"

  [[ -z "${current_fingerprint}" || "${current_fingerprint}" != "${desired_fingerprint}" ]]
}
