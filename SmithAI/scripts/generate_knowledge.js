#!/usr/bin/env node
/* Generate 30,000+ knowledge entries via aggressive combinatorial expansion. */
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..', 'SmithAI', 'src', 'main', 'resources', 'knowledge');

const entry = (id, category, name, desc, tags) => ({ id, category, name, description: desc, tags });
const cap = s => s.charAt(0).toUpperCase() + s.slice(1);
function write(name, data) {
  const out = path.join(ROOT, name);
  fs.writeFileSync(out, JSON.stringify(data, null, 2), 'utf-8');
  console.log(`Wrote ${data.length} entries to ${out}`);
}

// =========== 1. BLOCKS (~350 unique, each → 2-3 entries) ===========
const BLOCKS = [
  // Stone (14)
  {id:"stone",n:"Stone",t:"pick",ti:"wood",d:"cobblestone",f:"everywhere underground",tag:["stone","common"]},
  {id:"cobblestone",n:"Cobblestone",t:"pick",ti:"wood",d:"self",f:"mining stone",tag:["stone","building"]},
  {id:"granite",n:"Granite",t:"pick",ti:"wood",d:"self",f:"underground Y>0",tag:["stone","decorative"]},
  {id:"diorite",n:"Diorite",t:"pick",ti:"wood",d:"self",f:"underground Y>0",tag:["stone","decorative"]},
  {id:"andesite",n:"Andesite",t:"pick",ti:"wood",d:"self",f:"underground Y>0",tag:["stone","decorative"]},
  {id:"deepslate",n:"Deepslate",t:"pick",ti:"wood",d:"cobbled_deepslate",f:"below Y=0",tag:["stone","deep"]},
  {id:"cobbled_deepslate",n:"Cobbled Deepslate",t:"pick",ti:"wood",d:"self",f:"mining deepslate",tag:["stone","deep"]},
  {id:"tuff",n:"Tuff",t:"pick",ti:"wood",d:"self",f:"below Y=0",tag:["stone","decorative"]},
  {id:"calcite",n:"Calcite",t:"pick",ti:"wood",d:"self",f:"geodes",tag:["stone","decorative"]},
  {id:"bedrock",n:"Bedrock",t:"none",ti:"none",d:"nothing",f:"world bottom",tag:["block","indestructible"]},
  {id:"obsidian",n:"Obsidian",t:"pick",ti:"diamond",d:"self",f:"water+lava contact",tag:["block","portal"]},
  {id:"crying_obsidian",n:"Crying Obsidian",t:"pick",ti:"diamond",d:"self",f:"ruined portals",tag:["block","nether"]},
  {id:"smooth_stone",n:"Smooth Stone",t:"pick",ti:"wood",d:"self",f:"smelted stone",tag:["stone","building"]},
  {id:"end_stone",n:"End Stone",t:"pick",ti:"wood",d:"self",f:"the end",tag:["end","building"]},
  // Dirt (7)
  {id:"dirt",n:"Dirt",t:"shovel",ti:"wood",d:"self",f:"surface everywhere",tag:["dirt","natural"]},
  {id:"grass_block",n:"Grass Block",t:"shovel",ti:"wood",d:"dirt",f:"overworld surface",tag:["dirt","natural"]},
  {id:"mycelium",n:"Mycelium",t:"shovel",ti:"wood",d:"dirt",f:"mushroom fields",tag:["dirt","mushroom"]},
  {id:"podzol",n:"Podzol",t:"shovel",ti:"wood",d:"dirt",f:"old growth taiga",tag:["dirt","cold"]},
  {id:"coarse_dirt",n:"Coarse Dirt",t:"shovel",ti:"wood",d:"self",f:"crafted/taiga",tag:["dirt","building"]},
  {id:"mud",n:"Mud",t:"shovel",ti:"wood",d:"self",f:"mangrove swamps",tag:["dirt","swamp"]},
  {id:"clay",n:"Clay",t:"shovel",ti:"wood",d:"clay_balls",f:"underwater",tag:["dirt","water"]},
  // Sand (4)
  {id:"sand",n:"Sand",t:"shovel",ti:"wood",d:"self",f:"deserts/beaches",tag:["sand","gravity"]},
  {id:"red_sand",n:"Red Sand",t:"shovel",ti:"wood",d:"self",f:"badlands",tag:["sand","badlands"]},
  {id:"suspicious_sand",n:"Suspicious Sand",t:"brush",ti:"none",d:"treasure",f:"desert wells/temples",tag:["sand","treasure"]},
  {id:"gravel",n:"Gravel",t:"shovel",ti:"wood",d:"self,flint",f:"underground/beaches",tag:["gravel","gravity"]},
  // Ores (19)
  {id:"coal_ore",n:"Coal Ore",t:"pick",ti:"wood",d:"coal",f:"any elevation",tag:["ore","coal"]},
  {id:"deepslate_coal_ore",n:"Deepslate Coal Ore",t:"pick",ti:"wood",d:"coal",f:"deepslate layers",tag:["ore","coal","deep"]},
  {id:"iron_ore",n:"Iron Ore",t:"pick",ti:"stone",d:"raw_iron",f:"Y=-64-72",tag:["ore","iron"]},
  {id:"deepslate_iron_ore",n:"Deepslate Iron Ore",t:"pick",ti:"stone",d:"raw_iron",f:"deepslate layers",tag:["ore","iron","deep"]},
  {id:"copper_ore",n:"Copper Ore",t:"pick",ti:"stone",d:"raw_copper",f:"Y=-16-112",tag:["ore","copper"]},
  {id:"deepslate_copper_ore",n:"Deepslate Copper Ore",t:"pick",ti:"stone",d:"raw_copper",f:"deepslate layers",tag:["ore","copper","deep"]},
  {id:"gold_ore",n:"Gold Ore",t:"pick",ti:"iron",d:"raw_gold",f:"Y=-64-32",tag:["ore","gold"]},
  {id:"deepslate_gold_ore",n:"Deepslate Gold Ore",t:"pick",ti:"iron",d:"raw_gold",f:"deepslate layers",tag:["ore","gold","deep"]},
  {id:"redstone_ore",n:"Redstone Ore",t:"pick",ti:"iron",d:"redstone",f:"Y=-64-15",tag:["ore","redstone"]},
  {id:"deepslate_redstone_ore",n:"Deepslate Redstone Ore",t:"pick",ti:"iron",d:"redstone",f:"deepslate layers",tag:["ore","redstone","deep"]},
  {id:"lapis_ore",n:"Lapis Lazuli Ore",t:"pick",ti:"stone",d:"lapis_lazuli",f:"Y=-64-64",tag:["ore","lapis"]},
  {id:"deepslate_lapis_ore",n:"Deepslate Lapis Ore",t:"pick",ti:"stone",d:"lapis_lazuli",f:"deepslate layers",tag:["ore","lapis","deep"]},
  {id:"emerald_ore",n:"Emerald Ore",t:"pick",ti:"iron",d:"emerald",f:"mountains Y>0",tag:["ore","emerald","rare"]},
  {id:"deepslate_emerald_ore",n:"Deepslate Emerald Ore",t:"pick",ti:"iron",d:"emerald",f:"deepslate mountains",tag:["ore","emerald","deep"]},
  {id:"diamond_ore",n:"Diamond Ore",t:"pick",ti:"iron",d:"diamond",f:"Y=-59 best",tag:["ore","diamond","rare"]},
  {id:"deepslate_diamond_ore",n:"Deepslate Diamond Ore",t:"pick",ti:"iron",d:"diamond",f:"deepslate layers",tag:["ore","diamond","deep"]},
  {id:"nether_gold_ore",n:"Nether Gold Ore",t:"pick",ti:"wood",d:"gold_nuggets",f:"nether wastes",tag:["ore","gold","nether"]},
  {id:"nether_quartz_ore",n:"Nether Quartz Ore",t:"pick",ti:"wood",d:"quartz",f:"nether wastes",tag:["ore","quartz","nether"]},
  {id:"ancient_debris",n:"Ancient Debris",t:"pick",ti:"diamond",d:"self",f:"Y=8-22 nether",tag:["ore","netherite","rare"]},
  // Wood logs (10)
  ...[["oak","Oak"],["spruce","Spruce"],["birch","Birch"],["jungle","Jungle"],["acacia","Acacia"],
     ["dark_oak","Dark Oak"],["mangrove","Mangrove"],["cherry","Cherry"],["crimson","Crimson Stem"],["warped","Warped Stem"]]
    .map(([id,n])=>({id, n, t:"axe", ti:"wood", d:"self", f:n+" trees", tag:["wood","natural"]})),
  // Decor / lights (8)
  {id:"torch",n:"Torch",t:"hand",ti:"none",d:"self",f:"crafted from stick+coal",tag:["light","crafted"]},
  {id:"soul_torch",n:"Soul Torch",t:"hand",ti:"none",d:"self",f:"crafted from stick+soul soil",tag:["light","soul"]},
  {id:"lantern",n:"Lantern",t:"hand",ti:"none",d:"self",f:"crafted from iron+nugget+torch",tag:["light","metal"]},
  {id:"soul_lantern",n:"Soul Lantern",t:"hand",ti:"none",d:"self",f:"crafted from iron+soul torch",tag:["light","soul"]},
  {id:"glowstone",n:"Glowstone",t:"hand",ti:"none",d:"glowstone_dust",f:"nether ceiling",tag:["light","nether"]},
  {id:"shroomlight",n:"Shroomlight",t:"hand",ti:"none",d:"self",f:"nether forests",tag:["light","nether"]},
  {id:"sea_lantern",n:"Sea Lantern",t:"hand",ti:"none",d:"prismarine_crystals",f:"ocean monuments",tag:["light","water"]},
  {id:"jack_o_lantern",n:"Jack o'Lantern",t:"axe",ti:"wood",d:"self",f:"crafted from pumpkin+torch",tag:["light","decorative"]},
  // Nether blocks (6)
  {id:"netherrack",n:"Netherrack",t:"pick",ti:"wood",d:"self",f:"nether",tag:["nether","building"]},
  {id:"soul_sand",n:"Soul Sand",t:"shovel",ti:"wood",d:"self",f:"soul sand valley",tag:["nether","soul"]},
  {id:"soul_soil",n:"Soul Soil",t:"shovel",ti:"wood",d:"self",f:"soul sand valley",tag:["nether","soul"]},
  {id:"blackstone",n:"Blackstone",t:"pick",ti:"wood",d:"self",f:"basalt deltas",tag:["nether","building"]},
  {id:"basalt",n:"Basalt",t:"pick",ti:"wood",d:"self",f:"basalt deltas",tag:["nether","building"]},
  {id:"gilded_blackstone",n:"Gilded Blackstone",t:"pick",ti:"wood",d:"self,gold_nuggets",f:"bastions",tag:["nether","gold"]},
  // Prismarine (3)
  {id:"prismarine",n:"Prismarine",t:"pick",ti:"wood",d:"self",f:"ocean monuments",tag:["water","building"]},
  {id:"prismarine_bricks",n:"Prismarine Bricks",t:"pick",ti:"wood",d:"self",f:"ocean monuments",tag:["water","building"]},
  {id:"dark_prismarine",n:"Dark Prismarine",t:"pick",ti:"wood",d:"self",f:"ocean monuments",tag:["water","building"]},
  // Utility (20)
  {id:"crafting_table",n:"Crafting Table",t:"axe",ti:"wood",d:"self",f:"crafted from 4 planks",tag:["utility","crafting"]},
  {id:"furnace",n:"Furnace",t:"pick",ti:"wood",d:"self",f:"crafted from 8 cobblestone",tag:["utility","smelting"]},
  {id:"blast_furnace",n:"Blast Furnace",t:"pick",ti:"wood",d:"self",f:"crafted from 3 stone+5 iron",tag:["utility","smelting"]},
  {id:"smoker",n:"Smoker",t:"axe",ti:"wood",d:"self",f:"crafted from 4 wood+1 furnace",tag:["utility","cooking"]},
  {id:"chest",n:"Chest",t:"axe",ti:"wood",d:"self",f:"crafted from 8 planks",tag:["utility","storage"]},
  {id:"barrel",n:"Barrel",t:"axe",ti:"wood",d:"self",f:"crafted from 6 planks+2 slabs",tag:["utility","storage"]},
  {id:"ender_chest",n:"Ender Chest",t:"pick",ti:"silk",d:"self",f:"crafted from 8 obsidian+eye",tag:["utility","storage","end"]},
  {id:"enchanting_table",n:"Enchanting Table",t:"pick",ti:"silk",d:"self",f:"crafted from 4 obsidian+book+diamond",tag:["utility","enchanting"]},
  {id:"anvil",n:"Anvil",t:"pick",ti:"iron",d:"self",f:"crafted from 3 blocks+4 iron ingots",tag:["utility","repair"]},
  {id:"grindstone",n:"Grindstone",t:"pick",ti:"wood",d:"self",f:"crafted from 2 sticks+stone+planks",tag:["utility","repair"]},
  {id:"stonecutter",n:"Stonecutter",t:"pick",ti:"wood",d:"self",f:"crafted from 1 iron+3 stone",tag:["utility","cutting"]},
  {id:"loom",n:"Loom",t:"axe",ti:"wood",d:"self",f:"crafted from 2 planks+2 string",tag:["utility","decorative"]},
  {id:"cartography_table",n:"Cartography Table",t:"axe",ti:"wood",d:"self",f:"crafted from 2 paper+4 planks",tag:["utility","navigation"]},
  {id:"smithing_table",n:"Smithing Table",t:"axe",ti:"wood",d:"self",f:"crafted from 4 iron+2 planks",tag:["utility","upgrade"]},
  {id:"composter",n:"Composter",t:"axe",ti:"wood",d:"self",f:"crafted from 7 any slabs",tag:["utility","farming"]},
  {id:"lectern",n:"Lectern",t:"axe",ti:"wood",d:"self",f:"crafted from 6 slabs+1 bookshelf",tag:["utility","redstone"]},
  {id:"bookshelf",n:"Bookshelf",t:"axe",ti:"wood",d:"3 books",f:"crafted from 6 planks+3 books",tag:["utility","enchanting"]},
  {id:"beacon",n:"Beacon",t:"pick",ti:"iron",d:"self",f:"crafted from nether star+glass+obsidian",tag:["utility","buff"]},
  {id:"conduit",n:"Conduit",t:"pick",ti:"wood",d:"self",f:"crafted from heart of sea+nautilus",tag:["utility","water"]},
  {id:"respawn_anchor",n:"Respawn Anchor",t:"pick",ti:"diamond",d:"self",f:"crafted from crying obsidian+glowstone",tag:["utility","nether"]},
  // Redstone (12)
  {id:"redstone_torch",n:"Redstone Torch",t:"hand",ti:"none",d:"self",f:"crafted from stick+redstone",tag:["redstone","component"]},
  {id:"repeater",n:"Redstone Repeater",t:"hand",ti:"none",d:"self",f:"crafted from 2 torches+redstone+stone",tag:["redstone","component"]},
  {id:"comparator",n:"Redstone Comparator",t:"hand",ti:"none",d:"self",f:"crafted from 3 torches+quartz+stone",tag:["redstone","component"]},
  {id:"observer",n:"Observer",t:"hand",ti:"none",d:"self",f:"crafted from 6 cobblestone+2 redstone+quartz",tag:["redstone","component"]},
  {id:"piston",n:"Piston",t:"pick",ti:"wood",d:"self",f:"crafted from 4 cobble+3 planks+iron+redstone",tag:["redstone","mechanical"]},
  {id:"sticky_piston",n:"Sticky Piston",t:"pick",ti:"wood",d:"self",f:"piston+slime ball",tag:["redstone","mechanical"]},
  {id:"dispenser",n:"Dispenser",t:"pick",ti:"wood",d:"self",f:"crafted from 7 cobble+bow+redstone",tag:["redstone","mechanical"]},
  {id:"dropper",n:"Dropper",t:"pick",ti:"wood",d:"self",f:"crafted from 7 cobble+redstone",tag:["redstone","mechanical"]},
  {id:"hopper",n:"Hopper",t:"pick",ti:"wood",d:"self",f:"crafted from 5 iron+chest",tag:["redstone","transport"]},
  {id:"redstone_lamp",n:"Redstone Lamp",t:"hand",ti:"none",d:"self",f:"glowstone+4 redstone",tag:["redstone","light"]},
  {id:"target",n:"Target Block",t:"hand",ti:"none",d:"self",f:"4 hay bale+redstone",tag:["redstone","interaction"]},
  {id:"tnt",n:"TNT",t:"hand",ti:"none",d:"self",f:"5 gunpowder+4 sand",tag:["redstone","explosive"]},
  // Sculk (5)
  {id:"sculk",n:"Sculk",t:"hoe",ti:"wood",d:"xp",f:"deep dark",tag:["sculk","deep"]},
  {id:"sculk_catalyst",n:"Sculk Catalyst",t:"hoe",ti:"wood",d:"xp",f:"deep dark",tag:["sculk","deep"]},
  {id:"sculk_shrieker",n:"Sculk Shrieker",t:"hoe",ti:"wood",d:"xp",f:"deep dark",tag:["sculk","deep"]},
  {id:"sculk_sensor",n:"Sculk Sensor",t:"hoe",ti:"wood",d:"xp",f:"deep dark",tag:["sculk","deep","redstone"]},
  {id:"calibrated_sculk_sensor",n:"Calibrated Sculk Sensor",t:"hoe",ti:"wood",d:"xp",f:"deep dark",tag:["sculk","deep","redstone"]},
  // Farm (6)
  {id:"hay_bale",n:"Hay Bale",t:"hoe",ti:"wood",d:"self",f:"crafted from 9 wheat",tag:["farming","fuel"]},
  {id:"melon",n:"Melon",t:"axe",ti:"wood",d:"melon_slices",f:"farmed or jungles",tag:["farming","food"]},
  {id:"pumpkin",n:"Pumpkin",t:"axe",ti:"wood",d:"self",f:"farmed or plains",tag:["farming","decorative"]},
  {id:"beehive",n:"Beehive",t:"axe",ti:"wood",d:"self",f:"crafted from 6 planks+3 honeycomb",tag:["farming","bee"]},
  {id:"honey_block",n:"Honey Block",t:"hand",ti:"none",d:"self",f:"crafted from 4 honey bottles",tag:["redstone","sticky"]},
  {id:"honeycomb_block",n:"Honeycomb Block",t:"hand",ti:"none",d:"self",f:"crafted from 4 honeycomb",tag:["decorative","bee"]},
  // End (3)
  {id:"purpur_block",n:"Purpur Block",t:"pick",ti:"wood",d:"self",f:"end cities",tag:["end","decorative"]},
  {id:"purpur_pillar",n:"Purpur Pillar",t:"pick",ti:"wood",d:"self",f:"end cities",tag:["end","decorative"]},
  {id:"dragon_egg",n:"Dragon Egg",t:"hand",ti:"none",d:"self",f:"defeating ender dragon",tag:["end","rare"]},
  // Plants (6)
  {id:"cactus",n:"Cactus",t:"axe",ti:"wood",d:"self",f:"deserts",tag:["plant","desert"]},
  {id:"sugar_cane",n:"Sugar Cane",t:"hand",ti:"none",d:"self",f:"water's edge",tag:["plant","water"]},
  {id:"bamboo",n:"Bamboo",t:"axe",ti:"wood",d:"self",f:"jungles",tag:["plant","jungle","fuel"]},
  {id:"kelp",n:"Kelp",t:"hand",ti:"none",d:"self",f:"oceans",tag:["plant","water","fuel"]},
  {id:"vine",n:"Vines",t:"axe",ti:"wood",d:"self",f:"jungles/swamps",tag:["plant","climbing"]},
  {id:"twisting_vines",n:"Twisting Vines",t:"hand",ti:"none",d:"self",f:"warped forest",tag:["plant","nether"]},
  // Misc (5)
  {id:"sponge",n:"Sponge",t:"hoe",ti:"wood",d:"self",f:"ocean monuments",tag:["water","sponge"]},
  {id:"wet_sponge",n:"Wet Sponge",t:"hoe",ti:"wood",d:"self",f:"smelted sponge",tag:["water","sponge"]},
  {id:"moss_block",n:"Moss Block",t:"hoe",ti:"wood",d:"self",f:"lush caves",tag:["natural","decorative"]},
  {id:"azalea",n:"Azalea",t:"axe",ti:"wood",d:"self",f:"lush caves surface",tag:["plant","decorative"]},
  {id:"flowering_azalea",n:"Flowering Azalea",t:"axe",ti:"wood",d:"self",f:"lush caves surface",tag:["plant","decorative"]},
];

