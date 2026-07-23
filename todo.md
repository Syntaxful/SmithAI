# SmithAI - MinecraftLLM Master TODO

This file is the single source of truth for what is finished and what remains. It is updated every time the user says `commit` or `STOP WORK`. Do not edit it casually.

## Current Snapshot (as of this commit)

- Plugin builds successfully with Maven: `SmithAI/target/SmithAI-2.0.0.jar` (~138KB shaded JAR)
- SmithAI-Server Python FastAPI server exists and can run with `python app.py`
- API key generation, console spam, and "Connected to ..." messages are implemented
- External AI failover to Smith-Mini 1.0 is implemented
- Built-in knowledge base loader exists with expanded entries across blocks, mobs, items, recipes, strategy, biomes
- Chat, memory, commands, listeners, tab completers exist
- **9000-core-skill library implemented** with runtime generation to keep the JAR small
  - 900 skills for Smith-Mini 1.0
  - 1800 skills for SmithGPT 1.0 (includes all Mini skills)
  - 6300 skills for SmithGPT 2.0 (includes all Mini + GPT-1 skills)
- Skill tier-aware registry, skill dispatcher, and skill executor queue implemented
- Task planner updated with broad, non-specific goals: beat the game, get diamonds, build nether portal, build base, defend, farm, etc.
- `/smithai do <task>` now queues skills into the executor
- `/smithai stop` added to cancel the active skill queue
- `/smithai goto <x> <y> <z>` added to send an NPC to coordinates
- `/smithai feedback <message>` added so players can describe exactly what the AI did wrong
- `/smithai report <description>` opens a prefilled GitHub issue; reports are also saved to `issue_reports.yml` if the URL is too long
- `/smithai reports` and `/smithai feedback-list` added for admins to review submissions
- `/smithai debug` and `/smithai debug global` added for in-game diagnostics
- `/smithai health` added to show subsystem health summary
- `/SmithAPI status` added to show key, server URL, model, and connection status
- Training manager persists good/bad feedback to disk
- Feedback manager persists detailed written feedback with context to disk
- Issue report manager persists bug reports to disk
- NPC follow/stay/teleport exists, plus velocity-based movement with step-up/jump and `goto` target support
- Real block breaking, block placing, and torch placing implemented in the skill dispatcher
- Tool selection by task (pickaxe, axe, sword, shovel) implemented
- Basic combat against nearest hostile or targeted mob type implemented
- Subsystem health tracking and graceful degradation implemented
- Debug manager for per-player and global debug toggles implemented
- CI workflow (`.github/workflows/build.yml`) builds the plugin and checks the server on every push
- `build.sh` script for non-Maven users; runs tests during build
- `package-release.sh` bundles plugin JAR, server, docs, and SHA-256 checksums into a zip
- `bump-version.sh` updates the project version in `pom.xml`
- `Makefile` with common build/test/release/docker targets
- Docker support for SmithAI-Server: `Dockerfile`, `docker-compose.yml`, `.dockerignore`
- Unit tests added: `SkillGeneratorTest`, `TaskPlannerTest`, `SubsystemHealthTest`
- Server-side test added: `SmithAI-Server/test_app.py`
- README, SKILLS.md, FAQ.md, HOSTING.md, CONTRIBUTING.md, REPORT_TEMPLATE.md, MODELS.md, API.md, TROUBLESHOOTING.md, PRIVACY.md, and models README updated
- Smarter Smith-Mini 1.0 with expanded conversational intents and version-aware mining advice
- VersionInfo detector for Bukkit/Eaglercraft with `hasDeepslate`, `hasNetherite`, `bestDiamondY`, etc.
- `/smithai version` command showing detected server version and feature flags
- SmithGPT model sizes reduced: SmithGPT 1.0 → 4GB, SmithGPT 2.0 → 7.5GB, with updated docs, configs, and tier detection
- Server startup scripts (`start.sh`, `start.bat`) and model download helper (`download_model.py`, `.sh`, `.bat`)
- Workspace environment fixed: `.replit` module + `replit.nix` provide Maven and JDK 17 alongside Python 3.11, server has a public `/` status endpoint, and `SmithAI-Server/requirements-server.txt` is the lightweight runtime dependency list
- GitHub commit rule: user is the only committer and only contributor; `GITHUB_PERSONAL_ACCESS_TOKEN` is used only with explicit authorization

---

## Project Goal (unchanged)

Build a single, official Minecraft/Eaglercraft plugin (`SmithAI`) that adds AI-controlled NPCs (`Smith_AI`) with player-like models, natural chat, long-term memory, and a task engine. The plugin includes a built-in small model (`Smith-Mini 1.0`) and can connect to an optional external model (`SmithGPT 1.0` 4GB or `SmithGPT 2.0` 7.5GB) hosted on Replit, Codespaces, Linux, Windows, VPS, or any machine the user chooses. The user picks the model by running the matching `SmithAI-Server` and pointing the plugin at its URL/IP/port. No signup, free forever.

---

## Skill Tiers (UPDATED)

Each brain has a fixed set of core skills it can execute. Higher brains can use all lower-brain skills too.

- **Smith-Mini 1.0** — 900 core skills (built-in, runs in plugin)
- **SmithGPT 1.0** — 1800 core skills (external, 4GB model)
- **SmithGPT 2.0** — 6300 core skills (external, 7.5GB model)
- **Total core skills** — 9000 (overlapping tiers)

