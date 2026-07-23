package com.smithai.npc;

import com.smithai.SmithAIPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

import java.util.UUID;

public class NPCSpawner {

    public static SmithNPC spawn(SmithAIPlugin plugin, Location location) {
        String name = plugin.getPluginConfig().getAiName();

        // Placeholder: use a villager with AI disabled.
        // In a full implementation, this would use ProtocolLib or Citizens API
        // to spawn a player-model entity with limbs and a robot skin.
        Entity entity = location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            villager.setAI(false);
            villager.setAware(false);
            villager.setCustomName(name);
            villager.setCustomNameVisible(true);
            villager.setSilent(true);
            villager.setInvulnerable(true);
            villager.setProfession(Villager.Profession.NONE);
        }

        return new SmithNPC(UUID.randomUUID(), entity, name);
    }
}
