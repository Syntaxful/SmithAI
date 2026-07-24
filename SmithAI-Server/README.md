# SmithAI-Server

The official external AI server for the SmithAI Minecraft plugin. It hosts the optional `SmithGPT` models (1.0 / ~600MB and 2.0 / ~1.5GB) that the plugin can connect to over the network.

## Run anywhere

- GitHub Codespaces
- Linux / Windows / macOS
- VPS / dedicated server

## Quick start

### Linux / macOS

From the repository root:

```bash
./BuildGPT1.0     # fastest: ~600MB, Llama 3.2 1B Instruct Q4_0
./use-gpt1.0.sh   # start the server
```

Or for the larger model:

```bash
./BuildGPT2.0     # ~1.5GB, Llama 3.2 3B Instruct Q4_0
./use-gpt2.0.sh   # start the server
```

To download both and switch later:

```bash
./BuildBoth.sh
./use-gpt1.0.sh   # or ./use-gpt2.0.sh
```

Re-runs are fast: the scripts reuse the virtual environment and skip already-downloaded model files.

### Windows

```cmd
cd SmithAI-Server
start.bat
```

### Manual run

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
  name: "SmithGPT 1.0 1B"
  path: "models/smithgpt-1.0-1b.gguf"
  context_size: 4096
  max_tokens: 200
  n_threads: 2

security:
  api_key: ""
```

If `security.api_key` is empty, the server creates one automatically and saves it to `config.yml`.

## Model files

Place your `.gguf` model files in `SmithAI-Server/models/` and update the `path` in `config.yml`. The server will warn and fall back to rule-based responses if no model is found.

### Model size vs capability trade-offs

SmithAI supports three model tiers. The smaller the quantization, the smaller the file and the faster the inference, but response quality may degrade. Choose the smallest model that still satisfies the feature set you need:

| Tier | Recommended size | Quantization | Notes |
|------|------------------|--------------|-------|
| Smith-Mini 1.0 | none | — | Runs inside the plugin. Rule-based fallback keeps the plugin working without any model. |
| SmithGPT 1.0 | ~600MB | Q4_0 | Llama 3.2 1B Instruct. Fast chat, movement, crafting, building, combat. |
| SmithGPT 2.0 | ~1.5GB | Q4_0 | Llama 3.2 3B Instruct. Better planning, strategy, richer chat. |

- **Q4_0** is the smallest and fastest. It is used by the default SmithGPT downloads to keep setup times short.
- **Q4_K_M / Q5_K_M** balance quality and size; recommended for larger 7B–8B models if you want more quality than speed.
- **Q8_0** or **FP16** give the highest quality but double or triple the file size and RAM use.
- Prefer **distilled** models (e.g., Llama-3.1-Instruct, DeepSeek-R1-Distill) when available, because they preserve instruction-following at smaller sizes.
- If a model is too large for your host, lower the quantization first. If it still does not fit, switch to a smaller tier; the rule-based fallback always remains available.

You can also use the included helper to download a model from a direct URL or Hugging Face:

```bash
# Linux / macOS
./download_model.sh --url <direct-gguf-url> --name my-model.gguf

# Windows
download_model.bat --url <direct-gguf-url> --name my-model.gguf

# Or use the Python script directly
python download_model.py --url <direct-gguf-url> --name my-model.gguf --skip-existing
```

For Hugging Face repos, use `--huggingface <repo_id>` and `--file <filename>` instead of `--url`.

## Endpoints

- `GET /health` — server status
- `POST /chat` — chat with Smith_AI (used by the plugin)
- `GET /skills` — list available skills
- `POST /embed` — embedding placeholder

All endpoints except `/health` require the `Authorization: Bearer SMA-...` header.

## Environment variables

- `PORT` — overrides `server.port` (used by many hosting platforms)
- `SMITHAI_CONFIG` — path to a custom `config.yml`
