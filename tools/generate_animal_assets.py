#!/usr/bin/env python3
"""Рисует иллюстрации животных для assets/images.

Стиль: мягкая пастельная «плюшевая» графика с плотным тёмным контуром,
крупными глазами и аккуратными тенями. Все картинки квадратные 512×512
с прозрачным фоном. Рисуем в 4× и уменьшаем — так контуры остаются гладкими.
"""
import math
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "images")

INK = (55, 71, 79, 255)
INK_SOFT = (89, 104, 113, 255)
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
PIG_PINK = (247, 169, 166, 255)
PIG_DARK = (230, 126, 132, 255)
GOAT_CREAM = (245, 226, 194, 255)
HORN = (188, 170, 164, 255)
FROG_GREEN = (129, 199, 132, 255)
FROG_DARK = (85, 160, 90, 255)
FROG_BELLY = (220, 237, 200, 255)
GOOSE_WHITE = (250, 250, 247, 255)
GOOSE_SHADE = (222, 226, 224, 255)
DONKEY_GRAY = (176, 190, 197, 255)
DONKEY_LIGHT = (224, 229, 231, 255)
DONKEY_MANE = (84, 110, 122, 255)
OWL_BROWN = (161, 136, 127, 255)
OWL_CREAM = (239, 226, 203, 255)
OWL_DARK = (109, 76, 65, 255)
MOUSE_GRAY = (189, 189, 189, 255)
MOUSE_LIGHT = (224, 224, 224, 255)
MOUSE_PINK = (244, 143, 177, 255)
BEE_YELLOW = (255, 213, 79, 255)
BEE_DARK = (93, 64, 55, 255)
WING = (232, 245, 255, 135)
TURKEY_BROWN = (141, 110, 99, 255)
TURKEY_DARK = (93, 64, 55, 255)
TURKEY_TAN = (196, 155, 112, 255)
CHICKEN_CREAM = (255, 240, 206, 255)
CHICKEN_WING = (241, 208, 156, 255)
CHICKEN_FEATHER = (214, 178, 132, 255)
CHICKEN_RED = (224, 98, 92, 255)
WOLF_GRAY = (156, 170, 181, 255)
WOLF_LIGHT = (226, 233, 237, 255)
WOLF_DARK = (101, 118, 131, 255)
BEAR_FUR = (166, 120, 84, 255)
BEAR_LIGHT = (216, 178, 136, 255)
BEAR_DARK = (126, 88, 62, 255)
CROW_BLUE = (74, 88, 104, 255)
CROW_DARK = (52, 62, 76, 255)
CROW_GLOSS = (96, 122, 148, 255)
CUCKOO_GRAY = (160, 174, 186, 255)
CUCKOO_DARK = (112, 128, 142, 255)
CUCKOO_LIGHT = (238, 240, 235, 255)
ELEPHANT_GRAY = (158, 172, 184, 255)
ELEPHANT_LIGHT = (208, 218, 226, 255)
ELEPHANT_DARK = (120, 135, 148, 255)

SCALE = 4
SIZE = 512
STROKE = 7


def scaled(value):
    return int(round(value * SCALE))


