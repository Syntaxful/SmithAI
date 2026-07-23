# SmithAI Changelog

## 2.1.0-dev (in progress)

### Added
- In-game help system: `/smithai help` lists all commands and usage.
- New commands: `/smithai inventory`, `/smithai give`, `/smithai list`, `/smithai teleport`, `/smithai skin` (placeholder), `/smithai help`.
- Expanded knowledge base from sample entries to ~334 core entries across blocks, mobs, items, recipes, biomes, and strategy.
- Added `scripts/generate_knowledge.js` and `scripts/generate_knowledge.py` to regenerate the knowledge JSON files.
- Added missing configuration keys: `ai.pathfinding.*`, `ai.combat.*`, `ai.crafting.*`, `ai.permissions.*`, `ai.models.*.skillTier`.
- Added action bar progress display during active skill tasks.
- Added SmithAI-Server `/task` endpoint for task planning.
- Added SmithAI-Server `/feedback` endpoint for receiving training feedback.
- Added `MODELS.md` with recommended model downloads and quantization guidance.
- Added `API.md` with full SmithAI-Server endpoint reference.
- Added unit tests: `KnowledgeBaseTest`, `TrainingManagerTest`, `MemoryManagerTest`, `ConfigTest`.
- Added Mockito test dependency for plugin unit tests.

### Changed
- Updated `plugin.yml` usage string to include new commands.
- Updated `todo.md` with new completed items and remaining priorities.

## 2.0.0

### Added
- Official SmithAI plugin with Smith-Mini 1.0 built-in brain.
- External SmithGPT 1.0 and 2.0 support via SmithAI-Server.
- 9000-core skill library generated at runtime.
- Chat, memory, commands, NPCs, and skill executor.
- `/smithai do`, `/smithai stop`, `/smithai goto`, `/smithai feedback`, `/smithai report`, `/smithai debug`, `/smithai health`, and `/SmithAPI` commands.
- Training feedback system and issue report manager.
- Subsystem health tracking and graceful degradation.
- Docker support for SmithAI-Server.
- CI workflow, build scripts, and release packaging.
- Unit tests for skill generator, task planner, and subsystem health.
