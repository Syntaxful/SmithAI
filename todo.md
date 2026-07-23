# SmithAI - MinecraftLLM Master TODO

This file is the single source of truth for what is finished and what remains. It is updated every time the user says `commit` or `STOP WORK`. Do not edit it casually.

## Current Snapshot (as of this commit)

- Plugin builds successfully with Maven: `SmithAI/target/SmithAI-2.0.0.jar` (115KB shaded JAR)
- SmithAI-Server Python FastAPI server exists and can run with `python app.py`
- API key generation, console spam, and "Connected to ..." messages are implemented
- External AI failover to Smith-Mini 1.0 is implemented
- Built-in knowledge base loader exists with 25 sample entries across blocks, mobs, items, recipes, strategy
- Chat, memory, commands, tab completers exist
- **9000-core-skill library implemented** with runtime generation to keep the JAR small
  - 900 skills for Smith-Mini 1.0
  - 1800 skills for SmithGPT 1.0 (includes all Mini skills)
  - 6300 skills for SmithGPT 2.0 (includes all Mini + GPT-1 skills)
- Skill tier-aware registry, skill dispatcher, and skill executor queue implemented
- Task planner updated with broad, non-specific goals: beat the game, get diamonds, build nether portal, build base, defend, farm, etc.
- `/smithai do <task>` now queues skills into the executor
- `/smithai stop` added to cancel the active skill queue
- Training manager persists good/bad feedback to disk
- NPC follow/stay/movement exists as teleport-based stubs
- README and SKILLS.md document the new 9000-skill system
- GitHub commit rule: user is the only committer and only contributor; `GITHUB_PERSONAL_ACCESS_TOKEN` is not used for commits without explicit authorization

---

## Project Goal (unchanged)

Build a single, official Minecraft/Eaglercraft plugin (`SmithAI`) that adds AI-controlled NPCs (`Smith_AI`) with player-like models, natural chat, long-term memory, and a task engine. The plugin includes a built-in small model (`Smith-Mini 1.0`) and can connect to an optional external model (`SmithGPT 1.0` 7.5GB or `SmithGPT 2.0` 15GB) hosted on Replit, Codespaces, Linux, Windows, VPS, or any machine the user chooses. The user picks the model by running the matching `SmithAI-Server` and pointing the plugin at its URL/IP/port. No signup, free forever.

---

## Skill Tiers (UPDATED)

Each brain has a fixed set of core skills it can execute. Higher brains can use all lower-brain skills too.

- **Smith-Mini 1.0** — 900 core skills (built-in, runs in plugin)
- **SmithGPT 1.0** — 1800 core skills (external, 7.5GB model)
- **SmithGPT 2.0** — 6300 core skills (external, 15GB model)
- **Total core skills** — 9000 (overlapping tiers)

Skills are generated at runtime by `SkillGenerator` into `plugins/SmithAI/skills.yml` so the plugin JAR stays small (~115KB). Higher-tier models include all lower-tier skills.

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
- [x] Maven project structure under `SmithAI-MinecraftLLM/SmithAI/`
- [x] `pom.xml` with Bukkit/Paper API dependency
- [x] `plugin.yml` with `api-version: 1.21`, commands, permissions
- [x] `config.yml` default configuration
- [x] Maven shade plugin producing shaded JAR
- [x] Build produces `SmithAI/target/SmithAI-2.0.0.jar` (115KB)
- [x] Runtime skill generation keeps JAR small
- [ ] Automated build script for non-Maven users
- [ ] CI workflow (GitHub Actions) to build on push
- [ ] Release packaging script that bundles plugin JAR, server, and docs
- [ ] Signed releases or checksums
- [ ] Version bumping automation

### 2. Plugin Lifecycle
- [x] `SmithAIPlugin` main class with `onEnable` and `onDisable`
- [x] Config loading and validation
- [x] Command and listener registration
- [x] Graceful shutdown: despawn NPCs, save memory, cancel tasks
- [x] Reload command that re-inits config, AI manager, reminder task
- [x] Skill executor stops on shutdown
- [ ] Proper error recovery when a subsystem fails to initialize
- [ ] Plugin metrics / bStats integration (optional, off by default)
- [ ] In-game debug mode toggle

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
- [ ] `ai.pathfinding.maxDistance`
- [ ] `ai.pathfinding.maxNodes`
- [ ] `ai.pathfinding.tickRate`
- [ ] `ai.combat.retreatHealth`
- [ ] `ai.combat.minFood`
- [ ] `ai.crafting.preferCraftingTable`
- [ ] `ai.permissions.restrictByModel`
- [ ] `ai.models.mini.skillTier` (default: 900)
- [ ] `ai.models.gpt1.skillTier` (default: 1800)
- [ ] `ai.models.gpt2.skillTier` (default: 6300)

