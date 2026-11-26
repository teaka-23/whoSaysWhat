#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
src="$repo_root/.cline/skills"
dst="${CLINE_SKILLS_DIR:-$HOME/.cline/skills}"
readme="$repo_root/docs/agents/global-skills-README.md"

if [[ ! -d "$src" ]]; then
  echo "Не найден каталог навыков: $src" >&2
  exit 1
fi

if [[ ! -f "$readme" ]]; then
  echo "Не найден README глобальной копии: $readme" >&2
  exit 1
fi

if ! command -v rsync >/dev/null 2>&1; then
  echo "Нужен rsync." >&2
  exit 1
fi

mkdir -p "$dst"
# README глобальной копии свой: не перезаписываем его проектным README.
rsync -a --exclude README.md "$src/" "$dst/"
cp "$readme" "$dst/README.md"

echo "Синхронизировано: $src -> $dst"
echo "Проверьте навыки в меню Cline после перезагрузки окна."
