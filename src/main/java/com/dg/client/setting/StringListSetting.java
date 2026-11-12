package com.dg.client.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Editable list setting for block IDs, entity IDs, chat keywords, or player filters.
 */
public final class StringListSetting extends Setting<List<String>> {
    public StringListSetting(String name, String description, List<String> defaults) {
        super(name, description, normalizeList(defaults));
    }

    private static List<String> normalizeList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String item : input) {
            if (item != null && !item.isBlank()) {
                set.add(item.trim());
            }
        }
        return new ArrayList<>(set);
    }

    public List<String> getItems() {
        return Collections.unmodifiableList(getValue());
    }

    public boolean containsIgnoreCase(String candidate) {
        if (candidate == null) {
            return false;
        }
        String needle = candidate.trim().toLowerCase(Locale.ROOT);
        for (String s : getValue()) {
            if (s.toLowerCase(Locale.ROOT).equals(needle)) {
                return true;
            }
        }
        return false;
    }

    public boolean addItem(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String clean = raw.trim();
        if (containsIgnoreCase(clean)) {
            return false;
        }
        List<String> updated = new ArrayList<>(getValue());
        updated.add(clean);
        setValue(updated);
        return true;
    }

    public boolean removeItem(String raw) {
        if (raw == null) {
            return false;
        }
        String needle = raw.trim().toLowerCase(Locale.ROOT);
        List<String> updated = new ArrayList<>(getValue());
        boolean removed = updated.removeIf(s -> s.toLowerCase(Locale.ROOT).equals(needle));
        if (removed) {
            setValue(updated);
        }
        return removed;
    }

    @Override
    protected List<String> validate(List<String> incoming) {
        return normalizeList(incoming);
    }

    @Override
    public JsonElement toJson() {
        JsonArray arr = new JsonArray();
        for (String s : getValue()) {
            arr.add(s);
        }
        return arr;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonArray()) {
            List<String> loaded = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                if (e != null && e.isJsonPrimitive()) {
                    loaded.add(e.getAsString());
                }
            }
            setValue(loaded);
        }
    }
}
