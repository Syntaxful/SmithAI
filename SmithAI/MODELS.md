# SmithAI Models

SmithAI supports three built-in brain tiers. The plugin itself ships with **Smith-Mini 1.0** (rule-based). Optional **SmithGPT** servers run on the user's own hardware for larger models.

## Model cards

| Model | Size | RAM | Location | Speed | Best for |
|-------|------|-----|----------|-------|----------|
| Smith-Mini 1.0 | ~500MB–1.5GB | 2GB+ | Built into the plugin | Fast | Chat, simple commands, basic tasks |
| SmithGPT 1.0 | 4GB | 8GB+ | External SmithAI-Server | Medium | Complex tasks, richer chat, 1800 skills |
| SmithGPT 2.0 | 7.5GB | 12GB+ | External SmithAI-Server | Slower | Endgame planning, automation, 6300 skills |

## Skill tiers

- **Smith-Mini 1.0**: 900 core skills (chat, movement, basic interaction, simple tasks)
- **SmithGPT 1.0**: 1800 skills (Mini + gathering, crafting, building, combat, farming, exploration)
- **SmithGPT 2.0**: 6300 skills (GPT-1 + advanced progression, automation, strategy, roleplay, endgame)

Higher-tier brains can use all lower-tier skills.

## Recommended model downloads

Smith-Mini 1.0, SmithGPT 1.0, and SmithGPT 2.0 are model names used by SmithAI. You can use any compatible GGUF model that fits your hardware. Below are practical starting points.

### Smith-Mini 1.0 (~500MB–1.5GB)

Look for small instruction-tuned models such as:
- **TinyLlama** 1.1B Chat (GGUF, ~600MB–1.1GB)
- **Phi-2** 2.7B (GGUF, ~1.5GB–3GB at Q4_K_M)
- **Qwen2.5** 1.5B Instruct (GGUF, ~1GB)

Use cases: offline play, low-end servers, fast responses.

### SmithGPT 1.0 (~4GB)

Look for medium instruction-tuned models at Q4_K_M or similar, such as:
- **Llama 3.1** 8B Instruct Q4_K_M (GGUF, ~4.5GB)
- **Mistral 7B Instruct** Q4_K_M (GGUF, ~4.1GB)
- **Qwen2.5** 7B Instruct Q4_K_M (GGUF, ~4.1GB)
- **Gemma 2** 9B Q4_K_M (GGUF, ~4.5GB)

Use cases: richer reasoning, longer context, better task planning.

### SmithGPT 2.0 (~7.5GB)

Look for larger models at Q4_K_M or dense 7B–9B models at higher quantization, such as:
- **Llama 3.1** 8B Instruct Q8_0 (GGUF, ~7.5GB)
- **Mistral 7B Instruct** Q8_0 (GGUF, ~7.5GB)
- **Qwen2.5** 14B Instruct Q4_K_M (GGUF, ~7.8GB)
- **Gemma 2** 27B Q3_K_M (GGUF, ~7.5GB)
- **Llama 3.1** 70B at Q2_K (GGUF, ~15GB+; too large for this tier)

These still keep the 6300-skill knowledge and planning surface while being far easier to host than the old 15 GB target.

## Download sources

Common places to find GGUF models:
- [Hugging Face](https://huggingface.co/models?sort=downloads&search=gguf) — search for `gguf`
- [TheBloke's Hugging Face repositories](https://huggingface.co/TheBloke) — many popular quantized models
- [LMBSYSorg](https://huggingface.co/lmstudio-community) — community GGUF conversions

## Quantization guidance

GGUF files use suffixes like `Q4_K_M`, `Q5_K_S`, `Q2_K`, etc. The format is `MODELNAME-BITNESS-TYPE.gguf`.

- **Q4_K_M**: good balance of quality and size. Recommended for 7B–8B models.
- **Q5_K_M**: slightly better quality, larger file.
- **Q2_K** or **Q3_K_S**: smaller, faster, but lower quality. Use only when RAM is tight.
- **Q6_K** or **Q8_0**: best quality, largest files. Use if you have plenty of RAM.

## Setting the model

1. Download a GGUF file. You can use the included helper:
   ```bash
   cd SmithAI-Server
   python download_model.py --url <direct-gguf-url> --name smithgpt-1.0-4.gguf
   ```
   Or download manually and place it in your SmithAI-Server directory, e.g., `models/smithgpt-1.0-4.gguf` or `models/smithgpt-2.0-7.5.gguf`.
3. Update `SmithAI-Server/config.yml`:
   ```yaml
   model:
     path: "models/smithgpt-1.0-4.gguf"
     name: "SmithGPT 1.0 4GB"
     context_size: 4096
     max_tokens: 200
     n_threads: 4
   ```
4. Start the server and paste the generated key into Minecraft with `/SmithAPI set SMA-...`.

If `llama-cpp-python` is not installed, the server falls back to rule-based responses so the plugin still works.

## Model verification

GGUF models do not have built-in checksums from SmithAI. Verify your download by:
- Checking the source page's SHA-256 or MD5 if provided.
- Comparing file sizes to the published value.
- Running a quick test with the server's `/health` endpoint.

## License compliance

Each model has its own license. Common ones:
- **Llama 3.x**: Meta Llama 3 Community License
- **Mistral**: Apache 2.0
- **Qwen**: Qwen License / Apache 2.0 depending on version
- **TinyLlama**: Apache 2.0
- **Phi-2**: MIT

Read the license of the model you download before distributing it or using it commercially.

## Notes for model cards

- The `name` in `config.yml` is used for status messages and `/health` responses. It does not need to match the filename.
- The plugin uses the `model` field from the server's `/chat` response to display which brain is active.
- If you switch model files, restart the server and re-run `/SmithAPI set` or use `/smithai reload`.
