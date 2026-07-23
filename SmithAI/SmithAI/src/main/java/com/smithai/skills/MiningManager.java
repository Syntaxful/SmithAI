package com.smithai.skills;

import com.smithai.SmithAIPlugin;
import com.smithai.npc.SmithNPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

/**
 * Safe mining operations: 1x2 strip mine, ladder descent, hazard avoidance.
 */
public class MiningManager {

    private final SmithAIPlugin plugin;
    private static final Set<Material> UNAFFECTED = new HashSet<>(Arrays.asList(
        Material.AIR, Material.WATER, Material.LAVA, Material.BEDROCK, Material.SPAWNER
    ));
    private static final Set<Material> ORES = new HashSet<>(Arrays.asList(
        Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE,
        Material.EMERALD_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE, Material.COPPER_ORE,
        Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE,
        Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_COPPER_ORE, Material.ANCIENT_DEBRIS,
        Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE
    ));

    public MiningManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(SmithNPC npc, String skill, Map<String, Object> params, Player contextPlayer) {
        Player fake = npc.getEntity() instanceof Player ? (Player) npc.getEntity() : null;
        if (fake == null) return;

        String lower = skill.toLowerCase();
        if (lower.contains("strip_mine_safe") || lower.contains("strip_mine") || lower.contains("stripmining") || lower.contains("strip_mining")) {
            stripMine(fake, params);
            return;
        }
        if (lower.contains("ladder_down") || lower.contains("descend") || lower.contains("mine_shaft") || lower.contains("vertical_mine")) {
            digLadderShaft(fake);
            return;
        }
        if (lower.contains("branch_mine") || lower.contains("branch_mining")) {
            branchMine(fake);
            return;
        }
        if (lower.contains("mine_diamond") || lower.contains("find_diamond") || lower.contains("diamond_mine")) {
            mineDiamonds(fake);
            return;
        }
    }

    /**
     * Safe 1x2 strip mine: mine a 2-high 1-wide tunnel, avoid lava.
     * The NPC mines blocks in front and above, checking for hazards.
     */
    private void stripMine(Player fake, Map<String, Object> params) {
        Location loc = fake.getLocation();
        int length = 20;
        if (params != null && params.containsKey("length")) {
            try { length = ((Number) params.get("length")).intValue(); } catch (Exception ignored) {}
        }

        int mined = 0;
        int lavaAvoided = 0;

        for (int step = 0; step < length; step++) {
            Block ahead = loc.getBlock().getRelative(fake.getFacing(), step + 1);
            Block above = ahead.getRelative(BlockFace.UP);
            Block ahead2 = ahead.getRelative(BlockFace.UP); // same as above

            // Check for lava
            if (isLava(ahead) || isLava(above)) {
                lavaAvoided++;
                // Place torch on wall if we have one
                placeTorchIfAvailable(fake, loc.getBlock().getRelative(BlockFace.UP));
                break;
            }

            // Mine blocks
            if (mineBlock(fake, ahead)) mined++;
            if (mineBlock(fake, above)) mined++;

            // Move forward
            loc = loc.add(fake.getFacing().getDirection().normalize());

            // Place torch every 5 blocks on left wall
            if (step > 0 && step % 5 == 0) {
                Block left = loc.getBlock().getRelative(getLeftFace(fake), 1).getRelative(BlockFace.UP);
                if (left.getType().isAir() || !left.getType().isSolid()) {
                    placeTorchIfAvailable(fake, left.getRelative(BlockFace.DOWN));
                }
            }
        }

        fake.sendMessage("§aStrip mined " + mined + " blocks. " +
            (lavaAvoided > 0 ? "§eAvoided lava " + lavaAvoided + " time(s)." : "§aNo lava encountered."));
    }

