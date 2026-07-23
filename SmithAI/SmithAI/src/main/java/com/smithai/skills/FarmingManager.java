package com.smithai.skills;

import com.smithai.SmithAIPlugin;
import com.smithai.npc.SmithNPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

/**
 * Farm automation: plant, grow (bone meal), harvest, replant.
 */
public class FarmingManager {

    private final SmithAIPlugin plugin;

    // Maps crop block → seed item
    private static final Map<Material, Material> CROP_TO_SEED = new HashMap<>();
    private static final Set<Material> CROPS = new HashSet<>();
    private static final Set<Material> FARMLAND = new HashSet<>(Arrays.asList(
        Material.FARMLAND, Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT
    ));

    static {
        CROP_TO_SEED.put(Material.WHEAT, Material.WHEAT_SEEDS);
        CROP_TO_SEED.put(Material.CARROTS, Material.CARROT);
        CROP_TO_SEED.put(Material.POTATOES, Material.POTATO);
        CROP_TO_SEED.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        CROP_TO_SEED.put(Material.NETHER_WART, Material.NETHER_WART);
        CROP_TO_SEED.put(Material.MELON_STEM, Material.MELON_SEEDS);
        CROP_TO_SEED.put(Material.PUMPKIN_STEM, Material.PUMPKIN_SEEDS);
        CROPS.addAll(CROP_TO_SEED.keySet());
        CROPS.add(Material.SUGAR_CANE);
        CROPS.add(Material.CACTUS);
        CROPS.add(Material.COCOA);
        CROPS.add(Material.BAMBOO);
    }

    public FarmingManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(SmithNPC npc, String skill, Map<String, Object> params, Player contextPlayer) {
        Player fake = npc.getEntity() instanceof Player ? (Player) npc.getEntity() : null;
        if (fake == null) return;

        String lower = skill.toLowerCase();
        if (lower.contains("plant_") || lower.contains("sow_")) {
            plantCrops(fake, lower, params);
            return;
        }
        if (lower.contains("water_")) {
            waterCrops(fake);
            return;
        }
        if (lower.contains("fertilize_") || lower.contains("bone_meal") || lower.contains("grow_")) {
            fertilizeCrops(fake);
            return;
        }
        if (lower.contains("harvest_") || lower.contains("replant_")) {
            harvestAndReplant(fake);
            return;
        }
        if (lower.contains("farm_")) {
            // Full automation: till → plant → water → fertilize → harvest
            automatedFarm(fake);
            return;
        }
        if (lower.contains("breed_") || lower.contains("feed_")) {
            breedAnimals(fake, lower, params);
            return;
        }
    }