Skills are generated at runtime by `SkillGenerator` into `plugins/SmithAI/skills.yml` so the plugin JAR stays small (~138KB). Higher-tier models include all lower-tier skills.

Each skill needs:
1. A unique ID
2. A human-readable name
3. A tier (`mini`, `gpt1`, `gpt2`)
4. A broad description/prompt for the LLM (not item-specific)
5. Required parameters and types (currently empty; to be expanded)
6. Preconditions (tool, item, environment, permission)
7. A primitive or composite executor in Java
8. A failure/retry policy
9. Success criteria
10. Knowledge tags for context lookup

---

## DONE vs TODO Matrix

### 1. Build & Packaging
- [x] Maven project structure under `SmithAI/`
- [x] `pom.xml` with Bukkit/Paper API dependency
- [x] `plugin.yml` with `api-version: 1.21`, commands, permissions
- [x] `config.yml` default configuration
- [x] Maven shade plugin producing shaded JAR
- [x] Build produces `SmithAI/target/SmithAI-2.0.0.jar` (~138KB)
- [x] Runtime skill generation keeps JAR small
- [x] Automated build script for non-Maven users (`build.sh`)
- [x] CI workflow (GitHub Actions) to build on push (`.github/workflows/build.yml`)
- [x] Release packaging script that bundles plugin JAR, server, and docs (`package-release.sh`)
- [x] Signed releases or checksums (SHA-256 generated in `package-release.sh`)
- [x] Version bumping automation (`bump-version.sh`)
- [ ] Maven release plugin / GitHub Releases integration
- [ ] Automated artifact upload to GitHub Releases

### 2. Plugin Lifecycle
- [x] `SmithAIPlugin` main class with `onEnable` and `onDisable`
- [x] Config loading and validation
- [x] Command and listener registration
- [x] Graceful shutdown: despawn NPCs, save memory, cancel tasks
- [x] Reload command that re-inits config, AI manager, reminder task
- [x] Skill executor stops on shutdown
- [x] Proper error recovery when a subsystem fails to initialize (`SubsystemHealth`)
- [x] In-game debug mode toggle (`/smithai debug`, `DebugManager`)
- [ ] Plugin metrics / bStats integration (config key exists, integration not wired)

### 3. Configuration System
- [x] `Config` wrapper class with all current keys
- [x] `ai.name` (Smith_AI)
- [x] `ai.skin` placeholder
- [x] `ai.memory.maxMessages` (17)
- [x] `ai.external.enabled`
- [x] `ai.external.url`
- [x] `ai.external.apiKey`
- [x] `ai.external.model` (model name string)
- [x] `ai.external.timeout`
- [x] `ai.local.enabled`
- [x] `ai.local.modelPath`
- [x] `ai.local.fallbackToRules`
- [x] `ai.reconnectInterval`
- [x] `ai.statusMessage`
- [x] `ai.reminder.enabled`
- [x] `ai.reminder.minSeconds` and `ai.reminder.maxSeconds`
- [x] `ai.followDistance`
- [x] `skills.maxQueueSize`
- [x] `skills.stepDelay`
- [x] `training.chatFeedback`
- [x] `training.persist`
- [x] `debug.enabled`
- [x] `metrics.bstats`
- [x] `ai.pathfinding.maxDistance`
- [x] `ai.pathfinding.maxNodes`
- [x] `ai.pathfinding.tickRate`
- [x] `ai.combat.retreatHealth`
- [x] `ai.combat.minFood`
- [x] `ai.crafting.preferCraftingTable`
- [x] `ai.permissions.restrictByModel`
- [x] `ai.models.mini.skillTier` (default: 900)
- [x] `ai.models.gpt1.skillTier` (default: 1800)
- [x] `ai.models.gpt2.skillTier` (default: 6300)

### 4. Plugin Commands
- [x] `/smithai` base command with tab completer
- [x] `/smithai spawn` — spawn a Smith_AI NPC
- [x] `/smithai despawn` — despawn all Smith_AI NPCs
- [x] `/smithai follow` — nearby Smith_AI follows the player
- [x] `/smithai stay` — nearby Smith_AI stops following
- [x] `/smithai goto <x> <y> <z>` — send nearby Smith_AI to coordinates
- [x] `/smithai do <task>` — plan and execute a task via the skill queue
- [x] `/smithai stop` — cancel all queued tasks
- [x] `/smithai status` — show active brain/model
- [x] `/smithai model` — show model information
- [x] `/smithai reload` — reload config and restart connector
- [x] `/smithai train good|bad` — reward or punish recent action
- [x] `/smithai feedback <message>` — describe exactly what the AI did wrong
- [x] `/smithai feedback-list` — show recent feedback (admin only)
- [x] `/smithai report <description>` — open prefilled GitHub issue or save to disk
- [x] `/smithai reports` — show recent saved issue reports (admin only)
- [x] `/smithai memory` — show last 17 messages
- [x] `/smithai debug` — toggle debug messages for the sender (admin only)
- [x] `/smithai debug global` — toggle debug messages for all online players (admin only)
- [x] `/smithai health` — show subsystem health summary (admin only)
- [x] `/SmithAPI set <key>` — save the external server key
- [x] `/SmithAPI status` — show current API key and connection status (masked)
- [x] Tab completion for both commands
- [x] `/smithai inventory` — inspect NPC inventory
- [x] `/smithai give <item>` — give item to NPC
- [x] `/smithai teleport` — teleport NPC to player
- [x] `/smithai skin <url>` — change NPC skin (placeholder, needs player model)
- [x] `/smithai list` — list all active NPCs
- [x] `/smithai help` — in-game help system
- [ ] `/smithai config` — in-game config viewer
- [ ] `/smithai export` — export memory/training data

