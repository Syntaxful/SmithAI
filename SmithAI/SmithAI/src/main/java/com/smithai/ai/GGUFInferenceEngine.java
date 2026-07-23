package com.smithai.ai;

import com.smithai.SmithAIPlugin;
import com.smithai.memory.Conversation;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Runs GGUF model inference by shelling out to llama.cpp CLI.
 * Falls back gracefully if no binary or model file is present.
 */
public class GGUFInferenceEngine {

    private final SmithAIPlugin plugin;
    private final String modelPath;
    private final String binaryPath;
    private final int contextSize;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "gguf-inference");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();
    private boolean available = false;
    private boolean availabilityChecked = false;

    public GGUFInferenceEngine(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.modelPath = plugin.getPluginConfig().getLocalModelPath();
        this.contextSize = 2048;

        // Detect llama.cpp binary
        String detected = detectBinary();
        this.binaryPath = detected;
        if (detected != null) {
            this.available = true;
            plugin.getLogger().info("GGUF inference engine ready: " + detected + " -> " + modelPath);
        }
    }

    private String detectBinary() {
        String[] candidates = {"llama-cli", "llama.cpp", "llama", "main"};
        for (String candidate : candidates) {
            try {
                Process p = new ProcessBuilder(candidate, "--help")
                    .redirectErrorStream(true)
                    .start();
                boolean exited = p.waitFor(2, TimeUnit.SECONDS);
                if (exited && p.exitValue() == 0) {
                    return candidate;
                }
            } catch (Exception ignored) {}
        }
        // Check common locations
        String[] paths = {"/usr/local/bin/llama-cli", "/usr/bin/llama-cli",
                          "/usr/local/bin/llama.cpp", "/usr/bin/llama.cpp",
                          "./llama-cli", "./llama.cpp"};
        for (String path : paths) {
            if (new File(path).canExecute()) return path;
        }
        return null;
    }

    public boolean isAvailable() {
        if (!availabilityChecked) {
            availabilityChecked = true;
            available = binaryPath != null && new File(modelPath).exists();
        }
        return available;
    }

    public boolean isModelDownloaded() {
        return new File(modelPath).exists();
    }

    /**
     * Get a prompt template for the given model type.
     */
    public String getPromptTemplate(String modelTier) {
        return promptCache.computeIfAbsent(modelTier, tier -> {
            switch (tier.toLowerCase()) {
                case "smith-mini":
                case "mini":
                    return "<|system|>\nYou are Smith_AI, a helpful Minecraft AI companion.\nCurrent task: {task}\nKnowledge: {knowledge}\nSkills: {skills}\n<|user|>\n{message}\n<|assistant|>\n";
                case "smithgpt-1.0":
                case "gpt1":
                    return "<|im_start|>system\nYou are Smith_AI, a helpful Minecraft AI companion for a {version} server.\nContext: {conversation}\nTask: {task}\nKnowledge: {knowledge}\nSkills: {skills}\n<|im_end|>\n<|im_start|>user\n{message}\n<|im_end|>\n<|im_start|>assistant\n";
                default:
                    return "System: You are Smith_AI, a helpful Minecraft AI companion.\nTask: {task}\nUser: {message}\nAssistant:";
            }
        });
    }

    /**
     * Synchronous inference — blocks until complete.
     */
    public CompletableFuture<String> infer(String prompt, int maxTokens) {
        if (!isAvailable()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                File tempInput = File.createTempFile("gguf_prompt_", ".txt");
                tempInput.deleteOnExit();
                try (Writer w = new OutputStreamWriter(new FileOutputStream(tempInput), "UTF-8")) {
                    w.write(prompt);
                }

                List<String> cmd = new ArrayList<>();
                cmd.add(binaryPath);
                cmd.add("-m"); cmd.add(modelPath);
                cmd.add("-f"); cmd.add(tempInput.getAbsolutePath());
                cmd.add("-n"); cmd.add(String.valueOf(maxTokens));
                cmd.add("--ctx-size"); cmd.add(String.valueOf(contextSize));
                cmd.add("--temp"); cmd.add("0.7");
                cmd.add("--repeat-penalty"); cmd.add("1.1");
                cmd.add("-c"); cmd.add("--no-display-prompt");

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                long start = System.currentTimeMillis();
                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    plugin.getLogger().warning("GGUF inference timed out after 30s");
                    return null;
                }

                long elapsed = System.currentTimeMillis() - start;
                String result = output.toString().trim();

                // Log performance telemetry
                String[] words = result.split("\\s+");
                int tokenCount = Math.min(words.length, maxTokens);
                plugin.getLogger().fine("GGUF inference: " + tokenCount + " tokens in " + elapsed + "ms (" +
                    String.format("%.1f", tokenCount > 0 ? (float) tokenCount / (elapsed / 1000f) : 0) + " t/s)");

                tempInput.delete();
                return result.isEmpty() ? null : result;

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "GGUF inference failed", e);
                return null;
            }
        }, executor);
    }

    /**
     * Format a prompt using the model's template.
     */
    public String formatPrompt(String modelTier, String message, Conversation conversation,
                                String task, List<String> knowledge, List<String> skills,
                                String version) {
        String template = getPromptTemplate(modelTier);
        String conv = conversation != null ? conversation.getFormattedHistory() : "";
        String knowStr = knowledge != null && !knowledge.isEmpty() ? String.join("; ", knowledge) : "general Minecraft";
        String skillStr = skills != null ? skills.size() + " skills available" : "unknown";

        return template
            .replace("{message}", message != null ? message : "")
            .replace("{task}", task != null ? task : "general help")
            .replace("{conversation}", conv)
            .replace("{knowledge}", knowStr)
            .replace("{skills}", skillStr)
            .replace("{version}", version != null ? version : "Minecraft");
    }

    /**
     * Warm up the model by running a short inference.
     */
    public void warmup() {
        if (!isAvailable()) return;
        plugin.getLogger().info("Warming up GGUF model: " + modelPath);
        infer("Hello", 10).thenAccept(result -> {
            if (result != null) {
                plugin.getLogger().info("GGUF model warmup complete. Response: " + result.substring(0, Math.min(50, result.length())));
            } else {
                plugin.getLogger().warning("GGUF model warmup returned empty or failed.");
            }
        });
    }

    /**
     * Get the model file size for cache validation.
     */
    public long getModelSize() {
        File f = new File(modelPath);
        return f.exists() ? f.length() : 0;
    }

    /**
     * Check if model needs to be re-downloaded (size changed).
     */
    public boolean isCacheStale(long expectedSize) {
        long actual = getModelSize();
        return actual > 0 && expectedSize > 0 && Math.abs(actual - expectedSize) > 1024;
    }
}