    /**
     * Dig a 1x1 vertical shaft with ladders.
     */
    private void digLadderShaft(Player fake) {
        Location loc = fake.getLocation();
        boolean hasLadders = fake.getInventory().contains(Material.LADDER);
        int dug = 0;

        for (int depth = 0; depth < 20; depth++) {
            Block below = loc.getBlock().getRelative(BlockFace.DOWN);
            if (below.getType() == Material.BEDROCK || below.getType() == Material.LAVA) break;
            if (below.getType() == Material.AIR) {
                loc = below.getLocation();
                continue;
            }
            below.breakNaturally();
            dug++;
            loc = below.getLocation();

            // Place ladder on north wall
            if (hasLadders && depth > 0 && depth % 2 == 0) {
                Block northWall = loc.getBlock().getRelative(BlockFace.NORTH);
                if (northWall.getType().isAir() || !northWall.getType().isSolid()) {
                    northWall.setType(Material.LADDER);
                    // Rotate ladder
                    removeOneItem(fake, Material.LADDER);
                }
            }
        }
        fake.sendMessage("§aDug " + dug + " blocks down. " +
            (hasLadders ? "Placed ladders." : "No ladders available."));
    }

    /**
     * Branch mine: dig main corridor, then side branches every 3 blocks.
     */
    private void branchMine(Player fake) {
        // First dig main corridor (strip mine)
        Location loc = fake.getLocation();
        int total = 0;

        // Main tunnel
        for (int step = 0; step < 15; step++) {
            Block ahead = loc.getBlock().getRelative(fake.getFacing(), step + 1);
            Block above = ahead.getRelative(BlockFace.UP);
            if (!isLava(ahead) && !isLava(above)) {
                if (mineBlock(fake, ahead)) total++;
                if (mineBlock(fake, above)) total++;
            }
        }

        // Side branches every 3 blocks (left and right)
        BlockFace left = getLeftFace(fake);
        BlockFace right = getRightFace(fake);
        for (int step = 3; step < 15; step += 3) {
            Block center = loc.getBlock().getRelative(fake.getFacing(), step);
            // Left branch (3 deep)
            for (int b = 0; b < 3; b++) {
                Block side = center.getRelative(left, b + 1);
                Block sideAbove = side.getRelative(BlockFace.UP);
                if (!isLava(side) && mineBlock(fake, side)) total++;
                if (!isLava(sideAbove) && mineBlock(fake, sideAbove)) total++;
            }
            // Right branch (3 deep)
            for (int b = 0; b < 3; b++) {
                Block side = center.getRelative(right, b + 1);
                Block sideAbove = side.getRelative(BlockFace.UP);
                if (!isLava(side) && mineBlock(fake, side)) total++;
                if (!isLava(sideAbove) && mineBlock(fake, sideAbove)) total++;
            }
        }

        fake.sendMessage("§aBranch mined " + total + " blocks total. Check cave walls for exposed ores!");
    }

    /**
     * Diamond-targeted mining at Y=-59 (deepslate level).
     */
    private void mineDiamonds(Player fake) {
        Location loc = fake.getLocation();
        // Move to Y=-59 if not already there
        if (loc.getBlockY() > -59) {
            fake.sendMessage("§eMoving to deepslate level Y=-59 for diamond mining...");
            fake.teleport(new Location(loc.getWorld(), loc.getX(), -59, loc.getZ()));
        }
        // Use branch mine at Y=-59
        branchMine(fake);
        fake.sendMessage("§bStrategy: Diamonds are most common at Y=-59 in deepslate. Use an iron+ pickaxe!");
    }

    // ── UTILITY ──

    private boolean mineBlock(Player fake, Block block) {
        if (block == null || UNAFFECTED.contains(block.getType())) return false;
        if (block.getType() == Material.BEDROCK) return false;
        block.breakNaturally();
        return true;
    }

    private boolean isLava(Block block) {
        return block.getType() == Material.LAVA || block.getType() == Material.MAGMA_BLOCK;
    }

    private void placeTorchIfAvailable(Player fake, Block target) {
        if (fake.getInventory().contains(Material.TORCH) && target.getType().isSolid()) {
            Block above = target.getRelative(BlockFace.UP);
            if (above.getType().isAir()) {
                above.setType(Material.TORCH);
                removeOneItem(fake, Material.TORCH);
            }
        }
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

    private BlockFace getLeftFace(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        if (yaw < 45 || yaw >= 315) return BlockFace.EAST;
        if (yaw < 135) return BlockFace.NORTH;
        if (yaw < 225) return BlockFace.WEST;
        return BlockFace.SOUTH;
    }

    private BlockFace getRightFace(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        if (yaw < 45 || yaw >= 315) return BlockFace.WEST;
        if (yaw < 135) return BlockFace.SOUTH;
        if (yaw < 225) return BlockFace.EAST;
        return BlockFace.NORTH;
    }
}