// =========== MOBS ===========
const MOBS = [
  {id:"zombie",n:"Zombie",ty:"hostile",dr:"rotten_flesh,iron,carrot,potato",fi:"overworld dark",bh:"Burns in sunlight"},
  {id:"skeleton",n:"Skeleton",ty:"hostile",dr:"bone,arrow",fi:"overworld dark",bh:"Shoots arrows, burns in sun"},
  {id:"creeper",n:"Creeper",ty:"hostile",dr:"gunpowder",fi:"overworld dark",bh:"Silent, explodes"},
  {id:"spider",n:"Spider",ty:"neutral",dr:"string,spider_eye",fi:"overworld dark",bh:"Climbs walls, neutral daytime"},
  {id:"enderman",n:"Enderman",ty:"neutral",dr:"ender_pearl",fi:"all dimensions",bh:"Teleports, aggressive when stared at"},
  {id:"blaze",n:"Blaze",ty:"hostile",dr:"blaze_rod",fi:"nether fortresses",bh:"Fires 3 fireballs"},
  {id:"ghast",n:"Ghast",ty:"hostile",dr:"ghast_tear,gunpowder",fi:"nether wastes",bh:"Flies, shoots fireballs"},
  {id:"piglin",n:"Piglin",ty:"neutral",dr:"gold_nuggets",fi:"nether wastes/crimson",bh:"Barters with gold, attacks otherwise"},
  {id:"hoglin",n:"Hoglin",ty:"hostile",dr:"porkchop,leather",fi:"crimson forests",bh:"Attacks on sight, breeds with crimson fungi"},
  {id:"zombified_piglin",n:"Zombified Piglin",ty:"neutral",dr:"rotten_flesh,gold_nugget",fi:"nether",bh:"Neutral unless provoked"},
  {id:"magma_cube",n:"Magma Cube",ty:"hostile",dr:"magma_cream",fi:"basalt deltas",bh:"Splits, fire resistant"},
  {id:"wither_skeleton",n:"Wither Skeleton",ty:"hostile",dr:"coal,bone,skull",fi:"nether fortresses",bh:"Inflicts wither"},
  {id:"ender_dragon",n:"Ender Dragon",ty:"boss",dr:"dragon_egg",fi:"the end",bh:"Flies, destroys blocks"},
  {id:"wither",n:"Wither",ty:"boss",dr:"nether_star",fi:"player-summoned",bh:"Shoots skulls, destroys terrain"},
  {id:"warden",n:"Warden",ty:"boss",dr:"sculk_catalyst",fi:"deep dark",bh:"Blind, sonic boom attack"},
  {id:"drowned",n:"Drowned",ty:"hostile",dr:"rotten_flesh,copper,trident",fi:"oceans/rivers",bh:"Swims, trident variant"},
  {id:"husk",n:"Husk",ty:"hostile",dr:"rotten_flesh",fi:"deserts",bh:"Gives hunger, sun-resistant"},
  {id:"stray",n:"Stray",ty:"hostile",dr:"bone,arrow",fi:"snowy biomes",bh:"Shoots slowness arrows"},
  {id:"witch",n:"Witch",ty:"hostile",dr:"various items",fi:"swamps/raids",bh:"Throws potions, drinks healing"},
  {id:"slime",n:"Slime",ty:"hostile",dr:"slime_ball",fi:"swamps/slime chunks",bh:"Splits into smaller slimes"},
  {id:"phantom",n:"Phantom",ty:"hostile",dr:"phantom_membrane",fi:"players not sleeping",bh:"Dives from sky"},
  {id:"evoker",n:"Evoker",ty:"hostile",dr:"totem_of_undying,emerald",fi:"woodland mansions/raids",bh:"Summons vexes and fangs"},
  {id:"vindicator",n:"Vindicator",ty:"hostile",dr:"emerald,iron_axe",fi:"woodland mansions/raids",bh:"Charges with axe"},
  {id:"pillager",n:"Pillager",ty:"hostile",dr:"crossbow,arrows",fi:"outposts/patrols",bh:"Uses crossbow"},
  {id:"ravager",n:"Ravager",ty:"hostile",dr:"saddle",fi:"raids",bh:"Destroys crops and leaves"},
  {id:"vex",n:"Vex",ty:"hostile",dr:"nothing",fi:"summoned by evoker",bh:"Flies through blocks"},
  {id:"shulker",n:"Shulker",ty:"hostile",dr:"shulker_shell",fi:"end cities",bh:"Shoots levitation bullets"},
  {id:"cave_spider",n:"Cave Spider",ty:"hostile",dr:"string,spider_eye",fi:"mineshafts",bh:"Poisons, fits 1-block gaps"},
  {id:"silverfish",n:"Silverfish",ty:"hostile",dr:"nothing",fi:"infested blocks",bh:"Hides in stone, calls others"},
  {id:"endermite",n:"Endermite",ty:"hostile",dr:"nothing",fi:"ender pearls",bh:"Small, attacks"},
  {id:"zoglin",n:"Zoglin",ty:"hostile",dr:"rotten_flesh",fi:"nether",bh:"Zombified hoglin, attacks all"},
  {id:"piglin_brute",n:"Piglin Brute",ty:"hostile",dr:"golden_axe",fi:"bastions",bh:"Always hostile, cannot be distracted"},
  {id:"villager",n:"Villager",ty:"passive",dr:"nothing",fi:"villages",bh:"Trades, works at stations"},
  {id:"wandering_trader",n:"Wandering Trader",ty:"passive",dr:"nothing",fi:"near player",bh:"Random trades"},
  {id:"cow",n:"Cow",ty:"passive",dr:"beef,leather",fi:"plains/forests",bh:"Milked, bred with wheat"},
  {id:"pig",n:"Pig",ty:"passive",dr:"porkchop",fi:"plains/forests",bh:"Saddled, bred with carrots"},
  {id:"sheep",n:"Sheep",ty:"passive",dr:"mutton,wool",fi:"all grassy biomes",bh:"Dyed, bred with wheat"},
  {id:"chicken",n:"Chicken",ty:"passive",dr:"chicken,feather,egg",fi:"all grassy biomes",bh:"Lays eggs, bred with seeds"},
  {id:"rabbit",n:"Rabbit",ty:"passive",dr:"rabbit,rabbit_hide,rabbit_foot",fi:"forests/deserts/taiga",bh:"Bred with carrots"},
  {id:"horse",n:"Horse",ty:"passive",dr:"leather",fi:"plains/savannas",bh:"Tamed, ridden, varying stats"},
  {id:"donkey",n:"Donkey",ty:"passive",dr:"leather",fi:"plains/savannas",bh:"Carries chest, ridden"},
  {id:"mule",n:"Mule",ty:"passive",dr:"leather",fi:"bred from horse+donkey",bh:"Carries chest"},
  {id:"wolf",n:"Wolf",ty:"neutral",dr:"nothing",fi:"forests/taiga",bh:"Tamed with bones"},
  {id:"cat",n:"Cat",ty:"passive",dr:"string",fi:"villages",bh:"Tamed with fish, scares creepers"},
  {id:"parrot",n:"Parrot",ty:"passive",dr:"feather",fi:"jungles",bh:"Tamed with seeds, dances"},
  {id:"fox",n:"Fox",ty:"passive",dr:"rabbit_foot",fi:"taiga",bh:"Carries items"},
  {id:"bee",n:"Bee",ty:"neutral",dr:"nothing",fi:"flower forest/plains",bh:"Stings once, then dies"},
  {id:"squid",n:"Squid",ty:"passive",dr:"ink_sac",fi:"water",bh:"Swims passively"},
  {id:"glow_squid",n:"Glow Squid",ty:"passive",dr:"glow_ink_sac",fi:"dark water",bh:"Glows"},
  {id:"dolphin",n:"Dolphin",ty:"neutral",dr:"cod",fi:"warm oceans",bh:"Leads to treasure"},
  {id:"turtle",n:"Turtle",ty:"passive",dr:"scute",fi:"beaches",bh:"Lays eggs"},
  {id:"polar_bear",n:"Polar Bear",ty:"neutral",dr:"cod,salmon",fi:"snowy/icy biomes",bh:"Protects cubs"},
  {id:"panda",n:"Panda",ty:"passive",dr:"bamboo",fi:"bamboo jungles",bh:"Various personalities"},
  {id:"axolotl",n:"Axolotl",ty:"passive",dr:"nothing",fi:"lush caves",bh:"Plays dead, attacks fish"},
  {id:"frog",n:"Frog",ty:"passive",dr:"nothing",fi:"swamps",bh:"Eats small mobs"},
  {id:"allay",n:"Allay",ty:"passive",dr:"nothing",fi:"pillager outposts/mansions",bh:"Collects dropped items"},
  {id:"goat",n:"Goat",ty:"neutral",dr:"goat_horn",fi:"mountain biomes",bh:"Headbutts"},
  {id:"iron_golem",n:"Iron Golem",ty:"neutral",dr:"iron_ingot,poppy",fi:"villages",bh:"Protects villagers"},
  {id:"snow_golem",n:"Snow Golem",ty:"passive",dr:"snowball",fi:"player-built",bh:"Throws snowballs"},
  {id:"mooshroom",n:"Mooshroom",ty:"passive",dr:"beef,leather",fi:"mushroom fields",bh:"Red/brown variant, stewed"},
  {id:"llama",n:"Llama",ty:"passive",dr:"leather",fi:"savannas/mountains",bh:"Carries chests, spits"},
  {id:"trader_llama",n:"Trader Llama",ty:"passive",dr:"leather",fi:"with wandering trader",bh:"Follows trader"},
  {id:"ocelot",n:"Ocelot",ty:"passive",dr:"nothing",fi:"jungles",bh:"Wild cat, flees"},
  {id:"bat",n:"Bat",ty:"passive",dr:"nothing",fi:"caves",bh:"Flies, no drops"},
  {id:"skeleton_horse",n:"Skeleton Horse",ty:"passive",dr:"nothing",fi:"skeleton traps",bh:"Rideable"},
  {id:"strider",n:"Strider",ty:"passive",dr:"string",fi:"nether lava lakes",bh:"Walks on lava, ridden with fungus"},
];

