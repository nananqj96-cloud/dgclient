package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SpotifyHudModule extends Module {
    private final BooleanSetting showArtist = this.addSetting(new BooleanSetting("Show Artist", true));
    private final BooleanSetting showAlbumArt = this.addSetting(new BooleanSetting("Show Album Art", true));
    private final BooleanSetting showProgress = this.addSetting(new BooleanSetting("Show Progress", true));
    private final BooleanSetting scrollTitles = this.addSetting(new BooleanSetting("Scroll Long Titles", true));
    private final BooleanSetting showLyrics = this.addSetting(new BooleanSetting("Show Lyrics", true));
    private final ModeSetting lyricLines = this.addSetting(new ModeSetting("Lyric Lines", "2", "1", "2", "3"));
    private final ModeSetting lyricMotion = this.addSetting(new ModeSetting("Lyric Motion", "Wave", "Wave", "Glide", "Fade"));
    private final ModeSetting lyricHighlight = this.addSetting(new ModeSetting("Lyric Highlight", "Karaoke", "Karaoke", "Line", "Off"));
    private final BooleanSetting lyricsOnline = this.addSetting(new BooleanSetting("Lyrics Online", true));
    private final BooleanSetting skyLyrics = this.addSetting(new BooleanSetting("Sky Lyrics", true));
    private final BooleanSetting skyTitle = this.addSetting(new BooleanSetting("Sky Song Title", false));
    private final BooleanSetting skyEmojis = this.addSetting(new BooleanSetting("Sky Emojis", true));
    private final SliderSetting skyHeight = this.addSetting(new SliderSetting("Sky Height", 10.0, 10.0, 70.0, 1.0F));

    public SpotifyHudModule() {
        super("spotify-hud", "Shows what Spotify is playing, with synced lyrics and optional media keys.", Category.CLIENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Spotify HUD logic stub - core music display functionality
        // preserved as module registration; detailed logic disabled
    }

    public boolean getShowArtist() {
        return this.showArtist.get();
    }

    public boolean getShowLyrics() {
        return this.showLyrics.get();
    }

    public String getLyricLines() {
        return this.lyricLines.get();
    }

    public String getLyricMotion() {
        return this.lyricMotion.get();
    }
}
