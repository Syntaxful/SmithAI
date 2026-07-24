# SmithAI Models

SmithAI supports three built-in brain tiers. The plugin itself ships with **Smith-Mini 1.0** (rule-based). Optional **SmithGPT** servers run on the user's own hardware for larger models.

## Model cards

| Model | Size | RAM | Location | Speed | Best for |
|-------|------|-----|----------|-------|----------|
| Smith-Mini 1.0 | none | 2GB+ | Built into the plugin | Fast | Chat, simple commands, basic tasks |
| SmithGPT 1.0 | ~600MB | 2GB+ | External SmithAI-Server | Fast | Chat, movement, crafting, building, combat |
| SmithGPT 2.0 | ~1.5GB | 4GB+ | External SmithAI-Server | Medium | Planning, strategy, richer chat, automation |

## Preconfigured downloads

SmithAI includes convenience scripts that download ready-to-use quantized models:

```bash
./BuildGPT1.0     # SmithGPT 1.0 — ~600MB (Llama 3.2 1B Instruct Q4_0)
./BuildGPT2.0     # SmithGPT 2.0 — ~1.5GB (Llama 3.2 3B Instruct Q4_0)
./BuildBoth.sh    # both models — ~2.1GB total
```

Re-runs are fast because the scripts reuse the existing virtual environment and skip already-downloaded model files.

### Manual model selection

You can use any compatible GGUF file. Download it into `SmithAI-Server/models/` and update `config.yml`:

```bash
cd SmithAI-Server
python download_model.py --url <direct-gguf-url> --name my-model.gguf
```

```yaml
model:
  path: "models/my-model.gguf"
  name: "My Model"
  context_size: 4096
  max_tokens: 200
  n_threads: 2
```

## Quantization guidance

GGUF files use suffixes like `Q4_0`, `Q4_K_M`, `Q5_K_M`, `Q3_K_S`, etc.

- **Q4_0**: smallest and fastest. Used by the default SmithGPT downloads.
- **Q4_K_M**: good balance of quality and size. Recommended for larger 7B–8B models.
- **Q5_K_M**: slightly better quality, larger file.
- **Q2_K** or **Q3_K_S**: smaller, faster, but lower quality. Use only when RAM is tight.
- **Q6_K** or **Q8_0**: best quality, largest files.

If a model is too large for your host, lower the quantization first. The plugin always falls back to the rule-based Smith-Mini 1.0 brain when no external server is connected.

## License compliance

Each model has its own license. Common ones:

- **Llama 3.x**: Meta Llama 3 Community License
- **Mistral**: Apache 2.0
- **Qwen**: Qwen License / Apache 2.0 depending on version
- **TinyLlama**: Apache 2.0
- **Phi-2**: MIT

Read the license of the model you download before distributing it or using it commercially.
