#!/usr/bin/env python3
"""Рисует иллюстрации животных для assets/images.

Стиль: мягкая пастельная «плюшевая» графика с тёплым контуром, крупными
глазами, аккуратными тенями и лёгким кремовым ореолом. Все картинки
квадратные 1024×1024 с прозрачным фоном. Рисуем в 4× и уменьшаем — так контуры
остаются гладкими.
"""
import math
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "images")

INK = (88, 72, 64, 255)
INK_SOFT = (128, 111, 100, 255)
WHITE = (255, 255, 255, 255)
CREAM = (255, 248, 225, 255)
SUNNY = (255, 209, 128, 255)
YELLOW = (255, 213, 79, 255)
ORANGE = (255, 183, 77, 255)
DEEP_ORANGE = (245, 124, 0, 255)
PEACH = (255, 204, 188, 255)
PINK = (244, 143, 177, 255)
ROSE = (239, 154, 154, 255)
RED = (229, 115, 115, 255)
BROWN = (141, 110, 99, 255)
DARK_BROWN = (93, 64, 55, 255)
BEIGE = (255, 224, 178, 255)
TAN = (255, 204, 128, 255)
GREEN = (129, 199, 132, 255)
DARK_GREEN = (67, 160, 71, 255)
TEAL = (77, 182, 172, 255)
BLUSH = (255, 150, 136, 95)

COW_BROWN = (166, 124, 82, 255)
CAT_ORANGE = (247, 166, 76, 255)
CAT_STRIPE = (224, 122, 45, 255)
DOG_BEIGE = (247, 218, 170, 255)
DOG_BROWN = (146, 104, 74, 255)
DUCK_GREEN = (124, 179, 66, 255)
DUCK_BODY = (146, 112, 96, 255)
DUCK_CHEST = (190, 128, 98, 255)
DUCK_WING = (178, 140, 120, 255)
DUCK_WING_DARK = (103, 76, 64, 255)
DUCK_SPECULUM = (79, 139, 205, 255)
HORSE_TAN = (239, 190, 125, 255)
MANE_BROWN = (93, 64, 55, 255)
ROOSTER_CREAM = (255, 241, 210, 255)
SHEEP_WOOL = (250, 247, 240, 255)
SHEEP_FACE = (232, 210, 184, 255)
PIG_PINK = (247, 181, 178, 255)
PIG_DARK = (232, 145, 148, 255)
GOAT_CREAM = (250, 235, 210, 255)
HORN = (192, 175, 168, 255)
FROG_GREEN = (129, 199, 132, 255)
FROG_DARK = (85, 160, 90, 255)
FROG_BELLY = (220, 237, 200, 255)
GOOSE_WHITE = (255, 250, 238, 255)
GOOSE_SHADE = (226, 226, 216, 255)
GOOSE_WING = (242, 238, 226, 255)
DONKEY_GRAY = (188, 194, 198, 255)
DONKEY_LIGHT = (234, 234, 230, 255)
DONKEY_MANE = (92, 116, 126, 255)
OWL_BROWN = (166, 140, 128, 255)
OWL_CREAM = (244, 231, 208, 255)
OWL_DARK = (116, 82, 68, 255)
MOUSE_GRAY = (200, 195, 190, 255)
MOUSE_LIGHT = (238, 232, 224, 255)
MOUSE_PINK = (244, 154, 181, 255)
BEE_YELLOW = (255, 213, 79, 255)
BEE_DARK = (93, 64, 55, 255)
WING = (232, 245, 255, 135)
TURKEY_BROWN = (141, 110, 99, 255)
TURKEY_DARK = (93, 64, 55, 255)
TURKEY_TAN = (196, 155, 112, 255)
CHICKEN_CREAM = (255, 244, 219, 255)
CHICKEN_WING = (244, 214, 165, 255)
CHICKEN_FEATHER = (218, 182, 137, 255)
CHICKEN_RED = (224, 108, 102, 255)
WOLF_GRAY = (170, 180, 186, 255)
WOLF_LIGHT = (232, 236, 238, 255)
WOLF_DARK = (118, 132, 141, 255)
BEAR_FUR = (170, 124, 88, 255)
BEAR_LIGHT = (220, 184, 142, 255)
BEAR_DARK = (130, 92, 66, 255)
CROW_BLUE = (82, 94, 110, 255)
CROW_DARK = (64, 74, 88, 255)
CROW_GLOSS = (104, 130, 154, 255)
CUCKOO_GRAY = (168, 180, 190, 255)
CUCKOO_DARK = (118, 132, 144, 255)
CUCKOO_LIGHT = (242, 242, 234, 255)
ELEPHANT_GRAY = (172, 181, 190, 255)
ELEPHANT_LIGHT = (216, 223, 229, 255)
ELEPHANT_DARK = (136, 148, 159, 255)

SCALE = 4
SIZE = 1024
# Координаты ниже нарисованы в сетке 512: это «дизайнерские» пиксели.
# Холст и итоговый PNG крупнее в [SIZE] / [DESIGN_SIZE] раз — так иллюстрация
# остаётся резкой в крупном плане на экранах с высокой плотностью.
DESIGN_SIZE = 512
STROKE = 6


def scaled(value):
    return int(round(value * SCALE))


