/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.Settings
 *  net.fabricmc.loader.api.FabricLoader
 */
package com.fakeclient;

import com.fakeclient.FakeClient;
import com.fakeclient.Settings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

public final class Configs {
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("fakeclient-configs");
    private static String active = "";

    private Configs() {
    }

    public static String active() {
        return active;
    }

    public static List<String> list() {
        try {
            if (!Files.isDirectory(DIR, new LinkOption[0])) {
                return new ArrayList<String>();
            }
            try (Stream<Path> st = Files.list(DIR)) {
                return st.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5)).sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            return new ArrayList<String>();
        }
    }

    public static boolean save(String name) {
        String safe = Configs.sanitize(name);
        if (safe.isEmpty()) {
            return false;
        }
        try {
            Files.createDirectories(DIR, new FileAttribute[0]);
            Files.writeString(DIR.resolve(safe + ".json"), (CharSequence)Settings.get().toJson(), new OpenOption[0]);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    public static boolean load(String name) {
        Path f = DIR.resolve(Configs.sanitize(name) + ".json");
        if (!Files.exists(f, new LinkOption[0])) {
            return false;
        }
        try {
            Settings.replaceFromJson((String)Files.readString(f));
            FakeClient.MODULES.loadState();
            active = Configs.sanitize(name);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    public static boolean duplicate(String name) {
        String safe = Configs.sanitize(name);
        Path f = DIR.resolve(safe + ".json");
        if (!Files.exists(f, new LinkOption[0])) {
            return false;
        }
        String target = Configs.sanitize(name + " copy");
        int n = 2;
        while (Configs.exists(target)) {
            target = Configs.sanitize(name + " copy " + n);
            ++n;
        }
        try {
            Files.writeString(DIR.resolve(target + ".json"), (CharSequence)Files.readString(f), new OpenOption[0]);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    public static int accentOf(String name) {
        Path f = DIR.resolve(Configs.sanitize(name) + ".json");
        if (!Files.exists(f, new LinkOption[0])) {
            return 0;
        }
        try {
            for (String line : Files.readAllLines(f)) {
                String t = line.trim();
                if (!t.startsWith("\"accent\"")) continue;
                String num = t.substring(t.indexOf(58) + 1).replace(",", "").trim();
                return (int)Long.parseLong(num);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return 0;
    }

    public static boolean delete(String name) {
        try {
            if (Configs.sanitize(name).equals(active)) {
                active = "";
            }
            return Files.deleteIfExists(DIR.resolve(Configs.sanitize(name) + ".json"));
        }
        catch (IOException e) {
            return false;
        }
    }

    public static boolean exists(String name) {
        return Files.exists(DIR.resolve(Configs.sanitize(name) + ".json"), new LinkOption[0]);
    }

    public static String sanitize(String name) {
        return name == null ? "" : name.trim().replaceAll("[^a-zA-Z0-9 _-]", "").trim();
    }
}