### 5. Chat & Memory System
- [x] `ChatListener` detects "Smith_AI" or configured name in chat
- [x] Memory manager holds 17 messages per NPC
- [x] Memory persistence to YAML
- [x] Training feedback detection ("good bot", "bad bot")
- [x] Automatic negative feedback detection in chat ("don't do that", "wrong", etc.)
- [x] Automatic report request detection in chat ("report", "bug", "broken", etc.)
- [ ] Long-term memory summarization
- [ ] Player-specific preference memory
- [ ] Emotion/mood tracking
- [ ] Conversation threading across sessions
- [ ] Memory search/retrieval by topic

### 6. AI Brain Switching
- [x] `AIManager` picks model by tier/config
- [x] `ExternalAIConnector` Java 11 `HttpClient` to SmithAI-Server
- [x] Health check ping
- [x] Automatic fallback to Smith-Mini when external is offline
- [x] Reconnection loop with status messages
- [x] API key reminder every 10-50 seconds when external is enabled but no key is set
- [x] Parse `action`/`target` from external server response
- [ ] Real GGUF inference for Smith-Mini (currently rule-based)
- [ ] Streaming responses from external model
- [ ] Server-side prompt templates per model tier
- [ ] Skill-aware response generation with action parsing (partial — parsing exists, not fully integrated into chat flow)
- [ ] Model performance telemetry

### 7. Local AI / Smith-Mini 1.0
- [x] `LocalMiniAI` rule-based fallback for chat
- [x] Config `ai.local.fallbackToRules`
- [x] Config `ai.local.modelPath`
- [x] Action tag emission for common commands (follow, stay, mine, build, fight, etc.)
- [ ] Real GGUF model loading (llama.cpp or llama-cpp-java)
- [ ] Prompt template for local model
- [ ] Skill-aware local inference
- [ ] Async inference to avoid lag spikes
- [ ] Local model cache and warmup

### 8. Knowledge Base
- [x] `KnowledgeBase` loader with expanded sample entries
- [x] Knowledge entries stored in `knowledge/*.json`
- [x] JSON format: id, category, name, description, tags
- [x] Knowledge lookup by keyword/tag
- [x] Biome category added
- [x] Expand from samples to several hundred core entries (334 entries across blocks, mobs, items, recipes, biomes, strategy)
- [ ] Expand to 29,000 entries
- [ ] Category index for fast lookup
- [ ] Knowledge versioning and updates
- [ ] Server-side knowledge embed endpoint
- [ ] Knowledge context window management

### 9. Skill System (MAJOR UPDATE THIS COMMIT)
- [x] Runtime skill generator (`SkillGenerator.java`) producing 9000 skills
- [x] Tier-aware `SkillRegistry` that loads only skills available to the active model
- [x] `SkillDispatcher` maps broad skill categories to concrete actions
- [x] `SkillExecutor` queue with tick-based step execution
- [x] `TaskPlanner` maps player requests to skill sequences
- [x] `/smithai do <task>` queues the planned skill list
- [x] `/smithai stop` cancels the queue
- [x] `skills.yml` generated on first run if missing
- [x] 900/1800/6300 split verified by generator test
- [x] Real block breaking, placing, and torch placing
- [x] Tool selection by task (pickaxe, axe, sword, shovel)
- [x] Basic combat with nearest hostile or targeted mob type
- [x] Action tag parsing from LLM/chat responses
- [ ] Full primitive executors for all 9000 skills
- [ ] Skill preconditions and success/failure detection
- [ ] Skill parameters from LLM responses fully wired
- [ ] Skill retry and recovery policies
- [ ] Skill usage analytics and training feedback

### 10. NPC System
- [x] `NPCManager` tracks spawned Smith_AI NPCs
- [x] `SmithNPC` wrapper with follow/stay/message/teleport methods
- [x] `NPCSpawner` spawns an entity (currently a villager)
- [x] `Conversation` ties NPC to a player and memory
- [x] Velocity-based movement with step-up/jump logic
- [x] `goto` target support via `setMoveTarget`
- [ ] Real player-model NPC with robot skin and limbs
- [x] Real pathfinding using Bukkit pathfinders or custom A*
- [x] NPC inventory and equipment mirroring
- [ ] NPC animation states (walking, mining, fighting)
- [ ] NPC damage, health, death, and respawn handling
- [ ] NPC nameplate and hologram display
- [ ] Eaglercraft-compatible player model rendering

### 11. Movement & Pathfinding
- [x] `follow` method with velocity-based movement toward player
- [x] `stay` and `teleport` methods
- [x] `lookAt` rotates NPC toward target
- [x] `goto` coordinates command
- [x] Step-up and jump logic for small obstacles
- [x] Real walking pathfinding to player/target block (A* or Bukkit navigator)
- [ ] Navigation around obstacles, water, lava, cliffs
- [ ] Sprint/jump/sneak/climb/swim integration
- [ ] Follow distance and leash behavior
- [ ] Stuck detection and recovery
- [ ] Multi-world teleport handling
- [ ] Path cost estimates (terrain, danger, distance)

