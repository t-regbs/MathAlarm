import pathlib
import tempfile
import unittest

from verify_native_ui_boundaries import violations


class NativeUIBoundaryTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.directory.name)

    def tearDown(self):
        self.directory.cleanup()

    def write(self, path, content):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content)

    def test_renderer_import_and_resources_are_rejected_in_shared(self):
        self.write('shared/src/commonMain/kotlin/Feature.kt', 'import androidx.compose.runtime.State\n')
        self.write('shared/src/commonMain/composeResources/drawable/logo.xml', '<vector/>')
        self.assertEqual(2, len(violations(self.root)))

    def test_native_service_sdk_is_allowed_only_in_platform_source_set(self):
        self.write('shared/src/iosMain/kotlin/Audio.kt', 'import platform.UIKit.UIImpactFeedbackGenerator\n')
        self.assertEqual([], violations(self.root))
        self.write('shared/src/commonMain/kotlin/Feature.kt', 'import platform.UIKit.UIView\n')
        self.assertEqual(1, len(violations(self.root)))

    def test_renderer_dependency_and_ios_compatibility_are_rejected(self):
        self.write('shared/build.gradle.kts', 'api(libs.compose.runtime)\nswiftExport { }\n')
        self.write('iosApp/iosApp/ContentView.swift', 'MainViewControllerKt.MainViewController()\n')
        self.assertEqual(2, len(violations(self.root)))

    def test_comments_and_android_rendering_are_allowed(self):
        self.write('shared/build.gradle.kts', '// Removed swiftExport { } and libs.compose.runtime\n')
        self.write('iosApp/iosApp/ContentView.swift', '// Removed ComposeView\nimport SwiftUI\n')
        self.write('androidApp/src/main/kotlin/Screen.kt', 'import androidx.compose.runtime.State\n')
        self.assertEqual([], violations(self.root))

    def test_production_framework_api_and_renderer_exports_are_checked(self):
        framework = self.root / 'app.framework'
        self.write('app.framework/Headers/app.h', 'IosApplication SharedFeatures AlarmListViewModel AlarmSettingsViewModel\n')
        self.assertEqual([], violations(self.root, framework))
        self.write('app.framework/Headers/app.h', 'IosApplication SharedFeatures AlarmListViewModel AlarmSettingsViewModel ComposeUIViewController\n')
        self.assertEqual(1, len(violations(self.root, framework)))
        self.write('app.framework/Headers/app.h', 'IosApplication SharedFeatures AlarmListViewModel AlarmSettingsViewModel AppAlarmDatabase_Impl AppKoinCoreKoin AppChallengeProgressStore\n')
        self.assertEqual(1, len(violations(self.root, framework)))

    def test_resolved_dependency_report_rejects_transitive_renderer(self):
        report = self.root / 'dependencies.log'
        report.write_text('+--- com.rickclephas.kmp:kmp-observableviewmodel-core:1.1.0\n')
        self.assertEqual([], violations(self.root, dependencies=report))
        report.write_text('+--- org.jetbrains.compose.runtime:runtime:1.12.0\n')
        self.assertEqual(1, len(violations(self.root, dependencies=report)))


if __name__ == '__main__':
    unittest.main()
