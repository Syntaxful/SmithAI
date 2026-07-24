package com.smithai.ai;

import com.smithai.SmithAIPlugin;
import com.smithai.config.Config;
import com.smithai.memory.Conversation;
import com.smithai.util.VersionInfo;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ExternalAIConnector {

    private final SmithAIPlugin plugin;
    private final HttpClient httpClient;
    private final VersionInfo versionInfo;

    public ExternalAIConnector(SmithAIPlugin plugin) {
        this.plugin = plugin;
        this.versionInfo = new VersionInfo();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    public CompletableFuture<Boolean> checkHealth() {
        Config config = plugin.getPluginConfig();
        if (!config.isExternalEnabled() || config.getExternalUrl().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        String url = normalizeUrl(config.getExternalUrl());
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url + "/health"))
            .timeout(Duration.ofSeconds(config.getExternalTimeout()))
            .header("Accept", "application/json")
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> response.statusCode() == 200)
            .exceptionally(ex -> false)
            .orTimeout(config.getExternalTimeout(), TimeUnit.SECONDS)
            .exceptionally(ex -> false);
    }

    public CompletableFuture<String> chat(Player player, String message, Conversation conversation, String task, List<String> knowledge, List<String> skills, java.util.Map<String, Integer> inventory) {
        Config config = plugin.getPluginConfig();
        String url = normalizeUrl(config.getExternalUrl());

        JSONObject body = buildChatBody(player, message, conversation, task, knowledge, skills, inventory);

        return sendChatWithRetry(url, body, config.getExternalApiKey(), config.getExternalTimeout(), 3);
    }

    private CompletableFuture<String> sendChatWithRetry(String url, JSONObject body, String apiKey, int timeout, int retries) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url + "/chat"))
            .timeout(Duration.ofSeconds(timeout))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = builder
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        CompletableFuture<String> future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new RuntimeException("External AI returned status " + response.statusCode());
                }
                JSONObject json = new JSONObject(response.body());
                String reply = json.optString("reply", "No response from SmithGPT.");
                if (json.has("action")) {
                    String action = json.optString("action", "");
                    String actionTarget = json.optString("target", "");
                    if (!action.isEmpty()) {
                        reply += " [action:" + action + "," + actionTarget + "]";
                    }
                }
                return reply;
            });

        if (retries > 0) {
            future = future.exceptionallyCompose(ex -> {
                plugin.getLogger().warning("External AI chat failed (retrying " + retries + " more): " + ex.getMessage());
                return sendChatWithRetry(url, body, apiKey, timeout, retries - 1);
            });
        }

        return future.orTimeout(timeout, TimeUnit.SECONDS);
    }

    private JSONObject buildChatBody(Player player, String message, Conversation conversation, String task, List<String> knowledge, List<String> skills, java.util.Map<String, Integer> inventory) {
        JSONObject body = new JSONObject();
        JSONArray messages = new JSONArray();
        for (Conversation.Message msg : conversation.getMessages()) {
            JSONObject m = new JSONObject();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            messages.put(m);
        }
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", message);
        messages.put(userMsg);

        body.put("messages", messages);
        body.put("model", plugin.getPluginConfig().getExternalModel());
        if (task != null) {
            body.put("task", task);
        }
        if (knowledge != null && !knowledge.isEmpty()) {
            body.put("knowledge", new JSONArray(knowledge));
        }
        if (skills != null && !skills.isEmpty()) {
            body.put("skills", new JSONArray(skills));
        }
        if (conversation != null && player != null) {
            body.put("conversation_id", player.getUniqueId().toString() + ":smithai");
        }
        JSONObject context = new JSONObject();
        context.put("player", player.getName());
        context.put("world", player.getWorld() != null ? player.getWorld().getName() : "unknown");
        context.put("minecraft_version", versionInfo.getMinecraftVersion());
        context.put("server_type", versionInfo.isEaglercraft() ? "eaglercraft" : "java");
        context.put("has_deepslate", versionInfo.hasDeepslate());
        context.put("has_netherite", versionInfo.hasNetherite());
        context.put("diamond_y", versionInfo.bestDiamondY());
        context.put("iron_y", versionInfo.bestIronY());
        context.put("gold_y", versionInfo.bestGoldY());
        if (inventory != null && !inventory.isEmpty()) {
            JSONObject inv = new JSONObject();
            for (java.util.Map.Entry<String, Integer> e : inventory.entrySet()) {
                inv.put(e.getKey(), e.getValue());
            }
            context.put("inventory", inv);
        }
        body.put("context", context);
        return body;
    }

    private String normalizeUrl(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
