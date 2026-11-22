#!/usr/bin/env python3
"""Генерирует базовые ассеты для карточки RuStore.

Иконка рисуется векторно (Pillow), поэтому её можно пересобрать в любой
момент и поправить цвета под финальный дизайн. Скриншоты приложения этот
скрипт не делает: их снимаем с реального устройства/эмулятора.
"""
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "store", "assets")

INK = (55, 71, 79, 255)
CREAM = (245, 246, 250, 255)
SUNNY = (255, 209, 128, 255)
PEACH = (255, 204, 188, 255)
BROWN = (141, 110, 99, 255)
WHITE = (255, 255, 255, 255)
BLUSH = (255, 171, 145, 110)


def font(size: int) -> ImageFont.FreeTypeFont:
    # Arial Rounded MT не содержит кириллицы, поэтому для текста берём Arial.
    path = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
    return ImageFont.truetype(path, size)


def rounded_mask(size: int, radius: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, size - 1, size - 1), radius=radius, fill=255
    )
    return mask


def draw_icon(size: int = 512, scale: int = 4) -> Image.Image:
    # Рисуем всегда в системе координат 512×512, затем уменьшаем до нужного
    # размера: так иконка не обрезается при вставке в баннер.
    s = 512 * scale
    image = Image.new("RGBA", (s, s), SUNNY)
    draw = ImageDraw.Draw(image)

    def px(value: float) -> int:
        return int(value * scale)

    def box(left, top, right, bottom):
        return (px(left), px(top), px(right), px(bottom))

    width = max(1, px(7))

    # Уши: внешняя часть и нежный розовый внутри.
    for left in (56, 356):
        draw.ellipse(box(left, 168, left + 100, 288), fill=WHITE, outline=INK, width=width)
        draw.ellipse(
            box(left + 22, 192, left + 78, 262), fill=PEACH, outline=None
        )

    # Морда.
    draw.ellipse(box(108, 112, 404, 438), fill=WHITE, outline=INK, width=width)

    # Пятна.
    draw.ellipse(box(146, 146, 226, 212), fill=BROWN)
    draw.ellipse(box(312, 206, 382, 264), fill=BROWN)

    # Щёки.
    blush = Image.new("RGBA", image.size, (0, 0, 0, 0))
    blush_draw = ImageDraw.Draw(blush)
    blush_draw.ellipse(box(136, 290, 204, 336), fill=BLUSH)
    blush_draw.ellipse(box(308, 290, 376, 336), fill=BLUSH)
    image.alpha_composite(blush)

    # Глаза с бликами.
    for cx in (204, 308):
        draw.ellipse(box(cx - 18, 236, cx + 18, 272), fill=INK)
        draw.ellipse(box(cx - 6, 242, cx + 5, 253), fill=WHITE)

    # Нос-морда и ноздри.
    draw.ellipse(box(172, 306, 340, 414), fill=PEACH, outline=INK, width=width)
    draw.ellipse(box(222, 338, 246, 362), fill=INK)
    draw.ellipse(box(266, 338, 290, 362), fill=INK)
    draw.arc(box(232, 352, 280, 392), start=20, end=160, fill=INK, width=px(5))

    # Хохолок.
    draw.arc(box(236, 86, 276, 126), start=200, end=340, fill=INK, width=px(6))
    draw.arc(box(252, 80, 292, 120), start=200, end=340, fill=INK, width=px(6))

    return image.resize((size, size), Image.LANCZOS)


def draw_feature(width: int = 1024, height: int = 500) -> Image.Image:
    image = Image.new("RGBA", (width, height), CREAM)
    draw = ImageDraw.Draw(image)

    draw.ellipse((470, -180, 1120, 470), fill=SUNNY)
    draw.ellipse((-120, 330, 300, 750), fill=PEACH)

    icon = draw_icon(360, scale=3)
    icon_layer = Image.new("RGBA", (360, 360), (0, 0, 0, 0))
    icon_layer.paste(icon, (0, 0), rounded_mask(360, 84))
    image.alpha_composite(icon_layer, (88, 70))

    title_font = font(54)
    subtitle_font = font(28)
    small_font = font(23)

    draw.text((500, 138), "Кто как говорит?", font=title_font, fill=INK)
    draw.text((500, 216), "Звуки животных и запуск речи", font=subtitle_font, fill=INK)
    draw.text((500, 272), "0+  •  офлайн  •  без рекламы", font=small_font, fill=INK)

    return image.convert("RGB")


def main() -> None:
    os.makedirs(ASSETS, exist_ok=True)

    icon_path = os.path.join(ASSETS, "icon-512.png")
    draw_icon().save(icon_path, "PNG", optimize=True)
    print(f"icon: {icon_path}")

    feature_path = os.path.join(ASSETS, "feature-1024x500.png")
    draw_feature().save(feature_path, "PNG", optimize=True)
    print(f"feature: {feature_path}")


if __name__ == "__main__":
    main()
