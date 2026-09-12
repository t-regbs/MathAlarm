#!/usr/bin/env python3
"""Regenerate icon assets from artwork/icon/mathalarm-mark.svg.
Requires Python 3, CairoSVG and Pillow (pip install cairosvg pillow).
"""
from pathlib import Path
import io
import json
import xml.etree.ElementTree as ET
import cairosvg
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'artwork/icon'
SOURCE = ART / 'mathalarm-mark.svg'
MARK = SOURCE.read_text().split('<g', 1)[1].split('</g>', 1)[0]
MARK = '<g' + MARK + '</g>'
BACKGROUND = '#1A1A1A'

def svg(background=None, adaptive=False):
    # Center within Android's 66dp circular safe zone on a 108dp canvas.
    transform = 'translate(256 256) scale(0.70) translate(-256 -264)' if adaptive else ''
    bg = f'<path fill="{background}" d="M0 0H512V512H0Z"/>' if background else ''
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">{bg}<g transform="{transform}">{MARK}</g></svg>'

def png(path, size, background=None, shape=None, adaptive=False):
    path = ROOT / path
    path.parent.mkdir(parents=True, exist_ok=True)
    # Render independently at every target size with supersampling.
    image = Image.open(io.BytesIO(cairosvg.svg2png(bytestring=svg(background, adaptive).encode(), output_width=size*4, output_height=size*4))).convert('RGBA')
    if shape:
        mask = Image.new('L', image.size)
        draw = ImageDraw.Draw(mask)
        box = (0, 0, size*4-1, size*4-1)
        if shape == 'round': draw.ellipse(box, fill=255)
        else: draw.rounded_rectangle(box, radius=size*4*0.18, fill=255)
        image.putalpha(mask)
    image = image.resize((size, size), Image.Resampling.LANCZOS)
    if background and not shape: image = image.convert('RGB')
    image.save(path)

for name, bg, adaptive in [('mathalarm-icon.svg', BACKGROUND, False), ('mathalarm-adaptive-foreground.svg', None, True)]:
    (ART/name).write_text(svg(bg, adaptive) + '\n')

res = Path('androidApp/src/main/res')
# Use actual vectors for adaptive launchers; no raster density can limit sharpness.
paths = ET.parse(SOURCE).getroot().findall('.//{http://www.w3.org/2000/svg}path')
vector = '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="512" android:viewportHeight="512">\n'
vector += '  <group android:pivotX="256" android:pivotY="264" android:scaleX="0.70" android:scaleY="0.70" android:translateY="-8">\n'
for p in paths:
    vector += f'    <path android:fillColor="#34B840" android:fillType="evenOdd" android:pathData="{p.attrib["d"]}"/>\n'
vector += '  </group>\n</vector>\n'
(ROOT/res/'drawable-v24/ic_launcher_foreground.xml').write_text(vector)
# Theme.SplashScreen uses a 288dp canvas with a 192dp circular safe area.
# A dedicated centered mark leaves padding around the bells and feet.
splash_vector = vector.replace('108dp', '288dp').replace('0.70', '0.80')
(ROOT/res/'drawable/ic_splash.xml').write_text(splash_vector)

for qualifier in ['mipmap-anydpi-v26', 'mipmap-anydpi-v33']:
    (ROOT/res/qualifier).mkdir(exist_ok=True)
    mono = '\n    <monochrome android:drawable="@drawable/ic_launcher_foreground"/>' if qualifier.endswith('v33') else ''
    adaptive_xml = '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/ic_launcher_background"/>\n    <foreground android:drawable="@drawable/ic_launcher_foreground"/>' + mono + '\n</adaptive-icon>\n'
    for name in ['ic_launcher', 'ic_launcher_round']:
        (ROOT/res/qualifier/f'{name}.xml').write_text(adaptive_xml)
for variant in ['main', 'debug', 'release']:
    base = Path(f'androidApp/src/{variant}/res')
    bg = '#FFFFFF' if variant == 'debug' else BACKGROUND
    for density, size in [('ldpi',36),('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
        for name, shape in [('ic_launcher','square'), ('ic_launcher_round','round')]:
            png(base/f'mipmap-{density}/{name}.png', size, bg, shape)
# Eliminate obsolete raster foregrounds now that adaptive icons reference a vector.
for path in (ROOT/res).glob('mipmap-*/ic_launcher_foreground.png'): path.unlink()
for path in (ROOT/res).glob('mipmap-*/icon.png'):
    png(path, Image.open(path).width)
png(res/'drawable/icon.png', 512)
png(Path('shared/src/commonMain/composeResources/drawable/icon.png'), 512)
for path in ['androidApp/src/main/ic_launcher-playstore.png', 'fastlane/metadata/android/en-US/images/icon.png']:
    png(Path(path), 512, BACKGROUND)
png(ART/'mathalarm-icon-1024.png', 1024, BACKGROUND)
icons = ROOT/'iosApp/iosApp/Assets.xcassets/AppIcon.appiconset'
catalog = json.loads((icons/'Contents.json').read_text())
for entry in catalog['images']:
    size = int(float(entry['size'].split('x')[0]) * float(entry['scale'][:-1]))
    if 'filename' not in entry: entry['filename'] = f'app-icon-{size}.png'
    png(icons/entry['filename'], size, BACKGROUND)
(icons/'Contents.json').write_text(json.dumps(catalog, indent=2) + '\n')
launch = ROOT/'iosApp/iosApp/Assets.xcassets/LaunchScreenIcon.imageset'
for path in launch.glob('*.png'):
    png(path, Image.open(path).width, '#FFFFFF')
print('Regenerated Android, iOS, shared UI, and store icons from SVG.')
