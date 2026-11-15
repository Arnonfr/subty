#!/usr/bin/env python3
import os
from flask import Flask, jsonify, request
from waitress import serve

import argostranslate.package
import argostranslate.translate


app = Flask(__name__)


def ensure_model(from_code: str, to_code: str) -> bool:
    installed = argostranslate.translate.get_installed_languages()
    from_lang = next((l for l in installed if l.code == from_code), None)
    if from_lang is not None:
        if any(t.to_lang.code == to_code for t in from_lang.translations_from):
            return True

    argostranslate.package.update_package_index()
    available = argostranslate.package.get_available_packages()
    pkg = next((p for p in available if p.from_code == from_code and p.to_code == to_code), None)
    if pkg is None:
        return False
    path = pkg.download()
    argostranslate.package.install_from_path(path)
    return True


def translate_text(text: str, source: str, target: str) -> str:
    return argostranslate.translate.translate(text, source, target)


@app.get("/health")
def health():
    return jsonify({"ok": True})


@app.get("/languages")
def languages():
    pairs = []
    for lang in argostranslate.translate.get_installed_languages():
        for tr in lang.translations_from:
            pairs.append({"source": lang.code, "target": tr.to_lang.code})
    return jsonify(pairs)


@app.post("/translate")
def translate():
    payload = request.get_json(silent=True) or {}
    text = payload.get("q", "")
    source = payload.get("source")
    target = payload.get("target")
    if not isinstance(text, str) or not source or not target:
        return jsonify({"error": "Missing q/source/target"}), 400

    if not ensure_model(source, target):
        return jsonify({"error": f"Model not available for {source}->{target}"}), 400

    translated = translate_text(text, source, target)
    return jsonify({"translatedText": translated})


if __name__ == "__main__":
    host = os.getenv("ARGOS_SERVER_HOST", "127.0.0.1")
    port = int(os.getenv("ARGOS_SERVER_PORT", "5001"))
    serve(app, host=host, port=port)
