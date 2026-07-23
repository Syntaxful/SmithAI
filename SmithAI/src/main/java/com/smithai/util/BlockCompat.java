package com.smithai.util;

import org.bukkit.block.Block;

import java.lang.reflect.Method;

/**
 * Version-safe block helpers so SmithAI loads on Minecraft 1.12 through 1.21.x.
 * Block.isPassable() was added in 1.13; on older servers we fall back to
 * Material.isSolid() (which exists in both old and new Bukkit).
 */
public class BlockCompat {

    private static Method blockIsPassable;
    private static boolean reflectionChecked = false;

    public static boolean isPassable(Block block) {
        if (block == null) return true;
        if (MaterialCompat.isAir(block.getType())) return true;
        ensureReflection();
        if (blockIsPassable != null) {
            try {
                return (Boolean) blockIsPassable.invoke(block);
            } catch (Exception ignored) {}
        }
        return !MaterialCompat.isSolid(block.getType());
    }

    public static boolean isAir(Block block) {
        return block == null || MaterialCompat.isAir(block.getType());
    }

    public static boolean isSolid(Block block) {
        return block != null && MaterialCompat.isSolid(block.getType());
    }

    private static synchronized void ensureReflection() {
        if (reflectionChecked) return;
        reflectionChecked = true;
        try { blockIsPassable = Block.class.getMethod("isPassable"); } catch (Exception ignored) {}
    }
}