class Canvas:
    """Холст 512×512 с координатами в «дизайнерских» пикселях."""

    def __init__(self):
        self.image = Image.new(
            "RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0)
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


    def shade(self, mask, box, color=(30, 40, 50, 42), blur=16):
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
            self.box(left, top, right, bottom), fill=(60, 64, 67, opacity)
        )
        layer = layer.filter(ImageFilter.GaussianBlur(scaled(10)))
        self.image.alpha_composite(layer)

    def blush(self, left, top, right, bottom, opacity=95):
        layer = Image.new("RGBA", self.image.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).ellipse(
            self.box(left, top, right, bottom),
            fill=(255, 145, 128, opacity),
        )
        layer = layer.filter(ImageFilter.GaussianBlur(scaled(7)))
        self.image.alpha_composite(layer)

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
        canvas = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
        canvas.alpha_composite(
            art,
            ((canvas.width - art.width) // 2, (canvas.height - art.height) // 2),
        )
        path = os.path.join(OUT, f"{name}_512.png")
        canvas.resize((SIZE, SIZE), Image.LANCZOS).save(path, "PNG", optimize=True)
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

    muzzle = (190, 302, 322, 438)
    c.ellipse(*muzzle, fill=BEIGE, outline=INK, width=5)
    muzzle_mask = c.inset(c.shape_mask_ellipse(*muzzle), 2)
    c.highlight(muzzle_mask, (204, 316, 274, 354), (255, 255, 255, 95))
    c.ellipse(220, 344, 238, 362, fill=DARK_BROWN, outline=None, width=0)
    c.ellipse(274, 344, 292, 362, fill=DARK_BROWN, outline=None, width=0)
    c.arc(230, 362, 282, 406, 20, 160, width=5)
    c.ellipse(246, 396, 266, 414, fill=ROSE, outline=None, width=0)
    c.ellipse(*head, fill=None, outline=INK, width=6)

    # Блики на гриве.
    for x, y in ((150, 260), (366, 260), (344, 360), (166, 360)):
        c.arc(x - 22, y - 18, x + 22, y + 24, 100, 250, fill=(130, 100, 85, 255), width=4)
    return c



def rooster():
    c = Canvas()
    c.ground_shadow(118, 444, 394, 496, opacity=26)

    # Хвост веером.
    c.blob([(326, 350), (394, 110), (454, 148), (420, 300), (380, 382)], DARK_GREEN)
    c.blob([(318, 352), (438, 182), (492, 236), (420, 356), (368, 392)], GREEN)
    c.blob([(312, 352), (382, 238), (446, 316), (390, 396)], (46, 125, 50, 255))
    for points in (
        [(350, 320), (410, 170), (442, 186)],
        [(346, 330), (430, 228), (456, 260)],
    ):
        c.line(points, fill=(30, 90, 45, 255), width=4)

    # Лапки.
    for x in (202, 282):
        c.line([(x, 438), (x, 482)], fill=DEEP_ORANGE, width=9)
        c.line([(x, 482), (x - 24, 492)], fill=DEEP_ORANGE, width=7)
        c.line([(x, 482), (x + 24, 492)], fill=DEEP_ORANGE, width=7)

    body = (112, 270, 400, 470)
    c.ellipse(*body, fill=ROOSTER_CREAM)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (118, 382, 400, 480), (30, 40, 50, 38))
    c.highlight(body_mask, (154, 292, 300, 352), (255, 255, 255, 100))

    # Крыло.
    wing = (138, 302, 296, 428)
    c.ellipse(*wing, fill=BEIGE, outline=INK, width=5)
    c.arc(166, 328, 274, 402, 200, 340, fill=BROWN, width=4)
    c.arc(166, 350, 252, 414, 200, 320, fill=BROWN, width=3)

    head = (166, 76, 346, 282)
    c.ellipse(*head, fill=ROOSTER_CREAM, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (184, 196, 342, 292), (30, 40, 50, 26))
    c.highlight(head_mask, (190, 100, 302, 168), (255, 255, 255, 85))

    # Гребешок.
    for left, top in ((180, 46), (218, 28), (256, 28), (294, 46)):
        c.ellipse(left, top, left + 52, top + 70, fill=RED, width=5)

    c.eye(214, 170)
    c.eye(294, 170)
    c.blush(176, 206, 220, 238)
    c.blush(288, 206, 332, 238)

    c.polygon([(216, 202), (292, 202), (254, 250)], fill=YELLOW, outline=INK, width=4)
    c.ellipse(246, 238, 262, 292, fill=RED, outline=INK, width=4)
    c.arc(230, 248, 278, 274, 20, 160, width=3)
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

    # Длинные уши.
    c.blob([(150, 230), (92, 232), (64, 292), (82, 354), (130, 342), (162, 286)], GOAT_CREAM)
    c.blob([(362, 230), (420, 232), (448, 292), (430, 354), (382, 342), (350, 286)], GOAT_CREAM)
    c.blob([(136, 250), (98, 254), (82, 294), (96, 330), (128, 320), (148, 284)], PEACH, outline=None)
    c.blob([(376, 250), (414, 254), (430, 294), (416, 330), (384, 320), (364, 284)], PEACH, outline=None)

    # Рожки.
    c.blob(
        [(194, 142), (174, 100), (154, 62), (146, 30), (168, 24), (190, 52), (204, 100), (212, 142)],
        HORN,
    )
    c.blob(
        [(318, 142), (338, 100), (358, 62), (366, 30), (344, 24), (322, 52), (308, 100), (300, 142)],
        HORN,
    )
    for x, y in ((164, 50), (176, 76), (346, 50), (334, 76)):
        c.arc(x - 14, y - 8, x + 14, y + 10, 190, 350, fill=(150, 132, 128, 255), width=3)

    head = (158, 104, 354, 394)
    c.ellipse(*head, fill=GOAT_CREAM, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (190, 286, 360, 402), (90, 60, 45, 30))
    c.highlight(head_mask, (180, 132, 300, 210), (255, 255, 255, 75))

    # Чубчик.
    for points in (
        [(224, 132), (216, 100), (240, 116)],
        [(246, 128), (252, 92), (264, 120)],
        [(270, 132), (288, 104), (286, 136)],
    ):
        c.blob(points, GOAT_CREAM, width=4)

    c.blush(176, 272, 228, 308)
    c.blush(288, 272, 340, 308)
    c.eye(210, 238)
    c.eye(302, 238)

    # Мордочка и бородка.
    c.ellipse(198, 286, 314, 382, fill=CREAM, outline=None, width=0)
    c.ellipse(230, 316, 282, 354, fill=ROSE, outline=None, width=0)
    c.ellipse(242, 326, 252, 338, fill=INK, outline=None, width=0)
    c.ellipse(260, 326, 270, 338, fill=INK, outline=None, width=0)
    c.arc(236, 334, 278, 362, 20, 160, width=4)
    c.blob([(228, 368), (284, 368), (276, 420), (256, 442), (236, 420)], GOAT_CREAM, width=4)
    for x in (244, 256, 268):
        c.line([(x, 390), (x, 418)], width=3)
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
    c.ellipse(*wing, fill=CREAM, outline=INK, width=5)
    c.arc(160, 398, 272, 444, 200, 340, fill=GOOSE_SHADE, width=4)
    c.arc(160, 426, 256, 462, 200, 320, fill=GOOSE_SHADE, width=3)

    # Голова.
    head = (170, 68, 344, 252)
    c.ellipse(*head, fill=GOOSE_WHITE, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (188, 194, 348, 262), (30, 40, 50, 22))
    c.highlight(head_mask, (190, 84, 270, 144), (255, 255, 255, 110))

    c.eye(220, 140)
    c.eye(296, 140)
    c.blush(182, 178, 216, 206, opacity=70)
    c.blush(300, 178, 334, 206, opacity=70)

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
    c.ground_shadow(114, 446, 398, 500, opacity=26)

    # Хвост-веер: широкие перья внахлёст, чтобы веер читался сплошным.
    cx, cy = 256, 376
    inner, outer = 50, 200
    colors = (TURKEY_TAN, TURKEY_BROWN, TURKEY_DARK, TURKEY_TAN, TURKEY_BROWN, TURKEY_TAN, TURKEY_DARK, TURKEY_BROWN)
    for index, angle in enumerate(range(-84, 85, 24)):
        theta = math.radians(angle - 90)
        dx, dy = math.cos(theta), math.sin(theta)
        nx, ny = -dy, dx
        half_base = 26
        half_tip = 44
        points = [
            (cx + dx * inner + nx * half_base, cy + dy * inner + ny * half_base),
            (cx + dx * outer + nx * half_tip, cy + dy * outer + ny * half_tip),
            (cx + dx * outer - nx * half_tip, cy + dy * outer - ny * half_tip),
            (cx + dx * inner - nx * half_base, cy + dy * inner - ny * half_base),
        ]
        c.blob(points, colors[index % len(colors)], width=5)
        c.ellipse(
            cx + dx * (outer - 8) - 9,
            cy + dy * (outer - 8) - 9,
            cx + dx * (outer - 8) + 9,
            cy + dy * (outer - 8) + 9,
            fill=TURKEY_DARK if index % 2 else TURKEY_TAN,
            outline=INK,
            width=2,
        )

    # Лапки.
    for x in (204, 286):
        c.line([(x, 430), (x, 484)], fill=DEEP_ORANGE, width=8)
        c.line([(x, 484), (x - 22, 496)], fill=DEEP_ORANGE, width=6)
        c.line([(x, 484), (x + 22, 496)], fill=DEEP_ORANGE, width=6)

    # Туловище — простое, как у остальных зверей.
    body = (116, 318, 396, 486)
    c.ellipse(*body, fill=TURKEY_BROWN)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (122, 424, 396, 496), (30, 40, 50, 40))
    c.highlight(body_mask, (156, 336, 296, 380), (255, 255, 255, 60))

    # Крыло.
    c.ellipse(140, 372, 268, 470, fill=TURKEY_TAN, outline=INK, width=5)
    for y in (404, 430):
        c.arc(160, y, 250, y + 24, 200, 340, fill=TURKEY_BROWN, width=3)

    # Пёрышки на груди справа.
    for y in (392, 420):
        c.arc(292, y, 352, y + 26, 20, 160, fill=TURKEY_TAN, width=3)

    # Крупная голова прямо на туловище.
    head = (150, 108, 362, 320)
    c.ellipse(*head, fill=TURKEY_BROWN, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (178, 272, 346, 330), (40, 46, 54, 26))
    c.highlight(head_mask, (176, 128, 258, 190), (255, 255, 255, 60))

    c.blush(172, 226, 210, 256, opacity=70)
    c.blush(302, 226, 340, 256, opacity=70)
    c.eye(216, 198)
    c.eye(296, 198)

    # Клюв, кожистый нарост на клюве и серёжка под клювом. Нарост рисуется
    # до клюва: торчит над ним и уходит под него, а не режет клюв пополам.
    c.ellipse(230, 190, 254, 254, fill=RED, outline=INK, width=4)
    c.polygon([(224, 234), (288, 234), (256, 282)], fill=YELLOW, outline=INK, width=4)
    c.ellipse(250, 278, 278, 316, fill=RED, outline=INK, width=4)
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

    # Уши — до головы, чтобы их основания ушли под контур.
    left_ear = [(100, 242), (104, 54), (214, 142), (224, 226)]
    right_ear = [(412, 242), (408, 54), (298, 142), (288, 226)]
    for ear, inner in (
        (left_ear, [(124, 216), (130, 92), (196, 158), (202, 212)]),
        (right_ear, [(388, 216), (382, 92), (316, 158), (310, 212)]),
    ):
        c.blob(ear, WOLF_GRAY)
        c.blob(inner, PEACH, outline=None)
    c.blob([(106, 102), (104, 62), (142, 84), (146, 124)], WOLF_DARK, width=4)
    c.blob([(406, 102), (408, 62), (370, 84), (366, 124)], WOLF_DARK, width=4)

    head = (112, 132, 400, 424)
    c.ellipse(*head, fill=WOLF_GRAY, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)

    def wolf_markings(d, _layer):
        d.ellipse(c.box(120, 268, 192, 364), fill=WOLF_LIGHT)
        d.ellipse(c.box(320, 268, 392, 364), fill=WOLF_LIGHT)
        d.ellipse(c.box(232, 136, 280, 218), fill=WOLF_DARK)
        d.ellipse(c.box(176, 224, 228, 276), fill=WOLF_LIGHT)
        d.ellipse(c.box(284, 224, 336, 276), fill=WOLF_LIGHT)

    c.clip(head_mask, wolf_markings)
    c.shade(head_mask, (196, 300, 402, 432), (30, 40, 50, 30))
    c.highlight(head_mask, (148, 156, 286, 226), (255, 255, 255, 85))

    # Чубчик и брови.
    for x, dx in ((232, -6), (256, 0), (280, 6)):
        c.line([(x, 148), (x + dx, 118)], width=4)
    c.line([(180, 214), (220, 204)], width=4)
    c.line([(332, 214), (292, 204)], width=4)

    c.blush(140, 300, 196, 336)
    c.blush(316, 300, 372, 336)
    c.eye(202, 256)
    c.eye(310, 256)

    muzzle = (186, 292, 326, 406)
    c.ellipse(*muzzle, fill=WOLF_LIGHT, outline=INK, width=5)
    muzzle_mask = c.inset(c.shape_mask_ellipse(*muzzle), 2)
    c.highlight(muzzle_mask, (200, 306, 274, 342), (255, 255, 255, 90))
    c.polygon([(238, 308), (274, 308), (256, 338)], fill=INK, outline=INK, width=4)
    c.arc(236, 334, 256, 362, 0, 90, width=4)
    c.arc(256, 334, 276, 362, 90, 180, width=4)
    c.arc(244, 352, 268, 378, 20, 160, width=4)

    # Пушистые «бакенбарды» поверх контура — силуэт волка, а не кошки.
    for points in (
        [(130, 292), (100, 312), (122, 330), (98, 350), (132, 348), (122, 372), (154, 350)],
        [(382, 292), (412, 312), (390, 330), (414, 350), (380, 348), (390, 372), (358, 350)],
    ):
        c.blob(points, WOLF_LIGHT, width=4)
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
    c.ground_shadow(118, 448, 398, 500, opacity=26)

    # Хвост.
    c.blob([(332, 408), (452, 432), (470, 468), (372, 470)], CROW_DARK, width=5)
    c.line([(392, 438), (448, 456)], fill=CROW_GLOSS, width=3)
    c.line([(380, 452), (430, 464)], fill=CROW_GLOSS, width=3)

    # Лапки.
    for x in (208, 286):
        c.line([(x, 448), (x, 486)], fill=(116, 122, 128, 255), width=7)
        c.line([(x, 486), (x - 20, 498)], fill=(116, 122, 128, 255), width=5)
        c.line([(x, 486), (x + 20, 498)], fill=(116, 122, 128, 255), width=5)

    body = (118, 330, 396, 500)
    c.ellipse(*body, fill=CROW_BLUE)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (124, 428, 396, 508), (20, 28, 38, 46))
    c.highlight(body_mask, (154, 348, 300, 400), (190, 210, 230, 55))

    # Крыло с блестящими пёрышками.
    c.ellipse(138, 366, 298, 476, fill=CROW_DARK, outline=INK, width=5)
    wing_mask = c.inset(c.shape_mask_ellipse(138, 366, 298, 476), 2)
    c.highlight(wing_mask, (154, 382, 232, 422), (150, 180, 210, 45))
    for y in (402, 428, 452):
        c.arc(156, y, 278, y + 24, 200, 340, fill=CROW_GLOSS, width=4)

    head = (138, 102, 374, 398)
    c.ellipse(*head, fill=CROW_BLUE, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (188, 300, 380, 408), (20, 28, 38, 42))
    c.highlight(head_mask, (166, 126, 288, 202), (190, 212, 232, 55))

    # Хохолок.
    for x, dx in ((230, -6), (256, 0), (282, 6)):
        c.line([(x, 118), (x + dx, 88)], width=4)

    # Светлые глаза, иначе чёрные зрачки теряются на тёмном оперении.
    for cx in (204, 308):
        c.ellipse(cx - 26, 220, cx + 26, 274, fill=(242, 246, 250, 255), outline=INK, width=4)
        c.eye(cx, 247, rx=15, ry=19)

    # Клюв: крупный, с крючком на конце и светлой спинкой, чтобы читался
    # на тёмном оперении.
    c.polygon([(228, 284), (284, 276), (268, 312), (284, 318), (258, 354)],
              fill=(104, 120, 138, 255), outline=INK, width=4)
    c.line([(234, 294), (278, 286)], width=3)
    c.line([(262, 312), (278, 316)], width=3)
    c.ellipse(250, 292, 256, 298, fill=INK, outline=None, width=0)
    c.ellipse(*head, fill=None, outline=INK, width=6)
    return c


