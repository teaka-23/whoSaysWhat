#!/bin/bash
# Снимает скриншоты карточки RuStore с запущенного эмулятора.
#
# Требования консоли (поле «Телефон», 16:9): до 5 МБ, максимум 3840×2160,
# рекомендуем JPG 1920×1080. Скрипт выставляет на эмуляторе ровно 1920×1080,
# проходит по трём экранам и сохраняет JPG в store/screenshots.
#
# Использование:
#   tools/capture_store_screenshots.sh [путь-к-adb]
# Эмулятор должен быть уже запущен (`adb devices` показывает устройство).
set -euo pipefail

ADB="${1:-$HOME/Library/Android/sdk/platform-tools/adb}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/store/screenshots"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
PKG="ru.tac.whosayswhat"
SIZE="1920x1080"
DENSITY=320

[ -x "$ADB" ] || { echo "adb не найден: $ADB" >&2; exit 1; }
[ -f "$APK" ] || { echo "нет debug APK: $APK (соберите :app:assembleDebug)" >&2; exit 1; }
"$ADB" devices | grep -q "device$" || { echo "эмулятор не подключён" >&2; exit 1; }

mkdir -p "$OUT"
"$ADB" shell wm size "$SIZE" >/dev/null
"$ADB" shell wm density "$DENSITY" >/dev/null
"$ADB" uninstall "$PKG" >/dev/null 2>&1 || true
"$ADB" install -r "$APK" >/dev/null
"$ADB" shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 5
# Первый кадр после установки ловит ещё не дорисованные иллюстрации: греем экран.
"$ADB" exec-out screencap -p > /dev/null
sleep 4

snap() { # snap <имя-без-расширения>
  local tmp="/tmp/rustore-screen-$1.png"
  "$ADB" exec-out screencap -p > "$tmp"
  sips -s format jpeg -s formatOptions 92 "$tmp" --out "$OUT/$1.jpg" >/dev/null
  echo "  $OUT/$1.jpg"
}

echo "1. Песочница"
snap "01-sandbox-1920x1080"

# Замок в правом верхнем углу; кнопка «Играть» — внизу по центру.
echo "2. Игра «Угадай»"
"$ADB" shell input tap 960 966
sleep 3
snap "02-guess-1920x1080"

echo "3. Для родителей"
"$ADB" shell input keyevent 4
sleep 1
"$ADB" shell "input tap 1855 118; input tap 1855 118; input tap 1855 118"
sleep 2
snap "03-parents-1920x1080"

echo "4. Приватность (прокрутка панели)"
"$ADB" shell input swipe 960 900 960 250 400
sleep 1
snap "04-privacy-1920x1080"

"$ADB" shell input keyevent 4 >/dev/null
"$ADB" shell wm size reset >/dev/null
"$ADB" shell wm density reset >/dev/null

echo "Готово. Проверка размеров:"
for f in "$OUT"/*.jpg; do
  printf '  %s: ' "$(basename "$f")"
  sips -g pixelWidth -g pixelHeight "$f" | tail -2 | tr -d '\n' | sed 's/  */ /g'
  printf ', %s КБ\n' "$(( $(stat -f%z "$f") / 1024 ))"
done
