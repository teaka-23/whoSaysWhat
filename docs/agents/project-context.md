# Контекст для агента

Этот файл читают навыки агента перед планированием, тестами и ревью.
Правила проекта в `.clinerules` важнее любых советов из навыков.

## Что за проект

«Кто как говорит?» — нативное Android-приложение для развития речи детей
1,5–4 лет: песочница со звуками животных, игра «Угадай», награда и
необязательный режим «Эхо». Работает офлайн, без аккаунтов, рекламы и
аналитики.

- Kotlin 2.0.21, Jetpack Compose, Material 3.
- `minSdk 26`, `targetSdk 35`, `versionName 0.3.1`.
- Контент: `app/src/main/assets/catalog.json`, изображения и WAV в APK.
- Прогресс: DataStore, только локально.
- `INTERNET` отсутствует; `RECORD_AUDIO` нужен только для «Эха».

## Документы

- `kto_kak_govorit_design_doc.md` — исходный дизайн-документ и продуктовая
  логика.
- `GLOSSARY.md` — канонические термины. Новые термины сначала добавляйте
  туда.
- `docs/adr/` — решения, которые нельзя молча менять.
- `README.md` — фактическое состояние сборки, звуков, публикации.

## Сборка и проверки

Gradle требует JDK 21:

```bash
export JAVA_HOME=$HOME/jdk/jdk-21.0.13.1+2/Contents/Home
./gradlew --offline :app:testDebugUnitTest
./gradlew --offline :app:lintDebug
./gradlew --offline :app:assembleRelease
./gradlew --offline :app:bundleRelease
```

Перед коммитом обязательны `testDebugUnitTest`, `lintDebug`,
`assembleRelease`. Для магазина — ещё `bundleRelease` (AAB).

## Где тестировать

- Unit-тесты: `app/src/test/java/ru/tac/whosayswhat/`.
- Чистая логика игры — `game/SessionEngine.kt`, без Android-зависимостей.
- Каталог и прогресс — `data/`, с тестами на JSON и `AnimalProgress`.
- Звук/TTS/микрофон — `audio/`; на эмуляторе проверяются вручную, unit-тестами
  покрываются только чистые функции (например, `narrationPhrase`,
  `EchoFiles`, `WavDuration`).

## Диагностика

- Для аудио смотрите `adb logcat` по тегам `SpeechAnnouncer`, `SoundPlayer`,
  `EchoRecorder`.
- Не логируйте содержимое записи «Эха» и не добавляйте разрешение `INTERNET`.
- Если баг в ассете, правьте скрипт в `tools/`, а не PNG/WAV вручную.

## Ревью

Стандарты: `.clinerules`, `README.md`, `GLOSSARY.md`,
`docs/agents/project-context.md`, `kto_kak_govorit_design_doc.md`.
Спека: GitHub Issue, `docs/specs/*` или раздел дизайн-документа; если спеки
нет, скажите об этом явно. Не считайте ошибкой то, что уже проверяет
Gradle или lint.

## Передача контекста

В handoff укажите версию (`versionName`/`versionCode`), последний коммит,
статус тестов и lint, пути к APK/AAB и что осталось проверить на физическом
устройстве. Не копируйте длинные логи и `THIRD_PARTY_NOTICES.md` — дайте
ссылки на файлы.

## Как работаем с навыками

1. Новая фича или неясное требование — `/grill-me` в Plan mode.
2. Архитектура и границы модулей — `codebase-design`.
3. Новые термины или решение — `domain-modeling`, обновить `GLOSSARY.md`
   и/или ADR.
4. Реализация — `tdd`, вертикальными срезами.
5. Сложный баг — `diagnosing-bugs`, сначала воспроизводимый красный тест.
6. Перед коммитом — `code-review` от закреплённой точки.
7. Длинная сессия — `handoff` в `$TMPDIR`, не в репозиторий.

## Трекер

Задачи и спеки — GitHub Issues репозитория `teaka-23/whoSaysWhat` (CLI `gh`).
Если задачи ещё нет, спекой может быть раздел `kto_kak_govorit_design_doc.md`
или файл в `docs/specs/`; в ревью укажите источник явно.
