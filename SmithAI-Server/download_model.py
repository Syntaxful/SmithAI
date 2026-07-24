#!/usr/bin/env python3
"""
Fast, resumable, multi-threaded model download helper for SmithAI-Server.

Usage:
    python download_model.py --url <direct-gguf-url> --name smithgpt-1.0-2.2gb.gguf
    python download_model.py --huggingface <repo_id> --file <filename> --name smithgpt-1.0-2.2gb.gguf
    python download_model.py --url <url> --name model.gguf --skip-existing

Prefers curl/wget for resume support and speed, falls back to urllib,
and supports a fast multi-threaded chunk downloader for large models.
"""

import argparse
import os
import shutil
import subprocess
import sys
import threading
import urllib.request
from pathlib import Path
from typing import Optional

MODELS_DIR = Path(__file__).parent / "models"

CHUNK_SIZE = 1024 * 1024  # 1 MiB
MAX_PARALLEL_CHUNKS = 8


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


def _fetch_range(url: str, start: int, end: int) -> bytes:
    """Fetch a byte range."""
    req = urllib.request.Request(url, headers={
        "User-Agent": "SmithAI/2.0.1",
        "Range": f"bytes={start}-{end}",
    })
    with urllib.request.urlopen(req, timeout=60) as response:
        return response.read()


def _parallel_download(url: str, destination: Path, threads: int = MAX_PARALLEL_CHUNKS) -> None:
    """Multi-threaded download using HTTP ranges."""
    destination.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(url, headers={"User-Agent": "SmithAI/2.0.1"})
    with urllib.request.urlopen(req, timeout=30) as response:
        total_size = int(response.headers.get("Content-Length", 0))
    if total_size <= 0:
        raise RuntimeError("Server did not provide Content-Length; cannot parallel download")

    print(f"Fast parallel download: {threads} chunks, {total_size / (1024*1024*1024):.2f} GB total")

    chunk_size = total_size // threads
    ranges = [(i * chunk_size, min((i + 1) * chunk_size - 1, total_size - 1)) for i in range(threads)]
    parts = [b""] * threads
    errors = [None] * threads
    lock = threading.Lock()

    def worker(i, start, end):
        try:
            data = _fetch_range(url, start, end)
            with lock:
                parts[i] = data
        except Exception as e:
            errors[i] = e

    threads_list = [threading.Thread(target=worker, args=(i, start, end)) for i, (start, end) in enumerate(ranges)]
    for t in threads_list:
        t.start()
    for t in threads_list:
        t.join()

    for e in errors:
        if e is not None:
            raise RuntimeError(f"Parallel download failed: {e}")

    with open(destination, "wb") as f:
        for part in parts:
            f.write(part)


def _urllib_download(url: str, destination: Path) -> None:
    """Fallback urllib download with progress."""
    destination.parent.mkdir(parents=True, exist_ok=True)
    print(f"Downloading from {url}")
    print(f"Saving to {destination}")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "SmithAI/2.0.1"})
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


def download(url: str, destination: Path, skip_existing: bool = False, parallel: bool = True) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)

    if destination.exists():
        if skip_existing:
            print(f"{destination} already exists. Skipping download.")
            return
        print(f"WARNING: {destination} already exists. Overwriting in non-interactive mode...")
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

    if parallel:
        try:
            print(f"Attempting fast parallel download for {destination.name}...")
            _parallel_download(url, destination)
            print(f"Done. Saved {destination} ({destination.stat().st_size / (1024*1024):.1f} MB)")
            return
        except Exception as e:
            print(f"Parallel download failed ({e}), falling back to single-stream...", file=sys.stderr)

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
    parser.add_argument("--parallel", type=int, default=MAX_PARALLEL_CHUNKS, help="Parallel chunks for fast download")
    args = parser.parse_args()

    if not args.url and not (args.huggingface and args.file):
        parser.print_help()
        sys.exit(1)

    url = args.url or huggingface_url(args.huggingface, args.file)
    destination = MODELS_DIR / args.name

    download(url, destination, skip_existing=args.skip_existing, parallel=args.parallel > 1)

    print("\nTo use this model, update SmithAI-Server/config.yml:")
    print(f'  path: "models/{args.name}"')


if __name__ == "__main__":
    main()
