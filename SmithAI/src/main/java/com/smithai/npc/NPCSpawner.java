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
import org.bukkit.inventory.PlayerInventory;

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
            if (entity instanceof Player) equipPlayer((Player) entity);
            return new SmithNPC(UUID.randomUUID(), entity, name);
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
        return new SmithNPC(UUID.randomUUID(), fallback, name);
    }

    private static void equipPlayer(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setHelmet(item("IRON_HELMET"));
        inv.setChestplate(item("IRON_CHESTPLATE"));
        inv.setLeggings(item("IRON_LEGGINGS"));
        inv.setBoots(item("IRON_BOOTS"));
        inv.setItemInMainHand(item("DIAMOND_SWORD"));
        inv.setItemInOffHand(item("SHIELD"));
    }

    private static ItemStack item(String name) {
        Material mat = MaterialCompat.get(name);
        return mat != null ? new ItemStack(mat, 1) : null;
    }
}