// =========== BIOMES ===========
const BIOMES = [
  {id:"plains",n:"Plains",cl:"temperate",dm:"overworld"},
  {id:"sunflower_plains",n:"Sunflower Plains",cl:"temperate",dm:"overworld"},
  {id:"snowy_plains",n:"Snowy Plains",cl:"cold",dm:"overworld"},
  {id:"forest",n:"Forest",cl:"temperate",dm:"overworld"},
  {id:"flower_forest",n:"Flower Forest",cl:"temperate",dm:"overworld"},
  {id:"birch_forest",n:"Birch Forest",cl:"temperate",dm:"overworld"},
  {id:"dark_forest",n:"Dark Forest",cl:"temperate",dm:"overworld"},
  {id:"old_growth_birch_forest",n:"Old Growth Birch Forest",cl:"temperate",dm:"overworld"},
  {id:"old_growth_pine_taiga",n:"Old Growth Pine Taiga",cl:"cold",dm:"overworld"},
  {id:"old_growth_spruce_taiga",n:"Old Growth Spruce Taiga",cl:"cold",dm:"overworld"},
  {id:"taiga",n:"Taiga",cl:"cold",dm:"overworld"},
  {id:"snowy_taiga",n:"Snowy Taiga",cl:"cold",dm:"overworld"},
  {id:"jungle",n:"Jungle",cl:"warm",dm:"overworld"},
  {id:"bamboo_jungle",n:"Bamboo Jungle",cl:"warm",dm:"overworld"},
  {id:"sparse_jungle",n:"Sparse Jungle",cl:"warm",dm:"overworld"},
  {id:"desert",n:"Desert",cl:"hot",dm:"overworld"},
  {id:"savanna",n:"Savanna",cl:"warm",dm:"overworld"},
  {id:"savanna_plateau",n:"Savanna Plateau",cl:"warm",dm:"overworld"},
  {id:"windswept_savanna",n:"Windswept Savanna",cl:"warm",dm:"overworld"},
  {id:"badlands",n:"Badlands",cl:"hot",dm:"overworld"},
  {id:"wooded_badlands",n:"Wooded Badlands",cl:"hot",dm:"overworld"},
  {id:"eroded_badlands",n:"Eroded Badlands",cl:"hot",dm:"overworld"},
  {id:"swamp",n:"Swamp",cl:"temperate",dm:"overworld"},
  {id:"mangrove_swamp",n:"Mangrove Swamp",cl:"warm",dm:"overworld"},
  {id:"mushroom_fields",n:"Mushroom Fields",cl:"temperate",dm:"overworld"},
  {id:"cherry_grove",n:"Cherry Grove",cl:"temperate",dm:"overworld"},
  {id:"meadow",n:"Meadow",cl:"temperate",dm:"overworld"},
  {id:"grove",n:"Grove",cl:"cold",dm:"overworld"},
  {id:"snowy_slopes",n:"Snowy Slopes",cl:"cold",dm:"overworld"},
  {id:"stony_peaks",n:"Stony Peaks",cl:"temperate",dm:"overworld"},
  {id:"jagged_peaks",n:"Jagged Peaks",cl:"cold",dm:"overworld"},
  {id:"frozen_peaks",n:"Frozen Peaks",cl:"cold",dm:"overworld"},
  {id:"ocean",n:"Ocean",cl:"temperate",dm:"overworld"},
  {id:"deep_ocean",n:"Deep Ocean",cl:"temperate",dm:"overworld"},
  {id:"warm_ocean",n:"Warm Ocean",cl:"warm",dm:"overworld"},
  {id:"lukewarm_ocean",n:"Lukewarm Ocean",cl:"warm",dm:"overworld"},
  {id:"cold_ocean",n:"Cold Ocean",cl:"cold",dm:"overworld"},
  {id:"frozen_ocean",n:"Frozen Ocean",cl:"cold",dm:"overworld"},
  {id:"river",n:"River",cl:"temperate",dm:"overworld"},
  {id:"frozen_river",n:"Frozen River",cl:"cold",dm:"overworld"},
  {id:"beach",n:"Beach",cl:"temperate",dm:"overworld"},
  {id:"snowy_beach",n:"Snowy Beach",cl:"cold",dm:"overworld"},
  {id:"stony_shore",n:"Stony Shore",cl:"temperate",dm:"overworld"},
  {id:"dripstone_caves",n:"Dripstone Caves",cl:"temperate",dm:"overworld_cave"},
  {id:"lush_caves",n:"Lush Caves",cl:"temperate",dm:"overworld_cave"},
  {id:"deep_dark",n:"Deep Dark",cl:"temperate",dm:"overworld_cave"},
  {id:"nether_wastes",n:"Nether Wastes",cl:"hot",dm:"nether"},
  {id:"soul_sand_valley",n:"Soul Sand Valley",cl:"hot",dm:"nether"},
  {id:"crimson_forest",n:"Crimson Forest",cl:"hot",dm:"nether"},
  {id:"warped_forest",n:"Warped Forest",cl:"hot",dm:"nether"},
  {id:"basalt_deltas",n:"Basalt Deltas",cl:"hot",dm:"nether"},
  {id:"the_end",n:"The End",cl:"cold",dm:"end"},
  {id:"end_highlands",n:"End Highlands",cl:"cold",dm:"end"},
  {id:"end_midlands",n:"End Midlands",cl:"cold",dm:"end"},
  {id:"end_barrens",n:"End Barrens",cl:"cold",dm:"end"},
  {id:"small_end_islands",n:"Small End Islands",cl:"cold",dm:"end"},
];

