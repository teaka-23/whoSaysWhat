#!/usr/bin/env python3
"""Проверяет, что сообщение для модератора RuStore укладывается в лимит.

Консоль RuStore ограничивает поле «Комментарий для модератора» 180
символами. Текст живёт в `store/permission-justification.md` и легко
обрастает подробностями при правках, поэтому лимит закреплён проверкой:
первый блок после заголовка — финальный вариант (символы), второй, если
есть, — резерв на случай байтового счётчика (байты). Копия сообщения в
`store/rustore-technical-form.md` должна совпадать с финальным вариантом.

Запуск: `python3 tools/check_moderator_note.py` (в CI идёт до тестов).
"""

from __future__ import annotations

import sys
from pathlib import Path

LIMIT_CHARS = 180
LIMIT_BYTES = 180
NOTE_HEADING = "## Сообщение в поле «Комментарий для модератора»"
NOTE_PATH = Path(__file__).resolve().parent.parent / "store" / "permission-justification.md"
FORM_PATH = Path(__file__).resolve().parent.parent / "store" / "rustore-technical-form.md"


def fenced_blocks(text: str) -> list[str]:
    """Возвращает все fenced-блоки документа в порядке появления."""
    blocks: list[str] = []
    current: list[str] | None = None
    for line in text.splitlines():
        if line.strip() == "```":
            if current is None:
                current = []
            else:
                blocks.append("\n".join(current).strip())
                current = None
        elif current is not None:
            current.append(line.rstrip())
    if current is not None:
        raise SystemExit("Незакрытый блок кода в документе")
    return [block for block in blocks if block]


def note_blocks(text: str) -> list[str]:
    """Возвращает fenced-блоки из раздела с сообщением для модератора."""
    lines = text.splitlines()
    start = next(
        (index for index, line in enumerate(lines) if line.startswith(NOTE_HEADING)),
        None,
    )
    if start is None:
        raise SystemExit(f"Не найден раздел «{NOTE_HEADING}» в {NOTE_PATH}")

    section: list[str] = []
    for line in lines[start + 1:]:
        if line.startswith("## "):
            break
        section.append(line)
    return fenced_blocks("\n".join(section))


def check(blocks: list[str]) -> list[str]:
    """Возвращает список проблем; пустой список — всё в порядке."""
    problems: list[str] = []
    if not blocks:
        problems.append(f"В разделе «{NOTE_HEADING}» нет ни одного варианта сообщения")
        return problems

    main = blocks[0]
    chars, size = len(main), len(main.encode("utf-8"))
    print(f"Основное сообщение: {chars} символов, {size} байт")
    if chars > LIMIT_CHARS:
        problems.append(
            f"Основное сообщение длиннее лимита: {chars} > {LIMIT_CHARS} символов"
        )

    if len(blocks) > 1:
        spare = blocks[1]
        spare_chars, spare_size = len(spare), len(spare.encode("utf-8"))
        print(f"Резервное сообщение: {spare_chars} символов, {spare_size} байт")
        if spare_chars > LIMIT_CHARS:
            problems.append(
                f"Резервное сообщение длиннее лимита: {spare_chars} > {LIMIT_CHARS} символов"
            )
        if spare_size > LIMIT_BYTES:
            problems.append(
                "Резервное сообщение не спасает при байтовом лимите: "
                f"{spare_size} > {LIMIT_BYTES} байт"
            )
    return problems


def form_mismatch(note: str) -> str | None:
    """Сверяет копию сообщения в описании технических сведений."""
    if not FORM_PATH.exists():
        return None
    copies = [
        block
        for block in fenced_blocks(FORM_PATH.read_text(encoding="utf-8"))
        if block.startswith("Микрофон")
    ]
    if copies and copies[0] != note:
        return (
            f"Копия сообщения в {FORM_PATH.name} не совпадает "
            "с основной версией из permission-justification.md"
        )
    return None


def main() -> int:
    blocks = note_blocks(NOTE_PATH.read_text(encoding="utf-8"))
    problems = check(blocks)
    if blocks:
        mismatch = form_mismatch(blocks[0])
        if mismatch:
            problems.append(mismatch)
    if problems:
        for problem in problems:
            print(f"ОШИБКА: {problem}", file=sys.stderr)
        print(f"Файл: {NOTE_PATH}", file=sys.stderr)
        return 1
    print("Лимит 180 символов соблюдён")
    return 0


if __name__ == "__main__":
    sys.exit(main())