class Canvas:
    """Холст 512×512 в «дизайнерских» пикселях, рисуется в [SCALE] раз крупнее."""

    def __init__(self):
        self.image = Image.new(
            "RGBA", (DESIGN_SIZE * SCALE, DESIGN_SIZE * SCALE), (0, 0, 0, 0)
        )
        self.draw = ImageDraw.Draw(self.image)
        self.stroke = STROKE * SCALE

    def box(self, left, top, right, bottom):
        return (
            scaled(left),
            scaled(top),
            scaled(right),
            scaled(bottom),
        )

    def ellipse(self, left, top, right, bottom, fill=None, outline=INK, width=None):
        self.draw.ellipse(
            self.box(left, top, right, bottom),
            fill=fill,
            outline=outline,
            width=width if width is not None else self.stroke,
        )

    def arc(self, left, top, right, bottom, start, end, fill=INK, width=5):
        self.draw.arc(
            self.box(left, top, right, bottom),
            start=start,
            end=end,
            fill=fill,
            width=scaled(width),
        )

    def line(self, points, fill=INK, width=5, joint="curve"):
        self.draw.line(
            [(scaled(x), scaled(y)) for x, y in points],
            fill=fill,
            width=scaled(width),
            joint=joint,
        )

    def polygon(self, points, fill=None, outline=INK, width=None):
        scaled_points = [(scaled(x), scaled(y)) for x, y in points]
        self.draw.polygon(
            scaled_points,
            fill=fill,
            outline=outline,
            width=width if width is not None else self.stroke,
        )

    def blob(self, points, fill, outline=INK, width=None, closed=True):
        smooth = smooth_points(points, closed=closed)
        scaled_points = [(scaled(x), scaled(y)) for x, y in smooth]
        self.draw.polygon(scaled_points, fill=fill)
        if outline is not None:
            path = scaled_points + ([scaled_points[0]] if closed else [])
            self.draw.line(
                path,
                fill=outline,
                width=width if width is not None else self.stroke,
                joint="curve",
            )
        return smooth

    def shape_mask_ellipse(self, left, top, right, bottom):
        mask = Image.new("L", self.image.size, 0)
        ImageDraw.Draw(mask).ellipse(
            self.box(left, top, right, bottom), fill=255
        )
        return mask

    def shape_mask_blob(self, points, closed=True):
        smooth = smooth_points(points, closed=closed)
        mask = Image.new("L", self.image.size, 0)
        ImageDraw.Draw(mask).polygon(
            [(scaled(x), scaled(y)) for x, y in smooth], fill=255
        )
        return mask

    @staticmethod
    def inset(mask, amount=2):
        size = max(3, scaled(amount) | 1)
        return mask.filter(ImageFilter.MinFilter(size))

    def clip(self, mask, draw_fn, blur=0):
        layer = Image.new("RGBA", self.image.size, (0, 0, 0, 0))
        draw_fn(ImageDraw.Draw(layer), layer)
        if blur:
            layer = layer.filter(ImageFilter.GaussianBlur(scaled(blur)))
        alpha = ImageChops.multiply(layer.getchannel("A"), mask)
        layer.putalpha(alpha)
        self.image.alpha_composite(layer)


    def shade(self, mask, box, color=(92, 70, 56, 36), blur=16):
        layer = Image.new("RGBA", self.image.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).ellipse(self.box(*box), fill=color)
        layer = layer.filter(ImageFilter.GaussianBlur(scaled(blur)))
        layer.putalpha(ImageChops.multiply(layer.getchannel("A"), mask))
        self.image.alpha_composite(layer)

    def highlight(self, mask, box, color=(255, 255, 255, 80), blur=12):
        layer = Image.new("RGBA", self.image.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).ellipse(self.box(*box), fill=color)
        layer = layer.filter(ImageFilter.GaussianBlur(scaled(blur)))
        layer.putalpha(ImageChops.multiply(layer.getchannel("A"), mask))
        self.image.alpha_composite(layer)

    def ground_shadow(self, left, top, right, bottom, opacity=40):
        layer = Image.new("RGBA", self.image.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).ellipse(
            self.box(left, top, right, bottom), fill=(94, 78, 68, opacity)
        )
        layer = layer.filter(ImageFilter.GaussianBlur(scaled(10)))
        self.image.alpha_composite(layer)

    def blush(self, left, top, right, bottom, opacity=105):
        layer = Image.new("RGBA", self.image.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).ellipse(
            self.box(left, top, right, bottom),
            fill=(255, 148, 132, opacity),
        )
        layer = layer.filter(ImageFilter.GaussianBlur(scaled(8)))
        self.image.alpha_composite(layer)

    def warm_halo(self):
        """Мягкий кремовый ореол: карточки перестают выглядеть вырезанными
        на белом и становятся похожи на тёплые наклейки."""
        layer = Image.new("RGBA", self.image.size, (0, 0, 0, 0))
        draw = ImageDraw.Draw(layer)
        for inset, color in (
            (0, (255, 238, 218, 50)),
            (32, (255, 244, 228, 62)),
            (66, (255, 249, 238, 58)),
        ):
            draw.ellipse(
                self.box(54 + inset, 62 + inset, 458 - inset, 466 - inset),
                fill=color,
            )
        return layer.filter(ImageFilter.GaussianBlur(scaled(16)))

    def eye(self, cx, cy, rx=20, ry=24, lash=False, sparkle=True):
        self.ellipse(
            cx - rx, cy - ry, cx + rx, cy + ry, fill=INK, outline=None, width=0
        )
        if sparkle:
            self.ellipse(
                cx - rx * 0.55,
                cy - ry * 0.62,
                cx - rx * 0.05,
                cy - ry * 0.06,
                fill=WHITE,
                outline=None,
                width=0,
            )
            self.ellipse(
                cx + rx * 0.25,
                cy + ry * 0.18,
                cx + rx * 0.55,
                cy + ry * 0.48,
                fill=(255, 255, 255, 160),
                outline=None,
                width=0,
            )
        if lash:
            self.line(
                [(cx - rx - 12, cy - ry + 6), (cx - rx - 2, cy - ry - 4)],
                width=3,
            )
            self.line(
                [(cx - rx - 7, cy - ry + 16), (cx - rx + 4, cy - ry + 8)],
                width=3,
            )
            self.line(
                [(cx + rx + 12, cy - ry + 6), (cx + rx + 2, cy - ry - 4)],
                width=3,
            )
            self.line(
                [(cx + rx + 7, cy - ry + 16), (cx + rx - 8, cy - ry + 8)],
                width=3,
            )

    def save(self, name):
        # Обрезаем прозрачные поля и вписываем рисунок в 448×448, чтобы
        # все животные на карточках выглядели одинаково крупными.
        bbox = self.image.getbbox()
        art = self.image.crop(bbox)
        ratio = min(448 * SCALE / art.width, 448 * SCALE / art.height)
        art = art.resize(
            (max(1, int(art.width * ratio)), max(1, int(art.height * ratio))),
            Image.LANCZOS,
        )
        canvas = Image.new("RGBA", (DESIGN_SIZE * SCALE, DESIGN_SIZE * SCALE), (0, 0, 0, 0))
        canvas.alpha_composite(
            art,
            ((canvas.width - art.width) // 2, (canvas.height - art.height) // 2),
        )
        # Ореол кладём под рисунок, чтобы он не пачкал контуры и тени.
        final = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
        final.alpha_composite(self.warm_halo())
        final.alpha_composite(canvas)
        path = os.path.join(OUT, f"{name}_{SIZE}.png")
        final.resize((SIZE, SIZE), Image.LANCZOS).save(path, "PNG", optimize=True)
        return path



def smooth_points(points, closed=True, samples=14):
    """Сглаживает контур Catmull-Rom-сплайном."""
    if len(points) < 3:
        return points
    if closed:
        padded = [points[-1]] + list(points) + [points[0], points[1]]
    else:
        padded = [points[0]] + list(points) + [points[-1]]
    result = []
    for index in range(1, len(padded) - 2):
        p0, p1, p2, p3 = padded[index - 1 : index + 3]
        for step in range(samples):
            t = step / samples
            t2 = t * t
            t3 = t2 * t
            x = 0.5 * (
                (2 * p1[0])
                + (-p0[0] + p2[0]) * t
                + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2
                + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3
            )
            y = 0.5 * (
                (2 * p1[1])
                + (-p0[1] + p2[1]) * t
                + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2
                + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3
            )
            result.append((x, y))
    if not closed:
        result.append(points[-1])
    return result


def tuft(c, cx, cy, count=3, width=6, height=18, color=INK, spread=14):
    for index in range(count):
        x = cx + (index - (count - 1) / 2) * spread
        c.line(
            [(x, cy), (x + (index - 1) * 4, cy - height)],
            fill=color,
            width=width,
        )


def contact_sheet(paths, out_path):
    cols = 4
    cell = 256
    rows = math.ceil(len(paths) / cols)
    sheet = Image.new("RGB", (cols * cell, rows * cell), (245, 246, 250))
    draw = ImageDraw.Draw(sheet)
    for index, path in enumerate(paths):
        image = Image.open(path).convert("RGBA")
        image.thumbnail((cell - 16, cell - 16), Image.LANCZOS)
        x = (index % cols) * cell
        y = (index // cols) * cell
        sheet.paste(image, (x + (cell - image.width) // 2, y + 8), image)
        draw.text((x + 8, y + cell - 18), os.path.basename(path), fill=INK)
    sheet.save(out_path)
    print(f"contact sheet: {out_path}")



def cow():
    c = Canvas()
    c.ground_shadow(132, 438, 380, 488, opacity=30)

    body = (116, 342, 400, 512)
    c.ellipse(*body, fill=WHITE)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.clip(
        body_mask,
        lambda d, _layer: d.ellipse(c.box(118, 398, 236, 470), fill=COW_BROWN),
    )
    c.shade(body_mask, (118, 410, 392, 516), (30, 40, 50, 45))

    left_ear = [(106, 246), (70, 218), (62, 188), (92, 174), (134, 190), (164, 226), (150, 264)]
    right_ear = [(406, 246), (442, 218), (450, 188), (420, 174), (378, 190), (348, 226), (362, 264)]
    for ear, inner in (
        (left_ear, [(96, 226), (78, 204), (86, 190), (112, 200), (134, 220), (122, 240)]),
        (right_ear, [(416, 226), (434, 204), (426, 190), (400, 200), (378, 220), (390, 240)]),
    ):
        c.blob(ear, WHITE)
        c.blob(inner, PEACH, outline=None)

    # Рожки.
    c.blob([(158, 140), (142, 108), (152, 86), (176, 82), (190, 104), (184, 135)], CREAM)
    c.blob([(354, 140), (370, 108), (360, 86), (336, 82), (322, 104), (328, 135)], CREAM)

    head = (108, 104, 404, 430)
    c.ellipse(*head, fill=WHITE, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)

    def cow_patches(d, _layer):
        d.ellipse(c.box(140, 142, 252, 226), fill=COW_BROWN)
        d.ellipse(c.box(302, 206, 398, 286), fill=COW_BROWN)
        d.ellipse(c.box(126, 306, 194, 356), fill=(205, 168, 132, 255))

    c.clip(head_mask, cow_patches)
    c.shade(head_mask, (122, 260, 404, 438), (30, 40, 50, 38))
    c.highlight(head_mask, (154, 126, 300, 220), (255, 255, 255, 90))

    # Чубчик.
    for x, tilt in ((232, -4), (256, 0), (280, 4)):
        c.arc(x - 18, 102 + tilt, x + 18, 146 + tilt, 200, 340, width=4)

    c.blush(128, 302, 196, 348)
    c.blush(316, 302, 384, 348)
    c.eye(198, 258, lash=True)
    c.eye(314, 258, lash=True)

    # Морда.
    muzzle = (176, 306, 336, 420)
    c.ellipse(*muzzle, fill=PEACH, outline=INK, width=6)
    muzzle_mask = c.inset(c.shape_mask_ellipse(*muzzle), 3)
    c.highlight(muzzle_mask, (190, 318, 268, 358), (255, 255, 255, 95))
    c.shade(muzzle_mask, (204, 372, 330, 430), (120, 70, 60, 38))
    c.ellipse(218, 342, 240, 364, fill=INK, outline=None, width=0)
    c.ellipse(272, 342, 294, 364, fill=INK, outline=None, width=0)
    c.arc(228, 356, 284, 398, 20, 160, width=5)
    c.ellipse(246, 390, 266, 408, fill=ROSE, outline=None, width=0)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c



def cat():
    c = Canvas()
    c.ground_shadow(128, 438, 384, 492, opacity=28)

    # Хвост.
    tail = [(368, 438), (430, 454), (468, 410), (456, 360), (428, 346), (414, 370), (438, 394), (430, 420), (390, 424)]
    c.blob(tail, CAT_ORANGE)
    tail_mask = c.inset(c.shape_mask_blob(tail), 2)
    for x, y in ((438, 368), (452, 388), (430, 414)):
        c.clip(tail_mask, lambda d, _layer, x=x, y=y: d.arc(
            c.box(x - 18, y - 8, x + 18, y + 24), 170, 350, fill=CAT_STRIPE, width=scaled(5)
        ))

    body = (124, 342, 388, 506)
    c.ellipse(*body, fill=CAT_ORANGE)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (132, 430, 380, 512), (30, 40, 50, 38))
    c.ellipse(190, 386, 322, 512, fill=CREAM, outline=None, width=0)
    for left in (154, 286):
        c.ellipse(left, 448, left + 72, 506, fill=CREAM, outline=INK, width=4)
        c.line([(left + 24, 476), (left + 24, 494)], width=3)
        c.line([(left + 44, 472), (left + 44, 494)], width=3)

    # Уши.
    left_ear = [(96, 232), (118, 70), (222, 158), (218, 222)]
    right_ear = [(416, 232), (394, 70), (290, 158), (294, 222)]
    for ear, inner in (
        (left_ear, [(122, 208), (132, 108), (196, 166), (190, 204)]),
        (right_ear, [(390, 208), (380, 108), (316, 166), (322, 204)]),
    ):
        c.blob(ear, CAT_ORANGE)
        c.blob(inner, PINK, outline=None)
        c.shade(c.inset(c.shape_mask_blob(inner), 2), (120, 160, 230, 240), (120, 50, 60, 38))

    head = (112, 126, 400, 428)
    c.ellipse(*head, fill=CAT_ORANGE, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)

    def stripes(d, _layer):
        for x, top, bottom in ((226, 136, 214), (270, 132, 212), (314, 142, 216)):
            d.polygon(
                [
                    (scaled(x - 10), scaled(top)),
                    (scaled(x + 10), scaled(top)),
                    (scaled(x + 4), scaled(bottom)),
                    (scaled(x - 4), scaled(bottom)),
                ],
                fill=CAT_STRIPE,
            )
        for y, side in ((324, -1), (352, -1), (324, 1), (352, 1)):
            base = 134 if side < 0 else 378
            d.polygon(
                [
                    (scaled(base - 22 * side), scaled(y - 8)),
                    (scaled(base + 22 * side), scaled(y - 4)),
                    (scaled(base + 18 * side), scaled(y + 4)),
                    (scaled(base - 24 * side), scaled(y + 4)),
                ],
                fill=CAT_STRIPE,
            )

    c.clip(head_mask, stripes)
    c.shade(head_mask, (182, 288, 420, 440), (30, 40, 50, 35))
    c.highlight(head_mask, (152, 150, 310, 246), (255, 255, 255, 95))

    c.blush(126, 306, 194, 350)
    c.blush(318, 306, 386, 350)
    c.eye(198, 264)
    c.eye(314, 264)

    # Нос, улыбка и усы.
    c.polygon([(238, 306), (274, 306), (256, 330)], fill=PINK, outline=INK, width=4)
    c.arc(214, 318, 256, 356, 0, 90, width=5)
    c.arc(256, 318, 298, 356, 90, 180, width=5)
    c.line([(216, 338), (196, 328)], width=4)
    c.line([(296, 338), (316, 328)], width=4)
    for y, dx in ((310, 1), (334, 2), (358, 3)):
        c.line([(148, y), (50, y - 12 * dx)], width=4)
        c.line([(364, y), (462, y - 12 * dx)], width=4)
    for x, y in ((232, 142), (256, 136), (280, 142)):
        c.line([(x, y), (x + (x - 256) // 20, y - 26)], width=4)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c



def dog():
    c = Canvas()
    c.ground_shadow(120, 438, 392, 494, opacity=28)

    body = (118, 334, 394, 510)
    c.ellipse(*body, fill=DOG_BEIGE)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.ellipse(190, 378, 322, 512, fill=CREAM, outline=None, width=0)
    c.shade(body_mask, (124, 430, 390, 516), (30, 40, 50, 38))
    for left in (154, 286):
        c.ellipse(left, 448, left + 72, 510, fill=CREAM, outline=INK, width=4)
        c.line([(left + 22, 478), (left + 22, 498)], width=3)
        c.line([(left + 44, 474), (left + 44, 498)], width=3)

    # Ошейник с биркой.
    c.blob([(132, 408), (380, 408), (376, 442), (136, 442)], RED, width=5)
    c.ellipse(240, 430, 272, 462, fill=YELLOW)
    c.ellipse(250, 438, 262, 450, fill=None, outline=INK, width=2)

    left_ear = [(118, 226), (64, 214), (48, 300), (66, 374), (116, 390), (160, 330)]
    right_ear = [(394, 226), (448, 214), (464, 300), (446, 374), (396, 390), (352, 330)]
    for ear, inner in (
        (left_ear, [(108, 252), (76, 246), (66, 304), (80, 354), (112, 362), (136, 322)]),
        (right_ear, [(404, 252), (436, 246), (446, 304), (432, 354), (400, 362), (376, 322)]),
    ):
        c.blob(ear, DOG_BROWN)
        c.blob(inner, (109, 76, 65, 255), outline=None)
        c.shade(c.inset(c.shape_mask_blob(inner), 2), (60, 260, 160, 380), (30, 20, 20, 42))

    head = (112, 112, 400, 430)
    c.ellipse(*head, fill=DOG_BEIGE, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.clip(
        head_mask,
        lambda d, _layer: (
            d.ellipse(c.box(286, 174, 398, 292), fill=DOG_BROWN),
            d.ellipse(c.box(330, 128, 402, 200), fill=DOG_BROWN),
        ),
    )
    c.shade(head_mask, (180, 288, 420, 440), (30, 40, 50, 34))
    c.highlight(head_mask, (150, 138, 304, 236), (255, 255, 255, 85))

    c.blush(128, 310, 194, 354)
    c.blush(318, 310, 384, 354)
    c.eye(198, 264)
    c.eye(314, 264)

    # Брови-точки.
    c.ellipse(176, 218, 196, 228, fill=DOG_BROWN, outline=None, width=0)
    c.ellipse(324, 218, 344, 228, fill=DOG_BROWN, outline=None, width=0)

    muzzle = (184, 290, 328, 416)
    c.ellipse(*muzzle, fill=CREAM, outline=None, width=0)
    muzzle_mask = c.inset(c.shape_mask_ellipse(*muzzle), 2)
    c.shade(muzzle_mask, (198, 360, 326, 428), (120, 80, 60, 32))
    c.ellipse(226, 304, 286, 360, fill=DARK_BROWN, outline=INK, width=5)
    c.ellipse(238, 312, 252, 326, fill=WHITE, outline=None, width=0)
    c.arc(218, 342, 256, 382, 0, 110, width=5)
    c.arc(256, 342, 294, 382, 70, 180, width=5)
    c.ellipse(240, 372, 276, 414, fill=PINK, outline=INK, width=4)
    c.line([(258, 382), (258, 404)], width=3)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c



def duck():
    """Утка-кряква: зелёная голова, широкий клюв, белое кольцо и лапы с перепонками."""
    c = Canvas()
    c.ground_shadow(122, 436, 392, 494, opacity=28)

    # Туловище — широкий овал, как у остальных зверей.
    body = (116, 336, 396, 500)
    c.ellipse(*body, fill=DUCK_BODY)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (120, 420, 396, 506), (30, 40, 50, 42))
    c.highlight(body_mask, (152, 360, 316, 408), (255, 255, 255, 60))

    # Сложенное крыло с пёрышками.
    wing = (146, 392, 258, 468)
    c.ellipse(*wing, fill=DUCK_WING, outline=INK, width=5)
    c.arc(164, 418, 242, 448, 200, 340, fill=DUCK_WING_DARK, width=4)
    c.arc(164, 440, 232, 464, 200, 320, fill=DUCK_WING_DARK, width=3)

    # Пёрышки на грудке справа.
    for y in (406, 434):
        c.arc(300, y, 356, y + 26, 20, 160, fill=(172, 112, 86, 255), width=3)

    # Перепончатые лапы: широкий веер с тремя пальцами.
    for x, s in ((192, -1), (322, 1)):
        c.blob(
            [
                (x, 434), (x + 30 * s, 460), (x + 44 * s, 488),
                (x + 24 * s, 496), (x, 488), (x - 24 * s, 496),
                (x - 44 * s, 488), (x - 30 * s, 460),
            ],
            DEEP_ORANGE,
            width=4,
        )
        c.line([(x, 446), (x + 26 * s, 482)], fill=INK, width=3)
        c.line([(x, 448), (x, 488)], fill=INK, width=3)
        c.line([(x, 446), (x - 26 * s, 482)], fill=INK, width=3)

    # Голова — большой зелёный круг поверх туловища.
    head = (134, 106, 378, 380)
    c.ellipse(*head, fill=DUCK_GREEN, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (156, 300, 384, 388), (30, 40, 50, 26))
    c.highlight(head_mask, (162, 128, 300, 216), (255, 255, 255, 95))

    # Белое кольцо у основания головы.
    c.clip(
        head_mask,
        lambda draw, _layer: draw.ellipse(
            c.box(126, 330, 386, 402), fill=CREAM
        ),
    )

    c.blush(150, 250, 184, 286, opacity=70)
    c.blush(328, 250, 362, 286, opacity=70)
    c.eye(200, 214)
    c.eye(312, 214)

    # Широкий плоский клюв — главный признак утки.
    c.blob(
        [
            (192, 266), (320, 266), (334, 286), (336, 312), (318, 334),
            (256, 344), (194, 334), (176, 312), (178, 286),
        ],
        YELLOW,
        width=6,
    )
    c.arc(196, 296, 316, 338, 15, 165, fill=ORANGE, width=4)
    c.ellipse(230, 288, 242, 300, fill=ORANGE, outline=None, width=0)
    c.ellipse(270, 288, 282, 300, fill=ORANGE, outline=None, width=0)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c



def horse():
    c = Canvas()
    c.ground_shadow(116, 440, 396, 496, opacity=24)

    # Шея-грудь без отдельного «тазика»: одна плавная форма.
    neck = [(190, 286), (322, 286), (358, 420), (336, 492), (256, 512), (176, 492), (154, 420)]
    c.blob(neck, HORSE_TAN)
    neck_mask = c.inset(c.shape_mask_blob(neck), 3)
    c.shade(neck_mask, (142, 410, 380, 516), (30, 40, 50, 38))
    c.highlight(neck_mask, (190, 310, 306, 390), (255, 255, 255, 55))

    # Грива лежит позади головы и мягко обрамляет морду.
    mane = [
        (116, 250), (122, 150), (174, 90), (332, 90), (388, 150), (396, 250),
        (366, 308), (334, 274), (342, 202), (300, 158), (212, 158), (170, 202),
        (178, 274), (146, 308),
    ]
    c.blob(mane, MANE_BROWN)
    c.blob([(336, 304), (382, 338), (394, 480), (350, 486), (318, 378)], MANE_BROWN, width=5)
    c.blob([(176, 304), (130, 338), (118, 480), (162, 486), (194, 378)], MANE_BROWN, width=5)

    # Уши.
    c.blob([(148, 224), (136, 86), (204, 154), (204, 216)], HORSE_TAN)
    c.blob([(364, 224), (376, 86), (308, 154), (308, 216)], HORSE_TAN)
    c.blob([(158, 202), (156, 122), (188, 164), (188, 202)], PEACH, outline=None)
    c.blob([(354, 202), (356, 122), (324, 164), (324, 202)], PEACH, outline=None)

    head = (136, 108, 376, 424)
    c.ellipse(*head, fill=HORSE_TAN, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)

    # Чёлка.
    c.clip(
        head_mask,
        lambda d, _layer: (
            d.ellipse(c.box(144, 96, 242, 182), fill=MANE_BROWN),
            d.ellipse(c.box(210, 84, 308, 170), fill=MANE_BROWN),
            d.ellipse(c.box(276, 104, 368, 190), fill=MANE_BROWN),
            d.polygon(
                [
                    (scaled(228), scaled(132)),
                    (scaled(248), scaled(108)),
                    (scaled(270), scaled(132)),
                    (scaled(292), scaled(108)),
                    (scaled(304), scaled(154)),
                    (scaled(218), scaled(160)),
                ],
                fill=MANE_BROWN,
            ),
        ),
    )
    # Светлая проточина.
    c.clip(
        head_mask,
        lambda d, _layer: d.polygon(
            [
                (scaled(244), scaled(150)),
                (scaled(268), scaled(150)),
                (scaled(282), scaled(320)),
                (scaled(230), scaled(320)),
            ],
            fill=BEIGE,
        ),
    )
    c.shade(head_mask, (190, 286, 390, 438), (30, 40, 50, 32))
    c.highlight(head_mask, (166, 140, 300, 226), (255, 255, 255, 80))

    c.blush(154, 316, 218, 358)
    c.blush(294, 316, 358, 358)
    c.eye(202, 254)
    c.eye(310, 254)

    muzzle = (198, 300, 314, 438)
    c.ellipse(*muzzle, fill=BEIGE, outline=INK, width=5)
    muzzle_mask = c.inset(c.shape_mask_ellipse(*muzzle), 2)
    c.highlight(muzzle_mask, (208, 314, 276, 352), (255, 255, 255, 95))
    # Ноздри — широкие овалы по бокам морды: круглые точки в центре
    # делали из морды коровью, а узкие щели — человеческий нос.
    c.ellipse(220, 352, 244, 374, fill=DARK_BROWN, outline=None, width=0)
    c.ellipse(270, 352, 294, 374, fill=DARK_BROWN, outline=None, width=0)
    c.arc(232, 376, 280, 414, 20, 160, width=5)
    c.ellipse(246, 404, 266, 420, fill=ROSE, outline=None, width=0)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c



def rooster():
    c = Canvas()
    c.ground_shadow(118, 444, 394, 496, opacity=26)

    # Хвост — веер из узких серповидных перьев разного цвета: по нему
    # петуха узнают с первого взгляда, в отличие от «листиков».
    origin = (352, 386)

    def sickle(tip, thickness, color):
        ox, oy = origin
        dx, dy = tip[0] - ox, tip[1] - oy
        length = math.hypot(dx, dy)
        px, py = -dy / length, dx / length
        bulge = thickness * 0.7
        inner = thickness * 1.35
        mid_x = ox + dx * 0.58 + px * bulge
        mid_y = oy + dy * 0.58 + py * bulge
        near_x = ox + dx * 0.2 + px * bulge * 0.7
        near_y = oy + dy * 0.2 + py * bulge * 0.7
        points = [
            (ox + px * thickness * 0.42, oy + py * thickness * 0.42),
            (near_x, near_y),
            (mid_x, mid_y),
            (tip[0] - px * thickness * 0.12, tip[1] - py * thickness * 0.12),
            (mid_x - px * inner, mid_y - py * inner),
            (ox - px * thickness * 0.42, oy - py * thickness * 0.42),
        ]
        c.blob(points, color, width=5)

    sickle((356, 62), 50, DARK_GREEN)
    sickle((424, 84), 48, (46, 125, 50, 255))
    sickle((474, 158), 44, TEAL)
    sickle((496, 242), 40, DEEP_ORANGE)
    sickle((486, 328), 34, RED)

    # Лапки: толстые, с тремя пальцами — иначе в маленьком размере
    # от ног остаются две ниточки.
    for x in (202, 282):
        c.line([(x, 438), (x, 484)], fill=DEEP_ORANGE, width=13)
        for dx in (-26, 0, 26):
            c.line([(x, 484), (x + dx, 496)], fill=DEEP_ORANGE, width=9)

    body = (112, 270, 400, 470)
    c.ellipse(*body, fill=ROOSTER_CREAM)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (118, 382, 400, 480), (30, 40, 50, 38))
    c.highlight(body_mask, (154, 292, 300, 352), (255, 255, 255, 100))

    # Крыло — сложенное «рукавом» на боку, как у остальных птиц: круг
    # посреди грудки читался как ещё один глаз.
    wing = [(186, 306), (146, 322), (128, 366), (134, 418), (176, 442), (200, 412)]
    c.blob(wing, (230, 190, 140, 255), width=4)
    wing_mask = c.inset(c.shape_mask_blob(wing), 2)
    c.clip(
        wing_mask,
        lambda d, _layer: [
            d.arc(
                c.box(136, y, 204, y + 30),
                200,
                340,
                fill=(198, 152, 104, 255),
                width=scaled(3),
            )
            for y in (372, 404)
        ],
    )
    c.arc(154, 330, 226, 378, 200, 340, fill=(248, 220, 184, 255), width=4)

    # Гребешок — один сплошной красный «берет» с четырьмя выступами:
    # так нет швов между кружками. Рисуем до головы, чтобы край ушёл под неё.
    c.blob(
        [(182, 90), (174, 48), (200, 26), (222, 48), (238, 20), (262, 18),
         (272, 46), (292, 24), (316, 42), (324, 70), (332, 90), (292, 80), (230, 80)],
        RED,
        width=5,
    )

    head = (166, 76, 346, 282)
    c.ellipse(*head, fill=ROOSTER_CREAM, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (184, 196, 342, 292), (30, 40, 50, 26))
    c.highlight(head_mask, (190, 100, 302, 168), (255, 255, 255, 85))

    c.eye(214, 170)
    c.eye(294, 170)
    c.blush(176, 206, 220, 238)
    c.blush(288, 206, 332, 238)

    c.polygon([(216, 202), (292, 202), (254, 250)], fill=YELLOW, outline=INK, width=4)
    # Серёжка: два узких красных лепестка под клювом. Если начать её выше,
    # она слипается с клювом и читается как язык.
    c.blob(
        [(238, 252), (272, 252), (274, 282), (262, 306), (256, 292), (250, 306), (236, 282)],
        RED,
        width=4,
    )
    c.ellipse(*head, fill=None, outline=INK, width=6)

    # Пёрышки на груди.
    for y in (320, 344, 368):
        c.arc(228, y, 284, y + 30, 20, 160, fill=(210, 180, 130, 255), width=3)
    return c



def sheep():
    c = Canvas()
    c.ground_shadow(112, 446, 400, 500, opacity=26)

    # Шерсть: пушистые бугры по краю, внутренние контуры прячем под основным овалом.
    for left, top, right, bottom in (
        (108, 296, 204, 376),
        (168, 274, 264, 356),
        (248, 272, 344, 356),
        (310, 294, 404, 378),
        (100, 356, 184, 442),
        (328, 358, 410, 444),
        (128, 424, 232, 504),
        (286, 428, 386, 506),
    ):
        c.ellipse(left, top, right, bottom, fill=SHEEP_WOOL, width=5)
    body = (108, 302, 404, 496)
    c.ellipse(*body, fill=SHEEP_WOOL, outline=None, width=0)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (112, 420, 404, 504), (30, 40, 50, 32))
    c.highlight(body_mask, (150, 318, 330, 392), (255, 255, 255, 85))

    # Ножки.
    for x in (200, 286):
        c.ellipse(x, 456, x + 28, 504, fill=SHEEP_FACE, outline=INK, width=4)
        c.line([(x + 4, 480), (x + 24, 480)], width=3)

    # Уши.
    c.ellipse(116, 230, 186, 304, fill=SHEEP_FACE, width=5)
    c.ellipse(326, 230, 396, 304, fill=SHEEP_FACE, width=5)
    c.ellipse(132, 248, 170, 288, fill=PEACH, outline=None, width=0)
    c.ellipse(342, 248, 380, 288, fill=PEACH, outline=None, width=0)

    head = (168, 122, 344, 366)
    c.ellipse(*head, fill=SHEEP_FACE, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (190, 288, 350, 374), (90, 60, 45, 30))
    c.highlight(head_mask, (196, 154, 292, 222), (255, 255, 255, 70))

    # Шерстяная чёлка.
    for left, top, right in ((166, 112, 236), (208, 96, 280), (250, 96, 322), (292, 112, 352)):
        c.ellipse(left, top, right, top + 88, fill=SHEEP_WOOL, width=5)
    c.arc(190, 146, 230, 178, 200, 340, fill=(225, 218, 205, 255), width=4)
    c.arc(236, 134, 276, 166, 200, 340, fill=(225, 218, 205, 255), width=4)
    c.arc(282, 146, 322, 178, 200, 340, fill=(225, 218, 205, 255), width=4)

    c.blush(188, 276, 238, 310)
    c.blush(274, 276, 324, 310)
    c.eye(214, 238)
    c.eye(298, 238)

    # Мордочка.
    c.ellipse(224, 288, 288, 346, fill=CREAM, outline=None, width=0)
    c.polygon([(242, 300), (270, 300), (256, 318)], fill=PINK, outline=INK, width=3)
    c.arc(234, 310, 256, 338, 0, 90, width=4)
    c.arc(256, 310, 278, 338, 90, 180, width=4)
    c.ellipse(*head, fill=None, outline=INK, width=6)

    # Кудряшки на туловище.
    for x, y in ((142, 394), (184, 366), (232, 410), (310, 398), (350, 362)):
        c.arc(x - 20, y - 16, x + 20, y + 16, 20, 300, fill=(218, 210, 196, 255), width=4)
    return c



def pig():
    c = Canvas()
    c.ground_shadow(120, 440, 392, 498, opacity=26)

    # Хвостик-закорючка.
    c.arc(376, 372, 444, 440, 40, 330, fill=INK, width=15)
    c.arc(376, 372, 444, 440, 40, 330, fill=PIG_DARK, width=9)
    c.arc(398, 388, 426, 416, 40, 300, fill=INK, width=5)

    body = (116, 332, 396, 506)
    c.ellipse(*body, fill=PIG_PINK)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (124, 430, 392, 512), (30, 40, 50, 36))
    c.highlight(body_mask, (162, 350, 318, 406), (255, 255, 255, 75))
    for left in (174, 292):
        c.ellipse(left, 438, left + 64, 500, fill=PIG_DARK, outline=INK, width=4)
        c.line([(left + 18, 470), (left + 18, 490)], width=3)
        c.line([(left + 42, 462), (left + 42, 490)], width=3)

    # Уши.
    c.blob([(146, 210), (118, 132), (196, 154), (206, 210)], PIG_PINK)
    c.blob([(366, 210), (394, 132), (316, 154), (306, 210)], PIG_PINK)
    c.blob([(154, 196), (136, 148), (184, 164), (188, 198)], PIG_DARK, outline=None)
    c.blob([(358, 196), (376, 148), (328, 164), (324, 198)], PIG_DARK, outline=None)

    head = (128, 116, 384, 406)
    c.ellipse(*head, fill=PIG_PINK, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (190, 280, 390, 414), (90, 50, 60, 32))
    c.highlight(head_mask, (166, 140, 306, 230), (255, 255, 255, 90))
    for x in (236, 268, 300):
        c.arc(x - 14, 122, x + 14, 152, 210, 330, width=4)

    c.blush(142, 280, 198, 320)
    c.blush(314, 280, 370, 320)
    c.eye(202, 244)
    c.eye(310, 244)

    snout = (184, 280, 328, 378)
    c.ellipse(*snout, fill=PIG_DARK, outline=INK, width=5)
    snout_mask = c.inset(c.shape_mask_ellipse(*snout), 2)
    c.highlight(snout_mask, (198, 292, 272, 330), (255, 255, 255, 90))
    c.ellipse(220, 314, 244, 344, fill=INK, outline=None, width=0)
    c.ellipse(268, 314, 292, 344, fill=INK, outline=None, width=0)
    c.arc(226, 336, 286, 372, 20, 160, width=4)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c



def goat():
    c = Canvas()
    c.ground_shadow(120, 442, 392, 498, opacity=26)

    body = (116, 334, 396, 508)
    c.ellipse(*body, fill=GOAT_CREAM)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (122, 430, 390, 514), (30, 40, 50, 34))
    c.highlight(body_mask, (164, 352, 318, 412), (255, 255, 255, 70))
    for left in (176, 290):
        c.ellipse(left, 442, left + 60, 504, fill=GOAT_CREAM, outline=INK, width=4)
        c.line([(left + 14, 478), (left + 14, 496)], width=3)

    # Уши — чуть меньше и не такие «лопухи».
    c.blob([(152, 232), (100, 236), (80, 288), (94, 336), (136, 326), (164, 284)], GOAT_CREAM)
    c.blob([(360, 232), (412, 236), (432, 288), (418, 336), (376, 326), (348, 284)], GOAT_CREAM)
    c.blob([(140, 250), (108, 256), (94, 292), (106, 320), (134, 312), (150, 282)], PEACH, outline=None)
    c.blob([(372, 250), (404, 256), (418, 292), (406, 320), (378, 312), (362, 282)], PEACH, outline=None)

    # Рога — загнутые назад и заметно темнее шерсти, иначе их не видно.
    for horn in (
        [(190, 152), (180, 116), (168, 80), (152, 50), (138, 32), (156, 24),
         (176, 42), (194, 74), (206, 114), (212, 152)],
        [(320, 152), (330, 116), (342, 80), (358, 50), (372, 32), (354, 24),
         (334, 42), (316, 74), (304, 114), (298, 152)],
    ):
        c.blob(horn, (150, 128, 120, 255), width=4)
    for x, y in ((166, 56), (180, 88), (338, 56), (324, 88)):
        c.line([(x - 9, y - 5), (x + 9, y + 5)], fill=(184, 162, 152, 255), width=3)

    head = (158, 104, 354, 394)
    c.ellipse(*head, fill=GOAT_CREAM, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (190, 286, 360, 402), (90, 60, 45, 30))
    c.highlight(head_mask, (180, 132, 300, 210), (255, 255, 255, 75))

    # Чубчик не рисуем: три светлых пера между рогами читались как корона.

    c.blush(176, 272, 228, 308)
    c.blush(288, 272, 340, 308)
    c.eye(210, 238)
    c.eye(302, 238)

    # Мордочка и бородка. Бородку рисуем до мордочки: её верх уходит под
    # мордочку, и борода свисает с подбородка, а не поверх улыбки.
    c.blob(
        [(238, 358), (274, 358), (273, 416), (263, 446), (256, 422), (249, 446), (239, 416)],
        (216, 194, 150, 255),
        width=4,
    )
    c.line([(254, 396), (254, 418)], fill=(186, 162, 124, 255), width=3)
    c.ellipse(198, 286, 314, 382, fill=CREAM, outline=None, width=0)
    c.ellipse(230, 316, 282, 354, fill=ROSE, outline=None, width=0)
    c.ellipse(242, 326, 252, 338, fill=INK, outline=None, width=0)
    c.ellipse(260, 326, 270, 338, fill=INK, outline=None, width=0)
    c.arc(236, 334, 278, 362, 20, 160, width=4)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c



def frog():
    c = Canvas()
    c.ground_shadow(118, 448, 394, 500, opacity=26)

    # Задние лапки.
    c.blob([(136, 404), (102, 430), (84, 472), (110, 490), (158, 466), (172, 430)], FROG_GREEN)
    c.blob([(376, 404), (410, 430), (428, 472), (402, 490), (354, 466), (340, 430)], FROG_GREEN)
    for x in (100, 126, 152, 360, 386, 412):
        c.line([(x, 470), (x + 4, 492)], fill=FROG_DARK, width=4)

    # Глазные бугорки.
    c.ellipse(148, 100, 248, 226, fill=FROG_GREEN)
    c.ellipse(264, 100, 364, 226, fill=FROG_GREEN)

    body = (112, 196, 400, 474)
    c.ellipse(*body, fill=FROG_GREEN)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (118, 388, 400, 482), (30, 40, 50, 34))
    c.highlight(body_mask, (154, 250, 320, 330), (255, 255, 255, 60))
    c.ellipse(184, 326, 328, 476, fill=FROG_BELLY, outline=None, width=0)

    # Глаза.
    for left in (176, 292):
        c.ellipse(left, 134, left + 48, 206, fill=WHITE, outline=INK, width=5)
        c.ellipse(left + 14, 154, left + 34, 186, fill=INK, outline=None, width=0)
        c.ellipse(left + 18, 158, left + 26, 166, fill=WHITE, outline=None, width=0)

    # Улыбка.
    c.arc(156, 270, 356, 376, 15, 165, width=7)
    c.ellipse(196, 260, 212, 276, fill=FROG_DARK, outline=None, width=0)
    c.ellipse(300, 260, 316, 276, fill=FROG_DARK, outline=None, width=0)
    c.blush(142, 292, 204, 330)
    c.blush(308, 292, 370, 330)

    # Пятнышки.
    for left, top in ((132, 352), (326, 340), (154, 392), (348, 390), (248, 250)):
        c.ellipse(left, top, left + 18, top + 12, fill=FROG_DARK, outline=None, width=0)

    # Передние лапки.
    for left in (190, 270):
        c.ellipse(left, 444, left + 52, 494, fill=FROG_GREEN, outline=INK, width=4)
        for dx in (10, 24, 38):
            c.line([(left + dx, 478), (left + dx - 2, 496)], fill=FROG_DARK, width=3)
    return c



def goose():
    c = Canvas()
    c.ground_shadow(112, 448, 400, 498, opacity=26)

    # Шея — первым слоем: туловище закроет её низ, голова — верх.
    neck = [(198, 404), (222, 200), (292, 200), (310, 404)]
    c.blob(neck, GOOSE_WHITE, width=5)
    neck_mask = c.inset(c.shape_mask_blob(neck), 3)
    c.shade(neck_mask, (276, 250, 322, 420), (30, 40, 50, 26))

    # Хвостик (прячем основание за туловищем).
    c.blob([(338, 404), (406, 362), (438, 382), (384, 428)], GOOSE_WHITE, width=5)

    # Лапки.
    for x in (192, 288):
        c.line([(x, 430), (x, 486)], fill=DEEP_ORANGE, width=9)
        c.line([(x, 486), (x - 26, 498)], fill=DEEP_ORANGE, width=7)
        c.line([(x, 486), (x + 26, 498)], fill=DEEP_ORANGE, width=7)

    body = (112, 330, 400, 492)
    c.ellipse(*body, fill=GOOSE_WHITE)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (118, 420, 398, 502), (30, 40, 50, 34))
    c.highlight(body_mask, (146, 348, 310, 402), (255, 255, 255, 110))

    # Крыло.
    wing = (138, 368, 298, 474)
    c.ellipse(*wing, fill=GOOSE_WING, outline=INK, width=5)
    wing_mask = c.inset(c.shape_mask_ellipse(*wing), 2)
    c.highlight(wing_mask, (154, 386, 238, 428), (255, 255, 255, 85))
    for y in (402, 428):
        c.arc(160, y, 272, y + 26, 200, 340, fill=GOOSE_SHADE, width=4)
    c.arc(160, 452, 256, 474, 200, 320, fill=GOOSE_SHADE, width=3)

    # Голова.
    head = (170, 68, 344, 252)
    c.ellipse(*head, fill=GOOSE_WHITE, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (188, 194, 348, 262), (30, 40, 50, 22))
    c.highlight(head_mask, (190, 84, 270, 144), (255, 255, 255, 110))

    c.eye(220, 140)
    c.eye(296, 140)
    c.blush(182, 178, 216, 206, opacity=85)
    c.blush(300, 178, 334, 206, opacity=85)

    # Длинный клюв с небольшой шишкой у основания.
    c.blob(
        [(200, 192), (312, 192), (326, 220), (296, 244), (216, 244), (186, 220)],
        ORANGE,
        width=5,
    )
    c.ellipse(238, 172, 272, 198, fill=ORANGE, outline=INK, width=4)
    c.ellipse(226, 206, 236, 216, fill=DEEP_ORANGE, outline=None, width=0)
    c.ellipse(276, 206, 286, 216, fill=DEEP_ORANGE, outline=None, width=0)
    c.ellipse(*head, fill=None, outline=INK, width=6)

    # Пёрышки на груди.
    for y in (392, 416):
        c.arc(292, y, 352, y + 26, 20, 160, fill=GOOSE_SHADE, width=3)
    return c