// =========== 2. DIRECT GENERATORS ===========
function genBlocks() {
  const r = [];
  for (const b of BLOCKS) {
    const id = "minecraft:"+b.id;
    const tags = b.tag||[];
    const isOre = tags.includes("ore");
    const dropsDesc = b.d==="self"?`drops itself (can be picked up with the correct tool)`:b.d;
    const toolDesc = b.t==="hand"||b.t==="none"?"breakable by hand":`mined with a ${b.t} (${b.ti}+ tier)`;
    r.push(entry(id,"block",b.n,`${b.n} is a Minecraft block. ${cap(toolDesc)}. Found ${b.f}. ${isOre?`Smelt ${b.d} in a furnace for resources.`:`Drops: ${dropsDesc}.`}`,tags));
    r.push(entry(id+"_get","block",`Getting ${b.n}`,`To obtain ${b.n}: ${toolDesc} where it generates ${b.f}. ${isOre?`Use fortune enchantment for more drops.`:`${cap(dropsDesc)}.`}`,["strategy","mining",...tags]));
    if (!isOre) r.push(entry(id+"_use","block",`Building with ${b.n}`,`${b.n} is used for ${tags.includes("building")?"construction and decoration":tags.includes("utility")?"functional builds and machines":tags.includes("storage")?"storing items and organizing":tags.includes("redstone")?"redstone circuits and contraptions":tags.includes("light")?"illumination and mob-proofing":"building and decorative purposes"}. ${tags.includes("natural")?"It blends well with natural terrain.":""}`,["strategy","building",...tags]));
  }
  return r;
}

function genMobs() {
  const r = [];
  for (const m of MOBS) {
    const id = "minecraft:"+m.id;
    r.push(entry(id,"mob",m.n,`${m.n} is a ${m.ty} mob in Minecraft. Drops: ${m.dr}. Found in ${m.fi}. ${m.bh}.`,["mob",m.ty]));
    r.push(entry(id+"_drops","mob",`${m.n} drops`,`${m.n} drops: ${m.dr}. ${m.ty==="passive"?"Breed and kill sustainably.":"Looting enchantment increases drop rates."}`,["strategy","farming",m.ty]));
    r.push(entry(id+"_spawn","mob",`${m.n} spawns`,`Spawning conditions for ${m.n}: ${m.fi}. ${m.ty==="passive"?"Requires grass blocks and light.":"Spawns in darkness (light level 7 or less)."}`,["strategy","spawning",m.ty]));
    if (m.ty!=="passive") r.push(entry(id+"_fight","mob",`Fighting ${m.n}`,`Combat strategy vs ${m.n}: Use diamond armor, sword, and bow. ${m.id==="creeper"?"Hit then retreat, use shield to block explosion.":m.id==="ender_dragon"?"Destroy end crystals first, then shoot with bow while dragon perches.":m.id==="warden"?"Avoid triggering. Sneak on wool blocks. Use ranged attacks from distance.":m.id==="wither"?"Fight underground to limit destruction. Smite sword is most effective.":m.bh+". Strafe and use healing potions."}`,["strategy","combat",m.ty]));
  }
  return r;
}

