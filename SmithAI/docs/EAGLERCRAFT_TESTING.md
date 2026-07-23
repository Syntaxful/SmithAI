# Eaglercraft Testing Guide

## Prerequisites
- Eaglercraft 1.8.x server running Bukkit/Spigot/Paper
- SmithAI v2.0.0+ plugin in `plugins/` folder
- Eaglercraft 1.8.x client connected to the server

## Automated Checks (No Client Needed)

### 1. Version Detection
- Start the server and check console for: `[SmithAI] Detected server: <version> Eaglercraft`
- Run `/smithai version` — should show "Eaglercraft" in the version string

### 2. Plugin Load
- Plugin should load without errors
- All subsystems should report healthy: `/smithai health`

### 3. Skill Dispatch
- Run `/smithai do mine diamond` — should target Y=11
- Run `/smithai do mine iron` — should target Y=40
- The skill executor should complete or give a valid error

### 4. Commands
- `/smithai help` — shows help
- `/smithai list` — lists NPCs
- `/smithai config` — shows config
- All 25+ subcommands should work

## Manual In-Game Tests

### 5. Chat Integration
1. Say "Smith_AI hello" in chat
2. The NPC should respond with a greeting
3. Say "Smith_AI good bot" — training recorded
4. Say "Smith_AI follow me" — NPC follows you

### 6. NPC Rendering
1. NPC should appear as a player model with a blue leather uniform
2. Nametag should show "Smith_AI" above the NPC
3. NPC should look at nearby players
4. Animate: when NPC walks, when NPC mines, when NPC fights

### 7. World Interaction
1. Say "Smith_AI mine stone" — NPC breaks stone
2. Say "Smith_AI place torch" — NPC places torch
3. Say "Smith_AI open door" — NPC opens nearby door
4. Say "Smith_AI farm" — NPC plants/harvests crops
5. Say "Smith_AI shear sheep" — NPC shears sheep
6. Say "Smith_AI milk cow" — NPC milks cow

### 8. Combat
1. Say "Smith_AI fight the zombie" — NPC fights zombie
2. NPC should auto-equip best armor and weapon
3. NPC should auto-heal and eat when low

### 9. Building
1. Say "Smith_AI build shelter" — NPC builds 3×3 shelter
2. Say "Smith_AI build from schematic" — NPC builds from schematic file

### 10. Inventory
1. Say "Smith_AI collect drops" — NPC collects nearby items
2. Say "Smith_AI stockpile" — NPC moves items to chest
3. Say "Smith_AI craft planks" — NPC crafts planks from logs

## Known Differences from Java Edition
| Feature | Java Edition | Eaglercraft |
|---------|-------------|-------------|
| Diamond Y level | -59 (1.18+) | 11 |
| Iron Y level | 16 (1.18+) | 40 |
| Modern blocks (barrels, etc.) | Available | Not available |
| Shields | Available | Not available (1.8) |
| Elytra | Available | Not available (1.8) |
| Netherite | Available | Not available |
| Warden/Deep Dark | Available | Not available |

All these differences are handled automatically by SmithAI's VersionInfo system.
