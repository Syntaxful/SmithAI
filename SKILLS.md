# SmithAI Skill Library

SmithAI ships with a **9000-core-skill library** that is generated at runtime so the plugin JAR stays small. The library is broad rather than item-specific, so the AI can reason about high-level goals like "beat the game", "secure the base", or "explore the nether".

## Tiers

| Model | Core Skills | What they cover |
|-------|-------------|-----------------|
| Smith-Mini 1.0 | 900 | Chat, social, movement, basic interaction, simple tasks |
| SmithGPT 1.0 | 1800 | All Mini skills + gathering, crafting, building, combat, farming, exploration |
| SmithGPT 2.0 | 6300 | All GPT-1 skills + advanced progression, automation, strategy, roleplay, endgame |

Higher-tier brains can use every lower-tier skill.

## How the library is generated

The first time the plugin starts, it creates `plugins/SmithAI/skills.yml` from a compact set of templates and word lists. The JAR itself only contains the generator code (~13KB), so the shipped plugin stays small. Delete `skills.yml` to regenerate it.

## Skill categories

### Smith-Mini 1.0 (900 skills)
- **Chat:** greet, ask, answer, joke, thank, apologize, warn, praise, encourage, etc.
- **Social:** wave, bow, dance, emote, celebrate, mourn, etc.
- **Movement:** follow, stay, move, turn, look, jump, sneak, sprint, etc.
- **Basic interaction:** use, inspect, equip, drop, pick up, eat, sleep, etc.
- **Memory/Status:** remember, recall, report, check health, check inventory, etc.
- **Task control:** begin task, end task, pause, resume, cancel, retry, mark done, etc.

### SmithGPT 1.0 (1800 skills)
- **Resource gathering:** gather wood, stone, coal, iron, copper, gold, diamond, crops, etc.
- **Crafting:** craft tools, weapons, armor, food, blocks, etc.
- **Building:** build house, shelter, wall, bridge, farm, room, etc.
- **Combat:** fight hostile mobs, defend, ambush, retreat, block, dodge, etc.
- **Farming:** plant, water, harvest, breed, feed, tame, etc.
- **Exploration:** explore caves, villages, biomes, locate structures, etc.
- **Utility:** sort, store, trade, manage, smelt, brew, etc.

### SmithGPT 2.0 (6300 skills)
- **Endgame progression:** conquer nether, master end, defeat dragon, wither, warden, etc.
- **Automation:** build autonomous mines, mob grinders, farms, storage networks, etc.
- **Strategy:** raid, siege, defend, fortify, patrol, escort, strategize, etc.
- **Magic/Enchantment:** enchant, summon, ward, channel, invoke, etc.
- **Economy/Social:** trade, barter, invest, build guilds, kingdoms, alliances, etc.
- **Terraforming:** terraform, irrigate, reforest, colonize, build infrastructure, etc.
- **Advanced building:** reactors, foundries, laboratories, portal networks, roads, etc.
- **Roleplay:** lead, recruit, train, inspire, command, govern, etc.

## Using skills in-game

Ask Smith_AI to do something:

```
Smith_AI, get diamonds
Smith_AI, build a nether portal
Smith_AI, beat the game
Smith_AI, build a secure base
Smith_AI, defend the area
Smith_AI, tell me a joke
Smith_AI, follow me
```

Or use commands:

```
/smithai do get diamonds
/smithai do beat the game
/smithai stop
/smithai goto 100 64 -200
```

## Adding custom skills

Edit `plugins/SmithAI/skills.yml` and add entries like:

```yaml
my_custom_skill:
  description: "My custom broad skill"
  type: composite
  tier: gpt2
  parameters: {}
```

Then add handling in `com/smithai/skills/SkillDispatcher.java` or a custom plugin that hooks into the skill executor.

## Implementation status

- [x] 9000 skill definitions generated at runtime
- [x] Tier-aware skill registry
- [x] Skill dispatcher for broad categories
- [x] Skill executor queue
- [x] Task planner for common goals
- [x] Basic movement (follow, stay, goto coordinates)
- [x] Basic block breaking and placing
- [x] Tool selection logic
- [x] Feedback and issue reporting
- [ ] Full primitive execution for every skill (in progress)
- [ ] Pathfinding-based movement (navmesh/raycast)
- [ ] Inventory-aware crafting
- [ ] Mob combat with tactics
- [ ] Full endgame task sequences
