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

if [[ -z "${artifact_base_name}" ]]; then
  artifact_base_name="DragonGestion"
fi

IFS='.' read -r major minor patch <<< "${current_version}"
major="${major:-1}"
minor="${minor:-0}"
patch="${patch:-0}"

next_patch=$((patch + 1))
next_version="${major}.${minor}.${next_patch}"
artifact_name="${artifact_base_name}-${next_version}"
output_dir="${project_dir}/versions/${artifact_name}"
output_file="${output_dir}/${artifact_name}.jar"

mkdir -p "${target_dir}" "${output_dir}"

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

echo "Prepared build version ${next_version}"
