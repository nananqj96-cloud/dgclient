package com.dg.client.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe friends manager integrated with ESP, Tracers, BetterTabList, Nametags, and AutoLog.
 */
public final class FriendManager {
    private final Set<String> friendsLower = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, String> displayNames = new ConcurrentHashMap<>();

    public boolean addFriend(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        String clean = username.trim();
        String key = clean.toLowerCase(Locale.ROOT);
        displayNames.put(key, clean);
        return friendsLower.add(key);
    }

    public boolean removeFriend(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        String key = username.trim().toLowerCase(Locale.ROOT);
        displayNames.remove(key);
        return friendsLower.remove(key);
    }

    public boolean isFriend(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return friendsLower.contains(username.trim().toLowerCase(Locale.ROOT));
    }

    public void clear() {
        friendsLower.clear();
        displayNames.clear();
    }

    public List<String> getFriends() {
        List<String> out = new ArrayList<>(displayNames.values());
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public JsonArray toJson() {
        JsonArray arr = new JsonArray();
        for (String friend : getFriends()) {
            arr.add(friend);
        }
        return arr;
    }

    public void fromJson(JsonElement elem) {
        clear();
        if (elem != null && elem.isJsonArray()) {
            for (JsonElement e : elem.getAsJsonArray()) {
                if (e != null && e.isJsonPrimitive()) {
                    addFriend(e.getAsString());
                }
            }
        }
    }
}
