#!/usr/bin/env python3
"""Run a small real OpenRouter subtitle translation QA sample.

The script reads OPENROUTER_API_KEY from the environment or local.properties,
never prints the key, and writes source/translated SRT plus a JSON report under
artifacts/translation-qa/.
"""

from __future__ import annotations

import json
import os
import re
import time
import urllib.request
from pathlib import Path


MODEL = "google/gemini-3.5-flash"

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


def api_key(root: Path) -> str:
    value = os.getenv("OPENROUTER_API_KEY") or read_local_property(root, "OPENROUTER_API_KEY")
    if not value:
        raise SystemExit("OPENROUTER_API_KEY is not configured.")
    return value


def system_prompt() -> str:
    return """You are a professional subtitle translator.
Context: This subtitle is from "The Matrix".
Translate from en to he.

Rules:
1. Translate as natural spoken dialogue, not literal word-for-word.
2. Maintain character voice consistency across lines.
3. Keep translations concise — subtitles must be read quickly.
4. Preserve proper nouns, character names, and technical terms.
5. Output EXACTLY the same number of numbered lines as the input.
6. Do NOT merge, split, or reorder lines. One input line = one output line.
7. If a line contains only sound effects like [music], [laughs], or "...", keep it as-is.
8. Multi-line subtitle cues use {LF} as a line separator — preserve {LF} markers in output.
9. Do not add any explanation, commentary, or extra text — only the numbered translated lines."""


def request_translation(key: str) -> tuple[str, float]:
    user_message = "\n".join(f"{idx}: {text}" for idx, _, _, text in ENTRIES)
    body = {
        "model": MODEL,
        "messages": [
            {"role": "system", "content": system_prompt()},
            {"role": "user", "content": user_message},
        ],
        "temperature": 0.1,
        "max_tokens": 12000,
    }
    request = urllib.request.Request(
        "https://openrouter.ai/api/v1/chat/completions",
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json",
            "HTTP-Referer": "https://subty.app",
            "X-Title": "Subty",
        },
        method="POST",
    )

    started = time.time()
    with urllib.request.urlopen(request, timeout=90) as response:
        data = json.loads(response.read().decode("utf-8"))
    return data["choices"][0]["message"]["content"], time.time() - started


def parse_numbered_lines(content: str) -> dict[int, str]:
    line_re = re.compile(r"^(\d+):\s*(.*)$")
    parsed: dict[int, str] = {}
    cleaned = content.replace("```he", "").replace("```", "")
    for raw in cleaned.splitlines():
        match = line_re.match(raw.strip())
        if match:
            parsed[int(match.group(1))] = match.group(2).replace("{LF}", "\n")
    return parsed


def render_srt(parsed: dict[int, str] | None = None) -> str:
    parts = []
    for idx, start, end, source_text in ENTRIES:
        text = parsed.get(idx, source_text) if parsed is not None else source_text
        parts.append(f"{idx}\n{start} --> {end}\n{text}")
    return "\n\n".join(parts) + "\n"


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    content, duration = request_translation(api_key(root))
    parsed = parse_numbered_lines(content)
    expected = [entry[0] for entry in ENTRIES]
    missing = [idx for idx in expected if idx not in parsed]
    extra = [idx for idx in parsed if idx not in expected]
    hebrew_chars = sum(1 for text in parsed.values() for ch in text if "\u0590" <= ch <= "\u05ff")
    english_letters = sum(
        1
        for text in parsed.values()
        for ch in text
        if ("A" <= ch <= "Z") or ("a" <= ch <= "z")
    )

    out_dir = root / "artifacts" / "translation-qa" / time.strftime("%Y%m%d-%H%M%S")
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "matrix_sample.en.srt").write_text(render_srt(), encoding="utf-8")
    (out_dir / "matrix_sample.he.srt").write_text(render_srt(parsed), encoding="utf-8")
    (out_dir / "openrouter_raw_response.txt").write_text(content, encoding="utf-8")
    (out_dir / "report.json").write_text(
        json.dumps(
            {
                "model": MODEL,
                "duration_seconds": round(duration, 2),
                "expected_lines": len(ENTRIES),
                "parsed_lines": len(parsed),
                "missing_indices": missing,
                "extra_indices": extra,
                "hebrew_chars": hebrew_chars,
                "english_letters": english_letters,
                "translated_srt": str(out_dir / "matrix_sample.he.srt"),
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )

    print("openrouter_model_status ok")
    print("model", MODEL)
    print("parsed_lines", len(parsed), "expected_lines", len(ENTRIES))
    print("missing_indices", missing)
    print("extra_indices", extra)
    print("hebrew_chars", hebrew_chars)
    print("english_letters", english_letters)
    print("artifact_dir", out_dir)

    if missing or extra or hebrew_chars <= 0:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
