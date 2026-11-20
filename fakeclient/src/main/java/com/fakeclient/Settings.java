/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.loader.api.FabricLoader
 */
package com.fakeclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public class Settings {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fakeclient.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Settings instance;
    public int accent = -4011822;
    public boolean rainbow = false;
    public boolean watermark = true;
    public int bgDim = 55;
    public boolean animations = true;
    public int animSpeed = 100;
    public int panelOpacity = 96;
    public int glow = 100;
    public float uiScale = 1.0f;
    public boolean menuSound = true;
    public boolean menuBlur = true;
    public boolean compactRows = false;
    public boolean showDescriptions = true;
    public int panelOffX = 0;
    public int panelOffY = 0;
    public boolean fpChat = true;
    public boolean fpActionBar = true;
    public boolean fpSound = true;
    public int itemMultiplier = 100;
    public int spawnerCount = 1;
    public String licenseKey = "";
    public String mediaName = "";
    public double paySimMin = 5000000.0;
    public double paySimMax = 1.0E8;
    public double paySimMinDelay = 45.0;
    public double paySimMaxDelay = 240.0;
    public boolean paySimWholeNumbers = false;
    public Map<String, Boolean> textureSwaps = new HashMap<String, Boolean>();
    public List<String> customSwaps = new ArrayList<String>();
    public Map<String, Integer> binds = new HashMap<String, Integer>();
    public Map<String, Boolean> moduleStates = new HashMap<String, Boolean>();

    public static Settings get() {
        if (instance == null) {
            Settings.load();
        }
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE, new LinkOption[0])) {
                instance = (Settings)GSON.fromJson(Files.readString(FILE), Settings.class);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        if (instance == null) {
            instance = new Settings();
        }
        instance.fixNulls();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent(), new FileAttribute[0]);
            Files.writeString(FILE, (CharSequence)GSON.toJson((Object)this), new OpenOption[0]);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void fixNulls() {
        if (this.binds == null) {
            this.binds = new HashMap<String, Integer>();
        }
        if (this.moduleStates == null) {
            this.moduleStates = new HashMap<String, Boolean>();
        }
        if (this.textureSwaps == null) {
            this.textureSwaps = new HashMap<String, Boolean>();
        }
        if (this.customSwaps == null) {
            this.customSwaps = new ArrayList<String>();
        }
    }

    public void resetToDefaults() {
        Settings d = new Settings();
        this.accent = d.accent;
        this.rainbow = d.rainbow;
        this.watermark = d.watermark;
        this.bgDim = d.bgDim;
        this.animations = d.animations;
        this.animSpeed = d.animSpeed;
        this.panelOpacity = d.panelOpacity;
        this.glow = d.glow;
        this.uiScale = d.uiScale;
        this.menuSound = d.menuSound;
        this.menuBlur = d.menuBlur;
        this.compactRows = d.compactRows;
        this.showDescriptions = d.showDescriptions;
        this.panelOffX = d.panelOffX;
        this.panelOffY = d.panelOffY;
        this.fpChat = d.fpChat;
        this.fpActionBar = d.fpActionBar;
        this.fpSound = d.fpSound;
        this.itemMultiplier = d.itemMultiplier;
        this.save();
    }

    public String toJson() {
        return GSON.toJson((Object)this);
    }

    public static void replaceFromJson(String json) {
        try {
            Settings s = (Settings)GSON.fromJson(json, Settings.class);
            if (s == null) {
                return;
            }
            s.fixNulls();
            instance = s;
            instance.save();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}
