import json
from pathlib import Path
import re
import unittest
from verify_ios_shared_bridge import REQUIRED_CHECKS
from verify_ios_release import pin_violations

ROOT = Path(__file__).resolve().parents[1]

class PermanentNativeContractTest(unittest.TestCase):
    def test_every_original_group_is_required_by_native_targets(self):
        text = (ROOT / 'iosApp/MathAlarmTests/VerificationContract.swift').read_text()
        groups = json.loads(re.search(r'= (\[.*\])', text, re.S)[1])
        self.assertEqual(groups, list(REQUIRED_CHECKS))
        self.assertEqual(len(set(groups)), 27)

    def test_bridge_pins_match_reviewed_resolution(self):
        self.assertEqual(pin_violations(ROOT), [])