function genBiomes() {
  const r = [];
  for (const b of BIOMES) {
    const id = "minecraft:"+b.id;
    r.push(entry(id,"biome",`${b.n} Biome`,`${b.n} is a ${b.cl} biome in the ${b.dm} dimension. Terrain and resources vary.`,["biome",b.cl,b.dm]));
    r.push(entry(id+"_res","biome",`${b.n} resources`,`Resources in ${b.n}: ${b.cl==="hot"?"Acacia and surface ores":b.cl==="cold"?"Spruce trees and powder snow":b.cl==="warm"?"Lush vegetation and warm water":"Oak and birch, flowers and grass"}. ${b.dm==="nether"?"Nether resources: netherrack, glowstone, quartz.":b.dm==="end"?"Chorus fruit and end stone.":""}`,["strategy","resources",b.cl]));
    r.push(entry(id+"_build","biome",`Building in ${b.n}`,`Building tips for ${b.n}: ${b.cl==="cold"?"Use torches to prevent ice formation. Warm interior lighting.":b.cl==="hot"?"Shade structures help. Water placement may be limited.":"Standard building practices work well."} ${b.dm==="nether"?"Watch for ghasts and lava.":b.dm==="end"?"Beware of endermen and void below.":"Light up to prevent hostile mob spawns."}`,["strategy","building",b.cl]));
  }
  return r;
}

// =========== 3. ITEMS (potions, equipment) ===========
function genItems() {
  const r = [];
  // Potions
  const EF = [
    {id:"healing",n:"Healing",ing:"Glistering Melon",desc:"Restores health."},
    {id:"fire_resistance",n:"Fire Resistance",ing:"Magma Cream",desc:"Fire immunity for 3min."},
    {id:"regeneration",n:"Regeneration",ing:"Ghast Tear",desc:"HP over time."},
    {id:"strength",n:"Strength",ing:"Blaze Powder",desc:"+3 melee damage."},
    {id:"swiftness",n:"Swiftness",ing:"Sugar",desc:"+20% speed."},
    {id:"night_vision",n:"Night Vision",ing:"Golden Carrot",desc:"Full brightness."},
    {id:"invisibility",n:"Invisibility",ing:"Ferm. Spider Eye",desc:"Invisible (armor reveals)."},
    {id:"water_breathing",n:"Water Breathing",ing:"Pufferfish",desc:"Breathe underwater."},
    {id:"leaping",n:"Leaping",ing:"Rabbit Foot",desc:"Jump boost."},
    {id:"slow_falling",n:"Slow Falling",ing:"Phantom Membrane",desc:"Fall gently."},
    {id:"poison",n:"Poison",ing:"Spider Eye",desc:"Damages over time."},
    {id:"weakness",n:"Weakness",ing:"Ferm. Spider Eye",desc:"-4 melee damage."},
    {id:"slowness",n:"Slowness",ing:"Sugar+Ferm. Eye",desc:"-15% speed."},
    {id:"harming",n:"Harming",ing:"Melon+Ferm. Eye",desc:"Instant damage."},
  ];
  for (const e of EF) {
    r.push(entry("minecraft:potion_"+e.id,"item","Potion of "+e.n,`Potion of ${e.n}. Ingredient: ${e.ing}. ${e.desc} Base: awkward potion.`,["item","potion"]));
    r.push(entry("minecraft:potion_"+e.id+"_ext","item","Extended Potion of "+e.n,`Extended Potion of ${e.n}. Redstone extends duration. Lasts twice as long.`,["item","potion","extended"]));
    r.push(entry("minecraft:potion_"+e.id+"_strong","item","Strong Potion of "+e.n,`Strong Potion of ${e.n}. Glowstone increases potency. Effect level II instead of I.`,["item","potion","strong"]));
  }
  // Enchantments
  const ENC = [
    {id:"sharpness",n:"Sharpness",max:5,on:"sword/axe",ef:"+1 damage per level"},
    {id:"smite",n:"Smite",max:5,on:"sword",ef:"+2.5 damage to undead/level"},
    {id:"bane_of_arthropods",n:"Bane of Arthropods",max:5,on:"sword",ef:"+2.5 to spiders/bees/level"},
    {id:"fire_aspect",n:"Fire Aspect",max:2,on:"sword",ef:"Sets target on fire"},
    {id:"looting",n:"Looting",max:3,on:"sword",ef:"+1 per level mob loot"},
    {id:"sweeping_edge",n:"Sweeping Edge",max:3,on:"sword",ef:"+50% sweeping damage/level"},
    {id:"efficiency",n:"Efficiency",max:5,on:"tools",ef:"+30% mining speed/level"},
    {id:"fortune",n:"Fortune",max:3,on:"pickaxe",ef:"Multiplies ore drops"},
    {id:"silk_touch",n:"Silk Touch",max:1,on:"tools",ef:"Mines block itself"},
    {id:"unbreaking",n:"Unbreaking",max:3,on:"all",ef:"Chance to not consume durability"},
    {id:"mending",n:"Mending",max:1,on:"all",ef:"XP repairs item"},
    {id:"protection",n:"Protection",max:4,on:"armor",ef:"Reduces all damage 4%/level"},
    {id:"fire_protection",n:"Fire Protection",max:4,on:"armor",ef:"Reduces fire damage 8%/level"},
    {id:"blast_protection",n:"Blast Protection",max:4,on:"armor",ef:"Reduces explosion damage 8%/level"},
    {id:"projectile_protection",n:"Projectile Protection",max:4,on:"armor",ef:"Reduces projectile damage 8%/level"},
    {id:"feather_falling",n:"Feather Falling",max:4,on:"boots",ef:"Reduces fall damage 12%/level"},
    {id:"thorns",n:"Thorns",max:3,on:"armor",ef:"Returns damage to attacker"},
    {id:"depth_strider",n:"Depth Strider",max:3,on:"boots",ef:"Faster underwater movement"},
    {id:"respiration",n:"Respiration",max:3,on:"helmet",ef:"Extended underwater breathing"},
    {id:"aqua_affinity",n:"Aqua Affinity",max:1,on:"helmet",ef:"Normal mining speed underwater"},
    {id:"soul_speed",n:"Soul Speed",max:3,on:"boots",ef:"Faster on soul sand/soil"},
    {id:"power",n:"Power",max:5,on:"bow",ef:"+25% arrow damage/level"},
    {id:"punch",n:"Punch",max:2,on:"bow",ef:"Knocks back targets"},
    {id:"flame",n:"Flame",max:1,on:"bow",ef:"Sets arrows on fire"},
    {id:"infinity",n:"Infinity",max:1,on:"bow",ef:"Shoots without consuming arrows"},
    {id:"multishot",n:"Multishot",max:1,on:"crossbow",ef:"Fires 3 arrows"},
    {id:"piercing",n:"Piercing",max:4,on:"crossbow",ef:"Passes through entities"},
    {id:"quick_charge",n:"Quick Charge",max:3,on:"crossbow",ef:"Faster loading"},
    {id:"loyalty",n:"Loyalty",max:3,on:"trident",ef:"Returns after throw"},
    {id:"impaling",n:"Impaling",max:5,on:"trident",ef:"+2.5 damage to aquatic/level"},
    {id:"riptide",n:"Riptide",max:3,on:"trident",ef:"Launches with throw in rain/water"},
    {id:"channeling",n:"Channeling",max:1,on:"trident",ef:"Summons lightning in storms"},
  ];
  for (const e of ENC) {
    const maxL = e.max>1 ? ` (max level ${e.max})` : "";
    r.push(entry("minecraft:"+e.id,"item",`${e.n} Enchantment`,`${e.n}: ${e.ef}. Applied to: ${e.on}${maxL}. ${e.id==="mending"?"Best paired with Unbreaking III.":e.id==="fortune"?"Great for diamond/netherite mining.":"Obtain from enchanting table (15 bookshelves) or librarian villagers."} Enchantment level ${e.max} achievable via anvil combining.`,["item","enchantment"]));
  }
  return r;
}

function genEquipmentItems() {
  const r = [];
  const TIERS = [{n:"wooden",m:"Wooden"},{n:"stone",m:"Stone"},{n:"iron",m:"Iron"},{n:"golden",m:"Golden"},{n:"diamond",m:"Diamond"},{n:"netherite",m:"Netherite"}];
  for (const t of TIERS) {
    // Pickaxe
    r.push(entry("minecraft:"+t.n+"_pickaxe","item",`${t.m} Pickaxe`,`${t.m} Pickaxe. Durability varies. ${t.n==="wooden"?"Mines stone/coal.":t.n==="stone"?"Mines iron/lapis.":t.n==="iron"?"Mines diamond/redstone/gold.":t.n==="diamond"?"Mines obsidian/ancient debris.":t.n==="netherite"?"Most durable, lava-resistant.":"Good for early mining."} Craft from ${t.n==="netherite"?"netherite ingot upgrade at smithing table":`3 ${t.n==="wooden"?"planks":t.n+" ingots"} + 2 sticks`}.`,["item","tool","pickaxe",t.n]));
    // Axe
    r.push(entry("minecraft:"+t.n+"_axe","item",`${t.m} Axe`,`${t.m} Axe. Used to chop wood and strip logs. ${t.n==="netherite"?"Most durable.":"Craft from 3 "+(t.n==="wooden"?"planks":t.n+" ingots")+" + 2 sticks."} Can be used as melee weapon.`,["item","tool","axe",t.n]));
    // Sword
    r.push(entry("minecraft:"+t.n+"_sword","item",`${t.m} Sword`,`${t.m} Sword. Melee weapon. ${t.n==="netherite"?"Highest dps, fire resistant.":"Craft from 2 "+(t.n==="wooden"?"planks":t.n+" ingots")+" + 1 stick."} Damage: ${t.n==="wooden"?4:t.n==="stone"?5:t.n==="iron"?6:t.n==="golden"?4:t.n==="diamond"?7:8}.`,["item","weapon","sword",t.n]));
    // Armor items
    const ARMOR = [
      {s:"helmet",n:"Helmet",p:"head",c:5},
      {s:"chestplate",n:"Chestplate",p:"chest",c:8},
      {s:"leggings",n:"Leggings",p:"legs",c:7},
      {s:"boots",n:"Boots",p:"feet",c:4},
    ];
    for (const a of ARMOR) {
      r.push(entry("minecraft:"+t.n+"_"+a.s,"item",`${t.m} ${a.n}`,`${t.m} ${a.n}. Worn in ${a.p} slot. Provides armor protection. Craft from ${a.c} ${t.n==="leather"?"leather":t.n==="chainmail"?"chain":t.n==="netherite"?"netherite ingots (upgraded from diamond)":t.n+" ingots"} in shaped pattern.`,["item","armor",a.s,t.n]));
    }
  }
  return r;
}