### 12. Inventory & Crafting Automation
- [x] Check inventory stub
- [x] Select/equip item by task (tool, weapon)
- [x] Real inventory scanning, item selection, pick up, drop, and use items
- [ ] Crafting recipes by name and available ingredients
- [ ] Crafting table / furnace / brewing stand interaction
- [ ] Smelting, fueling, and result collection
- [ ] Chest storage and retrieval
- [ ] Item pickup and drop
- [ ] Tool/food/weapon/armor selection by task (partial — tools done)
- [ ] Durability-aware tool switching
- [ ] Resource stockpiling and restocking

### 13. Combat & Survival
- [x] Attack nearest hostile mob in `SkillDispatcher`
- [x] Heal via food in `SkillDispatcher`
- [x] Place torch in `SkillDispatcher`
- [x] Mob-specific target selection via `target` parameter
- [ ] Mob-specific combat tactics (creeper, skeleton, zombie, etc.)
- [ ] Equip best armor and weapon
- [ ] Retreat when low health/hunger
- [ ] Dodge, strafe, block, counter
- [ ] Buff potion usage (healing, strength, fire resistance)
- [ ] Food/hunger management
- [ ] Bed/sleep behavior
- [ ] Environmental hazard avoidance (lava, cactus, fall)
- [ ] Boss fight sequences (dragon, wither, warden)

### 14. World Interaction
- [x] Break blocks with best tool (instant via `breakNaturally`)
- [x] Place blocks with material parameter
- [x] Place torches
- [ ] Break blocks with correct timing and drops
- [ ] Place blocks with correct facing and support
- [ ] Interact with doors, levers, buttons, chests, furnaces, etc.
- [ ] Use buckets, flint and steel, ender pearls, etc.
- [ ] Harvest crops, shear sheep, milk cows, tame animals
- [ ] Build structures from schematic or plan
- [ ] Light area with torches
- [ ] Farm automation (plant, grow, harvest)
- [ ] Mine safely (1x2 strip, ladder down, avoid lava)
- [ ] Terraform and landscape

### 15. Endgame & Progression Tasks
- [x] Task planner sequences for "beat the game", diamonds, nether portal, base, etc.
- [ ] Real diamond mining at Y=-59
- [ ] Nether portal creation and travel
- [ ] Blaze rod farming and potion brewing
- [ ] Eye of ender crafting and stronghold location
- [ ] End portal activation and dragon fight
- [ ] Post-dragon elytra/shulker acquisition
- [ ] Wither and Warden encounters
- [ ] Advancement completion tracking
- [ ] Automated speedrun path (optional)

### 16. Training System
- [x] `/smithai train good|bad` command
- [x] `good bot` and `bad bot` chat detection
- [x] Training manager with YAML persistence
- [x] Good/bad feedback scores per action
- [x] Detailed written feedback via `/smithai feedback` and chat detection
- [ ] Demonstration learning: player performs action, AI copies
- [ ] Per-player preference memory
- [ ] Per-NPC learned behavior profiles
- [ ] Export/import training data
- [ ] Training data merge conflicts resolution
- [ ] Visual feedback when training is recorded
- [ ] Reset training for a specific player or NPC
- [ ] Use training scores to influence skill selection
- [ ] Training data privacy toggle

### 17. Commands & Permissions
- [x] `/smithai` base command
- [x] `/smithai spawn`
- [x] `/smithai despawn`
- [x] `/smithai follow`
- [x] `/smithai stay`
- [x] `/smithai goto`
- [x] `/smithai do <task>`
- [x] `/smithai stop`
- [x] `/smithai status`
- [x] `/smithai model`
- [x] `/smithai reload`
- [x] `/smithai train good|bad`
- [x] `/smithai feedback <message>`
- [x] `/smithai feedback-list`
- [x] `/smithai report <description>`
- [x] `/smithai reports`
- [x] `/smithai memory`
- [x] `/smithai debug`
- [x] `/smithai health`
- [x] `/SmithAPI set <key>`
- [x] `/SmithAPI status`
- [x] Tab completers for `/smithai` and `/smithapi`
- [x] Permissions: `smithai.admin`, `smithai.use`, `smithai.spawn`, `smithai.api`
- [ ] `/smithai inventory` — view/inspect NPC inventory
- [ ] `/smithai give <item>` — give item to NPC
- [ ] `/smithai teleport` — teleport NPC to player
- [ ] `/smithai skin <url>` — change NPC skin
- [ ] `/smithai list` — list all active NPCs
- [ ] `/smithai config` — in-game config viewer/editor
- [ ] `/smithai export` — export memory/training data

### 18. In-Game Status & Notifications
- [x] Chat message when switching models
- [x] Status command
- [x] API key reminder every 10-50 seconds until connected
- [x] Loading/offline/error messages in console
- [x] Action bar for active task progress
- [ ] NPC speech bubbles or holograms above head
- [ ] Sound cues for mode switch, task start/finish, errors
- [ ] Toast notifications for achievements/milestones
- [ ] Per-player notification settings
- [ ] Language/locale support

