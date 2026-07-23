package com.smithai.util;

import org.bukkit.entity.Villager;

import java.lang.reflect.Method;

/**
 * Version-safe Villager helpers so SmithAI loads on 1.12 through 1.21.x.
 * Villager.setAware(boolean) and Profession.NONE are modern additions.
 */
public class VillagerCompat {

    private static Method setAwareMethod;
    private static Object noneProfession;
    private static boolean reflectionChecked = false;

    public static void setAware(Villager villager, boolean aware) {
        if (villager == null) return;
        ensureReflection();
        if (setAwareMethod != null) {
            try {
                setAwareMethod.invoke(villager, aware);
            } catch (Exception ignored) {}
        }
    }

    public static void setNoProfession(Villager villager) {
        if (villager == null) return;
        ensureReflection();
        if (noneProfession != null) {
            try {
                Class<?> professionClass = noneProfession.getClass();
                Method setProfession = Villager.class.getMethod("setProfession", professionClass);
                setProfession.invoke(villager, noneProfession);
            } catch (Exception ignored) {}
        }
    }

    private static synchronized void ensureReflection() {
        if (reflectionChecked) return;
        reflectionChecked = true;
        try { setAwareMethod = Villager.class.getMethod("setAware", boolean.class); } catch (Exception ignored) {}
        try {
            noneProfession = Villager.Profession.class.getEnumConstants()[0];
            for (Object c : Villager.Profession.class.getEnumConstants()) {
                if (c.toString().equalsIgnoreCase("NONE")) {
                    noneProfession = c;
                    break;
                }
            }
        } catch (Exception ignored) {}
    }
}