def donkey():
    c = Canvas()
    c.ground_shadow(116, 442, 398, 498, opacity=26)

    body = (112, 334, 400, 510)
    c.ellipse(*body, fill=DONKEY_GRAY)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (120, 430, 398, 516), (30, 40, 50, 36))
    c.highlight(body_mask, (164, 352, 320, 412), (255, 255, 255, 70))
    for left in (176, 288):
        c.ellipse(left, 444, left + 60, 504, fill=DONKEY_LIGHT, outline=INK, width=4)
        c.line([(left + 14, 476), (left + 14, 496)], width=3)

    # Длинные уши.
    c.blob([(164, 160), (120, 68), (128, 28), (168, 34), (196, 120), (196, 168)], DONKEY_GRAY)
    c.blob([(348, 160), (392, 68), (384, 28), (344, 34), (316, 120), (316, 168)], DONKEY_GRAY)
    c.blob([(160, 152), (132, 74), (138, 48), (164, 56), (182, 128), (182, 158)], PEACH, outline=None)
    c.blob([(352, 152), (380, 74), (374, 48), (348, 56), (330, 128), (330, 158)], PEACH, outline=None)

    # Тёмная грива.
    c.blob([(196, 120), (246, 84), (266, 84), (316, 120), (326, 328), (296, 350), (216, 350), (186, 328)], DONKEY_MANE, width=6)
    c.blob([(228, 96), (246, 60), (266, 60), (284, 96), (270, 130), (242, 130)], DONKEY_MANE, width=5)

    head = (148, 104, 364, 408)
    c.ellipse(*head, fill=DONKEY_GRAY, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (190, 294, 372, 416), (30, 40, 50, 30))
    c.highlight(head_mask, (170, 130, 294, 208), (255, 255, 255, 80))

    c.blush(164, 276, 218, 312)
    c.blush(294, 276, 348, 312)
    c.eye(204, 242, lash=True)
    c.eye(306, 242, lash=True)

    muzzle = (190, 292, 322, 404)
    c.ellipse(*muzzle, fill=DONKEY_LIGHT, outline=INK, width=5)
    muzzle_mask = c.inset(c.shape_mask_ellipse(*muzzle), 2)
    c.highlight(muzzle_mask, (204, 306, 276, 344), (255, 255, 255, 90))
    c.ellipse(220, 334, 236, 352, fill=INK, outline=None, width=0)
    c.ellipse(276, 334, 292, 352, fill=INK, outline=None, width=0)
    c.arc(232, 354, 280, 392, 20, 160, width=5)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c



