import unittest
from verify_ios_release import metadata_violations, required_reason_findings


class ReleaseCandidateTests(unittest.TestCase):
    def test_upgrade_requires_increasing_build_and_keeps_device_scope(self):
        info = {'CFBundleIdentifier': 'com.timilehinaregbesola.mathalarm',
                'MinimumOSVersion': '26.0', 'UIDeviceFamily': [1, 2],
                'CFBundleShortVersionString': '2.3.1', 'CFBundleVersion': '21'}
        self.assertEqual(metadata_violations(info, 20), [])
        self.assertTrue(metadata_violations({**info, 'CFBundleVersion': '20'}, 20))
        self.assertTrue(metadata_violations({**info, 'MinimumOSVersion': '27.0'}, 20))
        self.assertTrue(metadata_violations({**info, 'UIDeviceFamily': [1]}, 20))

    def test_previous_userdefaults_only_manifest_misses_linked_sqlite_categories(self):
        privacy = {'NSPrivacyAccessedAPITypes': [
            {'NSPrivacyAccessedAPIType': 'NSPrivacyAccessedAPICategoryUserDefaults',
             'NSPrivacyAccessedAPITypeReasons': ['CA92.1']}]}
        imports = ' U _stat\n U _fstat\n U _statfs\n U _OBJC_CLASS_$_NSUserDefaults\n'
        findings = required_reason_findings(privacy, imports)
        self.assertEqual(len(findings), 2)
        self.assertIn('FileTimestamp', findings[0])
        self.assertIn('DiskSpace', findings[1])
        privacy['NSPrivacyAccessedAPITypes'].append(
            {'NSPrivacyAccessedAPIType': 'NSPrivacyAccessedAPICategoryFileTimestamp',
             'NSPrivacyAccessedAPITypeReasons': ['C617.1']})
        self.assertEqual(required_reason_findings(privacy, imports), [findings[1]])

    def test_processinfo_does_not_prove_boot_time_access(self):
        self.assertEqual(required_reason_findings({}, ' U _OBJC_CLASS_$_NSProcessInfo'), [])
        self.assertIn('SystemBootTime', required_reason_findings({}, ' U _mach_absolute_time')[0])


if __name__ == '__main__':
    unittest.main()
