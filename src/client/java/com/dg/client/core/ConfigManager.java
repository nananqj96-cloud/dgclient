package com.dg.client.core;

import com.dg.client.DGClient;
import com.dg.client.hud.HudElement;
import com.dg.client.module.Module;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists DGClient module configs, named profiles, friends, waypoints, HUD layout,
 * and ClickGUI panel coordinates in config/dgclient/.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path rootDir;
    private final Path profilesDir;
    private String activeProfile = "default";
    private final Map<String, float[]> panelPositions = new LinkedHashMap<>();

    public ConfigManager() {
        this.rootDir = FabricLoader.getInstance().getConfigDir().resolve("dgclient");
        this.profilesDir = rootDir.resolve("profiles");
    }

    public Path getRootDir() {
        return rootDir;
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    public Map<String, float[]> getPanelPositions() {
        return panelPositions;
    }

    public void setPanelPosition(String id, float x, float y, boolean expanded) {
        panelPositions.put(id, new float[]{x, y, expanded ? 1.0f : 0.0f});
    }

    public float[] getPanelPosition(String id) {
        return panelPositions.get(id);
    }

    public void loadAll() {
        try {
            Files.createDirectories(rootDir);
            Files.createDirectories(profilesDir);
        } catch (IOException e) {
            DGLogger.error("Failed creating config directories", e);
        }

        // Active profile meta
        Path metaFile = rootDir.resolve("meta.json");
        if (Files.exists(metaFile)) {
            try (BufferedReader reader = Files.newBufferedReader(metaFile, StandardCharsets.UTF_8)) {
                JsonElement elem = JsonParser.parseReader(reader);
                if (elem != null && elem.isJsonObject()) {
                    JsonObject meta = elem.getAsJsonObject();
                    if (meta.has("activeProfile")) {
                        activeProfile = sanitizeName(meta.get("activeProfile").getAsString());
                    }
                    if (meta.has("debug")) {
                        DGLogger.setDebugEnabled(meta.get("debug").getAsBoolean());
                    }
                }
            } catch (Throwable t) {
                DGLogger.warn("Failed reading meta.json: {}", t.getMessage());
            }
        }

        loadFriends();
        loadWaypoints();
        loadGuiAndHud();
        loadProfile(activeProfile);
    }

    public void saveAll() {
        try {
            Files.createDirectories(rootDir);
            Files.createDirectories(profilesDir);
            JsonObject meta = new JsonObject();
            meta.addProperty("activeProfile", activeProfile);
            meta.addProperty("debug", DGLogger.isDebugEnabled());
            writeJson(rootDir.resolve("meta.json"), meta);

            saveFriends();
            saveWaypoints();
            saveGuiAndHud();
            saveProfile(activeProfile);
        } catch (Throwable t) {
            DGLogger.error("Failed saving DGClient configuration", t);
        }
    }

    public boolean loadProfile(String profileName) {
        String safe = sanitizeName(profileName);
        Path file = profilesDir.resolve(safe + ".json");
        this.activeProfile = safe;
        if (!Files.exists(file)) {
            saveProfile(safe);
            return false;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement elem = JsonParser.parseReader(reader);
            if (elem != null && elem.isJsonObject()) {
                JsonObject root = elem.getAsJsonObject();
                JsonObject modulesObj = root.has("modules") && root.get("modules").isJsonObject()
                        ? root.getAsJsonObject("modules")
                        : root;
                for (Module module : DGClient.getInstance().getModuleManager().getModules()) {
                    if (modulesObj.has(module.getName()) && modulesObj.get(module.getName()).isJsonObject()) {
                        module.fromJson(modulesObj.getAsJsonObject(module.getName()));
                    }
                }
                return true;
            }
        } catch (Throwable t) {
            DGLogger.error("Failed loading profile " + safe, t);
        }
        return false;
    }

    public void saveProfile(String profileName) {
        String safe = sanitizeName(profileName);
        this.activeProfile = safe;
        JsonObject root = new JsonObject();
        root.addProperty("profile", safe);
        JsonObject modulesObj = new JsonObject();
        for (Module module : DGClient.getInstance().getModuleManager().getModules()) {
            modulesObj.add(module.getName(), module.toJson());
        }
        root.add("modules", modulesObj);
        writeJson(profilesDir.resolve(safe + ".json"), root);
    }

    public boolean deleteProfile(String profileName) {
        String safe = sanitizeName(profileName);
        if (safe.equalsIgnoreCase("default")) {
            return false;
        }
        try {
            boolean deleted = Files.deleteIfExists(profilesDir.resolve(safe + ".json"));
            if (deleted && activeProfile.equalsIgnoreCase(safe)) {
                loadProfile("default");
            }
            return deleted;
        } catch (IOException e) {
            return false;
        }
    }

    public List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        if (Files.exists(profilesDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(profilesDir, "*.json")) {
                for (Path p : stream) {
                    String fileName = p.getFileName().toString();
                    names.add(fileName.substring(0, fileName.length() - 5));
                }
            } catch (IOException ignored) {
            }
        }
        if (!names.contains("default")) {
            names.add("default");
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public void resetCurrentProfileToDefaults() {
        for (Module module : DGClient.getInstance().getModuleManager().getModules()) {
            module.resetSettings();
            if (module.getCategory() != com.dg.client.module.ModuleCategory.HUD) {
                module.setEnabled(false, false);
            }
        }
        saveProfile(activeProfile);
    }

    private void saveFriends() {
        writeJson(rootDir.resolve("friends.json"), DGClient.getInstance().getFriendManager().toJson());
    }

    private void loadFriends() {
        Path f = rootDir.resolve("friends.json");
        if (Files.exists(f)) {
            try (BufferedReader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                DGClient.getInstance().getFriendManager().fromJson(JsonParser.parseReader(r));
            } catch (Throwable t) {
                DGLogger.warn("Could not read friends.json: {}", t.getMessage());
            }
        }
    }

    private void saveWaypoints() {
        writeJson(rootDir.resolve("waypoints.json"), DGClient.getInstance().getWaypointManager().toJson());
    }

    private void loadWaypoints() {
        Path f = rootDir.resolve("waypoints.json");
        if (Files.exists(f)) {
            try (BufferedReader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                DGClient.getInstance().getWaypointManager().fromJson(JsonParser.parseReader(r));
            } catch (Throwable t) {
                DGLogger.warn("Could not read waypoints.json: {}", t.getMessage());
            }
        }
    }

    private void saveGuiAndHud() {
        JsonObject root = new JsonObject();
        JsonObject panels = new JsonObject();
        for (Map.Entry<String, float[]> entry : panelPositions.entrySet()) {
            JsonObject p = new JsonObject();
            p.addProperty("x", entry.getValue()[0]);
            p.addProperty("y", entry.getValue()[1]);
            p.addProperty("expanded", entry.getValue()[2] > 0.5f);
            panels.add(entry.getKey(), p);
        }
        root.add("panels", panels);

        JsonObject hud = new JsonObject();
        if (DGClient.getInstance().getHudManager() != null) {
            for (HudElement elem : DGClient.getInstance().getHudManager().getElements()) {
                hud.add(elem.getId(), elem.toJson());
            }
        }
        root.add("hud", hud);
        writeJson(rootDir.resolve("ui_layout.json"), root);
    }

    private void loadGuiAndHud() {
        Path f = rootDir.resolve("ui_layout.json");
        if (!Files.exists(f)) {
            return;
        }
        try (BufferedReader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            JsonElement elem = JsonParser.parseReader(r);
            if (elem != null && elem.isJsonObject()) {
                JsonObject root = elem.getAsJsonObject();
                if (root.has("panels") && root.get("panels").isJsonObject()) {
                    JsonObject panels = root.getAsJsonObject("panels");
                    for (String key : panels.keySet()) {
                        JsonObject p = panels.getAsJsonObject(key);
                        panelPositions.put(key, new float[]{
                                p.get("x").getAsFloat(),
                                p.get("y").getAsFloat(),
                                (!p.has("expanded") || p.get("expanded").getAsBoolean()) ? 1.0f : 0.0f
                        });
                    }
                }
                if (root.has("hud") && root.get("hud").isJsonObject() && DGClient.getInstance().getHudManager() != null) {
                    JsonObject hud = root.getAsJsonObject("hud");
                    for (HudElement he : DGClient.getInstance().getHudManager().getElements()) {
                        if (hud.has(he.getId()) && hud.get(he.getId()).isJsonObject()) {
                            he.fromJson(hud.getAsJsonObject(he.getId()));
                        }
                    }
                }
            }
        } catch (Throwable t) {
            DGLogger.warn("Could not read ui_layout.json: {}", t.getMessage());
        }
    }

    private void writeJson(Path path, JsonElement element) {
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(element, writer);
            }
        } catch (IOException e) {
            DGLogger.error("Failed writing JSON to " + path, e);
        }
    }

    private static String sanitizeName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "default";
        }
        String clean = raw.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return clean.isBlank() ? "default" : clean;
    }
}