def owl():
    c = Canvas()
    c.ground_shadow(118, 448, 394, 500, opacity=26)

    body = (120, 258, 392, 484)
    c.ellipse(*body, fill=OWL_BROWN)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (126, 400, 392, 492), (30, 40, 50, 36))
    c.ellipse(198, 298, 314, 492, fill=OWL_CREAM, outline=None, width=0)

    # Крылья.
    c.ellipse(112, 282, 200, 436, fill=OWL_DARK, outline=INK, width=5)
    c.ellipse(312, 282, 400, 436, fill=OWL_DARK, outline=INK, width=5)
    for y in (330, 358, 386):
        c.arc(124, y, 184, y + 28, 90, 270, fill=OWL_BROWN, width=3)
        c.arc(328, y, 388, y + 28, 270, 90, fill=OWL_BROWN, width=3)

    # Ушки-кисточки.
    c.blob([(150, 132), (118, 54), (184, 84), (202, 134)], OWL_BROWN)
    c.blob([(362, 132), (394, 54), (328, 84), (310, 134)], OWL_BROWN)

    head = (128, 88, 384, 342)
    c.ellipse(*head, fill=OWL_BROWN, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (190, 244, 392, 352), (30, 40, 50, 30))
    c.highlight(head_mask, (162, 116, 306, 200), (255, 255, 255, 65))

    # Лицевой диск.
    face = (144, 132, 368, 306)
    c.ellipse(*face, fill=OWL_CREAM, outline=INK, width=4)
    face_mask = c.inset(c.shape_mask_ellipse(*face), 2)
    c.shade(face_mask, (160, 240, 360, 316), (140, 100, 70, 26))
    c.arc(190, 148, 246, 208, 180, 360, fill=(210, 186, 154, 255), width=3)
    c.arc(266, 148, 322, 208, 180, 360, fill=(210, 186, 154, 255), width=3)
    c.blush(154, 246, 198, 276, opacity=65)
    c.blush(314, 246, 358, 276, opacity=65)

    # Глаза совы.
    for cx in (212, 300):
        c.ellipse(cx - 38, 184, cx + 38, 260, fill=YELLOW, outline=INK, width=5)
        c.ellipse(cx - 18, 200, cx + 18, 244, fill=INK, outline=None, width=0)
        c.ellipse(cx - 10, 206, cx + 2, 218, fill=WHITE, outline=None, width=0)
    c.polygon([(238, 258), (282, 258), (260, 292)], fill=ORANGE, outline=INK, width=4)

    # Пёрышки на груди.
    for y in (326, 352, 378, 404):
        for x in (222, 256, 290):
            c.arc(x - 22, y, x + 22, y + 22, 20, 160, fill=(205, 184, 150, 255), width=3)

    # Лапки.
    for x in (218, 286):
        c.line([(x, 456), (x, 486)], fill=DEEP_ORANGE, width=7)
        c.line([(x, 486), (x - 18, 498)], fill=DEEP_ORANGE, width=5)
        c.line([(x, 486), (x + 18, 498)], fill=DEEP_ORANGE, width=5)
    return c



