# SmithAI Models

Place your model files in this directory.

## Plugin-side models (Minecraft server)

- `smith-mini-1.0.gguf` — optional tiny GGUF model for local inference inside the plugin.
  - If not present, Smith-Mini 1.0 falls back to its rule-based brain.
  - Configure the path in `plugins/SmithAI/config.yml` under `ai.local.modelPath`.

## Server-side models (SmithAI-Server)

- `smithgpt-1.0-7.5.gguf` — 7.5GB model for SmithGPT 1.0
- `smithgpt-2.0-15.gguf` — 15GB model for SmithGPT 2.0

These go in the `SmithAI-Server/models/` directory of the host running the Python server.

## Notes

- You can also use the Docker setup. Mount the model directory into the container:
  `docker run -v /path/to/models:/app/models:ro ...`
- The plugin/server will fall back to rule-based responses if the GGUF model is not found.
