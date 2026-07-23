# SmithAI-Server

This is the official external AI server for the `SmithAI` Minecraft plugin. It runs the larger `SmithGPT` models (1.0 7.5GB or 2.0 15GB) on a host of your choice, and the Minecraft plugin connects to it over HTTP.

## What it does

- Loads a GGUF model file
- Generates a secure API key starting with `SMA-` on first start
- Exposes a `/chat` endpoint for the plugin (requires the API key)
- Provides `/health`, `/skills`, and `/embed` endpoints
- Lets you host `SmithGPT 1.0` or `SmithGPT 2.0` on Replit, GitHub Codespaces, Linux, Windows, VPS, or your own PC

## Quick start

1. Install Python 3.10+ and create a virtual environment:
   ```bash
   python -m venv venv
   source venv/bin/activate  # on Windows: venv\Scripts\activate
   ```

2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

3. Download the model you want:
   - `SmithGPT 1.0` (7.5GB) — place at `models/smithgpt-1.0-7.5.gguf`
   - `SmithGPT 2.0` (15GB) — place at `models/smithgpt-2.0-15.gguf`

4. Edit `config.yml` to set the correct `model.path` and `model.name`.

5. Run the server:
   ```bash
   python app.py
   ```

6. On first start, the server prints an API key like:
   ```
   SMA-aB3x9QzL2mN7wE4rT6yU8iO1pK5jH3gF
   ```
   Copy it.

7. In the Minecraft server, paste the key with:
   ```
   /SmithAPI set SMA-aB3x9QzL2mN7wE4rT6yU8iO1pK5jH3gF
   ```

8. The plugin will now send authenticated requests to the external server.

## Hosting platforms

- **Replit:** Use the web server workflow; make the port public.
- **GitHub Codespaces:** Forward port 8000 and make it public.
- **Linux / Windows:** Run directly on any machine with enough RAM.
- **VPS:** Run with `screen`, `tmux`, or as a systemd service.

## Model sizes

| Model | File size | Recommended server RAM |
|-------|-----------|------------------------|
| SmithGPT 1.0 | 7.5GB | 12GB+ |
| SmithGPT 2.0 | 15GB | 24GB+ |

## Notes

- The server falls back to a simple response if the model file is not found.
- Keep `n_threads` low on shared hosts to avoid lagging the Minecraft server.
- The Minecraft plugin will switch to `Smith-Mini 1.0` if this server becomes unreachable.
- Never share your `SMA-...` API key publicly. Players do not need it — only the server admin.
