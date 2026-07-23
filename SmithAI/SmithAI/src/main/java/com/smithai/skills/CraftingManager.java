package com.smithai.skills;

import com.smithai.SmithAIPlugin;
import com.smithai.npc.SmithNPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;

/**
 * Handles actual in-game crafting smelting brewing and chest operations
 * using the NPC's inventory. Each method validates materials exist removes
 * inputs and places outputs.
 */
public class CraftingManager {

    private final SmithAIPlugin plugin;

    public CraftingManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
    }

    // ── MAIN ENTRY ──

    public void execute(SmithNPC npc, String skill, Map<String, Object> params, Player contextPlayer) {
        String lower = skill.toLowerCase();
        Player fake = npc.getEntity() instanceof Player ? (Player) npc.getEntity() : null;
        if (fake == null) return;

        if (lower.contains("craft_") || lower.contains("make_")) {
            String target = lower.replace("craft_", "").replace("make_", "").replace("_", "");
            craftItem(fake, target, params);
            return;
        }
        if (lower.contains("smelt_") || lower.contains("cook_") || lower.contains("bake_")) {
            smeltItem(fake, lower, params);
            return;
        }
        if (lower.contains("brew_")) {
            brewPotion(fake, lower, params);
            return;
        }
        if (lower.contains("chest") || lower.contains("store_") || lower.contains("deposit_") || lower.contains("withdraw_")) {
            chestOperation(fake, lower, params);
            return;
        }
        if (lower.contains("fuel_") || lower.contains("configure_furnace")) {
            fuelFurnace(fake, params);
            return;
        }

        // Fallback: show recipe
        showRecipe(fake, lower);
    }

    // ── CRAFTING ──

    private void craftItem(Player fake, String target, Map<String, Object> params) {
        Recipe recipe = null;
        // Try to find a matching recipe
        if (target.contains("craftingtable") || target.contains("workbench")) {
            if (removeMaterials(fake, Material.OAK_PLANKS, 4) ||
                removeMaterials(fake, Material.SPRUCE_PLANKS, 4) ||
                removeMaterials(fake, Material.BIRCH_PLANKS, 4)) {
                giveResult(fake, Material.CRAFTING_TABLE, 1);
            }
            return;
        }
        if (target.contains("stick") || target.contains("sticks")) {
            if (removeMaterials(fake, Material.OAK_PLANKS, 2) ||
                removeMaterials(fake, Material.SPRUCE_PLANKS, 2)) {
                giveResult(fake, Material.STICK, 4);
            }
            return;
        }
        if (target.contains("torch") || target.contains("torches")) {
            if (removeMaterials(fake, Material.COAL, 1) || removeMaterials(fake, Material.CHARCOAL, 1)) {
                if (removeMaterials(fake, Material.STICK, 1)) {
                    giveResult(fake, Material.TORCH, 4);
                }
            }
            return;
        }
        if (target.contains("furnace")) {
            if (removeMaterials(fake, Material.COBBLESTONE, 8)) {
                giveResult(fake, Material.FURNACE, 1);
            }
            return;
        }
        if (target.contains("chest")) {
            if (removeMaterials(fake, Material.OAK_PLANKS, 8) ||
                removeMaterials(fake, Material.SPRUCE_PLANKS, 8)) {
                giveResult(fake, Material.CHEST, 1);
            }
            return;
        }
        if (target.contains("plank") || target.contains("planks")) {
            if (removeMaterials(fake, Material.OAK_LOG, 1) ||
                removeMaterials(fake, Material.SPRUCE_LOG, 1) ||
                removeMaterials(fake, Material.BIRCH_LOG, 1)) {
                giveResult(fake, Material.OAK_PLANKS, 4);
            }
            return;
        }
        if (target.contains("pickaxe")) {
            String tier = extractToolTier(target);
            Material head = tierToIngot(tier);
            if (head != null && removeMaterials(fake, head, 3) && removeMaterials(fake, Material.STICK, 2)) {
                giveResult(fake, tierToPickaxe(tier), 1);
            }
            return;
        }
        if (target.contains("sword")) {
            String tier = extractToolTier(target);
            Material head = tierToIngot(tier);
            if (head != null && removeMaterials(fake, head, 2) && removeMaterials(fake, Material.STICK, 1)) {
                giveResult(fake, tierToSword(tier), 1);
            }
            return;
        }
        if (target.contains("axe")) {
            String tier = extractToolTier(target);
            Material head = tierToIngot(tier);
            if (head != null && removeMaterials(fake, head, 3) && removeMaterials(fake, Material.STICK, 2)) {
                giveResult(fake, tierToAxe(tier), 1);
            }
            return;
        }
        if (target.contains("shovel")) {
            String tier = extractToolTier(target);
            Material head = tierToIngot(tier);
            if (head != null && removeMaterials(fake, head, 1) && removeMaterials(fake, Material.STICK, 2)) {
                giveResult(fake, tierToShovel(tier), 1);
            }
            return;
        }
        if (target.contains("hoe")) {
            String tier = extractToolTier(target);
            Material head = tierToIngot(tier);
            if (head != null && removeMaterials(fake, head, 2) && removeMaterials(fake, Material.STICK, 2)) {
                giveResult(fake, tierToHoe(tier), 1);
            }
            return;
        }
        if (target.contains("door")) {
            Material door = Material.OAK_DOOR;
            if (removeMaterials(fake, Material.OAK_PLANKS, 6)) giveResult(fake, door, 3);
            return;
        }
        if (target.contains("trapdoor")) {
            if (removeMaterials(fake, Material.OAK_PLANKS, 6)) giveResult(fake, Material.OAK_TRAPDOOR, 2);
            return;
        }
        if (target.contains("bed")) {
            if (removeMaterials(fake, Material.OAK_PLANKS, 4) || removeMaterials(fake, Material.SPRUCE_PLANKS, 4)) {
                if (removeMaterialAny(fake, "wool", 3)) {
                    giveResult(fake, Material.RED_BED, 1);
                }
            }
            return;
        }
        if (target.contains("ladder")) {
            if (removeMaterials(fake, Material.STICK, 7)) giveResult(fake, Material.LADDER, 3);
            return;
        }
        if (target.contains("bucket")) {
            if (removeMaterials(fake, Material.IRON_INGOT, 3)) giveResult(fake, Material.BUCKET, 1);
            return;
        }
        if (target.contains("compass")) {
            if (removeMaterials(fake, Material.IRON_INGOT, 4) && removeMaterials(fake, Material.REDSTONE, 1))
                giveResult(fake, Material.COMPASS, 1);
            return;
        }
        if (target.contains("enderchest") || target.contains("ender_chest")) {
            if (removeMaterials(fake, Material.OBSIDIAN, 8) && removeMaterials(fake, Material.ENDER_EYE, 1))
                giveResult(fake, Material.ENDER_CHEST, 1);
            return;
        }
        if (target.contains("anvil")) {
            if (removeMaterials(fake, Material.IRON_BLOCK, 3) && removeMaterials(fake, Material.IRON_INGOT, 4))
                giveResult(fake, Material.ANVIL, 1);
            return;
        }
        if (target.contains("enchantingtable") || target.contains("enchanting_table")) {
            if (removeMaterials(fake, Material.OBSIDIAN, 4) && removeMaterials(fake, Material.DIAMOND, 2) && removeMaterials(fake, Material.BOOK, 1))
                giveResult(fake, Material.ENCHANTING_TABLE, 1);
            return;
        }
        if (target.contains("brewingstand") || target.contains("brewing_stand")) {
            if (removeMaterials(fake, Material.BLAZE_ROD, 1) && removeMaterials(fake, Material.COBBLESTONE, 3))
                giveResult(fake, Material.BREWING_STAND, 1);
            return;
        }

        fake.sendMessage("§eI don't know how to craft that yet. Check with /smithai craft <item>");
    }

    // ── SMELTING ──

    private void smeltItem(Player fake, String skill, Map<String, Object> params) {
        if (!hasNearbyBlock(fake, Material.FURNACE, 5)) {
            fake.sendMessage("§eI need a nearby furnace to smelt.");
            return;
        }
        if (!removeMaterials(fake, Material.COAL, 1) && !removeMaterials(fake, Material.CHARCOAL, 1) &&
            !removeMaterials(fake, Material.OAK_PLANKS, 2) && !removeMaterials(fake, Material.LAVA_BUCKET, 1)) {
            fake.sendMessage("§eI need fuel (coal/charcoal/wood) to smelt.");
            return;
        }
        // Determine what to smelt
        String target = skill.replace("smelt_", "").replace("cook_", "").replace("bake_", "");
        Material input = null;
        Material output = null;
        int count = 1;
        if (target.contains("iron")) { input = Material.IRON_ORE; output = Material.IRON_INGOT; }
        else if (target.contains("gold")) { input = Material.GOLD_ORE; output = Material.GOLD_INGOT; }
        else if (target.contains("copper")) { input = Material.COPPER_ORE; output = Material.COPPER_INGOT; }
        else if (target.contains("ancient_debris") || target.contains("netherite")) { input = Material.ANCIENT_DEBRIS; output = Material.NETHERITE_SCRAP; }
        else if (target.contains("beef") || target.contains("steak")) { input = Material.BEEF; output = Material.COOKED_BEEF; count = 1; }
        else if (target.contains("pork") || target.contains("porkchop")) { input = Material.PORKCHOP; output = Material.COOKED_PORKCHOP; }
        else if (target.contains("chicken")) { input = Material.CHICKEN; output = Material.COOKED_CHICKEN; }
        else if (target.contains("mutton")) { input = Material.MUTTON; output = Material.COOKED_MUTTON; }
        else if (target.contains("rabbit")) { input = Material.RABBIT; output = Material.COOKED_RABBIT; }
        else if (target.contains("potato")) { input = Material.POTATO; output = Material.BAKED_POTATO; }
        else if (target.contains("sand") || target.contains("glass")) { input = Material.SAND; output = Material.GLASS; }
        else if (target.contains("cobble") || target.contains("stone")) { input = Material.COBBLESTONE; output = Material.STONE; }
        else if (target.contains("clay")) { input = Material.CLAY; output = Material.TERRACOTTA; }
        else if (target.contains("cactus")) { input = Material.CACTUS; output = Material.GREEN_DYE; }
        else if (target.contains("log") || target.contains("wood")) { input = Material.OAK_LOG; output = Material.CHARCOAL; }

        if (input != null && removeMaterials(fake, input, 1)) {
            giveResult(fake, output, count);
            fake.sendMessage("§aSmeleted " + input.name().toLowerCase().replace("_", " ") + " → " + output.name().toLowerCase().replace("_", " ") + ".");
        } else {
            fake.sendMessage("§eI don't have the materials to smelt that.");
        }
    }

    // ── BREWING ──

    private void brewPotion(Player fake, String skill, Map<String, Object> params) {
        if (!hasNearbyBlock(fake, Material.BREWING_STAND, 5)) {
            fake.sendMessage("§eI need a nearby brewing stand to brew.");
            return;
        }
        if (!removeMaterials(fake, Material.BLAZE_POWDER, 1)) {
            fake.sendMessage("§eI need blaze powder to fuel the brewing stand.");
            return;
        }
        if (!removeMaterials(fake, Material.GLASS_BOTTLE, 1)) {
            fake.sendMessage("§eI need glass bottles.");
            return;
        }
        giveResult(fake, Material.POTION, 1);
        fake.sendMessage("§aBrewing started (advanced potion effects need specific ingredients).");
    }

    // ── CHEST OPERATIONS ──

    private void chestOperation(Player fake, String skill, Map<String, Object> params) {
        Chest chest = findNearbyChest(fake, 5);
        if (chest == null) {
            fake.sendMessage("§eI need a nearby chest.");
            return;
        }
        Inventory chestInv = chest.getInventory();
        PlayerInventory pInv = fake.getInventory();

        if (skill.contains("store_") || skill.contains("deposit_")) {
            Map<Integer, ItemStack> excess = new HashMap<>();
            for (int i = 0; i < pInv.getSize(); i++) {
                ItemStack stack = pInv.getItem(i);
                if (stack != null && stack.getType() != Material.AIR) {
                    if (!isEssentialTool(stack.getType())) {
                        chestInv.addItem(stack);
                        pInv.setItem(i, null);
                    }
                }
            }
            fake.sendMessage("§aStored excess items in chest.");
            return;
        }
        if (skill.contains("withdraw_") || skill.contains("retrieve_")) {
            for (ItemStack stack : chestInv.getContents()) {
                if (stack != null && stack.getType() != Material.AIR) {
                    if (pInv.firstEmpty() >= 0) {
                        pInv.addItem(stack.clone());
                        chestInv.remove(stack);
                    }
                }
            }
            fake.sendMessage("§aRetrieved items from chest.");
            return;
        }
        // Scan chest contents
        StringBuilder sb = new StringBuilder("Chest: ");
        boolean empty = true;
        for (ItemStack stack : chestInv.getContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                sb.append(stack.getAmount()).append("x ").append(stack.getType().name().toLowerCase().replace("_", " ")).append(", ");
                empty = false;
            }
        }
        if (empty) sb.append("empty");
        else sb.setLength(sb.length() - 2);
        fake.sendMessage(sb.toString());
    }

    private boolean isEssentialTool(Material mat) {
        String n = mat.name().toLowerCase();
        return n.contains("pickaxe") || n.contains("axe") || n.contains("sword") ||
               n.contains("shovel") || n.contains("hoe") || n.contains("helmet") ||
               n.contains("chestplate") || n.contains("leggings") || n.contains("boots") ||
               n.contains("bow") || n.contains("shield") || n.contains("trident");
    }

    // ── FURNACE FUELING ──

    private void fuelFurnace(Player fake, Map<String, Object> params) {
        Furnace furnace = findNearbyFurnace(fake, 5);
        if (furnace == null) {
            fake.sendMessage("§eNo furnace nearby.");
            return;
        }
        FurnaceInventory finv = furnace.getInventory();
        if (finv.getFuel() == null || finv.getFuel().getType() == Material.AIR) {
            if (removeMaterials(fake, Material.COAL, 1)) {
                finv.setFuel(new ItemStack(Material.COAL, 1));
                fake.sendMessage("§aCoal added to furnace.");
            } else {
                fake.sendMessage("§eNo coal to fuel furnace.");
            }
        }
    }

    // ── UTILITY ──

    private boolean removeMaterials(Player player, Material mat, int count) {
        PlayerInventory inv = player.getInventory();
        int found = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() == mat) found += stack.getAmount();
        }
        if (found < count) return false;
        int remaining = count;
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && stack.getType() == mat) {
                int take = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - take);
                remaining -= take;
                if (stack.getAmount() <= 0) inv.setItem(i, null);
            }
        }
        return true;
    }

    private boolean removeMaterialAny(Player player, String nameContains, int count) {
        PlayerInventory inv = player.getInventory();
        int found = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType().name().toLowerCase().contains(nameContains))
                found += stack.getAmount();
        }
        if (found < count) return false;
        int remaining = count;
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && stack.getType().name().toLowerCase().contains(nameContains)) {
                int take = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - take);
                remaining -= take;
                if (stack.getAmount() <= 0) inv.setItem(i, null);
            }
        }
        return true;
    }

    private void giveResult(Player player, Material mat, int count) {
        ItemStack result = new ItemStack(mat, count);
        player.getInventory().addItem(result);
    }

    private boolean hasNearbyBlock(Player player, Material type, double radius) {
        Location loc = player.getLocation();
        double r2 = radius * radius;
        for (int dx = -(int)radius; dx <= (int)radius; dx++) {
            for (int dz = -(int)radius; dz <= (int)radius; dz++) {
                for (int dy = -(int)radius; dy <= (int)radius; dy++) {
                    Block b = loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY() + dy, loc.getBlockZ() + dz);
                    if (b.getType() == type && b.getLocation().distanceSquared(loc) <= r2) return true;
                }
            }
        }
        return false;
    }

    private Furnace findNearbyFurnace(Player player, double radius) {
        Location loc = player.getLocation();
        double r2 = radius * radius;
        for (int dx = -(int)radius; dx <= (int)radius; dx++) {
            for (int dz = -(int)radius; dz <= (int)radius; dz++) {
                for (int dy = -(int)radius; dy <= (int)radius; dy++) {
                    Block b = loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY() + dy, loc.getBlockZ() + dz);
                    if (b.getState() instanceof Furnace && b.getLocation().distanceSquared(loc) <= r2)
                        return (Furnace) b.getState();
                }
            }
        }
        return null;
    }

    private Chest findNearbyChest(Player player, double radius) {
        Location loc = player.getLocation();
        double r2 = radius * radius;
        for (int dx = -(int)radius; dx <= (int)radius; dx++) {
            for (int dz = -(int)radius; dz <= (int)radius; dz++) {
                for (int dy = -(int)radius; dy <= (int)radius; dy++) {
                    Block b = loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY() + dy, loc.getBlockZ() + dz);
                    if (b.getState() instanceof Chest && b.getLocation().distanceSquared(loc) <= r2)
                        return (Chest) b.getState();
                }
            }
        }
        return null;
    }

    private String extractToolTier(String name) {
        if (name.contains("diamond")) return "diamond";
        if (name.contains("iron")) return "iron";
        if (name.contains("stone")) return "stone";
        if (name.contains("gold") || name.contains("golden")) return "gold";
        if (name.contains("wood") || name.contains("wooden")) return "wood";
        return "stone";
    }

    private Material tierToIngot(String tier) {
        switch (tier) {
            case "diamond": return Material.DIAMOND;
            case "iron": return Material.IRON_INGOT;
            case "gold": return Material.GOLD_INGOT;
            case "stone": return Material.COBBLESTONE;
            case "wood": return Material.OAK_PLANKS;
            default: return Material.COBBLESTONE;
        }
    }

    private Material tierToPickaxe(String tier) {
        switch (tier) {
            case "diamond": return Material.DIAMOND_PICKAXE;
            case "iron": return Material.IRON_PICKAXE;
            case "gold": return Material.GOLDEN_PICKAXE;
            case "stone": return Material.STONE_PICKAXE;
            case "wood": return Material.WOODEN_PICKAXE;
            default: return Material.STONE_PICKAXE;
        }
    }

    private Material tierToSword(String tier) {
        switch (tier) {
            case "diamond": return Material.DIAMOND_SWORD;
            case "iron": return Material.IRON_SWORD;
            case "gold": return Material.GOLDEN_SWORD;
            case "stone": return Material.STONE_SWORD;
            case "wood": return Material.WOODEN_SWORD;
            default: return Material.STONE_SWORD;
        }
    }

    private Material tierToAxe(String tier) {
        switch (tier) {
            case "diamond": return Material.DIAMOND_AXE;
            case "iron": return Material.IRON_AXE;
            case "gold": return Material.GOLDEN_AXE;
            case "stone": return Material.STONE_AXE;
            case "wood": return Material.WOODEN_AXE;
            default: return Material.STONE_AXE;
        }
    }

    private Material tierToShovel(String tier) {
        switch (tier) {
            case "diamond": return Material.DIAMOND_SHOVEL;
            case "iron": return Material.IRON_SHOVEL;
            case "gold": return Material.GOLDEN_SHOVEL;
            case "stone": return Material.STONE_SHOVEL;
            case "wood": return Material.WOODEN_SHOVEL;
            default: return Material.STONE_SHOVEL;
        }
    }

    private Material tierToHoe(String tier) {
        switch (tier) {
            case "diamond": return Material.DIAMOND_HOE;
            case "iron": return Material.IRON_HOE;
            case "gold": return Material.GOLDEN_HOE;
            case "stone": return Material.STONE_HOE;
            case "wood": return Material.WOODEN_HOE;
            default: return Material.STONE_HOE;
        }
    }

    private void showRecipe(Player fake, String item) {
        fake.sendMessage("§eI can craft many items. Try: craft_pickaxe, craft_furnace, craft_chest, smelt_iron, etc.");
    }
}
