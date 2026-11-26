# Нейтральные навыки Cline

Это глобальная копия пакета навыков из репозитория
[whoSaysWhat](https://github.com/teaka-23/whoSaysWhat). Она установлена в
`~/.cline/skills/` и доступна во всех проектах. Cline 4.1.27 также читает
`~/.agents/skills/`; если вы предпочитаете этот каталог, скопируйте пакет туда.

Пакет основан на [mattpocock/skills](https://github.com/mattpocock/skills),
коммит `6f7f7831a625f040a8e06a0e7954826b87ce775a`, лицензия MIT (см. `LICENSE`).

## Что значит «нейтральные»

В `SKILL.md` нет путей и терминов конкретного проекта: ни `app/src/...`, ни
`JAVA_HOME`, ни имён Kotlin-классов, ни ссылок на дизайн-документ. Вместо этого
каждый навык перед работой ищет контекст в текущем репозитории:

- `.clinerules`, `AGENTS.md`;
- `docs/agents/project-context.md`;
- `GLOSSARY.md`;
- `docs/adr/`, `CODING_STANDARDS.md`, `CONTRIBUTING.md`;
- фактические команды сборки, тестов и lint.

Правила проекта важнее навыка. Если контекста нет, навык не выдумывает пути и
команды: он выясняет стек по репозиторию и спрашивает только то, что нельзя
прочитать. Ответы и документы — на языке пользователя или на языке, который
требует проект.

Межнавыковые ссылки относительные: `grilling/SKILL.md`,
`codebase-design/SKILL.md`, а не абсолютные пути.

## Навыки

`grill-me` — тонкая обёртка над `grilling`, поэтому каталогов восемь, а
логических навыков семь.

| Навык | Роль |
| --- | --- |
| `grilling` | Допрос по дереву решений: требования, границы, риски |
| `grill-me` | Явный запуск `grilling` командой `/grill-me` |
| `codebase-design` | Глубокие модули, интерфейсы, швы и тестируемость |
| `domain-modeling` | `GLOSSARY.md` и ADR |
| `tdd` | Red-green-refactor |
| `diagnosing-bugs` | Диагностика сложных багов через обратную связь |
| `code-review` | Ревью diff по стандартам и по спеке |
| `handoff` | Компактная передача контекста следующей сессии |

## Обновление

Источник истины — `.cline/skills/` в репозитории `whoSaysWhat`. После правки
навыков синхронизируйте глобальную копию:

```bash
cd /path/to/whoSaysWhat
./tools/sync_global_skills.sh
```

Скрипт копирует каталоги навыков и `LICENSE`, не удаляя посторонние навыки,
которые уже лежат в `~/.cline/skills/`. README глобальной копии берётся из
`docs/agents/global-skills-README.md`.

Ручной вариант:

```bash
rsync -a --exclude README.md .cline/skills/ "$HOME/.cline/skills/"
cp docs/agents/global-skills-README.md "$HOME/.cline/skills/README.md"
```

Перед синхронизацией проверьте:

- `name` в frontmatter совпадает с именем каталога;
- `description` короче 1024 символов;
- в frontmatter нет Claude-специфичных полей вроде `argument-hint` или
  `disable-model-invocation`: Cline core принимает `name`, `description`,
  `license`, `compatibility`, `metadata`, `allowed-tools`;
- в тексте нет проектных путей и команд.

## Почему копии должны совпадать

Cline сканирует сначала проектные каталоги (`.clinerules/skills`,
`.cline/skills`, `.claude/skills`, `.agents/skills`), затем глобальные
(`~/.cline/skills`, `~/.agents/skills`). При одинаковом id запись из более
позднего каталога может перекрыть предыдущую. Поэтому локальная и глобальная
копии держатся идентичными, а проектные особенности живут в `.clinerules` и
`docs/agents/project-context.md`, а не в `SKILL.md`.

Не добавляйте в глобальный каталог второй вариант навыка с тем же именем, но
другим содержимым: будет неочевидно, какая версия сработает.

## Проверка

- Навыки видны в меню Skills и по `/` после перезагрузки окна Cline.
- `/grill-me` в Plan mode не редактирует файлы, а задаёт вопросы.
- `tools/sync_global_skills.sh` завершается без ошибок.
- `diff -r` между `.cline/skills` и `~/.cline/skills` показывает только
  различия `README.md`.
