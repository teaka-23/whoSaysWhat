#!/usr/bin/env python3
"""Готовит короткие фрагменты «мяу», «хрю» и жужжания пчелы из оригиналов.

Первая версия этих трёх звуков не прошла проверку на слух: у кошки стояло
сиамское «мяу», похожее на плач ребёнка, у свиньи — щелчок с 80 % энергии
выше 1,2 кГц вместо низкого хрюканья, у пчелы — писк на 8 кГц вместо
жужжания. Скрипт режет другие записи Wikimedia Commons в тот же формат,
что и остальные `assets/sounds/*_real.wav`: моно 44,1 кГц PCM 16 бит,
короткие фейды, пик не выше 0,88, RMS около 0,15.

Зависимости: numpy и soundfile (`python3 -m pip install --user soundfile`).
FFmpeg не нужен: OGG/Opus/FLAC читаются libsndfile, ресемплинг — через
спектр, чтобы не тянуть scipy.

Использование:

    python3 tools/make_animal_sounds.py --fetch --src-dir /tmp/animal-src
    python3 tools/make_animal_sounds.py --src-dir /tmp/animal-src --out-dir app/src/main/assets/sounds

`--fetch` скачивает оригиналы с Commons (нужен curl), без него скрипт ждёт
файлы с именами из `SOURCES` в `--src-dir`.

Выбранные фрагменты сверены по признакам: у «мяу» один слог с подъёмом
470 → 650 → 590 Гц, у хрюканья ≈ 77 % энергии ниже 300 Гц (пик спектра
≈ 290 Гц, выше 1,2 кГц — меньше 9 %), у жужжания основной тон 216–241 Гц
с гармониками и ровным конвертом.
"""
import argparse
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
SOURCES = {
    'cat_meow.ogg': 'File:Meow.ogg',
    'boar_grunts.ogg': 'File:Boar.Grwls(1).ogg',
    'bee_swarm.wav': (
        'File:263673 ylearkisto mehilainen-tarhamehilainen-parvi-bees-honeybees-'
        'a-swarm-of-bees-buzzing-among-the-flowers-apis-mellifera.wav'
    ),
}

# out: имя ассета, src: файл в --src-dir, start/length: окно в секундах.
CLIPS = (
    dict(out='cat_real.wav', src='cat_meow.ogg', start=0.00, length=0.75, fade_in=0.005, fade_out=0.15),
    dict(out='pig_real.wav', src='boar_grunts.ogg', start=14.60, length=1.80, fade_in=0.03, fade_out=0.15),
    dict(out='bee_real.wav', src='bee_swarm.wav', start=29.00, length=2.20, fade_in=0.10, fade_out=0.20),
)


def fetch(src_dir: str) -> None:
    """Скачивает оригиналы по коротким именам из SOURCES."""
    ua = 'whoSaysWhat-assets/1.0 (kids app; sounds build script)'
    for name, title in SOURCES.items():
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
        url = page['imageinfo'][0]['url']
        print(f'скачиваю {name}')
        subprocess.run(['curl', '-sL', '-m', '600', '-A', ua, '-o', dst, url], check=True)


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


def level(x: np.ndarray) -> np.ndarray:
    """Подтягивает RMS к общему уровню остальных звуков, но не выше пика 0.88."""
    rms = float(np.sqrt(np.mean(x ** 2))) or 1.0
    x = x * (RMS / rms)
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
        src = os.path.join(args.src_dir, clip['src'])
        data, sr = read_mono(src)
        a = int(clip['start'] * sr)
        b = a + int(clip['length'] * sr)
        if b > len(data):
            raise SystemExit(f'{src}: запись короче {clip["start"] + clip["length"]:.2f} с')
        piece = resample(data[a:b], sr)
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
