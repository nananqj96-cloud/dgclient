package dev.aether.module.impl;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomGlintModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
    public static volatile CustomGlintModule active;
    private static final String COLOR_CUSTOM = "Custom";
    private static final String COLOR_ACCENT = "Accent";
    private final ModeSetting colorMode = this.addSetting(new ModeSetting("Color", "Custom", "Custom", "Accent"));
    private final ColorSetting itemColor = this.addSetting(new ColorSetting("Item Color", -9524994));
    private final BooleanSetting separateArmor = this.addSetting(new BooleanSetting("Separate Armor", false));
    private final ColorSetting armorColor = this.addSetting(new ColorSetting("Armor Color", -3697942));
    private final SliderSetting intensity = this.addSetting(new SliderSetting("Intensity", 1.0, 0.05, 1.0, 0.05F));
    private final SliderSetting speed = this.addSetting(new SliderSetting("Speed", 1.0, 0.1, 4.0, 0.1F));
    private final SliderSetting patternSize = this.addSetting(new SliderSetting("Pattern Size", 1.0, 0.25, 4.0, 0.25F));
    private final CustomGlintModule.GlintTexture items = new CustomGlintModule.GlintTexture(ItemRenderer.ENCHANTED_GLINT_ITEM);
    private final CustomGlintModule.GlintTexture armor = new CustomGlintModule.GlintTexture(ItemRenderer.ENCHANTED_GLINT_ARMOR);
    private double glintClock = Double.NaN;
    private long glintClockReal;

    public CustomGlintModule() {
        super("custom-glint", "Recolours the enchantment glint and changes its speed and pattern size.", Category.VISUALS);
        this.itemColor.visibleWhen(() -> this.colorMode.is("Custom"));
        this.separateArmor.visibleWhen(() -> this.colorMode.is("Custom"));
        this.armorColor.visibleWhen(() -> this.colorMode.is("Custom") && this.separateArmor.get());
    }

    @Override
    protected void onEnable() {
        this.glintClock = Double.NaN;
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isSameThread()) {
            this.restore();
        } else {
            mc.execute(() -> {
                if (active == null) {
                    this.restore();
                }
            });
        }
    }

    public long glintMillis(long realMillis) {
        if (Double.isNaN(this.glintClock)) {
            this.glintClock = realMillis;
        } else {
            this.glintClock = this.glintClock + (double)Math.max(0L, realMillis - this.glintClockReal) * this.speed.get();
        }

        this.glintClockReal = realMillis;
        return (long)this.glintClock;
    }

    public float uvScaleMultiplier() {
        return 1.0F / this.patternSize.get();
    }

    private void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.getOverlay() == null && RenderSystem.tryGetDevice() != null) {
            int itemRgb = this.colorMode.is("Accent") ? CosmeticAccent.argb() & 16777215 : this.itemColor.rgb();
            int armorRgb = this.colorMode.is("Custom") && this.separateArmor.get() ? this.armorColor.rgb() : itemRgb;
            float strength = this.intensity.get();
            this.items.tint(mc, itemRgb, strength);
            this.armor.tint(mc, armorRgb, strength);
        }
    }

    private void restore() {
        Minecraft mc = Minecraft.getInstance();
        this.items.restore(mc);
        this.armor.restore(mc);
    }

        private static final class GlintTexture {
        private final Identifier id;
        private int width;
        private int height;
        private int[] original;
        private int[] pattern;
        private GpuTexture written;
        private long writtenKey;
        private GpuTexture unusable;

        GlintTexture(Identifier id) {
            this.id = id;
        }

        void tint(Minecraft mc, int rgb, float intensity) {
            GpuTexture gpu = this.gpuTexture(mc);
            if (gpu != null && !gpu.isClosed() && gpu != this.unusable) {
                long key = (long)(rgb & 16777215) << 8 | Math.round(intensity * 255.0F);
                if (gpu != this.written || key != this.writtenKey) {
                    if (gpu != this.written) {
                        this.written = null;
                        if ((gpu.usage() & 1) == 0 || !this.readShipped(mc, gpu)) {
                            this.unusable = gpu;
                            return;
                        }
                    }

                    if (!this.upload(gpu, this.tinted(rgb, intensity))) {
                        this.unusable = gpu;
                    } else {
                        this.written = gpu;
                        this.writtenKey = key;
                    }
                }
            }
        }

        void restore(Minecraft mc) {
            GpuTexture gpu = this.written != null ? this.gpuTexture(mc) : null;
            if (gpu != null && gpu == this.written && !gpu.isClosed() && this.original != null) {
                this.upload(gpu, this.original);
            }

            this.written = null;
            this.unusable = null;
            this.original = null;
            this.pattern = null;
        }

        private GpuTexture gpuTexture(Minecraft mc) {
            try {
                AbstractTexture texture = mc.getTextureManager().getTexture(this.id);
                return texture == null ? null : texture.getTexture();
            } catch (IllegalStateException notLoaded) {
                return null;
            }
        }

        private boolean readShipped(Minecraft mc, GpuTexture gpu) {
            Optional<Resource> resource = mc.getResourceManager().getResource(this.id);
            if (resource.isEmpty()) {
                return false;
            }

            try (
                InputStream in = resource.get().open();
                NativeImage image = NativeImage.read(in);
            ) {
                int w = image.getWidth();
                int h = image.getHeight();
                if (w != gpu.getWidth(0) || h != gpu.getHeight(0)) {
                    return false;
                }

                int[] pixels = new int[w * h];
                int[] luma = new int[w * h];
                int y = 0;
                int i = 0;

                while (y < h) {
                    for (int x = 0; x < w; i++) {
                        int argb = image.getPixel(x, y);
                        pixels[i] = argb;
                        luma[i] = Math.max(ARGB.red(argb), Math.max(ARGB.green(argb), ARGB.blue(argb)));
                        x++;
                    }

                    y++;
                }

                this.width = w;
                this.height = h;
                this.original = pixels;
                this.pattern = luma;
                return true;
            } catch (IOException | RuntimeException e) {
                CustomGlintModule.LOGGER.warn("[Anubis] Custom Glint could not read {}", this.id, e);
                return false;
            }
        }

        private int[] tinted(int rgb, float intensity) {
            float r = ARGB.red(rgb) * intensity / 255.0F;
            float g = ARGB.green(rgb) * intensity / 255.0F;
            float b = ARGB.blue(rgb) * intensity / 255.0F;
            int[] out = new int[this.pattern.length];

            for (int i = 0; i < out.length; i++) {
                int l = this.pattern[i];
                out[i] = ARGB.color(ARGB.alpha(this.original[i]), Math.round(l * r), Math.round(l * g), Math.round(l * b));
            }

            return out;
        }

        private boolean upload(GpuTexture gpu, int[] argb) {
            try (NativeImage image = new NativeImage(this.width, this.height, false)) {
                int y = 0;
                int i = 0;

                while (y < this.height) {
                    for (int x = 0; x < this.width; i++) {
                        image.setPixel(x, y, argb[i]);
                        x++;
                    }

                    y++;
                }

                RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpu, image);
                return true;
            } catch (RuntimeException e) {
                CustomGlintModule.LOGGER.warn("[Anubis] Custom Glint could not write {}", this.id, e);
                return false;
            }
        }
    }
}
