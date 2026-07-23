package com.smithai.npc;

import com.smithai.SmithAIPlugin;
import com.smithai.util.LivingEntityCompat;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;

import java.util.UUID;

public class NPCSpawner {

    public static SmithNPC spawn(SmithAIPlugin plugin, Location location) {
        String name = plugin.getPluginConfig().getAiName();

        // Prefer Citizens for a real player-model NPC.
        Entity entity = PlayerModelHelper.spawnPlayer(location, name);
        if (entity != null) {
            entity.setCustomName(name);
            entity.setCustomNameVisible(true);
            entity.setSilent(true);
            entity.setInvulnerable(true);
            return new SmithNPC(UUID.randomUUID(), entity, name);
        }

        // Fallback: villager with disabled AI.
        plugin.getLogger().info("Citizens not found; spawning Smith_AI as a villager fallback.");
        Entity fallback = location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        if (fallback instanceof Villager) {
            Villager villager = (Villager) fallback;
            LivingEntityCompat.setAI(villager, false);
            com.smithai.util.VillagerCompat.setAware(villager, false);
            villager.setCustomName(name);
            villager.setCustomNameVisible(true);
            villager.setSilent(true);
            villager.setInvulnerable(true);
            com.smithai.util.VillagerCompat.setNoProfession(villager);
        }
        if (fallback instanceof LivingEntity) {
            LivingEntityCompat.setAI((LivingEntity) fallback, false);
        }

        return new SmithNPC(UUID.randomUUID(), fallback, name);
    }
}