    private void plantCrops(Player fake, String skill, Map<String, Object> params) {
        PlayerInventory inv = fake.getInventory();
        Material seedType = null;
        if (skill.contains("wheat") || skill.contains("seed")) seedType = Material.WHEAT_SEEDS;
        else if (skill.contains("carrot")) seedType = Material.CARROT;
        else if (skill.contains("potato")) seedType = Material.POTATO;
        else if (skill.contains("beetroot")) seedType = Material.BEETROOT_SEEDS;
        else if (skill.contains("nether_wart") || skill.contains("wart")) seedType = Material.NETHER_WART;
        else if (skill.contains("melon")) seedType = Material.MELON_SEEDS;
        else if (skill.contains("pumpkin")) seedType = Material.PUMPKIN_SEEDS;
        else {
            // Try any seed in inventory
            for (Material m : CROP_TO_SEED.values()) {
                if (inv.contains(m)) { seedType = m; break; }
            }
        }
        if (seedType == null || !inv.contains(seedType)) {
            fake.sendMessage("§eI don't have seeds to plant.");
            return;
        }

        // Find a nearby farmland block
        Location loc = fake.getLocation();
        int planted = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                Block ground = loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY(), loc.getBlockZ() + dz);
                if (ground.getType() == Material.FARMLAND || ground.getType() == Material.SOUL_SAND) {
                    Block above = ground.getRelative(BlockFace.UP);
                    if (above.getType().isAir()) {
                        Material crop = getCropForSeed(seedType);
                        if (crop != null) {
                            above.setType(crop);
                            removeOneItem(fake, seedType);
                            planted++;
                            if (planted >= 8) break;
                        }
                    }
                }
            }
            if (planted >= 8) break;
        }
        fake.sendMessage("§aPlanted " + planted + " crops.");
    }

    private void waterCrops(Player fake) {
        ItemStack held = fake.getInventory().getItemInMainHand();
        boolean hasWaterBucket = false;
        for (ItemStack s : fake.getInventory().getContents()) {
            if (s != null && s.getType() == Material.WATER_BUCKET) {
                hasWaterBucket = true;
                fake.getInventory().setItemInMainHand(s);
                break;
            }
        }
        if (!hasWaterBucket) {
            fake.sendMessage("§eI need a water bucket to hydrate farmland.");
            return;
        }
        // Find farmland block and simulate hydration
        Location loc = fake.getLocation();
        int water = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                Block ground = loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY(), loc.getBlockZ() + dz);
                if (ground.getType() == Material.FARMLAND) {
                    water++;
                }
            }
        }
        fake.sendMessage("§aHydrated " + water + " farmland blocks.");
    }

    private void fertilizeCrops(Player fake) {
        if (!fake.getInventory().contains(Material.BONE_MEAL)) {
            fake.sendMessage("§eI need bone meal to fertilize.");
            return;
        }
        Location loc = fake.getLocation();
        int fertilized = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                Block crop = loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY(), loc.getBlockZ() + dz);
                if (CROPS.contains(crop.getType())) {
                    if (crop.getBlockData() instanceof Ageable) {
                        Ageable age = (Ageable) crop.getBlockData();
                        if (age.getAge() < age.getMaximumAge()) {
                            age.setAge(age.getMaximumAge());
                            crop.setBlockData(age);
                            removeOneItem(fake, Material.BONE_MEAL);
                            fertilized++;
                            if (fertilized >= 6) break;
                        }
                    }
                }
            }
            if (fertilized >= 6) break;
        }
        fake.sendMessage("§aFertilized " + fertilized + " crops.");
    }

    private void harvestAndReplant(Player fake) {
        Location loc = fake.getLocation();
        int harvested = 0;
        int replanted = 0;

        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                Block crop = loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY(), loc.getBlockZ() + dz);
                Material type = crop.getType();
                if (CROPS.contains(type)) {
                    // Check if mature
                    if (crop.getBlockData() instanceof Ageable) {
                        Ageable age = (Ageable) crop.getBlockData();
                        if (age.getAge() >= age.getMaximumAge()) {
                            // Harvest
                            crop.breakNaturally();
                            harvested++;
                            // Replant
                            Material seed = CROP_TO_SEED.getOrDefault(type, null);
                            if (seed != null && fake.getInventory().contains(seed)) {
                                crop.setType(type);
                                removeOneItem(fake, seed);
                                replanted++;
                            }
                        }
                    } else {
                        // Non-ageable crops (sugar cane, cactus)
                        Block above = crop.getRelative(BlockFace.UP);
                        if (above.getType() == type) {
                            above.breakNaturally();
                            harvested++;
                        }
                    }
                }
            }
        }
        fake.sendMessage("§aHarvested " + harvested + " crops. Replanted " + replanted + ".");
    }

    private void automatedFarm(Player fake) {
        // Full farm cycle
        // 1. Till dirt if present
        Location loc = fake.getLocation();
        int tilled = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                Block ground = loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY(), loc.getBlockZ() + dz);
                if (ground.getType() == Material.DIRT || ground.getType() == Material.GRASS_BLOCK) {
                    ground.setType(Material.FARMLAND);
                    tilled++;
                }
            }
        }

        // 2. Plant
        plantCrops(fake, "farm", null);

        // 3. Harvest mature
        harvestAndReplant(fake);

        fake.sendMessage("§aAutomated farm complete. Tilled " + tilled + " blocks, planted and harvested.");
    }

    private void breedAnimals(Player fake, String skill, Map<String, Object> params) {
        // Find nearby animals
        boolean fed = false;
        Material food = null;
        if (skill.contains("cow") || skill.contains("mooshroom")) food = Material.WHEAT;
        else if (skill.contains("sheep")) food = Material.WHEAT;
        else if (skill.contains("pig") || skill.contains("hoglin")) food = Material.CARROT;
        else if (skill.contains("chicken")) food = Material.WHEAT_SEEDS;
        else if (skill.contains("rabbit")) food = Material.CARROT;
        else if (skill.contains("wolf") || skill.contains("dog")) food = Material.BONE;
        else if (skill.contains("cat") || skill.contains("ocelot")) food = Material.COD;
        else if (skill.contains("horse")) food = Material.GOLDEN_APPLE;

        if (food != null && fake.getInventory().contains(food)) {
            removeOneItem(fake, food);
            fed = true;
        }

        if (fed) {
            fake.sendMessage("§aFed nearby animals. They may breed if two are nearby.");
        } else {
            fake.sendMessage("§eI need the right food to breed animals.");
        }
    }

    // ── UTILITY ──

    private Material getCropForSeed(Material seed) {
        for (Map.Entry<Material, Material> e : CROP_TO_SEED.entrySet()) {
            if (e.getValue() == seed) return e.getKey();
        }
        return null;
    }

    private void removeOneItem(Player player, Material mat) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == mat) {
                if (stack.getAmount() <= 1) inv.setItem(i, null);
                else stack.setAmount(stack.getAmount() - 1);
                return;
            }
        }
    }
}
