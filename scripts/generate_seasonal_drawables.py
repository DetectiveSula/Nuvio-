#!/usr/bin/env python3
"""Generate the seasonal profile-picker decoration drawables.

Reads scripts/seasonal_themes.json (decorations laid out on a 390x844 phone canvas) and writes
one Android vector drawable per theme to
composeApp/src/commonMain/composeResources/drawable/seasonal_<theme>.xml.

Run from the repository root:  python3 scripts/generate_seasonal_drawables.py
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "scripts", "seasonal_themes.json")
OUT_DIR = os.path.join(ROOT, "composeApp", "src", "commonMain", "composeResources", "drawable")
CANVAS_W, CANVAS_H = 390, 844


def circle(cx, cy, r):
    return f"M{cx - r},{cy}a{r},{r} 0 1,0 {2 * r},0a{r},{r} 0 1,0 {-2 * r},0z"


def ellipse(cx, cy, rx, ry):
    return f"M{cx - rx},{cy}a{rx},{ry} 0 1,0 {2 * rx},0a{rx},{ry} 0 1,0 {-2 * rx},0z"


def rect(x, y, w, h):
    return f"M{x},{y}h{w}v{h}h{-w}z"


# Each shape: (viewBox width, viewBox height, [parts]).
# A part is (kind, pathData, color, alpha, strokeWidth) where kind is "fill" or "stroke" and
# color None means "the decoration's own color".
SHAPES = {
    "snow": (24, 24, [("stroke", "M12 2v20M2 12h20M5 5l14 14M19 5 5 19M9 3.5l3 2.5 3-2.5M9 20.5l3-2.5 3 2.5M3.5 9l2.5 3-2.5 3M20.5 9 18 12l2.5 3", None, 1, 1.5)]),
    "star": (24, 24, [("fill", "M12 2l2.9 6.6 7.1.7-5.4 4.7 1.6 7-6.2-3.7L5.8 21l1.6-7L2 9.3l7.1-.7z", None, 1, 0)]),
    "sparkle": (24, 24, [("fill", "M12 2c.8 6 4 9.2 10 10-6 .8-9.2 4-10 10-.8-6-4-9.2-10-10 6-.8 9.2-4 10-10z", None, 1, 0)]),
    "crescent": (24, 24, [("fill", "M15 3a9 9 0 1 0 6 15.5A7.5 7.5 0 1 1 15 3z", None, 1, 0)]),
    "lantern": (24, 36, [
        ("stroke", "M12 0v5", None, 1, 1),
        ("fill", rect(8, 5, 8, 3), None, 1, 0),
        ("fill", "M7 9h10l2 6v9l-2 6H7l-2-6v-9z", None, 1, 0),
        ("fill", rect(9, 13, 6, 12), "#FFF1C2", 0.85, 0),
        ("fill", rect(10, 30, 4, 3), None, 1, 0),
    ]),
    "ornament": (24, 36, [
        ("stroke", "M12 0v10", "#E9D8A6", 1, 0.8),
        ("fill", rect(10, 9, 4, 3), "#C9A227", 1, 0),
        ("fill", circle(12, 22, 9), None, 1, 0),
        ("fill", circle(9, 19, 2), "#FFFFFF", 0.45, 0),
    ]),
    "tree": (24, 25, [
        ("fill", "M12 2l6 8h-3l5 6h-5l6 6H3l6-6H4l5-6H6z", None, 1, 0),
        ("fill", rect(10.5, 22, 3, 3), "#6B4423", 1, 0),
        ("fill", "M12 0l.9 2 2.1.2-1.6 1.4.5 2.1L12 4.6 10.1 5.7l.5-2.1L9 2.2 11.1 2z", "#FFD45C", 1, 0),
    ]),
    "pumpkin": (24, 24, [
        ("fill", rect(11, 3, 2, 4), "#5B7F2A", 1, 0),
        ("fill", ellipse(7.5, 14, 5, 7), None, 1, 0),
        ("fill", ellipse(16.5, 14, 5, 7), None, 1, 0),
        ("fill", ellipse(12, 14, 5.5, 7.5), None, 1, 0),
        ("fill", "M8 12l2-2 1 3zM13 13l1-3 2 2zM8 16q4 3 8 0l-1 2q-3 1.5-6 0z", "#2A1405", 1, 0),
    ]),
    "bat": (24, 24, [("fill", "M12 9c-1 0-1.5 1-1.5 1L8 8c0 2-1 3-3 3 1 1 1 3 0 4 2-1 4 0 5 1l2-2 2 2c1-1 3-2 5-1-1-1-1-3 0-4-2 0-3-1-3-3l-2.5 2S13 9 12 9z", None, 1, 0)]),
    "ghost": (24, 24, [
        ("fill", "M12 3a7 7 0 0 0-7 7v10l2.3-1.6L9.7 20l2.3-1.6 2.3 1.6 2.4-1.6L19 20V10a7 7 0 0 0-7-7z", None, 1, 0),
        ("fill", circle(9.5, 10, 1.2) + circle(14.5, 10, 1.2), "#1A0F24", 1, 0),
    ]),
    "egg": (24, 24, [
        ("fill", ellipse(12, 13, 7, 9), None, 1, 0),
        ("stroke", "M5.5 12l2.2-2 2.2 2 2.1-2 2.2 2 2.1-2 2.2 2", "#FFFFFF", 1, 1.4),
        ("fill", circle(9, 17, 1) + circle(15, 17, 1), "#FFFFFF", 1, 0),
    ]),
    "flower": (24, 24, [
        ("fill", circle(12, 6.5, 4) + circle(17.2, 10.3, 4) + circle(15.2, 16.5, 4) + circle(8.8, 16.5, 4) + circle(6.8, 10.3, 4), None, 1, 0),
        ("fill", circle(12, 12, 3), "#FFD45C", 1, 0),
    ]),
    "leaf": (24, 24, [("fill", "M20 4C10 4 4 9 4 16c0 2 .5 3 1 4 1-4 4-8 9-10-4 3-6 6-7 10 9 0 13-7 13-16z", None, 1, 0)]),
    "sun": (24, 24, [
        ("fill", circle(12, 12, 5), None, 1, 0),
        ("stroke", "M12 1v3M12 20v3M1 12h3M20 12h3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1", None, 1, 1.8),
    ]),
    "cloud": (24, 24, [("fill", "M7 18a4 4 0 0 1-.5-8A5.5 5.5 0 0 1 17 8.5a4 4 0 0 1 1 9.5z", None, 1, 0)]),
    "firework": (24, 24, [
        ("stroke", "M12 2v5M12 17v5M2 12h5M17 12h5M4.9 4.9l3.5 3.5M15.6 15.6l3.5 3.5M4.9 19.1l3.5-3.5M15.6 8.4l3.5-3.5", None, 1, 1.3),
        ("fill", circle(12, 12, 1.2), None, 1, 0),
    ]),
    "confetti": (10, 18, [("fill", rect(0, 0, 10, 18), None, 1, 0)]),
    "wave": (120, 24, [("fill", "M0 12Q15 2 30 12T60 12T90 12T120 12V24H0z", None, 1, 0)]),
    "mosque": (120, 40, [
        ("fill", "M0 40V30h8V12l2-6 2 6v18h18v-4a14 14 0 0 1 28 0v4h4v-8a12 12 0 0 1 24 0v8h4v-4a14 14 0 0 1 28 0v4h0z", None, 1, 0),
        ("fill", "M104 30V12l2-6 2 6v18h12v10H96V30z", None, 1, 0),
    ]),
    "disc": (24, 24, [("fill", circle(12, 12, 12), None, 1, 0)]),
    "palm": (24, 48, [("fill", "M12 48C12 34 10 24 13 14h1.5C12.5 24 14 34 14.5 48zM13 14C8 10 4 11 1 14c4-5 8-6 12-1zM13.5 14c4.5-4 8.5-3 9.5 1-4-5-7-6-9.5-1.5zM13 13c-2-5-6-8-10-8 5-2 9 1 10.5 7zM13.5 13c1.5-5 5.5-8 9.5-7-5-3-9 0-10.5 6zM13 13c0-5-1-9-3-12 4 2 4.5 7 3.6 12z", None, 1, 0)]),
    "tulip": (24, 40, [
        ("fill", rect(11.3, 16, 1.4, 24), "#2D6A4F", 1, 0),
        ("fill", "M12 30c-5-4-7-8-7-12 3 3 5 6 7 10z", "#40916C", 1, 0),
        ("fill", "M6 8c0 6 2.5 9 6 9s6-3 6-9l-3 3-3-5-3 5z", None, 1, 0),
    ]),
    "grass": (120, 30, [("fill", "M0 30V16l3-10 3 10 3-12 3 12 4-8 3 8 3-13 3 13 4-9 3 9 3-11 3 11 4-8 3 8 3-12 3 12 4-10 3 10 3-12 3 12 4-8 3 8 3-11 3 11 4-9 3 9 3-12 3 12 4-10 3 10 3-8 3 8 4-12 3 12H120V30z", None, 1, 0)]),
    "rain": (24, 24, [("stroke", "M14 2l-3 8M20 8l-3 8M8 12l-3 8", None, 1, 1.6)]),
    "bird": (24, 12, [("stroke", "M1 9Q6 2 12 9Q18 2 23 9", None, 1, 1.8)]),
    "bunny": (24, 24, [
        ("fill", "M9 1c-1.6 0-2 3.5-1.4 6.5.3 1.4.8 2.4 1.3 3C6.4 11.6 5 14 5 16.8 5 20 7.4 22 10.5 22h4.2c2.8 0 4.3-1.8 4.3-4.3 0-3-2-5.5-4.6-6.6.5-.6 1-1.6 1.3-3C16.3 5 15.6 1 14 1s-2 3.4-2 6c0 .4 0 .8.1 1.1h-1.2c.1-.3.1-.7.1-1.1 0-2.6-.4-6-2-6z", None, 1, 0),
        ("fill", circle(9, 14, 0.9), "#1E2C45", 1, 0),
    ]),
}


def fmt(value):
    text = f"{value:.4f}".rstrip("0").rstrip(".")
    return "0" if text in ("-0", "") else text


def decoration_xml(d):
    vbw, vbh, parts = SHAPES[d["k"]]
    sx, sy = d["w"] / vbw, d["h"] / vbh
    tx = d["x"] + d["w"] / 2 - vbw / 2
    ty = d["y"] + d["h"] / 2 - vbh / 2
    lines = [
        f'    <group android:pivotX="{fmt(vbw / 2)}" android:pivotY="{fmt(vbh / 2)}"'
        f' android:scaleX="{fmt(sx)}" android:scaleY="{fmt(sy)}" android:rotation="{fmt(d["r"])}"'
        f' android:translateX="{fmt(tx)}" android:translateY="{fmt(ty)}">'
    ]
    for kind, path, color, alpha, stroke_width in parts:
        color = color or d["c"]
        alpha = fmt(alpha * d["o"])
        if kind == "fill":
            lines.append(f'        <path android:pathData="{path}" android:fillColor="{color}" android:fillAlpha="{alpha}" />')
        else:
            lines.append(
                f'        <path android:pathData="{path}" android:strokeColor="{color}" android:strokeAlpha="{alpha}"'
                f' android:strokeWidth="{fmt(stroke_width)}" android:strokeLineCap="round" android:strokeLineJoin="round" />'
            )
    lines.append("    </group>")
    return "\n".join(lines)


def theme_file_name(label):
    return "seasonal_" + re.sub(r"[^a-z0-9]+", "_", label.lower()).strip("_") + ".xml"


def main():
    with open(DATA) as f:
        themes = json.load(f)
    for theme in themes:
        body = "\n".join(decoration_xml(d) for d in theme["decor"])
        xml = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            f'<!-- Generated by scripts/generate_seasonal_drawables.py from scripts/seasonal_themes.json ({theme["label"]}). -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{CANVAS_W}dp"\n    android:height="{CANVAS_H}dp"\n'
            f'    android:viewportWidth="{CANVAS_W}"\n    android:viewportHeight="{CANVAS_H}">\n'
            f"{body}\n</vector>\n"
        )
        path = os.path.join(OUT_DIR, theme_file_name(theme["label"]))
        with open(path, "w") as out:
            out.write(xml)
        print(f"wrote {os.path.relpath(path, ROOT)} ({len(theme['decor'])} decorations)")


if __name__ == "__main__":
    main()