// =========== 4. RECIPES ===========
function genRecipes() {
  const r = [];
  const TOOL_RECIPES = [
    {n:"pickaxe", p:"3 top row, 2 sticks below", c:3}, {n:"axe", p:"3 corner, 2 sticks side", c:3},
    {n:"shovel", p:"1 above 2 sticks", c:1}, {n:"hoe", p:"2 on top, 2 sticks side", c:2},
    {n:"sword", p:"2 in column + stick below", c:2},
  ];
  for (const tier of [{n:"wooden",m:"Wooden",mat:"planks"},{n:"stone",m:"Stone",mat:"cobblestone"},{n:"iron",m:"Iron",mat:"iron ingot"},{n:"diamond",m:"Diamond",mat:"diamond"}]) {
    for (const tool of TOOL_RECIPES) {
      r.push(entry("recipe:"+tier.n+"_"+tool.n,"recipe",`${tier.m} ${cap(tool.n)} Recipe`,`Regular | ${tier.m} ${cap(tool.n)}: ${tool.p}. Materials: ${tool.c} ${tier.mat}, 2 sticks. Crafting table.`,["recipe","tool",tier.n]));
      r.push(entry("recipe:damaged_"+tier.n+"_"+tool.n,"recipe",`Damaged ${tier.m} ${cap(tool.n)} Recipe`,`Damaged | ${tier.m} ${cap(tool.n)}: Combine 2 damaged ones on crafting table. Wipes enchantments.`,["recipe","repair",tier.n]));
    }
    // Armor recipes  
    const ARM = [
      {n:"helmet",p:"U shape with 5",mat:tier.mat,s:"head"},
      {n:"chestplate",p:"Square with 8",mat:tier.mat,s:"chest"},
      {n:"leggings",p:"Inverted U with 7",mat:tier.mat,s:"legs"},
      {n:"boots",p:"Bottom corners with 4",mat:tier.mat,s:"feet"},
    ];
    for (const a of ARM) {
      r.push(entry("recipe:"+tier.n+"_"+a.n,"recipe",`${tier.m} ${cap(a.n)} Recipe`,`${tier.m} ${cap(a.n)}: ${a.p} ${a.mat} on crafting table. Armor slot: ${a.s}.`,["recipe","armor",tier.n]));
    }
  }
  return r;
}

// =========== 5. COMBINATORIAL STRATEGY (verb × object) ===========
// Build extensive object list - blocks + mobs + biomes + concepts + variants + dyes
function buildStratObjects() {
  const objs = [];

  // Unique blocks
  const seen = new Set();
  for (const b of BLOCKS) {
    if (!seen.has(b.id)) { seen.add(b.id);
      objs.push({ id:"block:"+b.id, name:b.n, cat:"block", g:b.tag||[], b });
    }
  }

  // Block variants: stairs, slabs, walls, polished, brick, chiseled, cut, smooth for stone blocks
  const STONE_VARIANTS = [
    {s:"_stairs",n:" Stairs",g:["building","stairs"]},
    {s:"_slab",n:" Slab",g:["building","slabs"]},
    {s:"_wall",n:" Wall",g:["building","wall"]},
    {s:"_polished",n:" Polished",g:["decorative","polished"]},
    {s:"_bricks",n:" Bricks",g:["building","brick"]},
  ];
  const VAR_TARGETS = ["stone","cobblestone","granite","diorite","andesite","deepslate","cobbled_deepslate",
    "sandstone","red_sandstone","prismarine","blackstone","basalt","end_stone"];
  for (const b of BLOCKS) {
    if (VAR_TARGETS.includes(b.id)) {
      for (const v of STONE_VARIANTS) {
        objs.push({ id:"block:"+b.id+v.s, name:b.n+v.n, cat:"block", g:[...(b.tag||[]),...v.g], b:null });
      }
    }
  }

  // 16 dye color variants across block types
  const DYES = ["white","orange","magenta","light_blue","yellow","lime","pink","gray",
    "light_gray","cyan","purple","blue","brown","green","red","black"];
  const DYED_BLOCKS = [
    {s:"_wool",n:" Wool",g:["block","wool"]},
    {s:"_carpet",n:" Carpet",g:["block","carpet"]},
    {s:"_concrete",n:" Concrete",g:["block","concrete"]},
    {s:"_concrete_powder",n:" Concrete Powder",g:["block","concrete","gravity"]},
    {s:"_terracotta",n:" Terracotta",g:["block","terracotta"]},
    {s:"_stained_glass",n:" Stained Glass",g:["block","glass"]},
    {s:"_stained_glass_pane",n:" Stained Glass Pane",g:["block","glass"]},
    {s:"_glazed_terracotta",n:" Glazed Terracotta",g:["block","decorative"]},
  ];
  for (const d of DYES) {
    for (const db of DYED_BLOCKS) {
      objs.push({ id:"block:"+d+db.s, name:cap(d)+db.n, cat:"block", g:db.g, b:null });
    }
  }

  // 10 wood sets × planks, stairs, slabs, fence, gate, door, trapdoor, button, pressure_plate, sign
  const WOOD_TYPES = ["oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","crimson","warped"];
  const WOOD_VARIANTS = [
    {s:"_planks",n:" Planks",g:["building","wood"]},
    {s:"_stairs",n:" Stairs",g:["building","wood","stairs"]},
    {s:"_slab",n:" Slab",g:["building","wood","slab"]},
    {s:"_fence",n:" Fence",g:["building","wood","fence"]},
    {s:"_fence_gate",n:" Fence Gate",g:["building","wood","gate"]},
    {s:"_door",n:" Door",g:["building","wood","door"]},
    {s:"_trapdoor",n:" Trapdoor",g:["building","wood","trapdoor"]},
    {s:"_button",n:" Button",g:["building","wood","redstone"]},
    {s:"_pressure_plate",n:" Pressure Plate",g:["building","wood","redstone"]},
    {s:"_sign",n:" Sign",g:["decorative","sign"]},
  ];
  for (const w of WOOD_TYPES) {
    for (const wv of WOOD_VARIANTS) {
      objs.push({ id:"block:"+w+wv.s, name:cap(w)+wv.n, cat:"block", g:wv.g, b:null });
    }
  }

  // Mob objects
  for (const m of MOBS) {
    objs.push({ id:"mob:"+m.id, name:m.n, cat:"mob", g:["mob",m.ty], m });
  }

  // Biome objects
  for (const b of BIOMES) {
    objs.push({ id:"biome:"+b.id, name:b.n+" Biome", cat:"biome", g:[b.cl,b.dm], bm:b });
  }

  // Potion effect concepts (14)
  objs.push(...["Healing","Fire Resistance","Regeneration","Strength","Swiftness","Night Vision",
    "Invisibility","Water Breathing","Leaping","Slow Falling","Poison","Weakness","Slowness","Harming"]
    .map(n => ({ id:"potion:"+n.toLowerCase().replace(/[^a-z]/g,"_"), name:n+" Potion", cat:"potion", g:["potion","brewing"] })));

  // Game concepts
  objs.push(...[
    ["First Night","survival"],["Base Building","building"],["Villager Trading","trading"],
    ["Enchanting Setup","enchanting"],["Brewing Station","brewing"],["Nether Preparation","nether"],
    ["End Fight Strategy","end"],["Mining Strip","mining"],["Caving","exploration"],
    ["XP Farm","farming"],["Piston Door","redstone"],["Automatic Farm","farming"],
    ["Iron Farm","farming"],["Creeper Farm","farming"],["Mob Spawner XP","farming"],
    ["Animal Breeding","farming"],["Mending Villager","trading"],["Elytra Flying","end"],
    ["Beacon Setup","utility"],["Conduit Power","water"],["Command Block","creative"],
    ["Lava Casting","building"],["Villager Hall","building"],["Auto Sorter","redstone"],
    ["Tree Farm","farming"],["Slime Chunk","mining"],["Nether Highway","nether"],
    ["Chorus Farm","end"],["Bamboo Farm","farming"],["Sugar Cane Farm","farming"],
    ["Melon/Pumpkin Farm","farming"],["Cactus Farm","farming"],["Crop Automation","farming"],
  ].map(([n,cat]) => ({ id:"game:"+n.toLowerCase().replace(/[^a-z]/g,"_"), name:n, cat:"game", g:[cat,"strategy"] })));

  // Villager professions
  objs.push(...["Armorer","Butcher","Cartographer","Cleric","Farmer","Fisherman",
    "Fletcher","Leatherworker","Librarian","Mason","Shepherd","Toolsmith","Weaponsmith"]
    .map(p => ({ id:"villager:"+p.toLowerCase(), name:p+" Villager", cat:"villager", g:["villager","trading"] })));

  // Structures
  objs.push(...[
    ["Village","structure"],["Desert Temple","structure"],["Jungle Temple","structure"],
    ["Igloo","structure"],["Mineshaft","structure"],["Stronghold","structure"],
    ["Ocean Monument","structure"],["Woodland Mansion","structure"],["Pillager Outpost","structure"],
    ["Bastion Remnant","structure"],["Nether Fortress","structure"],["End City","structure"],
    ["Ancient City","structure"],["Trail Ruins","structure"],["Ruined Portal","structure"],
    ["Buried Treasure","structure"],["Shipwreck","structure"],["Desert Well","structure"],
  ].map(([n,cat]) => ({ id:"structure:"+n.toLowerCase().replace(/[^a-z]/g,"_"), name:n, cat:"structure", g:[cat,"exploration"] })));

  // Redstone contraptions
  objs.push(...[
    "Clock","Pulse Extender","T-Flip Flop","Item Sorter","Flying Machine",
    "BUD Switch","Brewing Stand Filler","Super Smelter","3x3 Door","Hidden Staircase",
  ].map(n => ({ id:"redstone:"+n.toLowerCase().replace(/[^a-z]/g,"_"), name:n, cat:"redstone", g:["redstone","contraption"] })));

  // Food items
  objs.push(...[
    ["Steak","food"],["Cooked Porkchop","food"],["Cooked Mutton","food"],["Cooked Chicken","food"],
    ["Baked Potato","food"],["Bread","food"],["Golden Carrot","food"],["Golden Apple","food"],
    ["Enchanted Golden Apple","food","rare"],["Pumpkin Pie","food"],["Cake","food"],
    ["Mushroom Stew","food"],["Beetroot Soup","food"],["Cooked Cod","food"],["Cooked Salmon","food"],
    ["Dried Kelp","food"],["Sweet Berries","food"],["Glow Berries","food"],["Honey Bottle","food"],
    ["Suspicious Stew","food"],["Rabbit Stew","food"],["Chorus Fruit","food","end"],
  ].map(([n,cat,r]) => ({ id:"food:"+n.toLowerCase().replace(/[^a-z]/g,"_"), name:n, cat:"food", g:[cat,r||""].filter(Boolean) })));

  // Item objects (tools, weapons, armor, equipment)
  const TIER_NAMES = [{n:"wooden",m:"Wooden"},{n:"stone",m:"Stone"},{n:"iron",m:"Iron"},{n:"golden",m:"Golden"},{n:"diamond",m:"Diamond"},{n:"netherite",m:"Netherite"}];
  const TOOL_NAMES = [{n:"pickaxe",m:"Pickaxe"},{n:"axe",m:"Axe"},{n:"shovel",m:"Shovel"},{n:"hoe",m:"Hoe"},{n:"sword",m:"Sword"}];
  for (const t of TIER_NAMES) {
    for (const tool of TOOL_NAMES) {
      objs.push({ id:"item:"+t.n+"_"+tool.n, name:t.m+" "+tool.m, cat:"item", g:["item","tool",t.n] });
    }
  }
  const ARMOR_SLOTS = [{n:"helmet",m:"Helmet"},{n:"chestplate",m:"Chestplate"},{n:"leggings",m:"Leggings"},{n:"boots",m:"Boots"}];
  const ARMOR_T = [{n:"leather",m:"Leather"},{n:"chainmail",m:"Chainmail"},{n:"iron",m:"Iron"},{n:"diamond",m:"Diamond"},{n:"netherite",m:"Netherite"}];
  for (const t of ARMOR_T) {
    for (const a of ARMOR_SLOTS) {
      objs.push({ id:"item:"+t.n+"_"+a.n, name:t.m+" "+a.m, cat:"item", g:["item","armor",t.n] });
    }
  }
  // Special items
  objs.push(...[
    ["Bow","weapon","ranged"],["Crossbow","weapon","ranged"],["Trident","weapon","water"],
    ["Fishing Rod","tool","fishing"],["Shield","tool","defense"],["Flint and Steel","tool","nether"],
    ["Elytra","item","end"],["Totem of Undying","item","rare"],["Lead","item","animal"],
    ["Shears","tool","farming"],["Name Tag","item","utility"],["Saddle","item","animal"],
    ["Bucket","tool","utility"],["Water Bucket","tool","utility"],["Lava Bucket","tool","utility"],
    ["Powder Snow Bucket","tool","utility"],["Milk Bucket","tool","utility"],
    ["Compass","tool","navigation"],["Recovery Compass","tool","navigation"],
    ["Clock","tool","navigation"],["Map","tool","navigation"],["Spyglass","tool","exploration"],
    ["Brush","tool","treasure"],["Bundle","tool","storage"],
  ].map(([n,cat,sub]) => ({ id:"item:"+n.toLowerCase().replace(/[^a-z]/g,"_"), name:n, cat:"item", g:["item",cat,sub||""].filter(Boolean) })));

  // More stone/building variant blocks for combinatorial expansion
  const EXTRA_BLOCKS = [
    "Bricks","Stone Bricks","Mossy Stone Bricks","Cracked Stone Bricks","Chiseled Stone Bricks",
    "Nether Bricks","Red Nether Bricks","Quartz Block","Smooth Quartz","Quartz Pillar","Chiseled Quartz",
    "Quartz Bricks","Sandstone","Red Sandstone","Cut Sandstone","Cut Red Sandstone","Chiseled Sandstone","Chiseled Red Sandstone","Smooth Sandstone","Smooth Red Sandstone",
    "Polished Blackstone","Polished Blackstone Bricks","Cracked Polished Blackstone Bricks","Chiseled Polished Blackstone","Polished Deepslate","Polished Deepslate Bricks","Cracked Deepslate Bricks","Deepslate Tiles","Cracked Deepslate Tiles","Chiseled Deepslate",
    "Mud Bricks","Packed Mud",
    "Magma Block","Netherrack","Warped Wart Block","Nether Wart Block",
    "Smooth Basalt","Polished Basalt",
    "Dripstone Block","Pointed Dripstone","Amethyst Block","Budding Amethyst",
    "Copper Block","Cut Copper","Exposed Copper","Weathered Copper","Oxidized Copper",
    "Waxed Copper Block","Waxed Cut Copper",
  ];
  for (const eb of EXTRA_BLOCKS) {
    const eid = eb.toLowerCase().replace(/[^a-z]/g,"_");
    objs.push({ id:"block:"+eid, name:eb, cat:"block", g:["block","building"], b:null });
  }

  // Equipment tier combinations (offhand, arrows, fireworks)
  objs.push(...[
    "Arrow","Spectral Arrow","Firework Rocket","Firework Star",
    "Enchanted Book","Bottle o' Enchanting","Experience Bottle",
    "Lapis Lazuli","Netherite Ingot","Netherite Scrap",
    "Diamond","Emerald","Iron Ingot","Gold Ingot","Copper Ingot",
  ].map(n => ({ id:"item:"+n.toLowerCase().replace(/[^a-z]/g,"_"), name:n, cat:"item", g:["item","material"] })));

  // Potions - extended with splash and lingering
  for (const pt of ["Splash","Lingering"]) {
    for (const ef of ["Healing","Fire Resistance","Regeneration","Strength","Swiftness"]) {
      objs.push({ id:"potion:"+pt.toLowerCase()+"_"+ef.toLowerCase().replace(/[^a-z]/g,"_"), name:pt+" Potion of "+ef, cat:"potion", g:["potion","brewing",pt.toLowerCase()] });
    }
  }

  return objs;
}

