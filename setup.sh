#!/usr/bin/env bash
#
# Copyright 2025-2026 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Author: Ivan Rodriguez
# Idempotent developer setup: installs the pre-commit CLI
# if needed, (re)installs the git hooks declared in .pre-commit-config.yaml,
# updates the project skills from skills-lock.json, and installs the
# OpenSpec CLI if missing.
set -euo pipefail
cd "$(dirname "$0")"

if command -v pre-commit >/dev/null 2>&1; then
    echo "pre-commit already installed: $(pre-commit --version)"
else
    echo "Installing pre-commit..."
    if command -v pipx >/dev/null 2>&1; then
        pipx install pre-commit
    elif command -v pip3 >/dev/null 2>&1; then
        pip3 install --user pre-commit
    else
        echo "error: need pipx or pip3 to install pre-commit" >&2
        exit 1
    fi
fi

# Safe to re-run: overwrites .git/hooks/pre-commit with the current config.
# If a (global) core.hooksPath shadows .git/hooks, pre-commit install fails and
# says so — unset it in this repo and retry, and only in that case.
if ! output=$(pre-commit install 2>&1); then
    printf '%s\n' "$output"
    if printf '%s' "$output" | grep -q 'core\.hooksPath'; then
        echo "core.hooksPath is set; unsetting it for this repo and retrying..."
        git config --unset-all core.hooksPath
        pre-commit install
    else
        exit 1
    fi
fi

echo "Done. Active hooks: $(grep -oE 'id: [a-z-]+' .pre-commit-config.yaml | awk '{print $2}' | paste -sd, -)"
echo ""
# Update project skills from skills-lock.json (non-interactive: -p project scope,
# -y skips the scope prompt, npx --yes skips the package-download prompt).
# Also installs the OpenSpec CLI if missing, pinned to the version the project's
# openspec/ workflows were built against.
if command -v npx >/dev/null 2>&1; then
    npx --yes skills update --project --yes
    if ! command -v openspec >/dev/null 2>&1; then
        echo "Installing OpenSpec CLI (1.16.0)..."
        npm install -g @fission-ai/openspec@1.16.0
    fi
else
    echo "error: need node/npx to update project skills and the OpenSpec CLI" >&2
    exit 1
fi
