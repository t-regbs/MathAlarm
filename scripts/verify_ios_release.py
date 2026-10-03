#!/usr/bin/env python3
"""Verify pinned bridge resolution and built app/archive packaging, not distribution acceptance."""
import argparse
import hashlib
import json
from pathlib import Path
import plistlib
import subprocess
from verify_native_localization import violations, LOCALES, TONES

PINS = {
    'kmp-nativecoroutines': ('1.0.6', '54a45fbb15bc0bebc54081f87683830d3bc9e84e'),
    'kmp-observableviewmodel': ('1.1.0', '9c73fbf9c879131715fd71e9e243a4ba10fd53a3'),
    'rxswift': ('6.10.2', '132aea4f236ccadc51590b38af0357a331d51fa2'),
}

def pin_violations(root):
    resolved = json.loads((root / 'iosApp/iosApp.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved').read_text())
    actual = {p['identity']: (p['state']['version'], p['state']['revision']) for p in resolved['pins']}
    errors = [] if actual == PINS else [f'Swift package lock changed: {actual}']
    catalog = (root / 'gradle/libs.versions.toml').read_text()
    project = (root / 'iosApp/iosApp.xcodeproj/project.pbxproj').read_text()
    for key, version in [('observableViewModel', '1.1.0'), ('nativeCoroutines', '1.0.6')]:
        if f'{key} = "{version}"' not in catalog or f'kind = exactVersion; version = {version};' not in project:
            errors.append(f'Kotlin/Swift bridge pin mismatch: {key}')
    return errors

def packaging_violations(root, app, release=False):
    errors = violations(root, app)
    source = root / 'iosApp/iosApp'
    resources = ['PrivacyInfo.xcprivacy'] + [f'Sounds/alarm_{t}.caf' for t in TONES]
    resources += [f'PermissionGuide/alarm-settings-guide-{locale}.{suffix}' for locale in sorted(LOCALES) for suffix in ('mp4', 'png')]
    for relative in resources:
        original, packaged = source / relative, app / Path(relative).name
        if not original.is_file() or not packaged.is_file():
            errors.append(f'Missing packaged resource: {relative}')
        elif original.suffix == '.png':
            # Xcode rewrites PNG compression; compare valid signature and IHDR dimensions.
            a, b = original.read_bytes(), packaged.read_bytes()
            if b[:8] != b'\x89PNG\r\n\x1a\n' or a[a.index(b'IHDR')+4:a.index(b'IHDR')+12] != b[b.index(b'IHDR')+4:b.index(b'IHDR')+12]:
                errors.append(f'Invalid/resized packaged guide poster: {relative}')
        elif hashlib.sha256(original.read_bytes()).digest() != hashlib.sha256(packaged.read_bytes()).digest():
            errors.append(f'Packaged resource differs from reviewed source: {relative}')
    try:
        privacy = plistlib.loads((app / 'PrivacyInfo.xcprivacy').read_bytes())
        if not privacy.get('NSPrivacyAccessedAPITypes'):
            errors.append('Privacy manifest has no required-reason API declarations')
    except (OSError, ValueError) as error:
        errors.append(f'Invalid privacy manifest: {error}')
    binary = app / 'MathAlarm'
    if not binary.is_file():
        errors.append('Linked production binary missing')
    elif release:
        symbols = subprocess.check_output(['nm', str(binary)], text=True, stderr=subprocess.DEVNULL)
        for forbidden in ('SharedBridgeVerification', 'VerificationResults', 'NativePresentationVerification', 'NativeSettingsVerification'):
            if forbidden in symbols:
                errors.append(f'Debug fixture leaked into Release: {forbidden}')
    return errors

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--app', type=Path)
    parser.add_argument('--release', action='store_true')
    args = parser.parse_args()
    errors = pin_violations(args.root)
    if args.app:
        errors += packaging_violations(args.root, args.app, args.release)
    if errors:
        raise SystemExit('\n'.join(errors))
    print('Pinned Swift/Kotlin bridge passed' + ('; compiled nine-locale catalogs, six stable tones, privacy and nine guide videos/posters passed' if args.app else '') + ('; Release excludes Debug fixtures' if args.release else ''))

if __name__ == '__main__': main()