### 19. External AI Server (SmithAI-Server)
- [x] Python FastAPI server
- [x] `/chat` endpoint
- [x] `/health` endpoint with model info and load status
- [x] `/skills` endpoint returning tier-appropriate broad skill list
- [x] API key generation (`SMA-...`)
- [x] API key console spam until plugin connects
- [x] `Connected to ...` message on successful auth
- [x] Bearer token auth middleware
- [x] Loads GGUF model via llama-cpp-python if present
- [x] Configurable host, port, model path, max tokens, context size
- [x] Uses `PORT` environment variable when available (Replit/Codespaces)
- [x] `requirements.txt`
- [x] Server README
- [x] Rule-based fallback when model is not loaded
- [x] Action tag parsing from LLM responses
- [x] Docker support (`Dockerfile` + `docker-compose.yml` + `.dockerignore`)
- [x] Server test script (`test_app.py`)
- [x] `/embed` endpoint for knowledge retrieval (stub, ready for embedding integration)
- [x] `/task` endpoint for task planning
- [x] `/feedback` endpoint to receive training data
- [x] Server startup script for Windows and Linux (`start.sh`, `start.bat`)
- [ ] Auto-download missing model files (with user consent)
- [ ] Model warmup on first request
- [ ] Logging to file with rotation
- [ ] Server-side prompt templates per model tier
- [ ] Rate limiting and concurrent request queue
- [ ] Multi-GPU support detection
- [ ] Health checks include GPU/RAM status
- [ ] Server dashboard / status page

### 20. Models
- [x] Models README with download sources
- [x] GGUF format guidance
- [x] Quantization notes
- [x] Tier guidance (Mini, GPT 1.0, GPT 2.0)
- [x] Specific recommended model downloads for SmithGPT 1.0 (4GB)
- [x] Specific recommended model downloads for SmithGPT 2.0 (7.5GB)
- [x] Specific recommended model downloads for Smith-Mini 1.0
- [x] Model download scripts (`download_model.py`, `.sh`, `.bat`)
- [ ] Hugging Face integration or `huggingface-cli` instructions
- [ ] Model checksums / verification
- [ ] License compliance notes for each recommended model
- [ ] Model cards explaining behavior differences

### 21. Eaglercraft & Minecraft 1.21.x Compatibility
- [x] Bukkit/Spigot/Paper API usage
- [x] `api-version: 1.21` in plugin.yml
- [x] Avoid NMS where possible
- [x] Eaglercraft compatibility documented in FAQ
- [ ] Test on Eaglercraft 1.8.x backend
- [ ] Test on Spigot 1.21.x
- [ ] Test on Paper 1.21.x
- [ ] Handle 1.8 protocol differences
- [ ] Verify chat packets work across versions
- [ ] Verify NPC rendering in Eaglercraft client
- [ ] Graceful degradation on unsupported versions

### 22. Testing & Quality
- [x] Unit tests for skill generator (`SkillGeneratorTest`)
- [x] Unit tests for task planner (`TaskPlannerTest`)
- [x] Unit tests for subsystem health (`SubsystemHealthTest`)
- [x] Unit tests for knowledge lookup (`KnowledgeBaseTest`)
- [x] Unit tests for memory system (`MemoryManagerTest`)
- [x] Unit tests for training manager (`TrainingManagerTest`)
- [x] Unit tests for config parsing (`ConfigTest`)
- [ ] Integration test for external AI connector
- [ ] Manual in-game test checklist
- [ ] Performance profiling under load
- [ ] Thread safety review
- [ ] Error handling review
- [ ] Memory leak review (NPC cleanup, conversation pruning)
- [ ] Security audit (API key handling, command injection)
- [ ] Load test with multiple NPCs and players
- [ ] Test on low-end hardware (2GB server)

### 23. Documentation
- [x] README.md (overview, install, config, commands, skill library summary, new commands)
- [x] HOSTING.md (Replit, Codespaces, Linux, Windows, VPS, Docker, health endpoint)
- [x] FAQ.md (feedback, reporting, skills, models)
- [x] LICENSE
- [x] SKILLS.md (overview of 9000 skills, how to add, implementation status)
- [x] CONTRIBUTING.md (coding style, build instructions, rules)
- [x] REPORT_TEMPLATE.md (bug report template)
- [x] MODELS.md (detailed model cards and downloads)
- [x] API.md (SmithAI-Server API reference)
- [x] CHANGELOG.md
- [x] In-game help system (`/smithai help`)
- [x] TROUBLESHOOTING.md
- [x] PRIVACY.md
- [ ] Video tutorial script (optional)

---

## Skill Library (9000 core skills)

The library is generated at runtime by `com.smithai.skills.SkillGenerator` into `plugins/SmithAI/skills.yml`. The plugin JAR only contains the generator code, so it stays small (~138KB). The full list and category breakdown are in `SKILLS.md`.

### Tier 1: Smith-Mini 1.0 (900 skills)
Generated from 100 base verbs + 30 topics, producing 900 unique broad skills. Categories include:
- Chat: greet, ask, answer, joke, thank, warn, praise, etc.
- Social: wave, bow, dance, emote, celebrate, etc.
- Movement: follow, stay, move, turn, look, jump, sneak, etc.
- Basic interaction: inspect, use, equip, drop, pick up, eat, sleep, etc.
- Memory/Status: remember, recall, report, check health, check inventory, etc.
- Task control: begin task, end task, cancel, retry, mark done, etc.