### 4. Plugin Commands
- [x] `/smithai` base command with tab completer
- [x] `/smithai spawn` — spawn a Smith_AI NPC
- [x] `/smithai despawn` — despawn all Smith_AI NPCs
- [x] `/smithai follow` — nearby Smith_AI follows the player
- [x] `/smithai stay` — nearby Smith_AI stops following
- [x] `/smithai do <task>` — plan and execute a task via the skill queue
- [x] `/smithai stop` — cancel all queued tasks
- [x] `/smithai status` — show active brain/model
- [x] `/smithai model` — show model information
- [x] `/smithai reload` — reload config and restart connector
- [x] `/smithai train good|bad` — reward or punish recent action
- [x] `/smithai memory` — show last 17 messages
- [x] `/SmithAPI` — show current API key status (masked)
- [x] `/SmithAPI set <key>` — save the external server key
- [x] `/SmithAPI clear` — remove saved key
- [x] Tab completion for both commands
- [ ] `/smithai inventory` — inspect NPC inventory
- [ ] `/smithai give <item>` — give item to NPC
- [ ] `/smithai teleport` — teleport NPC to player
- [ ] `/smithai skin <url>` — change NPC skin
- [ ] `/smithai list` — list all active NPCs
- [ ] `/smithai config` — in-game config viewer
- [ ] `/smithai export` — export memory/training data

### 5. Chat & Memory System
- [x] `ChatListener` detects "Smith_AI" or configured name in chat
- [x] Memory manager holds 17 messages per NPC
- [x] Memory persistence to YAML
- [x] Training feedback detection ("good bot", "bad bot")
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
- [ ] Real GGUF inference for Smith-Mini (currently rule-based)
- [ ] Streaming responses from external model
- [ ] Server-side prompt templates per model tier
- [ ] Skill-aware response generation with action parsing
- [ ] Model performance telemetry

### 7. Local AI / Smith-Mini 1.0
- [x] `LocalMiniAI` rule-based fallback for chat
- [x] Config `ai.local.fallbackToRules`
- [x] Config `ai.local.modelPath`
- [ ] Real GGUF model loading (llama.cpp or llama-cpp-java)
- [ ] Prompt template for local model
- [ ] Skill-aware local inference
- [ ] Async inference to avoid lag spikes
- [ ] Local model cache and warmup

### 8. Knowledge Base
- [x] `KnowledgeBase` loader with 25 sample entries
- [x] Knowledge entries stored in `knowledge/*.json`
- [x] JSON format: category, topic, content, tags
- [x] Knowledge lookup by keyword/tag
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
- [ ] Full primitive executors for all 9000 skills
- [ ] Skill preconditions and success/failure detection
- [ ] Skill parameters from LLM responses
- [ ] Skill retry and recovery policies
- [ ] Skill usage analytics and training feedback

### 10. NPC System
- [x] `NPCManager` tracks spawned Smith_AI NPCs
- [x] `SmithNPC` wrapper with follow/stay/message/teleport methods
- [x] `NPCSpawner` spawns an entity (currently a villager)
- [x] `Conversation` ties NPC to a player and memory
- [ ] Real player-model NPC with robot skin and limbs
- [ ] Real pathfinding using Bukkit pathfinders or custom A*
- [ ] NPC inventory and equipment mirroring
- [ ] NPC animation states (walking, mining, fighting)
- [ ] NPC damage, health, death, and respawn handling
- [ ] NPC nameplate and hologram display
- [ ] Eaglercraft-compatible player model rendering

### 11. Movement & Pathfinding
- [x] `follow` method teleports NPC to player
- [x] `stay` and `teleport` methods
- [x] `lookAt` rotates NPC toward target
- [ ] Real walking pathfinding to player/target block
- [ ] Navigation around obstacles, water, lava, cliffs
- [ ] Sprint/jump/sneak/climb/swim integration
- [ ] Follow distance and leash behavior
- [ ] Stuck detection and recovery
- [ ] Multi-world teleport handling
- [ ] Path cost estimates (terrain, danger, distance)

