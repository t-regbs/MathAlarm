#!/usr/bin/env python3
"""Verify saved theme startup on an explicitly selected dedicated emulator."""
import argparse
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

PACKAGE = "com.timilehinaregbesola.mathalarm.debug"
PREFS = f"shared_prefs/{PACKAGE}_preferences.xml"
KEY = "mathalarm_theme_option"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("serial", help="Dedicated emulator serial with app and test APKs installed")
    parser.add_argument("--output", type=Path, default=Path("build/alarm-device-results/saved-theme"))
    args = parser.parse_args()
    if not args.serial.startswith("emulator-"):
        parser.error("Pass an explicit dedicated emulator serial")

    def adb(*command, data=None, check=True):
        return subprocess.run(["adb", "-s", args.serial, *command], input=data,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=check)

    def read_pref(path):
        exists = adb("shell", "run-as", PACKAGE, "test", "-f", path, check=False)
        if exists.returncode:
            return None
        return adb("exec-out", "run-as", PACKAGE, "cat", path).stdout

    def write_pref(path, data):
        if data is None:
            adb("shell", "run-as", PACKAGE, "rm", "-f", path)
        else:
            adb("shell", "run-as", PACKAGE, "mkdir", "-p", "shared_prefs")
            adb("shell", f"run-as {PACKAGE} sh -c 'cat > {path}'", data=data)

    for package in (PACKAGE, f"{PACKAGE}.test"):
        if not adb("shell", "pm", "path", package).stdout.startswith(b"package:"):
            raise RuntimeError(f"Install {package} before running this check")
    args.output.mkdir(parents=True, exist_ok=True)
    adb("shell", "am", "force-stop", PACKAGE)
    backups = {path: read_pref(path) for path in (PREFS, f"{PREFS}.bak")}
    original = backups[f"{PREFS}.bak"] or backups[PREFS] or b"<map />"
    try:
        for label, stored, expected in (
            ("light", 0, 1), ("dark", 1, 2), ("system", 2, -1),
            ("invalid", 98765, -1), ("missing", None, -1),
        ):
            prefs = ET.fromstring(original)
            for item in list(prefs):
                if item.get("name") == KEY:
                    prefs.remove(item)
            if stored is not None:
                ET.SubElement(prefs, "int", name=KEY, value=str(stored))
            adb("shell", "am", "force-stop", PACKAGE)
            write_pref(f"{PREFS}.bak", None)
            write_pref(PREFS, ET.tostring(prefs, encoding="utf-8", xml_declaration=True))
            result = adb("shell", "am", "instrument", "-w", "-r", "-e", "class",
                         "com.timilehinaregbesola.mathalarm.SavedThemeStartupTest",
                         "-e", "expectedNightMode", str(expected),
                         f"{PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner")
            output = result.stdout.decode()
            (args.output / f"{label}.txt").write_text(output)
            if not re.search(r"^OK \(1 test\)", output, re.MULTILINE) or re.search(
                r"FAILURES|INSTRUMENTATION_FAILED|INSTRUMENTATION_STATUS_CODE: -[1234]", output
            ):
                raise AssertionError(f"{label} startup failed; see {args.output / (label + '.txt')}")
            print(f"{label}: saved={stored}, startup night mode={expected}, passed", flush=True)
    finally:
        adb("shell", "am", "force-stop", PACKAGE)
        for path, data in backups.items():
            write_pref(path, data)
            if read_pref(path) != data:
                raise AssertionError(f"Failed to restore {path}")
        print("Original preference files restored", flush=True)


if __name__ == "__main__":
    main()