### Tier 2: SmithGPT 1.0 (1800 skills)
Includes all 900 Mini skills plus 900 generated composite skills from 60 verbs × 90 objects. Categories include:
- Gathering: gather wood, stone, coal, iron, copper, gold, diamond, crops, etc.
- Crafting: craft tools, weapons, armor, food, blocks, potions, etc.
- Building: build house, shelter, wall, bridge, farm, room, etc.
- Combat: fight hostile mobs, defend, ambush, retreat, block, dodge, etc.
- Farming: plant, water, harvest, breed, feed, tame, etc.
- Exploration: explore caves, villages, biomes, locate structures, etc.
- Utility: store, sort, trade, manage, smelt, brew, etc.

### Tier 3: SmithGPT 2.0 (6300 skills)
Includes all 1800 lower-tier skills plus 6300 generated advanced composite skills from 100 verbs × 130 objects. Categories include:
- Endgame progression: conquer nether/end/stronghold/dragon, master systems, etc.
- Automation: build autonomous mines, mob grinders, farms, storage networks, etc.
- Strategy: raid, siege, defend, fortify, patrol, escort, strategize, etc.
- Magic/Tech: enchant, summon, channel, ward, build reactors, laboratories, etc.
- Economy/Social: trade, barter, invest, build guilds, kingdoms, alliances, etc.
- Terraforming: terraform, irrigate, reforest, colonize, build infrastructure, etc.
- Advanced building: portals, networks, roads, railways, wonders, monuments, etc.
- Roleplay: lead, recruit, train, inspire, command, govern, etc.

---

## Phase Plan (Updated)

### Phase 1: Foundation — DONE
- [x] Maven project setup
- [x] Plugin main class
- [x] Config system
- [x] Command skeleton + tab completers
- [x] Basic NPC spawn/despawn
- [x] Robot skin placeholder
- [x] Lifecycle, reload, shutdown

### Phase 2: AI Brains — PARTIAL
- [x] External AI connector + health check
- [x] Smith-Mini rule-based fallback with action tags
- [x] Auto-failover and reconnection
- [x] Status notifications
- [x] API key auth flow
- [x] SmithAI-Server with console spam + connected message
- [x] Server-side broad skill list by tier
- [x] Server action tag parsing from LLM responses
- [ ] Real Smith-Mini GGUF inference
- [ ] Server-side prompt templates per model
- [ ] Streaming responses

### Phase 3: Chat & Memory — PARTIAL
- [x] Chat listener + AI name detection
- [x] 17-message memory
- [x] Memory persistence
- [x] Basic responses
- [x] Training feedback detection
- [x] Negative feedback and report detection in chat
- [ ] Long-term memory summarization
- [ ] Player-specific preferences
- [ ] Emotion/mood tracking

### Phase 4: Knowledge & Skills — IN PROGRESS
- [x] Knowledge base loader + expanded sample entries
- [x] Skill registry skeleton
- [x] Task planner expanded with common goals
- [x] 9000 Smith-Mini + SmithGPT skills generated at runtime
- [x] Tier-aware skill registry
- [x] Skill dispatcher and executor with real block/place/torch/tool/combat actions
- [x] `/smithai do` queues skill plans
- [x] `/smithai stop` cancels queue
- [x] `/smithai goto` coordinates command
- [ ] Primitive skill executors for most common skills (partial)
- [ ] Composite skill execution with real world effects (partial)
- [ ] Skill preconditions and failure handling
- [ ] Skill parameters from LLM fully wired

### Phase 5: Advanced Gameplay — IN PROGRESS
- [x] Basic velocity-based movement and step-up/jump
- [x] Basic block breaking/placing/torch placing
- [x] Tool selection by task
- [x] Basic mob combat with target selection
- [x] Pathfinding (A* or navigator)
- [x] Inventory automation (scan, pick up, drop, use)
- [ ] Crafting automation
- [ ] Combat/survival automation
- [ ] Endgame task sequences
- [ ] Base building and farming

### Phase 6: Scale & Polish — NOT STARTED
- [ ] Expand knowledge to 29,000 entries
- [ ] Complete training system
- [ ] Docker/server packaging (partial — Docker files exist)
- [ ] Documentation finalization
- [ ] Eaglercraft testing
- [ ] Unit tests and QA (partial)

---

## Current Status Summary