### 12. Inventory & Crafting Automation
- [x] Check inventory stub
- [x] Select/equip item stubs
- [ ] Real inventory scanning and item selection
- [ ] Crafting recipes by name and available ingredients
- [ ] Crafting table / furnace / brewing stand interaction
- [ ] Smelting, fueling, and result collection
- [ ] Chest storage and retrieval
- [ ] Item pickup and drop
- [ ] Tool/food/weapon/armor selection by task
- [ ] Durability-aware tool switching
- [ ] Resource stockpiling and restocking

### 13. Combat & Survival
- [x] Attack nearest hostile mob in `SkillDispatcher`
- [x] Heal via food in `SkillDispatcher`
- [x] Place torch in `SkillDispatcher`
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
- [ ] Break blocks with correct tool and timing
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
- [x] `/smithai do <task>`
- [x] `/smithai stop` (NEW this commit)
- [x] `/smithai status`
- [x] `/smithai model`
- [x] `/smithai reload`
- [x] `/smithai train good|bad`
- [x] `/smithai memory`
- [x] `/SmithAPI set <key>`
- [x] `/SmithAPI` (status check)
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
- [ ] Action bar or boss bar for active task progress
- [ ] NPC speech bubbles or holograms above head
- [ ] Sound cues for mode switch, task start/finish, errors
- [ ] Toast notifications for achievements/milestones
- [ ] Per-player notification settings
- [ ] Language/locale support

### 19. External AI Server (SmithAI-Server)
- [x] Python FastAPI server
- [x] `/chat` endpoint
- [x] `/health` endpoint
- [x] API key generation (`SMA-...`)
- [x] API key console spam until plugin connects
- [x] `Connected to ...` message on successful auth
- [x] Bearer token auth middleware
- [x] Loads GGUF model via llama-cpp-python if present
- [x] Configurable host, port, model path, max tokens, context size
- [x] Uses `PORT` environment variable when available (Replit/Codespaces)
- [x] `requirements.txt`
- [x] Server README
- [x] `/skills` endpoint returning tier-appropriate broad skill list
- [ ] `/embed` endpoint for knowledge retrieval
- [ ] `/task` endpoint for task planning
- [ ] `/feedback` endpoint to receive training data
- [ ] Docker support (`Dockerfile` + `docker-compose.yml`)
- [ ] Server startup script for Windows and Linux
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
- [ ] Specific recommended model downloads for SmithGPT 1.0 (7.5GB)
- [ ] Specific recommended model downloads for SmithGPT 2.0 (15GB)
- [ ] Specific recommended model downloads for Smith-Mini 1.0
- [ ] Model download scripts
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
- [ ] Unit tests for skill planner
- [ ] Unit tests for knowledge lookup
- [ ] Unit tests for memory system
- [ ] Unit tests for training manager
- [ ] Unit tests for config parsing
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
- [x] README.md (overview, install, config, commands, skill library summary)
- [x] HOSTING.md (Replit, Codespaces, Linux, Windows, VPS)
- [x] FAQ.md
- [x] LICENSE
- [x] SKILLS.md (overview of 9000 skills, how to add)
- [ ] MODELS.md (detailed model cards and downloads)
- [ ] API.md (SmithAI-Server API reference)
- [ ] CONTRIBUTING.md (coding style, how to add skills/knowledge)
- [ ] CHANGELOG.md
- [ ] In-game help system (`/smithai help`)
- [ ] Video tutorial script (optional)
- [ ] Troubleshooting guide
- [ ] Privacy policy (no data collection, free forever)

---

## Skill Library (9000 core skills)

The library is generated at runtime by `com.smithai.skills.SkillGenerator` into `plugins/SmithAI/skills.yml`. The plugin JAR only contains the generator code, so it stays small (~115KB). The full list and category breakdown are in `SKILLS.md`.

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
- [x] Smith-Mini rule-based fallback
- [x] Auto-failover and reconnection
- [x] Status notifications
- [x] API key auth flow
- [x] SmithAI-Server with console spam + connected message
- [x] Server-side broad skill list by tier
- [ ] Real Smith-Mini GGUF inference
- [ ] Server-side prompt templates per model
- [ ] Skill-aware response generation
- [ ] Streaming responses

