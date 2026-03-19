#!/usr/bin/env bash
set -euo pipefail

project_dir="${1:-$(pwd)}"
versions_dir="${project_dir}/versions"
artifact_base_name="${2:-DragonGestion}"

mkdir -p "${versions_dir}"

tmp_file="$(mktemp)"
trap 'rm -f "${tmp_file}"' EXIT

find "${versions_dir}" -maxdepth 2 -type f -name '*.jar' -print0 \
  | xargs -0 sha256sum \
  | while read -r hash path; do
      version="$(basename "$(dirname "${path}")" | sed "s#^${artifact_base_name}-##")"
      printf '%s\t%s\t%s\n' "${hash}" "${version}" "${path}"
    done \
  | sort -k1,1 -k2,2V > "${tmp_file}"

current_hash=""
current_path=""
while IFS=$'\t' read -r hash version path; do
  if [[ "${hash}" == "${current_hash}" ]]; then
    rm -f "${current_path}"
    rmdir "$(dirname "${current_path}")" 2>/dev/null || true
  fi
  current_hash="${hash}"
  current_path="${path}"
done < "${tmp_file}"

find "${versions_dir}" -maxdepth 1 -mindepth 1 -type d -name "${artifact_base_name}-*" \
  | sed "s#${versions_dir}/${artifact_base_name}-##" \
  | sort -V > "${versions_dir}/RELEASES.txt"

latest_version="$(tail -n 1 "${versions_dir}/RELEASES.txt" 2>/dev/null || true)"
if [[ -n "${latest_version}" ]]; then
  ln -sfn "${artifact_base_name}-${latest_version}" "${versions_dir}/latest"
fi