| System | Status | Notes |
|--------|--------|-------|
| Build & Packaging | 95% | Maven, build script, CI, release packaging, checksums, version bump all done |
| Plugin Lifecycle | 95% | Enable/disable/reload, subsystem health, debug mode done; bStats config only |
| Config System | 85% | Core keys done; some advanced pathfinding/combat/crafting keys missing |
| NPC System | 45% | Spawn/follow/stay/goto done; real player model and pathfinding pending |
| External AI Connector | 85% | Chat/health/failover/action parsing done; streaming/templates pending |
| Local AI (Smith-Mini) | 40% | Rule-based fallback + action tags; real GGUF inference pending |
| Chat & Memory | 75% | 17-message memory, persistence, feedback/report detection done |
| Knowledge Base | 10% | Expanded samples; 29,000 target still pending |
| Skill System | 50% | 9000 skills generated, dispatcher/executor with real basic actions; most skills still stubs |
| Pathfinding & Movement | 60% | Basic A* pathfinding implemented; obstacle handling and navigation smoothing remain |
| Inventory & Crafting | 35% | Inventory scan, pick up, drop, and item use done; crafting automation pending |
| Combat & Survival | 15% | Basic attack/eat/torch done; tactics/hazards pending |
| Training System | 60% | Commands, persistence, detailed feedback done; demo learning pending |
| Commands & Permissions | 90% | Core commands + feedback/report/debug/health done; some advanced commands missing |
| Status & Notifications | 65% | Switch messages, reminders, debug/health done; action bar pending |
| External AI Server | 85% | Chat/health/key auth/skills/Docker/tests done; /embed/task/feedback pending |
| Models | 30% | README done; no specific download scripts |
| Eaglercraft Compatibility | 10% | API usage correct; no live testing |
| Testing & Quality | 20% | Generator, planner, health tests done; many areas still untested |
| Documentation | 85% | README, HOSTING, FAQ, SKILLS, CONTRIBUTING, REPORT_TEMPLATE done; MODELS/API/CHANGELOG pending |

---

## Next Actions (High Priority)

1. Implement real pathfinding (A* or Bukkit navigator) so NPCs can walk to players/blocks without teleporting.
2. Expand inventory automation beyond tool selection: scan items, pick up, drop, craft simple recipes.
3. Add real block-breaking timing and drop collection instead of instant `breakNaturally`.
4. Add mob-specific combat tactics and environmental hazard avoidance.
5. Implement the endgame sequence with real world effects: nether portal, blaze rods, eyes of ender, end portal, dragon fight.
6. Expand the knowledge base from sample entries to a few hundred core blocks/mobs/items/recipes/strategies/biomes.
7. Add `/smithai inventory`, `/smithai give`, and `/smithai list` commands.
8. Add `/smithai help` in-game help system.
9. Add more unit tests: knowledge lookup, memory system, training manager, config parsing.
10. Continue live testing on Spigot/Paper/Eaglercraft when possible.

---

## Commit Log

### Commit — July 23, 2026

User triggered a commit with the message `COMMIT!`. This section records every detail of the work done since the previous commit snapshot.

#### New and Changed Files

- `todo.md` — updated DONE vs TODO matrix, current snapshot, status summary, and next actions.
- `README.md` — documented new commands (`goto`, `feedback`, `report`, `reports`, `debug`, `health`), Docker support, build/test/release scripts.
- `SKILLS.md` — updated implementation status and added `goto` usage example.
- `FAQ.md` — added feedback and reporting sections.
- `HOSTING.md` — added Docker and health endpoint notes.
- `CONTRIBUTING.md` — new file with build instructions, project rules, and style guide.
- `REPORT_TEMPLATE.md` — new bug report template.
- `build.sh` — updated to run Maven tests during build.
- `package-release.sh` — new release bundling script with SHA-256 checksums.
- `bump-version.sh` — new version-bumping script.
- `Makefile` — new convenience targets for build/test/release/docker.
- `.gitattributes` — new file for consistent line endings.
- `.github/workflows/build.yml` — new CI workflow that builds the plugin and checks the Python server.
- `docker-compose.yml` — new Docker Compose setup for SmithAI-Server.
- `SmithAI-Server/Dockerfile` — new Dockerfile for the Python server.
- `SmithAI-Server/.dockerignore` — new ignore file for Docker builds.
- `SmithAI-Server/requirements.txt` — marked `llama-cpp-python` as optional and added explanatory comment.
- `SmithAI-Server/app.py` — added rule-based fallback, action tag parsing, skill prompting, health response model info, better error handling.
- `SmithAI-Server/test_app.py` — new Python sanity test for action tag parsing.
- `SmithAI-Server/README.md` — updated with endpoint and config details.
- `SmithAI/pom.xml` — added JUnit 5 and Surefire for testing.
- `SmithAI/src/main/java/com/smithai/SmithAIPlugin.java` — added `DebugManager`, `IssueReportManager`, `SubsystemHealth`, subsystem init with error recovery.
- `SmithAI/src/main/java/com/smithai/config/Config.java` — added `debug.enabled` and `metrics.bstats` keys.
- `SmithAI/src/main/java/com/smithai/commands/SmithAICommand.java` — added `goto`, `debug`, `health`, `feedback`, `feedback-list`, `report`, `reports` subcommands; fixed report URL truncation.
- `SmithAI/src/main/java/com/smithai/commands/SmithAITabCompleter.java` — added tab completions for new commands.
- `SmithAI/src/main/java/com/smithai/commands/SmithAPICommand.java` — added `status` subcommand with connection details.
- `SmithAI/src/main/java/com/smithai/ai/ExternalAIConnector.java` — parses `action`/`target` from server response and appends action tag to reply.
- `SmithAI/src/main/java/com/smithai/ai/LocalMiniAI.java` — expanded rule-based responses and added action tag emission.
- `SmithAI/src/main/java/com/smithai/chat/ChatManager.java` — added automatic negative feedback and report detection.
- `SmithAI/src/main/java/com/smithai/knowledge/KnowledgeBase.java` — loads biomes.json and expanded knowledge entries.
- `SmithAI/src/main/java/com/smithai/npc/SmithNPC.java` — replaced teleport stubs with velocity-based movement, step-up/jump logic, and pathfinding target support.
- `SmithAI/src/main/java/com/smithai/skills/SkillDispatcher.java` — added real executors for block break/place, torch placing, tool selection, targeted combat, and movement.
- `SmithAI/src/main/java/com/smithai/skills/SkillExecutor.java` — added `cancel()` to tasks, adjusted durations, action tag parsing.
- `SmithAI/src/main/java/com/smithai/skills/TaskPlanner.java` — greatly expanded common goal mappings (biomes, farming, combat, exploration, etc.).
- `SmithAI/src/main/java/com/smithai/training/FeedbackManager.java` — new class storing detailed written feedback with context.
- `SmithAI/src/main/java/com/smithai/training/IssueReportManager.java` — new class persisting bug reports to YAML.
- `SmithAI/src/main/java/com/smithai/debug/DebugManager.java` — new debug toggle and broadcast helper.
- `SmithAI/src/main/java/com/smithai/health/SubsystemHealth.java` — new subsystem health tracker.
- `SmithAI/src/main/resources/config.yml` — added `debug.enabled` and `metrics.bstats` defaults.
- `SmithAI/src/main/resources/plugin.yml` — updated usage string and aliases.
- `SmithAI/src/main/resources/knowledge/` — expanded `blocks.json`, `items.json`, `mobs.json`, `recipes.json`, `strategy.json`, and added `biomes.json` and `README.md`.
- `SmithAI/src/test/java/com/smithai/skills/SkillGeneratorTest.java` — new test verifying skill counts.
- `SmithAI/src/test/java/com/smithai/skills/TaskPlannerTest.java` — new test verifying task plans.
- `SmithAI/src/test/java/com/smithai/health/SubsystemHealthTest.java` — new test verifying health states.
- `models/README.md` — updated with Docker and local/server model notes.

