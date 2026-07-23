# SmithAI Troubleshooting Guide

If SmithAI is not working as expected, start here.

## Plugin will not load

1. Check that the server is running **Bukkit, Spigot, or Paper 1.21.x**.
   - SmithAI is built against `api-version: 1.21`.
   - Eaglercraft servers use Bukkit-compatible backends and should work, but chat/NPC rendering may vary by client.
2. Check the server console for errors during startup.
3. Make sure `SmithAI-2.0.0.jar` is in the `plugins/` folder.
4. Delete the `SmithAI` plugin data folder and restart to regenerate default configs.

## NPC does not spawn

1. Make sure you have permission: `smithai.spawn` (default: op).
2. Check that you are not already at the NPC limit for your region.
3. Run `/smithai health` to see if the NPC subsystem is healthy.
4. Try `/smithai reload` and then `/smithai spawn` again.

## Smith_AI does not respond to chat

1. Make sure you are talking to the NPC. Look at `Smith_AI` and say something in normal chat, or use `/smithai do <task>`.
2. Check the chat listener range — the NPC must be within listening distance.
3. Run `/smithai debug` to see which brain is active and what the NPC is doing.
4. Check `/smithai health` for failing subsystems.

## External AI is not connecting

1. Check the server URL in `plugins/SmithAI/config.yml`:
   ```yaml
   ai:
     external:
       enabled: true
       url: "https://your-host.example.com"
       apiKey: "SMA-xxxxxxxx"
   ```
2. Run `/SmithAPI status` in-game to see the connection status.
3. Check the SmithAI-Server console for the correct API key. It starts with `SMA-`.
4. Verify the server is reachable from the Minecraft server. Try:
   ```bash
   curl -H "Authorization: Bearer SMA-xxxxxxxx" https://your-host.example.com/health
   ```
5. Make sure the model file exists on the host and is in `SmithAI-Server/models/`.
6. If the server is on Replit, make sure the deployment is public and the model file is uploaded.

## External AI is too slow or uses too much RAM

1. Switch to a smaller GGUF model. For example, use a `Q4_K_M` quantization instead of `Q8_0` or `Q5_K_M`.
2. Lower `context_size` and `max_tokens` in `SmithAI-Server/config.yml`.
3. Lower `n_threads` if the host is shared (e.g., Replit free tier).
4. Use Smith-Mini 1.0 only if you do not need the external model: set `ai.external.enabled: false`.

## Wrong mining advice or version confusion

1. Run `/smithai version` to see the detected server version.
2. On Eaglercraft (1.12), the plugin will avoid advice that requires 1.21 features, such as deepslate mining or Y=-59 diamond levels.
3. If the version is detected incorrectly, check the Bukkit version string in the server startup logs.

## Model file is missing or model not found

1. The server will fall back to rule-based responses if the GGUF model is not found. This is normal if you have not downloaded a model yet.
2. Download a model and place it in `SmithAI-Server/models/`.
3. Update `SmithAI-Server/config.yml` to point at the downloaded file.
4. Use the included helper script:
   ```bash
   cd SmithAI-Server
   python download_model.py --url <direct-gguf-url> --name smithgpt-1.0-4.gguf
   ```

## Server says "Connected to ..." but replies are wrong

1. The plugin might be using the built-in Smith-Mini 1.0 brain if the external AI fails over.
2. Run `/smithai status` to see the active brain.
3. Check `/smithai debug` for the last response source.
4. Send feedback with `/smithai feedback <what went wrong>` so the AI can learn from it.

## Reports or feedback are not saved

1. Check that the plugin data folder exists: `plugins/SmithAI/`.
2. Check file permissions if the server is running under a restricted user.
3. Reports are saved to `plugins/SmithAI/issue_reports.yml`.
4. Feedback is saved to `plugins/SmithAI/training_data.yml` and `plugins/SmithAI/feedback_data.yml`.

## Still stuck?

Use `/smithai report <short description>` in-game to generate a pre-filled GitHub issue. You can also open an issue manually and include:
- Server type and version (from `/smithai version`)
- Active brain (from `/smithai status`)
- Recent console errors
- Steps to reproduce the problem
