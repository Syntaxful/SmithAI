# SmithAI Knowledge Base

This folder contains the knowledge entries used by Smith_AI. The target is 29,000 entries covering Minecraft mechanics, blocks, items, mobs, recipes, structures, and strategies.

## Format

Each file is a JSON file with entries like:

```json
[
  {
    "id": "minecraft:stone",
    "category": "block",
    "name": "Stone",
    "description": "A common solid block found underground. Drops cobblestone when mined without Silk Touch.",
    "tags": ["block", "mining", "building"]
  }
]
```

## Categories

- `blocks` — block properties, hardness, drops, best tools
- `items` — weapons, tools, food, materials
- `mobs` — hostile, passive, neutral mob behaviors and drops
- `recipes` — crafting and smelting recipes
- `structures` — villages, temples, strongholds, fortresses
- `biomes` — biome characteristics and resources
- `dimensions` — overworld, nether, end
- `mechanics` — redstone, enchanting, brewing, farming
- `combat` — weapon stats, mob attack patterns, defense
- `strategy` — how to beat the game, find diamonds, build farms

## Generating entries

A generator script can be added later to build this from public Minecraft data sources (e.g., Minecraft Wiki, MinecraftData, PrismarineJS).

## Current status

This is a placeholder. The knowledge base will be populated as the project grows.
