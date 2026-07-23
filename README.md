# SmithAI - MinecraftLLM

The official `SmithAI` Minecraft/Eaglercraft plugin adds a trainable AI companion named `Smith_AI` that can chat, move, build, mine, fight, and work toward tasks like finding diamonds or beating the game.

## What is included

- **One official plugin:** `SmithAI`
- **Built-in brain:** `Smith-Mini 1.0` — works without any external hosting
- **Optional big brains:**
  - `SmithGPT 1.0` — 7.5GB external model
  - `SmithGPT 2.0` — 15GB external model
- **Robot skin** for `Smith_AI`
- **Chat, memory, tasks, skills, and training**
- **Specific feedback system** — tell the AI exactly what it did wrong with `/smithai feedback`
- **GitHub issue reporting** — `/smithai report` opens a prefilled issue with context
- **API key authentication** for external servers (keys start with `SMA-`)
- **Subsystem health monitoring** and in-game debug mode
- **Hosting guide** for Replit, GitHub Codespaces, Linux, Windows, and VPS
- **Docker support** for the Python server
- **CI workflow** for GitHub Actions
- **Build script** (`build.sh`) and **release packaging** (`package-release.sh`) for non-Maven users

## How the brains work

1. By default, `Smith_AI` runs on `Smith-Mini 1.0` locally inside the Minecraft server.
2. If you host `SmithGPT` on another machine and set the URL in the plugin config, `Smith_AI` will use that instead.
3. The external server generates an API key `SMA-...` and prints it to its console. Paste it in Minecraft with `/SmithAPI set SMA-...`.
4. If the external `SmithGPT` server goes offline, `Smith_AI` instantly switches back to `Smith-Mini 1.0` and tells nearby players:  
   `SmithAI hosting is offline. Switched to Smith-Mini 1.0.`
5. The plugin keeps trying to reconnect to the external server and switches back when it returns.

## Model sizes

You choose which model to run on your external server. The plugin only connects to it.

| Model | Size | Recommended server RAM | How it runs |
|-------|------|------------------------|-------------|
| Smith-Mini 1.0 | ~500MB–1.5GB | 2GB+ | Built into the plugin, no external server |
| SmithGPT 1.0 | 7.5GB | 12GB+ | External SmithAI-Server |
| SmithGPT 2.0 | 15GB | 24GB+ | External SmithAI-Server |

## Quick start

1. Build the plugin:
   - With Maven: `cd SmithAI && mvn clean package`
   - Or use the build script: `./build.sh`
2. Copy `SmithAI/target/SmithAI-2.0.0.jar` to your server's `plugins/` folder
3. Start the server
4. Spawn `Smith_AI` with `/smithai spawn`
5. Talk to it: `Smith_AI, follow me` or `Smith_AI, get diamonds`

## External AI server (optional)

For SmithGPT 1.0 or 2.0, see:
- `SmithAI-Server/README.md` — how to run the server
- `HOSTING.md` — how to host on Replit, Codespaces, Linux, Windows, VPS
- `models/README.md` — which model files to download
- `Dockerfile` and `docker-compose.yml` — Docker deployment

## Commands

- `/smithai spawn` — spawn `Smith_AI`
- `/smithai despawn` — remove all `Smith_AI` NPCs
- `/smithai follow` — nearby `Smith_AI` will follow you
- `/smithai stay` — nearby `Smith_AI` will stop following
- `/smithai goto <x> <y> <z>` — send nearby `Smith_AI` to coordinates
- `/smithai do <task>` — ask it to do something (e.g., `get diamonds`, `build nether portal`, `beat the game`)
- `/smithai stop` — cancel all queued tasks
- `/smithai status` — show which brain is active
- `/smithai model` — show available models and active model
- `/smithai reload` — reload config
- `/smithai train good|bad` — reward or punish the AI
- `/smithai feedback <what it did wrong>` — tell the AI exactly what it did wrong
- `/smithai feedback-list` — show recent feedback (admin only)
- `/smithai report <short description>` — open a prefilled GitHub issue
- `/smithai reports` — show recent saved issue reports (admin only)
- `/smithai memory` — show recent conversation
- `/smithai debug` — toggle debug messages for you (admin only)
- `/smithai debug global` — toggle debug messages for all online players (admin only)
- `/smithai health` — show subsystem health summary (admin only)
- `/SmithAPI set <key>` — set the API key for the external server
- `/SmithAPI status` — show current API key and connection status (masked)

You can also tell the AI directly in chat: `don't do that`, `that was wrong`, `never do that`, etc. It will remember it as feedback.

## Skill Library

SmithAI ships with a **9000-core-skill library** generated at runtime so the plugin JAR stays small.

- **Smith-Mini 1.0** — 900 core skills (chat, movement, basic interaction, simple tasks)
- **SmithGPT 1.0** — 1800 core skills (Mini + gathering, crafting, building, combat, farming, exploration)
- **SmithGPT 2.0** — 6300 core skills (GPT-1 + advanced progression, automation, strategy, roleplay, endgame)

Higher-tier brains can use all lower-tier skills. Skills are broad, not item-specific, so the model can reason about goals like "beat the game" or "build a secure base" instead of memorizing thousands of individual recipes.

The library is generated into `plugins/SmithAI/skills.yml` on first start. Delete that file to regenerate it.

## Configuration

Edit `plugins/SmithAI/config.yml` after the first run.

```yaml
ai:
  name: "Smith_AI"
  skin: "robot"
  memory:
    maxMessages: 17
  external:
    enabled: false
    url: "http://localhost:8000"
    apiKey: ""
    model: "smithgpt-1.0-7.5"
  local:
    enabled: true
    modelPath: "plugins/SmithAI/models/smith-mini-1.0.gguf"
    fallbackToRules: true
  reminder:
    enabled: true
    minSeconds: 10
    maxSeconds: 50

debug:
  enabled: false

metrics:
  bstats: false
```

## Building and packaging

```bash
# Build the plugin JAR
./build.sh

# Or with Maven
cd SmithAI && mvn clean package

# Package a full release bundle
./package-release.sh 2.0.0
```

Output:
- `SmithAI/target/SmithAI-2.0.0.jar` — plugin JAR
- `release/SmithAI-v2.0.0.zip` — full release bundle with docs, server, and checksums

## Requirements

- Minecraft server: Spigot, Paper, or compatible (1.21.x)
- Java 17 or newer
- For SmithGPT: a machine with enough RAM for the chosen model

## License

MIT
