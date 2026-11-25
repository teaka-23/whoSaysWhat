#!/usr/bin/env python3
"""Готовит короткие фрагменты звуков животных из оригиналов.

Первая версия трёх звуков не прошла проверку на слух: у кошки стояло
сиамское «мяу», похожее на плач ребёнка, у свиньи — щелчок с 80 % энергии
выше 1,2 кГц вместо низкого хрюканья, у пчелы — писк на 8 кГц вместо
жужжания. Скрипт режет другие записи Wikimedia Commons в тот же формат,
что и остальные `assets/sounds/*_real.wav`: моно 44,1 кГц PCM 16 бит,
короткие фейды, пик не выше 0,88, RMS около 0,15.

Для гуся, свиньи, осла и мышки выбраны более чистые и дружелюбные записи:
домашние гуси, три настоящих «oink» с фермы Mudchute, полный «иа» осла и два
коротких пика мышки вместо шумного превью. Волк перерезан на более низком
и мягком вое. Всем новым фрагментам добавлен gentle low-pass: он убирает
шипение и щелчки, не делая голос глухим.

Зависимости: numpy и soundfile (`python3 -m pip install --user soundfile`).
FFmpeg не нужен: OGG/FLAC/MP3 читаются libsndfile, ресемплинг — через
спектр, чтобы не тянуть scipy. Для записей с Ogg Skeleton (курица) скрипт
скачивает MP3-транскод Commons: такие файлы libsndfile тоже читает.

Использование:

    python3 tools/make_animal_sounds.py --fetch --src-dir /tmp/animal-src
    python3 tools/make_animal_sounds.py --src-dir /tmp/animal-src --out-dir app/src/main/assets/sounds

`--fetch` скачивает оригиналы с Commons (нужен curl), без него скрипт ждёт
файлы с именами из `SOURCES` в `--src-dir`.

Выбранные фрагменты сверены по признакам: у «мяу» один слог с подъёмом
470 → 650 → 590 Гц; у гуся — ровные домашние «га-га» (3,55–6,00 с); у свиньи —
три коротких «oink» с фермы Mudchute (0,34 / 0,28 / 0,18 с); у осла — полное
«иа» с вдохом и выдохом (5,90–8,60 с); у мышки — два пика в 1,5–4 кГц
(0,27–0,51 и 3,27–3,63 с); у волка — низкий вой с основным тоном ≈ 400–500 Гц
(9,00–11,30 с). У курицы выбраны звонкие «ко-ко» (6,60–9,00 с), у медведя —
низкое рычание (0,20–2,60 с), у вороны — четыре «кар» подряд (0–2,55 с),
у кукушки — два полных «ку-ку» (2,55–5,00 с, 650–1000 Гц), у слона — труба
(0–1,40 с). Все фрагменты проходят через мягкий high-pass, а новые — ещё и
через gentle low-pass, чтобы убрать гул, шипение и щелчки.
"""
import argparse
import math
import os
import subprocess
import urllib.parse

import numpy as np
import soundfile as sf

SR = 44100
PEAK = 0.88
RMS = 0.15

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_OUT = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'sounds')

# Оригиналы на Wikimedia Commons (атрибуция — в assets/licenses/animal-sounds.md).
# dict с transcode=True — скачать MP3-транскод Commons вместо оригинала.
SOURCES = {
    'cat_meow.ogg': 'File:Meow.ogg',
    'goose_honking.ogg': 'File:Geese Honking (loud).ogg',
    'pig_oink_1.ogg': 'File:Mudchute pig 1.ogg',
    'pig_oink_2.ogg': 'File:Mudchute pig 2.ogg',
    'pig_oink_3.ogg': 'File:Mudchute pig 3.ogg',
    'bee_swarm.wav': (
        'File:263673 ylearkisto mehilainen-tarhamehilainen-parvi-bees-honeybees-'
        'a-swarm-of-bees-buzzing-among-the-flowers-apis-mellifera.wav'
    ),
    'chicken_clucks.mp3': {
        'title': 'File:Chickens demanding food.ogg',
        'transcode': True,
    },
    'donkey_bray.wav': (
        'File:157763 felix-blume a-donkey-is-braying-in-his-enclosure-in-'
        'south-of-france.wav'
    ),
    'mouse_squeaks.flac': 'File:Mouse voice.flac',
    'wolf_howls.ogg': 'File:Wolf howls.ogg',
    'bear_growl.ogg': 'File:Bear growl.ogg',
    'crow_caws.mp3': 'File:Corvus corone - Carrion Crow XC511975.mp3',
    'cuckoo_song.ogg': 'File:Cuculus canorus v01.ogg',
    'elephant_trumpet.ogg': 'File:Elephant voice - trumpeting.ogg',
}

