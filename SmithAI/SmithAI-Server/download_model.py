#!/usr/bin/env python3
"""
Auto-download missing model files for SmithAI-Server.
Prompts user for consent before downloading.

Usage: python3 download_model.py [model_name]
  model_name: smith-mini-1.0, smithgpt-1.0, smithgpt-2.0 (default: auto-detect from config.yml)
"""

import os
import sys
import json
import yaml
from pathlib import Path

MODEL_SOURCES = {
    "smith-mini-1.0": {
        "url": "https://huggingface.co/Syntaxful/Smith-Mini-1.0-GGUF/resolve/main/smith-mini-1.0-q4_k_m.gguf",
        "filename": "smith-mini-1.0-q4_k_m.gguf",
        "size_gb": 0.5,
        "description": "Smith-Mini 1.0 (500MB, local-only, rule-based+GGUF)"
    },
    "smithgpt-1.0": {
        "url": "https://huggingface.co/Syntaxful/SmithGPT-1.0-GGUF/resolve/main/smithgpt-1.0-q4_k_m.gguf",
        "filename": "smithgpt-1.0-q4_k_m.gguf",
        "size_gb": 4.0,
        "description": "SmithGPT 1.0 (4GB, recommended for most users)"
    },
    "smithgpt-2.0": {
        "url": "https://huggingface.co/Syntaxful/SmithGPT-2.0-GGUF/resolve/main/smithgpt-2.0-q4_k_m.gguf",
        "filename": "smithgpt-2.0-q4_k_m.gguf",
        "size_gb": 7.5,
        "description": "SmithGPT 2.0 (7.5GB, maximum capability)"
    },
}

def get_config_model():
    """Read model name from config.yml."""
    config_path = Path(__file__).parent / "config.yml"
    if config_path.exists():
        try:
            with open(config_path) as f:
                config = yaml.safe_load(f)
            return config.get("model", {}).get("name", "auto")
        except Exception:
            return "auto"
    return "auto"

def download_model(model_name):
    if model_name not in MODEL_SOURCES:
        available = ", ".join(MODEL_SOURCES.keys())
        print(f"Unknown model '{model_name}'. Available: {available}")
        return False

    info = MODEL_SOURCES[model_name]
    models_dir = Path(__file__).parent / "models"
    models_dir.mkdir(exist_ok=True)
    dest = models_dir / info["filename"]

    if dest.exists():
        size_mb = dest.stat().st_size / (1024 * 1024)
        print(f"✓ {info['filename']} already exists ({size_mb:.0f} MB)")
        return True

    print(f"\nModel: {info['description']}")
    print(f"Size: {info['size_gb']} GB")
    print(f"URL: {info['url']}")
    print(f"Target: {dest}")

    # User consent
    try:
        response = input("\nDownload this model? [y/N]: ").strip().lower()
    except (EOFError, KeyboardInterrupt):
        response = "n"

    if response not in ("y", "yes"):
        print("Download cancelled.")
        return False

    print(f"\nDownloading {info['filename']}...")
    print("This may take a while depending on your connection speed.\n")

    import urllib.request
    import shutil

    try:
        # Download with progress
        req = urllib.request.Request(info["url"], headers={"User-Agent": "SmithAI-Server/2.0"})
        with urllib.request.urlopen(req) as src:
            with open(dest, "wb") as dst:
                shutil.copyfileobj(src, dst)
        size_mb = dest.stat().st_size / (1024 * 1024)
        print(f"\n✓ Downloaded {info['filename']} ({size_mb:.0f} MB)")
        return True
    except Exception as e:
        print(f"\n✗ Download failed: {e}")
        if dest.exists():
            dest.unlink()
        return False

if __name__ == "__main__":
    print("SmithAI-Server Model Downloader")
    print("=" * 40)

    model_name = sys.argv[1] if len(sys.argv) > 1 else get_config_model()
    if model_name == "auto":
        print("Detecting model from config.yml...")
        model_name = get_config_model()
    if model_name == "auto" or model_name not in MODEL_SOURCES:
        print("Please specify a model name:")
        for k, v in MODEL_SOURCES.items():
            print(f"  {k}: {v['description']}")
        print(f"\nUsage: python3 download_model.py <model_name>")
        sys.exit(1)

    success = download_model(model_name)
    sys.exit(0 if success else 1)