def cuckoo():
    c = Canvas()
    c.ground_shadow(116, 448, 398, 500, opacity=26)

    # Длинный хвост с пестринами.
    c.blob([(332, 428), (446, 456), (462, 482), (380, 470)], CUCKOO_DARK, width=5)
    for x, y in ((388, 450), (414, 460), (440, 470)):
        c.ellipse(x - 8, y - 6, x + 8, y + 6, fill=CUCKOO_LIGHT, outline=None, width=0)

    # Лапки.
    for x in (208, 286):
        c.line([(x, 440), (x, 484)], fill=ORANGE, width=8)
        c.line([(x, 484), (x - 22, 498)], fill=ORANGE, width=6)
        c.line([(x, 484), (x + 22, 498)], fill=ORANGE, width=6)

    body = (120, 330, 394, 500)
    c.ellipse(*body, fill=CUCKOO_GRAY)
    body_mask = c.inset(c.shape_mask_ellipse(*body), 3)
    c.shade(body_mask, (126, 428, 394, 508), (30, 40, 50, 34))
    c.highlight(body_mask, (152, 348, 294, 400), (255, 255, 255, 70))

    # Светлая грудка в поперечную полоску.
    belly = (186, 366, 326, 506)
    c.ellipse(*belly, fill=CUCKOO_LIGHT, outline=None, width=0)
    belly_mask = c.inset(c.shape_mask_ellipse(*belly), 2)

    def cuckoo_bars(d, _layer):
        for y in (386, 410, 434, 458, 482):
            d.arc(c.box(190, y, 322, y + 26), 200, 340, fill=CUCKOO_DARK, width=scaled(4))

    c.clip(belly_mask, cuckoo_bars)

    # Крыло.
    c.ellipse(138, 366, 296, 474, fill=CUCKOO_DARK, outline=INK, width=5)
    for y in (400, 426, 450):
        c.arc(156, y, 276, y + 22, 200, 340, fill=CUCKOO_GRAY, width=4)

    head = (142, 106, 370, 396)
    c.ellipse(*head, fill=CUCKOO_GRAY, outline=None, width=0)
    head_mask = c.inset(c.shape_mask_ellipse(*head), 3)
    c.shade(head_mask, (190, 300, 376, 406), (30, 40, 50, 30))
    c.highlight(head_mask, (164, 128, 286, 202), (255, 255, 255, 85))

    # Тёмная шапочка.
    c.clip(head_mask, lambda d, _layer: d.ellipse(c.box(154, 108, 358, 206), fill=CUCKOO_DARK))

    # Жёлтая радужка и чёрный зрачок.
    for cx in (204, 308):
        c.ellipse(cx - 28, 218, cx + 28, 274, fill=YELLOW, outline=INK, width=4)
        c.eye(cx, 246, rx=15, ry=19)

    # Небольшой, но заметный клюв.
    c.polygon([(234, 272), (282, 268), (262, 334), (252, 314)],
              fill=CUCKOO_DARK, outline=INK, width=4)
    c.line([(240, 288), (276, 286)], width=3)
    c.ellipse(*head, fill=None, outline=INK, width=6)
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
