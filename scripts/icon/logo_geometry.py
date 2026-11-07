import math

PLATE = (7, 7, 8)
PLATE_LIGHT = [
    (0.00, (44, 45, 48)),
    (0.32, (21, 22, 24)),
    (0.75, (9, 9, 10)),
    (1.00, PLATE),
]
PLATE_LIGHT_REACH = 1.05
RIM_ALPHA = [(0.00, 70), (0.55, 17), (1.00, 0)]
RIM_INSET = 3.0
RIM_WIDTH = 5.0
LIT = (128, 131, 217)
NIGHT = (64, 62, 166)
RING = (226, 227, 255)

CX, CY = 516.0, 512.0
OUTER = 430.0
INNER = 200.0
OPEN_DEG = 36.0
RING_RX = 495.0
RING_RY = 185.0
RING_ROT_DEG = -42.0
RING_WIDTH = 30.0
RING_GAP = 12.0
FAR = 4000.0


def hex_color(rgb):
    return "#%02X%02X%02X" % rgb


class Frame:
    def __init__(self, scale=1.0, dx=0.0, dy=0.0):
        self.scale = scale
        self.dx = dx
        self.dy = dy

    def p(self, x, y):
        return (CX + x) * self.scale + self.dx, (CY + y) * self.scale + self.dy

    def n(self, v):
        return v * self.scale


def _fmt(v):
    s = ("%.2f" % v).rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def _pt(xy):
    return "%s %s" % (_fmt(xy[0]), _fmt(xy[1]))


def _rot(x, y):
    a = math.radians(RING_ROT_DEG)
    return x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)


def _polar(radius, degrees):
    a = math.radians(degrees)
    return radius * math.cos(a), -radius * math.sin(a)


def body_path(f):
    o1 = f.p(*_polar(OUTER, OPEN_DEG))
    o2 = f.p(*_polar(OUTER, -OPEN_DEG))
    i2 = f.p(*_polar(INNER, -OPEN_DEG))
    i1 = f.p(*_polar(INNER, OPEN_DEG))
    R, r = f.n(OUTER), f.n(INNER)
    return (f"M{_pt(o1)}A{_fmt(R)} {_fmt(R)} 0 1 0 {_pt(o2)}"
            f"L{_pt(i2)}A{_fmt(r)} {_fmt(r)} 0 1 1 {_pt(i1)}Z")


def _front_arc(f, grow):
    rx, ry = RING_RX + grow, RING_RY + grow
    start = f.p(*_rot(rx, 0))
    end = f.p(*_rot(-rx, 0))
    return start, end, f.n(rx), f.n(ry)


def lit_clip_path(f):
    cut = RING_WIDTH / 2 + RING_GAP
    start, end, rx, ry = _front_arc(f, -cut)
    far_start = f.p(*_rot(RING_RX + FAR, 0))
    far_end = f.p(*_rot(-RING_RX - FAR, 0))
    up_a = f.p(*_rot(-RING_RX - FAR, -FAR))
    up_b = f.p(*_rot(RING_RX + FAR, -FAR))
    return (f"M{_pt(far_start)}L{_pt(start)}A{_fmt(rx)} {_fmt(ry)} {_fmt(RING_ROT_DEG)} 0 1 {_pt(end)}"
            f"L{_pt(far_end)}L{_pt(up_a)}L{_pt(up_b)}Z")


def night_clip_path(f):
    cut = RING_WIDTH / 2 + RING_GAP
    start, end, rx, ry = _front_arc(f, cut)
    far_start = f.p(*_rot(RING_RX + FAR, 0))
    far_end = f.p(*_rot(-RING_RX - FAR, 0))
    down_a = f.p(*_rot(-RING_RX - FAR, FAR))
    down_b = f.p(*_rot(RING_RX + FAR, FAR))
    return (f"M{_pt(far_start)}L{_pt(start)}A{_fmt(rx)} {_fmt(ry)} {_fmt(RING_ROT_DEG)} 0 1 {_pt(end)}"
            f"L{_pt(far_end)}L{_pt(down_a)}L{_pt(down_b)}Z")


def ring_path(f):
    a = f.p(*_rot(RING_RX, 0))
    b = f.p(*_rot(-RING_RX, 0))
    rx, ry = f.n(RING_RX), f.n(RING_RY)
    rot = _fmt(RING_ROT_DEG)
    return f"M{_pt(a)}A{_fmt(rx)} {_fmt(ry)} {rot} 1 1 {_pt(b)}A{_fmt(rx)} {_fmt(ry)} {rot} 1 1 {_pt(a)}"


def ring_clip_path(f, extent):
    o1 = f.p(*_polar(OUTER, OPEN_DEG))
    o2 = f.p(*_polar(OUTER, -OPEN_DEG))
    ray1 = f.p(*_polar(FAR, OPEN_DEG))
    ray2 = f.p(*_polar(FAR, -OPEN_DEG))
    R = f.n(OUTER)
    x0, y0, x1, y1 = extent
    frame = f"M{_fmt(x0)} {_fmt(y0)}L{_fmt(x0)} {_fmt(y1)}L{_fmt(x1)} {_fmt(y1)}L{_fmt(x1)} {_fmt(y0)}Z"
    hole = f"M{_pt(o1)}L{_pt(ray1)}L{_pt(ray2)}L{_pt(o2)}A{_fmt(R)} {_fmt(R)} 0 1 1 {_pt(o1)}Z"
    return frame + hole


def ring_stroke_width(f):
    return f.n(RING_WIDTH)
