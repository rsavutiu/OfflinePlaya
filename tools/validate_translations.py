#!/usr/bin/env python3
"""Validate Compose-Resources translation files against the English source.

For every shared/src/commonMain/composeResources/values-XX/strings.xml this
asserts, relative to the default values/strings.xml:

  (a) XML parses
  (b) string-key set is identical (reports missing / extra)
  (c) plural-key set is identical
  (d) for every string key, the *multiset* of printf placeholders
      (e.g. %1$d, %2$s) matches the English value exactly
  (e) for every plural key, each <item> carries the same placeholder
      multiset as the English "other" item

Placeholder drift and dropped keys are the failures that crash at runtime
(MissingResourceException / IllegalFormat); this catches them deterministically
regardless of translation quality.

Usage:
    python tools/validate_translations.py            # validate all locales
    python tools/validate_translations.py values-ar  # validate one locale dir
Exit code is non-zero if any locale fails.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.join("shared", "src", "commonMain", "composeResources")
EN_DIR = os.path.join(ROOT, "values")
PLACEHOLDER = re.compile(r"%(\d+\$)?[a-zA-Z]")


def placeholders(text):
    """Multiset (sorted list) of printf placeholders in a string."""
    return sorted(m.group(0) for m in PLACEHOLDER.finditer(text or ""))


def parse(path):
    """Return (strings: name->text, plurals: name->{qty->text})."""
    tree = ET.parse(path)
    root = tree.getroot()
    strings, plurals = {}, {}
    for el in root:
        name = el.get("name")
        if el.tag == "string":
            strings[name] = "".join(el.itertext())
        elif el.tag == "plurals":
            plurals[name] = {
                item.get("quantity"): "".join(item.itertext())
                for item in el.findall("item")
            }
    return strings, plurals


def validate(locale_dir, en_strings, en_plurals):
    path = os.path.join(ROOT, locale_dir, "strings.xml")
    errors = []
    try:
        strings, plurals = parse(path)
    except ET.ParseError as e:
        return [f"XML parse error: {e}"]

    # (b) string-key parity
    missing = set(en_strings) - set(strings)
    extra = set(strings) - set(en_strings)
    if missing:
        errors.append(f"missing {len(missing)} string keys: {sorted(missing)}")
    if extra:
        errors.append(f"extra {len(extra)} string keys: {sorted(extra)}")

    # (c) plural-key parity
    pmissing = set(en_plurals) - set(plurals)
    pextra = set(plurals) - set(en_plurals)
    if pmissing:
        errors.append(f"missing plural keys: {sorted(pmissing)}")
    if pextra:
        errors.append(f"extra plural keys: {sorted(pextra)}")

    # (d) placeholder parity per string
    for name in set(en_strings) & set(strings):
        want = placeholders(en_strings[name])
        got = placeholders(strings[name])
        if want != got:
            errors.append(f"placeholder mismatch in '{name}': en={want} got={got}")

    # (e) placeholder parity per plural item (vs en "other")
    for name in set(en_plurals) & set(plurals):
        want = placeholders(en_plurals[name].get("other", ""))
        for qty, text in plurals[name].items():
            got = placeholders(text)
            if got != want:
                errors.append(
                    f"placeholder mismatch in plural '{name}' [{qty}]: en={want} got={got}"
                )
    return errors


def main():
    en_strings, en_plurals = parse(os.path.join(EN_DIR, "strings.xml"))
    if len(sys.argv) > 1:
        locales = sys.argv[1:]
    else:
        locales = sorted(
            d for d in os.listdir(ROOT)
            if d.startswith("values-") and
            os.path.isfile(os.path.join(ROOT, d, "strings.xml"))
        )

    total_fail = 0
    print(f"en source: {len(en_strings)} strings, {len(en_plurals)} plurals\n")
    for loc in locales:
        errs = validate(loc, en_strings, en_plurals)
        if errs:
            total_fail += 1
            print(f"FAIL {loc}")
            for e in errs:
                print(f"     - {e}")
        else:
            print(f"ok   {loc}")
    print()
    if total_fail:
        print(f"{total_fail} locale(s) FAILED")
        sys.exit(1)
    print(f"all {len(locales)} locale(s) passed")


if __name__ == "__main__":
    main()