#### Build & Verification

- `mvn -f SmithAI/pom.xml clean test package` succeeds and produces `SmithAI/target/SmithAI-2.0.0.jar` (~138KB).
- JUnit 5 tests pass: `SkillGeneratorTest` (2 tests), `TaskPlannerTest` (6 tests), `SubsystemHealthTest` (3 tests).
- `python3 -m py_compile SmithAI-Server/app.py` passes.
- `python3 SmithAI-Server/test_app.py` passes.
- `package-release.sh` can be run to bundle a full release with checksums.

#### Skill Counts

- Smith-Mini 1.0: 900 core skills
- SmithGPT 1.0: 1800 core skills (900 + 900)
- SmithGPT 2.0: 6300 core skills (900 + 900 + 4500)
- Total: 9000 core skills

#### Design Decisions This Commit

1. Skills are generated at runtime rather than shipped as a 9000-entry YAML file. This keeps the plugin JAR small (~138KB) and makes the library easy to extend by changing the generator templates.
2. Skills are broad (e.g., `conquer_dragon`, `build_base`, `gather_diamonds`) rather than item-specific. This lets the LLM reason about goals and lets the dispatcher decide the concrete steps.
3. The `SkillDispatcher` categorizes skills by prefix and implements real basic actions for the most common categories (movement, block break/place, torch placing, tool selection, combat). It is still a starter implementation for the full 9000-skill surface.
4. Player feedback is stored as free-text plus context (active task or chat), so future training can learn from specific mistakes rather than just thumbs-up/down scores.
5. Issue reports are saved to `plugins/SmithAI/issue_reports.yml` as a fallback when the GitHub issue URL is too long for chat, so bug reports are never lost.
6. Subsystem health is tracked so non-critical failures can degrade gracefully instead of crashing the entire plugin on startup.
7. The SmithAI-Server falls back to rule-based responses when the GGUF model is not present, so the plugin/server integration can be tested and used without downloading multi-gigabyte models.

#### Known Limitations / Outstanding Work

- Most skill executors are still stubs or chat placeholders. Real world interaction (timed mining, crafting, pathfinding, farming) is only partially implemented.
- The NPC is still a generic villager; a real player model with robot skin and limbs is pending.
- Smith-Mini 1.0 is still rule-based; real GGUF inference is pending.
- Pathfinding is velocity-based follow/goto; true A* navigation around obstacles is pending.
- The knowledge base has expanded samples but the 29,000-entry target is pending.
- Live testing on Spigot/Paper/Eaglercraft is pending.
- Some external server endpoints (`/embed`, `/task`, `/feedback`) are not yet implemented.

#### Git & Commit Rules

- GitHub commits must be made ONLY from the user's account.
- The user is the sole contributor and the only committer.
- Do not commit under the agent's identity.
- Use the `GITHUB_PERSONAL_ACCESS_TOKEN` secret only when the user explicitly authorizes a push, and only push on their behalf with their configured git identity.
- Never commit to a remote repository without the user's explicit `commit` or `push` command.
- The current commit is a workspace snapshot; the user should push it when ready.

#### Notes for Next Build

- Focus on real pathfinding and navigation so the skill queue can execute physical tasks reliably.
- After movement, expand inventory/crafting automation and timed block breaking with drop collection.
- Continue adding unit tests for knowledge, memory, training, and config.
- Update this TODO again when the user says `commit` or `STOP WORK`.

---
