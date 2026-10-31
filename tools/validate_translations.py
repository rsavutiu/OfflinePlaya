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
  (f) no LLM thinking marker ("/no_think", "<think>") survives in ANY
      values-*/*.xml value — these are machine-translated via a local model,
      and qwen3 glues its own `/no_think` directive onto the last line of a
      batch. Nothing else catches it: key sets and placeholders both still
      validate, so five locales shipped it in v1.0.1 as a visible UI string.

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


THINK_MARKER = re.compile(r"/no_?think|</?think>", re.I)


def find_thinking_markers():
    """Scan every locale resource file — not just strings.xml — for leaked LLM
    thinking markers. Returns a list of "<dir>/<file>: <key>" strings."""
    hits = []
    for entry in sorted(os.listdir(ROOT)):
        if not entry.startswith("values-"):
            continue
        d = os.path.join(ROOT, entry)
        if not os.path.isdir(d):
            continue
        for fname in sorted(os.listdir(d)):
            if not fname.endswith(".xml"):
                continue
            path = os.path.join(d, fname)
            try:
                root = ET.parse(path).getroot()
            except ET.ParseError:
                continue  # (a) reports malformed XML for strings.xml already
            for el in root.iter():
                if el.tag not in ("string", "item"):
                    continue
                if el.text and THINK_MARKER.search(el.text):
                    name = el.get("name") or el.get("quantity") or "?"
                    hits.append(f"{entry}/{fname}: {name}")
    return hits


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
    leaks = find_thinking_markers()
    if leaks:
        print(f"FAIL thinking markers leaked into {len(leaks)} value(s):")
        for h in leaks:
            print(f"     - {h}")
        print()

    if total_fail or leaks:
        if total_fail:
            print(f"{total_fail} locale(s) FAILED")
        sys.exit(1)
    print(f"all {len(locales)} locale(s) passed; no thinking markers")


if __name__ == "__main__":
    main()
