package com.smithai.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SubsystemHealthTest {

    @Test
    public void testInitialState() {
        SubsystemHealth health = new SubsystemHealth(null);
        assertTrue(health.isHealthy(), "All subsystems should start healthy");
    }

    @Test
    public void testMarkDegraded() {
        SubsystemHealth health = new SubsystemHealth(null);
        health.markDegraded(SubsystemHealth.Subsystem.NPC, "npc manager failed");
        assertFalse(health.isHealthy(), "Health should be false when a subsystem is degraded");
        assertEquals(SubsystemHealth.Status.DEGRADED, health.getStatus(SubsystemHealth.Subsystem.NPC));
    }

    @Test
    public void testMarkDisabled() {
        SubsystemHealth health = new SubsystemHealth(null);
        health.markDisabled(SubsystemHealth.Subsystem.EXTERNAL, "external disabled");
        assertTrue(health.isHealthy(), "Disabled subsystems should not count as unhealthy");
    }
}
