#!/usr/bin/env python3
"""Генерирует базовые ассеты для карточки RuStore.

Иконка рисуется векторно (Pillow), поэтому её можно пересобрать в любой
момент и поправить цвета под финальный дизайн. Скриншоты приложения этот
скрипт не делает: их снимаем с реального устройства/эмулятора.
"""
import os

from PIL import Image, ImageCms, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "store", "assets")

INK = (76, 61, 53, 255)
CREAM = (255, 248, 238, 255)
SUNNY = (255, 209, 128, 255)
PEACH = (255, 204, 188, 255)
BROWN = (141, 110, 99, 255)
WHITE = (255, 255, 255, 255)
BLUSH = (255, 171, 145, 110)


def font(size: int) -> ImageFont.FreeTypeFont:
    # Arial Rounded MT не содержит кириллицы, поэтому для текста берём Arial.
    path = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
    return ImageFont.truetype(path, size)


def srgb_profile() -> bytes:
    """Профиль sRGB: без ICC валидаторы каталога иногда отвергают картинку молча."""
    return ImageCms.ImageCmsProfile(ImageCms.createProfile("sRGB")).tobytes()


def rounded_mask(size: int, radius: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, size - 1, size - 1), radius=radius, fill=255
    )
    return mask


def cow_art() -> Image.Image:
    """Корова без прозрачных полей вокруг: иначе в мелком размере она мелкая."""
    cow_path = os.path.join(
        ROOT, "app", "src", "main", "assets", "images", "cow_512.png"
    )
    art = Image.open(cow_path).convert("RGBA")
    return art.crop(art.getbbox())


def draw_icon(size: int = 512, zoom: float = 0.96, dy: float = 0.01) -> Image.Image:
    # Иконка использует ту же корову, что и карточки в приложении: так
    # магазин и игра выглядят единым целым. Прозрачные поля исходника
    # обрезаны, поэтому голова читается на превью 32–48 px; zoom < 1
    # оставляет поля по краям, zoom > 1 подрезает корову.
    image = Image.new("RGBA", (size, size), SUNNY)
    art = cow_art()
    width = round(size * zoom)
    height = round(art.height * width / art.width)
    art = art.resize((width, height), Image.LANCZOS)
    image.alpha_composite(
        art,
        ((size - art.width) // 2, (size - art.height) // 2 + round(size * dy)),
    )
    # RuStore не любит прозрачность в иконке: отдаём плотный RGB.
    return image.convert("RGB")


def draw_feature(width: int = 1024, height: int = 500) -> Image.Image:
    image = Image.new("RGBA", (width, height), CREAM)
    draw = ImageDraw.Draw(image)

    draw.ellipse((470, -180, 1120, 470), fill=SUNNY)
    draw.ellipse((-120, 330, 300, 750), fill=PEACH)

    icon = draw_icon(360)
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

    icon = draw_icon()
    icon_path = os.path.join(ASSETS, "icon-512.png")
    icon.save(icon_path, "PNG", optimize=True, icc_profile=srgb_profile())
    print(f"icon: {icon_path}")

    # JPEG-двойник: консоль RuStore показывает по иконке только «не прошло
    # валидацию» без деталей, поэтому держим под рукой второй формат из
    # списка допустимых. Pillow не пишет EXIF, а baseline-JPEG читают все.
    icon_jpg_path = os.path.join(ASSETS, "icon-512.jpg")
    icon.save(
        icon_jpg_path,
        "JPEG",
        quality=92,
        optimize=True,
        progressive=False,
        dpi=(72, 72),
        icc_profile=srgb_profile(),
    )
    print(f"icon (jpg): {icon_jpg_path}")

    feature_path = os.path.join(ASSETS, "feature-1024x500.png")
    draw_feature().save(feature_path, "PNG", optimize=True)
    print(f"feature: {feature_path}")


if __name__ == "__main__":
    main()
