#!/usr/bin/env python3
"""Рисует иллюстрации животных для assets/images.

Стиль совпадает с иконкой: плотный тёмный контур, пастельные заливки,
крупные глаза и мягкие блики. Все картинки квадратные 512×512 с прозрачным
фоном, поэтому их можно использовать и в песочнице, и в игре «Угадай».
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "images")

INK = (55, 71, 79, 255)
WHITE = (255, 255, 255, 255)
CREAM = (255, 248, 225, 255)
SUNNY = (255, 209, 128, 255)
ORANGE = (255, 183, 77, 255)
DEEP_ORANGE = (245, 124, 0, 255)
PEACH = (255, 204, 188, 255)
PINK = (244, 143, 177, 255)
BROWN = (141, 110, 99, 255)
DARK_BROWN = (93, 64, 55, 255)
BEIGE = (255, 224, 178, 255)
TAN = (255, 204, 128, 255)
GREEN = (129, 199, 132, 255)
DARK_GREEN = (67, 160, 71, 255)
RED = (229, 115, 115, 255)
YELLOW = (255, 213, 79, 255)
BLUSH = (255, 171, 145, 120)

SCALE = 4
SIZE = 512


class Canvas:
    """Холст 512×512 с координатами в «дизайнерских» пикселях."""

    def __init__(self) -> None:
        self.image = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.image)
        self.stroke = 7 * SCALE

    def box(self, left: float, top: float, right: float, bottom: float):
        return (
            int(left * SCALE),
            int(top * SCALE),
            int(right * SCALE),
            int(bottom * SCALE),
        )

    def ellipse(self, left, top, right, bottom, fill=None, outline=None, width=None):
        self.draw.ellipse(
            self.box(left, top, right, bottom),
            fill=fill,
            outline=outline,
            width=width if width is not None else self.stroke,
        )

    def polygon(self, points, fill=None, outline=None, width=None):
        scaled = [(int(x * SCALE), int(y * SCALE)) for x, y in points]
        self.draw.polygon(
            scaled,
            fill=fill,
            outline=outline,
            width=width if width is not None else self.stroke,
        )

    def line(self, points, fill=INK, width=5):
        scaled = [(int(x * SCALE), int(y * SCALE)) for x, y in points]
        self.draw.line(scaled, fill=fill, width=int(width * SCALE), joint="curve")

    def arc(self, left, top, right, bottom, start, end, fill=INK, width=5):
        self.draw.arc(
            self.box(left, top, right, bottom),
            start=start,
            end=end,
            fill=fill,
            width=int(width * SCALE),
        )

    def blush(self, left, top, right, bottom):
        layer = Image.new("RGBA", self.image.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).ellipse(
            self.box(left, top, right, bottom), fill=BLUSH
        )
        self.image.alpha_composite(layer)

    def clip(self, left, top, right, bottom, draw_fn) -> None:
        """Рисует деталь только внутри эллипса (пятна, маски)."""
        mask = Image.new("L", self.image.size, 0)
        ImageDraw.Draw(mask).ellipse(self.box(left, top, right, bottom), fill=255)
        layer = Image.new("RGBA", self.image.size, (0, 0, 0, 0))
        draw_fn(ImageDraw.Draw(layer), layer)
        clipped = Image.composite(
            layer, Image.new("RGBA", self.image.size, (0, 0, 0, 0)), mask
        )
        self.image.alpha_composite(clipped)

    def eye(self, cx, cy, radius=18):
        self.ellipse(cx - radius, cy - radius, cx + radius, cy + radius, fill=INK)
        self.ellipse(
            cx - radius // 3,
            cy - radius // 2,
            cx + radius // 4,
            cy,
            fill=WHITE,
            outline=None,
        )

    def save(self, name: str) -> str:
        # Нормализуем масштаб: у разных зверей разный силуэт, но на карточке
        # они должны выглядеть одинаково крупными. Обрезаем прозрачные поля,
        # вписываем рисунок в 448×448 и центрируем на холсте 512×512.
        bbox = self.image.getbbox()
        art = self.image.crop(bbox)
        ratio = min(448 * SCALE / art.width, 448 * SCALE / art.height)
        art = art.resize(
            (max(1, int(art.width * ratio)), max(1, int(art.height * ratio))),
            Image.LANCZOS,
        )
        canvas = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
        canvas.alpha_composite(
            art,
            ((canvas.width - art.width) // 2, (canvas.height - art.height) // 2),
        )
        path = os.path.join(OUT, f"{name}_512.png")
        canvas.resize((SIZE, SIZE), Image.LANCZOS).save(path, "PNG", optimize=True)
        return path


def cow() -> Canvas:
    c = Canvas()
    for left in (48, 364):
        c.ellipse(left, 176, left + 96, 292, fill=WHITE, outline=INK)
        c.ellipse(left + 22, 198, left + 74, 264, fill=PEACH, outline=None)

    c.ellipse(108, 112, 404, 438, fill=WHITE, outline=INK)

    def patches(layer: ImageDraw.ImageDraw, _layer: Image.Image) -> None:
        s = SCALE
        layer.ellipse(
            (146 * s, 146 * s, 236 * s, 214 * s), fill=BROWN
        )
        layer.ellipse(
            (304 * s, 204 * s, 386 * s, 266 * s), fill=BROWN
        )

    c.clip(108, 112, 404, 438, patches)

    c.blush(136, 290, 204, 336)
    c.blush(308, 290, 376, 336)

    c.eye(204, 254)
    c.eye(308, 254)

    c.ellipse(172, 306, 340, 414, fill=PEACH, outline=INK)
    c.ellipse(222, 338, 246, 362, fill=INK, outline=None)
    c.ellipse(266, 338, 290, 362, fill=INK, outline=None)
    c.arc(232, 352, 280, 392, 20, 160)

    c.arc(236, 86, 276, 126, 200, 340)
    c.arc(252, 80, 292, 120, 200, 340)
    return c


def cat() -> Canvas:
    c = Canvas()

    for left, mirror in ((78, False), (324, True)):
        points = [
            (left, 238),
            (left + (36 if not mirror else 36), 62),
            (left + 128, 166),
        ]
        c.polygon(points, fill=ORANGE, outline=INK)
        inner = [
            (left + 30, 214),
            (left + 52, 106),
            (left + 106, 176),
        ]
        c.polygon(inner, fill=PINK, outline=None)

    c.ellipse(92, 126, 420, 446, fill=ORANGE, outline=INK)

    def stripes(layer: ImageDraw.ImageDraw, _layer: Image.Image) -> None:
        s = SCALE
        for x1, y1, x2, y2 in (
            (222, 135, 214, 210),
            (266, 132, 268, 205),
            (308, 142, 318, 208),
        ):
            layer.line((x1 * s, y1 * s, x2 * s, y2 * s), fill=DEEP_ORANGE, width=14 * s)

    c.clip(92, 126, 420, 446, stripes)

    c.blush(122, 310, 190, 354)
    c.blush(322, 310, 390, 354)

    c.eye(190, 272)
    c.eye(322, 272)

    # Носик-сердечко и улыбка.
    c.polygon([(238, 314), (274, 314), (256, 336)], fill=PINK, outline=INK, width=4)
    c.arc(218, 326, 256, 362, 0, 90, width=5)
    c.arc(256, 326, 294, 362, 90, 180, width=5)
    c.line([(218, 344), (198, 334)], width=4)
    c.line([(294, 344), (314, 334)], width=4)

    # Усы.
    for y, dx in ((320, 1), (344, 2), (368, 3)):
        c.line([(150, y), (54, y - 10 * dx)], width=4)
        c.line([(362, y), (458, y - 10 * dx)], width=4)

    # Хохолок.
    c.line([(232, 146), (238, 118)], width=4)
    c.line([(256, 142), (256, 110)], width=4)
    c.line([(280, 146), (274, 118)], width=4)
    return c


def dog() -> Canvas:
    c = Canvas()

    # Висячие уши.
    c.ellipse(48, 150, 186, 382, fill=BROWN, outline=INK)
    c.ellipse(326, 150, 464, 382, fill=BROWN, outline=INK)
    c.ellipse(74, 190, 154, 344, fill=(109, 76, 65, 255), outline=None)

    c.ellipse(112, 116, 400, 424, fill=BEIGE, outline=INK)

    def patch(layer: ImageDraw.ImageDraw, _layer: Image.Image) -> None:
        s = SCALE
        layer.ellipse((286 * s, 186 * s, 392 * s, 288 * s), fill=BROWN)

    c.clip(112, 116, 400, 424, patch)

    c.blush(136, 314, 200, 358)
    c.blush(312, 314, 376, 358)

    c.eye(198, 264)
    c.eye(310, 264)

    c.ellipse(190, 296, 322, 410, fill=CREAM, outline=None)
    c.ellipse(228, 310, 284, 356, fill=DARK_BROWN, outline=None)
    c.ellipse(238, 318, 252, 330, fill=WHITE, outline=None, width=0)

    # Улыбка и язык.
    c.arc(216, 340, 256, 382, 0, 110, width=5)
    c.arc(256, 340, 296, 382, 70, 180, width=5)
    c.ellipse(240, 372, 276, 412, fill=PINK, outline=INK, width=4)
    c.line([(258, 380), (258, 404)], width=3)
    return c


def duck() -> Canvas:
    c = Canvas()

    # Лапки.
    for x in (196, 268):
        c.line([(x, 440), (x, 478)], fill=DEEP_ORANGE, width=10)
        c.line([(x, 478), (x - 26, 492)], fill=DEEP_ORANGE, width=8)
        c.line([(x, 478), (x + 26, 492)], fill=DEEP_ORANGE, width=8)

    # Туловище и крыло.
    c.ellipse(112, 272, 400, 470, fill=BROWN, outline=INK)
    c.ellipse(142, 310, 286, 426, fill=(109, 76, 65, 255), outline=INK)
    c.arc(160, 330, 270, 400, 200, 340, fill=BEIGE, width=4)

    # Голова.
    c.ellipse(166, 74, 346, 304, fill=GREEN, outline=INK)
    c.arc(232, 86, 252, 118, 210, 330, width=4)
    c.arc(268, 86, 288, 118, 210, 330, width=4)

    c.eye(214, 186)
    c.eye(298, 186)
    c.blush(180, 224, 226, 258)
    c.blush(286, 224, 332, 258)

    # Клюв.
    c.polygon(
        [(214, 238), (298, 238), (256, 302)],
        fill=YELLOW,
        outline=INK,
    )
    c.ellipse(244, 252, 252, 260, fill=ORANGE, outline=None, width=0)
    c.ellipse(260, 252, 268, 260, fill=ORANGE, outline=None, width=0)
    return c


def horse() -> Canvas:
    c = Canvas()

    # Уши.
    c.polygon([(148, 238), (140, 92), (216, 156)], fill=TAN, outline=INK)
    c.polygon([(364, 238), (372, 92), (296, 156)], fill=TAN, outline=INK)
    c.polygon([(166, 216), (162, 126), (204, 162)], fill=PEACH, outline=None)
    c.polygon([(346, 216), (350, 126), (308, 162)], fill=PEACH, outline=None)

    # Голова.
    c.ellipse(136, 118, 376, 424, fill=TAN, outline=INK)

    # Грива и чёлка.
    def mane(layer: ImageDraw.ImageDraw, _layer: Image.Image) -> None:
        s = SCALE
        layer.ellipse((128 * s, 96 * s, 244 * s, 196 * s), fill=DARK_BROWN)
        layer.ellipse((212 * s, 82 * s, 330 * s, 188 * s), fill=DARK_BROWN)
        layer.ellipse((292 * s, 108 * s, 390 * s, 208 * s), fill=DARK_BROWN)

    c.clip(136, 118, 376, 424, mane)
    c.ellipse(206, 108, 310, 178, fill=DARK_BROWN, outline=None)

    c.blush(156, 318, 214, 358)
    c.blush(298, 318, 356, 358)

    c.eye(202, 256)
    c.eye(310, 256)

    # Морда.
    c.ellipse(194, 302, 318, 440, fill=BEIGE, outline=INK)
    c.ellipse(222, 344, 242, 364, fill=DARK_BROWN, outline=None, width=0)
    c.ellipse(270, 344, 290, 364, fill=DARK_BROWN, outline=None, width=0)
    c.arc(230, 362, 282, 404, 20, 160)

    return c


def rooster() -> Canvas:
    c = Canvas()

    # Хвост.
    c.polygon(
        [(338, 340), (418, 118), (470, 186), (420, 322)],
        fill=DARK_GREEN,
        outline=INK,
    )
    c.polygon(
        [(330, 336), (440, 190), (486, 274), (410, 356)],
        fill=GREEN,
        outline=INK,
    )
    c.polygon(
        [(330, 330), (392, 244), (444, 340), (376, 382)],
        fill=(46, 125, 50, 255),
        outline=INK,
    )

    # Лапки.
    for x in (206, 286):
        c.line([(x, 440), (x, 478)], fill=DEEP_ORANGE, width=10)
        c.line([(x, 478), (x - 28, 490)], fill=DEEP_ORANGE, width=8)
        c.line([(x, 478), (x + 28, 490)], fill=DEEP_ORANGE, width=8)

    # Туловище и крыло.
    c.ellipse(118, 252, 392, 478, fill=CREAM, outline=INK)
    c.ellipse(148, 300, 300, 422, fill=BEIGE, outline=INK)
    c.arc(168, 326, 282, 400, 200, 340, fill=BROWN, width=4)

    # Голова.
    c.ellipse(168, 80, 332, 276, fill=CREAM, outline=INK)

    # Гребешок.
    for left, top in ((186, 48), (224, 30), (262, 30), (300, 52)):
        c.ellipse(left, top, left + 52, top + 66, fill=RED, outline=INK, width=5)

    c.eye(214, 164)
    c.eye(288, 164)
    c.blush(178, 206, 218, 236)
    c.blush(284, 206, 324, 236)

    # Клюв и серёжка.
    c.polygon(
        [(214, 198), (286, 198), (250, 244)],
        fill=YELLOW,
        outline=INK,
    )
    c.ellipse(240, 238, 264, 292, fill=RED, outline=INK, width=4)
    return c


def main() -> None:
    os.makedirs(OUT, exist_ok=True)
    for name, factory in (
        ("cow", cow),
        ("cat", cat),
        ("dog", dog),
        ("duck", duck),
        ("horse", horse),
        ("rooster", rooster),
    ):
        print(f"{name}: {factory().save(name)}")


if __name__ == "__main__":
    main()