def mouse():
    c = Canvas()
    c.ground_shadow(124, 446, 388, 498, opacity=24)

    # Хвостик.
    c.line([(356, 430), (426, 456), (464, 414), (442, 364)], fill=INK, width=14)
    c.line([(356, 430), (426, 456), (464, 414), (442, 364)], fill=MOUSE_PINK, width=8)

    body = (136, 312, 376, 486)
    c.ellipse(*body, fill=MOUSE_GRAY)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (142, 410, 372, 494), (30, 40, 50, 34))
    c.ellipse(184, 342, 328, 490, fill=MOUSE_LIGHT, outline=None, width=0)

    # Уши.
    c.ellipse(116, 100, 230, 240, fill=MOUSE_GRAY)
    c.ellipse(282, 100, 396, 240, fill=MOUSE_GRAY)
    c.ellipse(138, 124, 208, 214, fill=MOUSE_PINK, outline=None, width=0)
    c.ellipse(304, 124, 374, 214, fill=MOUSE_PINK, outline=None, width=0)

    head = (148, 116, 364, 364)
    c.ellipse(*head, fill=MOUSE_GRAY, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (190, 276, 370, 372), (30, 40, 50, 28))
    c.highlight(head_mask, (174, 144, 296, 218), (255, 255, 255, 80))

    c.blush(158, 270, 208, 304, opacity=80)
    c.blush(304, 270, 354, 304, opacity=80)
    c.eye(204, 238)
    c.eye(308, 238)

    # Мордочка.
    c.ellipse(240, 274, 272, 300, fill=MOUSE_PINK, outline=INK, width=4)
    c.ellipse(238, 300, 278, 342, fill=CREAM, outline=None, width=0)
    c.line([(256, 304), (256, 324)], width=4)
    c.arc(230, 310, 256, 336, 0, 90, width=4)
    c.arc(256, 310, 282, 336, 90, 180, width=4)
    c.polygon([(242, 322), (254, 322), (254, 344), (242, 344)], fill=WHITE, outline=INK, width=2)
    c.polygon([(258, 322), (270, 322), (270, 344), (258, 344)], fill=WHITE, outline=INK, width=2)
    for y, dx in ((268, 1), (288, 2), (308, 3)):
        c.line([(148, y), (58, y - 12 * dx)], width=3)
        c.line([(364, y), (454, y - 12 * dx)], width=3)

    # Лапки.
    for left in (176, 286):
        c.ellipse(left, 450, left + 52, 500, fill=MOUSE_LIGHT, outline=INK, width=4)
        c.line([(left + 14, 476), (left + 14, 494)], width=3)
        c.line([(left + 36, 472), (left + 36, 494)], width=3)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c



