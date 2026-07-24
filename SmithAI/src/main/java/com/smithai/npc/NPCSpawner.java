package com.smithai.npc;

import com.smithai.SmithAIPlugin;
import com.smithai.util.LivingEntityCompat;
import com.smithai.util.MaterialCompat;
import com.smithai.util.VillagerCompat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class NPCSpawner {

    public static SmithNPC spawn(SmithAIPlugin plugin, Location location) {
        String name = plugin.getPluginConfig().getAiName();
        Entity entity = PlayerModelHelper.spawnPlayer(location, name);
        if (entity != null) {
            entity.setCustomName(name);
            entity.setCustomNameVisible(true);
            // Do NOT call setSilent(true) here: PlayerModelHelper already sets silent(false)
            // so the NPC is audible and visible as required.
            entity.setInvulnerable(true);
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (plugin.getPluginConfig().isSpawnEquipped()) {
                    equipPlayer(player);
                }
                // Force the equipment update packet so armor/offhand is visible to clients.
                updateEquipment(player);
                return new SmithNPC(UUID.randomUUID(), entity, name, new NPCInventory(player));
            }
            return new SmithNPC(UUID.randomUUID(), entity, name, new NPCInventory());
        }

        plugin.getLogger().info("Citizens not found; spawning Smith_AI as a villager fallback.");
        Entity fallback = location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        if (fallback instanceof Villager) {
            Villager villager = (Villager) fallback;
            LivingEntityCompat.setAI(villager, false);
            VillagerCompat.setAware(villager, false);
            villager.setCustomName(name);
            villager.setCustomNameVisible(true);
            villager.setSilent(true);
            villager.setInvulnerable(true);
            VillagerCompat.setNoProfession(villager);
        }
        if (fallback instanceof LivingEntity) LivingEntityCompat.setAI((LivingEntity) fallback, false);
        return new SmithNPC(UUID.randomUUID(), fallback, name, new NPCInventory());
    }

    private static void equipPlayer(Player player) {
        NPCInventory inv = new NPCInventory(player);
        inv.setArmor(item("IRON_HELMET"), item("IRON_CHESTPLATE"), item("IRON_LEGGINGS"), item("IRON_BOOTS"));
        inv.setMainHand(item("DIAMOND_SWORD"));
        inv.setOffHand(item("SHIELD"));
    }

    private static void updateEquipment(Player player) {
        try {
            // Refresh the equipment update for nearby players by resetting the held item slot.
            player.getInventory().setHeldItemSlot(0);
            // Bukkit/Spigot sends the equipment packet automatically when inventory changes.
            // Forcing a small update by setting the hand again ensures the offhand is visible.
            ItemStack offHand = player.getInventory().getItemInOffHand();
            player.getInventory().setItemInOffHand(null);
            player.getInventory().setItemInOffHand(offHand);
        } catch (Exception ignored) {}
    }

    private static ItemStack item(String name) {
        Material mat = MaterialCompat.get(name);
        return mat != null ? new ItemStack(mat, 1) : null;
    }
}
