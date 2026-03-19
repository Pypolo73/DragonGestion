#!/usr/bin/env bash
set -euo pipefail

project_dir="${1:-$(pwd)}"
state_file="${project_dir}/.dragon-build.properties"
target_dir="${project_dir}/target"

if [[ ! -f "${state_file}" ]]; then
  cat > "${state_file}" <<'EOF'
artifactBaseName=DragonGestion
currentVersion=1.0.0
EOF
fi

artifact_base_name="$(grep '^artifactBaseName=' "${state_file}" | cut -d'=' -f2- | tr -d '\r')"
current_version="$(grep '^currentVersion=' "${state_file}" | cut -d'=' -f2- | tr -d '\r')"
next_version_override="$(grep '^nextVersionOverride=' "${state_file}" | cut -d'=' -f2- | tr -d '\r' || true)"

if [[ -n "${DRAGON_BUILD_VERSION:-}" ]]; then
  next_version_override="${DRAGON_BUILD_VERSION}"
fi

if [[ -z "${artifact_base_name}" ]]; then
  artifact_base_name="DragonGestion"
fi

version_pattern='^[0-9]+\.[0-9]+\.[0-9]+$'
if [[ -n "${next_version_override}" ]]; then
  if [[ ! "${next_version_override}" =~ ${version_pattern} ]]; then
    echo "Invalid explicit build version: ${next_version_override}" >&2
    exit 1
  fi
  next_version="${next_version_override}"
else
  IFS='.' read -r major minor patch <<< "${current_version}"
  major="${major:-1}"
  minor="${minor:-0}"
  patch="${patch:-0}"
  next_patch=$((patch + 1))
  next_version="${major}.${minor}.${next_patch}"
fi
artifact_name="${artifact_base_name}-${next_version}"
output_dir="${project_dir}/versions/${artifact_name}"
output_file="${output_dir}/${artifact_name}.jar"

mkdir -p "${target_dir}"

cat > "${state_file}" <<EOF
artifactBaseName=${artifact_base_name}
currentVersion=${next_version}
EOF

cat > "${target_dir}/build-version.properties" <<EOF
dragon.plugin.version=${next_version}
dragon.artifact.base=${artifact_base_name}
dragon.artifact.name=${artifact_name}
dragon.output.dir=${output_dir}
dragon.output.file=${output_file}
EOF

releases_dir="${project_dir}/versions"
mkdir -p "${releases_dir}"
{
  find "${releases_dir}" -maxdepth 1 -mindepth 1 -type d -name "${artifact_base_name}-*" \
    | sed "s#${releases_dir}/${artifact_base_name}-##"
  echo "${next_version}"
} | sort -V | awk 'NF && !seen[$0]++' > "${releases_dir}/RELEASES.txt"
ln -sfn "${artifact_name}" "${releases_dir}/latest"

echo "Prepared build version ${next_version}"
