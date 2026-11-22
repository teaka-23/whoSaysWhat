# Кто как говорит? — Android-приложение

Нативное приложение по `kto_kak_govorit_design_doc.md`: спокойная детская
игра для запуска речи. Шесть животных с иллюстрациями и звуками, игра
«Угадай», родительская панель с прогрессом и необязательный режим «Эхо».
Работает офлайн, без рекламы, аналитики и аккаунтов.

## Стек

| Слой | Решение |
| --- | --- |
| Язык / UI | Kotlin 2.0.21, Jetpack Compose, Material 3 |
| Сборка | Gradle 8.9, AGP 8.7.3, version catalog |
| minSdk / target | 26 / 35 (Android 8.0 … 15) |
| Контент | `assets/catalog.json` + kotlinx.serialization |
| Звук | `SoundPool` + `AudioFocus`, всё в APK |
| Прогресс | DataStore Preferences |
| Разрешения | `RECORD_AUDIO` — только для необязательного режима «Эхо»; `INTERNET` отсутствует |

## Сборка

Нужны JDK 17+ (проверено на Temurin 21) и Android SDK: `platforms;android-35`,
`build-tools;35.0.0`, `platform-tools`.

```bash
export JAVA_HOME=$HOME/jdk/jdk-21.0.13.1+2/Contents/Home
export ANDROID_HOME=$HOME/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew testDebugUnitTest     # unit-тесты логики и каталога
./gradlew lintDebug             # отчёт: app/build/reports/lint-results-debug.html
./gradlew assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease       # подписанный APK (при наличии keystore.properties)
./gradlew bundleRelease         # AAB для RuStore
```

`RECORD_AUDIO` запрашивается только после объяснения и явного нажатия
«Разрешить микрофон»; без него игра работает полностью. AndroidX добавляет
в манифест signature-level разрешение
`ru.tac.whosayswhat.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` для внутренних
receiver'ов — оно не показывается пользователю и не запрашивается в рантайме.

Подпись: скопируйте `keystore.properties.example` в `keystore.properties`,
положите `release.keystore` в корень проекта. Оба файла в `.gitignore`.

## Что уже работает

- **Песочница**: 6 животных с авторскими иллюстрациями, тап по карточке →
  звук; крупные зоны нажатия, верхний отступ не пересекается с замком.
- **Игра «Угадай»**: 5 правильных ответов до награды; 2/3/4 карточки
  (родительская настройка); сетка 2×N подстраивается под высоту экрана без
  прокрутки; звук повторяется автоматически и по кнопке; ошибка не
  наказывает — карточка качается, раунд продолжается.
- **Без повторов**: правильный ответ не выпадает два раза подряд, позиция
  балансируется, набор карточек меняется.
- **Награда**: короткое конфетти + «Молодец!» и звуковая похвала.
- **Прогресс**: сессии, верные ответы и звёзды по каждому животному в
  DataStore; сброс в родительской панели.
- **Родительский гейт**: три быстрых нажатия по замку (без математики),
  панель с уровнем, режимом «Эхо», сбросом прогресса и блоком «Приватность».
  Back тоже открывает панель.
- **Режим «Эхо»**: объяснение до системного разрешения, запись до 5 секунд,
  воспроизведение, удаление файла при закрытии окна. Запись не покидает
  устройство.
- **Приватность**: нет `INTERNET`, рекламы, аналитики и аккаунтов.
- **Киоск-режим**: экран не гаснет, системные панели скрыты.

## Перед публикацией

Готово:

- [x] **Звуки.** Шесть записей животных — реальные (Wikimedia Commons,
  CC0 / CC BY-SA / public domain) в `assets/sounds/*_real.wav`; атрибуция
  и ссылки на лицензии — в `assets/licenses/animal-sounds.md`,
  `THIRD_PARTY_NOTICES.md` и в панели «Для родителей». `praise.wav` и
  `nudge.wav` пока синтезированы скриптом
  `tools/generate_placeholder_sounds.py`. Человеческие звукоподражания
  («му», «мяу») можно добавить позже отдельными файлами и полем
  `onomatopoeia_file` — код уже поддерживает такой фолбэк.
