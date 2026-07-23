#!/usr/bin/env python3
"""
Model download helper for SmithAI-Server.

Usage:
    python download_model.py --url <direct-gguf-url> --name smithgpt-1.0-4.gguf
    python download_model.py --huggingface <repo_id> --file <filename> --name smithgpt-1.0-4.gguf

Examples:
    python download_model.py --url https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf --name smithgpt-1.0-4.gguf
    python download_model.py --huggingface TheBloke/Mistral-7B-Instruct-v0.2-GGUF --file mistral-7b-instruct-v0.2.Q4_K_M.gguf --name smithgpt-1.0-4.gguf
"""

import argparse
import os
import sys
import urllib.request
import urllib.parse
from pathlib import Path

MODELS_DIR = Path(__file__).parent / "models"


def download(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    print(f"Downloading from {url}")
    print(f"Saving to {destination}")
    try:
        urllib.request.urlretrieve(url, destination, reporthook=_progress)
    except Exception as e:
        print(f"ERROR: download failed: {e}", file=sys.stderr)
        sys.exit(1)
    print(f"\nDone. Saved {destination} ({destination.stat().st_size / (1024 * 1024):.1f} MB)")


def _progress(block_num: int, block_size: int, total_size: int) -> None:
    if total_size > 0:
        downloaded = block_num * block_size
        pct = min(100, downloaded * 100 / total_size)
        mb = downloaded / (1024 * 1024)
        total_mb = total_size / (1024 * 1024)
        print(f"\rProgress: {pct:.1f}% ({mb:.1f} / {total_mb:.1f} MB)", end="", flush=True)
    else:
        print(f"\rDownloaded {block_num * block_size / (1024 * 1024):.1f} MB", end="", flush=True)


def huggingface_url(repo_id: str, filename: str) -> str:
    return f"https://huggingface.co/{repo_id}/resolve/main/{filename}"


def main() -> None:
    parser = argparse.ArgumentParser(description="Download a GGUF model for SmithAI-Server")
    parser.add_argument("--url", help="Direct download URL for a GGUF file")
    parser.add_argument("--huggingface", help="Hugging Face repo ID, e.g. TheBloke/Mistral-7B-Instruct-v0.2-GGUF")
    parser.add_argument("--file", help="Filename inside the Hugging Face repo")
    parser.add_argument("--name", default="model.gguf", help="Local filename to save as")
    args = parser.parse_args()

    if not args.url and not (args.huggingface and args.file):
        parser.print_help()
        sys.exit(1)

    url = args.url or huggingface_url(args.huggingface, args.file)
    destination = MODELS_DIR / args.name

    if destination.exists():
        print(f"WARNING: {destination} already exists. Overwrite? (y/n): ", end="")
        response = input().strip().lower()
        if response != "y":
            print("Cancelled.")
            sys.exit(0)

    download(url, destination)

    print("\nTo use this model, update SmithAI-Server/config.yml:")
    print(f"  path: \"models/{args.name}\"")


if __name__ == "__main__":
    main()
