package com.smithai.util;

import org.bukkit.Material;

import java.lang.reflect.Method;

/**
 * Version-safe material helpers so SmithAI loads on Minecraft 1.12 through 1.21.x.
 * Never reference Material enum constants that were added after 1.12 directly;
 * always look them up by name so the class can load on legacy servers.
 */
public class MaterialCompat {

    private static Method materialIsAir;
    private static Method materialIsSolid;
    private static Method materialIsBlock;
    private static Method materialIsEdible;
    private static boolean reflectionChecked = false;

    /**
     * Try every name until one matches the current server's Material registry.
     * Names should be ordered from newest to oldest so the best available is used.
     */
    public static Material get(String... names) {
        if (names == null) return null;
        for (String name : names) {
            if (name == null || name.isEmpty()) continue;
            Material mat = Material.matchMaterial(name);
            if (mat != null) return mat;
        }
        return null;
    }

    public static boolean isAir(Material type) {
        if (type == null) return true;
        if (type == Material.AIR) return true;
        Material caveAir = get("CAVE_AIR", "LEGACY_AIR");
        if (caveAir != null && type == caveAir) return true;
        Material voidAir = get("VOID_AIR");
        if (voidAir != null && type == voidAir) return true;
        return invokeBoolean(type, materialIsAir);
    }

    public static boolean isSolid(Material type) {
        if (type == null) return false;
        return invokeBoolean(type, materialIsSolid);
    }

    public static boolean isBlock(Material type) {
        if (type == null) return false;
        if (type == Material.AIR) return false;
        return invokeBoolean(type, materialIsBlock);
    }

    public static boolean isEdible(Material type) {
        if (type == null) return false;
        return invokeBoolean(type, materialIsEdible);
    }

    private static boolean invokeBoolean(Material type, Method method) {
        ensureReflection();
        if (method == null) return false;
        try {
            return (Boolean) method.invoke(type);
        } catch (Exception e) {
            return false;
        }
    }

    private static synchronized void ensureReflection() {
        if (reflectionChecked) return;
        reflectionChecked = true;
        try { materialIsAir = Material.class.getMethod("isAir"); } catch (Exception ignored) {}
        try { materialIsSolid = Material.class.getMethod("isSolid"); } catch (Exception ignored) {}
        try { materialIsBlock = Material.class.getMethod("isBlock"); } catch (Exception ignored) {}
        try { materialIsEdible = Material.class.getMethod("isEdible"); } catch (Exception ignored) {}
    }
}
