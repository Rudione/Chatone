import math
import os
import sys
from PIL import Image, ImageDraw

SIZE = 1024
SS = 4
CENTER = SIZE / 2
RING_RADIUS = 340.0
RING_WIDTH = 150.0
GAP_HALF_ANGLE = 44.0
PLATE_RADIUS = 228
PLATE_RADIUS_SMALL = 212

RING_STOPS = [
    (0.00, (217, 162, 255)),
    (0.38, (160, 99, 255)),
    (0.70, (109, 58, 240)),
    (1.00, (43, 127, 255)),
]
RING_STOPS_SMALL = [
    (0.00, (220, 168, 255)),
    (0.45, (155, 92, 255)),
    (1.00, (59, 140, 255)),
]
PLATE_STOPS = [
    (0.00, (34, 27, 51)),
    (0.55, (20, 16, 32)),
    (1.00, (10, 8, 16)),
]
PLATE_FLAT = (20, 16, 32)


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


def linear_gradient(size, stops, start, end):
    step = 4
    small = Image.new("RGB", (size // step, size // step))
    px = small.load()
    ax, ay = start
    bx, by = end
    dx, dy = bx - ax, by - ay
    denom = dx * dx + dy * dy
    for y in range(small.height):
        fy = y / (small.height - 1)
        for x in range(small.width):
            fx = x / (small.width - 1)
            t = ((fx - ax) * dx + (fy - ay) * dy) / denom
            px[x, y] = sample(stops, t)
    return small.resize((size, size), Image.BICUBIC)


def ring_mask(size, radius, width, scale):
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    r = radius * scale
    w = width * scale
    c = size / 2
    box = [c - r, c - r, c + r, c + r]
    draw.arc(box, start=GAP_HALF_ANGLE, end=360 - GAP_HALF_ANGLE, fill=255, width=round(w))
    for sign in (-1, 1):
        a = math.radians(GAP_HALF_ANGLE * sign)
        ex = c + r * math.cos(a)
        ey = c + r * math.sin(a)
        draw.ellipse([ex - w / 2, ey - w / 2, ex + w / 2, ey + w / 2], fill=255)
    return mask


def sheen_layer(size, scale):
    layer = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(layer)
    r = (RING_RADIUS + RING_WIDTH * 0.26) * scale
    c = size / 2
    box = [c - r, c - r, c + r, c + r]
    draw.arc(box, start=196, end=300, fill=110, width=round(26 * scale))
    return layer


def build(size, detailed=True):
    scale = size * SS / SIZE
    big = size * SS

    plate_radius = (PLATE_RADIUS if detailed else PLATE_RADIUS_SMALL) * scale
    plate_mask = Image.new("L", (big, big), 0)
    ImageDraw.Draw(plate_mask).rounded_rectangle(
        [0, 0, big - 1, big - 1], radius=plate_radius, fill=255
    )

    if detailed:
        plate = linear_gradient(big, PLATE_STOPS, (0.2, 0.0), (0.85, 1.0))
    else:
        plate = Image.new("RGB", (big, big), PLATE_FLAT)

    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    canvas.paste(plate.convert("RGBA"), (0, 0), plate_mask)

    stops = RING_STOPS if detailed else RING_STOPS_SMALL
    ring_w = RING_WIDTH if detailed else 168.0
    ring_r = RING_RADIUS if detailed else 330.0
    ring = linear_gradient(big, stops, (0.06, 0.04), (0.90, 0.96)).convert("RGBA")
    canvas.paste(ring, (0, 0), ring_mask(big, ring_r, ring_w, scale))

    if detailed and size >= 128:
        sheen = sheen_layer(big, scale)
        white = Image.new("RGBA", (big, big), (255, 255, 255, 255))
        combined = Image.new("L", (big, big), 0)
        combined.paste(sheen, (0, 0), ring_mask(big, ring_r, ring_w, scale))
        canvas.paste(white, (0, 0), combined)

        edge = Image.new("L", (big, big), 0)
        ImageDraw.Draw(edge).rounded_rectangle(
            [4 * scale, 4 * scale, big - 1 - 4 * scale, big - 1 - 4 * scale],
            radius=plate_radius - 3 * scale, outline=20, width=round(9 * scale)
        )
        canvas.paste(Image.new("RGBA", (big, big), (255, 255, 255, 255)), (0, 0), edge)

    return canvas.resize((size, size), Image.LANCZOS)


def build_mark(size):
    scale = size * SS / SIZE
    big = size * SS
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    ring = linear_gradient(big, RING_STOPS, (0.06, 0.04), (0.90, 0.96)).convert("RGBA")
    canvas.paste(ring, (0, 0), ring_mask(big, RING_RADIUS, RING_WIDTH, scale))
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
    print("rendered to", out)
