import argparse
import io
import re
import sys
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
APP_SOURCES = ROOT / "composeApp" / "src"
MODULE_SOURCES = ROOT / "core" / "icons" / "src" / "commonMain" / "kotlin"
BASE_PACKAGE = "io.rudione.chatone.icons"
MATERIAL_PACKAGE = BASE_PACKAGE + ".material"
LUCIDE_PACKAGE = BASE_PACKAGE + ".lucide"
CACHE = ROOT / "build" / "icon-sources"

MAVEN = "https://repo1.maven.org/maven2"
MATERIAL_JARS = [
    "org/jetbrains/compose/material/material-icons-core-desktop/1.7.3/material-icons-core-desktop-1.7.3-sources.jar",
    "org/jetbrains/compose/material/material-icons-extended-desktop/1.7.3/material-icons-extended-desktop-1.7.3-sources.jar",
]
LUCIDE_JAR = "com/composables/icons-lucide-cmp/2.2.1/icons-lucide-cmp-2.2.1-sources.jar"

STYLE_PACKAGES = {
    "Filled": "filled",
    "Default": "filled",
    "Outlined": "outlined",
    "Rounded": "rounded",
    "Sharp": "sharp",
    "TwoTone": "twotone",
    "AutoMirrored.Filled": "automirrored.filled",
    "AutoMirrored.Default": "automirrored.filled",
    "AutoMirrored.Outlined": "automirrored.outlined",
    "AutoMirrored.Rounded": "automirrored.rounded",
    "AutoMirrored.Sharp": "automirrored.sharp",
    "AutoMirrored.TwoTone": "automirrored.twotone",
}

MATERIAL_REF = re.compile(
    r"\bIcons\.(AutoMirrored\.(?:Filled|Outlined|Rounded|Sharp|TwoTone|Default)|Filled|Outlined|Default|Rounded|Sharp|TwoTone)\.([A-Z_][A-Za-z0-9_]*)"
)
LUCIDE_REF = re.compile(r"\bLucide\.([A-Z][A-Za-z0-9]*)")

ICONS_OBJECT = f"""package {MATERIAL_PACKAGE}

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object Icons {{
    object Filled

    object Outlined

    object Rounded

    object TwoTone

    object Sharp

    object AutoMirrored {{
        object Filled

        object Outlined

        object Rounded

        object TwoTone

        object Sharp

        val Default = Filled
    }}

    val Default = Filled
}}

inline fun materialIcon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> ImageVector.Builder
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = MaterialIconDimension.dp,
    defaultHeight = MaterialIconDimension.dp,
    viewportWidth = MaterialIconDimension,
    viewportHeight = MaterialIconDimension,
    autoMirror = autoMirror
).block().build()

inline fun ImageVector.Builder.materialPath(
    fillAlpha: Float = 1f,
    strokeAlpha: Float = 1f,
    pathFillType: PathFillType = DefaultFillType,
    pathBuilder: PathBuilder.() -> Unit
) = path(
    fill = SolidColor(Color.Black),
    fillAlpha = fillAlpha,
    stroke = null,
    strokeAlpha = strokeAlpha,
    strokeLineWidth = 1f,
    strokeLineCap = StrokeCap.Butt,
    strokeLineJoin = StrokeJoin.Bevel,
    strokeLineMiter = 1f,
    pathFillType = pathFillType,
    pathBuilder = pathBuilder
)

@PublishedApi
internal const val MaterialIconDimension = 24f
"""

LUCIDE_OBJECT = f"""package {LUCIDE_PACKAGE}

object Lucide
"""


def fetch(relative: str) -> zipfile.ZipFile:
    target = CACHE / Path(relative).name
    if not target.exists():
        target.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(f"{MAVEN}/{relative}", timeout=120) as response:
            target.write_bytes(response.read())
    return zipfile.ZipFile(io.BytesIO(target.read_bytes()))


def strip_comments(source: str) -> str:
    source = re.sub(r"/\*.*?\*/", "", source, flags=re.S)
    source = re.sub(r"^\s*//.*$", "", source, flags=re.M)
    return source


def strip_deprecations(source: str) -> str:
    out = []
    i = 0
    while True:
        start = source.find("@Deprecated(", i)
        if start < 0:
            out.append(source[i:])
            break
        out.append(source[i:start])
        depth = 0
        j = start + len("@Deprecated")
        in_string = False
        while j < len(source):
            ch = source[j]
            if ch == '"' and source[j - 1] != "\\":
                in_string = not in_string
            elif not in_string:
                if ch == "(":
                    depth += 1
                elif ch == ")":
                    depth -= 1
                    if depth == 0:
                        j += 1
                        break
            j += 1
        i = j
    return "".join(out).replace("import kotlin.Deprecated\n", "")


def tidy(source: str) -> str:
    source = re.sub(r"[ \t]+$", "", source, flags=re.M)
    source = re.sub(r"\n{3,}", "\n\n", source)
    return source.strip() + "\n"


def scan_app():
    material, lucide = set(), set()
    for path in APP_SOURCES.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        for style, name in MATERIAL_REF.findall(text):
            material.add((STYLE_PACKAGES[style], name))
        lucide.update(LUCIDE_REF.findall(text))
    return material, lucide


