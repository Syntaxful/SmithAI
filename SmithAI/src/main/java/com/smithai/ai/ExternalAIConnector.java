package com.smithai.ai;

import com.smithai.SmithAIPlugin;
import com.smithai.config.Config;
import com.smithai.memory.Conversation;
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

public class ExternalAIConnector {

    private final SmithAIPlugin plugin;
    private final HttpClient httpClient;

    public ExternalAIConnector(SmithAIPlugin plugin) {
        this.plugin = plugin;
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
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> response.statusCode() == 200)
            .exceptionally(ex -> false);
    }

    public CompletableFuture<String> chat(Player player, String message, Conversation conversation, String task, List<String> knowledge, List<String> skills) {
        Config config = plugin.getPluginConfig();
        String url = normalizeUrl(config.getExternalUrl());

        JSONObject body = buildChatBody(player, message, conversation, task, knowledge, skills);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url + "/chat"))
            .timeout(Duration.ofSeconds(config.getExternalTimeout()))
            .header("Content-Type", "application/json");

        String apiKey = config.getExternalApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = builder
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
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
    }

    private JSONObject buildChatBody(Player player, String message, Conversation conversation, String task, List<String> knowledge, List<String> skills) {
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
        body.put("context", new JSONObject().put("player", player.getName()));
        return body;
    }

    private String normalizeUrl(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
