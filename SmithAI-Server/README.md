# SmithAI-Server

The official external AI server for the SmithAI Minecraft plugin. It hosts the larger `SmithGPT` models (1.0 / 7.5GB and 2.0 / 15GB) that the plugin can connect to over the network.

## Run anywhere

- Replit
- GitHub Codespaces
- Linux / Windows / macOS
- VPS / dedicated server

## Quick start

```bash
cd SmithAI-Server
pip install -r requirements.txt
python app.py
```

On first start the server generates an API key and prints it to the console. Copy the key and paste it into Minecraft:

```
/SmithAPI set SMA-...
```

## Configuration

Edit `config.yml` to choose the model and host settings:

```yaml
server:
  host: 0.0.0.0
  port: 8000

model:
  name: "SmithGPT 1.0 7.5GB"
  path: "models/smithgpt-1.0-7.5.gguf"
  context_size: 4096
  max_tokens: 200
  n_threads: 2

security:
  api_key: ""
```

If `security.api_key` is empty, the server creates one automatically and saves it to `config.yml`.

## Model files

Place your `.gguf` model files in `SmithAI-Server/models/` and update the `path` in `config.yml`. The server will warn and fall back to rule-based responses if no model is found.

## Endpoints

- `GET /health` — server status
- `POST /chat` — chat with Smith_AI (used by the plugin)
- `GET /skills` — list available skills
- `POST /embed` — embedding placeholder

All endpoints except `/health` require the `Authorization: Bearer SMA-...` header.

## Environment variables

- `PORT` — overrides `server.port` (used by Replit and some hosts)
- `SMITHAI_CONFIG` — path to a custom `config.yml`
