package com.smithai.util;

import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Method;

/**
 * Version-safe LivingEntity helpers so SmithAI loads on 1.12 through 1.21.x.
 * LivingEntity.setAI(boolean) exists in most modern Bukkit but is absent on very old builds.
 */
public class LivingEntityCompat {

    private static Method setAIMethod;
    private static boolean reflectionChecked = false;

    public static void setAI(LivingEntity entity, boolean ai) {
        if (entity == null) return;
        ensureReflection();
        if (setAIMethod != null) {
            try {
                setAIMethod.invoke(entity, ai);
            } catch (Exception ignored) {}
        }
    }

    private static synchronized void ensureReflection() {
        if (reflectionChecked) return;
        reflectionChecked = true;
        try { setAIMethod = LivingEntity.class.getMethod("setAI", boolean.class); } catch (Exception ignored) {}
    }
}
