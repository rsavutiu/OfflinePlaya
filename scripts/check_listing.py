"""
Validate the store listing in fastlane/metadata/android/<locale>/ against the
rules Play actually enforces.

Checks, per locale:
  * title <= 30, short_description <= 80, full_description <= 4000 characters
  * every glossary term that appears in en-US still appears (so a translation
    can't quietly rename OfflinePlaya, FLAC, Android Auto, …)
  * no price/promotion keyword in the title or short description — Play flags
    those and marks the app "may not be promoted on Google Play", which is a
    discovery penalty, not a rejection, so nothing in the build would catch it
  * no characters from a script the locale doesn't use, which is how
    machine-translated copy fails (see the warning in translate_strings.py)

The listing text is hand-written, NOT generated: a qwen3:8b pass was tried and
rejected — it produced "Musikspeler" for Dutch, "Player Music" for Italian,
"Pemain musik" (musician) for Indonesian and katakana'd the app name in
Japanese. Treat any future generated copy as a draft to be read, not output to
be committed.

Run from the repo root:
    python scripts/check_listing.py
"""

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
METADATA = REPO_ROOT / "fastlane" / "metadata" / "android"
REFERENCE = "en-US"

LIMITS = {"title": 30, "short_description": 80, "full_description": 4000}

GLOSSARY = [
    "OfflinePlaya", "MP3", "FLAC", "M4A", "OGG", "WAV", "Opus",
    "Android Auto", "Material You", "USB DAC", "Windows", "Mac", "Linux",
]

PROMO_WORDS = [
    "gratis", "kostenlos", "gratuit", "grátis", "gratuito", "бесплатн",
    "ücretsiz", "darmow", "bezpłatn", "無料", "free download", "no ads",
    "sin anuncios", "sans publicité", "ohne werbung", "senza pubblicità",
    "best ", "#1", "top ", "nr. 1", "n°1",
]

SCRIPT_BLOCKS = {
    "cyrillic": r"[Ѐ-ӿ]",
    "cjk": r"[一-鿿]",
    "kana": r"[぀-ヿ]",
    "devanagari": r"[ऀ-ॿ]",
    "arabic": r"[؀-ۿ]",
    "hangul": r"[가-힯]",
}

# Locales whose own writing system is one of the blocks above. Everything else
# is expected to be Latin-only.
ALLOWED_SCRIPTS = {
    "ru-RU": {"cyrillic"},
    "ja-JP": {"cjk", "kana"},
}


def read_fields(locale: str) -> "dict[str, str]":
    d = METADATA / locale
    out = {}
    for name in LIMITS:
        path = d / f"{name}.txt"
        if path.exists():
            out[name] = path.read_text(encoding="utf-8").strip()
    return out


def main() -> int:
    if not METADATA.is_dir():
        print(f"no metadata directory at {METADATA}", file=sys.stderr)
        return 2

    reference = read_fields(REFERENCE)
    if not reference:
        print(f"no {REFERENCE} listing to compare against", file=sys.stderr)
        return 2
    required_terms = [
        t for t in GLOSSARY if t in "\n".join(reference.values())
    ]

    locales = sorted(
        p.name for p in METADATA.iterdir()
        if p.is_dir() and (p / "title.txt").exists()
    )
    problems = {}

    for locale in locales:
        fields = read_fields(locale)
        found = []

        for name, limit in LIMITS.items():
            if name not in fields:
                found.append(f"{name}.txt missing")
                continue
            if len(fields[name]) > limit:
                found.append(
                    f"{name} is {len(fields[name])} chars, limit {limit}"
                )

        joined = "\n".join(fields.values())
        for term in required_terms:
            if term not in joined:
                found.append(f"glossary term missing: {term}")

        for word in PROMO_WORDS:
            for name in ("title", "short_description"):
                if word.lower() in fields.get(name, "").lower():
                    found.append(f"promotion keyword '{word.strip()}' in {name}")

        allowed = ALLOWED_SCRIPTS.get(locale, set())
        for script, pattern in SCRIPT_BLOCKS.items():
            if script not in allowed and re.search(pattern, joined):
                found.append(f"unexpected {script} characters")

        if found:
            problems[locale] = found
        else:
            print(
                f"{locale}: ok — title {len(fields['title'])}ch, "
                f"short {len(fields['short_description'])}ch, "
                f"full {len(fields['full_description'])}ch"
            )

    if problems:
        print("", file=sys.stderr)
        for locale, found in problems.items():
            print(f"{locale}:", file=sys.stderr)
            for p in found:
                print(f"  - {p}", file=sys.stderr)
        print(
            f"\n{len(locales) - len(problems)} ok, {len(problems)} with problems",
            file=sys.stderr,
        )
        return 1

    print(f"\n{len(locales)} locales ok")
    return 0


if __name__ == "__main__":
    sys.exit(main())
