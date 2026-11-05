import os
import sys

import logo_geometry as g

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SITE = os.path.expanduser("~/webprojects/ChatoneSite")


def fit_frame(viewport, extent_ratio):
    radius = max(g.RING_RX + g.RING_WIDTH / 2, g.OUTER)
    scale = viewport * extent_ratio / (2 * radius)
    dx = viewport / 2 - g.CX * scale
    dy = viewport / 2 - g.CY * scale
    return g.Frame(scale, dx, dy)


def svg_layers(f, viewport, ids):
    lit, night, ring = (g.hex_color(c) for c in (g.LIT, g.NIGHT, g.RING))
    extent = (0, 0, viewport, viewport)
    defs = (
        f'<clipPath id="{ids}-lit"><path d="{g.lit_clip_path(f)}"/></clipPath>'
        f'<clipPath id="{ids}-night"><path d="{g.night_clip_path(f)}"/></clipPath>'
        f'<clipPath id="{ids}-orbit"><path d="{g.ring_clip_path(f, extent)}"/></clipPath>'
    )
    body = g.body_path(f)
    shapes = (
        f'<path clip-path="url(#{ids}-orbit)" d="{g.ring_path(f)}" fill="none" stroke="{ring}" '
        f'stroke-width="{g._fmt(g.ring_stroke_width(f))}"/>'
        f'<path clip-path="url(#{ids}-lit)" d="{body}" fill="{lit}"/>'
        f'<path clip-path="url(#{ids}-night)" d="{body}" fill="{night}"/>'
    )
    return defs, shapes


def svg_document(viewport, plate_radius=None, ids="c"):
    f = fit_frame(viewport, 0.94 if plate_radius is None else 0.97)
    defs, shapes = svg_layers(f, viewport, ids)
    plate = ""
    if plate_radius is not None:
        plate = f'<rect width="{viewport}" height="{viewport}" rx="{g._fmt(plate_radius)}" fill="{g.hex_color(g.PLATE)}"/>'
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {viewport} {viewport}" '
            f'width="{viewport}" height="{viewport}"><defs>{defs}</defs>{plate}{shapes}</svg>\n')


def inline_svg(viewport, ids, css_class=None):
    f = fit_frame(viewport, 0.94)
    defs, shapes = svg_layers(f, viewport, ids)
    cls = f' class="{css_class}"' if css_class else ""
    return f'<svg{cls} viewBox="0 0 {viewport} {viewport}" focusable="false"><defs>{defs}</defs>{shapes}</svg>'


def android_vector(viewport, extent_ratio):
    f = fit_frame(viewport, extent_ratio)
    body = g.body_path(f)
    extent = (0, 0, viewport, viewport)
    lit, night, ring = ("#FF" + g.hex_color(c)[1:] for c in (g.LIT, g.NIGHT, g.RING))
    return f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{viewport}dp"
    android:height="{viewport}dp"
    android:viewportWidth="{viewport}"
    android:viewportHeight="{viewport}">
    <group android:name="orbit">
        <clip-path android:pathData="{g.ring_clip_path(f, extent)}" />
        <path
            android:pathData="{g.ring_path(f)}"
            android:strokeColor="{ring}"
            android:strokeWidth="{g._fmt(g.ring_stroke_width(f))}" />
    </group>
    <group android:name="day">
        <clip-path android:pathData="{g.lit_clip_path(f)}" />
        <path android:pathData="{body}" android:fillColor="{lit}" />
    </group>
    <group android:name="night">
        <clip-path android:pathData="{g.night_clip_path(f)}" />
        <path android:pathData="{body}" android:fillColor="{night}" />
    </group>
</vector>
'''


def kotlin_mark(viewport):
    f = fit_frame(viewport, 0.96)
    extent = (0, 0, viewport, viewport)
    return f'''package io.rudione.chatone.presentation.components.expressive

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.ChatoneBrand

private const val VIEWPORT = {viewport}f
private const val BODY = "{g.body_path(f)}"
private const val DAY_CLIP = "{g.lit_clip_path(f)}"
private const val NIGHT_CLIP = "{g.night_clip_path(f)}"
private const val ORBIT = "{g.ring_path(f)}"
private const val ORBIT_CLIP = "{g.ring_clip_path(f, extent)}"
private const val ORBIT_WIDTH = {g._fmt(g.ring_stroke_width(f))}f

val ChatoneMark: ImageVector by lazy {{
    ImageVector.Builder(
        name = "ChatoneMark",
        defaultWidth = VIEWPORT.dp,
        defaultHeight = VIEWPORT.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT
    ).apply {{
        addGroup(name = "orbit", clipPathData = addPathNodes(ORBIT_CLIP))
        addPath(pathData = addPathNodes(ORBIT), stroke = SolidColor(ChatoneBrand.Orbit), strokeLineWidth = ORBIT_WIDTH)
        clearGroup()
        addGroup(name = "day", clipPathData = addPathNodes(DAY_CLIP))
        addPath(pathData = addPathNodes(BODY), fill = SolidColor(ChatoneBrand.Accent))
        clearGroup()
        addGroup(name = "night", clipPathData = addPathNodes(NIGHT_CLIP))
        addPath(pathData = addPathNodes(BODY), fill = SolidColor(ChatoneBrand.Night))
        clearGroup()
    }}.build()
}}
'''


def write(path, text):
    with open(path, "w") as out:
        out.write(text)
    print("wrote", os.path.relpath(path, ROOT) if path.startswith(ROOT) else path)


if __name__ == "__main__":
    icon_dir = os.path.join(ROOT, "scripts", "icon")
    write(os.path.join(icon_dir, "chatone-mark.svg"), svg_document(1024))
    write(os.path.join(icon_dir, "chatone-icon.svg"), svg_document(1024, plate_radius=228))
    write(os.path.join(ROOT, "androidApp/src/main/res/drawable/ic_launcher_foreground.xml"), android_vector(108, 0.66))
    write(os.path.join(ROOT, "composeApp/src/commonMain/kotlin/io/rudione/chatone/presentation/components/expressive/ChatoneMark.kt"), kotlin_mark(66))
    if os.path.isdir(SITE) and "--site" in sys.argv:
        write(os.path.join(SITE, "assets/img/favicon.svg"), svg_document(32, plate_radius=7.1, ids="f"))
    print(inline_svg(32, "brand"))