def material_sources():
    files = {}
    for jar in MATERIAL_JARS:
        archive = fetch(jar)
        for entry in archive.namelist():
            match = re.match(r"commonMain/androidx/compose/material/icons/(.+)/([A-Za-z0-9_]+)\.kt$", entry)
            if match:
                files[(match.group(1).replace("/", "."), match.group(2))] = archive.read(entry).decode("utf-8")
    return files


def lucide_sources():
    archive = fetch(LUCIDE_JAR)
    files = {}
    for entry in archive.namelist():
        if not entry.endswith(".kt"):
            continue
        text = archive.read(entry).decode("utf-8")
        match = re.search(r"^val Lucide\.([A-Za-z0-9]+):", text, flags=re.M)
        if match:
            files[match.group(1)] = text
    return files


def convert_material(source: str, style_package: str) -> str:
    source = strip_deprecations(strip_comments(source))
    source = re.sub(
        r"^package androidx\.compose\.material\.icons\.[a-z.]+$",
        f"package {MATERIAL_PACKAGE}.{style_package}",
        source,
        flags=re.M,
    )
    source = source.replace("import androidx.compose.material.icons.", f"import {MATERIAL_PACKAGE}.")
    return tidy(source)


def convert_lucide(source: str) -> str:
    source = strip_comments(source)
    source = source.replace("package com.composables.icons.lucide", f"package {LUCIDE_PACKAGE}")
    return tidy(source)


def write_if_changed(path: Path, content: str) -> bool:
    if path.exists() and path.read_text(encoding="utf-8") == content:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return True


def drop_unused_icon_imports(text: str, material, lucide) -> str:
    def keep(match):
        package, name = match.group(1), match.group(2)
        if package in ("Icons", "materialIcon", "materialPath") or name == "*":
            return match.group(0)
        return match.group(0) if (package, name) in material else ""

    text = re.sub(
        r"^import " + re.escape(MATERIAL_PACKAGE) + r"\.([a-z.]+)\.([A-Za-z0-9_*]+)\n",
        keep,
        text,
        flags=re.M,
    )
    return re.sub(
        r"^import " + re.escape(LUCIDE_PACKAGE) + r"\.([A-Za-z0-9]+)\n",
        lambda match: match.group(0) if match.group(1) == "Lucide" or match.group(1) in lucide else "",
        text,
        flags=re.M,
    )


def rewrite_imports(style_packages, material=frozenset(), lucide=frozenset()) -> int:
    changed = 0
    for path in APP_SOURCES.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        updated = re.sub(
            r"^import androidx\.compose\.material\.icons\.",
            f"import {MATERIAL_PACKAGE}.",
            text,
            flags=re.M,
        )
        updated = re.sub(r"^import com\.composables\.icons\.lucide\.", f"import {LUCIDE_PACKAGE}.", updated, flags=re.M)
        updated = re.sub(
            r"^import " + re.escape(MATERIAL_PACKAGE) + r"\.([a-z.]+)\.\*\n",
            lambda match: match.group(0) if match.group(1) in style_packages else "",
            updated,
            flags=re.M,
        )
        updated = drop_unused_icon_imports(updated, material, lucide)
        if updated != text:
            path.write_text(updated, encoding="utf-8")
            changed += 1
    return changed


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Copies only the Material and Lucide icons referenced by composeApp into the :core:icons module."
    )
    parser.add_argument("--check", action="store_true", help="fail if the vendored icons are out of date")
    args = parser.parse_args()

    material, lucide = scan_app()
    material_files = material_sources()
    lucide_files = lucide_sources()

    expected = {}
    material_root = MODULE_SOURCES / Path(*MATERIAL_PACKAGE.split("."))
    lucide_root = MODULE_SOURCES / Path(*LUCIDE_PACKAGE.split("."))
    expected[material_root / "Icons.kt"] = ICONS_OBJECT
    expected[lucide_root / "Lucide.kt"] = LUCIDE_OBJECT

    missing = []
    for style_package, name in sorted(material):
        source = material_files.get((style_package, name))
        if source is None:
            missing.append(f"Material {style_package}.{name}")
            continue
        target = material_root / Path(*style_package.split(".")) / f"{name}.kt"
        expected[target] = convert_material(source, style_package)
    for name in sorted(lucide):
        source = lucide_files.get(name)
        if source is None:
            missing.append(f"Lucide {name}")
            continue
        expected[lucide_root / f"{name}.kt"] = convert_lucide(source)

    if missing:
        print("Unknown icons: " + ", ".join(missing), file=sys.stderr)
        return 1

    existing = set(material_root.rglob("*.kt")) | set(lucide_root.rglob("*.kt"))
    stale = sorted(existing - set(expected))
    outdated = [path for path, content in expected.items()
                if not path.exists() or path.read_text(encoding="utf-8") != content]

    if args.check:
        if stale or outdated:
            print(f"Vendored icons are out of date: {len(outdated)} to write, {len(stale)} to delete", file=sys.stderr)
            return 1
        print("Vendored icons are up to date")
        return 0

    for path in stale:
        path.unlink()
    written = sum(write_if_changed(path, content) for path, content in expected.items())
    rewritten = rewrite_imports({style_package for style_package, _ in material}, material, lucide)
    print(f"material={len(material)} lucide={len(lucide)} written={written} deleted={len(stale)} imports={rewritten}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
