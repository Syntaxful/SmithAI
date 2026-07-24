package com.smithai.skills;

import com.smithai.SmithAIPlugin;
import com.smithai.npc.SmithNPC;
import com.smithai.util.BlockCompat;
import com.smithai.util.CraftingHelper;
import com.smithai.util.LivingEntityCompat;
import com.smithai.util.MaterialCompat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * Dispatches broad skill categories to concrete Bukkit actions.
 * This is a starter implementation; primitive executors are added as the project grows.
 */
public class SkillDispatcher {

    private final SmithAIPlugin plugin;

    public SkillDispatcher(SmithAIPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(SmithNPC npc, String skillName, Map<String, Object> params, Player contextPlayer) {
        if (npc == null || npc.getEntity() == null || npc.getEntity().isDead()) {
            return;
        }
        String lower = skillName.toLowerCase();

        // Chat / social
        if (isChatSkill(lower)) {
            executeChat(npc, lower, contextPlayer);
            return;
        }

        // Movement
        if (lower.startsWith("follow") || lower.equals("move_to_player") || lower.equals("teleport_to_player")) {
            if (contextPlayer != null) npc.follow(contextPlayer);
            return;
        }
        if (lower.startsWith("stay") || lower.equals("stay")) {
            npc.stay();
            return;
        }
        if (lower.startsWith("teleport_to_spawn") || lower.equals("go_home") || lower.equals("teleport_home") || lower.equals("spawn")) {
            Location spawn = npc.getLocation() != null ? npc.getLocation().getWorld().getSpawnLocation() : null;
            if (spawn != null) npc.teleport(spawn);
            return;
        }
        if (lower.startsWith("look") || lower.startsWith("turn") || lower.startsWith("inspect") || lower.startsWith("check_")) {
            if (contextPlayer != null) npc.lookAt(contextPlayer.getLocation());
            return;
        }
        if (lower.startsWith("move") || lower.startsWith("jump") || lower.startsWith("sneak") || lower.startsWith("sprint") ||
            lower.startsWith("climb") || lower.startsWith("swim") || lower.startsWith("explore_random") || lower.startsWith("wander") ||
            lower.startsWith("walk") || lower.startsWith("run") || lower.startsWith("go_to") || lower.startsWith("goto")) {
            executeMovement(npc, lower, contextPlayer, params);
            return;
        }

        // Basic interaction
        if (isInteractionSkill(lower)) {
            executeInteraction(npc, lower, contextPlayer, params);
            return;
        }

        // Combat / survival / attack
        if (isCombatSkill(lower)) {
            executeCombat(npc, lower, contextPlayer, params);
            return;
        }

        // Gather / resource / farming / crafting / building
        if (isResourceSkill(lower)) {
            executeResource(npc, lower, contextPlayer, params);
            return;
        }

        // Advanced / progression / strategy
        executeComposite(npc, lower, contextPlayer, "I'll plan and execute that advanced objective: " + humanize(skillName));
    }

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
            lower.startsWith("farm_") || lower.startsWith("plant_") || lower.startsWith("water_") || lower.startsWith("harvest_") || lower.startsWith("replant_");
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

    private void executeChat(SmithNPC npc, String skill, Player player) {
        String reply = generateChatReply(skill, player);
        if (player != null) {
            npc.sendMessage(player, reply);
        } else {
            npc.sendMessageToAll(reply);
        }
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
        if (skill.contains("jump")) {
            npc.jump();
            return;
        }
        if (target != null) {
            npc.setMoveTarget(target);
            npc.setTaskLookTarget(target.clone().add(0, 1, 0), 1000);
            return;
        }
        if (player != null) npc.lookAt(player.getLocation());
    }

    private void executeInteraction(SmithNPC npc, String skill, Player player, Map<String, Object> params) {
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
            breakBlock(npc, skill, params);
            return;
        }
        // No chat spam; action bar shows the current skill.
    }

