package com.smithai.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Version-safe crafting helper for common recipes.
 * Handles a small set of recipes explicitly so the plugin works on 1.12-1.21.x
 * without relying on the shaped/shapeless recipe API, which varies by version.
 */
public class CraftingHelper {

    private static final List<Recipe> RECIPES = new ArrayList<>();

    static {
        // Planks
        add(MaterialCompat.get("OAK_PLANKS", "PLANKS", "WOOD"),
            ing(MaterialCompat.get("OAK_LOG", "LOG", "LEGACY_LOG"), 1),
            ing(MaterialCompat.get("SPRUCE_LOG", "LOG_2", "LEGACY_LOG_2"), 1),
            ing(MaterialCompat.get("BIRCH_LOG", "LOG", "LEGACY_LOG"), 1),
            ing(MaterialCompat.get("JUNGLE_LOG", "LOG", "LEGACY_LOG"), 1),
            ing(MaterialCompat.get("ACACIA_LOG", "LOG_2", "LEGACY_LOG_2"), 1),
            ing(MaterialCompat.get("DARK_OAK_LOG", "LOG_2", "LEGACY_LOG_2"), 1));
        add(MaterialCompat.get("STICK"), ing(MaterialCompat.get("OAK_PLANKS", "PLANKS", "WOOD"), 2));
        add(MaterialCompat.get("CRAFTING_TABLE", "WORKBENCH"), ing(MaterialCompat.get("OAK_PLANKS", "PLANKS", "WOOD"), 4));
        add(MaterialCompat.get("FURNACE"), ing(MaterialCompat.get("COBBLESTONE"), 8));
        add(MaterialCompat.get("CHEST"), ing(MaterialCompat.get("OAK_PLANKS", "PLANKS", "WOOD"), 8));
        add(MaterialCompat.get("TORCH"),
            ing(MaterialCompat.get("COAL"), 1),
            ing(MaterialCompat.get("CHARCOAL"), 1),
            ing(MaterialCompat.get("STICK"), 1));

        // Tools
        addPickaxe("WOODEN_PICKAXE", "WOOD_PICKAXE", "WOOD_SPADE");
        addPickaxe("STONE_PICKAXE", "STONE_PICKAXE", "STONE_SPADE");
        addPickaxe("IRON_PICKAXE", "IRON_PICKAXE", "IRON_SPADE");
        addPickaxe("GOLDEN_PICKAXE", "GOLD_PICKAXE", "GOLD_SPADE");
        addPickaxe("DIAMOND_PICKAXE", "DIAMOND_PICKAXE", "DIAMOND_SPADE");

        addAxe("WOODEN_AXE", "WOOD_AXE");
        addAxe("STONE_AXE", "STONE_AXE");
        addAxe("IRON_AXE", "IRON_AXE");
        addAxe("GOLDEN_AXE", "GOLD_AXE");
        addAxe("DIAMOND_AXE", "DIAMOND_AXE");

        addSword("WOODEN_SWORD", "WOOD_SWORD");
        addSword("STONE_SWORD", "STONE_SWORD");
        addSword("IRON_SWORD", "IRON_SWORD");
        addSword("GOLDEN_SWORD", "GOLD_SWORD");
        addSword("DIAMOND_SWORD", "DIAMOND_SWORD");

        addShovel("WOODEN_SHOVEL", "WOOD_SPADE");
        addShovel("STONE_SHOVEL", "STONE_SPADE");
        addShovel("IRON_SHOVEL", "IRON_SPADE");
        addShovel("GOLDEN_SHOVEL", "GOLD_SPADE");
        addShovel("DIAMOND_SHOVEL", "DIAMOND_SPADE");

        addHoe("WOODEN_HOE", "WOOD_HOE");
        addHoe("STONE_HOE", "STONE_HOE");
        addHoe("IRON_HOE", "IRON_HOE");
        addHoe("GOLDEN_HOE", "GOLD_HOE");
        addHoe("DIAMOND_HOE", "DIAMOND_HOE");

        // Armor
        addHelmet("LEATHER_HELMET");
        addHelmet("IRON_HELMET");
        addHelmet("GOLDEN_HELMET", "GOLD_HELMET");
        addHelmet("DIAMOND_HELMET");
        addChestplate("LEATHER_CHESTPLATE");
        addChestplate("IRON_CHESTPLATE");
        addChestplate("GOLDEN_CHESTPLATE", "GOLD_CHESTPLATE");
        addChestplate("DIAMOND_CHESTPLATE");
        addLeggings("LEATHER_LEGGINGS");
        addLeggings("IRON_LEGGINGS");
        addLeggings("GOLDEN_LEGGINGS", "GOLD_LEGGINGS");
        addLeggings("DIAMOND_LEGGINGS");
        addBoots("LEATHER_BOOTS");
        addBoots("IRON_BOOTS");
        addBoots("GOLDEN_BOOTS", "GOLD_BOOTS");
        addBoots("DIAMOND_BOOTS");

        // Food / blocks
        add(MaterialCompat.get("BREAD"), ing(MaterialCompat.get("WHEAT"), 3));
        add(MaterialCompat.get("PAPER"), ing(MaterialCompat.get("SUGAR_CANE", "SUGAR_CANE"), 3));
    }

