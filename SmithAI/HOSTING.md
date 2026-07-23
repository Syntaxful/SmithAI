# Hosting SmithAI-Server

The `SmithAI-Server` is the external AI host for the SmithGPT models. This guide covers how to run it on **GitHub Codespaces**, **Linux**, **Windows**, and any **VPS**.

## What you need

- A machine with enough RAM for your chosen model:
  - Smith-Mini 1.0 → built into the plugin, no external server needed
  - SmithGPT 1.0 (4GB) → 8GB+ RAM recommended
  - SmithGPT 2.0 (7.5GB) → 12GB+ RAM recommended
- The GGUF model file for the model you want to run (SmithGPT only)
- A way to expose the server URL to the Minecraft plugin

## General setup

1. Clone or download this repository to your host.
2. Go into the `SmithAI-Server` folder.
3. Install Python 3.10+ if needed.
4. Create a virtual environment:
   ```bash
   python -m venv venv
   source venv/bin/activate  # Windows: venv\Scripts\activate
   ```
5. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
6. Put your GGUF model file in `SmithAI-Server/models/`:
   - `smithgpt-1.0-4.gguf` for SmithGPT 1.0
   - `smithgpt-2.0-7.5.gguf` for SmithGPT 2.0
7. Edit `SmithAI-Server/config.yml` and set `model.path` and `model.name`.
8. Run the server:
   ```bash
   python app.py
   ```
9. The server will generate an API key starting with `SMA-` and print it to the console.
10. Copy the API key and paste it into the Minecraft server with:
    ```
    /SmithAPI set SMA-...
    ```
11. The server will stop spamming the key and print `Connected to ...` once the plugin connects.

---

## Docker

For Docker users, the included `docker-compose.yml` handles everything:

```bash
docker-compose up -d
```

This builds the server image, mounts your models folder, and exposes port 8000. Override with `PORT=8080 docker-compose up -d`.

---

## GitHub Codespaces

1. Open the repository in a Codespace.
2. In the terminal, install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Upload the GGUF model file to `SmithAI-Server/models/`.
4. Run the server:
   ```bash
   python app.py
   ```
5. VS Code will prompt you to forward port `8000`. Forward it and make it **public**.
6. Copy the public URL (something like `https://your-codespace-8000.github.dev`).
7. Paste it into the Minecraft plugin config.
8. Copy the API key from the Codespaces terminal and paste it in Minecraft.

---

## Linux / Windows / VPS

1. Install Python 3.10+ and `pip`.
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Place the model file in `SmithAI-Server/models/`.
4. Run the server:
   ```bash
   python app.py
   ```
5. The server listens on port `8000` by default.
6. If the server is on the same network as the Minecraft server, use the local IP:
   ```yaml
   url: "http://192.168.1.50:8000"
   ```
7. If it's on a remote VPS, use the VPS public IP or domain.
8. For a persistent service, use `screen`, `tmux`, or set up a systemd service.

---

## Security notes

- Never share your `SMA-...` API key publicly.
- Players do not need the key. Only the Minecraft server admin needs it.
- If you think the key is compromised, restart the server to generate a new one, or delete the key from `config.yml` and let it regenerate.
- The server now includes a health endpoint that shows whether the model is loaded. Use `GET /health` to check status.