# out: имя ассета, src: файл в --src-dir, start/length: окно в секундах.
# parts — несколько окон подряд (для «oink-oink» и «пи-пи»); gap — пауза
# между ними. high_pass/low_pass — однополюсные фильтры против гула и шипения.
CLIPS = (
    dict(out='cat_real.wav', src='cat_meow.ogg', start=0.00, length=0.75, fade_in=0.005, fade_out=0.15),
    dict(
        out='goose_real.wav', src='goose_honking.ogg', start=3.55, length=2.45,
        fade_in=0.08, fade_out=0.25, high_pass=180, low_pass=4800,
    ),
    dict(
        out='pig_real.wav',
        parts=(
            dict(src='pig_oink_1.ogg', start=0.00, length=0.34),
            dict(src='pig_oink_2.ogg', start=0.04, length=0.28),
            dict(src='pig_oink_3.ogg', start=0.02, length=0.18),
        ),
        gap=0.16, high_pass=250, low_pass=6000, fade_in=0.01, fade_out=0.15,
    ),
    dict(out='bee_real.wav', src='bee_swarm.wav', start=29.00, length=2.20, fade_in=0.10, fade_out=0.20),
    dict(out='chicken_real.wav', src='chicken_clucks.mp3', start=6.60, length=2.40, fade_in=0.01, fade_out=0.15, high_pass=150),
    dict(
        out='donkey_real.wav', src='donkey_bray.wav', start=5.90, length=2.70,
        fade_in=0.06, fade_out=0.30, high_pass=120, low_pass=5000,
    ),
    dict(
        out='mouse_real.wav',
        parts=(
            dict(src='mouse_squeaks.flac', start=0.27, length=0.24),
            dict(src='mouse_squeaks.flac', start=3.27, length=0.36),
        ),
        gap=0.20, high_pass=900, low_pass=8000, fade_in=0.01, fade_out=0.08,
    ),
    dict(
        out='wolf_real.wav', src='wolf_howls.ogg', start=9.00, length=2.30,
        fade_in=0.12, fade_out=0.30, high_pass=90, low_pass=3000,
    ),
    dict(out='bear_real.wav', src='bear_growl.ogg', start=0.20, length=2.40, fade_in=0.03, fade_out=0.20, high_pass=60),
    dict(out='crow_real.wav', src='crow_caws.mp3', start=0.00, length=2.55, fade_in=0.02, fade_out=0.15, high_pass=150),
    dict(out='cuckoo_real.wav', src='cuckoo_song.ogg', start=2.55, length=2.45, fade_in=0.02, fade_out=0.15, high_pass=200),
    dict(out='elephant_real.wav', src='elephant_trumpet.ogg', start=0.00, length=1.40, fade_in=0.02, fade_out=0.15, high_pass=60),
)


def fetch(src_dir: str) -> None:
    """Скачивает оригиналы по коротким именам из SOURCES."""
    ua = 'whoSaysWhat-assets/1.0 (kids app; sounds build script)'
    for name, entry in SOURCES.items():
        if isinstance(entry, str):
            entry = {'title': entry}
        title = entry['title']
        dst = os.path.join(src_dir, name)
        if os.path.exists(dst) and os.path.getsize(dst) > 4096:
            print(f'уже есть: {dst}')
            continue
        query = urllib.parse.urlencode({
            'action': 'query', 'format': 'json', 'titles': title,
            'prop': 'imageinfo', 'iiprop': 'url',
        })
        raw = subprocess.run(
            ['curl', '-s', '-m', '60', '-A', ua, 'https://commons.wikimedia.org/w/api.php?' + query],
            check=True, capture_output=True,
        ).stdout
        import json
        page = next(iter(json.loads(raw)['query']['pages'].values()))
        if 'imageinfo' not in page:
            raise SystemExit(f'на Commons нет файла {title}')
        url = page['imageinfo'][0]['url'].split('?')[0]
        if entry.get('transcode'):
            url = (
                url.replace('/wikipedia/commons/', '/wikipedia/commons/transcoded/')
                + '/' + os.path.basename(url) + '.mp3'
            )
        print(f'скачиваю {name}')
        subprocess.run(
            ['curl', '-fL', '--retry', '5', '--retry-delay', '2', '--retry-all-errors',
             '-m', '600', '-A', ua, '-o', dst, url],
            check=True,
        )


def read_mono(path: str):
    data, sr = sf.read(path, always_2d=False)
    data = np.asarray(data, dtype=np.float64)
    if data.ndim > 1:
        data = data.mean(axis=1)
    return data, sr


