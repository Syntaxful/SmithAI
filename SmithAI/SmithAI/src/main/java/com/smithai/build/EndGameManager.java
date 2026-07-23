package com.smithai.build;

import com.smithai.SmithAIPlugin;
import com.smithai.npc.SmithNPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

/**
 * Endgame task sequences: nether portal creation, blaze rod farming,
 * eye of ender crafting, stronghold location, end portal activation, dragon fight.
 */
public class EndGameManager {

    private final SmithAIPlugin plugin;

    public EndGameManager(SmithAIPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(SmithNPC npc, String skill, Map<String, Object> params, Player contextPlayer) {
        Player fake = npc.getEntity() instanceof Player ? (Player) npc.getEntity() : null;
        if (fake == null) return;

        String lower = skill.toLowerCase();
        if (lower.contains("portal") || lower.contains("nether") || lower.contains("obsidian")) {
            buildNetherPortal(fake);
            return;
        }
        if (lower.contains("blaze") || lower.contains("rod") || lower.contains("fortress")) {
            blazeRodFarming(fake);
            return;
        }
        if (lower.contains("eye_of_ender") || lower.contains("eye") || lower.contains("ender_eye")) {
            craftEyeOfEnder(fake);
            return;
        }
        if (lower.contains("stronghold")) {
            locateStronghold(fake);
            return;
        }
        if (lower.contains("end_portal") || lower.contains("end_portal_frame") || lower.contains("dragon")) {
            endPortalAndDragon(fake);
            return;
        }
        if (lower.contains("wither")) {
            summonWither(fake);
            return;
        }
        if (lower.contains("elytra") || lower.contains("shulker")) {
            postDragonAcquisition(fake);
            return;
        }
    }

    /**
     * Build a nether portal: 4 wide × 5 tall obsidian frame, light with flint & steel.
     */
    public void buildNetherPortal(Player player) {
        PlayerInventory inv = player.getInventory();
        int obsidianCount = countMaterial(inv, Material.OBSIDIAN);
        boolean hasFlintSteel = inv.contains(Material.FLINT_AND_STEEL);

        if (obsidianCount < 10) {
            player.sendMessage("§eI need 10 obsidian for a nether portal (you have " + obsidianCount + "). Cast obsidian with water + lava.");
            return;
        }
        if (!hasFlintSteel) {
            player.sendMessage("§eI need flint and steel to light the portal. Craft: flint + iron ingot.");
            return;
        }

        Location loc = player.getLocation();
        World world = loc.getWorld();

        // Find a flat area 5 blocks wide
        int px = loc.getBlockX();
        int py = loc.getBlockY();
        int pz = loc.getBlockZ();

        // Build portal frame (4 wide, 5 tall, leave corners and center open)
        // Bottom row
        for (int dx = 0; dx < 4; dx++) {
            setAndDeduct(player, world.getBlockAt(px + dx, py, pz), Material.OBSIDIAN);
        }
        // Top row
        for (int dx = 0; dx < 4; dx++) {
            setAndDeduct(player, world.getBlockAt(px + dx, py + 4, pz), Material.OBSIDIAN);
        }
        // Left column
        setAndDeduct(player, world.getBlockAt(px, py + 1, pz), Material.OBSIDIAN);
        setAndDeduct(player, world.getBlockAt(px, py + 2, pz), Material.OBSIDIAN);
        setAndDeduct(player, world.getBlockAt(px, py + 3, pz), Material.OBSIDIAN);
        // Right column
        setAndDeduct(player, world.getBlockAt(px + 3, py + 1, pz), Material.OBSIDIAN);
        setAndDeduct(player, world.getBlockAt(px + 3, py + 2, pz), Material.OBSIDIAN);
        setAndDeduct(player, world.getBlockAt(px + 3, py + 3, pz), Material.OBSIDIAN);

        // Light the portal
        Block lightBlock = world.getBlockAt(px + 1, py + 1, pz);
        lightBlock.setType(Material.FIRE);
        player.sendMessage("§aNether portal built and lit! Step through to enter the Nether.");
    }

    /**
     * Blaze rod farming in the Nether fortress.
     */
    public void blazeRodFarming(Player player) {
        if (player.getWorld().getEnvironment() != World.Environment.NETHER) {
            player.sendMessage("§eI need to be in the Nether to find a fortress. Use the nether portal first.");
            return;
        }
        player.sendMessage("§aSearching for a Nether fortress...");
        player.sendMessage("§7Tip: Fortresses are usually along the North/South or East/West axis. Look for dark brown bridges.");
        player.sendMessage("§7Kill blazes for blaze rods. Craft blaze powder: 1 rod → 2 powder.");
    }

    /**
     * Craft eyes of ender: blaze powder + ender pearl.
     */
    public void craftEyeOfEnder(Player player) {
        PlayerInventory inv = player.getInventory();
        if (!inv.contains(Material.BLAZE_POWDER)) {
            player.sendMessage("§eI need blaze powder. Kill blazes in the Nether fortress and craft: 1 blaze rod → 2 blaze powder.");
            return;
        }
        if (!inv.contains(Material.ENDER_PEARL)) {
            player.sendMessage("§eI need ender pearls. Kill endermen at night or trade with villagers.");
            return;
        }
        // Simple craft: remove one of each, add eye of ender
        removeOneItem(player, Material.BLAZE_POWDER);
        removeOneItem(player, Material.ENDER_PEARL);
        player.getInventory().addItem(new ItemStack(Material.ENDER_EYE, 1));
        player.sendMessage("§aCrafted an eye of ender! Need " +
            (countMaterial(inv, Material.ENDER_EYE) >= 12 ? "enough for the portal." : "at least 12 to fill the end portal."));
    }

    /**
     * Locate stronghold instructions.
     */
    public void locateStronghold(Player player) {
        if (!player.getInventory().contains(Material.ENDER_EYE)) {
            player.sendMessage("§eI need an eye of ender to locate the stronghold. Throw it and follow where it floats.");
            return;
        }
        player.sendMessage("§aTo find the stronghold:");
        player.sendMessage("§71. Throw an eye of ender (right-click).");
        player.sendMessage("§72. Follow the direction it floats. It will lead you to the stronghold.");
        player.sendMessage("§73. Dig straight down when you're directly above it.");
        player.sendMessage("§74. Find the end portal room and fill the frames with eyes of ender.");
    }

    /**
     * End portal activation and dragon fight prep.
     */
    public void endPortalAndDragon(Player player) {
        PlayerInventory inv = player.getInventory();
        int eyes = countMaterial(inv, Material.ENDER_EYE);
        player.sendMessage("§6=== END DRAGON PREPARATION ===");
        if (eyes < 12) {
            player.sendMessage("§eNeed 12 eyes of ender to fill the portal frames (have " + eyes + ").");
            return;
        }
        player.sendMessage("§aReady for the End! You have " + eyes + " eyes of ender.");
        player.sendMessage("§6Fight strategy:");
        player.sendMessage("§7- Destroy the end crystals on top of obsidian pillars first (they heal the dragon).");
        player.sendMessage("§7- Use a bow to shoot the dragon when it flies.");
        player.sendMessage("§7- Use a sword when the dragon perches on the portal.");
        player.sendMessage("§7- Bring: good armor, bow & arrows, slow falling potions, golden apples.");
    }

    /**
     * Wither summoning.
     */
    public void summonWither(Player player) {
        PlayerInventory inv = player.getInventory();
        if (countMaterial(inv, Material.WITHER_SKELETON_SKULL) < 3) {
            player.sendMessage("§eNeed 3 wither skeleton skulls. Kill wither skeletons in Nether fortresses.");
            return;
        }
        if (countMaterial(inv, Material.SOUL_SAND) < 4 && countMaterial(inv, Material.SOUL_SOIL) < 4) {
            player.sendMessage("§eNeed 4 soul sand or soul soil. Find in the Nether soul sand valley.");
            return;
        }
        player.sendMessage("§cTo summon the wither:");
        player.sendMessage("§7Place soul sand in a T-shape (3 on bottom, 1 on top), place 3 skulls on top.");
        player.sendMessage("§7The wither explodes on spawn — run back, then shoot from distance with a bow.");
        player.sendMessage("§7Reward: Nether star (beacon crafting).");
    }

    /**
     * Post-dragon: elytra, shulker boxes, End City.
     */
    public void postDragonAcquisition(Player player) {
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) {
            player.sendMessage("§eI need to be in the End. Defeat the dragon first, then go through the end gateway portals.");
            return;
        }
        player.sendMessage("§aExploring End City:");
        player.sendMessage("§7- After killing the dragon, walk through the end gateway portal.");
        player.sendMessage("§7- Find an End City (tall tower with purple blocks).");
        player.sendMessage("§7- Elytra is in the End Ship (floating ship next to the city).");
        player.sendMessage("§7- Shulker boxes drop from shulkers. Use them for portable storage!");
    }

    // ── UTILITY ──

    private int countMaterial(PlayerInventory inv, Material mat) {
        int count = 0;
        for (ItemStack s : inv.getContents()) {
            if (s != null && s.getType() == mat) count += s.getAmount();
        }
        return count;
    }

    private void setAndDeduct(Player player, Block block, Material mat) {
        block.setType(mat);
        removeOneItem(player, mat);
    }

    private void removeOneItem(Player player, Material mat) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && stack.getType() == mat) {
                if (stack.getAmount() <= 1) inv.setItem(i, null);
                else stack.setAmount(stack.getAmount() - 1);
                return;
            }
        }
    }
}
