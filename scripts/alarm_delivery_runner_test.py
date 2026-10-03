"""Failure-path checks for emulator state restoration; no device required."""
import unittest

from test_alarm_delivery import run_cleanup, apply_idle_test_overrides, idle_test_overrides_applied


class CleanupTest(unittest.TestCase):
    def test_partial_device_config_setup_restores_only_changed_keys(self):
        calls, restorations = [], []

        def adb(*args):
            calls.append(args)
            if args[2] == "get":
                return "null" if args[4] == "min_time_to_alarm" else "90000"
            if args == ("shell", "device_config", "put", "device_idle", "idle_to", "30000"):
                raise RuntimeError("setup failed")
            return ""

        with self.assertRaisesRegex(RuntimeError, "setup failed"):
            apply_idle_test_overrides(adb, 32, restorations)
        self.assertEqual({}, run_cleanup(restorations))
        self.assertEqual(calls[-2:], [
            ("shell", "device_config", "delete", "device_idle", "min_time_to_alarm"),
            ("shell", "device_config", "put", "device_idle", "idle_to", "90000"),
        ])
        self.assertFalse(any("reset" in call for call in calls))

    def test_settings_override_preserves_unrelated_values_and_restores_exact_original(self):
        calls, restorations = [], []
        original = "idle_to=90000,unrelated=7,min_time_to_alarm=60000"

        def adb(*args):
            calls.append(args)
            return original if args[2] == "get" else ""

        apply_idle_test_overrides(adb, 35, restorations)
        self.assertEqual(calls[-1][-1], "unrelated=7,min_time_to_alarm=0,idle_to=30000")
        self.assertEqual({}, run_cleanup(restorations))
        self.assertEqual(calls[-1][-1], original)

    def test_effective_threshold_check_rejects_stale_service_configuration(self):
        self.assertTrue(idle_test_overrides_applied("  min_time_to_alarm=0\n  idle_to=+30s0ms\n"))
        self.assertFalse(idle_test_overrides_applied("  min_time_to_alarm=+30m0s0ms\n  idle_to=+30s0ms\n"))
        self.assertFalse(idle_test_overrides_applied("  min_time_to_alarm=0\n  idle_to=+1h0m0s0ms\n"))
        self.assertFalse(idle_test_overrides_applied("  min_time_to_alarm=0\n  light_idle_to=+30s0ms\n"))

    def test_failures_do_not_skip_settings_or_alarm_cleanup(self):
        completed = []

        def fail():
            raise RuntimeError("device command failed")

        errors = run_cleanup([
            ("exit idle", fail),
            ("reset battery", fail),
            ("restore idle settings", lambda: completed.append("settings")),
            ("remove test alarm", lambda: completed.append("alarm")),
        ])

        self.assertEqual(["settings", "alarm"], completed)
        self.assertEqual({"exit idle": "device command failed", "reset battery": "device command failed"}, errors)

    def test_successful_cleanup_reports_no_errors(self):
        self.assertEqual({}, run_cleanup([("cleanup", lambda: None)]))


if __name__ == "__main__":
    unittest.main()
