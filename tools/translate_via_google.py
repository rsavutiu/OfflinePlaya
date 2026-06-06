#!/usr/bin/env python3
"""Translate Compose-Resources strings into many locales via the free
Google endpoint (deep-translator). Quality/coverage beats a local 8B model;
no API key required.

Same contract as the Ollama variant:
  * Existing human translations are PRESERVED; only keys missing relative to
    English are filled. New locales are translated in full.
  * Output rewritten in English key order (guarantees validator key-parity).
  * Placeholders (%1$d, %2$s, …) are masked as {0},{1},… before translation
    and restored after, so the engine never mangles them. A small on-disk
    cache (tools/.trcache/) makes re-runs cheap and idempotent.

Plurals: only `one`+`other` emitted; richer CLDR categories fall back to
`other` at runtime (rough but never a crash). Flagged for native QA.

Run:
    python tools/translate_via_google.py                 # all locales
    python tools/translate_via_google.py values-ar       # subset
"""
import json
import os
import re
import sys
import time
import xml.etree.ElementTree as ET

from deep_translator import GoogleTranslator

ROOT = os.path.join("shared", "src", "commonMain", "composeResources")
EN_PATH = os.path.join(ROOT, "values", "strings.xml")
CACHE_DIR = os.path.join("tools", ".trcache")
CHUNK = 25  # strings joined per request (keeps request count + length sane)

# Android locale dir -> language name + Google language code
LOCALES = {
    "values-de": "German", "values-es": "Spanish", "values-fr": "French",
    "values-it": "Italian", "values-ja": "Japanese", "values-ko": "Korean",
    "values-pt-rBR": "Brazilian Portuguese", "values-ru": "Russian",
    "values-zh-rCN": "Simplified Chinese",
    "values-ar": "Arabic", "values-bg": "Bulgarian", "values-ca": "Catalan",
    "values-cs": "Czech", "values-da": "Danish", "values-el": "Greek",
    "values-fa": "Persian", "values-fil": "Filipino", "values-fi": "Finnish",
    "values-he": "Hebrew", "values-hi": "Hindi", "values-hr": "Croatian",
    "values-hu": "Hungarian", "values-id": "Indonesian", "values-ms": "Malay",
    "values-nb": "Norwegian Bokmal", "values-nl": "Dutch", "values-pl": "Polish",
    "values-pt-rPT": "European Portuguese", "values-ro": "Romanian",
    "values-sk": "Slovak", "values-sl": "Slovenian", "values-sr": "Serbian",
    "values-sv": "Swedish", "values-th": "Thai", "values-tr": "Turkish",
    "values-uk": "Ukrainian", "values-vi": "Vietnamese",
    "values-zh-rTW": "Traditional Chinese", "values-et": "Estonian",
    "values-lt": "Lithuanian", "values-lv": "Latvian",
}
# Android dir code -> Google Translate code (only where they differ)
GCODE = {
    "values-pt-rBR": "pt", "values-pt-rPT": "pt", "values-zh-rCN": "zh-CN",
    "values-zh-rTW": "zh-TW", "values-he": "iw", "values-fil": "tl",
    "values-nb": "no",
}

PH = re.compile(r"%(\d+\$)?[a-zA-Z]")
MASK = re.compile(r"\{\s*(\d+)\s*\}")


def gcode(locale_dir):
    return GCODE.get(locale_dir, locale_dir[len("values-"):])


def android_unescape(s):
    return s.replace('\\"', '"').replace("\\'", "'")


def android_escape(s):
    # Compose Resources renders the raw text content; it does NOT process
    # Android-style \' \" backslash escapes (those render literally as a
    # visible backslash). So only the XML-special characters are escaped;
    # apostrophes and quotes are written raw.
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def mask(text):
    """Replace placeholders with {0},{1},… ; return (masked, originals[])."""
    originals = []

    def repl(m):
        originals.append(m.group(0))
        return "{%d}" % (len(originals) - 1)

    return PH.sub(repl, text), originals


def unmask(text, originals):
    def repl(m):
        i = int(m.group(1))
        return originals[i] if i < len(originals) else m.group(0)

    return MASK.sub(repl, text)


def parse(path):
    tree = ET.parse(path)
    strings, plurals = [], {}
    for el in tree.getroot():
        name = el.get("name")
        if el.tag == "string":
            strings.append((name, android_unescape("".join(el.itertext()))))
        elif el.tag == "plurals":
            plurals[name] = {
                it.get("quantity"): android_unescape("".join(it.itertext()))
                for it in el.findall("item")
            }
    return strings, plurals


