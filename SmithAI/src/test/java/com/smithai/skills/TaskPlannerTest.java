package com.smithai.skills;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TaskPlannerTest {

    @Test
    public void testPlanGetDiamonds() {
        List<String> plan = TaskPlanner.plan("get diamonds");
        assertFalse(plan.isEmpty(), "Should return a plan for diamonds");
        assertEquals("chop_tree", plan.get(0));
        assertTrue(plan.contains("mine_diamonds"), "Plan should end with mining diamonds");
    }

    @Test
    public void testPlanBeatTheGame() {
        List<String> plan = TaskPlanner.plan("beat the game");
        assertFalse(plan.isEmpty(), "Should return a plan for beating the game");
        assertTrue(plan.contains("defeat_ender_dragon"), "Plan should include defeating the dragon");
        assertTrue(plan.contains("fill_end_portal"), "Plan should include filling the end portal");
    }

    @Test
    public void testPlanBuildBase() {
        List<String> plan = TaskPlanner.plan("build base");
        assertFalse(plan.isEmpty(), "Should return a plan for building a base");
        assertTrue(plan.contains("build_house"), "Plan should include building a house");
    }

    @Test
    public void testPlanUnknownTask() {
        List<String> plan = TaskPlanner.plan("do something completely unknown");
        assertTrue(plan.isEmpty(), "Unknown tasks should return empty list");
    }

    @Test
    public void testDescribePlan() {
        List<String> plan = List.of("a", "b", "c");
        String desc = TaskPlanner.describePlan("test", plan);
        assertTrue(desc.contains("a -> b -> c"), "Plan should be described as joined steps");
    }

    @Test
    public void testDescribeEmptyPlan() {
        String desc = TaskPlanner.describePlan("unknown", java.util.Collections.emptyList());
        assertTrue(desc.contains("I don't know how to do that yet"), "Empty plan should have helpful message");
    }
}
