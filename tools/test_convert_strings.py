"""Tests for convert_strings.py and check_translations.py. Run: python3 -m unittest discover -s tools -p 'test_*.py'"""

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import check_translations  # noqa: E402
import convert_strings as cs  # noqa: E402

LOCALES = ("es", "pt-PT", "pt-BR")


def unit(value):
    return {"stringUnit": {"state": "translated", "value": value}}


def entry(en=None, **translations):
    """A catalog entry in every locale; `en` overrides the English text, the rest default to it."""
    localizations = {loc: unit(translations.get(loc.replace("-", "_"))) for loc in LOCALES}
    if en is not None:
        localizations["en"] = unit(en)
    return {"localizations": localizations}


def build(strings, keymap=None, android=None):
    """Untranslated locales in `entry` default to the English text."""
    for key, item in strings.items():
        localizations = item.get("localizations", {})
        english = localizations.get("en", {}).get("stringUnit", {}).get("value", key)
        for localization in localizations.values():
            if localization.get("stringUnit", {"value": ""})["value"] is None:
                localization["stringUnit"]["value"] = english
    catalog = {"sourceLanguage": "en", "strings": strings}
    return cs.build([catalog], dict(keymap or {}), dict(android or {}))


class ConvertTest(unittest.TestCase):
    def test_plain_key(self):
        result = build({"Try Again": entry(es="Reintentar")})
        self.assertEqual({"Try Again": "try_again"}, result.keymap)
        self.assertEqual("Try Again", result.locales["en"]["try_again"].text)
        self.assertEqual("Reintentar", result.locales["es"]["try_again"].text)

    def test_formats_become_positional_android_specifiers(self):
        result = build({"%@ · Set %lld": entry(es="%1$@ · serie %2$lld")})
        name = result.keymap["%@ · Set %lld"]
        self.assertEqual("%1$s · Set %2$d", result.locales["en"][name].text)
        self.assertEqual("%1$s · serie %2$d", result.locales["es"][name].text)

    def test_translation_with_reordered_arguments_keeps_its_positions(self):
        result = build({"%@ of %@": entry(es="%2$@ de %1$@")})
        self.assertEqual("%2$s de %1$s", result.locales["es"][result.keymap["%@ of %@"]].text)

    def test_translation_with_different_arguments_is_rejected(self):
        result = build({"%lld sets": entry(es="series")})
        self.assertEqual({}, result.keymap)
        self.assertEqual(["%lld sets"], [key for key, _ in result.rejected])

    def test_percent_sign_in_a_format_string_stays_escaped(self):
        result = build({"%lld%%": entry(es="%lld %%")})
        self.assertEqual("%1$d%%", result.locales["en"][result.keymap["%lld%%"]].text)

    def test_a_string_without_arguments_is_literal(self):
        result = build({"About 60% of the sets": entry(es="Alrededor del 60 % de las series")})
        res = result.locales["en"][result.keymap["About 60% of the sets"]]
        self.assertEqual("About 60% of the sets", res.text)
        self.assertFalse(res.formatted)
        self.assertIn('formatted="false"', cs.render(result.locales["en"]))

    def test_plural_variation_becomes_plurals(self):
        plural = {
            "variations": {
                "plural": {"one": unit("%lld exercise"), "other": unit("%lld exercises")},
            }
        }
        es = {"variations": {"plural": {"one": unit("%lld ejercicio"), "other": unit("%lld ejercicios")}}}
        strings = {"%lld exercises": {"localizations": {"en": plural, "es": es, "pt-PT": es, "pt-BR": es}}}
        result = build(strings)
        name = result.keymap["%lld exercises"]
        self.assertEqual("x_exercises", name)
        xml = cs.render(result.locales["es"])
        self.assertIn(f'<plurals name="{name}">', xml)
        self.assertIn('<item quantity="one">%1$d ejercicio</item>', xml)
        self.assertIn('<item quantity="other">%1$d ejercicios</item>', xml)

    def test_a_plural_translation_of_a_plain_english_string_is_rejected(self):
        es = {"variations": {"plural": {"one": unit("%lld serie"), "other": unit("%lld series")}}}
        strings = {"%lld sets": {"localizations": {"es": es, "pt-PT": es, "pt-BR": es}}}
        self.assertEqual(["%lld sets"], [key for key, _ in build(strings).rejected])

    def test_spanish_and_portuguese_plurals_get_many_from_other(self):
        en = {"variations": {"plural": {"one": unit("%lld set"), "other": unit("%lld sets")}}}
        es = {"variations": {"plural": {"one": unit("%lld serie"), "other": unit("%lld series")}}}
        strings = {"%lld sets": {"localizations": {"en": en, "es": es, "pt-PT": es, "pt-BR": es}}}
        xml = cs.render(build(strings).locales["pt-BR"])
        self.assertIn('<item quantity="many">%1$d series</item>', xml)

    def test_apostrophes_quotes_ampersands_and_leading_at_are_escaped(self):
        result = build({"Don't \"say\" A & B <now>": entry(), "@home": entry()})
        xml = cs.render(result.locales["en"])
        self.assertIn("Don\\'t \\\"say\\\" A &amp; B &lt;now&gt;", xml)
        self.assertIn(">\\@home<", xml)

    def test_known_key_keeps_its_name_when_the_english_text_changes(self):
        strings = {"appearance.dark": entry(en="Always dark")}
        result = build(strings, keymap={"appearance.dark": "appearance_dark"})
        self.assertEqual({"appearance.dark": "appearance_dark"}, result.keymap)
        self.assertEqual("Always dark", result.locales["en"]["appearance_dark"].text)

    def test_new_key_gets_a_snake_case_name_from_its_english_text(self):
        result = build({"How's the week, José?": entry(), "1 rest day": entry(), "%@ · %@": entry()})
        self.assertEqual("hows_the_week_jose", result.keymap["How's the week, José?"])
        self.assertEqual("text_1_rest_day", result.keymap["1 rest day"])
        self.assertEqual("x_x", result.keymap["%@ · %@"])

    def test_names_never_collide(self):
        result = build({"Light": entry(), "appearance.light": entry(en="Light")})
        self.assertEqual({"light", "light_2"}, set(result.keymap.values()))

    def test_long_names_are_cut_at_a_word(self):
        name = build({"The coach is reading your check-in and writing a note for you": entry()}).keymap
        self.assertEqual("the_coach_is_reading_your_check_in_and", next(iter(name.values())))

    def test_untranslated_and_stale_keys_are_left_out_and_reported(self):
        strings = {"High": {}, "Old": dict(entry(), extractionState="stale"), "Kept": entry()}
        result = build(strings)
        self.assertEqual({"Kept": "kept"}, result.keymap)
        self.assertEqual(["High"], result.untranslated)

    def test_android_strings_reserve_their_names(self):
        android = {"try_again": {"en": "Try again", "es": "Reintentar", "pt-PT": "Tentar", "pt-BR": "Tentar"}}
        result = build({"Try Again": entry()}, android=android)
        self.assertEqual("try_again_2", result.keymap["Try Again"])
        self.assertEqual("Try again", result.locales["en"]["try_again"].text)

    def test_same_key_in_two_catalogs_is_emitted_once_preferring_the_translated_copy(self):
        bare = {"sourceLanguage": "en", "strings": {"Offline": {}}}
        translated = {"sourceLanguage": "en", "strings": {"Offline": entry(es="Sin conexión", pt_PT="a", pt_BR="b")}}
        result = cs.build([bare, translated, translated], {}, {})
        self.assertEqual({"Offline": "offline"}, result.keymap)
        self.assertEqual("Sin conexión", result.locales["es"]["offline"].text)
        self.assertEqual([], result.untranslated)


    def test_a_live_copy_wins_over_a_stale_one_in_another_catalog(self):
        stale = {"sourceLanguage": "en", "strings": {"Back to mood": dict(entry(), extractionState="stale")}}
        live = {"sourceLanguage": "en", "strings": {"Back to mood": entry(es="Volver", pt_PT="a", pt_BR="b")}}
        result = cs.build([stale, live], {}, {})
        self.assertEqual("Volver", result.locales["es"]["back_to_mood"].text)
        self.assertEqual(0, result.stale)

    def test_a_key_that_drops_out_keeps_its_name_and_no_new_key_takes_it(self):
        result = build({"Save!": entry()}, keymap={"Save": "save"})
        self.assertEqual({"Save": "save", "Save!": "save_2"}, result.keymap)

    def test_an_android_name_equal_to_a_mapped_name_fails(self):
        android = {"save": {"en": "Save", "es": "a", "pt-PT": "b", "pt-BR": "c"}}
        with self.assertRaises(ValueError):
            build({"Save": entry()}, keymap={"Save": "save"}, android=android)

    def test_an_android_string_missing_a_locale_or_with_other_arguments_fails(self):
        with self.assertRaises(ValueError):
            build({}, android={"a": {"en": "A", "es": "a", "pt-PT": "a"}})
        with self.assertRaises(ValueError):
            build({}, android={"a": {"en": "%1$s A", "es": "a", "pt-PT": "a", "pt-BR": "a"}})

    def test_an_english_only_android_string_is_not_translatable(self):
        result = build({}, android={"brand_name": {"en": "Khepri"}})
        self.assertIn('<string name="brand_name" translatable="false">Khepri</string>', cs.render(result.locales["en"]))
        self.assertNotIn("brand_name", result.locales["es"])

    def test_an_unsupported_specifier_is_rejected_not_emitted_as_literal(self):
        result = build({"Time %02d:%02d": entry(), "Rest %s": entry()})
        self.assertEqual({"Time %02d:%02d", "Rest %s"}, {key for key, _ in result.rejected})

    def test_a_translation_that_adds_an_argument_to_a_literal_is_rejected(self):
        result = build({"Carbs, % of target": entry(es="Carbohidratos, %@")})
        self.assertEqual(["Carbs, % of target"], [key for key, _ in result.rejected])

    def test_decimals_and_plain_integers_convert(self):
        result = build({"%.1f kg in %d days": entry()})
        self.assertEqual("%1$.1f kg in %2$d days", result.locales["en"][result.keymap["%.1f kg in %d days"]].text)

    def test_empty_keys_and_empty_values_are_not_emitted(self):
        result = build({"": entry(), "Name": entry(es="")})
        self.assertEqual({}, result.keymap)
        self.assertEqual(["Name"], [key for key, _ in result.rejected])

    def test_a_plural_without_other_is_rejected(self):
        one = {"variations": {"plural": {"one": unit("%lld set")}}}
        strings = {"%lld sets": {"localizations": {"en": one, "es": one, "pt-PT": one, "pt-BR": one}}}
        self.assertEqual(["%lld sets"], [key for key, _ in build(strings).rejected])

    def test_backslashes_newlines_question_marks_and_spaces_are_escaped(self):
        self.assertEqual("a\\\\b\\nc", cs.escape("a\\b\nc"))
        self.assertEqual("\\?why", cs.escape("?why"))
        self.assertEqual("\\u0020kg", cs.escape(" kg"))
        self.assertEqual("\\u0020\\u0020", cs.escape("  "))

    def test_java_keywords_get_a_suffix(self):
        self.assertEqual("new_text", build({"New": entry()}).keymap["New"])


