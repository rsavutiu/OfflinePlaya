"""
Fill in missing translations in shared/src/commonMain/composeResources/values-<locale>/
locale files by querying a local Ollama instance.

Algorithm per locale:
  1. Read master values/strings.xml — that's the source of truth for keys + English values.
  2. Read the locale file — that's the set of keys already translated.
  3. Compute the *missing* set (keys present in master, absent in locale).
  4. Batch-translate the missing values via Ollama. Send VALUES only (one per
     line, numbered). Get translated values back. Never let the model see or
     write the key names — that's how we avoid "settings_section_metadata_tags"
     getting renamed to "settings_nametadata_tags".
  5. Validate the response: same line count, same number of format specifiers
     in each line. Skip the locale entirely if any line fails validation
     (keeps the existing file intact rather than half-translating).
  6. Write the merged locale file: existing translations untouched, missing
     keys appended at the bottom under an auto-generated comment block.

Run from the repo root:
    python scripts/translate_strings.py

Requires: python 3.9+, Ollama running on localhost:11434 with qwen3:8b-ctx16k.
"""

import json
import re
import sys
import urllib.request
from pathlib import Path
from xml.etree import ElementTree as ET

REPO_ROOT = Path(__file__).resolve().parent.parent
RESOURCES_DIR = REPO_ROOT / "shared" / "src" / "commonMain" / "composeResources"
MASTER_FILE = RESOURCES_DIR / "values" / "strings.xml"

LOCALES = {
    "de": "German",
    "es": "Spanish",
    "fr": "French",
    "it": "Italian",
    "ja": "Japanese",
    "ko": "Korean",
    "pt-rBR": "Brazilian Portuguese",
    "ru": "Russian",
    "zh-rCN": "Simplified Chinese",
}

OLLAMA_URL = "http://localhost:11434/api/chat"
MODEL = "qwen3:8b-ctx16k"

SYSTEM_PROMPT = """You are a translation engine for an Android music app's UI strings.

INPUT: a numbered list of English UI strings, one per line, like:
1. Equalizer
2. Settings
3. No tracks

OUTPUT: the SAME numbered list with values translated to the requested language, like:
1. Ecualizador
2. Ajustes
3. Sin pistas

STRICT RULES — violating any of these breaks the build:
- Output EXACTLY the same number of lines, in the same order, with the same numbers.
- Preserve EVERY format specifier verbatim: %1$d, %2$s, %1$s, %s, %d.
- Preserve special characters: em-dash (—), bullet (•), play symbol (▶), ellipsis (…), the &amp; entity, escaped quotes \\" and \\'.
- Do NOT translate proper nouns: OfflinePlaya, MusicBrainz, Android, Material You, Auto, Manual.
- Keep translations short — they go on phone screens.
- Output ONLY the numbered list. No preamble, no explanation, no markdown fences, no thinking."""


def parse_strings_xml(path: Path) -> "dict[str, str]":
    """Return {key: raw-value} for every <string> element. Plurals skipped."""
    if not path.exists():
        return {}
    tree = ET.parse(path)
    out = {}
    for el in tree.getroot().findall("string"):
        name = el.get("name")
        # ET unescapes &amp; → & on read; we'll re-escape on write. But keep
        # backslash-escapes (\\", \\') as-is, since those are Android resource
        # escapes, not XML.
        out[name] = el.text or ""
    return out


def count_format_specs(s: str) -> int:
    return len(re.findall(r"%\d+\$[ds]|%[ds]", s))


def call_ollama(messages: list) -> str:
    payload = {
        "model": MODEL,
        "messages": messages,
        "stream": False,
        "options": {"temperature": 0.2},
        "think": False,
    }
    req = urllib.request.Request(
        OLLAMA_URL,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=600) as resp:
        body = json.loads(resp.read().decode("utf-8"))
    return body["message"]["content"]


