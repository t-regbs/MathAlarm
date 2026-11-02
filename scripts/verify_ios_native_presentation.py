#!/usr/bin/env python3
"""Capture production native screens for the nine shipped locales.

Use only a disposable simulator. The opt-in DEBUG run has an independent,
once-only task lifetime across native presentation, saves uniquely named disabled
fixtures, captures retained native screens, and deletes only its exact IDs through
production list commands with retained-result acknowledgement in the same process.
The base phase briefly enables its fixtures only through that controlled scheduler
to render the next-alarm subtitle, then disables them before exact-ID cleanup.
The separate delivered phase creates one enabled occurrence only through the
controlled DEBUG scheduler, private handoff queue and no-op native recovery hook;
it completes that occurrence and deletes its exact disabled fixture afterward.
No real AlarmKit registration or external recipient is used. Rendering does not
establish acoustic output, physical delivery, animation, or accessibility client output.
"""
import argparse
import pathlib
import re
import selectors
import shutil
import subprocess
import time

REGIONS = dict(en="en_GB", es="es_ES", de="de_DE", ru="ru_RU", pt="pt_PT",
               hi="hi_IN", pa="pa_IN", bn="bn_BD", zh="zh_CN")
BUNDLE = "com.timilehinaregbesola.mathalarm"


def evidence_directory(args, locale):
    container = pathlib.Path(subprocess.check_output(
        ["xcrun", "simctl", "get_app_container", args.udid, BUNDLE, "data"], text=True).strip())
    return container / "Documents/native-ui-m6-presentation" / REGIONS[locale]


def copy_evidence(args, locale, log_path):
    """Preserve completed captures even when a later assertion fails."""
    source = evidence_directory(args, locale)
    destination = args.output / locale
    destination.mkdir(parents=True, exist_ok=True)
    # An optional bottom viewport can disappear after a layout fix. Copy only
    # files actually emitted by this process, never stale captures in Documents.
    names = set(re.findall(r"M6 CAPTURE [^/\s]+/([^\s]+)", log_path.read_text(errors="replace")))
    for name in names:
        for suffix in (".png", ".json"):
            path = source / (name + suffix)
            if path.is_file():
                shutil.copy2(path, destination / path.name)


def run_locale(args, locale, log_path, *, delivered=False):
    command = ["xcrun", "simctl", "launch", "--console", "--terminate-running-process",
               args.udid, BUNDLE, "--verify-m6-presentation"]
    if delivered:
        command.append("--verify-m6-delivered-presentation")
    marker = b"M6_DELIVERED_PRESENTATION_PASSED" if delivered else b"M6_PRESENTATION_VERIFICATION_PASSED"
    command += ["-AppleLanguages", f"({locale})", "-AppleLocale", REGIONS[locale]]
    destination = args.output / locale
    destination.mkdir(parents=True, exist_ok=True)
    for path in destination.iterdir():
        if path.suffix in (".png", ".json") and path.stem.startswith("delivered-") == delivered:
            path.unlink()
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    selector = selectors.DefaultSelector()
    selector.register(process.stdout, selectors.EVENT_READ)
    captured = b""
    passed = False
    deadline = time.monotonic() + args.timeout
    with log_path.open("wb") as log:
        try:
            while time.monotonic() < deadline:
                events = selector.select(timeout=1)
                for key, _ in events:
                    chunk = key.fileobj.read1(65536)
                    if not chunk:
                        selector.unregister(key.fileobj)
                        break
                    log.write(chunk)
                    log.flush()
                    captured += chunk
                    if marker in captured:
                        passed = True
                        break
                if passed or not selector.get_map() or (not events and process.poll() is not None):
                    break
        finally:
            selector.close()
            subprocess.run(["xcrun", "simctl", "terminate", args.udid, BUNDLE],
                           check=False, capture_output=True)
            if process.poll() is None:
                process.terminate()
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
    copy_evidence(args, locale, log_path)
    if not passed:
        raise SystemExit(f"Native {'delivered ' if delivered else ''}presentation failed for {locale}; inspect {log_path}")


def required_capture_names():
    names = {"launch-storyboard-rendered-light", "launch-storyboard-rendered-dark", "settings-synthetic-rtl"}
    screens = ("editor", "repeat", "challenge-preset", "challenge-mixing", "challenge-custom",
               "snooze", "sound", "sound-missing-tone-fallback", "preview", "settings",
               "whats-new-maths", "whats-new-snooze", "list-populated")
    dials = {f"editor-dial-{hour}-{minute}-{theme}"
             for hour, minute in ((0, 0), (3, 15), (7, 20), (9, 45)) for theme in ("light", "dark")}
    dials |= {f"editor-dial-increased-contrast-{theme}" for theme in ("light", "dark")}
    summaries = {"list-next-alarm-light", "list-next-alarm-dark", "list-next-alarm-dark-accessibility3",
                 "list-next-alarm-one-enabled-light", "list-next-alarm-one-enabled-dark"}
    return names | dials | summaries | {f"{screen}-{suffix}" for screen in screens for suffix in ("light", "dark-accessibility3")}


def required_delivered_capture_names():
    return {"delivered-returned-draft"} | {
        f"delivered-{screen}-{suffix}" for screen in ("progress", "feedback")
        for suffix in ("light", "dark-accessibility3")}


def require_captures(evidence, names, locale):
    missing = [name for name in sorted(names)
               if not (evidence / f"{name}.png").is_file() or not (evidence / f"{name}.json").is_file()]
    if missing:
        raise SystemExit(f"Missing native {locale} rendered captures: {missing}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--udid", required=True)
    parser.add_argument("--app", type=pathlib.Path, required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    parser.add_argument("--locales", nargs="+", choices=REGIONS, default=list(REGIONS))
    parser.add_argument("--timeout", type=float, default=180)
    phases = parser.add_mutually_exclusive_group()
    phases.add_argument("--delivered-only", action="store_true", help="Run only the controlled delivered-screen phase")
    phases.add_argument("--base-only", action="store_true", help="Run only the disabled-fixture base screens")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    subprocess.run(["xcrun", "simctl", "install", args.udid, str(args.app)], check=True)
    for locale in args.locales:
        if not args.delivered_only:
            run_locale(args, locale, args.output / f"{locale}.log")
            evidence = args.output / locale
            require_captures(evidence, required_capture_names(), locale)
            if not any((evidence / f"{name}.png").exists() for name in
                       ("list-empty-after-fixture-removal", "list-after-fixture-removal")):
                raise SystemExit(f"Missing native list capture after exact-ID cleanup for {locale}")
        if not args.base_only:
            run_locale(args, locale, args.output / f"{locale}-delivered.log", delivered=True)
            require_captures(args.output / locale, required_delivered_capture_names(), locale)
        evidence = args.output / locale
        print(f"Native presentation captured {locale}: {len(list(evidence.glob('*.png')))} screens; same-process typed cleanup passed", flush=True)
    print("Native presentation matrix completed; visual review and client checks remain separate.")


if __name__ == "__main__":
    main()
