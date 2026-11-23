package dev.aether.module.impl;
import dev.aether.module.Module;

import java.util.Arrays;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.util.ARGB;

public class PharaohHeaddressModule extends HeadCosmeticModule {
    private static final String STYLE_NEMES = "Nemes";
    private static final String STYLE_KHEPRESH = "Khepresh";
    private static final String STYLE_SUN_DISC = "Sun Disc";
    private final ModeSetting style = this.addSetting(new ModeSetting("Style", "Nemes", "Nemes", "Khepresh", "Sun Disc"));
    private final SliderSetting stripes = this.addSetting(new SliderSetting("Stripes", 5.0, 3.0, 9.0, 1.0F));
    private final BooleanSetting collar = this.addSetting(new BooleanSetting("Broad Collar", true));
    private final SliderSetting size = this.addSetting(new SliderSetting("Size", 1.0, 0.8, 1.3, 0.05F));
    private final ModeSetting color = this.addSetting(new ModeSetting("Color", "Theme", "Theme", "Manual"));
    private final ColorSetting gold = this.addSetting(new ColorSetting("Gold Color", -7688204));
    private final ColorSetting inlay = this.addSetting(new ColorSetting("Inlay Color", -13738055));
    private final ColorSetting gem = this.addSetting(new ColorSetting("Gem Color", -10313473));
    private final BooleanSetting gemGlow = this.addSetting(new BooleanSetting("Gem Glow", true));
    private final BooleanSetting glint = this.addSetting(new BooleanSetting("Glint", true));
    private final BooleanSetting friends = this.addSetting(new BooleanSetting("On Friends", false));
    private final PharaohMesh[] meshes = new PharaohMesh[4];
    private long styleRevision = -1L;
    private long stripesRevision = -1L;
    private long collarRevision = -1L;

    public PharaohHeaddressModule() {
        super("pharaoh-headdress", "Pharaoh Headdress", "Puts a pharaoh headdress on your player model.");
        this.stripes.visibleWhen(() -> this.style.is("Nemes"));
        this.gold.visibleWhen(() -> this.color.is("Manual"));
        this.inlay.visibleWhen(() -> this.color.is("Manual"));
        this.gem.visibleWhen(() -> this.color.is("Manual"));
    }

    @Override
    void reset() {
        super.reset();
        Arrays.fill(this.meshes, null);
        this.styleRevision = -1L;
    }

    @Override
    PharaohMesh headMesh(boolean helmet) {
        this.refreshMeshes();
        int slot = helmet ? 1 : 0;
        PharaohMesh mesh = this.meshes[slot];
        if (mesh == null) {
            mesh = PharaohMesh.head(this.styleIndex(), this.stripes.getInt(), helmet);
            this.meshes[slot] = mesh;
        }

        return mesh;
    }

    @Override
    PharaohMesh bodyMesh(boolean chest) {
        this.refreshMeshes();
        int slot = chest ? 3 : 2;
        PharaohMesh mesh = this.meshes[slot];
        if (mesh == null) {
            mesh = PharaohMesh.body(this.styleIndex(), this.stripes.getInt(), chest, this.collar.get());
            this.meshes[slot] = mesh;
        }

        return mesh;
    }

    private void refreshMeshes() {
        long s = this.style.revision();
        long n = this.stripes.revision();
        long c = this.collar.revision();
        if (s != this.styleRevision || n != this.stripesRevision || c != this.collarRevision) {
            this.styleRevision = s;
            this.stripesRevision = n;
            this.collarRevision = c;
            Arrays.fill(this.meshes, null);
        }
    }

    private int styleIndex() {
        if (this.style.is("Khepresh")) {
            return 1;
        } else {
            return this.style.is("Sun Disc") ? 2 : 0;
        }
    }

    @Override
    float scale() {
        return this.size.get();
    }

    @Override
    int[] palette() {
        int[] palette = new int[5];
        if (this.color.is("Manual")) {
            int metal = this.gold.argb() | 0xFF000000;
            palette[0] = metal;
            palette[1] = this.inlay.argb() | 0xFF000000;
            palette[2] = this.gem.argb() | 0xFF000000;
            palette[3] = bronze(metal);
        } else {
            int accent = CosmeticAccent.argb();
            int metal = metal(accent);
            palette[0] = metal;
            palette[1] = ARGB.scaleRGB(accent, 0.78F);
            palette[2] = gem(accent);
            palette[3] = ARGB.scaleRGB(metal, 0.62F);
        }

        return palette;
    }

    @Override
    boolean glows() {
        return this.gemGlow.get();
    }

    @Override
    boolean glints() {
        return this.glint.get();
    }

    @Override
    boolean onFriends() {
        return this.friends.get();
    }

    private static int bronze(int argb) {
        int r = (int)((argb >> 16 & 0xFF) * 0.7F);
        int g = (int)((argb >> 8 & 0xFF) * 0.55F);
        int b = (int)((argb & 0xFF) * 0.4F);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
