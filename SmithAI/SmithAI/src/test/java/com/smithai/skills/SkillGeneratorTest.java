package com.smithai.skills;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SkillGeneratorTest {

    @TempDir
    File tempDir;

    @Test
    public void testGeneratesCorrectSkillCounts() throws Exception {
        File output = new File(tempDir, "skills.yml");
        SkillGenerator.generateIfMissing(output);

        assertTrue(output.exists(), "Skills file should be generated");
        List<String> lines = Files.readAllLines(output.toPath());
        long mini = lines.stream().filter(l -> l.contains("tier: mini")).count();
        long gpt1 = lines.stream().filter(l -> l.contains("tier: gpt1")).count();
        long gpt2 = lines.stream().filter(l -> l.contains("tier: gpt2")).count();

        assertEquals(2000, mini, "Expected 2000 Smith-Mini skills");
        assertEquals(5200, gpt1, "Expected 5200 SmithGPT 1.0 skills");
        assertEquals(6300, gpt2, "Expected 6300 SmithGPT 2.0 skills");
    }

    @Test
    public void testDoesNotOverwriteExistingFile() throws Exception {
        File output = new File(tempDir, "existing.yml");
        Files.write(output.toPath(), "existing: true".getBytes());
        SkillGenerator.generateIfMissing(output);
        String content = new String(Files.readAllBytes(output.toPath()));
        assertEquals("existing: true", content);
    }
}
