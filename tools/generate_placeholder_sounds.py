#!/usr/bin/env python3
"""Генерирует короткие WAV-заглушки для скелета приложения.

Это НЕ финальные звуки: простые тоны, чтобы проверить пайплайн
(assets -> SoundPool -> play). Заменить на реальные записи и озвучку.
"""
import math
import os
import struct
import wave

SR = 44100
DST = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "sounds")


def envelope(i: int, n: int, attack: float = 0.02, release: float = 0.08) -> float:
    a = int(n * attack) or 1
    r = int(n * release) or 1
    if i < a:
        return i / a
    if i > n - r:
        return max(0.0, (n - i) / r)
    return 1.0


def tone(freqs, dur, amp=0.5, vibrato=0.0, pulses=1):
    n = int(SR * dur)
    out = []
    for i in range(n):
        t = i / SR
        f = freqs[min(int(t / dur * len(freqs)), len(freqs) - 1)]
        if vibrato:
            f *= 1 + vibrato * math.sin(2 * math.pi * 5 * t)
        gate = 1.0
        if pulses > 1:
            gate = 0.35 + 0.65 * max(0.0, math.sin(2 * math.pi * pulses * t))
        s = amp * envelope(i, n) * gate * (
            math.sin(2 * math.pi * f * t) + 0.25 * math.sin(4 * math.pi * f * t)
        )
        out.append(s)
    return out


def arpeggio(freqs, step=0.14, amp=0.4):
    n = int(SR * step * len(freqs))
    out = [0.0] * n
    for k, f in enumerate(freqs):
        segment = tone([f], step, amp * 0.8)
        start = int(SR * step * k)
        for i, s in enumerate(segment):
            if start + i < n:
                out[start + i] += s
    return out


def save(name: str, samples):
    n = len(samples)
    frames = struct.pack("<%dh" % n, *[int(max(-1, min(1, s)) * 32767) for s in samples])
    with wave.open(os.path.join(DST, name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(frames)


def main():
    os.makedirs(DST, exist_ok=True)
    save("cow_real.wav", tone([135], 0.9, amp=0.55, vibrato=0.04))
    save("cow_voice.wav", tone([230, 200, 185], 0.75, amp=0.45))
    save("cat_real.wav", tone([640], 0.55, amp=0.35, vibrato=0.02))
    save("cat_voice.wav", tone([520, 680, 600], 0.6, amp=0.4))
    save("dog_real.wav", tone([280, 240], 0.45, amp=0.5, pulses=3))
    save("dog_voice.wav", tone([260, 300], 0.5, amp=0.45, pulses=2))
    save("duck_real.wav", tone([480], 0.4, amp=0.4, pulses=2))
    save("duck_voice.wav", tone([440, 500], 0.5, amp=0.4, pulses=2))
    save("horse_real.wav", tone([420, 300, 220], 0.8, amp=0.45))
    save("horse_voice.wav", tone([380, 300, 260], 0.7, amp=0.4))
    save("rooster_real.wav", tone([700, 850, 520], 0.85, amp=0.4))
    save("rooster_voice.wav", tone([650, 800, 700, 500], 0.9, amp=0.4))
    save("praise.wav", arpeggio([523, 659, 784, 1047]))
    save("nudge.wav", tone([180], 0.18, amp=0.18))
    print("generated", len(os.listdir(DST)), "files in", os.path.abspath(DST))


if __name__ == "__main__":
    main()