    private static void add(Material result, Ingredient... ingredients) {
        if (result == null) return;
        RECIPES.add(new Recipe(result, 1, Arrays.asList(ingredients)));
    }

    private static void add(Material result, int amount, Ingredient... ingredients) {
        if (result == null) return;
        RECIPES.add(new Recipe(result, amount, Arrays.asList(ingredients)));
    }

    private static Ingredient ing(Material material, int amount) {
        return new Ingredient(material, amount);
    }

    private static void addPickaxe(String headName, String legacyHeadName, String legacyStickName) {
        Material head = MaterialCompat.get(headName, legacyHeadName);
        Material stick = MaterialCompat.get("STICK", legacyStickName);
        if (head == null || stick == null) return;
        add(head, ing(stick, 2), ing(head, 3));
    }

    private static void addAxe(String headName, String legacyHeadName) {
        Material head = MaterialCompat.get(headName, legacyHeadName);
        Material stick = MaterialCompat.get("STICK");
        if (head == null || stick == null) return;
        add(head, ing(stick, 2), ing(head, 3));
    }

    private static void addSword(String headName, String legacyHeadName) {
        Material head = MaterialCompat.get(headName, legacyHeadName);
        Material stick = MaterialCompat.get("STICK");
        if (head == null || stick == null) return;
        add(head, ing(stick, 1), ing(head, 2));
    }

    private static void addShovel(String headName, String legacyHeadName) {
        Material head = MaterialCompat.get(headName, legacyHeadName);
        Material stick = MaterialCompat.get("STICK");
        if (head == null || stick == null) return;
        add(head, ing(stick, 2), ing(head, 1));
    }

    private static void addHoe(String headName, String legacyHeadName) {
        Material head = MaterialCompat.get(headName, legacyHeadName);
        Material stick = MaterialCompat.get("STICK");
        if (head == null || stick == null) return;
        add(head, ing(stick, 2), ing(head, 2));
    }

    private static Material getMat(String name, String... legacyNames) {
        String[] names = new String[legacyNames.length + 1];
        names[0] = name;
        System.arraycopy(legacyNames, 0, names, 1, legacyNames.length);
        return MaterialCompat.get(names);
    }

    private static void addHelmet(String name, String... legacyNames) {
        Material mat = getMat(name, legacyNames);
        if (mat == null) return;
        add(mat, ing(mat, 5));
    }

    private static void addChestplate(String name, String... legacyNames) {
        Material mat = getMat(name, legacyNames);
        if (mat == null) return;
        add(mat, ing(mat, 8));
    }

    private static void addLeggings(String name, String... legacyNames) {
        Material mat = getMat(name, legacyNames);
        if (mat == null) return;
        add(mat, ing(mat, 7));
    }

    private static void addBoots(String name, String... legacyNames) {
        Material mat = getMat(name, legacyNames);
        if (mat == null) return;
        add(mat, ing(mat, 4));
    }

