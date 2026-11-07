import math
import os
import sys
from PIL import Image, ImageChops, ImageDraw

import logo_geometry as geo

SIZE = 1024
SS = 4
PLATE_RADIUS = 228
PLATE_RADIUS_SMALL = 212

def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def sample(stops, t):
    t = min(1.0, max(0.0, t))
    for i in range(len(stops) - 1):
        p0, c0 = stops[i]
        p1, c1 = stops[i + 1]
        if p0 <= t <= p1:
            span = p1 - p0
            return c0 if span == 0 else lerp(c0, c1, (t - p0) / span)
    return stops[-1][1]


def corner_light(size, stops, reach):
    cells = max(2, size // 4)
    small = Image.new("RGB", (cells, cells))
    px = small.load()
    for y in range(cells):
        fy = y / (cells - 1)
        for x in range(cells):
            fx = x / (cells - 1)
            px[x, y] = sample(stops, math.hypot(fx, fy) / reach)
    return small.resize((size, size), Image.BICUBIC)


def corner_rim(big, scale, plate_radius):
    inset = geo.RIM_INSET * scale
    edge = Image.new("L", (big, big), 0)
    ImageDraw.Draw(edge).rounded_rectangle(
        [inset, inset, big - 1 - inset, big - 1 - inset],
        radius=plate_radius - inset, outline=255, width=max(1, round(geo.RIM_WIDTH * scale))
    )
    fade = corner_light(big, [(t, (a, a, a)) for t, a in geo.RIM_ALPHA], 1.0).convert("L")
    return ImageChops.multiply(edge, fade)


def _disk(size, x, y, radius):
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([x - radius, y - radius, x + radius, y + radius], fill=255)
    return mask


def _polygon(size, points):
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).polygon(points, fill=255)
    return mask


def _ellipse_points(f, rx, ry, start, end, steps=900):
    a = math.radians(geo.RING_ROT_DEG)
    out = []
    for i in range(steps + 1):
        t = start + (end - start) * i / steps
        x, y = rx * math.cos(t), ry * math.sin(t)
        out.append(f.p(x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)))
    return out


def _rotated(f, x, y):
    a = math.radians(geo.RING_ROT_DEG)
    return f.p(x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a))


def paint_mark(canvas, size, f, with_orbit=True):
    cx, cy = f.p(0, 0)
    outer = _disk(size, cx, cy, f.n(geo.OUTER))
    body = ImageChops.subtract(outer, _disk(size, cx, cy, f.n(geo.INNER)))
    far = f.n(geo.FAR)
    a = math.radians(geo.OPEN_DEG)
    wedge = _polygon(size, [(cx, cy), (cx + far * math.cos(a), cy - far * math.sin(a)), (cx + far * math.cos(a), cy + far * math.sin(a))])
    body = ImageChops.subtract(body, wedge)
    cut = geo.RING_WIDTH / 2 + geo.RING_GAP
    far_x = geo.RING_RX + geo.FAR

    def side(grow, sign):
        arc = _ellipse_points(f, geo.RING_RX + grow, geo.RING_RY + grow, 0, math.pi)
        return _polygon(size, [_rotated(f, far_x, 0)] + arc + [_rotated(f, -far_x, 0), _rotated(f, -far_x, sign * geo.FAR), _rotated(f, far_x, sign * geo.FAR)])

    day = ImageChops.multiply(body, side(-cut, -1))
    night = ImageChops.multiply(body, side(cut, 1))
    if with_orbit:
        half = geo.RING_WIDTH / 2
        ring = _polygon(size, _ellipse_points(f, geo.RING_RX + half, geo.RING_RY + half, 0, 2 * math.pi))
        ring = ImageChops.subtract(ring, _polygon(size, _ellipse_points(f, geo.RING_RX - half, geo.RING_RY - half, 0, 2 * math.pi)))
        visible = ImageChops.subtract(ImageChops.invert(outer), wedge)
        canvas.paste(Image.new("RGBA", (size, size), geo.RING + (255,)), (0, 0), ImageChops.multiply(ring, visible))
    canvas.paste(Image.new("RGBA", (size, size), geo.LIT + (255,)), (0, 0), day)
    canvas.paste(Image.new("RGBA", (size, size), geo.NIGHT + (255,)), (0, 0), night)
    return canvas


def _frame(big, ratio):
    radius = geo.RING_RX + geo.RING_WIDTH / 2
    scale = big * ratio / (2 * radius)
    return geo.Frame(scale, big / 2 - geo.CX * scale, big / 2 - geo.CY * scale)


def build(size, detailed=True, rounded=True):
    scale = size * SS / SIZE
    big = size * SS

    plate_radius = (PLATE_RADIUS if detailed else PLATE_RADIUS_SMALL) * scale
    plate_mask = Image.new("L", (big, big), 0 if rounded else 255)
    if rounded:
        ImageDraw.Draw(plate_mask).rounded_rectangle(
            [0, 0, big - 1, big - 1], radius=plate_radius, fill=255
        )

    plate = corner_light(big, geo.PLATE_LIGHT, geo.PLATE_LIGHT_REACH)

    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    canvas.paste(plate.convert("RGBA"), (0, 0), plate_mask)

    if detailed and rounded and size >= 128:
        rim = corner_rim(big, scale, plate_radius)
        canvas.paste(Image.new("RGBA", (big, big), (255, 255, 255, 255)), (0, 0), rim)

    canvas = paint_mark(canvas, big, _frame(big, 0.97 if detailed else 1.08), with_orbit=detailed)

    return canvas.resize((size, size), Image.LANCZOS)


def build_mark(size):
    big = size * SS
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    canvas = paint_mark(canvas, big, _frame(big, 0.96))
    return canvas.resize((size, size), Image.LANCZOS)


def icon(size):
    return build(size, detailed=size >= 64)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    for s in (16, 24, 32, 48, 64, 128, 256, 512, 1024):
        icon(s).save(os.path.join(out, f"icon-{s}.png"))
    for s in (108, 432, 512, 1024):
        build_mark(s).save(os.path.join(out, f"mark-{s}.png"))
    for s in (512, 1024):
        build(s, rounded=False).convert("RGB").save(os.path.join(out, f"store-{s}.png"))
    print("rendered to", out)
