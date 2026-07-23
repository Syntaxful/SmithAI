package com.smithai.skills;

import com.smithai.SmithAIPlugin;
import com.smithai.npc.SmithNPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;

/**
 * Dispatches broad skill categories to concrete Bukkit actions.
 * Delegates to specialized managers for crafting, farming, mining, and endgame.
 */
public class SkillDispatcher {

    private final SmithAIPlugin plugin;
    private final CraftingManager crafting;
    private final FarmingManager farming;
    private final MiningManager mining;
    private final com.smithai.build.EndGameManager endgame;

    // Tracks ongoing timed block breaks
    private final Map<Location, UUID> activeBreaks = new HashMap<>();

    public SkillDispatcher(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.crafting = new CraftingManager(plugin);
        this.farming = new FarmingManager(plugin);
        this.mining = new MiningManager(plugin);
        this.endgame = new com.smithai.build.EndGameManager(plugin);
    }

    // Mob-specific combat tactics
    private static final Set<Material> HAZARD_BLOCKS = EnumSet.of(
        Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.CACTUS,
        Material.SWEET_BERRY_BUSH, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
        Material.MAGMA_BLOCK, Material.WITHER_ROSE
    );

    public void execute(SmithNPC npc, String skillName, Map<String, Object> params, Player contextPlayer) {
        if (npc == null || npc.getEntity() == null || npc.getEntity().isDead()) return;
        String lower = skillName.toLowerCase();

        // Precondition check
        String reason = checkPrecondition(npc, skillName);
        if (reason != null) {
            sendToast(contextPlayer, "§c⚠ Skill blocked: " + reason);
            if (contextPlayer != null) npc.sendMessage(contextPlayer, "I can't do that: " + reason);
            return;
        }

        // Enchanting
        if (lower.startsWith("enchant_") || lower.startsWith("enchant") || lower.startsWith("magic_")) {
            executeEnchant(npc, lower, contextPlayer, params); return;
        }

        // Water bucket clutch
        if (lower.startsWith("clutch_") || lower.startsWith("clutch") || lower.startsWith("water_clutch") || lower.startsWith("mlg")) {
            executeClutch(npc, lower, contextPlayer); return;
        }

        // Building
        if (lower.startsWith("build_") || lower.startsWith("construct_") || lower.startsWith("shelter") || lower.startsWith("wall") ||
            lower.startsWith("floor") || lower.startsWith("roof") || lower.startsWith("house_") || lower.startsWith("make_shelter")) {
            executeBuild(npc, lower, contextPlayer, params); return;
        }

        // Sleep / bed behavior
        if (lower.startsWith("sleep_") || lower.startsWith("sleep") || lower.startsWith("use_bed") || lower.startsWith("bed")) {
            executeSleep(npc, lower, contextPlayer); return;
        }

        // Chat / social
        if (isChatSkill(lower)) { executeChat(npc, lower, contextPlayer); return; }

        // Movement
        if (lower.startsWith("follow") || lower.equals("move_to_player") || lower.equals("teleport_to_player")) {
            if (contextPlayer != null) npc.follow(contextPlayer); return;
        }
        if (lower.startsWith("stay") || lower.equals("stay")) { npc.stay(); return; }
        if (lower.startsWith("teleport_to_spawn") || lower.equals("go_home") || lower.equals("teleport_home") || lower.equals("spawn")) {
            Location spawn = npc.getLocation() != null ? npc.getLocation().getWorld().getSpawnLocation() : null;
            if (spawn != null) npc.teleport(spawn); return;
        }
        if (lower.startsWith("look") || lower.startsWith("turn") || lower.startsWith("inspect") || lower.startsWith("check_")) {
            if (contextPlayer != null) npc.lookAt(contextPlayer.getLocation()); return;
        }
        if (lower.startsWith("move") || lower.startsWith("jump") || lower.startsWith("sneak") || lower.startsWith("sprint") ||
            lower.startsWith("climb") || lower.startsWith("swim") || lower.startsWith("explore_random") || lower.startsWith("wander") ||
            lower.startsWith("walk") || lower.startsWith("run") || lower.startsWith("go_to") || lower.startsWith("goto")) {
            executeMovement(npc, lower, contextPlayer, params); return;
        }

        // Basic interaction
        if (isInteractionSkill(lower)) { executeInteraction(npc, lower, contextPlayer, params); return; }

        // Combat / survival / attack
        if (isCombatSkill(lower)) {
            executeCombat(npc, lower, contextPlayer, params);
            // After combat, do smart inventory management
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> smartInventoryManagement(npc), 5L);
            return;
        }

        // Gather / resource / farming / crafting / building
        if (isResourceSkill(lower)) { executeResource(npc, lower, contextPlayer, params); return; }

        // Endgame / progression tasks
        if (isEndgameSkill(lower)) { endgame.execute(npc, lower, params, contextPlayer); return; }
        // Mining operations (safe strip, branch, diamond)
        if (isMiningSkill(lower)) { mining.execute(npc, lower, params, contextPlayer); return; }
        // Farming operations (plant, water, fertilize, harvest, breed)
        if (isFarmingSkill(lower)) { farming.execute(npc, lower, params, contextPlayer); return; }
        // Crafting / smelting / brewing / chest operations
        if (isCraftingSkill(lower)) { crafting.execute(npc, lower, params, contextPlayer); return; }

