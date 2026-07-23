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
    /**
     * Apply a player-model appearance to the NPC using a player head and colored armor.
     * Eaglercraft-compatible since it uses only standard Bukkit APIs.
     */
    public void applyPlayerModelSkin(SmithNPC npc) {
        if (!(npc.getEntity() instanceof Player)) return;
        Player fake = (Player) npc.getEntity();

        // Player head with named skin
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setDisplayName("§b§l" + npc.getName());
            skullMeta.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(npc.getName()));
            head.setItemMeta(skullMeta);
        }
        fake.getInventory().setHelmet(head);

        // Blue-dyed leather chestplate
        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chest.getItemMeta();
        if (chestMeta != null) {
            chestMeta.setColor(org.bukkit.Color.fromRGB(60, 120, 200));
            chestMeta.setDisplayName("§9" + npc.getName() + "'s Chestplate");
            chest.setItemMeta(chestMeta);
        }
        fake.getInventory().setChestplate(chest);

        // Blue leggings
        ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta legMeta = (LeatherArmorMeta) legs.getItemMeta();
        if (legMeta != null) {
            legMeta.setColor(org.bukkit.Color.fromRGB(40, 90, 170));
            legMeta.setDisplayName("§9" + npc.getName() + "'s Leggings");
            legs.setItemMeta(legMeta);
        }
        fake.getInventory().setLeggings(legs);

        // Blue boots
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootMeta = (LeatherArmorMeta) boots.getItemMeta();
        if (bootMeta != null) {
            bootMeta.setColor(org.bukkit.Color.fromRGB(30, 70, 150));
            bootMeta.setDisplayName("§9" + npc.getName() + "'s Boots");
            boots.setItemMeta(bootMeta);
        }
        fake.getInventory().setBoots(boots);

        // Ensure visible and interactive
        fake.setInvisible(false);
        fake.setCollidable(true);
    }

    /**
     * Legacy robot skin — delegates to player model.
     */
    public void applyRobotSkin(SmithNPC npc) {
        applyPlayerModelSkin(npc);
    }

    /**
     * Set a player head as the NPC's helmet for face display.
     */
    public void setPlayerHead(SmithNPC npc, String skinUrl) {
        applyPlayerModelSkin(npc);
    }

    /**
     * Map an animation state to visual/equipment changes on the NPC.
     * Supported states: IDLE, WALKING, MINING, FIGHTING
     */
    public void setAnimationState(SmithNPC npc, String state) {
        if (!(npc.getEntity() instanceof Player)) return;
        Player fake = (Player) npc.getEntity();
        String s = state.toUpperCase();

        // Reset pose defaults
        fake.setSneaking(false);
        fake.setSwimming(false);

        switch (s) {
            case "MINING":
                ItemStack pick = new ItemStack(Material.IRON_PICKAXE);
                ItemMeta pickMeta = pick.getItemMeta();
                if (pickMeta != null) { pickMeta.setDisplayName("§7Mining Pick"); pick.setItemMeta(pickMeta); }
                fake.getInventory().setItemInMainHand(pick);
                fake.getInventory().setItemInOffHand(null);
                fake.setSneaking(true);
                fake.getWorld().playSound(fake.getLocation(), org.bukkit.Sound.BLOCK_STONE_HIT, 0.5f, 1.0f);
                break;
            case "FIGHTING":
                ItemStack sword = new ItemStack(Material.IRON_SWORD);
                ItemMeta swordMeta = sword.getItemMeta();
                if (swordMeta != null) { swordMeta.setDisplayName("§cCombat Sword"); sword.setItemMeta(swordMeta); }
                fake.getInventory().setItemInMainHand(sword);
                ItemStack shield = new ItemStack(Material.SHIELD);
                ItemMeta shieldMeta = shield.getItemMeta();
                if (shieldMeta != null) { shieldMeta.setDisplayName("§7Shield"); shield.setItemMeta(shieldMeta); }
                fake.getInventory().setItemInOffHand(shield);
                break;
            case "WALKING":
                // Walking pose: empty hands, normal stance
                fake.getInventory().setItemInMainHand(null);
                fake.getInventory().setItemInOffHand(null);
                break;
            case "IDLE":
            default:
                fake.getInventory().setItemInMainHand(null);
                fake.getInventory().setItemInOffHand(null);
                break;
        }
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
