package com.smithai.npc;

import com.smithai.SmithAIPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * NPC visual enhancements: nametag, hologram, robot skin, damage/health/death.
 */
public class NPCMesh {

    private final SmithAIPlugin plugin;

    public NPCMesh(SmithAIPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Set a custom nametag above the NPC visible to all players.
     */
    public void setNameTag(SmithNPC npc, String displayName) {
        Entity entity = npc.getEntity();
        if (entity == null) return;
        entity.setCustomName(formatName(displayName));
        entity.setCustomNameVisible(true);
    }

    /**
     * Show a temporary hologram-style floating message.
     * Uses a simple delayed nametag swap since hologram entities need a separate entity.
     */
    public void showSpeechBubble(SmithNPC npc, String message) {
        Entity entity = npc.getEntity();
        if (entity == null) return;
        String original = entity.getCustomName();
        entity.setCustomName("§f" + message);
        entity.setCustomNameVisible(true);
        // Revert after 2.5 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (entity != null && !entity.isDead()) {
                entity.setCustomName(original != null ? original : formatName(npc.getName()));
                entity.setCustomNameVisible(true);
            }
        }, 50L);
    }

    /**
     * Apply the robot skin to the NPC.
     * Uses armor stands with player heads / leather armor colored gray.
     */
    public void applyRobotSkin(SmithNPC npc) {
        if (!(npc.getEntity() instanceof Player)) return;
        Player fake = (Player) npc.getEntity();

        // Robot helmet (gray dyed leather cap)
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
        if (meta != null) {
            meta.setColor(org.bukkit.Color.fromRGB(100, 100, 100));
            meta.setDisplayName("§7Robot Head");
            helmet.setItemMeta(meta);
        }
        fake.getInventory().setHelmet(helmet);

        // Gray chestplate
        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chest.getItemMeta();
        if (chestMeta != null) {
            chestMeta.setColor(org.bukkit.Color.fromRGB(80, 80, 80));
            chestMeta.setDisplayName("§7Robot Body");
            chest.setItemMeta(chestMeta);
        }
        fake.getInventory().setChestplate(chest);

        // Gray leggings
        ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta legMeta = (LeatherArmorMeta) legs.getItemMeta();
        if (legMeta != null) {
            legMeta.setColor(org.bukkit.Color.fromRGB(70, 70, 70));
            legMeta.setDisplayName("§7Robot Legs");
            legs.setItemMeta(legMeta);
        }
        fake.getInventory().setLeggings(legs);

        // Gray boots
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootMeta = (LeatherArmorMeta) boots.getItemMeta();
        if (bootMeta != null) {
            bootMeta.setColor(org.bukkit.Color.fromRGB(60, 60, 60));
            bootMeta.setDisplayName("§7Robot Feet");
            boots.setItemMeta(bootMeta);
        }
        fake.getInventory().setBoots(boots);
    }

    /**
     * Set a player head as the NPC's helmet for face display.
     */
    public void setPlayerHead(SmithNPC npc, String skinUrl) {
        if (!(npc.getEntity() instanceof Player)) return;
        Player fake = (Player) npc.getEntity();
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(fake);
            meta.setDisplayName("§b" + npc.getName());
            skull.setItemMeta(meta);
        }
        fake.getInventory().setHelmet(skull);
    }

    // ── DAMAGE / HEALTH / DEATH / RESPAWN ──

    /**
     * Apply damage to the NPC entity.
     */
    public void damage(SmithNPC npc, double amount) {
        Entity entity = npc.getEntity();
        if (entity instanceof org.bukkit.entity.Damageable) {
            org.bukkit.entity.Damageable d = (org.bukkit.entity.Damageable) entity;
            d.damage(amount);
        }
    }

    /**
     * Set NPC health.
     */
    public void setHealth(SmithNPC npc, double health) {
        Entity entity = npc.getEntity();
        if (entity instanceof org.bukkit.entity.Damageable) {
            org.bukkit.entity.Damageable d = (org.bukkit.entity.Damageable) entity;
            d.setHealth(Math.max(0, Math.min(health, d.getMaxHealth())));
        }
    }

    /**
     * Get NPC health.
     */
    public double getHealth(SmithNPC npc) {
        Entity entity = npc.getEntity();
        if (entity instanceof org.bukkit.entity.Damageable) {
            return ((org.bukkit.entity.Damageable) entity).getHealth();
        }
        return 20.0;
    }

    /**
     * Check if NPC is dead.
     */
    public boolean isDead(SmithNPC npc) {
        return npc.getEntity() == null || npc.getEntity().isDead();
    }

    /**
     * Respawn NPC at a location with full health.
     */
    public void respawn(SmithNPC npc, Location location, String name) {
        Entity old = npc.getEntity();
        if (old != null && !old.isDead()) {
            old.remove();
        }
        SmithNPC newNpc = plugin.getNpcManager().spawn(location);
        if (newNpc != null) {
            setNameTag(newNpc, name);
            applyRobotSkin(newNpc);
        }
    }

    // ── UTILITY ──

    private String formatName(String name) {
        return "§b§l" + name + " §7[Smith_AI]";
    }
}
