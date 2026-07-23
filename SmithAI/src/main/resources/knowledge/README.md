# SmithAI Knowledge Base

These JSON files provide the built-in knowledge base for Smith-Mini 1.0 and the external SmithGPT servers.

## Files

- `blocks.json` — blocks, ores, and utility blocks
- `items.json` — tools, weapons, materials, and food
- `mobs.json` — hostile, neutral, and passive mobs
- `recipes.json` — crafting and smelting recipes
- `strategy.json` — game strategies and progression tips

## Format

Each entry is a JSON object:

```json
{
  "id": "minecraft:stone",
  "category": "block",
  "name": "Stone",
  "description": "A common solid block found underground.",
  "tags": ["block", "mining", "building"]
}
```

## Adding knowledge

Add more entries to any JSON file, then rebuild the plugin. The knowledge base is loaded from the JAR at runtime.
