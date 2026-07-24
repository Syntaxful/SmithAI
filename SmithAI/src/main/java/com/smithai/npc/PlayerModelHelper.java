package com.smithai.npc;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Optional Citizens support that lets Smith_AI spawn as a real player-model NPC.
 * Entirely reflection-based so Citizens remains optional: if not installed,
 * Smith_AI falls back to a villager with a custom name.
 */
public class PlayerModelHelper {

    private static final boolean CITIZENS_AVAILABLE;
    private static Object registry;
    private static Method createNPCMethod;
    private static Method npcDestroyMethod;
    private static Method npcGetEntityMethod;
    private static Method npcSpawnMethod;
    private static Method npcSetProtectedMethod;
    private static Method npcSetUseMinecraftAIMethod;
    private static Method npcFaceLocationMethod;
    private static Method npcGetNavigatorMethod;
    private static Method navigatorSetTargetLocMethod;
    private static Method navigatorSetTargetEntityMethod;
    private static Method navigatorCancelMethod;
    private static Method navigatorDefaultParamsMethod;
    private static Method paramsSpeedMethod;

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
            npcGetNavigatorMethod = npcClass.getMethod("getNavigator");
            npcFaceLocationMethod = npcClass.getMethod("faceLocation", Location.class);

            try { npcSetProtectedMethod = npcClass.getMethod("setProtected", boolean.class); } catch (Exception ignored) {}
            try { npcSetUseMinecraftAIMethod = npcClass.getMethod("setUseMinecraftAI", boolean.class); } catch (Exception ignored) {}

            Class<?> navigatorClass = Class.forName("net.citizensnpcs.api.ai.Navigator");
            Class<?> paramsClass = Class.forName("net.citizensnpcs.api.ai.NavigatorParameters");
            navigatorSetTargetLocMethod = navigatorClass.getMethod("setTarget", Location.class);
            navigatorSetTargetEntityMethod = navigatorClass.getMethod("setTarget", Entity.class, boolean.class);
            navigatorCancelMethod = navigatorClass.getMethod("cancelNavigation");
            navigatorDefaultParamsMethod = navigatorClass.getMethod("getDefaultParameters");
            paramsSpeedMethod = paramsClass.getMethod("speed", float.class);

            available = true;
        } catch (Exception e) {
            // Citizens is not present or incompatible. Fall back to villager.
        }
        CITIZENS_AVAILABLE = available;
    }

    public static boolean isAvailable() { return CITIZENS_AVAILABLE; }

    public static Entity spawnPlayer(Location location, String name) {
        if (!CITIZENS_AVAILABLE) return null;
        try {
            Object npc = createNPCMethod.invoke(registry, EntityType.PLAYER, name);
            if (npcSetProtectedMethod != null) npcSetProtectedMethod.invoke(npc, true);
            if (npcSetUseMinecraftAIMethod != null) npcSetUseMinecraftAIMethod.invoke(npc, false);
            if ((Boolean) npcSpawnMethod.invoke(npc, location)) {
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
            log(Level.WARNING, "Failed to spawn Citizens player-model NPC", e);
        }
        return null;
    }

    public static Object getCitizensHandle(Entity entity) {
        if (entity == null || !entity.hasMetadata("smithai_citizens_npc")) return null;
        return entity.getMetadata("smithai_citizens_npc").get(0).value();
    }

    public static void destroyCitizensNpc(Entity entity) {
        Object handle = getCitizensHandle(entity);
        if (handle != null) {
            try { npcDestroyMethod.invoke(handle); } catch (Exception e) { entity.remove(); }
        } else {
            entity.remove();
        }
    }

    public static void setNavigatorSpeed(Object handle, float speed) {
        if (handle == null || !CITIZENS_AVAILABLE) return;
        try {
            Object navigator = npcGetNavigatorMethod.invoke(handle);
            Object params = navigatorDefaultParamsMethod.invoke(navigator);
            paramsSpeedMethod.invoke(params, speed);
        } catch (Exception ignored) {}
    }

    public static void navigateTo(Object handle, Location target) {
        if (handle == null || !CITIZENS_AVAILABLE) return;
        try {
            Object navigator = npcGetNavigatorMethod.invoke(handle);
            navigatorSetTargetLocMethod.invoke(navigator, target);
        } catch (Exception ignored) {}
    }

    public static void followEntity(Object handle, Entity target) {
        if (handle == null || !CITIZENS_AVAILABLE) return;
        try {
            Object navigator = npcGetNavigatorMethod.invoke(handle);
            navigatorSetTargetEntityMethod.invoke(navigator, target, false);
        } catch (Exception ignored) {}
    }

    public static void cancelNavigation(Object handle) {
        if (handle == null || !CITIZENS_AVAILABLE) return;
        try { navigatorCancelMethod.invoke(npcGetNavigatorMethod.invoke(handle)); } catch (Exception ignored) {}
    }

    public static void faceLocation(Object handle, Location target) {
        if (handle == null || !CITIZENS_AVAILABLE) return;
        try { npcFaceLocationMethod.invoke(handle, target); } catch (Exception ignored) {}
    }

    private static void log(Level level, String msg, Throwable t) {
        com.smithai.SmithAIPlugin inst = com.smithai.SmithAIPlugin.getInstance();
        if (inst != null) inst.getLogger().log(level, msg, t); else t.printStackTrace();
    }
}
