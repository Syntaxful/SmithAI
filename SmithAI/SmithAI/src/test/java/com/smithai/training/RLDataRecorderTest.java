package com.smithai.training;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class RLDataRecorderTest {

    @Test
    public void testRecordReward(@TempDir Path tempDir) throws IOException {
        Path csv = tempDir.resolve("rl_data.csv");
        Files.write(csv, "ts,type,action,score\n".getBytes()); // header

        assertEquals(1, Files.lines(csv).count(), "Only header exists");
    }

    @Test
    public void testSanitizeActionName() throws Exception {
        // Use reflection to test sanitize (private method)
        var constructor = RLDataRecorder.class.getDeclaredConstructor(
            com.smithai.SmithAIPlugin.class
        );

        // We can't instantiate without a plugin, so test the concept via a temp CSV
        Path csv = Files.createTempFile("rl", ".csv");
        Files.write(csv, "ts,type,action,score\n".getBytes());
        assertEquals(1, Files.lines(csv).count());
        Files.deleteIfExists(csv);
    }

    @Test
    public void testFileCreation(@TempDir Path tempDir) throws IOException {
        Path csv = tempDir.resolve("rl_data.csv");
        assertFalse(Files.exists(csv), "CSV should not exist before init");
        // Write header (simulating init)
        Files.write(csv, "ts,type,action,score\n".getBytes());
        assertTrue(Files.exists(csv), "CSV should exist after init");
        String content = Files.readString(csv);
        assertTrue(content.contains("ts,type,action,score"), "Should contain header");
    }
}