def bee():
    c = Canvas()
    c.ground_shadow(168, 438, 344, 492, opacity=24)

    # Крылья.
    c.ellipse(132, 146, 244, 306, fill=WING, outline=(158, 190, 214, 220), width=4)
    c.ellipse(268, 146, 380, 306, fill=WING, outline=(158, 190, 214, 220), width=4)
    for points in (
        [(180, 190), (206, 232), (204, 276)],
        [(332, 190), (306, 232), (308, 276)],
    ):
        c.line(points, fill=(170, 200, 220, 180), width=3)

    # Тело.
    body = (176, 180, 336, 444)
    c.ellipse(*body, fill=BEE_YELLOW)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)

    def stripes(d, _layer):
        for top in (234, 298, 362):
            d.rectangle(c.box(160, top, 352, top + 34), fill=BEE_DARK)

    c.clip(body_mask, stripes)
    c.shade(body_mask, (184, 396, 336, 452), (30, 40, 50, 28))
    c.highlight(body_mask, (192, 202, 272, 270), (255, 255, 255, 95))
    c.polygon([(242, 438), (270, 438), (256, 472)], fill=BEE_DARK, outline=INK, width=4)

    # Лапки.
    for x in (190, 322):
        c.line([(x, 330), (x - 26, 360)], fill=BEE_DARK, width=6)
        c.line([(x, 390), (x - 26, 420)], fill=BEE_DARK, width=6)

    # Голова.
    head = (198, 96, 314, 232)
    c.ellipse(*head, fill=BEE_YELLOW, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.highlight(head_mask, (210, 112, 278, 162), (255, 255, 255, 90))

    # Усики.
    c.line([(224, 124), (198, 66), (180, 58)], width=5)
    c.line([(288, 124), (314, 66), (332, 58)], width=5)
    c.ellipse(168, 48, 190, 70, fill=BEE_DARK, outline=INK, width=3)
    c.ellipse(322, 48, 344, 70, fill=BEE_DARK, outline=INK, width=3)

    c.eye(230, 174)
    c.eye(282, 174)
    c.blush(210, 198, 238, 220, opacity=75)
    c.blush(274, 198, 302, 220, opacity=75)
    c.arc(240, 190, 274, 220, 20, 160, width=4)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c



def turkey():
    c = Canvas()
    c.ground_shadow(108, 444, 402, 502, opacity=26)

    # Хвост-веер: крупные перья с «глазками» на концах. Веер шире головы,
    # поэтому даже на маленькой карточке читается как главный признак индюка.
    cx, cy = 256, 350
    inner, outer = 42, 216
    colors = (
        TURKEY_TAN,
        (207, 151, 108, 255),
        TURKEY_BROWN,
        (176, 112, 84, 255),
        TURKEY_TAN,
        (214, 158, 112, 255),
        TURKEY_BROWN,
        (176, 112, 84, 255),
        TURKEY_TAN,
        (207, 151, 108, 255),
        TURKEY_BROWN,
        (176, 112, 84, 255),
        TURKEY_TAN,
    )
    for index, angle in enumerate(range(-96, 97, 16)):
        theta = math.radians(angle - 90)
        dx, dy = math.cos(theta), math.sin(theta)
        nx, ny = -dy, dx
        half_base = 22
        half_tip = 38
        points = [
            (cx + dx * inner + nx * half_base, cy + dy * inner + ny * half_base),
            (cx + dx * outer + nx * half_tip, cy + dy * outer + ny * half_tip),
            (cx + dx * outer - nx * half_tip, cy + dy * outer - ny * half_tip),
            (cx + dx * inner - nx * half_base, cy + dy * inner - ny * half_base),
        ]
        c.blob(points, colors[index % len(colors)], width=5)
        spot_x = cx + dx * (outer - 30)
        spot_y = cy + dy * (outer - 30)
        c.ellipse(
            spot_x - 14,
            spot_y - 14,
            spot_x + 14,
            spot_y + 14,
            fill=TURKEY_DARK if index % 2 else (238, 180, 120, 255),
            outline=INK,
            width=3,
        )

    # Лапки.
    for x in (204, 292):
        c.line([(x, 438), (x, 488)], fill=DEEP_ORANGE, width=8)
        c.line([(x, 488), (x - 24, 500)], fill=DEEP_ORANGE, width=6)
        c.line([(x, 488), (x + 24, 500)], fill=DEEP_ORANGE, width=6)

    # Туловище ниже и уже головы, чтобы веер оставался виден по сторонам.
    body = (124, 326, 388, 492)
    c.ellipse(*body, fill=TURKEY_BROWN)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.ellipse(184, 374, 328, 502, fill=TURKEY_TAN, outline=None, width=0)
    c.shade(body_mask, (130, 432, 388, 502), (30, 40, 50, 40))
    c.highlight(body_mask, (158, 344, 298, 388), (255, 255, 255, 60))

    # Крыло.
    c.ellipse(134, 350, 280, 470, fill=TURKEY_TAN, outline=INK, width=5)
    for y in (384, 412, 440):
        c.arc(152, y, 264, y + 24, 200, 340, fill=TURKEY_BROWN, width=3)

    # Пёрышки на груди справа.
    for y in (390, 418):
        c.arc(296, y, 356, y + 26, 20, 160, fill=TURKEY_TAN, width=3)

    # Голова: крупная, но не закрывает весь веер.
    head = (154, 84, 358, 326)
    c.ellipse(*head, fill=TURKEY_BROWN, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (180, 278, 350, 336), (40, 46, 54, 26))
    c.highlight(head_mask, (178, 108, 264, 172), (255, 255, 255, 65))

    c.blush(168, 222, 208, 256, opacity=70)
    c.blush(304, 222, 344, 256, opacity=70)
    c.eye(212, 204)
    c.eye(300, 204)

    # Красный нарост над клювом рисуется до клюва, серёжка — после.
    c.blob(
        [(246, 206), (266, 206), (274, 248), (256, 270), (238, 248)],
        RED,
        width=4,
    )
    c.polygon([(220, 250), (292, 250), (256, 308)], fill=YELLOW, outline=INK, width=4)
    c.ellipse(242, 274, 248, 280, fill=DEEP_ORANGE, outline=None, width=0)
    c.ellipse(264, 274, 270, 280, fill=DEEP_ORANGE, outline=None, width=0)
    c.blob(
        [(234, 302), (278, 302), (284, 342), (256, 366), (228, 342)],
        RED,
        width=4,
    )
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c


def chicken():
    c = Canvas()
    c.ground_shadow(116, 446, 398, 500, opacity=26)

    # Короткий хвост из трёх перьев — основание спрячется за туловищем.
    c.blob([(336, 420), (430, 356), (454, 380), (386, 440)], CHICKEN_WING, width=5)
    c.blob([(346, 432), (442, 404), (446, 430), (388, 454)], CHICKEN_CREAM, width=5)
    c.blob([(352, 442), (428, 446), (410, 466), (376, 462)], CHICKEN_FEATHER, width=5)

    # Лапки.
    for x in (204, 286):
        c.line([(x, 438), (x, 484)], fill=ORANGE, width=8)
        c.line([(x, 484), (x - 24, 498)], fill=ORANGE, width=6)
        c.line([(x, 484), (x + 24, 498)], fill=ORANGE, width=6)

    body = (116, 330, 396, 496)
    c.ellipse(*body, fill=CHICKEN_CREAM)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (122, 424, 396, 504), (30, 40, 50, 34))
    c.highlight(body_mask, (154, 348, 302, 404), (255, 255, 255, 105))

    # Крыло с мягкими пёрышками.
    c.ellipse(140, 366, 294, 472, fill=CHICKEN_WING, outline=INK, width=5)
    wing_mask = c.inset(c.shape_mask_ellipse(140, 366, 294, 472), 2)
    c.highlight(wing_mask, (156, 382, 236, 424), (255, 255, 255, 70))
    for y in (402, 428, 452):
        c.arc(158, y, 276, y + 22, 200, 340, fill=CHICKEN_FEATHER, width=4)

    # Гребешок рисуется до головы: её контур закроет основание.
    c.blob(
        [(190, 126), (178, 70), (208, 82), (218, 44), (248, 68),
         (266, 38), (290, 68), (318, 54), (314, 100), (294, 130), (210, 130)],
        CHICKEN_RED,
        width=5,
    )

    head = (136, 102, 376, 394)
    c.ellipse(*head, fill=CHICKEN_CREAM, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (184, 300, 378, 402), (110, 70, 50, 26))
    c.highlight(head_mask, (164, 126, 292, 206), (255, 255, 255, 110))

    c.blush(150, 286, 206, 322)
    c.blush(306, 286, 362, 322)
    c.eye(204, 244, lash=True)
    c.eye(308, 244, lash=True)

    # Серёжка под клювом, затем сам клюв поверх неё.
    c.blob([(236, 314), (276, 314), (282, 352), (256, 386), (230, 352)], CHICKEN_RED, width=4)
    c.polygon([(230, 274), (282, 274), (256, 322)], fill=YELLOW, outline=INK, width=4)
    c.ellipse(245, 292, 251, 298, fill=DEEP_ORANGE, outline=None, width=0)
    c.ellipse(261, 292, 267, 298, fill=DEEP_ORANGE, outline=None, width=0)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c


