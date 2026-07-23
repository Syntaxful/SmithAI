# SmithAI Models

Place your model files in this directory.

## Plugin-side models (Minecraft server)

- `smith-mini-1.0.gguf` — optional tiny GGUF model for local inference inside the plugin.
  - If not present, Smith-Mini 1.0 falls back to its rule-based brain.
  - Configure the path in `plugins/SmithAI/config.yml` under `ai.local.modelPath`.

## Server-side models (SmithAI-Server)

- `smithgpt-1.0-4.gguf` — 4GB model for SmithGPT 1.0 (recommended: Mistral 7B or Llama 2 7B Q4_K_M)
- `smithgpt-2.0-7.5.gguf` — 7.5GB model for SmithGPT 2.0 (recommended: Mistral-Nemo 12B Q4_K_M or Llama 3 8B Q8_0)

These go in the `SmithAI-Server/models/` directory of the host running the Python server.

## Downloading Models

### Using Hugging Face (Recommended)

```bash
# SmithGPT 1.0 (4GB) — Download a Q4_K_M quantized 7B model
pip install huggingface-hub
huggingface-cli download TheBloke/Mistral-7B-Instruct-v0.3-GGUF mistral-7b-instruct-v0.3.Q4_K_M.gguf \
  --local-dir SmithAI-Server/models --local-dir-use-symlinks False
mv SmithAI-Server/models/mistral-7b-instruct-v0.3.Q4_K_M.gguf SmithAI-Server/models/smithgpt-1.0-4.gguf

# SmithGPT 2.0 (7.5GB) — Download a Q4_K_M quantized 12B model
huggingface-cli download TheBloke/Mistral-Nemo-Instruct-2407-GGUF mistral-nemo-instruct-2407.Q4_K_M.gguf \
  --local-dir SmithAI-Server/models --local-dir-use-symlinks False
mv SmithAI-Server/models/mistral-nemo-instruct-2407.Q4_K_M.gguf SmithAI-Server/models/smithgpt-2.0-7.5.gguf
```

### Using Download Scripts

Pre-built download scripts are provided:
- `SmithAI-Server/download_model.py` — downloads from Hugging Face
- `SmithAI-Server/download_model.sh` — Linux/macOS shell script
- `SmithAI-Server/download_model.bat` — Windows batch file

### Verify Downloads (SHA-256)

After downloading, verify integrity:
```bash
sha256sum SmithAI-Server/models/smithgpt-1.0-4.gguf
# Compare with the checksum provided at the download source (e.g., TheBloke page)
```

## Model Behavior Differences

| Model | Size | Behavior | Best For |
|-------|------|----------|----------|
| Smith-Mini 1.0 | Built-in | Rule-based, ~900 skills, no GPU needed | Low-resource servers, fallback |
| SmithGPT 1.0 | 4GB (7B Q4_K_M) | Conversational, 1800 skills, creative | General gameplay, chat, exploration |
| SmithGPT 2.0 | 7.5GB (12B Q4_K_M) | Advanced reasoning, 6300 skills, strategic | Complex tasks, automation, endgame |

## License Compliance

Different base models have different licenses. You are responsible for complying with the license of the model you download:

- **Mistral models** — Apache 2.0 (permissive for commercial and personal use)
- **Llama models** — Llama 2 Community License (non-commercial requires approval for some uses)
- **CodeLlama / Deepseek** — Check individual model cards

The SmithAI plugin itself is MIT licensed. Model licensing is separate.

## Notes

- You can also use the Docker setup. Mount the model directory into the container:
  `docker run -v /path/to/models:/app/models:ro ...`
- The plugin/server will fall back to rule-based responses if the GGUF model is not found.
- Recommended quantizations: Q4_K_M (best quality/size balance), Q5_K_M (higher quality), Q8_0 (max quality, larger)
