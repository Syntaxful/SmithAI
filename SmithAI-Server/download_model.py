#!/usr/bin/env python3
"""
Fast, resumable model download helper for SmithAI-Server.

Usage:
    python download_model.py --url <direct-gguf-url> --name smithgpt-1.0-1b.gguf
    python download_model.py --huggingface <repo_id> --file <filename> --name smithgpt-1.0-1b.gguf
    python download_model.py --url <url> --name model.gguf --skip-existing

Prefers curl/wget for resume support and speed, falls back to urllib.
"""

import argparse
import os
import shutil
import subprocess
import sys
import urllib.request
from pathlib import Path
from typing import Optional

MODELS_DIR = Path(__file__).parent / "models"

CHUNK_SIZE = 1024 * 1024  # 1 MiB


def which(cmd: str) -> Optional[str]:
    return shutil.which(cmd)


def _curl_download(url: str, destination: Path, resume: bool = True) -> bool:
    """Download with curl. Returns True on success."""
    args = ["curl", "-L", "--fail", "--show-error"]
    if resume and destination.exists():
        args.extend(["-C", "-"])
    args.extend(["-o", str(destination), url])
    try:
        subprocess.run(args, check=True)
        return True
    except Exception as e:
        print(f"curl download failed: {e}", file=sys.stderr)
        return False


def _wget_download(url: str, destination: Path) -> bool:
    """Download with wget. Returns True on success."""
    args = ["wget", "--continue", "--show-progress", "-O", str(destination), url]
    try:
        subprocess.run(args, check=True)
        return True
    except Exception as e:
        print(f"wget download failed: {e}", file=sys.stderr)
        return False


def _urllib_download(url: str, destination: Path) -> None:
    """Fallback urllib download with progress."""
    destination.parent.mkdir(parents=True, exist_ok=True)
    print(f"Downloading from {url}")
    print(f"Saving to {destination}")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "SmithAI/2.0.0"})
        with urllib.request.urlopen(req, timeout=30) as response:
            total_size = int(response.headers.get("Content-Length", 0))
            downloaded = 0
            with open(destination, "wb") as f:
                while True:
                    chunk = response.read(CHUNK_SIZE)
                    if not chunk:
                        break
                    f.write(chunk)
                    downloaded += len(chunk)
                    if total_size > 0:
                        pct = min(100, downloaded * 100 / total_size)
                        print(f"\rProgress: {pct:.1f}% ({downloaded / (1024*1024):.1f} / {total_size / (1024*1024):.1f} MB)", end="", flush=True)
                    else:
                        print(f"\rDownloaded {downloaded / (1024*1024):.1f} MB", end="", flush=True)
        print(f"\nDone. Saved {destination} ({destination.stat().st_size / (1024*1024):.1f} MB)")
    except Exception as e:
        print(f"ERROR: download failed: {e}", file=sys.stderr)
        sys.exit(1)


def download(url: str, destination: Path, skip_existing: bool = False) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)

    if destination.exists():
        if skip_existing:
            print(f"{destination} already exists. Skipping download.")
            return
        print(f"WARNING: {destination} already exists. Overwrite? (y/n): ", end="")
        response = input().strip().lower()
        if response != "y":
            print("Cancelled.")
            sys.exit(0)
        destination.unlink()

    # Prefer curl/wget for resume and speed.
    if which("curl"):
        print(f"Downloading {destination.name} with curl...")
        if _curl_download(url, destination):
            print(f"Done. Saved {destination} ({destination.stat().st_size / (1024*1024):.1f} MB)")
            return
    if which("wget"):
        print(f"Downloading {destination.name} with wget...")
        if _wget_download(url, destination):
            print(f"Done. Saved {destination} ({destination.stat().st_size / (1024*1024):.1f} MB)")
            return

    print("curl/wget not available, falling back to urllib...")
    _urllib_download(url, destination)


def huggingface_url(repo_id: str, filename: str) -> str:
    return f"https://huggingface.co/{repo_id}/resolve/main/{filename}"


def main() -> None:
    parser = argparse.ArgumentParser(description="Download a GGUF model for SmithAI-Server")
    parser.add_argument("--url", help="Direct download URL for a GGUF file")
    parser.add_argument("--huggingface", help="Hugging Face repo ID, e.g. bartowski/Llama-3.2-1B-Instruct-GGUF")
    parser.add_argument("--file", help="Filename inside the Hugging Face repo")
    parser.add_argument("--name", default="model.gguf", help="Local filename to save as")
    parser.add_argument("--skip-existing", action="store_true", help="Skip download if file already exists")
    args = parser.parse_args()

    if not args.url and not (args.huggingface and args.file):
        parser.print_help()
        sys.exit(1)

    url = args.url or huggingface_url(args.huggingface, args.file)
    destination = MODELS_DIR / args.name

    download(url, destination, skip_existing=args.skip_existing)

    print("\nTo use this model, update SmithAI-Server/config.yml:")
    print(f'  path: "models/{args.name}"')


if __name__ == "__main__":
    main()
