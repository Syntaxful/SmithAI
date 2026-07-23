package com.smithai.training;

import com.smithai.SmithAIPlugin;
import com.smithai.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TrainingManagerTest {

    private SmithAIPlugin mockPlugin(File dataFolder) {
        SmithAIPlugin plugin = mock(SmithAIPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        Config config = mock(Config.class);
        when(config.isPersistTraining()).thenReturn(true);
        when(plugin.getPluginConfig()).thenReturn(config);
        return plugin;
    }

    @Test
    public void testRecordGoodIncreasesScore(@TempDir File tempDir) {
        TrainingManager tm = new TrainingManager(mockPlugin(tempDir));
        tm.recordGood("mine_diamonds");
        tm.recordGood("mine_diamonds");
        assertEquals(2, tm.getScore("mine_diamonds"), "Good feedback should increase score");
    }

    @Test
    public void testRecordBadDecreasesScore(@TempDir File tempDir) {
        TrainingManager tm = new TrainingManager(mockPlugin(tempDir));
        tm.recordBad("fight_zombie");
        assertEquals(-1, tm.getScore("fight_zombie"), "Bad feedback should decrease score");
    }

    @Test
    public void testScoresAreCaseInsensitive(@TempDir File tempDir) {
        TrainingManager tm = new TrainingManager(mockPlugin(tempDir));
        tm.recordGood("Build_Base");
        assertEquals(1, tm.getScore("build_base"), "Scores should be stored lowercase");
    }

    @Test
    public void testGetAllScores(@TempDir File tempDir) {
        TrainingManager tm = new TrainingManager(mockPlugin(tempDir));
        tm.recordGood("task:build");
        assertEquals(1, tm.getAllScores().get("task:build"), "All scores should include recorded action");
    }

    @Test
    public void testUnknownScoreIsZero(@TempDir File tempDir) {
        TrainingManager tm = new TrainingManager(mockPlugin(tempDir));
        assertEquals(0, tm.getScore("nonexistent"), "Unknown actions should score zero");
    }
}