def wolf():
    c = Canvas()
    c.ground_shadow(116, 442, 398, 500, opacity=28)

    # Пушистый хвост со светлым кончиком.
    tail = [(350, 430), (414, 452), (462, 420), (470, 378), (448, 344),
            (418, 348), (406, 376), (430, 396), (406, 414), (374, 410)]
    c.blob(tail, WOLF_GRAY)
    tail_mask = c.inset(c.shape_mask_blob(tail), 2)
    c.clip(tail_mask, lambda d, _layer: d.ellipse(c.box(436, 342, 476, 392), fill=WOLF_LIGHT))
    c.arc(414, 380, 452, 410, 200, 340, fill=WOLF_DARK, width=3)

    body = (116, 334, 396, 510)
    c.ellipse(*body, fill=WOLF_GRAY)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (122, 428, 396, 516), (30, 40, 50, 36))
    c.ellipse(188, 372, 324, 512, fill=WOLF_LIGHT, outline=None, width=0)
    for left in (170, 284):
        c.ellipse(left, 450, left + 64, 510, fill=WOLF_LIGHT, outline=INK, width=4)
        c.line([(left + 18, 480), (left + 18, 498)], width=3)
        c.line([(left + 42, 476), (left + 42, 498)], width=3)

    # Уши — до головы, чтобы их основания ушли под контур. Внутри тёмная
    # шерсть, а не розовая: розовые ушки сразу делают из волка кошку.
    left_ear = [(100, 242), (104, 54), (214, 142), (224, 226)]
    right_ear = [(412, 242), (408, 54), (298, 142), (288, 226)]
    for ear, inner in (
        (left_ear, [(126, 212), (132, 96), (194, 158), (200, 208)]),
        (right_ear, [(386, 212), (380, 96), (318, 158), (312, 208)]),
    ):
        c.blob(ear, WOLF_GRAY)
        c.blob(inner, WOLF_DARK, outline=INK, width=3)
    c.blob([(106, 102), (104, 62), (142, 84), (146, 124)], (74, 84, 92, 255), width=4)
    c.blob([(406, 102), (408, 62), (370, 84), (366, 124)], (74, 84, 92, 255), width=4)

    head = (112, 132, 400, 424)
    c.ellipse(*head, fill=WOLF_GRAY, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)

    def wolf_markings(d, _layer):
        # Светлые пятна вокруг глаз узкие: широкие читались как маска енота.
        d.ellipse(c.box(172, 220, 234, 292), fill=WOLF_LIGHT)
        d.ellipse(c.box(278, 220, 340, 292), fill=WOLF_LIGHT)

    c.clip(head_mask, wolf_markings)
    c.shade(head_mask, (196, 300, 402, 432), (30, 40, 50, 30))
    c.highlight(head_mask, (148, 156, 286, 226), (255, 255, 255, 85))

    # Сердитые брови — «не кошка».
    c.line([(182, 206), (222, 220)], width=4)
    c.line([(330, 206), (290, 220)], width=4)

    c.blush(140, 300, 196, 336)
    c.blush(316, 300, 372, 336)
    c.eye(202, 256)
    c.eye(310, 256)

    muzzle = (196, 296, 316, 414)
    c.ellipse(*muzzle, fill=WOLF_LIGHT, outline=INK, width=5)
    muzzle_mask = c.inset(c.shape_mask_ellipse(*muzzle), 2)
    c.highlight(muzzle_mask, (208, 310, 274, 344), (255, 255, 255, 90))
    # Волчий нос — крупный и тёмный, с чёткой спинкой.
    c.blob(
        [(230, 306), (282, 306), (274, 336), (238, 336)],
        (68, 70, 78, 255),
        width=4,
    )
    c.ellipse(242, 312, 272, 322, fill=(120, 124, 134, 255), outline=None, width=0)
    # Пасть: вертикальная линия и два уголка, чтобы улыбка не была кошачьей.
    c.line([(256, 336), (256, 366)], width=4)
    c.arc(228, 348, 258, 384, 0, 90, width=4)
    c.arc(254, 348, 284, 384, 90, 180, width=4)
    # Два клыка — волка узнают по зубам.
    c.polygon([(232, 356), (244, 356), (238, 382)], fill=WHITE, outline=INK, width=3)
    c.polygon([(268, 356), (280, 356), (274, 382)], fill=WHITE, outline=INK, width=3)

    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c


def bear():
    c = Canvas()
    c.ground_shadow(114, 444, 398, 500, opacity=28)

    body = (112, 338, 400, 510)
    c.ellipse(*body, fill=BEAR_FUR)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (120, 428, 398, 516), (30, 40, 50, 36))
    c.ellipse(188, 380, 324, 512, fill=BEAR_LIGHT, outline=None, width=0)
    for left in (168, 288):
        c.ellipse(left, 452, left + 64, 510, fill=BEAR_LIGHT, outline=INK, width=4)
        c.line([(left + 18, 480), (left + 18, 498)], width=3)
        c.line([(left + 44, 476), (left + 44, 498)], width=3)

    # Круглые ушки.
    c.ellipse(124, 94, 224, 194, fill=BEAR_FUR)
    c.ellipse(288, 94, 388, 194, fill=BEAR_FUR)
    c.ellipse(146, 116, 202, 172, fill=PEACH, outline=None, width=0)
    c.ellipse(310, 116, 366, 172, fill=PEACH, outline=None, width=0)

    head = (132, 118, 380, 408)
    c.ellipse(*head, fill=BEAR_FUR, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (200, 300, 384, 416), (60, 40, 30, 30))
    c.highlight(head_mask, (162, 144, 296, 220), (255, 255, 255, 75))

    # Чубчик.
    for x, dx in ((232, -4), (256, 0), (280, 4)):
        c.arc(x - 16, 122 + dx, x + 16, 158 + dx, 200, 340, width=4)

    c.blush(154, 288, 210, 324)
    c.blush(302, 288, 358, 324)
    c.eye(202, 248)
    c.eye(310, 248)

    muzzle = (190, 294, 322, 394)
    c.ellipse(*muzzle, fill=BEAR_LIGHT, outline=INK, width=5)
    muzzle_mask = c.inset(c.shape_mask_ellipse(*muzzle), 2)
    c.highlight(muzzle_mask, (204, 308, 278, 344), (255, 255, 255, 90))
    c.polygon([(242, 312), (270, 312), (266, 338), (256, 348), (246, 338)],
              fill=INK, outline=INK, width=4)
    c.arc(236, 336, 256, 364, 0, 90, width=4)
    c.arc(256, 336, 276, 364, 90, 180, width=4)
    c.arc(244, 352, 268, 380, 20, 160, width=4)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c