    private void selectBestTool(SmithNPC npc, String skill) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();
        Material preferred = null;
        if (skill.contains("pickaxe") || skill.contains("mine") || skill.contains("dig") || skill.contains("break_stone") || skill.contains("ore") || skill.contains("obsidian")) {
            preferred = firstAvailable(inv, "DIAMOND_PICKAXE", "IRON_PICKAXE", "STONE_PICKAXE", "WOODEN_PICKAXE", "WOOD_PICKAXE");
        } else if (skill.contains("axe") || skill.contains("chop") || skill.contains("break_wood") || skill.contains("log")) {
            preferred = firstAvailable(inv, "DIAMOND_AXE", "IRON_AXE", "STONE_AXE", "WOODEN_AXE", "WOOD_AXE");
        } else if (skill.contains("sword") || skill.contains("fight") || skill.contains("attack") || skill.contains("combat") || skill.contains("hostile") || skill.contains("mob")) {
            preferred = firstAvailable(inv, "DIAMOND_SWORD", "IRON_SWORD", "STONE_SWORD", "WOODEN_SWORD", "WOOD_SWORD");
        } else if (skill.contains("shovel") || skill.contains("dirt") || skill.contains("sand") || skill.contains("gravel") || skill.contains("soil")) {
            preferred = firstAvailable(inv, "DIAMOND_SHOVEL", "IRON_SHOVEL", "STONE_SHOVEL", "WOODEN_SHOVEL", "WOOD_SPADE");
        }
        if (preferred != null && inv.contains(preferred)) {
            int slot = inv.first(preferred);
            if (slot >= 0 && slot < 9) {
                inv.setHeldItemSlot(slot);
            }
        }
    }

    private Material firstAvailable(PlayerInventory inv, String... names) {
        for (String name : names) {
            Material mat = MaterialCompat.get(name);
            if (mat != null && inv.contains(mat)) return mat;
        }
        return null;
    }

    private void placeTorch(SmithNPC npc) {
        Location loc = npc.getLocation();
        if (loc == null) return;
        Entity entity = npc.getEntity();
        if (!(entity instanceof Player)) return;
        Player fake = (Player) entity;
        PlayerInventory inv = fake.getInventory();
        Material torch = MaterialCompat.get("TORCH", "LEGACY_TORCH");
        if (torch == null || !inv.contains(torch)) return;
        Block target = loc.getBlock();
        Block placeOn = target.getRelative(BlockFace.DOWN);
        if (MaterialCompat.isSolid(placeOn.getType()) && BlockCompat.isAir(target)) {
            target.setType(torch);
            removeOne(fake, torch);
            npc.setTaskLookTarget(target.getLocation().add(0.5, 0.5, 0.5), 1500);
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
        if (mat == null || !MaterialCompat.isBlock(mat)) mat = Material.DIRT;
        PlayerInventory inv = fake.getInventory();
        if (!inv.contains(mat)) return;
        Block target = loc.getBlock();
        if (BlockCompat.isAir(target)) {
            target.setType(mat);
            removeOne(fake, mat);
            npc.setTaskLookTarget(target.getLocation().add(0.5, 0.5, 0.5), 1500);
        }
    }

    private void breakBlock(SmithNPC npc, String skill, Map<String, Object> params) {
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
        Material t = target.getType();
        if (BlockCompat.isAir(target) || t == Material.BEDROCK || t == Material.WATER || t == Material.LAVA || t == MaterialCompat.get("STATIONARY_WATER") || t == MaterialCompat.get("STATIONARY_LAVA")) {
            return;
        }
        selectBestTool(npc, skill);
        target.breakNaturally();
        npc.setTaskLookTarget(target.getLocation().add(0.5, 0.5, 0.5), 1500);
    }

    private void removeOne(Player player, Material mat) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == mat) {
                if (stack.getAmount() <= 1) {
                    inv.setItem(i, null);
                } else {
                    stack.setAmount(stack.getAmount() - 1);
                    inv.setItem(i, stack);
                }
                return;
            }
        }
    }

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
            // Default food/potion choices
            Material[] foods = {
                MaterialCompat.get("COOKED_BEEF", "COOKED_BEEF"),
                MaterialCompat.get("COOKED_PORKCHOP", "GRILLED_PORK"),
                MaterialCompat.get("COOKED_CHICKEN", "COOKED_CHICKEN"),
                MaterialCompat.get("BREAD", "BREAD"),
                MaterialCompat.get("COOKED_MUTTON", "COOKED_MUTTON"),
                MaterialCompat.get("COOKED_RABBIT", "COOKED_RABBIT"),
                MaterialCompat.get("BAKED_POTATO", "BAKED_POTATO"),
                MaterialCompat.get("GOLDEN_APPLE", "GOLDEN_APPLE"),
                MaterialCompat.get("APPLE", "APPLE"),
                MaterialCompat.get("CARROT", "CARROT_ITEM")
            };
            for (Material food : foods) {
                if (food != null && inv.contains(food)) {
                    mat = food;
                    break;
                }
            }
        }
        if (mat == null || mat == Material.AIR || !inv.contains(mat)) {
            return;
        }
        // For food items, heal the NPC instead of consuming in the fake inventory
        if (MaterialCompat.isEdible(mat) && entity instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) entity;
            le.setHealth(Math.min(le.getHealth() + 4, le.getMaxHealth()));
            removeOne(fake, mat);
        } else {
            // General item use: remove one and simulate usage (e.g., potions)
            removeOne(fake, mat);
        }
    }

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
            // Drop the held item if nothing specified
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
            if (fake.getInventory().firstEmpty() >= 0) {
                fake.getInventory().addItem(stack.clone());
                drop.remove();
            }
        }
    }

    private void executeCombat(SmithNPC npc, String skill, Player player, Map<String, Object> params) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof LivingEntity)) return;
        LivingEntity self = (LivingEntity) entity;

        LivingEntity target = null;
        if (params != null && params.containsKey("target")) {
            String targetName = String.valueOf(params.get("target")).toLowerCase();
            for (Entity e : self.getWorld().getNearbyEntities(self.getLocation(), 8, 8, 8)) {
                if (e instanceof LivingEntity && e != self && !(e instanceof Player)) {
                    String type = e.getType().name().toLowerCase();
                    if (type.contains(targetName)) {
                        target = (LivingEntity) e;
                        break;
                    }
                }
            }
        }
        if (target == null) target = findNearestHostile(self, 8);
        if (target != null) {
            selectBestTool(npc, "sword");
            LivingEntityCompat.attack(self, target);
            npc.setTaskLookTarget(target.getLocation().add(0, 1, 0), 1500);
        }
    }

    private LivingEntity findNearestHostile(LivingEntity self, double radius) {
        LivingEntity nearest = null;
        double best = radius * radius;
        Location loc = self.getLocation();
        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e instanceof LivingEntity && e != self && !(e instanceof Player)) {
                double d = e.getLocation().distanceSquared(loc);
                if (d < best) {
                    best = d;
                    nearest = (LivingEntity) e;
                }
            }
        }
        return nearest;
    }

    private void executeResource(SmithNPC npc, String skill, Player player, Map<String, Object> params) {
        if (skill.contains("place_torch") || skill.contains("light_") || skill.contains("torch")) {
            placeTorch(npc);
            return;
        }
        if (skill.contains("break_") || skill.contains("mine_") || skill.contains("chop_") || skill.contains("dig_") ||
            skill.contains("harvest_") || skill.contains("gather_wood") || skill.contains("gather_stone") || skill.contains("gather_ore")) {
            breakBlock(npc, skill, params);
            return;
        }
        if (skill.contains("craft_") || skill.contains("make_") || skill.contains("forge_") || skill.contains("assemble_") ||
            skill.contains("repair_") || skill.contains("cook_") || skill.contains("bake_") || skill.contains("brew_") ||
            skill.contains("smelt_")) {
            if (craftItem(npc, skill)) return;
        }
        if (skill.contains("place_") || skill.contains("build_") || skill.contains("expand_") || skill.contains("wall")) {
            placeBlock(npc, params);
            return;
        }
        executeComposite(npc, skill, player, "I'll work on that task: " + humanize(skill));
    }

    private boolean craftItem(SmithNPC npc, String skill) {
        if (!(npc.getEntity() instanceof Player)) return false;
        Player fake = (Player) npc.getEntity();
        if (CraftingHelper.craft(fake, skill)) {
            npc.setTaskLookTarget(fake.getLocation().add(0, -1, 0), 1000);
            return true;
        }
        return false;
    }

    private void executeComposite(SmithNPC npc, String skill, Player player, String defaultMessage) {
        // No chat spam; action bar already shows the current skill.
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
