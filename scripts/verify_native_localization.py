#!/usr/bin/env python3
"""Verify native catalogs, production presentation keys and bundled resources.

This static gate complements Xcode catalog compilation and mounted/device checks.
It does not establish visual locale coverage or accessibility-client behavior.
"""
import argparse
from collections import Counter
import json
from pathlib import Path
import plistlib
import re


LOCALES = frozenset("en es de ru pt hi pa bn zh".split())
TONES = ("daybreak", "orbit", "rally", "glass_garden", "stepping_stones", "clear_signal")
ANNOUNCEMENT_IDS = frozenset(("math-challenges-v1", "skip-next-alarm-v1", "snooze-settings-v1"))
REQUIRED_PLURALS = {
    "en": {"one", "other"}, "es": {"one", "other"},
    "de": {"one", "other"}, "ru": {"one", "few", "many", "other"},
    "pt": {"one", "other"}, "hi": {"one", "other"},
    "pa": {"one", "other"}, "bn": {"one", "other"}, "zh": {"other"},
}
SWIFT_STRING = r'"((?:[^"\\]|\\.)*)"'


def presentation_keys(source):
    """Extract literal presentation keys, including native dynamic adapters.

    This is intentionally a bounded Swift source audit, not a Swift parser.
    String interpolation formats are separately checked by the plural contract.
    """
    source = re.sub(r"/\*.*?\*/", "", source, flags=re.S)
    source = re.sub(r"//[^\n]*", "", source)
    calls = (r"(?:\b(?:Text|TextField|Label|Button|Toggle|Picker|Section|ProgressView|ContentUnavailableView|"
             r"LabeledContent|Menu|DatePicker|NavigationLink|NSLocalizedString|rangePicker|settingsLabel)|"
             r"\.navigationTitle|\.confirmationDialog|\.alert)\(\s*" + SWIFT_STRING)
    keys = set(re.findall(calls, source))
    keys.update(re.findall(r"String\(localized:\s*" + SWIFT_STRING, source))
    keys.update(re.findall(r"LocalizedStringResource\s*=\s*" + SWIFT_STRING, source))
    keys.update(re.findall(r"@Parameter\(title:\s*" + SWIFT_STRING, source))
    if "enum NativeStrings" in source:
        keys.update(re.findall(r"\btext\(\s*" + SWIFT_STRING, source))
    for call in re.findall(r"NativeStrings\.text\((.*?)\)", source, flags=re.S):
        keys.update(re.findall(SWIFT_STRING, call))
    keys.update(re.findall(r"\blabel:\s*" + SWIFT_STRING, source))
    keys.update(re.findall(r"\bpreviewMessage\s*=\s*" + SWIFT_STRING, source))
    keys.update(re.findall(r"\bfailure\s*=\s*" + SWIFT_STRING, source))
    keys.update(re.findall(r"\bdetail:\s*" + SWIFT_STRING, source))
    keys.update(re.findall(r'(?:case\s+"alarm_[^"]+"|default):\s*' + SWIFT_STRING, source))
    for names in re.findall(r"let names = \[(.*?)\]", source):
        keys.update(re.findall(SWIFT_STRING, names))
    # Empty separators and locale-formatted operand ranges are not catalog keys.
    return {key for key in keys if key.strip() and "\\(" not in key and
            key not in ANNOUNCEMENT_IDS | {"+", "−", "×", "÷"}}


def format_arguments(value):
    # Count types while allowing a translator to reorder positional arguments.
    return Counter(re.findall(r"(?<!%)%(?:\d+\$)?(lld|@)", value))


def catalog_violations(catalog, sources=None, require_question_plurals=True):
    errors = []
    if catalog.get("sourceLanguage") != "en" or catalog.get("version") != "1.0":
        errors.append("Catalog must use sourceLanguage en and version 1.0")
    strings = catalog.get("strings", {})
    for key, entry in strings.items():
        translations = entry.get("localizations", {})
        if set(translations) != LOCALES:
            errors.append(f"{key!r}: expected all nine locales")
        for locale, translation in translations.items():
            plurals = translation.get("variations", {}).get("plural")
            units = list(plurals.values()) if plurals else [translation]
            for variation in units:
                unit = variation.get("stringUnit", {})
                value = unit.get("value", "")
                if unit.get("state") != "translated" or not value.strip():
                    errors.append(f"{key!r}/{locale}: missing translated value")
                if format_arguments(key) != format_arguments(value):
                    errors.append(f"{key!r}/{locale}: format argument mismatch")
    question_entry = strings.get("%lld questions", {})
    for locale, categories in (REQUIRED_PLURALS.items() if require_question_plurals else []):
        plurals = question_entry.get("localizations", {}).get(locale, {}).get("variations", {}).get("plural", {})
        if not categories.issubset(plurals):
            errors.append(f"Question plural categories missing for {locale}")
    for filename, source in (sources or {}).items():
        for key in sorted(presentation_keys(source) - strings.keys()):
            errors.append(f"{filename}: uncataloged presentation key {key!r}")
    return errors