### Phase 3: Chat & Memory — PARTIAL
- [x] Chat listener + AI name detection
- [x] 17-message memory
- [x] Memory persistence
- [x] Basic responses
- [x] Training feedback detection
- [ ] Long-term memory summarization
- [ ] Player-specific preferences
- [ ] Emotion/mood tracking

### Phase 4: Knowledge & Skills — IN PROGRESS
- [x] Knowledge base loader + 25 sample entries
- [x] Skill registry skeleton
- [x] Task planner
- [x] 9000 Smith-Mini + SmithGPT skills generated at runtime
- [x] Tier-aware skill registry
- [x] Skill dispatcher and executor
- [x] `/smithai do` queues skill plans
- [x] `/smithai stop` cancels queue
- [ ] Primitive skill executors for most common skills
- [ ] Composite skill execution with real world effects
- [ ] Skill preconditions and failure handling
- [ ] Skill parameters from LLM

### Phase 5: Advanced Gameplay — NOT STARTED
- [ ] Pathfinding
- [ ] Inventory/crafting automation
- [ ] Combat/survival automation
- [ ] Endgame task sequences
- [ ] Base building and farming

### Phase 6: Scale & Polish — NOT STARTED
- [ ] Expand knowledge to 29,000 entries
- [ ] Complete training system
- [ ] Docker/server packaging
- [ ] Documentation finalization
- [ ] Eaglercraft testing
- [ ] Unit tests and QA

---

## Current Status Summary

| System | Status | Notes |
|--------|--------|-------|
| Build & Packaging | 80% | Maven works, JAR produced, runtime skill generation keeps size small, no CI yet |
| Plugin Lifecycle | 90% | Enable/disable/reload done, skill executor stops cleanly, metrics not added |
| Config System | 80% | Core keys done, missing advanced model/skill configs |
| NPC System | 40% | Spawn/follow/stay done, no real player model or pathfinding |
| External AI Connector | 75% | Chat/health/failover done, missing skill response parsing |
| Local AI (Smith-Mini) | 35% | Rule-based fallback done, no real GGUF inference |
| Chat & Memory | 70% | 17-message memory, persistence, basic chat done |
| Knowledge Base | 5% | 25 sample entries, 29,000 target |
| Skill System | 40% | 9000 skills generated, tier registry, dispatcher, executor, task planner; executors are mostly stubs |
| Pathfinding & Movement | 10% | Teleport follow stub only |
| Inventory & Crafting | 5% | Inspect own inventory stub only |
| Combat & Survival | 10% | Basic attack and heal, no tactics |
| Training System | 50% | Good/bad commands and persistence done, no demo learning |
| Commands & Permissions | 85% | Core commands + stop done, some advanced commands missing |
| Status & Notifications | 60% | Switch messages and reminders done, no action bar |
| External AI Server | 75% | Chat/health/key auth/skills done, missing /embed /task /feedback |
| Models | 30% | README done, no specific download scripts |
| Eaglercraft Compatibility | 10% | API usage correct, no live testing |
| Testing & Quality | 5% | No unit tests yet |
| Documentation | 75% | README, HOSTING, FAQ, LICENSE, SKILLS done; missing MODELS, API, CONTRIBUTING |

---

## Next Actions (High Priority)

1. Implement real primitive skill executors for the most common 100 skills (chat, follow, stay, place_torch, attack, eat, jump, look, inspect, etc.).
2. Add basic pathfinding movement so the NPC can walk to a player or block instead of teleporting.
3. Add real block-breaking and block-placing for tasks like "build shelter" or "get diamonds".
4. Add inventory scanning and tool selection for mining/crafting/combat.
5. Implement endgame task sequence: nether portal, blaze rods, eyes of ender, end portal, dragon fight.
6. Add action bar or boss bar progress reporting for the active task.
7. Expand the knowledge base from 25 samples to a few hundred core entries.
8. Implement `/smithai inventory`, `/smithai give`, and `/smithai list` commands.
9. Add unit tests for the skill planner and task planner.
10. Build and test after each batch.

---

## Commit Log

### Commit — July 23, 2026

User triggered a commit with the message `COMMIT!`. This section records every detail of the work done since the previous commit snapshot.

#### New and Changed Files