def translate_batch(values: list, language: str) -> "list[str] | None":
    """Translate a list of English values. Returns translated list, or None on failure."""
    numbered_input = "\n".join(f"{i + 1}. {v}" for i, v in enumerate(values))
    user_msg = f"Translate these UI strings to {language}. Output the numbered list, nothing else:\n\n{numbered_input}"
    raw = call_ollama([
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": user_msg},
    ])

    # Strip any <think> block that qwen3 sometimes emits even when told not to.
    raw = re.sub(r"<think>.*?</think>", "", raw, flags=re.DOTALL).strip()

    lines = [l for l in raw.splitlines() if l.strip()]
    parsed = []
    for line in lines:
        m = re.match(r"\s*(\d+)\.\s*(.*)$", line)
        if m:
            parsed.append((int(m.group(1)), m.group(2).strip()))

    if len(parsed) != len(values):
        print(f"  ! expected {len(values)} lines, got {len(parsed)}; skipping batch",
              file=sys.stderr)
        return None

    # Validate format-specifier preservation per line.
    out = [None] * len(values)
    for idx, text in parsed:
        i = idx - 1
        if not (0 <= i < len(values)):
            print(f"  ! line index {idx} out of range; skipping batch", file=sys.stderr)
            return None
        if count_format_specs(values[i]) != count_format_specs(text):
            print(
                f"  ! format-specifier mismatch on line {idx} ({values[i]!r} vs {text!r}); skipping batch",
                file=sys.stderr)
            return None
        out[i] = text

    if any(v is None for v in out):
        print(f"  ! gap in numbered output; skipping batch", file=sys.stderr)
        return None
    return out


def xml_escape_value(s: str) -> str:
    # Re-escape what ET unescaped on read. Order matters — & first.
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return s


def write_merged_locale(locale_path: Path, existing: dict, new_translations: dict, language: str):
    """Append new translations to the existing locale file, preserving everything that's already there."""
    if not existing and not new_translations:
        return

    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for key, value in existing.items():
        lines.append(f'    <string name="{key}">{xml_escape_value(value)}</string>')

    if new_translations:
        lines.append("")
        lines.append(
            f"    <!-- Auto-translated to {language} via local LLM; review and refine. -->")
        for key, value in new_translations.items():
            lines.append(f'    <string name="{key}">{xml_escape_value(value)}</string>')

    lines.append("</resources>")
    locale_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


BATCH_SIZE = 20


def main():
    master = parse_strings_xml(MASTER_FILE)
    print(f"Master: {len(master)} string keys at {MASTER_FILE.relative_to(REPO_ROOT)}")

    for locale_code, language in LOCALES.items():
        locale_path = RESOURCES_DIR / f"values-{locale_code}" / "strings.xml"
        existing = parse_strings_xml(locale_path)
        missing_keys = [k for k in master if k not in existing]
        print(
            f"\n[{locale_code}] {language}: {len(existing)} existing, {len(missing_keys)} missing")

        if not missing_keys:
            print(f"  already complete; nothing to do")
            continue

        new_translations = {}
        for start in range(0, len(missing_keys), BATCH_SIZE):
            batch_keys = missing_keys[start:start + BATCH_SIZE]
            batch_values = [master[k] for k in batch_keys]
            print(
                f"  translating batch {start + 1}–{start + len(batch_keys)} of {len(missing_keys)}...")
            translated = translate_batch(batch_values, language)
            if translated is None:
                print(f"  batch failed validation, leaving these keys untranslated for this locale")
                continue
            for k, v in zip(batch_keys, translated):
                new_translations[k] = v

        if new_translations:
            locale_path.parent.mkdir(parents=True, exist_ok=True)
            write_merged_locale(locale_path, existing, new_translations, language)
            print(
                f"  wrote {len(new_translations)} new translations to {locale_path.relative_to(REPO_ROOT)}")
        else:
            print(f"  no translations produced; locale file untouched")


if __name__ == "__main__":
    main()
