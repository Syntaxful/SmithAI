package com.smithai.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Method;

/**
 * Version-safe LivingEntity helpers so SmithAI loads on 1.12 through 1.21.x.
 * LivingEntity.setAI(boolean) and attack(Entity) exist in modern Bukkit but are absent on 1.12.
 */
public class LivingEntityCompat {

    private static Method setAIMethod;
    private static Method attackMethod;
    private static Method swingMethod;
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

    public static void attack(LivingEntity attacker, LivingEntity target) {
        if (attacker == null || target == null) return;
        ensureReflection();
        if (attackMethod != null) {
            try {
                attackMethod.invoke(attacker, target);
                swing(attacker);
                return;
            } catch (Exception ignored) {}
        }
        // 1.12 fallback: deal damage directly.
        try {
            target.damage(5.0, attacker);
        } catch (Exception ignored) {}
        swing(attacker);
    }

    private static void swing(LivingEntity attacker) {
        if (swingMethod != null) {
            try {
                swingMethod.invoke(attacker);
            } catch (Exception ignored) {}
        }
    }

    private static synchronized void ensureReflection() {
        if (reflectionChecked) return;
        reflectionChecked = true;
        try { setAIMethod = LivingEntity.class.getMethod("setAI", boolean.class); } catch (Exception ignored) {}
        try { attackMethod = LivingEntity.class.getMethod("attack", Entity.class); } catch (Exception ignored) {}
        // swingMainHand exists on Player in older versions and LivingEntity in newer ones.
        try { swingMethod = LivingEntity.class.getMethod("swingMainHand"); } catch (Exception ignored) {}
        if (swingMethod == null) {
            try { swingMethod = Entity.class.getMethod("playEffect", org.bukkit.EntityEffect.class); } catch (Exception ignored) {}
        }
    }
}
