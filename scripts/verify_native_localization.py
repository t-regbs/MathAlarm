#!/usr/bin/env python3
"""Verify the Milestone 4/5 native catalog, source keys and packaged resources.

This static gate complements Xcode catalog compilation and mounted/device checks.
It deliberately excludes Milestone 6 development placeholder copy.
"""
import argparse
from collections import Counter
import json
from pathlib import Path
import re


LOCALES = frozenset("en es de ru pt hi pa bn zh".split())
SOURCE_FILES = ("NativeAlarmViews.swift", "NativeEditorControls.swift",
                "NativeSoundPicker.swift", "NativeChallengeViews.swift", "NativeStrings.swift", "ContentView.swift")
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
    source = re.sub(r"^struct NativeDevelopmentScreen:.*?^}", "", source, flags=re.S | re.M)
    source = re.sub(r"/\*.*?\*/", "", source, flags=re.S)
    source = re.sub(r"//[^\n]*", "", source)
    calls = (r"(?:\b(?:Text|TextField|Label|Button|Toggle|Picker|Section|ProgressView|ContentUnavailableView|"
             r"LabeledContent|Menu|DatePicker|NSLocalizedString|rangePicker)|"
             r"\.navigationTitle|\.confirmationDialog|\.alert)\(\s*" + SWIFT_STRING)
    keys = set(re.findall(calls, source))
    if "enum NativeStrings" in source:
        keys.update(re.findall(r"\btext\(\s*" + SWIFT_STRING, source))
    for call in re.findall(r"NativeStrings\.text\((.*?)\)", source, flags=re.S):
        keys.update(re.findall(SWIFT_STRING, call))
    keys.update(re.findall(r"\blabel:\s*" + SWIFT_STRING, source))
    keys.update(re.findall(r"\bpreviewMessage\s*=\s*" + SWIFT_STRING, source))
    keys.update(re.findall(r'(?:case\s+"alarm_[^"]+"|default):\s*' + SWIFT_STRING, source))
    for names in re.findall(r"let names = \[(.*?)\]", source):
        keys.update(re.findall(SWIFT_STRING, names))
    # Empty separators and locale-formatted operand ranges are not catalog keys.
    return {key for key in keys if key.strip() and "\\(" not in key and key not in {"+", "−", "×", "÷"}}


def format_arguments(value):
    # Count types while allowing a translator to reorder positional arguments.
    return Counter(re.findall(r"(?<!%)%(?:\d+\$)?(lld|@)", value))


def catalog_violations(catalog, sources=None):
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
    for locale, categories in REQUIRED_PLURALS.items():
        plurals = question_entry.get("localizations", {}).get(locale, {}).get("variations", {}).get("plural", {})
        if not categories.issubset(plurals):
            errors.append(f"Question plural categories missing for {locale}")
    for filename, source in (sources or {}).items():
        for key in sorted(presentation_keys(source) - strings.keys()):
            errors.append(f"{filename}: uncataloged presentation key {key!r}")
    return errors


def violations(root):
    catalog_path = root / "iosApp/iosApp/Localizable.xcstrings"
    try:
        catalog = json.loads(catalog_path.read_text())
    except (OSError, json.JSONDecodeError) as error:
        return [f"Unable to read native String Catalog: {error}"]
    sources = {filename: (root / "iosApp/iosApp" / filename).read_text()
               for filename in SOURCE_FILES}
    errors = catalog_violations(catalog, sources)
    project = (root / "iosApp/iosApp.xcodeproj/project.pbxproj").read_text()
    for reference in ("Localizable.xcstrings in Resources", "NativeStrings.swift in Sources",
                      "NativeChallengeViews.swift in Sources"):
        if reference not in project:
            errors.append(f"Missing Xcode build phase reference: {reference}")
    regions = re.search(r"knownRegions = \((.*?)\);", project, flags=re.S)
    if not regions or not LOCALES.issubset(set(re.findall(r"\b[a-z]{2}\b", regions.group(1)))):
        errors.append("Xcode knownRegions does not contain all nine locales")
    for tone in ("daybreak", "orbit", "rally", "glass_garden", "stepping_stones", "clear_signal"):
        name = f"alarm_{tone}.caf"
        if not (root / "iosApp/iosApp/Sounds" / name).is_file() or f"{name} in Resources" not in project:
            errors.append(f"Missing bundled tone resource: {name}")
    helper = sources["NativeStrings.swift"]
    if 'String(localized: "\\(count) questions")' not in helper:
        errors.append("Native question interpolation no longer matches %lld questions catalog contract")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    errors = violations(args.root)
    if errors:
        for error in errors:
            print(error)
        return 1
    print("Native Milestone 4/5 localization passed: nine locales, plurals, source keys and packaged resources")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