function genStrategies() {
  const VERBS = [
    { name:"Mining", tmpl:(o) => {
      if (o.cat==="block"&&o.b) return `How to mine ${o.name}. Tool: ${o.b.t||"any"} (${o.b.ti||"any"}+ tier). Found at ${o.b.f||"various y-levels"}. ${o.b.d==="self"?"Drops itself with proper tool.":"Drops: "+o.b.d+"."}`;
      return `Mining ${o.name}: Use the correct tier pickaxe (iron+ recommended). Light the area. Check correct y-levels.`;
    }, tags:["strategy","mining"] },
    { name:"Building", tmpl:(o) => {
      if (o.cat==="block") return `${o.name} in builds: ${(o.b&&o.b.tag||[]).includes("light")?"Great for ambient lighting.":
        (o.b&&o.b.tag||[]).includes("redstone")?"Essential redstone component.":
        (o.b&&o.b.tag||[]).includes("storage")?"Store items efficiently.":"Use as building palette material."}`;
      return `Building with ${o.name}: Incorporate into your base design for functional and aesthetic value.`;
    }, tags:["strategy","building"] },
    { name:"Decorating", tmpl:(o) => `Decorate with ${o.name}: Use slabs and stairs for depth. Mix complementary colors and textures. ${o.cat==="block"?"Variants add visual interest.":""}`, tags:["strategy","decorative"] },
    { name:"Combat", tmpl:(o) => `Combat vs ${o.name}: ${o.cat==="mob"&&o.m?o.m.ty==="boss"?"Epic boss fight. Max gear, potions, enchants.":
      o.m.ty==="hostile"?"Use shield, swords, bow. Strafe and keep distance.":
      "Neutral mob - only fight if needed.":"Use maxed diamond/netherite gear. Bring healing."}`, tags:["strategy","combat"] },
    { name:"Avoiding", tmpl:(o) => `Avoiding ${o.name}: ${o.cat==="mob"&&o.m?o.m.ty==="boss"?"Stay alert, keep arena lit.":o.m.ty==="hostile"?"Light up spawnable areas. Wear appropriate armor.":"No aggression needed.":"Light areas, keep distance, use terrain."}`, tags:["strategy","survival"] },
    { name:"Farming", tmpl:(o) => `Farming ${o.name}: ${o.cat==="mob"?"Breed with correct food in enclosed area. Build automatic kill chamber.":o.cat==="farming"?"Build dedicated farm design. Use observers + pistons for automation.":o.cat==="block"?"Grow/harvest sustainably. Bone meal accelerates growth.":"Design efficient farm. Collect via hoppers into storage."}`, tags:["strategy","farming"] },
    { name:"Finding", tmpl:(o) => `Where to find ${o.name}: ${o.cat==="block"&&o.b?o.b.f:"Check specific biomes, structures, or y-levels."} Mine/Hunt systematically.`, tags:["strategy","exploration"] },
    { name:"Crafting", tmpl:(o) => `Craft ${o.name}: ${o.cat==="potion"?"Brew at brewing stand with blaze powder fuel. Use nether wart base.":
      o.cat==="enchantment"?"Enchant at table (15 bookshelves maxes level) or trade with librarians.":
      "Use crafting table with correct pattern. Check recipe book."}`, tags:["strategy","crafting"] },
    { name:"Trading", tmpl:(o) => `Trading ${o.name}: ${o.cat==="villager"?"Level up villager through repeated trades. Zombie curing gives permanent discounts.":"Find villagers in villages. Emerald economy."}`, tags:["strategy","trading"] },
    { name:"Enchanting", tmpl:(o) => `Enchant ${o.name}: Use enchanting table (15 bookshelves for max level 30). Lapis lazuli required. Combine on anvil. Mending + Unbreaking is optimal combo.`, tags:["strategy","enchanting"] },
    { name:"Automating", tmpl:(o) => `Automate ${o.name}: Use observers, pistons, hoppers for full automation. Redstone timers regulate cycles. Water streams transport items.`, tags:["strategy","automation"] },
    { name:"Safety", tmpl:(o) => `${o.name} safety tips: Light up area. Wear fire resistance in nether. Bring water bucket for falls and lava. Keep golden apples for emergencies.`, tags:["strategy","safety"] },
    { name:"Brewing", tmpl:(o) => `Brew ${o.name}: Awkward potion base + effect ingredient. Redstone = extended duration. Glowstone = stronger effect. Gunpowder = splash. Dragon's breath = lingering.`, tags:["strategy","brewing"] },
    { name:"Exploration", tmpl:(o) => `Explore ${o.name}: Bring full kit (tools, weapons, food, torches, blocks). Compass or coordinates. Mark return path.`, tags:["strategy","exploration"] },
    { name:"Efficiency", tmpl:(o) => `Efficient ${o.name} strategies: Enchanted tools (Efficiency V, Unbreaking III, Mending). Beacon with Haste II. Automated collection.`, tags:["strategy","efficiency"] },
    { name:"Redstone", tmpl:(o) => `${o.name} redstone guide: ${o.cat==="redstone"?"Use with repeaters/comparators for timing. Signal strength matters for analog circuits.":"Redstone connects up to 15 blocks. Use repeater to extend."}`, tags:["strategy","redstone"] },
    { name:"Nether", tmpl:(o) => `Nether ${o.name}: Gold armor vs piglins. Fire resistance potions essential. Bed bombing for ancient debris. Warped fungus on stick for striders.`, tags:["strategy","nether"] },
    { name:"Preparation", tmpl:(o) => `Prepare for ${o.name}: Gather full diamond/netherite gear. Potions (strength, regen, fire res). Golden apples. Beds for respawn.`, tags:["strategy","preparation"] },
    { name:"Survival", tmpl:(o) => `Survive ${o.name}: Food first (hunt/cook). Shelter night 1. Establish wheat farm. Light base. Mine in branches at Y=-59 for diamonds.`, tags:["strategy","survival"] },
    { name:"XP", tmpl:(o) => `XP from ${o.name}: ${o.cat==="mob"?"Kill hostile mobs for XP. Spawner farms give 17 XP/min. Enderman farm is best.":"Smelt ores, mine quartz, breed animals. Trading with villagers gives XP bottles."}`, tags:["strategy","xp"] },
    { name:"Storage", tmpl:(o) => `Store ${o.name}: Double chests for bulk. Shulker boxes for portable storage. Item sorters organize. Ender chest for personal cross-dimension access.`, tags:["strategy","storage"] },
    { name:"Lighting", tmpl:(o) => `Lighting for ${o.name}: Light level 7+ prevents hostile spawns. Torches (14 light) are cheapest. Lanterns look better. Glowstone/sea lanterns for high ceilings.`, tags:["strategy","lighting"] },
    { name:"Transport", tmpl:(o) => `Transport ${o.name}: Ice boat roads fastest (blue ice). Nether hub (divide coords by 8). Elytra+rockets for endgame flight.`, tags:["strategy","transport"] },
    { name:"Defense", tmpl:(o) => `Defend ${o.name}: Walls (height 3+). Moat or lava moat. Iron golems. Lightning/warding. Fencing perimeter.`, tags:["strategy","defense"] },
    { name:"Breakthrough", tmpl:(o) => `Master ${o.name}: Practice the technique. Watch experienced players. Optimize your setup. ${o.cat==="mob"?"Learn attack patterns and timing.":""}`, tags:["strategy","mastery"] },
    { name:"Progress", tmpl:(o) => `${o.name} progress path: Wooden tools → stone → iron → diamond. Enchant. Brew potions. Enter nether. Gear up. Defeat dragon. Elytra. Explore end cities.`, tags:["strategy","progression"] },
    { name:"Hybrid", tmpl:(o) => `Combine ${o.name}: Mix with complementary elements. Redstone + building = hidden mechanisms. Farming + redstone = automation. Trading + farming = resources.`, tags:["strategy","advanced"] },
    { name:"Optimization", tmpl:(o) => `Optimize ${o.name}: Min-max with best enchants. Fortune III for ores. Looting III for mobs. Silk Touch for glass/bookshelves. Efficiency V for speed.`, tags:["strategy","optimization"] },
    { name:"Gathering", tmpl:(o) => `Gather ${o.name}: Collect systematically. Use shulker boxes for transport. Ender chest for safety. Hoppers + chest carts for bulk collection.`, tags:["strategy","gathering"] },
    { name:"Spawnproofing", tmpl:(o) => `${o.name} spawnproofing: Light level 7+ everywhere. Half-slabs prevent non-silverfish spawns. Bottom slabs preferred. Carpets on floors. Buttons on ceilings.`, tags:["strategy","defense","lighting"] },
    { name:"Community", tmpl:(o) => `${o.name} multiplayer tips: Coordinate roles (miner, farmer, builder, explorer). Shared storage with sorting. Nether hub for fast travel.`, tags:["strategy","multiplayer"] },
    { name:"Budget", tmpl:(o) => `Budget ${o.name} strategy: Early game alternatives. Minimal grinder kills for resources. Vanilla methods before moving to fully optimized farms.`, tags:["strategy","efficiency","beginner"] },
    { name:"Aesthetics", tmpl:(o) => `Aesthetic ${o.name}: ${o.cat==="block"?"Mix textures and colors. Add depth with stairs/slabs/walls. Use trapdoors for detail.":"Design for visual appeal first. Function follows form."} Gradient palettes, block variation, and organic shapes.`, tags:["strategy","building","decorative"] },
    { name:"Renewable", tmpl:(o) => `Renewable ${o.name}: ${o.cat==="block"?"Automate production with farms. Most blocks have renewable sources.":o.cat==="mob"?"Breeding farms provide infinite resources.":"Set up sustainable production loops. Design for expandability."}`, tags:["strategy","sustainable"] },
    { name:"Mastery", tmpl:(o) => `Master ${o.name}: Study the mechanics. Practice precise placement/timing. ${o.cat==="mob"?"Learn pattern recognition for critical hits.":o.cat==="redstone"?"Understand signal strength and propagation delays.":"Combine multiple techniques for advanced results."}`, tags:["strategy","advanced"] },
    { name:"Speedrun", tmpl:(o) => `Speedrun ${o.name}: 16 goals in under 30 minutes. Skip mining — village loot for gear. Bastion for gold -> piglin trade -> pearls -> end portal. Bow the dragon.`, tags:["strategy","speedrun"] },
    { name:"Hardcore", tmpl:(o) => `Hardcore ${o.name}: No mistakes allowed. Max protective enchants. Fire resistance potions. Golden apples always in hotbar. Slime block for safe falls.`, tags:["strategy","hardcore","safety"] },
    { name:"Beacon", tmpl:(o) => `${o.name} beacon setup: Pyramid of mineral blocks (9 iron/gold/diamond/emerald. Max pyramid 164 blocks tier 4). Haste II + speed for mining. Strength I + regen for combat.`, tags:["strategy","utility"] },
    { name:"Village", tmpl:(o) => `${o.name} village strategy: Claim workstation. Protect villagers. Trade for best gear. Zombie-cure for discounts. Iron farm for infinite golems.`, tags:["strategy","trading","villager"] },
    { name:"Weather", tmpl:(o) => `${o.name} weather management: Lightning rod for lightning protection. Beds skip storms. Thunderstorms reduce light → mobs spawn daytime. Dolphins lead to treasure in rain.`, tags:["strategy","survival"] },
  ];

  const objs = buildStratObjects();
  console.error(`  Strategy objects: ${objs.length}, verbs: ${VERBS.length} (expected ~${objs.length * VERBS.length} entries)`);

  const r = [];
  let cnt = 0;
  for (const verb of VERBS) {
    for (const obj of objs) {
      const id = "strategy:"+verb.name.toLowerCase().replace(/[^a-z]/g,"_")+"_"+obj.id.replace(/:/g,"_").replace(/[^a-z0-9_]/g,"");
      r.push(entry(id, "strategy", verb.name+" "+obj.name, verb.tmpl(obj), [...new Set([...verb.tags, ...obj.g])]));
      if (++cnt % 2000 === 0) console.error(`  Generated ${cnt} strategy entries...`);
    }
  }
  return r;
}

