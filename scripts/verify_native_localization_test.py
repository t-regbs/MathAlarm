import unittest

from verify_native_localization import (LOCALES, REQUIRED_PLURALS, catalog_violations,
                                        presentation_keys)


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

    def test_development_placeholder_does_not_hide_production_delivery_copy(self):
        source = '''Text("Every %@")
struct NativeDevelopmentScreen: View {
    var body: some View { Text("Later milestone") }
}
struct NativePendingDelivery: View { Text("Pending delivery") }
enum NativeAlarmPresentation { NativeStrings.text("Once") }
'''
        self.assertEqual({"Every %@", "Once", "Pending delivery"}, presentation_keys(source))


if __name__ == "__main__":
    unittest.main()
