#!/usr/bin/env python3
"""Create an offline review index; capture completeness is not a visual review pass."""
import html
import json
from pathlib import Path
import sys
from verify_ios_native_presentation import REGIONS, required_capture_names, required_delivered_capture_names, require_captures
root = Path(sys.argv[1])
names = required_capture_names() | required_delivered_capture_names()
for locale in REGIONS:
    require_captures(root / locale, names, locale)
captured_names = {p.stem for locale in REGIONS for p in (root / locale).glob('*.png')}
sections = []
for name in sorted(captured_names):
    cards = ''.join(
        f'<figure><figcaption>{locale}</figcaption><a href="{locale}/{name}.png"><img loading="lazy" src="{locale}/{name}.png"></a><a href="{locale}/{name}.json">Traits</a></figure>'
        if (root / locale / f'{name}.png').is_file() else f'<figure>{locale}: no additional viewport</figure>'
        for locale in REGIONS)
    sections.append(f'<h2>{html.escape(name)}</h2><div>{cards}</div>')
(root/'review.html').write_text('<!doctype html><meta charset="utf-8"><title>Native nine-locale review</title><style>body{font:16px system-ui}div{display:flex;overflow:auto}figure{margin:8px}img{width:240px}h2{position:sticky;top:0;background:white}</style><h1>Controlled simulator captures — review required</h1><p>Inspect clipping, glyphs, controls and contrast. No physical delivery, acoustic or VoiceOver claim.</p>'+''.join(sections))
(root/'review-status.json').write_text(json.dumps({'captureCompleteness':'passed','visualReview':'pending','locales':list(REGIONS),'requiredFamilies':len(names),'capturedFamilies':len(captured_names)},indent=2)+'\n')
print(root/'review.html')
