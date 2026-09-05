"""Failure-path checks for emulator state restoration; no device required."""
import unittest

from test_alarm_delivery import run_cleanup


class CleanupTest(unittest.TestCase):
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
