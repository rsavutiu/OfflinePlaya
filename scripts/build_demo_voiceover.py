"""
Render the demo video's voiceover (store-listing/demo-voiceover.md) to a single
WAV aligned to the video's timeline.

Each section is synthesized on its own, then padded with silence so it starts
at its timestamp — so the finished track drops straight onto the 3:09 demo with
no manual nudging. A section that overruns its slot is reported rather than
silently overlapping the next one.

Requires: edge-tts (pip install edge-tts) and ffmpeg on PATH.

    python scripts/build_demo_voiceover.py [--voice en-GB-RyanNeural]

Writes build/voiceover/voiceover.wav.
"""

import argparse
import asyncio
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = REPO_ROOT / "build" / "voiceover"

VIDEO_LENGTH = 189  # 3:09
DEFAULT_VOICE = "en-GB-RyanNeural"

# (start second, section name, text). Timings match the headings in
# store-listing/demo-voiceover.md — keep the two in step.
SECTIONS = [
    (0, "hook",
     "If you still keep your own music — ripped CDs, Bandcamp downloads, "
     "DJ sets, bootlegs — streaming apps are the wrong tool for it. "
     "This one is built for files."),
    # The hook needs 13s read at this rate, so it gets a 14s slot rather than
    # being rushed — it is the line that decides whether anyone keeps watching.
    (14, "permission",
     "OfflinePlaya asks for access to the audio already on your phone. "
     "That's the only thing it ever asks for. No account, no sign-up, "
     "no email."),
    (30, "folder",
     "You point it at a folder, and it reads your music into a proper "
     "library. It handles MP3, FLAC, M4A, OGG, WAV and Opus — and it reads "
     "the tags, the embedded artwork, and the folder covers sitting next to "
     "your files."),
    (55, "browse",
     "From there you browse the way your collection is actually organised — "
     "by artist, by album, or straight down the folder tree, which most "
     "players won't let you do. Tags wrong? Fix them in the app. It writes "
     "the change back into the file, so every other app sees your correction "
     "too."),
    (85, "eq",
     "Ten-band equalizer, with presets tuned by hand. Or let Auto mode pick "
     "one from each track's genre as it plays."),
    (115, "dac",
     "Plug in a USB DAC and, on Android 14 and up, it hands your DAC an "
     "untouched signal — bit-perfect, with the system mixer out of the way."),
    (140, "playtopc",
     "And it plays to your computer. Over your own WiFi, to a companion app "
     "on Windows, Mac or Linux — straight out of your real speakers. Nothing "
     "is saved on the PC, and it never leaves your home network."),
    (165, "close",
     "No ads. No tracking. No account. No subscription. Just the music you "
     "already own, on the phone you already have. OfflinePlaya. On Google "
     "Play, and on GitHub."),
]

SAMPLE_RATE = 48000


async def synth(text: str, voice: str, rate: str, path: Path) -> None:
    import edge_tts
    await edge_tts.Communicate(text, voice, rate=rate).save(str(path))


def to_wav(src: Path, dst: Path) -> None:
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", str(src),
         "-ar", str(SAMPLE_RATE), "-ac", "1", str(dst)],
        check=True,
    )


def duration(path: Path) -> float:
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "default=nw=1:nk=1", str(path)],
        check=True, capture_output=True, text=True,
    )
    return float(out.stdout.strip())


def silence(seconds: float, path: Path) -> None:
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi",
         "-i", f"anullsrc=r={SAMPLE_RATE}:cl=mono",
         "-t", f"{max(seconds, 0.01):.3f}", str(path)],
        check=True,
    )


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--voice", default=DEFAULT_VOICE)
    ap.add_argument("--rate", default="-8%",
                    help="speech rate; negative slows it down")
    args = ap.parse_args()

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    parts = []
    overruns = []

    for i, (start, name, text) in enumerate(SECTIONS):
        mp3 = OUT_DIR / f"{name}.mp3"
        wav = OUT_DIR / f"{name}.wav"
        print(f"{name}: synthesizing...", flush=True)
        asyncio.run(synth(text, args.voice, args.rate, mp3))
        to_wav(mp3, wav)
        spoken = duration(wav)

        end = SECTIONS[i + 1][0] if i + 1 < len(SECTIONS) else VIDEO_LENGTH
        slot = end - start
        if spoken > slot:
            overruns.append((name, spoken, slot))
        print(f"  {spoken:5.1f}s spoken, {slot:5.1f}s slot "
              f"({'OVER' if spoken > slot else 'ok'})")

        parts.append(wav)
        gap = slot - spoken
        if gap > 0:
            pad = OUT_DIR / f"{name}_pad.wav"
            silence(gap, pad)
            parts.append(pad)

    listing = OUT_DIR / "concat.txt"
    listing.write_text(
        "\n".join(f"file '{p.as_posix()}'" for p in parts) + "\n",
        encoding="utf-8",
    )
    final = OUT_DIR / "voiceover.wav"
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0",
         "-i", str(listing), "-c", "copy", str(final)],
        check=True,
    )

    total = duration(final)
    print(f"\nwrote {final}  ({total:.1f}s, target {VIDEO_LENGTH}s)")

    if overruns:
        print("\nSections that run past their slot — shorten the text or lower "
              "--rate:", file=sys.stderr)
        for name, spoken, slot in overruns:
            print(f"  {name}: {spoken:.1f}s in a {slot:.1f}s slot",
                  file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
