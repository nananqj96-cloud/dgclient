package dev.aether.module.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import dev.aether.settings.StringSetting;
import dev.aether.module.Module;
import dev.aether.module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public class SkinChangeModule extends Module {
    private volatile SkinChangeModule.CustomSkin selfSkin;
    private volatile SkinChangeModule.CustomSkin othersSkin;
    private String loadedSelfName = "";
    private String loadedOthersName = "";
    private boolean selfLoading;
    private boolean othersLoading;
    private final SkinChangeModule.SwapCache selfSwaps = new SkinChangeModule.SwapCache();
    private final SkinChangeModule.SwapCache othersSwaps = new SkinChangeModule.SwapCache();

    private final StringSetting selfPlayerSetting = this.addSetting(new StringSetting("Self Player", "Self Player", ""));
    private final StringSetting othersPlayerSetting = this.addSetting(new StringSetting("Others Player", "Others Player", ""));

    public SkinChangeModule() {
        super("skin-change", "Wear any player's skin client-side.", Category.MISC);
    }

    @Override
    protected void onEnable() {
        this.reloadIfChanged();
    }

    @Override
    protected void onDisable() {
        this.selfSkin = null;
        this.othersSkin = null;
        this.loadedSelfName = "";
        this.loadedOthersName = "";
        this.selfSwaps.clear();
        this.othersSwaps.clear();
    }

    private void reloadIfChanged() {
        String selfName = this.selfPlayerSetting.get().trim();
        String othersName = this.othersPlayerSetting.get().trim();
        if (!selfName.equals(this.loadedSelfName)) {
            this.selfSkin = null;
            if (selfName.isEmpty()) {
                this.loadedSelfName = "";
            } else if (!this.selfLoading) {
                this.loadedSelfName = selfName;
                this.selfLoading = true;
                this.loadFromUsername(selfName, true);
            }
        }

        if (!othersName.equals(this.loadedOthersName)) {
            this.othersSkin = null;
            if (othersName.isEmpty()) {
                this.loadedOthersName = "";
            } else if (!this.othersLoading) {
                this.loadedOthersName = othersName;
                this.othersLoading = true;
                this.loadFromUsername(othersName, false);
            }
        }
    }

    private void loadFromUsername(String username, boolean isSelf) {
        Minecraft mc = Minecraft.getInstance();
        CompletableFuture.runAsync(
            () -> {
                try {
                    String uuid = fetchUUID(username);
                    if (uuid == null) {
                        mc.execute(() -> this.finishLoad(username, isSelf, null));
                        return;
                    }

                    SkinChangeModule.SkinSource skin = fetchSkin(uuid);
                    if (skin == null) {
                        mc.execute(() -> this.finishLoad(username, isSelf, null));
                        return;
                    }

                    Identifier id = isSelf ? Identifier.fromNamespaceAndPath("anubis", "skin/self") : Identifier.fromNamespaceAndPath("anubis", "skin/others");
                    Path cacheDir = mc.gameDirectory.toPath().resolve("cache/anubis_skins");

                    try {
                        Files.createDirectories(cacheDir);
                    } catch (IOException ignored) {
                    }

                    Path cachePath = cacheDir.resolve((isSelf ? "self_" : "others_") + uuid + ".png");
                    registerSkin(id, cachePath, skin.url())
                        .whenCompleteAsync(
                            (texture, error) -> this.finishLoad(
                                username, isSelf, texture == null ? null : new SkinChangeModule.CustomSkin(texture, skin.model())
                            ),
                            mc
                        );
                } catch (Exception e) {
                    mc.execute(() -> this.finishLoad(username, isSelf, null));
                }
            }
        );
    }

    private void finishLoad(String username, boolean isSelf, SkinChangeModule.CustomSkin loaded) {
        if (isSelf) {
            this.selfLoading = false;
        } else {
            this.othersLoading = false;
        }

        if (this.isEnabled()) {
            String current = (isSelf ? this.selfPlayerSetting : this.othersPlayerSetting).get().trim();
            if (loaded != null && username.equals(current) && username.equals(isSelf ? this.loadedSelfName : this.loadedOthersName)) {
                if (isSelf) {
                    this.selfSkin = loaded;
                } else {
                    this.othersSkin = loaded;
                }
            }

            this.reloadIfChanged();
        }
    }

    private static CompletableFuture<Texture> registerSkin(Identifier id, Path cachePath, String url) {
        Minecraft mc = Minecraft.getInstance();
        SkinTextureDownloader downloader = new SkinTextureDownloader(mc.getProxy(), mc.getTextureManager(), mc);
        return downloader.downloadAndRegisterSkin(id, cachePath, url, true);
    }

    private static String fetchUUID(String username) throws Exception {
        HttpURLConnection conn = openGet("https://api.mojang.com/users/profiles/minecraft/" + username);
        int status = conn.getResponseCode();
        if (status != 204 && status != 404) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                JsonObject obj = JsonParser.parseString(sb.toString()).getAsJsonObject();
                return obj.get("id").getAsString();
            }
        } else {
            return null;
        }
    }

    private static SkinChangeModule.SkinSource fetchSkin(String uuid) throws Exception {
        HttpURLConnection conn = openGet("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
        if (conn.getResponseCode() != 200) {
            return null;
        }

        String json;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            json = sb.toString();
        }

        JsonObject profile = JsonParser.parseString(json).getAsJsonObject();

        for (JsonElement elem : profile.getAsJsonArray("properties")) {
            JsonObject prop = elem.getAsJsonObject();
            if ("textures".equals(prop.get("name").getAsString())) {
                String decoded = new String(Base64.getDecoder().decode(prop.get("value").getAsString()));
                JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
                if (!textures.has("SKIN")) {
                    return null;
                }

                JsonObject skin = textures.getAsJsonObject("SKIN");
                JsonObject metadata = skin.has("metadata") ? skin.getAsJsonObject("metadata") : null;
                String model = metadata != null && metadata.has("model") ? metadata.get("model").getAsString() : null;
                return new SkinChangeModule.SkinSource(skin.get("url").getAsString(), PlayerModelType.byLegacyServicesName(model));
            }
        }

        return null;
    }

    private static HttpURLConnection openGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection)URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", "Anubis/1.0");
        return conn;
    }

    public PlayerSkin skinFor(int entityId, PlayerSkin original) {
        Minecraft mc = Minecraft.getInstance();
        return this.skinFor(mc.player != null && entityId == mc.player.getId(), original);
    }

    public PlayerSkin skinFor(UUID profileId, PlayerSkin original) {
        Minecraft mc = Minecraft.getInstance();
        return this.skinFor(mc.player != null && mc.player.getUUID().equals(profileId), original);
    }

    private PlayerSkin skinFor(boolean isSelf, PlayerSkin original) {
        SkinChangeModule.CustomSkin custom = isSelf ? this.selfSkin : this.othersSkin;
        return custom == null ? original : (isSelf ? this.selfSwaps : this.othersSwaps).swap(original, custom);
    }

        private record CustomSkin(Texture body, PlayerModelType model) {
    }

        private record SkinSource(String url, PlayerModelType model) {
    }

        private static final class SwapCache {
        private static final int MAX = 256;
        private final Map<PlayerSkin, PlayerSkin> swapped = new IdentityHashMap<>();
        private SkinChangeModule.CustomSkin wearing;

        PlayerSkin swap(PlayerSkin original, SkinChangeModule.CustomSkin custom) {
            if (custom != this.wearing || this.swapped.size() >= 256) {
                this.swapped.clear();
                this.wearing = custom;
            }

            PlayerSkin skin = this.swapped.get(original);
            if (skin == null) {
                skin = new PlayerSkin(custom.body(), original.cape(), original.elytra(), custom.model(), original.secure());
                this.swapped.put(original, skin);
            }

            return skin;
        }

        void clear() {
            this.swapped.clear();
            this.wearing = null;
        }
    }
}
