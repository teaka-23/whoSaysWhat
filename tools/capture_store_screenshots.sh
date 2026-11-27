#!/bin/bash
# Снимает скриншоты карточки RuStore с запущенного эмулятора.
#
# Требования консоли (поле «Телефон», 16:9): до 5 МБ, максимум 3840×2160,
# рекомендуем JPG 1920×1080. Скрипт выставляет на эмуляторе ровно 1920×1080,
# проходит по экранам и сохраняет JPG в store/screenshots.
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
# 16:9 по умолчанию; при отказах валидатора можно снять то же в 1280×720:
#   SIZE=1280x720 tools/capture_store_screenshots.sh
SIZE="${SIZE:-1920x1080}"
DENSITY=320
# Координаты жестов заданы для 1920×1080; для другого 16:9 размера пересчитываем.
BASE_W=1920
BASE_H=1080
W="${SIZE%x*}"
H="${SIZE#*x}"

[ -x "$ADB" ] || { echo "adb не найден: $ADB" >&2; exit 1; }
[ -f "$APK" ] || { echo "нет debug APK: $APK (соберите :app:assembleDebug)" >&2; exit 1; }
"$ADB" devices | grep -q "device$" || { echo "эмулятор не подключён" >&2; exit 1; }

mkdir -p "$OUT"
MADE=()
"$ADB" shell wm size "$SIZE" >/dev/null
"$ADB" shell wm density "$DENSITY" >/dev/null
"$ADB" uninstall "$PKG" >/dev/null 2>&1 || true
"$ADB" install -r "$APK" >/dev/null
"$ADB" shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 5
# Первый кадр после установки ловит ещё не дорисованные иллюстрации: греем экран.
"$ADB" exec-out screencap -p > /dev/null
sleep 4

tap() { # tap <x> <y> в координатах 1920×1080
  "$ADB" shell input tap $(( $1 * W / BASE_W )) $(( $2 * H / BASE_H ))
}

swipe() { # swipe <x1> <y1> <x2> <y2> <мс> в координатах 1920×1080
  "$ADB" shell input swipe \
    $(( $1 * W / BASE_W )) $(( $2 * H / BASE_H )) \
    $(( $3 * W / BASE_W )) $(( $4 * H / BASE_H )) "$5"
}

snap() { # snap <имя-без-расширения>
  local tmp="/tmp/rustore-screen-$1.png"
  "$ADB" exec-out screencap -p > "$tmp"
  # JPEG собираем через Pillow: sRGB-профиль внутрь, без EXIF и Photoshop-блока,
  # 4:4:4 без прореживания цвета — так файл принимают любые валидаторы каталогов.
  python3 - "$tmp" "$OUT/$1.jpg" <<'PY'
import sys
from PIL import Image, ImageCms

src, dst = sys.argv[1], sys.argv[2]
srgb = ImageCms.ImageCmsProfile(ImageCms.createProfile("sRGB")).tobytes()
image = Image.open(src).convert("RGB")
image.save(dst, "JPEG", quality=90, subsampling=0, optimize=True, icc_profile=srgb)
PY
  echo "  $OUT/$1.jpg"
  MADE+=("$OUT/$1.jpg")
}

echo "1. Песочница"
snap "01-sandbox-$SIZE"

# Карточка коровы в первой ячейке сетки. Окно живёт от 2,5 до 6,5 с, поэтому
# кадр снимаем сразу после тапа, а потом ждём автозакрытия: иначе следующий
# тап по «Играть» попадёт в окно и звук заиграет заново.
echo "2. Крупный план"
tap 200 315
sleep 1
snap "02-closeup-$SIZE"
sleep 7

# Замок в правом верхнем углу; кнопка «Играть» — внизу по центру.
echo "3. Игра «Угадай»"
tap 960 966
sleep 3
snap "03-guess-$SIZE"

echo "4. Для родителей"
"$ADB" shell input keyevent 4
sleep 1
tap 1855 118
tap 1855 118
tap 1855 118
sleep 2
snap "04-parents-$SIZE"

echo "5. Приватность (прокрутка панели)"
swipe 960 900 960 250 400
sleep 1
snap "05-privacy-$SIZE"

"$ADB" shell input keyevent 4 >/dev/null
"$ADB" shell wm size reset >/dev/null
"$ADB" shell wm density reset >/dev/null

echo "Готово. Проверка размеров:"
for f in "${MADE[@]}"; do
  printf '  %s: ' "$(basename "$f")"
  sips -g pixelWidth -g pixelHeight "$f" | tail -2 | tr -d '\n' | sed 's/  */ /g'
  printf ', %s КБ\n' "$(( $(stat -f%z "$f") / 1024 ))"
done