def existing_translation(locale_dir):
    path = os.path.join(ROOT, locale_dir, "strings.xml")
    if not os.path.isfile(path):
        return {}, {}
    s, p = parse(path)
    return dict(s), p


def cache_path(code):
    return os.path.join(CACHE_DIR, f"{code}.json")


def load_cache(code):
    p = cache_path(code)
    if os.path.isfile(p):
        with open(p, encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_cache(code, cache):
    os.makedirs(CACHE_DIR, exist_ok=True)
    with open(cache_path(code), "w", encoding="utf-8") as f:
        json.dump(cache, f, ensure_ascii=False, indent=0)


def translate_texts(texts, code, cache):
    """Translate a list of clean English texts -> code. Masks placeholders,
    chunks via newline-join with per-item fallback, caches by source."""
    tr = GoogleTranslator(source="en", target=code)
    todo = [t for t in texts if t not in cache]
    # de-dup while preserving order
    seen, uniq = set(), []
    for t in todo:
        if t not in seen:
            seen.add(t)
            uniq.append(t)

    for i in range(0, len(uniq), CHUNK):
        chunk = uniq[i:i + CHUNK]
        masked, masks = [], []
        for t in chunk:
            m, orig = mask(t)
            masked.append(m)
            masks.append(orig)
        joined = "\n".join(masked)
        ok = False
        try:
            res = tr.translate(joined)
            parts = res.split("\n") if res else []
            if len(parts) == len(chunk):
                for src, out, orig in zip(chunk, parts, masks):
                    cache[src] = unmask(out.strip(), orig)
                ok = True
        except Exception as e:
            print(f"    chunk {i // CHUNK} batch error: {e}")
        if not ok:  # fall back to one-by-one
            for src, m, orig in zip(chunk, masked, masks):
                try:
                    out = tr.translate(m)
                    cache[src] = unmask((out or src).strip(), orig)
                except Exception as e:
                    print(f"    item error ({src!r}): {e}")
                    cache[src] = src  # leave English
                time.sleep(0.2)
        time.sleep(0.3)
    return cache


def write_locale(locale_dir, en_strings, en_plurals):
    code = gcode(locale_dir)
    ex_strings, ex_plurals = existing_translation(locale_dir)
    cache = load_cache(code)

    # English texts that still need a translation for THIS locale
    need_texts = [t for k, t in en_strings if k not in ex_strings]
    for pname, qmap in en_plurals.items():
        for qty, text in qmap.items():
            if pname not in ex_plurals or qty not in ex_plurals.get(pname, {}):
                need_texts.append(text)
    if need_texts:
        translate_texts(need_texts, code, cache)
        save_cache(code, cache)

    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    fell_back = []
    for k, en_text in en_strings:
        if k in ex_strings:
            val = ex_strings[k]
        else:
            val = cache.get(en_text, en_text)
            if val == en_text:
                fell_back.append(k)
        lines.append(f'    <string name="{k}">{android_escape(val)}</string>')

    for pname, qmap in en_plurals.items():
        lines.append(f'    <plurals name="{pname}">')
        for qty, en_text in qmap.items():
            if pname in ex_plurals and qty in ex_plurals[pname]:
                val = ex_plurals[pname][qty]
            else:
                val = cache.get(en_text, en_text)
            lines.append(f'        <item quantity="{qty}">{android_escape(val)}</item>')
        lines.append("    </plurals>")
    lines.append("</resources>")
    lines.append("")

    os.makedirs(os.path.join(ROOT, locale_dir), exist_ok=True)
    with open(os.path.join(ROOT, locale_dir, "strings.xml"), "w",
              encoding="utf-8") as f:
        f.write("\n".join(lines))
    note = f"{len(need_texts)} translated" if need_texts else "no change"
    if fell_back:
        note += f"  !! {len(fell_back)} EN-fallback: {fell_back[:6]}"
    print(f"  wrote {locale_dir} ({note})")


def main():
    en_strings, en_plurals = parse(EN_PATH)
    targets = sys.argv[1:] or list(LOCALES)
    print(f"en: {len(en_strings)} strings, {len(en_plurals)} plurals; "
          f"{len(targets)} locales\n")
    for loc in targets:
        if loc not in LOCALES:
            print(f"  skip {loc}: unknown")
            continue
        print(f"[{loc}] {LOCALES[loc]} -> google '{gcode(loc)}'")
        try:
            write_locale(loc, en_strings, en_plurals)
        except Exception as e:
            print(f"  FAILED {loc}: {e}")


if __name__ == "__main__":
    main()