def resample(x: np.ndarray, sr: int, target: int = SR) -> np.ndarray:
    """Ресемплинг через спектр: без scipy и без слышимых артефактов для наших записей."""
    if sr == target:
        return x
    n_new = int(round(len(x) * target / sr))
    spec = np.fft.rfft(x)
    keep = n_new // 2 + 1
    if keep <= len(spec):
        spec = spec[:keep]
    else:
        spec = np.pad(spec, (0, keep - len(spec)))
    return np.fft.irfft(spec, n_new) * (n_new / len(x))


def fade(x: np.ndarray, sr: int, fade_in: float, fade_out: float) -> np.ndarray:
    x = x.copy()
    n_in = max(1, int(fade_in * sr))
    n_out = max(1, int(fade_out * sr))
    x[:n_in] *= np.linspace(0.0, 1.0, n_in) ** 1.5
    x[-n_out:] *= np.linspace(1.0, 0.0, n_out) ** 1.5
    return x


def high_pass(x: np.ndarray, sr: int, cutoff: float) -> np.ndarray:
    """Однополюсный high-pass: убирает гул и ветер, не трогая голос."""
    if not cutoff:
        return x
    rc = 1.0 / (2.0 * math.pi * cutoff)
    alpha = rc / (rc + 1.0 / sr)
    y = np.empty_like(x)
    prev_x = 0.0
    prev_y = 0.0
    for index, sample in enumerate(x):
        prev_y = alpha * (prev_y + sample - prev_x)
        prev_x = sample
        y[index] = prev_y
    return y


def low_pass(x: np.ndarray, sr: int, cutoff: float) -> np.ndarray:
    """Однополюсный low-pass: убирает шипение и щелчки, оставляя голос тёплым."""
    if not cutoff:
        return x
    rc = 1.0 / (2.0 * math.pi * cutoff)
    alpha = 1.0 / (1.0 + rc * sr)
    y = np.empty_like(x)
    prev = 0.0
    for index, sample in enumerate(x):
        prev = prev + alpha * (sample - prev)
        y[index] = prev
    return y


def level(x: np.ndarray) -> np.ndarray:
    """Подтягивает RMS к общему уровню остальных звуков, но не выше пика 0.88."""
    for _ in range(4):
        rms = float(np.sqrt(np.mean(x ** 2))) or 1.0
        x = x * (RMS / rms)
        peak = float(np.max(np.abs(x))) or 1.0
        if peak <= PEAK:
            break
        # Мягко ограничиваем одиночные щелчки, иначе из-за них весь фрагмент
        # приходится делать тише остальных.
        x = np.tanh(x / PEAK) * PEAK / np.tanh(1.0)
    peak = float(np.max(np.abs(x))) or 1.0
    if peak > PEAK:
        x = x * (PEAK / peak)
    return x


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--src-dir', required=True, help='куда скачивать/откуда брать оригиналы')
    parser.add_argument('--out-dir', default=DEFAULT_OUT, help='папка assets/sounds')
    parser.add_argument('--fetch', action='store_true', help='скачать оригиналы с Commons')
    args = parser.parse_args()

    os.makedirs(args.src_dir, exist_ok=True)
    if args.fetch:
        fetch(args.src_dir)

    for clip in CLIPS:
        parts = clip.get('parts') or (clip,)
        pieces = []
        for index, part in enumerate(parts):
            src = os.path.join(args.src_dir, part['src'])
            data, sr = read_mono(src)
            a = int(part['start'] * sr)
            b = a + int(part['length'] * sr)
            if b > len(data):
                raise SystemExit(
                    f'{src}: запись короче {part["start"] + part["length"]:.2f} с'
                )
            piece = resample(data[a:b], sr)
            piece = high_pass(piece, SR, clip.get('high_pass', 0))
            piece = low_pass(piece, SR, clip.get('low_pass', 0))
            if len(parts) > 1:
                # Каждый «oink»/«пи» получает собственные микрофейды, чтобы
                # склейка не щёлкала; общий конверт — ниже.
                piece = fade(piece, SR, 0.006, 0.012)
            if pieces:
                pieces.append(np.zeros(int(clip.get('gap', 0.0) * SR)))
            pieces.append(piece)
        piece = np.concatenate(pieces)
        piece = fade(piece, SR, clip['fade_in'], clip['fade_out'])
        piece = level(piece)
        dst = os.path.join(args.out_dir, clip['out'])
        sf.write(dst, piece, SR, subtype='PCM_16')
        peak = float(np.max(np.abs(piece)))
        rms = float(np.sqrt(np.mean(piece ** 2)))
        print(f'{clip["out"]:16s} {len(piece) / SR:.2f} с  пик {peak:.2f}  RMS {rms:.3f}  '
              f'{os.path.getsize(dst) // 1024} КБ')


if __name__ == '__main__':
    main()
