import unittest
from pathlib import Path
import plistlib
import tempfile

from verify_native_localization import (LOCALES, REQUIRED_PLURALS, TONES, catalog_violations,
                                        packaged_violations, presentation_keys)


def catalog_fixture():
    question = {locale: {"variations": {"plural": {
        category: {"stringUnit": {"state": "translated", "value": "%lld questions"}}
        for category in categories}}} for locale, categories in REQUIRED_PLURALS.items()}
    return {"sourceLanguage": "en", "version": "1.0", "strings": {
        "%lld questions": {"localizations": question},
        "Every %@": {"localizations": {locale: {"stringUnit": {
            "state": "translated", "value": "Every %@"}} for locale in LOCALES}},
    }}


class NativeLocalizationTest(unittest.TestCase):
    def test_all_locales_and_plural_forms_pass(self):
        self.assertEqual([], catalog_violations(catalog_fixture()))

    def test_missing_locale_and_blank_translation_fail(self):
        catalog = catalog_fixture()
        translations = catalog["strings"]["Every %@"]["localizations"]
        translations.pop("bn")
        translations["de"]["stringUnit"]["value"] = ""
        errors = catalog_violations(catalog)
        self.assertTrue(any("all nine locales" in error for error in errors))
        self.assertTrue(any("missing translated value" in error for error in errors))

    def test_incompatible_format_type_and_missing_russian_plural_fail(self):
        catalog = catalog_fixture()
        catalog["strings"]["Every %@"]["localizations"]["de"]["stringUnit"]["value"] = "Jeden %lld"
        catalog["strings"]["%lld questions"]["localizations"]["ru"]["variations"]["plural"].pop("few")
        errors = catalog_violations(catalog)
        self.assertTrue(any("format argument mismatch" in error for error in errors))
        self.assertTrue(any("categories missing for ru" in error for error in errors))

    def test_new_native_control_requires_catalog_key(self):
        errors = catalog_violations(catalog_fixture(), {
            "Controls.swift": 'Section("New control") { Text(" ") }'})
        self.assertEqual(["Controls.swift: uncataloged presentation key 'New control'"], errors)

    def test_semantic_error_helper_requires_catalog_key(self):
        source = 'enum NativeStrings { static func error() -> String { text("New failure") } }'
        self.assertEqual({"New failure"}, presentation_keys(source))

    def test_challenge_input_and_accessible_problem_copy_are_audited(self):
        source = '''TextField("Answer", text: answer)
        Label("Challenge progress", systemImage: "function")
        Text(NativeStrings.text("Submit answer"))
        .accessibilityHint(Text("Enter a whole number."))
        '''
        self.assertEqual({"Answer", "Challenge progress", "Submit answer", "Enter a whole number."},
                         presentation_keys(source))

    def test_translator_can_reorder_two_native_formatted_numbers(self):
        catalog = catalog_fixture()
        catalog["strings"]["Question %@ of %@"] = {"localizations": {
            locale: {"stringUnit": {"state": "translated", "value": "%2$@ / %1$@"}}
            for locale in LOCALES}}
        self.assertEqual([], catalog_violations(catalog))

    def test_dynamic_sound_and_difficulty_sources_are_audited(self):
        source = '''
        let names = ["Easy", "Medium"]
        operation(symbol: "+", label: "Addition")
        previewMessage = "Preview failed"
        case "alarm_orbit": "Flowing synth · bright"
        NativeStrings.text(playing ? "Stop preview" : "Preview")
        Text("\\(lower)–\\(upper)")
        // Text("Not production copy")
        '''
        self.assertEqual({"Easy", "Medium", "Addition", "Preview failed", "Flowing synth · bright",
                          "Stop preview", "Preview"}, presentation_keys(source))

    def test_development_placeholder_copy_is_no_longer_excluded(self):
        source = '''Text("Every %@")
struct NativeDevelopmentScreen: View {
    var body: some View { Text("Later milestone") }
}
struct NativePendingDelivery: View { Text("Pending delivery") }
enum NativeAlarmPresentation { NativeStrings.text("Once") }
'''
        self.assertEqual({"Every %@", "Once", "Pending delivery", "Later milestone"}, presentation_keys(source))

    def test_settings_announcements_native_errors_and_intents_are_audited(self):
        source = '''
        settingsLabel("Send Feedback", detail: "Send feedback to the developer", symbol: "envelope")
        failure = "No email app is available."
        static var title: LocalizedStringResource = "Stop Alarm"
        @Parameter(title: "Alarm ID") var alarmId: String
        String(localized: "Solve Math")
        NavigationLink("Test Alarm", value: .preview)
        NativeStrings.text(id == "math-challenges-v1" ? "More ways to wake up" : "Snooze on your terms")
        '''
        self.assertEqual({"Send Feedback", "Send feedback to the developer", "No email app is available.",
                          "Stop Alarm", "Alarm ID", "Solve Math", "Test Alarm", "More ways to wake up",
                          "Snooze on your terms"}, presentation_keys(source))

    def test_permission_catalog_requires_translations_without_question_plural(self):
        info = {"sourceLanguage": "en", "version": "1.0", "strings": {
            "NSAlarmKitUsageDescription": {"localizations": {locale: {"stringUnit": {
                "state": "translated", "value": "MathAlarm uses scheduled alarms."}} for locale in LOCALES}}}}
        self.assertEqual([], catalog_violations(info, require_question_plurals=False))
        info["strings"]["NSAlarmKitUsageDescription"]["localizations"].pop("pa")
        self.assertTrue(catalog_violations(info, require_question_plurals=False))

    def test_compiled_resources_detect_missing_locale_key_sound_and_launch(self):
        catalog = catalog_fixture()
        with tempfile.TemporaryDirectory() as directory:
            app = Path(directory)
            for locale in LOCALES:
                localized = app / f"{locale}.lproj"
                localized.mkdir()
                (localized / "Localizable.strings").write_bytes(plistlib.dumps({"Every %@": "Every %@"}))
                (localized / "Localizable.stringsdict").write_bytes(plistlib.dumps({"%lld questions": {}}))
            for tone in TONES:
                (app / f"alarm_{tone}.caf").write_bytes(b"caff\0\x01")
            (app / "Assets.car").write_bytes(b"compiled")
            launch = app / "Base.lproj/LaunchScreen.storyboardc"
            launch.mkdir(parents=True)
            (launch / "Info.plist").write_bytes(plistlib.dumps({}))
            (app / "Info.plist").write_bytes(plistlib.dumps({"UILaunchStoryboardName": "LaunchScreen"}))
            self.assertEqual([], packaged_violations(app, {"Localizable": catalog}))
            (app / "pa.lproj/Localizable.stringsdict").unlink()
            (app / "alarm_orbit.caf").write_bytes(b"bad")
            (app / "Assets.car").unlink()
            errors = packaged_violations(app, {"Localizable": catalog})
            self.assertEqual(3, len(errors))
            self.assertTrue(any("Localizable/pa" in error for error in errors))
            self.assertTrue(any("alarm_orbit.caf" in error for error in errors))
            self.assertTrue(any("Assets.car" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
