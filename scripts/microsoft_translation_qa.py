#!/usr/bin/env python3
"""Run a small real Microsoft Translator subtitle QA sample.

Reads MICROSOFT_API_KEY and MICROSOFT_REGION from the environment or
local.properties, never prints secrets, and writes source/translated SRT plus a
JSON report under artifacts/translation-qa/.
"""

from __future__ import annotations

import json
import os
import time
import urllib.parse
import urllib.request
from urllib.error import HTTPError
from pathlib import Path


ENTRIES = [
    (1, "00:00:01,000", "00:00:03,000", "I know why you are here, Neo."),
    (2, "00:00:03,500", "00:00:06,000", "You have felt it your entire life."),
    (3, "00:00:06,500", "00:00:09,000", "There is something wrong with the world."),
    (4, "00:00:09,500", "00:00:12,000", "Do you want to know what it is?"),
    (5, "00:00:12,500", "00:00:14,000", "The Matrix has you."),
]


def read_local_property(root: Path, key: str) -> str | None:
    local_props = root / "local.properties"
    if not local_props.exists():
        return None
    for line in local_props.read_text(errors="ignore").splitlines():
        if line.strip().startswith(f"{key}="):
            value = line.split("=", 1)[1].strip()
            return value or None
    return None


def config(root: Path) -> tuple[str, str]:
    key = os.getenv("MICROSOFT_API_KEY") or read_local_property(root, "MICROSOFT_API_KEY")
    region = os.getenv("MICROSOFT_REGION") or read_local_property(root, "MICROSOFT_REGION") or "global"
    if not key:
        raise SystemExit("MICROSOFT_API_KEY is not configured.")
    return key, region


def request_translation(key: str, region: str) -> tuple[list[str], float]:
    query = urllib.parse.urlencode({"api-version": "3.0", "from": "en", "to": "he"})
    body = json.dumps([{"Text": entry[3]} for entry in ENTRIES]).encode("utf-8")
    request = urllib.request.Request(
        f"https://api.cognitive.microsofttranslator.com/translate?{query}",
        data=body,
        headers={
            "Ocp-Apim-Subscription-Key": key,
            "Content-Type": "application/json",
        },
        method="POST",
    )
    if region and region.lower() != "global":
        request.add_header("Ocp-Apim-Subscription-Region", region)

    started = time.time()
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            data = json.loads(response.read().decode("utf-8"))
    except HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")[:300]
        raise SystemExit(f"microsoft_translation_failed HTTP {exc.code}: {body}") from exc
    translations = [item["translations"][0]["text"] for item in data]
    return translations, time.time() - started


def render_srt(translations: list[str] | None = None) -> str:
    parts = []
    for index, (idx, start, end, source_text) in enumerate(ENTRIES):
        text = translations[index] if translations is not None else source_text
        parts.append(f"{idx}\n{start} --> {end}\n{text}")
    return "\n\n".join(parts) + "\n"


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    key, region = config(root)
    translations, duration = request_translation(key, region)

    hebrew_chars = sum(1 for text in translations for ch in text if "\u0590" <= ch <= "\u05ff")
    english_letters = sum(
        1
        for text in translations
        for ch in text
        if ("A" <= ch <= "Z") or ("a" <= ch <= "z")
    )

    out_dir = root / "artifacts" / "translation-qa" / time.strftime("%Y%m%d-%H%M%S-microsoft")
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "matrix_sample.en.srt").write_text(render_srt(), encoding="utf-8")
    (out_dir / "matrix_sample.he.srt").write_text(render_srt(translations), encoding="utf-8")
    (out_dir / "report.json").write_text(
        json.dumps(
            {
                "provider": "microsoft",
                "duration_seconds": round(duration, 2),
                "expected_lines": len(ENTRIES),
                "parsed_lines": len(translations),
                "hebrew_chars": hebrew_chars,
                "english_letters": english_letters,
                "translated_srt": str(out_dir / "matrix_sample.he.srt"),
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )

    print("microsoft_translation_status ok")
    print("parsed_lines", len(translations), "expected_lines", len(ENTRIES))
    print("duration_seconds", round(duration, 2))
    print("hebrew_chars", hebrew_chars)
    print("english_letters", english_letters)
    print("artifact_dir", out_dir)

    if len(translations) != len(ENTRIES) or hebrew_chars <= 0:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
