# DONOW — Next Agent Checklist

When you pick up this project, do **all** of the items below before declaring the task done. This list captures the current owner’s explicit requirements.

## Movement
- [ ] Smith_AI must actually move. The primary path uses Citizens’ Navigator when Citizens is installed; velocity-based fallback is kept for the villager fallback.
- [ ] `/smithai follow` and `/smithai goto` must make the NPC walk to the target.
- [ ] If movement is broken, verify `PlayerModelHelper` is reflecting into `net.citizensnpcs.api.CitizensAPI` without assuming methods like `setUseMinecraftAI` exist in every Citizens version.

## Visuals / Equipment
- [ ] Smith_AI must spawn as a **player model** (not a villager) when Citizens is present.
- [ ] The player-model NPC must visibly wear armor (helmet, chestplate, leggings, boots) and hold items in both main hand and off hand.
- [ ] NPC must be visible and not invisible/silent-only.

## Looking / Animations
- [ ] When idle and a player is within ~8 blocks, the NPC should look at the player.
- [ ] While following a task, the NPC should look at what it is doing (e.g., at the block it is breaking/placing).
- [ ] When bridging or placing blocks, the NPC should crouch and look downward.
- [ ] Idle-looking must be suppressed while a task is active (`SmithNPC.isDoingTask()` / `setTaskLookTarget`).

## Chat / Communication
- [ ] The NPC must **not** broadcast or whisper plan-style messages like `get wood -> craft` or long step descriptions.
- [ ] Keep confirmations minimal; use the action bar for current-skill feedback instead of chat spam.
- [ ] Do not announce tasks like `Task understood: ...` in chat.

## API / External Brain
- [ ] External LLM server is optional. The plugin works with the built-in Smith-Mini 1.0 brain when no API is configured.
- [ ] Do not require the user to set an API key for basic movement/equipment/chat behavior.

## Startup Speed
- [ ] The plugin must enable quickly. Do not synchronously generate large YAML files or parse huge JSON during `onEnable`.
- [ ] Keep heavy initialization (e.g., skill registry generation, knowledge base loading) in-memory or lazy-loaded.
- [ ] If a custom `skills.yml` exists, loading it must not block the server for seconds.

## Model Files (AI / SmithAI-Server)
- [ ] Keep model files (GGUF, checkpoints, weights) as small as possible while preserving all model capabilities and features.
- [ ] Prefer smaller quantization levels or distilled models that still satisfy the feature set.
- [ ] Document any trade-offs between model size and capability in the SmithAI-Server README.

## Code Quality
- [ ] Keep files as small and direct as possible while preserving every feature and keeping the plugin working on 1.12–1.21.x.
- [ ] Keep Java 8 bytecode and `spigot-api 1.12.2` target so the JAR loads on 1.12 servers.
- [ ] Keep builds fast; avoid adding heavy dependencies or long build steps.
- [ ] After editing, rebuild the shaded JAR and replace `SmithAI-2.0.0.jar` at the repo root.

## Release
- [ ] Commit all changes with a clear message and push to the remote `main` branch.
- [ ] Copy the final JAR and source changes back to the workspace root so the user can download it.
- [ ] Present the updated `SmithAI-2.0.0.jar` to the user.
