package com.smithai.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VersionInfoTest {

    @Test
    public void testModernJavaVersion() {
        VersionInfo v = new VersionInfo("1.21.1-R0.1-SNAPSHOT", "Paper 1.21.1");
        assertEquals(1, v.getMajor());
        assertEquals(21, v.getMinor());
        assertEquals(1, v.getPatch());
        assertEquals("1.21.1", v.getMinecraftVersion());
        assertEquals("1.21.1 Java Edition", v.getFriendlyName());
        assertTrue(v.hasDeepslate());
        assertTrue(v.hasNetherite());
        assertTrue(v.hasTrialChambers());
        assertEquals(-59, v.bestDiamondY());
        assertEquals(16, v.bestIronY());
        assertEquals(-16, v.bestGoldY());
    }

    @Test
    public void testLegacyJavaVersion() {
        VersionInfo v = new VersionInfo("1.12.2-R0.1-SNAPSHOT", "Spigot 1.12.2");
        assertEquals(1, v.getMajor());
        assertEquals(12, v.getMinor());
        assertEquals(2, v.getPatch());
        assertFalse(v.hasDeepslate());
        assertFalse(v.hasNetherite());
        assertFalse(v.hasWarden());
        assertEquals(11, v.bestDiamondY());
        assertEquals(40, v.bestIronY());
        assertEquals(32, v.bestGoldY());
    }

    @Test
    public void testEaglercraftVersion() {
        VersionInfo v = new VersionInfo("1.12.2-R0.1-SNAPSHOT", "Eaglercraft 1.12.2");
        assertTrue(v.isEaglercraft());
        assertEquals("1.12.2 Eaglercraft", v.getFriendlyName());
        assertFalse(v.hasDeepslate());
    }

    @Test
    public void testAtLeastComparison() {
        VersionInfo v = new VersionInfo("1.17.1-R0.1-SNAPSHOT", "Paper 1.17.1");
        assertTrue(v.isAtLeast(1, 17));
        assertTrue(v.isAtLeast(1, 17, 1));
        assertFalse(v.isAtLeast(1, 18));
    }

    @Test
    public void testEmptyVersionFallback() {
        VersionInfo v = new VersionInfo("", "");
        assertEquals(1, v.getMajor());
        assertEquals(8, v.getMinor());
        assertEquals(0, v.getPatch());
    }
}