        executeComposite(npc, lower, contextPlayer, "I'll plan and execute that advanced objective: " + humanize(skillName));
    }

    /**
     * Send a toast/notification to the player via the action bar.
     */
    private void sendToast(Player player, String message) {
        if (player == null || !player.isOnline()) return;
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }

    /**
     * Show a toast for achievements/milestones (gold-colored with star prefix).
     */
    private void sendAchievementToast(Player player, String achievement) {
        sendToast(player, "§6✦ §e" + achievement);
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        }
    }

    // --- SKILL CATEGORIES ---

    private boolean isChatSkill(String lower) {
        return lower.startsWith("chat_") || lower.startsWith("say_") || lower.startsWith("ask_") || lower.startsWith("tell_") ||
            lower.startsWith("greet") || lower.startsWith("farewell") || lower.startsWith("thank") || lower.startsWith("apologize") ||
            lower.startsWith("compliment") || lower.startsWith("tease") || lower.startsWith("warn") || lower.startsWith("encourage") ||
            lower.startsWith("praise") || lower.startsWith("scold") || lower.startsWith("narrate") || lower.startsWith("describe") ||
            lower.startsWith("summarize") || lower.startsWith("translate") || lower.startsWith("repeat") || lower.startsWith("joke") ||
            lower.startsWith("explain") || lower.startsWith("suggest") || lower.startsWith("advise") || lower.startsWith("agree") ||
            lower.startsWith("disagree") || lower.startsWith("confirm") || lower.startsWith("deny") || lower.startsWith("offer_help") ||
            lower.startsWith("accept") || lower.startsWith("decline") || lower.startsWith("invite") || lower.startsWith("welcome") ||
            lower.startsWith("bid_farewell") || lower.startsWith("introduce") || lower.startsWith("state_") || lower.startsWith("set_mood") ||
            lower.startsWith("emote") || lower.startsWith("wave") || lower.startsWith("bow") || lower.startsWith("salute") ||
            lower.startsWith("dance") || lower.startsWith("pose") || lower.startsWith("gesture") || lower.startsWith("point") ||
            lower.startsWith("call") || lower.startsWith("whisper") || lower.startsWith("shout") || lower.startsWith("mourn") ||
            lower.startsWith("celebrate") || lower.startsWith("calm") || lower.startsWith("alert") || lower.startsWith("relax") ||
            lower.startsWith("focus") || lower.startsWith("remember") || lower.startsWith("recall") || lower.startsWith("forget") ||
            lower.startsWith("list_memory") || lower.startsWith("report_status") || lower.startsWith("report_location") || lower.startsWith("think") ||
            lower.startsWith("noop") || lower.startsWith("wait");
    }

    private boolean isInteractionSkill(String lower) {
        return lower.startsWith("use") || lower.startsWith("interact") || lower.startsWith("open") || lower.startsWith("close") ||
            lower.startsWith("equip") || lower.startsWith("select") || lower.startsWith("pick_up") || lower.startsWith("drop") ||
            lower.startsWith("sleep") || lower.startsWith("wake") || lower.startsWith("eat") || lower.startsWith("heal") ||
            lower.startsWith("avoid") || lower.startsWith("retreat") || lower.startsWith("set_home") || lower.startsWith("save_favorite") ||
            lower.startsWith("load_favorite") || lower.startsWith("clear_favorite") || lower.startsWith("set_nickname") || lower.startsWith("get_nickname") ||
            lower.startsWith("begin_task") || lower.startsWith("end_task") || lower.startsWith("pause_task") || lower.startsWith("resume_task") ||
            lower.startsWith("cancel_task") || lower.startsWith("retry_task") || lower.startsWith("skip_step") || lower.startsWith("repeat_step") ||
            lower.startsWith("mark_done") || lower.startsWith("mark_failed") || lower.startsWith("place_torch") || lower.startsWith("strip_mine_safe") ||
            lower.startsWith("place_") || lower.startsWith("break_") || lower.startsWith("mine_") || lower.startsWith("chop_") || lower.startsWith("dig_") ||
            lower.startsWith("farm_") || lower.startsWith("plant_") || lower.startsWith("water_") || lower.startsWith("harvest_") || lower.startsWith("replant_") ||
            lower.startsWith("scan_inventory") || lower.startsWith("inventory_scan") || lower.startsWith("collect_all") ||
            lower.startsWith("drop_all") || lower.startsWith("craft_") || lower.startsWith("make_") || lower.startsWith("smelt_");
    }

    private boolean isCombatSkill(String lower) {
        return lower.startsWith("attack") || lower.startsWith("defend") || lower.startsWith("fight_") || lower.startsWith("hunt_") ||
            lower.startsWith("auto_equip") || lower.startsWith("auto_eat") || lower.startsWith("auto_heal") || lower.startsWith("retreat_from_combat") ||
            lower.startsWith("ambush") || lower.startsWith("block") || lower.startsWith("dodge") || lower.startsWith("counter") ||
            lower.startsWith("raid_") || lower.startsWith("siege_") || lower.startsWith("storm_") || lower.startsWith("invade_") ||
            lower.startsWith("fortify_") || lower.startsWith("guard_") || lower.startsWith("patrol_") || lower.startsWith("protect_") ||
            lower.startsWith("suppress_") || lower.startsWith("secure_") || lower.startsWith("track_") || lower.startsWith("stalk_") ||
            lower.startsWith("tame_") || lower.startsWith("befriend") || lower.startsWith("recruit") || lower.startsWith("train_elite");
    }

    private boolean isResourceSkill(String lower) {
        return lower.startsWith("gather_") || lower.startsWith("collect_") || lower.startsWith("harvest_") || lower.startsWith("mine_") ||
            lower.startsWith("chop_") || lower.startsWith("dig_") || lower.startsWith("quarry_") || lower.startsWith("excavate_") ||
            lower.startsWith("farm_") || lower.startsWith("fish") || lower.startsWith("hunt_") || lower.startsWith("shear_") ||
            lower.startsWith("milk_") || lower.startsWith("pick_") || lower.startsWith("forage_") || lower.startsWith("scavenge_") ||
            lower.startsWith("salvage_") || lower.startsWith("loot_") || lower.startsWith("find_") || lower.startsWith("obtain_") ||
            lower.startsWith("craft_") || lower.startsWith("make_") || lower.startsWith("build_") || lower.startsWith("assemble_") ||
            lower.startsWith("forge_") || lower.startsWith("smelt_") || lower.startsWith("brew_") || lower.startsWith("cook_") ||
            lower.startsWith("bake_") || lower.startsWith("repair_") || lower.startsWith("place_") || lower.startsWith("remove_") ||
            lower.startsWith("decorate_") || lower.startsWith("terraform_") || lower.startsWith("light_") || lower.startsWith("secure_") ||
            lower.startsWith("expand_") || lower.startsWith("shrink_") || lower.startsWith("relocate_") || lower.startsWith("plant_") ||
            lower.startsWith("sow_") || lower.startsWith("water_") || lower.startsWith("fertilize_") || lower.startsWith("breed_") ||
            lower.startsWith("feed_") || lower.startsWith("grow_") || lower.startsWith("prune_") || lower.startsWith("replant_") ||
            lower.startsWith("explore_") || lower.startsWith("locate_") || lower.startsWith("scout_") || lower.startsWith("map_") ||
            lower.startsWith("navigate_") || lower.startsWith("search_") || lower.startsWith("survey_") || lower.startsWith("discover_") ||
            lower.startsWith("travel_") || lower.startsWith("venture_") || lower.startsWith("store_") || lower.startsWith("sort_") ||
            lower.startsWith("organize_") || lower.startsWith("deposit_") || lower.startsWith("withdraw_") || lower.startsWith("stack_") ||
            lower.startsWith("transfer_") || lower.startsWith("stock_") || lower.startsWith("manage_") || lower.startsWith("fuel_") ||
            lower.startsWith("configure_") || lower.startsWith("prepare_") || lower.startsWith("use_");
    }

    // --- CHAT ---

    private void executeChat(SmithNPC npc, String skill, Player player) {
        String reply = generateChatReply(skill, player);
        if (player != null) npc.sendMessage(player, reply);
        else npc.sendMessageToAll(reply);
    }

    private String generateChatReply(String skill, Player player) {
        String name = player != null ? player.getName() : "there";
        if (skill.contains("greet") || skill.contains("hello") || skill.contains("say_hello")) return "Hello, " + name + "!";
        if (skill.contains("farewell") || skill.contains("goodbye") || skill.contains("say_goodbye")) return "See you later, " + name + "!";
        if (skill.contains("thank") || skill.contains("say_thanks")) return "You're welcome!";
        if (skill.contains("apologize") || skill.contains("sorry") || skill.contains("say_sorry")) return "No worries.";
        if (skill.contains("joke") || skill.contains("tell_joke")) return "Why did the creeper cross the road? To get to the other side... boom!";
        if (skill.contains("warn")) return "Be careful!";
        if (skill.contains("encourage")) return "You can do it!";
        if (skill.contains("praise") || skill.contains("compliment")) return "Great job!";
        if (skill.contains("status")) return "I'm ready to help.";
        if (skill.contains("location")) return "I'm right here.";
        return "Got it.";
    }

    // --- MOVEMENT ---

    private void executeMovement(SmithNPC npc, String skill, Player player, Map<String, Object> params) {
        Location target = null;
        if (player != null && (skill.contains("player") || skill.contains("follow"))) {
            target = player.getLocation();
        } else if (params != null && params.containsKey("x") && params.containsKey("z")) {
            try {
                double x = ((Number) params.get("x")).doubleValue();
                double z = ((Number) params.get("z")).doubleValue();
                double y = params.containsKey("y") ? ((Number) params.get("y")).doubleValue() : npc.getLocation().getY();
                target = new Location(npc.getLocation().getWorld(), x, y, z);
            } catch (Exception ignored) {}
        }
        if (skill.contains("jump")) { npc.jump(); return; }
        if (target != null) { npc.setMoveTarget(target); return; }
        if (player != null) npc.lookAt(player.getLocation());
    }

    // --- INTERACTION ---

    private void executeSleep(SmithNPC npc, String skill, Player contextPlayer) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;

        // Find a nearby bed
        Location loc = fake.getLocation();
        if (loc == null) return;
        Block bedBlock = null;
        for (Block b : getNearbyBlocks(loc, 4, b -> b.getType().name().contains("BED"))) {
            bedBlock = b;
            break;
        }
        if (bedBlock == null) {
            npc.sendMessage(contextPlayer, "No bed nearby to sleep in.");
            return;
        }

        // Set bed spawn point and make player lie down
        fake.setBedSpawnLocation(bedBlock.getLocation(), true);
        fake.sleep(bedBlock.getLocation(), true);
        npc.sendMessage(contextPlayer, "Good night! Sleeping until morning.");
        sendAchievementToast(contextPlayer, "Smith_AI went to sleep!");

        // Schedule wake up after a short time
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!fake.isDead()) {
                fake.wakeup(true);
                npc.sendMessage(contextPlayer, "Good morning! Ready to work.");
            }
        }, 100L); // ~5 seconds
    }

    private void executeInteraction(SmithNPC npc, String skill, Player player, Map<String, Object> params) {
        // --- Inventory automation ---
        if (skill.startsWith("scan_inventory") || skill.startsWith("inventory_scan")) {
            scanInventory(npc, player);
            return;
        }
        if (skill.startsWith("collect_all") || skill.startsWith("pick_up_all")) {
            collectAllDrops(npc);
            return;
        }
        if (skill.startsWith("drop_all") || skill.startsWith("drop_everything")) {
            dropAllItems(npc);
            return;
        }
        // --- Simple crafting (stub that reports what's needed) ---
        if (skill.startsWith("craft_") || skill.startsWith("make_") || skill.startsWith("smelt_")) {
            simpleCraft(npc, player, skill, params);
            return;
        }

        if (skill.startsWith("eat") || skill.startsWith("heal") || skill.startsWith("consume") || skill.startsWith("use_item")) {
            useItem(npc, skill, params);
            return;
        }
        if (skill.startsWith("equip") || skill.startsWith("select")) {
            selectBestTool(npc, skill);
            return;
        }
        if (skill.startsWith("drop_") || skill.startsWith("throw_")) {
            dropItem(npc, params);
            return;
        }
        if (skill.startsWith("pick_up") || skill.startsWith("collect") || skill.startsWith("loot_")) {
            pickUpItems(npc, params);
            return;
        }
        if (skill.contains("place_torch")) {
            placeTorch(npc);
            return;
        }
        if (skill.contains("place_") && params != null && params.containsKey("material")) {
            placeBlock(npc, params);
            return;
        }
        if (skill.contains("break_") || skill.contains("mine_") || skill.contains("chop_") || skill.contains("dig_")) {
            breakBlockTimed(npc, skill, params);
            return;
        }
        npc.sendMessage(player, "Working on: " + humanize(skill));
    }

    // --- INVENTORY AUTOMATION ---

    private void scanInventory(SmithNPC npc, Player player) {
        if (!npc.hasInventory()) { npc.sendMessage(player, "I don't have an inventory."); return; }
        org.bukkit.inventory.Inventory inv = npc.getInventory();
        if (inv == null) return;
        StringBuilder report = new StringBuilder("Inventory: ");
        boolean empty = true;
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                report.append(stack.getAmount()).append("x ").append(stack.getType().name().toLowerCase().replace("_", " ")).append(", ");
                empty = false;
            }
        }
        if (empty) report.append("empty");
        else report.setLength(report.length() - 2);
        npc.sendMessage(player, report.toString());
    }

    private void collectAllDrops(SmithNPC npc) {
        Location loc = npc.getLocation();
        if (loc == null) return;
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        int count = 0;
        for (Item drop : loc.getWorld().getEntitiesByClass(Item.class)) {
            if (drop == null || drop.isDead() || drop.getLocation().distanceSquared(loc) > 16) continue;
            ItemStack stack = drop.getItemStack();
            if (fake.getInventory().firstEmpty() >= 0 || fake.getInventory().contains(stack.getType())) {
                fake.getInventory().addItem(stack.clone());
                drop.remove();
                count += stack.getAmount();
            }
        }
        if (count > 0) npc.sendMessageToAll("Collected " + count + " items.");
    }

    private void dropAllItems(SmithNPC npc) {
        if (!npc.hasInventory()) return;
        org.bukkit.inventory.Inventory inv = npc.getInventory();
        if (inv == null) return;
        Location loc = npc.getLocation();
        if (loc == null) return;
        int count = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                loc.getWorld().dropItemNaturally(loc, stack.clone());
                count += stack.getAmount();
            }
        }
        inv.clear();
        if (count > 0) npc.sendMessageToAll("Dropped " + count + " items.");
    }

    private void simpleCraft(SmithNPC npc, Player player, String skill, Map<String, Object> params) {
        // For now, report what materials would be needed for common recipes
        String lower = skill.toLowerCase();
        if (lower.contains("crafting_table") || lower.contains("workbench")) {
            npc.sendMessage(player, "To craft a crafting table: place 4 wooden planks in a 2x2 grid. Check inventory for planks.");
        } else if (lower.contains("stick")) {
            npc.sendMessage(player, "To craft sticks: place 2 planks vertically in the crafting grid.");
        } else if (lower.contains("torch")) {
            npc.sendMessage(player, "To craft torches: place coal above a stick in the crafting grid.");
        } else if (lower.contains("furnace")) {
            npc.sendMessage(player, "To craft a furnace: fill the edge of the crafting grid with 8 cobblestone.");
        } else if (lower.contains("chest")) {
            npc.sendMessage(player, "To craft a chest: fill the edge of the crafting grid with 8 planks.");
        } else if (lower.contains("plank")) {
            npc.sendMessage(player, "To craft planks: place logs vertically in the crafting grid.");
        } else if (lower.contains("pickaxe")) {
            npc.sendMessage(player, "To craft a pickaxe: 3 [material] across the top, 2 sticks down the middle.");
        } else if (lower.contains("sword")) {
            npc.sendMessage(player, "To craft a sword: 2 [material] vertically, 1 stick below.");
        } else if (lower.contains("axe")) {
            npc.sendMessage(player, "To craft an axe: 3 [material] in corner + side, 2 sticks down the right column.");
        } else {
            npc.sendMessage(player, "I need materials to craft that. Check the recipe in the knowledge base.");
        }
    }

    // --- TOOL SELECTION ---

    private void selectBestTool(SmithNPC npc, String skill) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();
        Material preferred = Material.AIR;
        if (skill.contains("pickaxe") || skill.contains("mine") || skill.contains("dig") || skill.contains("break_stone") || skill.contains("ore") || skill.contains("obsidian")) {
            preferred = findBestDurable(inv, Material.DIAMOND_PICKAXE, Material.IRON_PICKAXE, Material.STONE_PICKAXE, Material.WOODEN_PICKAXE);
        } else if (skill.contains("axe") || skill.contains("chop") || skill.contains("break_wood") || skill.contains("log")) {
            preferred = findBestDurable(inv, Material.DIAMOND_AXE, Material.IRON_AXE, Material.STONE_AXE, Material.WOODEN_AXE);
        } else if (skill.contains("sword") || skill.contains("fight") || skill.contains("attack") || skill.contains("combat") || skill.contains("hostile") || skill.contains("mob")) {
            preferred = findBestDurable(inv, Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.STONE_SWORD, Material.WOODEN_SWORD);
        } else if (skill.contains("shovel") || skill.contains("dirt") || skill.contains("sand") || skill.contains("gravel") || skill.contains("soil")) {
            preferred = findBestDurable(inv, Material.DIAMOND_SHOVEL, Material.IRON_SHOVEL, Material.STONE_SHOVEL, Material.WOODEN_SHOVEL);
        }
        if (preferred != Material.AIR && inv.contains(preferred)) {
            int slot = inv.first(preferred);
            if (slot >= 0 && slot < 9) inv.setHeldItemSlot(slot);
        }
    }

    /**
     * Durability-aware tool selection: picks the highest-tier tool that still has durability remaining.
     */
    private Material findBestDurable(PlayerInventory inv, Material best, Material good, Material ok, Material fallback) {
        for (Material m : new Material[]{best, good, ok, fallback}) {
            if (!inv.contains(m)) continue;
            // Check durability of each matching item
            int first = inv.first(m);
            if (first < 0) continue;
            ItemStack stack = inv.getItem(first);
            if (stack != null && stack.getType() == m) {
                org.bukkit.inventory.meta.Damageable dmg = null;
                if (stack.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable) {
                    dmg = (org.bukkit.inventory.meta.Damageable) stack.getItemMeta();
                }
                // If undamageable or still has more than 10% durability, use it
                if (dmg == null || dmg.getDamage() < stack.getType().getMaxDurability() * 0.9) {
                    return m;
                }
                // If damaged, check if we have another of same type
                for (int i = first + 1; i < inv.getSize(); i++) {
                    ItemStack alt = inv.getItem(i);
                    if (alt != null && alt.getType() == m) {
                        org.bukkit.inventory.meta.Damageable altDmg = null;
                        if (alt.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable) {
                            altDmg = (org.bukkit.inventory.meta.Damageable) alt.getItemMeta();
                        }
                        if (altDmg == null || altDmg.getDamage() < alt.getType().getMaxDurability() * 0.9) {
                            return m;
                        }
                    }
                }
            }
        }
        return fallback; // last resort
    }

    // --- TORCH / BLOCK PLACING ---

    private void placeTorch(SmithNPC npc) {
        Location loc = npc.getLocation();
        if (loc == null) return;
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();
        if (!inv.contains(Material.TORCH)) return;
        Block target = loc.getBlock();
        Block placeOn = target.getRelative(BlockFace.DOWN);
        if (placeOn.getType().isSolid() && target.getType().isAir()) {
            target.setType(Material.TORCH);
            removeOne(fake, Material.TORCH);
        }
    }

    private void placeBlock(SmithNPC npc, Map<String, Object> params) {
        Location loc = npc.getLocation();
        if (loc == null) return;
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        String matName = String.valueOf(params.get("material")).toUpperCase();
        Material mat = Material.matchMaterial(matName);
        if (mat == null || !mat.isBlock()) mat = Material.DIRT;
        PlayerInventory inv = fake.getInventory();
        if (!inv.contains(mat)) return;
        Block target = loc.getBlock();
        if (target.getType().isAir()) {
            target.setType(mat);
            removeOne(fake, mat);
        }
    }

    // --- TIMED BLOCK BREAKING WITH DROP COLLECTION ---

    private void breakBlockTimed(SmithNPC npc, String skill, Map<String, Object> params) {
        Location loc = npc.getLocation();
        if (loc == null) return;
        Block target = loc.getBlock();
        if (params != null && params.containsKey("x") && params.containsKey("y") && params.containsKey("z")) {
            try {
                int x = ((Number) params.get("x")).intValue();
                int y = ((Number) params.get("y")).intValue();
                int z = ((Number) params.get("z")).intValue();
                target = loc.getWorld().getBlockAt(x, y, z);
            } catch (Exception ignored) {}
        }
        Material type = target.getType();
        if (type == Material.AIR || type == Material.BEDROCK || type == Material.LAVA || type == Material.FIRE) return;
        if (type == Material.WATER) return;

        // Capture final copy for inner class
        final Block breakBlock = target;
        // Calculate break time based on tool and block hardness
        int breakTime = getBreakTime(npc, breakBlock, skill);
        selectBestTool(npc, skill);
        Location finalTarget = breakBlock.getLocation();

        // Show block damage animation via scheduled task
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;

        // Schedule the timed break
        new BukkitRunnable() {
            int elapsed = 0;
            @Override
            public void run() {
                elapsed++;
                if (elapsed >= breakTime || fake.isDead() || !fake.isOnline() || !finalTarget.getBlock().equals(breakBlock)) {
                    if (elapsed >= breakTime && !fake.isDead() && fake.isOnline()) {
                        // Actually break the block
                        Block b = finalTarget.getBlock();
                        if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK) {
                            b.breakNaturally();
                            // Collect dropped items
                            collectDropsAt(fake, finalTarget, 3);
                        }
                    }
                    activeBreaks.remove(finalTarget);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private int getBreakTime(SmithNPC npc, Block block, String skill) {
        Material type = block.getType();
        String name = type.name().toLowerCase();

        // Base hardness tiers (in ticks, 20 ticks = 1 second)
        if (name.contains("obsidian")) return 100; // ~5 seconds
        if (name.contains("deepslate") || name.contains("ancient_debris")) return 40;
        if (name.contains("diamond_ore") || name.contains("emerald_ore") || name.contains("netherite")) return 30;
        if (name.contains("iron_ore") || name.contains("gold_ore") || name.contains("redstone_ore") || name.contains("lapis")) return 20;
        if (name.contains("stone") || name.contains("cobble") || name.contains("andesite") || name.contains("diorite") || name.contains("granite") || name.contains("basalt")) return 15;
        if (name.contains("coal_ore") || name.contains("copper_ore") || name.contains("netherrack")) return 10;
        if (name.contains("log") || name.contains("wood") || name.contains("plank")) return 12;
        if (name.contains("dirt") || name.contains("sand") || name.contains("gravel") || name.contains("clay")) return 5;
        if (name.contains("glass") || name.contains("ice") || name.contains("torch")) return 3;
        return 15; // default
    }

    private void collectDropsAt(Player player, Location location, double radius) {
        double r2 = radius * radius;
        for (Item drop : location.getWorld().getEntitiesByClass(Item.class)) {
            if (drop == null || drop.isDead() || drop.getLocation().distanceSquared(location) > r2) continue;
            ItemStack stack = drop.getItemStack();
            if (player.getInventory().firstEmpty() >= 0 || player.getInventory().contains(stack.getType())) {
                player.getInventory().addItem(stack.clone());
                drop.remove();
            }
        }
    }

    private void removeOne(Player player, Material mat) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == mat) {
                if (stack.getAmount() <= 1) inv.setItem(i, null);
                else { stack.setAmount(stack.getAmount() - 1); inv.setItem(i, stack); }
                return;
            }
        }
    }

    // --- ITEM USE / FOOD MANAGEMENT ---

    private void useItem(SmithNPC npc, String skill, Map<String, Object> params) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();
        Material mat = Material.AIR;
        if (params != null && params.containsKey("material")) {
            mat = Material.matchMaterial(String.valueOf(params.get("material")).toUpperCase());
        }
        if (mat == null || mat == Material.AIR) {
            mat = findBestFood(inv);
        }
        if (mat == null || mat == Material.AIR || !inv.contains(mat)) return;
        if (mat.isEdible() && entity instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) entity;
            le.setHealth(Math.min(le.getHealth() + 4, le.getMaxHealth()));
            removeOne(fake, mat);
        } else {
            removeOne(fake, mat);
        }
    }

    /**
     * Food/hunger management: find best food in inventory.
     * Prioritizes golden apples (heal) > cooked meats > bread > vegetables.
     */
    private Material findBestFood(PlayerInventory inv) {
        Material[][] tiers = {
            { Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE },
            { Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_MUTTON, Material.COOKED_CHICKEN, Material.COOKED_RABBIT, Material.BAKED_POTATO },
            { Material.BREAD, Material.COOKED_COD, Material.COOKED_SALMON, Material.MUSHROOM_STEW, Material.BEETROOT_SOUP, Material.RABBIT_STEW, Material.PUMPKIN_PIE },
            { Material.APPLE, Material.CARROT, Material.BAKED_POTATO, Material.BREAD, Material.COOKIE, Material.DRIED_KELP, Material.SWEET_BERRIES, Material.GLOW_BERRIES },
        };
        for (Material[] tier : tiers) {
            for (Material m : tier) {
                if (inv.contains(m)) return m;
            }
        }
        return Material.AIR;
    }

    /**
     * Auto-feed when hunger is low. Called automatically from combat and movement.
     */
    private void autoEatIfNeeded(SmithNPC npc) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        if (fake.getFoodLevel() >= 20) return; // full
        PlayerInventory inv = fake.getInventory();
        Material food = findBestFood(inv);
        if (food == Material.AIR || food == null) return;
        removeOne(fake, food);
        fake.setFoodLevel(Math.min(fake.getFoodLevel() + 6, 20));
        fake.setSaturation(Math.min(fake.getSaturation() + 3, 10));
    }

    // --- DROP ITEM ---

    private void dropItem(SmithNPC npc, Map<String, Object> params) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();
        Material mat = Material.AIR;
        if (params != null && params.containsKey("material")) {
            mat = Material.matchMaterial(String.valueOf(params.get("material")).toUpperCase());
        }
        if (mat == null || mat == Material.AIR) {
            ItemStack held = inv.getItemInMainHand();
            if (held != null && held.getType() != Material.AIR) {
                fake.getWorld().dropItemNaturally(fake.getLocation(), held.clone());
                inv.setItemInMainHand(null);
            }
            return;
        }
        if (!inv.contains(mat)) return;
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() == mat) {
                fake.getWorld().dropItemNaturally(fake.getLocation(), stack.clone());
                inv.remove(stack);
                return;
            }
        }
    }

    // --- RESOURCE STOCKPILING & RESTOCKING ---

    /**
     * Stockpile resources: move all matching items from inventory into a nearby chest.
     */
    private void stockpileResources(SmithNPC npc, Player player, Material itemType, int keepAmount) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();

        // Count how many we have
        int total = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() == itemType) total += stack.getAmount();
        }
        int toStore = total - keepAmount;
        if (toStore <= 0) {
            npc.sendMessage(player, "Not enough " + itemType.name().toLowerCase().replace("_", " ") + " to stockpile (have " + total + ", keeping " + keepAmount + ").");
            return;
        }

        // Find nearby chest
        Location loc = npc.getLocation();
        if (loc == null) return;
        org.bukkit.block.Chest chest = null;
        double bestDist = 25;
        for (Block block : getNearbyBlocks(loc, 5, b -> b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST)) {
            double d = block.getLocation().distanceSquared(loc);
            if (d < bestDist) {
                bestDist = d;
                if (block.getState() instanceof org.bukkit.block.Chest) {
                    chest = (org.bukkit.block.Chest) block.getState();
                }
            }
        }

        if (chest == null) {
            npc.sendMessage(player, "No chest nearby to stockpile into.");
            return;
        }

        org.bukkit.inventory.Inventory chestInv = chest.getBlockInventory();
        int stored = 0;
        for (int i = 0; i < inv.getSize() && toStore > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && stack.getType() == itemType) {
                int moveAmount = Math.min(stack.getAmount(), toStore);
                ItemStack toAdd = stack.clone();
                toAdd.setAmount(moveAmount);
                java.util.HashMap<Integer, ItemStack> remaining = chestInv.addItem(toAdd);
                int placed = moveAmount;
                if (!remaining.isEmpty()) {
                    placed -= remaining.get(0).getAmount();
                }
                if (stack.getAmount() <= placed) {
                    inv.setItem(i, null);
                } else {
                    stack.setAmount(stack.getAmount() - placed);
                    inv.setItem(i, stack);
                }
                stored += placed;
                toStore -= placed;
            }
        }
        npc.sendMessage(player, "Stockpiled " + stored + " " + itemType.name().toLowerCase().replace("_", " ") + " into chest.");
    }

    /**
     * Get nearby blocks matching a condition.
     */
    private java.util.List<Block> getNearbyBlocks(Location loc, int radius, java.util.function.Predicate<Block> filter) {
        java.util.List<Block> results = new java.util.ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = loc.getBlock().getRelative(x, y, z);
                    if (filter.test(b)) results.add(b);
                }
            }
        }
        return results;
    }

    // --- PICK UP ITEMS ---

    private void pickUpItems(SmithNPC npc, Map<String, Object> params) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        Location loc = npc.getLocation();
        if (loc == null) return;
        double radius = 3.0;
        if (params != null && params.containsKey("radius")) {
            try { radius = ((Number) params.get("radius")).doubleValue(); } catch (Exception ignored) {}
        }
        Material filter = null;
        if (params != null && params.containsKey("material")) {
            filter = Material.matchMaterial(String.valueOf(params.get("material")).toUpperCase());
        }
        for (Item drop : loc.getWorld().getEntitiesByClass(Item.class)) {
            if (drop == null || drop.isDead() || drop.getLocation().distanceSquared(loc) > radius * radius) continue;
            ItemStack stack = drop.getItemStack();
            if (filter != null && stack.getType() != filter) continue;
            if (fake.getInventory().firstEmpty() >= 0 || fake.getInventory().contains(stack.getType())) {
                fake.getInventory().addItem(stack.clone());
                drop.remove();
            }
        }
    }

    // --- MOB-SPECIFIC COMBAT TACTICS + HAZARD AVOIDANCE ---

    private void executeCombat(SmithNPC npc, String skill, Player player, Map<String, Object> params) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof LivingEntity)) return;
        LivingEntity self = (LivingEntity) entity;

        // Auto-eat if food is low before combat
        if (self instanceof Player && ((Player) self).getFoodLevel() < 12) {
            autoEatIfNeeded(npc);
        }

        // Hazard avoidance: if standing in lava/fire/cactus, move away first
        if (avoidHazards(npc, self)) {
            npc.sendMessage(player, "Moving away from hazard first.");
            return;
        }

        LivingEntity target = null;
        String targetType = null;
        if (params != null && params.containsKey("target")) {
            targetType = String.valueOf(params.get("target")).toLowerCase();
            double bestDist = 64;
            for (Entity e : self.getWorld().getNearbyEntities(self.getLocation(), 8, 8, 8)) {
                if (e instanceof LivingEntity && e != self && !(e instanceof Player)) {
                    String type = e.getType().name().toLowerCase();
                    if (type.contains(targetType)) {
                        double d = e.getLocation().distanceSquared(self.getLocation());
                        if (d < bestDist) { bestDist = d; target = (LivingEntity) e; }
                    }
                }
            }
        }
        if (target == null) {
            target = findNearestHostile(self, 8);
            if (target != null) targetType = target.getType().name().toLowerCase();
        }

        if (target == null) {
            npc.sendMessage(player, "No hostile target found.");
            return;
        }

        selectBestTool(npc, "sword");
        equipBestArmor(npc);

        // Mob-specific tactics
        String mobName = target.getType().name().toLowerCase();
        boolean isRanged = mobName.contains("skeleton") || mobName.contains("pillager") || mobName.contains("stray") || mobName.contains("ghast") || mobName.contains("blaze") || mobName.contains("witch");
        boolean isExplosive = mobName.contains("creeper");
        boolean isFlying = mobName.contains("ghast") || mobName.contains("phantom") || mobName.contains("vex") || mobName.contains("blaze");
        boolean isBoss = mobName.contains("ender_dragon") || mobName.contains("wither") || mobName.contains("warden");
        boolean isNether = mobName.contains("blaze") || mobName.contains("ghast") || mobName.contains("piglin") || mobName.contains("hoglin") || mobName.contains("magma") || mobName.contains("wither_skeleton");

        if (isBoss) {
            // Boss fight: use ranged approach, retreat and heal
            self.attack(target);
            npc.sendMessage(player, "Fighting boss " + humanize(mobName) + "! Using ranged tactics and healing between hits.");
            // Auto-heal after boss attacks
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!self.isDead()) {
                    self.setHealth(Math.min(self.getHealth() + 2, self.getMaxHealth()));
                }
            }, 20L);
            return;
        }

        if (isExplosive) {
            // Creeper: attack and retreat to avoid explosion
            double dist = self.getLocation().distanceSquared(target.getLocation());
            if (dist < 9) {
                // Creeper is close - retreat first
                Location retreat = self.getLocation().add(self.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(3));
                self.teleport(retreat);
                npc.sendMessage(player, "Backing away from creeper to avoid explosion.");
            }
            // Attack from range with bow if available, otherwise hit and run
            self.attack(target);
            npc.sendMessage(player, "Hit-and-run against creeper.");
            return;
        }

        if (isRanged) {
            // Ranged mob: strafe and close distance
            self.attack(target);
            // Apply slowness if we have a weapon with it (placeholder - just attack)
            npc.sendMessage(player, "Closing in on " + humanize(mobName) + " with strafing.");
            return;
        }

        if (isFlying && !mobName.contains("blaze")) {
            // Flying mobs: use bow if possible
            self.attack(target);
            npc.sendMessage(player, "Attacking flying " + humanize(mobName) + ".");
            return;
        }

        // Default melee combat with dodging
        // Random dodge: 40% chance to strafe sideways before melee
        if (Math.random() < 0.4) {
            org.bukkit.util.Vector dir = self.getLocation().getDirection();
            org.bukkit.util.Vector strafe = new org.bukkit.util.Vector(-dir.getZ(), 0, dir.getX()).normalize();
            if (Math.random() < 0.5) strafe.multiply(-1);
            self.setVelocity(strafe.multiply(0.5));
        }

        // Blocking with shield: if shield in offhand, simulate shield usage
        if (self instanceof Player) {
            Player fake = (Player) self;
            ItemStack offhand = fake.getInventory().getItemInOffHand();
            if (offhand != null && offhand.getType() == Material.SHIELD) {
                fake.setWalkSpeed(0.1f); // Slow down while blocking
                fake.getWorld().playSound(fake.getLocation(), org.bukkit.Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
            }
        }

        // Use buff potions before combat
        useBuffPotions(npc);

        self.attack(target);
        String tacticDesc = "Attacking " + humanize(mobName) + " with dodge+block tactics.";
        if (isNether) tacticDesc = "Engaging " + humanize(mobName) + " in the Nether. Watch for fire.";
        npc.sendMessage(player, tacticDesc);
    }

    // --- BUFF POTIONS ---

    /**
     * Use beneficial potions from inventory before combat.
     */
    private void useBuffPotions(SmithNPC npc) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();

        // Check all inventory slots for potions
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null) continue;
            if (stack.getType() == Material.POTION || stack.getType() == Material.LINGERING_POTION || stack.getType() == Material.SPLASH_POTION) {
                PotionMeta meta = (PotionMeta) stack.getItemMeta();
                if (meta == null) continue;
                // Check for beneficial effects
                for (PotionEffect effect : meta.getCustomEffects()) {
                    if (isBeneficialEffect(effect.getType())) {
                        // Apply the effect by consuming the potion
                        fake.addPotionEffect(new PotionEffect(effect.getType(), effect.getDuration(), effect.getAmplifier()));
                        fake.getInventory().setItem(i, null);
                        break;
                    }
                }
                // Check base potion effects via item name/type clues
                String itemName = stack.getType().name().toLowerCase();
                if (itemName.contains("strength")) {
                    fake.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 3600, 0));
                    fake.getInventory().setItem(i, null);
                } else if (itemName.contains("swiftness") || itemName.contains("speed")) {
                    fake.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3600, 0));
                    fake.getInventory().setItem(i, null);
                } else if (itemName.contains("healing") || itemName.contains("health")) {
                    fake.setHealth(Math.min(fake.getHealth() + 4, fake.getMaxHealth()));
                    fake.getInventory().setItem(i, null);
                } else if (itemName.contains("fire_resistance")) {
                    fake.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 6000, 0));
                    fake.getInventory().setItem(i, null);
                } else if (itemName.contains("regeneration")) {
                    fake.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 3600, 0));
                    fake.getInventory().setItem(i, null);
                } else if (itemName.contains("invisibility")) {
                    fake.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 3600, 0));
                    fake.getInventory().setItem(i, null);
                } else if (itemName.contains("night_vision")) {
                    fake.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 6000, 0));
                    fake.getInventory().setItem(i, null);
                }
            }
        }
    }

    private boolean isBeneficialEffect(PotionEffectType type) {
        String n = type.getName().toLowerCase();
        return n.contains("strength") || n.contains("speed") || n.contains("regeneration") ||
               n.contains("fire_resistance") || n.contains("resistance") || n.contains("absorption") ||
               n.contains("health_boost") || n.contains("night_vision") || n.contains("invisibility");
    }

    /**
     * Checks if the NPC is standing in a hazard and moves away.
     * @return true if the NPC moved away from a hazard
     */
    private boolean avoidHazards(SmithNPC npc, LivingEntity self) {
        Location loc = self.getLocation();
        if (loc == null) return false;
        Block feet = loc.getBlock();
        Block below = feet.getRelative(BlockFace.DOWN);

        // Check for hazards at feet or below
        if (HAZARD_BLOCKS.contains(feet.getType()) || HAZARD_BLOCKS.contains(below.getType()) ||
            feet.getType() == Material.LAVA || below.getType() == Material.LAVA ||
            feet.getType() == Material.FIRE) {

            // Try to teleport to safe nearby spot
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Location candidate = loc.clone().add(dx, 0, dz);
                    Block candFeet = candidate.getBlock();
                    Block candBelow = candFeet.getRelative(BlockFace.DOWN);
                    if (candFeet.getType().isAir() && !candBelow.isPassable() && !HAZARD_BLOCKS.contains(candFeet.getType()) && !HAZARD_BLOCKS.contains(candBelow.getType())) {
                        self.teleport(candidate.add(0.5, 0, 0.5));
                        return true;
                    }
                }
            }
            // Emergency: teleport to spawn
            self.teleport(self.getWorld().getSpawnLocation());
            return true;
        }
        return false;
    }

    private LivingEntity findNearestHostile(LivingEntity self, double radius) {
        LivingEntity nearest = null;
        double best = radius * radius;
        Location loc = self.getLocation();

        // Prioritize targets closer to the player's facing direction
        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e instanceof LivingEntity && e != self && !(e instanceof Player)) {
                // Skip if it's a passive mob that's not being hunted
                String type = e.getType().name().toLowerCase();
                if (isPassiveMob(type)) continue;

                double d = e.getLocation().distanceSquared(loc);
                if (d < best) {
                    best = d;
                    nearest = (LivingEntity) e;
                }
            }
        }
        return nearest;
    }

    private boolean isPassiveMob(String type) {
        return type.contains("cow") || type.contains("pig") || type.contains("sheep") || type.contains("chicken") ||
            type.contains("rabbit") || type.contains("mooshroom") || type.contains("horse") || type.contains("donkey") ||
            type.contains("mule") || type.contains("llama") || type.contains("wolf") || type.contains("cat") ||
            type.contains("parrot") || type.contains("fox") || type.contains("bee") || type.contains("bat") ||
            type.contains("squid") || type.contains("dolphin") || type.contains("turtle") || type.contains("polar_bear") ||
            type.contains("panda") || type.contains("ocelot") || type.contains("villager") || type.contains("wandering_trader") ||
            type.contains("iron_golem") || type.contains("snow_golem");
    }

    // --- SMART INVENTORY MANAGEMENT ---

    /**
     * Smart inventory management: auto-upgrade armor/tools, drop inferior items.
     * Called automatically after combat and resource gathering.
     */
    private void smartInventoryManagement(SmithNPC npc) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();

        // 1. Upgrade armor (drop iron if you have diamond equipped now)
        equipBestArmor(npc);

        // 2. Remove inferior armor/tools from inventory (keep only best tier)
        Material[][] upgradeChains = {
            { Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.GOLDEN_HELMET, Material.IRON_HELMET, Material.DIAMOND_HELMET, Material.TURTLE_HELMET },
            { Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE },
            { Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.IRON_LEGGINGS, Material.DIAMOND_LEGGINGS },
            { Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.GOLDEN_BOOTS, Material.IRON_BOOTS, Material.DIAMOND_BOOTS },
        };
        for (Material[] chain : upgradeChains) {
            // Find the best tier we HAVE (not just equipped)
            Material bestWeHave = Material.AIR;
            for (int t = chain.length - 1; t >= 0; t--) {
                if (inv.contains(chain[t])) { bestWeHave = chain[t]; break; }
            }
            if (bestWeHave == Material.AIR) continue;
            // Remove all lower-tier items of this type
            for (Material worse : chain) {
                if (worse == bestWeHave) break; // stop at the best we have
                while (inv.contains(worse)) {
                    int idx = inv.first(worse);
                    if (idx >= 0) inv.setItem(idx, null);
                }
            }
        }

        // 3. Tool upgrade: keep only best pickaxe/axe/shovel/sword
        Material[][] toolChains = {
            { Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE },
            { Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE },
            { Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL },
            { Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD },
        };
        for (Material[] chain : toolChains) {
            Material bestWeHave = Material.AIR;
            for (int t = chain.length - 1; t >= 0; t--) {
                if (inv.contains(chain[t])) { bestWeHave = chain[t]; break; }
            }
            if (bestWeHave == Material.AIR) continue;
            for (Material worse : chain) {
                if (worse == bestWeHave) break;
                while (inv.contains(worse)) {
                    int idx = inv.first(worse);
                    if (idx >= 0) inv.setItem(idx, null);
                }
            }
        }
    }

    // --- ENCHANTING ---

    /**
     * Enchant the held item or a specified item using available XP and lapis.
     */
    private void executeEnchant(SmithNPC npc, String skill, Player contextPlayer, Map<String, Object> params) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;

        Material target = Material.AIR;
        if (params != null && params.containsKey("material")) {
            target = Material.matchMaterial(String.valueOf(params.get("material")).toUpperCase());
        }
        if (target == Material.AIR || target == null) {
            target = fake.getInventory().getItemInMainHand().getType();
        }
        if (target == Material.AIR || !isEnchantable(target)) {
            npc.sendMessage(contextPlayer, "I need an enchantable item in hand or specified.");
            return;
        }

        // Find enchanting table nearby
        Location loc = fake.getLocation();
        if (loc == null) return;
        Block table = null;
        for (Block b : getNearbyBlocks(loc, 5, b -> b.getType() == Material.ENCHANTING_TABLE)) {
            table = b; break;
        }
        if (table == null) {
            npc.sendMessage(contextPlayer, "I need an enchanting table nearby.");
            return;
        }

        // Open enchantment table and apply
        fake.openEnchanting(table.getLocation(), true);
        npc.sendMessage(contextPlayer, "Opened enchanting table. Place your item and select an enchantment.");
        sendAchievementToast(contextPlayer, "Enchanting ready!");
    }

    private boolean isEnchantable(Material mat) {
        String n = mat.name();
        return n.contains("SWORD") || n.contains("PICKAXE") || n.contains("AXE") || n.contains("SHOVEL") ||
               n.contains("HOE") || n.contains("HELMET") || n.contains("CHESTPLATE") ||
               n.contains("LEGGINGS") || n.contains("BOOTS") || n.contains("BOW") ||
               n.contains("CROSSBOW") || n.contains("TRIDENT") || n.contains("FISHING_ROD") ||
               n.contains("ELYTRA");
    }

    // --- WATER BUCKET CLUTCHING ---

    /**
     * Water bucket clutch: place water below to negate fall damage.
     */
    private void executeClutch(SmithNPC npc, String skill, Player contextPlayer) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();

        if (!inv.contains(Material.WATER_BUCKET)) {
            npc.sendMessage(contextPlayer, "I need a water bucket to clutch!");
            return;
        }

        Location loc = fake.getLocation();
        if (loc == null) return;
        Block below = loc.getBlock().getRelative(BlockFace.DOWN);

        // Place water below
        below.setType(Material.WATER);
        fake.setFallDistance(0);
        inv.remove(Material.WATER_BUCKET);
        // Place the empty bucket back
        inv.addItem(new ItemStack(Material.BUCKET, 1));

        npc.sendMessage(contextPlayer, "Water clutch deployed! Safe landing.");
        sendAchievementToast(contextPlayer, "Water Bucket Clutch!");
    }

    // --- BUILDING ---

    /**
     * Build a simple structure: wall, floor, or shelter.
     */
    private void executeBuild(SmithNPC npc, String skill, Player contextPlayer, Map<String, Object> params) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();

        // Determine build material
        Material blockMat = Material.AIR;
        if (params != null && params.containsKey("material")) {
            blockMat = Material.matchMaterial(String.valueOf(params.get("material")).toUpperCase());
        }
        if (blockMat == Material.AIR || blockMat == null) {
            // Auto-detect best building block
            Material[] candidates = { Material.STONE, Material.COBBLESTONE, Material.OAK_PLANKS, Material.DIRT, Material.OAK_LOG };
            for (Material m : candidates) {
                if (inv.contains(m)) { blockMat = m; break; }
            }
        }
        if (blockMat == Material.AIR || !inv.contains(blockMat)) {
            npc.sendMessage(contextPlayer, "I need building materials.");
            return;
        }

        Location base = fake.getLocation();
        if (base == null) return;

        // Build a small shelter (3x3 floor, 3 high walls, roof)
        int blocksPlaced = 0;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Floor at y=0 (feet level)
                Block floorBlock = base.clone().add(x, -1, z).getBlock();
                if (floorBlock.getType() == Material.AIR && inv.contains(blockMat)) {
                    floorBlock.setType(blockMat);
                    removeOne(fake, blockMat);
                    blocksPlaced++;
                }
                // Walls at y=0,1,2
                for (int y = 0; y <= 2; y++) {
                    boolean isEdge = Math.abs(x) == 2 || Math.abs(z) == 2;
                    if (!isEdge) continue;
                    Block wallBlock = base.clone().add(x, y, z).getBlock();
                    if (wallBlock.getType() == Material.AIR && inv.contains(blockMat)) {
                        wallBlock.setType(blockMat);
                        removeOne(fake, blockMat);
                        blocksPlaced++;
                    }
                }
                // Roof at y=3
                if (inv.contains(blockMat)) {
                    Block roofBlock = base.clone().add(x, 3, z).getBlock();
                    if (roofBlock.getType() == Material.AIR) {
                        roofBlock.setType(blockMat);
                        removeOne(fake, blockMat);
                        blocksPlaced++;
                    }
                }
            }
        }
        npc.sendMessage(contextPlayer, "Built a shelter (" + blocksPlaced + " blocks).");
        if (blocksPlaced > 0) sendAchievementToast(contextPlayer, "Built a shelter!");
    }

    // --- ARMOR EQUIPPING ---

    /**
     * Equip the best available armor from inventory.
     */
    private void equipBestArmor(SmithNPC npc) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();

        // Armor priorities: helmet, chestplate, leggings, boots
        Material[][] armorTiers = {
            { Material.DIAMOND_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.CHAINMAIL_HELMET, Material.LEATHER_HELMET, Material.TURTLE_HELMET },
            { Material.DIAMOND_CHESTPLATE, Material.IRON_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.LEATHER_CHESTPLATE },
            { Material.DIAMOND_LEGGINGS, Material.IRON_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.LEATHER_LEGGINGS },
            { Material.DIAMOND_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.CHAINMAIL_BOOTS, Material.LEATHER_BOOTS },
        };

        for (int slot = 0; slot < 4; slot++) {
            org.bukkit.inventory.ItemStack current = inv.getArmorContents()[slot];
            if (current != null && current.getType() != Material.AIR) continue; // already armored
            for (Material tier : armorTiers[slot]) {
                if (!inv.contains(tier)) continue;
                int idx = inv.first(tier);
                if (idx < 0) continue;
                org.bukkit.inventory.ItemStack stack = inv.getItem(idx);
                if (stack == null) continue;
                // Check durability
                if (stack.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable) {
                    org.bukkit.inventory.meta.Damageable dmg = (org.bukkit.inventory.meta.Damageable) stack.getItemMeta();
                    if (dmg.getDamage() > tier.getMaxDurability() * 0.85) continue;
                }
                // Equip using slot-specific setters
                switch (slot) {
                    case 0: inv.setHelmet(stack); break;
                    case 1: inv.setChestplate(stack); break;
                    case 2: inv.setLeggings(stack); break;
                    case 3: inv.setBoots(stack); break;
                }
                inv.setItem(idx, null);
                break;
            }
        }
    }

    // --- RESOURCE SKILLS ---

    private void executeResource(SmithNPC npc, String skill, Player player, Map<String, Object> params) {
        if (skill.contains("place_torch") || skill.contains("light_") || skill.contains("torch")) {
            placeTorch(npc); return;
        }
        if (skill.contains("break_") || skill.contains("mine_") || skill.contains("chop_") || skill.contains("dig_") ||
            skill.contains("harvest_") || skill.contains("gather_wood") || skill.contains("gather_stone") || skill.contains("gather_ore") ||
            skill.contains("gather_") || skill.contains("collect_")) {
            breakBlockTimed(npc, skill, params); return;
        }
        if (skill.contains("place_") || skill.contains("build_") || skill.contains("expand_") || skill.contains("make_") || skill.contains("wall")) {
            placeBlock(npc, params); return;
        }
        if (skill.contains("scan_inventory") || skill.contains("collect_all")) {
            if (skill.contains("collect_all") || skill.contains("pick_up_all")) { collectAllDrops(npc); return; }
            scanInventory(npc, player); return;
        }
        executeComposite(npc, skill, player, "I'll work on that task: " + humanize(skill));
    }

    private void executeComposite(SmithNPC npc, String skill, Player player, String defaultMessage) {
        npc.sendMessage(player, defaultMessage);
    }

    // ── SKILL PRECONDITIONS ──

    /**
     * Check if a skill can be executed given current conditions.
     * Returns null if OK, or a reason string if blocked.
     */
    private String checkPrecondition(SmithNPC npc, String skill) {
        Entity entity = npc.getEntity();
        if (entity == null) return "No entity";
        Location loc = entity.getLocation();
        if (loc == null) return "No location";

        // Health check: if skill requires work and health is very low, block
        if (entity instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) entity;
            double health = le.getHealth();
            if (health < 4.0 && (skill.contains("mine_") || skill.contains("fight_") || skill.contains("build_"))) {
                return "Health too low (" + String.format("%.1f", health) + ")";
            }
        }

        // Tool check: if skill needs a specific tool, verify
        if (skill.contains("mine_") || skill.contains("break_")) {
            if (entity instanceof Player) {
                Player fake = (Player) entity;
                org.bukkit.inventory.ItemStack held = fake.getInventory().getItemInMainHand();
                boolean hasPickaxe = held != null && (held.getType().name().contains("PICKAXE") ||
                    held.getType().name().contains("SHOVEL") || held.getType().name().contains("AXE"));
                if (!hasPickaxe && !held.getType().name().contains("SWORD")) {
                    // Check if any tool is in inventory
                    boolean hasAnyTool = false;
                    for (ItemStack stack : fake.getInventory().getContents()) {
                        if (stack != null) {
                            String n = stack.getType().name();
                            if (n.contains("PICKAXE") || n.contains("AXE") || n.contains("SHOVEL")) {
                                hasAnyTool = true; break;
                            }
                        }
                    }
                    if (!hasAnyTool) {
                        return "No suitable tool available";
                    }
                }
            }
        }

        // Food check for combat
        if (skill.contains("fight_") || skill.contains("combat_")) {
            if (entity instanceof Player) {
                Player fake = (Player) entity;
                if (fake.getFoodLevel() < 6 && !invContainsFood(fake)) {
                    return "No food available for combat";
                }
            }
        }

        return null; // All good
    }

    /**
     * Check if inventory has any edible items.
     */
    private boolean invContainsFood(Player player) {
        Material[] foods = { Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN, Material.BREAD,
            Material.COOKED_MUTTON, Material.COOKED_RABBIT, Material.BAKED_POTATO, Material.GOLDEN_APPLE,
            Material.APPLE, Material.CARROT, Material.COOKED_COD, Material.COOKED_SALMON };
        for (Material f : foods) {
            if (player.getInventory().contains(f)) return true;
        }
        return false;
    }

    // ── NEW SKILL DETECTORS ──

    private boolean isEndgameSkill(String lower) {
        return lower.contains("portal") || lower.contains("nether") || lower.contains("blaze") ||
            lower.contains("eye_of_ender") || lower.contains("ender_eye") || lower.contains("stronghold") ||
            lower.contains("end_portal") || lower.contains("dragon") || lower.contains("wither") ||
            lower.contains("elytra") || lower.contains("shulker") || lower.contains("end_city");
    }

    private boolean isMiningSkill(String lower) {
        return lower.contains("strip_mine") || lower.contains("branch_mine") || lower.contains("mine_diamond") ||
            lower.contains("find_diamond") || lower.contains("diamond_mine") || lower.contains("ladder_down") ||
            lower.contains("mine_shaft") || lower.contains("vertical_mine") || lower.contains("safe_mine");
    }

    private boolean isFarmingSkill(String lower) {
        return lower.contains("plant_") || lower.contains("water_crop") || lower.contains("fertilize_") ||
            lower.contains("bone_meal") || lower.contains("grow_crop") || lower.contains("harvest_") ||
            lower.contains("replant_") || lower.contains("farm_automated") || lower.contains("breed_") ||
            lower.contains("feed_animal") || lower.contains("till_") || lower.contains("sow_");
    }

    private boolean isCraftingSkill(String lower) {
        return (lower.startsWith("craft_") || lower.startsWith("make_") || lower.startsWith("smelt_") ||
            lower.startsWith("cook_") || lower.startsWith("bake_") || lower.startsWith("brew_") ||
            lower.contains("chest") || lower.contains("store_") || lower.contains("deposit_") ||
            lower.contains("withdraw_") || lower.contains("fuel_") || lower.contains("configure_furnace")) &&
            !lower.contains("crafting_table"); // crafting_table is handled by interaction
    }

    private String humanize(String id) {
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}
