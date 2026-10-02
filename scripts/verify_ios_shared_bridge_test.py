import unittest

from verify_ios_shared_bridge import REQUIRED_CHECKS, missing_checks


class BridgeVerificationTranscriptTest(unittest.TestCase):
    def test_full_production_transcript_contains_every_required_group(self):
        transcript = '\n'.join(f'BRIDGE PASS {check}' for check in REQUIRED_CHECKS).encode()
        self.assertEqual([], missing_checks(transcript))

    def test_final_marker_does_not_accept_an_old_or_partial_harness(self):
        self.assertEqual(list(REQUIRED_CHECKS), missing_checks(b'SHARED_BRIDGE_VERIFICATION_PASSED\n'))
        transcript = '\n'.join(f'BRIDGE PASS {check}' for check in REQUIRED_CHECKS[:-1]).encode()
        self.assertEqual([REQUIRED_CHECKS[-1]], missing_checks(transcript))


if __name__ == '__main__':
    unittest.main()