    /**
     * Try to craft the named item for the player. Returns true if crafting succeeded.
     * Skill names like "craft_wooden_pickaxe" are mapped to the result material.
     */
    public static boolean craft(Player player, String skillName) {
        if (player == null || skillName == null) return false;
        String target = skillName.toLowerCase().replaceAll("^(craft|make|build|forge|assemble)_", "");
        Material result = MaterialCompat.get(target.toUpperCase());
        if (result == null) {
            // Try common aliases
            result = resolveAlias(target);
        }
        if (result == null) return false;
        return craft(player, result);
    }

    private static Material resolveAlias(String target) {
        if (target.contains("plank")) return MaterialCompat.get("OAK_PLANKS", "PLANKS", "WOOD");
        if (target.contains("stick")) return MaterialCompat.get("STICK");
        if (target.contains("pick")) return MaterialCompat.get("WOODEN_PICKAXE", "WOOD_PICKAXE");
        if (target.contains("axe")) return MaterialCompat.get("WOODEN_AXE", "WOOD_AXE");
        if (target.contains("sword")) return MaterialCompat.get("WOODEN_SWORD", "WOOD_SWORD");
        if (target.contains("shovel") || target.contains("spade")) return MaterialCompat.get("WOODEN_SHOVEL", "WOOD_SPADE");
        if (target.contains("hoe")) return MaterialCompat.get("WOODEN_HOE", "WOOD_HOE");
        if (target.contains("helmet")) return MaterialCompat.get("LEATHER_HELMET");
        if (target.contains("chestplate")) return MaterialCompat.get("LEATHER_CHESTPLATE");
        if (target.contains("leggings")) return MaterialCompat.get("LEATHER_LEGGINGS");
        if (target.contains("boots")) return MaterialCompat.get("LEATHER_BOOTS");
        return null;
    }

    public static boolean craft(Player player, Material result) {
        if (player == null || result == null) return false;
        Recipe recipe = findRecipe(result);
        if (recipe == null) return false;
        if (!hasIngredients(player.getInventory(), recipe)) return false;
        removeIngredients(player.getInventory(), recipe);
        player.getInventory().addItem(new ItemStack(recipe.result, recipe.amount));
        return true;
    }

    private static Recipe findRecipe(Material result) {
        for (Recipe recipe : RECIPES) {
            if (recipe.result == result) return recipe;
        }
        return null;
    }

    private static boolean hasIngredients(Inventory inv, Recipe recipe) {
        Map<Material, Integer> needed = new HashMap<>();
        for (Ingredient ing : recipe.ingredients) {
            if (ing.material == null) continue;
            needed.merge(ing.material, ing.amount, Integer::sum);
        }
        Map<Material, Integer> available = new HashMap<>();
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                available.merge(stack.getType(), stack.getAmount(), Integer::sum);
            }
        }
        for (Map.Entry<Material, Integer> entry : needed.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }
        return true;
    }

    private static void removeIngredients(Inventory inv, Recipe recipe) {
        Map<Material, Integer> needed = new HashMap<>();
        for (Ingredient ing : recipe.ingredients) {
            if (ing.material == null) continue;
            needed.merge(ing.material, ing.amount, Integer::sum);
        }
        for (ItemStack stack : inv.getContents()) {
            if (stack == null || stack.getType() == Material.AIR) continue;
            Integer remaining = needed.get(stack.getType());
            if (remaining == null) continue;
            int take = Math.min(remaining, stack.getAmount());
            if (stack.getAmount() <= take) {
                inv.remove(stack);
                needed.remove(stack.getType());
            } else {
                stack.setAmount(stack.getAmount() - take);
                int left = remaining - take;
                if (left <= 0) needed.remove(stack.getType());
                else needed.put(stack.getType(), left);
            }
            if (needed.isEmpty()) break;
        }
    }

    private static class Recipe {
        final Material result;
        final int amount;
        final List<Ingredient> ingredients;
        Recipe(Material result, int amount, List<Ingredient> ingredients) {
            this.result = result;
            this.amount = amount;
            this.ingredients = ingredients;
        }
    }

    private static class Ingredient {
        final Material material;
        final int amount;
        Ingredient(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }
    }
}