- [x] **Подпись.** `release.keystore` и `keystore.properties` созданы
  локально (в `.gitignore`), `assembleRelease` и `bundleRelease` собираются.
  Сохраните оба файла и пароль в надёжном месте: без них нельзя выпускать
  обновления.
- [x] **Политика конфиденциальности.** Оператор и контакт заполнены в
  `store/privacy-policy.md` / `.html`. HTML проверен локально и публикуется
  workflow `.github/workflows/pages.yml`.
- [x] **Проверка сборки.** `testDebugUnitTest`, `lintDebug`, `assembleRelease`
  и `bundleRelease` проходят; звуки и диалог лицензий проверены на эмуляторе
  Android 15.

Осталось:

- [ ] Инициализировать git-репозиторий, запушить в `teaka-23/whoSaysWhat` и
  включить GitHub Pages (Settings → Pages → Source: GitHub Actions). После
  публикации проверить
  https://teaka-23.github.io/whoSaysWhat/privacy-policy.html и указать этот
  URL в карточке RuStore.
- [ ] Проверить на Android 8.0, телефоне с 2 ГБ RAM и планшете 10″; проверить
  запись и отзыв разрешения микрофона.
- [ ] Загрузить `app/build/outputs/bundle/release/app-release.aab` в RuStore
  Консоль и заполнить карточку по `store/listing-ru.md`.

## Структура

```
app/src/main/
├── assets/catalog.json          # каталог животных
├── assets/images/*_512.png      # шесть иллюстраций
├── assets/licenses/animal-sounds.md # атрибуция звуков
├── assets/sounds/*.wav          # звуки (в APK, офлайн)
├── java/ru/tac/whosayswhat/
│   ├── MainActivity.kt          # edge-to-edge, киоск-режим
│   ├── audio/SoundPlayer.kt     # SoundPool + AudioFocus
│   ├── audio/EchoRecorder.kt    # запись/воспроизведение «Эхо»
│   ├── data/                    # Animal, CatalogRepository, ProgressStore
│   ├── game/SessionEngine.kt    # чистая логика раундов
│   └── ui/                      # Sandbox, Guess, Reward, ParentPanel, Echo, App
└── res/                         # тема, иконка, строки
app/src/test/…/
├── game/SessionEngineTest.kt    # логика раундов
├── data/AnimalProgressTest.kt   # пороги звёзд и освоения
└── data/CatalogJsonTest.kt      # валидность catalog.json и наличие звуков
```

## Тесты

`SessionEngineTest` проверяет состав раунда, отсутствие повторов правильного
ответа, баланс позиций, реакцию на ошибку/успех и порог освоения.
`AnimalProgressTest` — звёзды, точность и переход животного в «освоено».
`CatalogJsonTest` парсит настоящий `catalog.json`, проверяет уникальность id
и то, что каждый файл из каталога существует в `assets/` (ловит опечатки до
запуска на устройстве).

```bash
./gradlew testDebugUnitTest
# отчёт: app/build/reports/tests/testDebugUnitTest/index.html
```

## Материалы для RuStore

- `store/listing-ru.md` — название, описания, ключевые слова, скриншоты,
  анкета возрастного рейтинга и декларация безопасности данных.
- `store/permission-justification.md` — обоснование `RECORD_AUDIO` и
  тестовый сценарий для модератора.
- `store/privacy-policy.md` / `.html` — политика конфиденциальности.
- `store/assets/icon-512.png`, `feature-1024x500.png` — иконка и баннер.
- `store/screenshots/01-sandbox.png` … `06-privacy.png` — шесть экранов
  1080×1920, снятых с текущей сборки.
- `THIRD_PARTY_NOTICES.md`, `app/src/main/assets/licenses/animal-sounds.md` —
  атрибуция звуков; в приложении она открывается из панели родителей.
- `.github/workflows/pages.yml` — публикация политики на GitHub Pages.

## Дальше по дизайн-документу

- человеческая озвучка звукоподражаний (лицензии на звуки уже сохранены в
  `assets/licenses/`);
- подпись релиза и публикация в RuStore;
- проверка на Android 8.0, устройстве с 2 ГБ RAM и планшете 10″;
- при желании: уровни по фонетической сложности и другие животные.
