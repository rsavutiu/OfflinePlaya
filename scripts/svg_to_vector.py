"""
Convert the ezgifframeNNN.xml browser-SVG files in androidApp/src/main/res/drawable/
to Android VectorDrawable XML in place.

The source files share a structure:
  <svg width=624 height=624>
    <text/>
    <style>.c0{...c3{...}</style>
    <g fill="rgba(255,255,255,0)" transform="scale(0.1)" stroke-width="15">
      <path d="..." class="cN" />
      ...
    </g>
  </svg>

We extract the .cN -> rgb(r,g,b) map from <style>, then emit a <vector> with
viewportWidth/Height = 6240 (so scale(0.1) is baked in) and one <path> per source
path with android:pathData, fillColor, strokeColor and strokeWidth=15.
"""

import re
from pathlib import Path
from xml.sax.saxutils import escape

DRAWABLE_DIR = Path(
    __file__).resolve().parent.parent / "androidApp" / "src" / "main" / "res" / "drawable"

CLASS_RE = re.compile(
    r"\.(c\d+)\s*\{\s*stroke:\s*rgb\((\d+),\s*(\d+),\s*(\d+)\);\s*fill:\s*rgb\((\d+),\s*(\d+),\s*(\d+)\);\s*\}")
PATH_RE = re.compile(r'<path\s+d="([^"]+)"\s+class="(c\d+)"\s*/>', re.DOTALL)
STYLE_RE = re.compile(r"<style>(.*?)</style>", re.DOTALL)


def rgb_to_hex(r, g, b):
    return f"#{int(r):02X}{int(g):02X}{int(b):02X}"


def convert(text: str) -> str:
    style_match = STYLE_RE.search(text)
    if not style_match:
        raise ValueError("no <style> block")
    classes = {}
    for m in CLASS_RE.finditer(style_match.group(1)):
        name = m.group(1)
        # stroke and fill turn out to be the same in every class; we use fill.
        classes[name] = rgb_to_hex(m.group(5), m.group(6), m.group(7))

    paths = PATH_RE.findall(text)
    if not paths:
        raise ValueError("no <path> elements")

    lines = [
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="624dp"',
        '    android:height="624dp"',
        '    android:viewportWidth="6240"',
        '    android:viewportHeight="6240">',
    ]
    for d, cls in paths:
        color = classes.get(cls, "#000000")
        # Collapse all whitespace runs in pathData to single spaces; AAPT is
        # picky about newlines inside attribute values on some versions.
        d_clean = re.sub(r"\s+", " ", d).strip()
        lines.append(
            f'    <path android:pathData="{escape(d_clean)}" '
            f'android:fillColor="{color}" '
            f'android:strokeColor="{color}" '
            f'android:strokeWidth="15" />'
        )
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def main():
    frame_files = sorted(DRAWABLE_DIR.glob("ezgifframe*.xml"))
    if not frame_files:
        print("no frames found")
        return
    for f in frame_files:
        src = f.read_text(encoding="utf-8")
        try:
            out = convert(src)
        except Exception as e:
            print(f"FAIL {f.name}: {e}")
            continue
        f.write_text(out, encoding="utf-8")
        print(f"ok   {f.name}")


if __name__ == "__main__":
    main()
