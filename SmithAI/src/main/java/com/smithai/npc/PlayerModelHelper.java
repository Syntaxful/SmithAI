package com.smithai.npc;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Optional Citizens support that lets Smith_AI spawn as a real player-model NPC.
 * This is entirely reflection-based so the plugin remains optional: if Citizens
 * is not installed, Smith_AI falls back to a villager with a custom name.
 */
public class PlayerModelHelper {

    private static final boolean CITIZENS_AVAILABLE;
    private static Object registry;
    private static Method createNPCMethod;
    private static Method npcSpawnMethod;
    private static Method npcGetEntityMethod;
    private static Method npcDestroyMethod;
    private static Method npcSetProtectedMethod;
    private static Method npcSetUseMinecraftAIMethod;
    private static Method npcDataSetterMethod;
    private static Method npcDataSetterValueMethod;

    static {
        boolean available = false;
        try {
            Class<?> citizensAPIClass = Class.forName("net.citizensnpcs.api.CitizensAPI");
            registry = citizensAPIClass.getMethod("getNPCRegistry").invoke(null);
            createNPCMethod = registry.getClass().getMethod("createNPC", EntityType.class, String.class);

            Class<?> npcClass = Class.forName("net.citizensnpcs.api.npc.NPC");
            npcSpawnMethod = npcClass.getMethod("spawn", Location.class);
            npcGetEntityMethod = npcClass.getMethod("getEntity");
            npcDestroyMethod = npcClass.getMethod("destroy");

            // Optional: protected + use Minecraft AI. Older Citizens builds (e.g., 2.0.22) do not have setUseMinecraftAI.
            try { npcSetProtectedMethod = npcClass.getMethod("setProtected", boolean.class); } catch (Exception ignored) {}
            try { npcSetUseMinecraftAIMethod = npcClass.getMethod("setUseMinecraftAI", boolean.class); } catch (Exception ignored) {}

            // Optional: metadata helpers. Class is MetadataStore in older builds, NPC.Data in newer ones.
            try {
                Class<?> dataClass = Class.forName("net.citizensnpcs.api.npc.NPC$Data");
                npcDataSetterMethod = npcClass.getMethod("data");
                npcDataSetterValueMethod = dataClass.getMethod("set", String.class, Object.class);
            } catch (Exception ignored) {
                try {
                    Class<?> dataClass = Class.forName("net.citizensnpcs.api.npc.MetadataStore");
                    npcDataSetterMethod = npcClass.getMethod("data");
                    npcDataSetterValueMethod = dataClass.getMethod("set", String.class, Object.class);
                } catch (Exception ignored2) {}
            }

            available = true;
        } catch (Exception e) {
            // Citizens is not present or an incompatible version. Fall back to villager.
        }
        CITIZENS_AVAILABLE = available;
    }

    public static boolean isAvailable() {
        return CITIZENS_AVAILABLE;
    }

    /**
     * Spawn a player-model NPC at the given location.
     * Returns the Bukkit entity, or null if Citizens is unavailable or spawn fails.
     * The returned NPC handle is stored as a tag on the entity so SmithNPC can destroy it later.
     */
    public static Entity spawnPlayer(Location location, String name) {
        if (!CITIZENS_AVAILABLE) return null;
        try {
            Object npc = createNPCMethod.invoke(registry, EntityType.PLAYER, name);
            if (npcSetProtectedMethod != null) {
                npcSetProtectedMethod.invoke(npc, true);
            }
            if (npcSetUseMinecraftAIMethod != null) {
                npcSetUseMinecraftAIMethod.invoke(npc, false);
            }
            if (npcDataSetterMethod != null && npcDataSetterValueMethod != null) {
                try {
                    Object data = npcDataSetterMethod.invoke(npc);
                    npcDataSetterValueMethod.invoke(data, "nameplate", true);
                    npcDataSetterValueMethod.invoke(data, "persist", false);
                } catch (Exception ignored) {
                }
            }
            boolean spawned = (Boolean) npcSpawnMethod.invoke(npc, location);
            if (spawned) {
                Entity entity = (Entity) npcGetEntityMethod.invoke(npc);
                if (entity != null) {
                    entity.setSilent(true);
                    entity.setInvulnerable(true);
                    entity.setMetadata("smithai_citizens_npc", new org.bukkit.metadata.FixedMetadataValue(
                            com.smithai.SmithAIPlugin.getInstance(), npc));
                }
                return entity;
            }
        } catch (Exception e) {
            if (com.smithai.SmithAIPlugin.getInstance() != null) {
                com.smithai.SmithAIPlugin.getInstance().getLogger().log(Level.WARNING,
                        "Failed to spawn Citizens player-model NPC, falling back to villager", e);
            }
        }
        return null;
    }

    /**
     * Retrieve the Citizens NPC handle stored on an entity (if any).
     */
    public static Object getCitizensHandle(Entity entity) {
        if (entity == null || !entity.hasMetadata("smithai_citizens_npc")) return null;
        return entity.getMetadata("smithai_citizens_npc").get(0).value();
    }
}