def packaged_violations(app, catalogs):
    """Inspect compiled catalogs/resources, independently of source project refs."""
    errors = []
    for catalog_name, catalog in catalogs.items():
        expected = set(catalog.get("strings", {}))
        for locale in sorted(LOCALES):
            keys = set()
            for suffix in ("strings", "stringsdict"):
                path = app / f"{locale}.lproj/{catalog_name}.{suffix}"
                if path.is_file():
                    try:
                        keys.update(plistlib.loads(path.read_bytes()))
                    except (OSError, plistlib.InvalidFileException, ValueError) as error:
                        errors.append(f"Unable to read packaged {path.name}/{locale}: {error}")
            missing = expected - keys
            if missing:
                errors.append(f"Packaged {catalog_name}/{locale}: missing keys {sorted(missing)!r}")
    for tone in TONES:
        path = app / f"alarm_{tone}.caf"
        if not path.is_file() or path.read_bytes()[:4] != b"caff":
            errors.append(f"Missing/invalid packaged CAF tone: {path.name}")
    for relative in ("Assets.car", "Base.lproj/LaunchScreen.storyboardc/Info.plist"):
        if not (app / relative).is_file():
            errors.append(f"Missing compiled native launch resource: {relative}")
    try:
        if plistlib.loads((app / "Info.plist").read_bytes()).get("UILaunchStoryboardName") != "LaunchScreen":
            errors.append("Packaged app does not select the native LaunchScreen storyboard")
    except (OSError, plistlib.InvalidFileException, ValueError) as error:
        errors.append(f"Unable to read packaged Info.plist: {error}")
    return errors


def violations(root, app=None):
    catalog_path = root / "iosApp/iosApp/Localizable.xcstrings"
    try:
        catalog = json.loads(catalog_path.read_text())
    except (OSError, json.JSONDecodeError) as error:
        return [f"Unable to read native String Catalog: {error}"]
    source_root = root / "iosApp/iosApp"
    source_paths = sorted(source_root.glob("Native*.swift")) + [
        source_root / name for name in ("ContentView.swift", "iOSApp.swift", "AlarmKitWrapper.swift")]
    sources = {path.name: path.read_text() for path in source_paths}
    errors = catalog_violations(catalog, sources)
    for filename, source in sources.items():
        if "NativeDevelopmentScreen" in source or "Native development screen" in source:
            errors.append(f"{filename}: production development placeholder remains")
    project = (root / "iosApp/iosApp.xcodeproj/project.pbxproj").read_text()
    for reference in ["Localizable.xcstrings in Resources", "LaunchScreen.storyboard in Resources",
                      "Assets.xcassets in Resources"] + [f"{name} in Sources" for name in sources]:
        if reference not in project:
            errors.append(f"Missing Xcode build phase reference: {reference}")
    regions = re.search(r"knownRegions = \((.*?)\);", project, flags=re.S)
    if not regions or not LOCALES.issubset(set(re.findall(r"\b[a-z]{2}\b", regions.group(1)))):
        errors.append("Xcode knownRegions does not contain all nine locales")
    catalogs = {"Localizable": catalog}
    info_catalog_path = source_root / "InfoPlist.xcstrings"
    try:
        info_catalog = json.loads(info_catalog_path.read_text())
        errors.extend(catalog_violations(info_catalog, require_question_plurals=False))
        if "NSAlarmKitUsageDescription" not in info_catalog.get("strings", {}):
            errors.append("InfoPlist catalog does not localize the AlarmKit permission explanation")
        if "InfoPlist.xcstrings in Resources" not in project:
            errors.append("Missing Xcode build phase reference: InfoPlist.xcstrings in Resources")
        catalogs["InfoPlist"] = info_catalog
    except (OSError, json.JSONDecodeError) as error:
        errors.append(f"Unable to read native InfoPlist String Catalog: {error}")
    for tone in TONES:
        name = f"alarm_{tone}.caf"
        if not (root / "iosApp/iosApp/Sounds" / name).is_file() or f"{name} in Resources" not in project:
            errors.append(f"Missing bundled tone resource: {name}")
    helper = sources["NativeStrings.swift"]
    if 'String(localized: "\\(count) questions")' not in helper:
        errors.append("Native question interpolation no longer matches %lld questions catalog contract")
    if app is not None:
        errors.extend(packaged_violations(app, catalogs))
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--app", type=Path, help="Freshly built MathAlarm.app to inspect compiled resources")
    args = parser.parse_args()
    errors = violations(args.root, args.app)
    if errors:
        for error in errors:
            print(error)
        return 1
    print("Native localization passed: nine locales, plurals, all production source keys and native resources" +
          ("; compiled app catalogs/tones/launch resources inspected" if args.app else "; source project packaging checked"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