- `todo.md` — this file, updated with 9000-skill changes, status, and next actions.
- `README.md` — added skill library summary and `/smithai stop` command documentation.
- `SKILLS.md` — new file documenting the 9000-skill tier system and how to add custom skills.
- `SmithAI/src/main/java/com/smithai/skills/SkillGenerator.java` — rewritten to generate 9000 broad skills at runtime using plain YAML output; does not depend on Bukkit so it can be tested standalone.
- `SmithAI/src/main/java/com/smithai/skills/SkillRegistry.java` — already loaded generated skills with tier support; unchanged this commit but now used by the 9000 generator.
- `SmithAI/src/main/java/com/smithai/skills/SkillDispatcher.java` — new class that maps broad skill categories to concrete Bukkit actions.
- `SmithAI/src/main/java/com/smithai/skills/SkillExecutor.java` — updated to own a `SkillDispatcher`, queue tasks, and execute them on a scheduler tick.
- `SmithAI/src/main/java/com/smithai/skills/TaskPlanner.java` — rewritten with broad, non-specific goal mappings for "beat the game", "get diamonds", "build nether portal", "build base", "defend", "farm", "fight", etc.
- `SmithAI/src/main/java/com/smithai/commands/SmithAICommand.java` — updated `/smithai do` to queue the planned skill list; added `/smithai stop` subcommand.
- `SmithAI/src/main/java/com/smithai/SmithAIPlugin.java` — added `SkillExecutor` field, initialization, shutdown, and getter.
- `SmithAI-Server/app.py` — `available_skills()` updated to return realistic broad skill lists by tier instead of repeated placeholders.

#### Build & Verification

- `mvn clean package` succeeds and produces `SmithAI-2.0.0.jar` (~115KB).
- Standalone `SkillGenerator.main()` run verified: `java -cp SmithAI-2.0.0.jar com.smithai.skills.SkillGenerator /tmp/skills-test.yml` produced a 45,004-line YAML file with exactly 900 `mini`, 1800 `gpt1`, and 6300 `gpt2` skills.
- `python3 -m py_compile SmithAI-Server/app.py` passed (syntax check).

#### Skill Counts

- Smith-Mini 1.0: 900 core skills
- SmithGPT 1.0: 1800 core skills (900 + 900)
- SmithGPT 2.0: 6300 core skills (900 + 900 + 4500)
- Total: 9000 core skills

#### Design Decisions This Commit

1. Skills are generated at runtime rather than shipped as a 9000-entry YAML file. This keeps the plugin JAR small (~115KB) and makes the library easy to extend by changing the generator templates.
2. Skills are broad (e.g., `conquer_dragon`, `build_base`, `gather_diamonds`) rather than item-specific. This lets the LLM reason about goals and lets the dispatcher decide the concrete steps.
3. The `SkillDispatcher` categorizes skills by prefix rather than trying to implement 9000 individual methods. This is a practical starter approach while real executors are built out.
4. The `/smithai do` command now pushes the planned skill sequence into the executor queue, so the NPC will attempt to execute each step in order.
5. `/smithai stop` clears the queue and stops the current task, giving players a way to cancel misinterpreted requests.

#### Known Limitations / Outstanding Work

- Most skill executors are still stubs or chat placeholders. Real world interaction (mining, placing, crafting, pathfinding) is not yet implemented.
- The NPC is still a generic villager; a real player model with robot skin and limbs is pending.
- Smith-Mini 1.0 is still rule-based; real GGUF inference is pending.
- The external server does not yet parse skill responses from the LLM or return structured actions.
- Pathfinding is teleport-based only.
- The knowledge base has 25 samples; the 29,000-entry target is pending.
- No unit tests or automated in-game tests yet.
- No CI/release scripts yet.
- Eaglercraft and Paper/Spigot 1.21.x live testing is pending.

#### Git & Commit Rules

- GitHub commits must be made ONLY from the user's account.
- The user is the sole contributor and the only committer.
- Do not commit under the agent's identity.
- Use the `GITHUB_PERSONAL_ACCESS_TOKEN` secret only when the user explicitly authorizes a push, and only push on their behalf with their configured git identity.
- Never commit to a remote repository without the user's explicit `commit` or `push` command.
- The current commit is a workspace snapshot; the user should push it when ready.

#### Notes for Next Build

- Focus on the top 10 next actions listed above.
- The most impactful next step is real pathfinding and movement, because without it the skill queue cannot execute physical tasks like "get diamonds" or "build a base".
- After movement, implement basic block breaking/placing and inventory/tool selection.
- Keep testing the build after each batch of changes.
- Update this TODO again when the user says `commit` or `STOP WORK`.

---