class CheckTranslationsTest(unittest.TestCase):
    def write(self, root, folder, body):
        path = root / "feature/x/src/main/res" / folder / "strings.xml"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f'<?xml version="1.0" encoding="utf-8"?>\n<resources>{body}</resources>\n')

    def test_reports_a_key_missing_from_one_locale(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            both = '<string name="a">A</string><string name="b">B</string>'
            self.write(root, "values", both + '<string name="url" translatable="false">x</string>')
            self.write(root, "values-es", both)
            self.write(root, "values-pt-rPT", both)
            self.write(root, "values-pt-rBR", '<string name="a">A</string>')
            problems = check_translations.problems(root)
        self.assertEqual(["feature/x/src/main/res values-pt-rBR: missing b"], problems)

    def test_reports_an_extra_key_and_passes_a_complete_module(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.write(root, "values", '<string name="a">A</string>')
            for folder in ("values-es", "values-pt-rPT"):
                self.write(root, folder, '<string name="a">A</string>')
            self.write(root, "values-pt-rBR", '<string name="a">A</string><string name="z">Z</string>')
            self.assertEqual(
                ["feature/x/src/main/res values-pt-rBR: not in values: z"], check_translations.problems(root)
            )


    def test_reports_a_kind_change_an_empty_translation_and_a_plural_without_other(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            body = '<string name="a">A</string><plurals name="p"><item quantity="other">P</item></plurals>'
            self.write(root, "values", body)
            self.write(root, "values-es", '<plurals name="a"><item quantity="other">A</item></plurals>'
                       '<plurals name="p"><item quantity="other">P</item></plurals>')
            self.write(root, "values-pt-rPT", '<string name="a"> </string><plurals name="p"><item quantity="other">P</item></plurals>')
            self.write(root, "values-pt-rBR", '<string name="a">A</string><plurals name="p"><item quantity="one">P</item></plurals>')
            self.assertEqual(
                [
                    "feature/x/src/main/res values-es: different kind: a",
                    "feature/x/src/main/res values-pt-rPT: a is empty",
                    "feature/x/src/main/res values-pt-rBR: p has no other",
                ],
                check_translations.problems(root),
            )

if __name__ == "__main__":
    unittest.main()
