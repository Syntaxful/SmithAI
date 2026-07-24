# SmithAI Models

Place your model files in this directory.

## Plugin-side models (Minecraft server)

- `smith-mini-1.0.gguf` — optional tiny GGUF model for local inference inside the plugin.
  - If not present, Smith-Mini 1.0 falls back to its rule-based brain.
  - Configure the path in `plugins/SmithAI/config.yml` under `ai.local.modelPath`.

## Server-side models (SmithAI-Server)

- `smithgpt-1.0-4.gguf` — 4GB model for SmithGPT 1.0
- `smithgpt-2.0-7.5.gguf` — 7.5GB model for SmithGPT 2.0

These go in the `SmithAI-Server/models/` directory of the host running the Python server.

## Model size and quantization trade-offs

SmithAI is designed to work with the smallest model that still covers your use case. Each tier is tested against the features it needs to support:

- **Smith-Mini 1.0** (~300 MB - 1 GB): intended for local plugin-side inference or very low-end hosts. Use Q4_0 or Q4_K_M quantization. The plugin falls back to its built-in rule brain if this model is missing.
- **SmithGPT 1.0** (~4 GB): the default external brain for chat, task planning, and basic world interaction. Q4_K_M or Q5_K_M is the sweet spot.
- **SmithGPT 2.0** (~7.5 GB): the largest brain for advanced strategy, endgame progression, and long-context planning. Q4_K_M is recommended; only move to Q5_K_M or Q8_0 if you have spare RAM and need higher reasoning quality.

General guidance:
- Lower quantization (Q4) = smaller file, lower RAM, faster inference, slightly lower quality.
- Higher quantization (Q8/FP16) = larger file, more RAM, slower inference, better quality.
- Distilled models usually give the best quality-per-megabyte.
- If a model is slow or causes out-of-memory errors, reduce the quantization level or switch to the next smaller tier.

## Notes

- You can also use the Docker setup. Mount the model directory into the container:
  `docker run -v /path/to/models:/app/models:ro ...`
- The plugin/server will fall back to rule-based responses if the GGUF model is not found.