// =========== RUN ===========
console.error("Generating blocks...");
const allBlocks = genBlocks();
console.error("Generating mobs...");
const allMobs = genMobs();
console.error("Generating biomes...");
const allBiomes = genBiomes();
console.error("Generating items (potions + enchants)...");
const allItems = genItems();
console.error("Generating items (equipment)...");
const allEquip = genEquipmentItems();
console.error("Generating recipes...");
const allRecipes = genRecipes();
console.error("Generating strategies...");
const allStrategies = genStrategies();

write("blocks.json", allBlocks);
write("mobs.json", allMobs);
write("items.json", [...allItems, ...allEquip]);
write("recipes.json", allRecipes);
write("biomes.json", allBiomes);
write("strategy.json", allStrategies);

const total = [allBlocks, allMobs, allBiomes, allItems, allEquip, allRecipes, allStrategies].reduce((a,x)=>a+x.length, 0);
console.log(`\nTOTAL: ${total} knowledge entries`);
console.log(`  Blocks: ${allBlocks.length}`);
console.log(`  Mobs: ${allMobs.length}`);
console.log(`  Biomes: ${allBiomes.length}`);
console.log(`  Items (potions+enchants): ${allItems.length}`);
console.log(`  Items (equipment): ${allEquip.length}`);
console.log(`  Recipes: ${allRecipes.length}`);
console.log(`  Strategies: ${allStrategies.length}`);
