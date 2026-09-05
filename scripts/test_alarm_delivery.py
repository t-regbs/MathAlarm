#!/usr/bin/env python3
"""Real OS delivery checks. Requires installed debug + test APKs and an explicit emulator serial.

Instrumentation exits before process death / Doze / reboot. This runner never injects ALARM_ACTION.
Player-start telemetry proves the playback path ran, not acoustic audibility.
"""
import argparse
import json
import os
import re
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.timilehinaregbesola.mathalarm.debug"
TEST_CLASS = "com.timilehinaregbesola.mathalarm.AlarmDeliverySeedTest"
RECEIVER = "com.timilehinaregbesola.mathalarm.AlarmReceiver"
ALARM_ID = 900001


def run_cleanup(steps):
    """Attempt every restoration step, preserving all failures for the run report."""
    errors = {}
    for name, operation in steps:
        try:
            operation()
        except Exception as error:
            errors[name] = str(error)
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--scenario", choices=["cold-start", "doze", "reboot", "snooze"], default="cold-start")
    parser.add_argument("--output", type=Path, default=Path("build/alarm-device-results"))
    parser.add_argument("--adb", default=os.environ.get("ADB", "adb"))
    args = parser.parse_args()
    if not args.serial.startswith("emulator-"):
        parser.error("Automated reboot/Doze tests require a disposable emulator; use the physical-device checklist for phones")
    args.output.mkdir(parents=True, exist_ok=True)

    def adb(*command, check=True, timeout=30):
        result = subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True, text=True, timeout=timeout)
        if check and result.returncode:
            raise RuntimeError(f"adb {command}: {result.stderr or result.stdout}")
        return result.stdout

    def instrument(method, delay=45):
        output = adb("shell", "am", "instrument", "-w", "-r", "-e", "alarmLifecycle", "true",
                     "-e", "delaySeconds", str(delay), "-e", "class", f"{TEST_CLASS}#{method}",
                     f"{PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner", timeout=90)
        (args.output / f"{method}.txt").write_text(output)
        if "OK (1 test)" not in output or "FAILURES" in output:
            raise AssertionError(output)

    def events():
        raw = adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/alarm_delivery_log.xml")
        return [e for e in json.loads(ET.fromstring(raw).find("string[@name='events']").text)
                if e["alarmId"] == ALARM_ID]

    def wait_for(description, predicate, timeout):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if predicate():
                return
            time.sleep(0.5)  # Bounded polling of real device state, never a completion guess.
        raise AssertionError(f"Timed out: {description}")

    def delivered(count):
        log = events()
        if any(e["event"] in ("audio_failed", "schedule_failed") for e in log):
            raise AssertionError(json.dumps(log, indent=2))
        for name in ("received", "service_started", "audio_started"):
            if sum(e["event"] == name for e in log) < count:
                return False
        return True

    def command(action):
        adb("shell", "am", "broadcast", "-n", f"{PACKAGE}/{RECEIVER}", "-a",
            f"com.timilehinaregbesola.mathalarm.{action}", "--el", "extra_task", str(ALARM_ID))

    result = {"scenario": args.scenario, "serial": args.serial, "audibility": "not measured"}
    seeded = False
    idle_constants = None
    try:
        api = int(adb("shell", "getprop", "ro.build.version.sdk").strip())
        if api >= 33:
            adb("shell", "pm", "grant", PACKAGE, "android.permission.POST_NOTIFICATIONS")
        if 31 <= api <= 32:
            adb("shell", "appops", "set", PACKAGE, "SCHEDULE_EXACT_ALARM", "allow")
        seeded = True
        lead_seconds = {"reboot": 300, "doze": 180}.get(args.scenario, 45)
        instrument("seedColdStartAlarm", lead_seconds)
        print(f"{args.scenario}: alarm registered", flush=True)
        initial = next(e for e in events() if e["event"] == "scheduled")
        trigger = initial["triggerAt"]
        adb("shell", "input", "keyevent", "KEYCODE_HOME")
        adb("shell", "input", "keyevent", "KEYCODE_SLEEP")
        # am kill allows normal scheduled wakeups; force-stop would intentionally suppress them.
        adb("shell", "am", "kill", PACKAGE)
        wait_for("app process exited", lambda: not adb("shell", "pidof", PACKAGE, check=False).strip(), 15)
        print(f"{args.scenario}: app process exited", flush=True)
        if args.scenario == "doze":
            # Android normally avoids Doze for an alarm-clock event within one hour.
            # Use a 30-second initial idle window with the alarm three minutes away,
            # outside AlarmManager's two-minute early-wake margin. Restore both settings.
            idle_constants = adb("shell", "settings", "get", "global", "device_idle_constants").strip()
            constants = [] if idle_constants in ("", "null") else idle_constants.split(",")
            constants = [c for c in constants if not c.startswith(("min_time_to_alarm=", "idle_to="))]
            adb("shell", "settings", "put", "global", "device_idle_constants",
                ",".join(constants + ["min_time_to_alarm=0", "idle_to=30000"]))
            wait_for("test idle threshold applied", lambda: "min_time_to_alarm=0" in
                     adb("shell", "dumpsys", "deviceidle"), 10)
            adb("shell", "dumpsys", "battery", "unplug")
            idle_output = adb("shell", "dumpsys", "deviceidle", "force-idle", "deep")
            def is_deep_idle():
                state = adb("shell", "dumpsys", "deviceidle", "get", "deep").strip()
                (args.output / "idle-entry.txt").write_text(idle_output + "\nstate=" + state)
                return state == "IDLE"
            wait_for("confirmed deep idle after any maintenance window", is_deep_idle, 15)
        if args.scenario == "reboot":
            old_boot = adb("shell", "cat", "/proc/sys/kernel/random/boot_id").strip()
            adb("reboot")
            def new_boot_completed():
                boot_id = adb("shell", "cat", "/proc/sys/kernel/random/boot_id", check=False).strip()
                return bool(boot_id and boot_id != old_boot) and adb(
                    "shell", "getprop", "sys.boot_completed", check=False).strip() == "1"
            wait_for("new boot completed", new_boot_completed, 240)
            print("reboot: new boot completed", flush=True)
            adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
            adb("shell", "wm", "dismiss-keyguard")
            adb("shell", "input", "keyevent", "KEYCODE_SLEEP")
        wait_for("OS receiver, service and player start", lambda: delivered(1), lead_seconds + 45)
        print(f"{args.scenario}: receiver, service and player observed", flush=True)
        log = events()
        received = next(e for e in log if e["event"] == "received")
        assert received["triggerAt"] == trigger, log
        assert received["at"] >= trigger - 1000, "Alarm arrived before its scheduled occurrence"
        if args.scenario == "reboot":
            assert sum(e["event"] == "scheduled" for e in log) >= 2, "Boot recovery did not re-register the alarm"
        if args.scenario == "snooze":
            command("SNOOZE")
            wait_for("snooze stops current playback", lambda: any(e["event"] == "dismissed" for e in events()), 15)
            wait_for("real snooze delivery", lambda: delivered(2), 90)
            triggers = [e["triggerAt"] for e in events() if e["event"] == "received"]
            assert len(set(triggers)) == 2, triggers
        command("SET_COMPLETE")
        expected_dismissals = 2 if args.scenario == "snooze" else 1
        wait_for("completion", lambda: sum(e["event"] == "dismissed" for e in events()) >= expected_dismissals, 15)
        wait_for("alarm service stopped", lambda: not re.search(
            r"ServiceRecord[^\n]*AlarmService", adb("shell", "dumpsys", "activity", "services", PACKAGE)), 15)
        # Re-enter instrumentation only after the background journey has completed.
        instrument("verifyCompleted")
        result["status"] = "passed"
    except Exception as error:
        result.update(status="failed", error=str(error))
    finally:
        for name, command_args in {
            "alarm.txt": ("shell", "dumpsys", "alarm"),
            "audio.txt": ("shell", "dumpsys", "audio"),
            "deviceidle.txt": ("shell", "dumpsys", "deviceidle"),
            "logcat.txt": ("logcat", "-d", "-t", "2000"),
        }.items():
            try:
                (args.output / name).write_text(adb(*command_args, check=False))
            except Exception:
                pass
        try:
            (args.output / "events.json").write_text(json.dumps(events(), indent=2))
        except Exception:
            pass
        cleanup_steps = []
        if args.scenario == "doze":
            cleanup_steps.extend([
                ("exit idle", lambda: adb("shell", "dumpsys", "deviceidle", "unforce")),
                ("reset battery", lambda: adb("shell", "dumpsys", "battery", "reset")),
            ])
            if idle_constants is not None:
                restore = ("delete", "global", "device_idle_constants") if idle_constants in ("", "null") else (
                    "put", "global", "device_idle_constants", idle_constants)
                cleanup_steps.append(("restore idle settings", lambda: adb("shell", "settings", *restore)))
        if seeded:
            cleanup_steps.append(("remove test alarm", lambda: instrument("cleanup")))
        cleanup_errors = run_cleanup(cleanup_steps)
        if cleanup_errors:
            result.update(status="failed", cleanup_errors=cleanup_errors)
        (args.output / "result.json").write_text(json.dumps(result, indent=2))
        print(json.dumps(result, indent=2))
    if result["status"] != "passed":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
