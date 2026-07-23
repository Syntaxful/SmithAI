package com.smithai.knowledge;

import com.smithai.SmithAIPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KnowledgeBase {

    private final SmithAIPlugin plugin;
    private final List<KnowledgeEntry> entries = new ArrayList<>();

    public KnowledgeBase(SmithAIPlugin plugin) {
        this.plugin = plugin;
        loadDefaults();
    }

    private void loadDefaults() {
        loadResource("knowledge/blocks.json");
        loadResource("knowledge/mobs.json");
        loadResource("knowledge/items.json");
        loadResource("knowledge/recipes.json");
        loadResource("knowledge/strategy.json");
        loadResource("knowledge/biomes.json");
    }

    private void loadResource(String path) {
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) return;
            String content = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
            JSONArray array = new JSONArray(content);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                entries.add(new KnowledgeEntry(
                    obj.getString("id"),
                    obj.getString("category"),
                    obj.getString("name"),
                    obj.getString("description"),
                    toTags(obj.optJSONArray("tags"))
                ));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load knowledge resource: " + path + " - " + e.getMessage());
        }
    }

    private List<String> toTags(JSONArray array) {
        List<String> tags = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                tags.add(array.getString(i));
            }
        }
        return tags;
    }

    public List<String> findRelevant(String query) {
        String lower = query.toLowerCase();
        List<String> results = new ArrayList<>();
        for (KnowledgeEntry entry : entries) {
            if (entry.matches(lower)) {
                results.add(entry.getName() + ": " + entry.getDescription());
                if (results.size() >= 5) break;
            }
        }
        return results;
    }

    public int size() {
        return entries.size();
    }

    public static class KnowledgeEntry {
        private final String id;
        private final String category;
        private final String name;
        private final String description;
        private final List<String> tags;

        public KnowledgeEntry(String id, String category, String name, String description, List<String> tags) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.description = description;
            this.tags = tags;
        }

        public boolean matches(String query) {
            if (name.toLowerCase().contains(query)) return true;
            if (description.toLowerCase().contains(query)) return true;
            if (category.toLowerCase().contains(query)) return true;
            for (String tag : tags) {
                if (tag.toLowerCase().contains(query)) return true;
            }
            return false;
        }

        public String getId() { return id; }
        public String getCategory() { return category; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<String> getTags() { return tags; }
    }
}