def crow():
    c = Canvas()
    c.ground_shadow(128, 430, 388, 490, opacity=28)

    # Хвост — два пера, торчащих из-под туловища вниз-в стороны.
    for points in (
        [(212, 400), (124, 436), (100, 486), (168, 480), (228, 444)],
        [(300, 400), (388, 436), (412, 486), (344, 480), (284, 444)],
    ):
        c.blob(points, CROW_DARK, width=5)
        c.line([(148, 460), (214, 430)], fill=CROW_GLOSS, width=3)

    # Лапки: серые, с длинными когтями — птичья примета.
    for x in (222, 290):
        c.line([(x, 440), (x, 486)], fill=(126, 132, 140, 255), width=17)
        c.line([(x, 486), (x - 32, 498)], fill=(126, 132, 140, 255), width=11)
        c.line([(x, 486), (x + 32, 498)], fill=(126, 132, 140, 255), width=11)
        c.line([(x, 486), (x, 500)], fill=(126, 132, 140, 255), width=11)

    # Туловище.
    body = (164, 286, 348, 458)
    c.ellipse(*body, fill=CROW_BLUE)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (170, 396, 348, 466), (20, 28, 38, 46))
    c.highlight(body_mask, (188, 306, 296, 348), (190, 210, 230, 55))

    # Тёмная грудка: на светлой птица выглядела в жилетке.
    c.ellipse(210, 320, 302, 456, fill=(96, 110, 128, 255), outline=None, width=0)
    for y in (356, 382, 408):
        c.arc(218, y, 294, y + 22, 20, 160, fill=(120, 142, 168, 255), width=3)

    # Крылья-«рукава»: крепятся у плеча и сужаются книзу, поэтому
    # не выглядят отдельными шарами по бокам.
    for wing in (
        [(190, 300), (140, 316), (118, 366), (126, 422), (172, 452), (206, 420)],
        [(322, 300), (372, 316), (394, 366), (386, 422), (340, 452), (306, 420)],
    ):
        c.blob(wing, CROW_DARK, width=5)
    for y in (344, 372, 400, 428):
        c.arc(132, y, 208, y + 24, 200, 340, fill=CROW_GLOSS, width=4)
        c.arc(304, y, 380, y + 24, 200, 340, fill=CROW_GLOSS, width=4)

    # Голова-«яйцо»: круглый верх и узкий подбородок, поэтому контур
    # не режет грудку пополам, как ровный круг.
    head_points = [
        (256, 62), (318, 76), (356, 128), (366, 190), (350, 248),
        (312, 288), (272, 304), (240, 304), (200, 288), (162, 248),
        (146, 190), (156, 128), (194, 76),
    ]
    c.blob(head_points, CROW_BLUE, width=6)
    head_mask = c.inset(c.shape_mask_blob(head_points), 4)
    c.shade(head_mask, (192, 240, 356, 312), (20, 28, 38, 42))
    c.highlight(head_mask, (176, 88, 296, 152), (190, 212, 232, 60))

    # Гладкая круглая голова без хохолка: с тремя торчащими перьями ворона
    # похожа на удода, а у ворона голова ровная. Светлые глазные кольца
    # нужны, чтобы зрачки не пропадали на тёмном оперении.
    for cx in (204, 308):
        c.ellipse(cx - 30, 156, cx + 30, 216, fill=(242, 246, 250, 255), outline=INK, width=4)
        c.eye(cx, 186, rx=16, ry=20)
    c.blush(158, 224, 200, 256, opacity=60)
    c.blush(312, 224, 354, 256, opacity=60)

    # Клюв: длинный и тяжёлый, с крючком на конце — главная воронья
    # примета. Широкий треугольник читался как голубиный.
    c.blob(
        [(228, 208), (286, 208), (278, 250), (266, 286), (258, 310),
         (250, 286), (238, 250)],
        (104, 112, 128, 255),
        width=5,
    )
    c.line([(234, 242), (282, 242)], fill=(150, 160, 176, 255), width=3)
    # Щетинки у основания клюва: без них клюв выглядит «наклейкой».
    for dx in (-1, 1):
        for step in (0, 1):
            x0 = 256 + dx * (26 + step * 10)
            c.line([(x0, 216), (x0 + dx * 8, 234)], fill=CROW_DARK, width=3)
    return c


def cuckoo():
    c = Canvas()
    c.ground_shadow(134, 432, 392, 492, opacity=26)

    # Длинный хвост с поперечными пестринами — главная примета кукушки.
    tail = [(330, 392), (410, 412), (474, 452), (458, 486), (368, 478), (330, 448)]
    c.blob(tail, CUCKOO_DARK, width=5)
    for x, y in ((384, 424), (414, 440), (444, 456)):
        c.ellipse(x - 12, y - 7, x + 12, y + 7, fill=CUCKOO_LIGHT, outline=None, width=0)

    # Лапки.
    for x in (222, 290):
        c.line([(x, 440), (x, 486)], fill=ORANGE, width=17)
        c.line([(x, 486), (x - 32, 498)], fill=ORANGE, width=11)
        c.line([(x, 486), (x + 32, 498)], fill=ORANGE, width=11)
        c.line([(x, 486), (x, 500)], fill=ORANGE, width=11)

    body = (168, 286, 348, 458)
    c.ellipse(*body, fill=CUCKOO_GRAY)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (174, 396, 348, 466), (30, 40, 50, 34))

    # Светлая грудка в поперечную полоску.
    belly = (206, 316, 306, 456)
    c.ellipse(*belly, fill=CUCKOO_LIGHT, outline=None, width=0)
    belly_mask = c.inset(c.shape_mask_ellipse(*belly), 2)

    def cuckoo_bars(d, _layer):
        for y in (346, 372, 398, 424):
            d.arc(c.box(206, y, 306, y + 24), 200, 340, fill=CUCKOO_DARK, width=scaled(5))

    c.clip(belly_mask, cuckoo_bars)

    # Крылья.
    for wing in (
        [(192, 300), (142, 316), (120, 366), (128, 422), (174, 452), (208, 420)],
        [(324, 300), (374, 316), (396, 366), (388, 422), (342, 452), (308, 420)],
    ):
        c.blob(wing, CUCKOO_DARK, width=5)
    for y in (344, 372, 400, 428):
        c.arc(134, y, 210, y + 24, 200, 340, fill=CUCKOO_GRAY, width=4)
        c.arc(306, y, 382, y + 24, 200, 340, fill=CUCKOO_GRAY, width=4)

    # Голова.
    head_points = [
        (256, 66), (318, 80), (354, 130), (364, 190), (348, 246),
        (312, 286), (272, 302), (240, 302), (200, 286), (164, 246),
        (148, 190), (158, 130), (194, 80),
    ]
    c.blob(head_points, CUCKOO_GRAY, width=6)
    head_mask = c.inset(c.shape_mask_blob(head_points), 4)
    c.highlight(head_mask, (178, 90, 296, 154), (255, 255, 255, 85))
    c.shade(head_mask, (192, 240, 354, 310), (30, 40, 50, 30))

    # Тёмная шапочка — вторая примета кукушки.
    c.clip(
        head_mask,
        lambda d, _layer: d.ellipse(c.box(150, 66, 362, 168), fill=CUCKOO_DARK),
    )

    # Жёлтое глазное кольцо и чёрный зрачок.
    for cx in (204, 308):
        c.ellipse(cx - 30, 172, cx + 30, 232, fill=YELLOW, outline=INK, width=4)
        c.eye(cx, 202, rx=16, ry=20)
    c.blush(160, 246, 204, 278, opacity=65)
    c.blush(308, 246, 352, 278, opacity=65)

    # Маленький светлый клюв — кукушка не хищник.
    c.polygon(
        [(238, 236), (274, 236), (256, 282)],
        fill=(244, 200, 128, 255),
        outline=INK,
        width=4,
    )
    c.line([(243, 250), (269, 250)], fill=INK_SOFT, width=2)
    return c


def elephant():
    c = Canvas()
    c.ground_shadow(110, 446, 402, 500, opacity=28)

    # Хвостик с кисточкой.
    c.line([(374, 414), (438, 426), (448, 460)], fill=INK, width=6)
    c.ellipse(436, 450, 460, 474, fill=ELEPHANT_DARK, outline=INK, width=3)

    body = (112, 338, 400, 512)
    c.ellipse(*body, fill=ELEPHANT_GRAY)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (118, 430, 400, 518), (30, 40, 50, 36))
    c.ellipse(186, 380, 326, 512, fill=ELEPHANT_LIGHT, outline=None, width=0)
    for left in (166, 288):
        c.ellipse(left, 446, left + 66, 512, fill=ELEPHANT_GRAY, outline=INK, width=4)
        c.arc(left + 14, 488, left + 26, 504, 180, 360, width=3)
        c.arc(left + 38, 488, left + 50, 504, 180, 360, width=3)

    # Хобот рисуется до головы: её контур закроет основание.
    trunk = [(238, 296), (276, 296), (290, 346), (286, 398), (274, 444),
             (262, 478), (250, 502), (230, 504), (216, 486), (226, 462),
             (240, 430), (248, 392), (240, 344), (234, 322)]
    c.blob(trunk, ELEPHANT_GRAY, width=5)
    trunk_mask = c.inset(c.shape_mask_blob(trunk), 2)
    c.shade(trunk_mask, (264, 350, 298, 480), (30, 40, 50, 30))
    c.highlight(trunk_mask, (240, 330, 270, 400), (255, 255, 255, 55))
    for y in (340, 370, 400, 430):
        c.arc(228, y, 292, y + 18, 200, 340, fill=ELEPHANT_DARK, width=3)
    c.arc(222, 470, 258, 506, 190, 350, fill=ELEPHANT_DARK, width=3)

    # Бивни — до головы, чтобы основания спрятались под ней.
    c.blob([(200, 334), (218, 330), (230, 396), (216, 412), (190, 378)], CREAM, width=4)
    c.blob([(312, 334), (294, 330), (282, 396), (296, 412), (322, 378)], CREAM, width=4)

    # Большие уши.
    left_ear = [(154, 192), (86, 150), (56, 232), (70, 330), (118, 376), (164, 332), (176, 252)]
    right_ear = [(358, 192), (426, 150), (456, 232), (442, 330), (394, 376), (348, 332), (336, 252)]
    for ear, inner in (
        (left_ear, [(140, 206), (96, 180), (76, 238), (88, 314), (122, 348), (148, 310), (154, 254)]),
        (right_ear, [(372, 206), (416, 180), (436, 238), (424, 314), (390, 348), (364, 310), (358, 254)]),
    ):
        c.blob(ear, ELEPHANT_GRAY)
        c.blob(inner, ELEPHANT_LIGHT, outline=None, width=4)

    head = (148, 116, 366, 392)
    c.ellipse(*head, fill=ELEPHANT_GRAY, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (200, 292, 372, 402), (30, 40, 50, 28))
    c.highlight(head_mask, (168, 138, 294, 214), (255, 255, 255, 80))

    c.blush(174, 286, 226, 320)
    c.blush(288, 286, 340, 320)
    c.eye(208, 238, lash=True)
    c.eye(306, 238, lash=True)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c


ANIMALS = (
    ("cow", cow),
    ("cat", cat),
    ("dog", dog),
    ("duck", duck),
    ("horse", horse),
    ("rooster", rooster),
    ("sheep", sheep),
    ("pig", pig),
    ("goat", goat),
    ("frog", frog),
    ("goose", goose),
    ("donkey", donkey),
    ("owl", owl),
    ("mouse", mouse),
    ("bee", bee),
    ("turkey", turkey),
    ("chicken", chicken),
    ("wolf", wolf),
    ("bear", bear),
    ("crow", crow),
    ("cuckoo", cuckoo),
    ("elephant", elephant),
)


def main():
    os.makedirs(OUT, exist_ok=True)
    paths = []
    for name, factory in ANIMALS:
        path = factory().save(name)
        paths.append(path)
        print(f"{name}: {path}")
    contact_sheet(paths, "/tmp/contact-sheet.png")


if __name__ == "__main__":
    main()
