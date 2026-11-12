package com.dg.client.hud;

import com.dg.client.DGClient;
import com.dg.client.core.NotificationManager;
import com.dg.client.core.ServerRateTracker;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.modules.hud.HudModule;
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Owns and renders all draggable DGClient HUD elements:
 * Watermark, ArrayList, Coordinates + Nether/OW conversion, Telemetry (FPS/Ping/TPS/Speed/Direction/Biome/Time),
 * Armor & Durability bars, Potion Effects, Totem Counter, and Toast Notifications.
 */
public final class HudManager {
    private final List<HudElement> elements = new ArrayList<>();
    private boolean hideHudForScreenshot = false;

    public void init() {
        registerDefaultElements();
        DGClient.getInstance().getEventBus().subscribe(this);
    }

    public void setHideHudForScreenshot(boolean hide) {
        this.hideHudForScreenshot = hide;
    }

    public boolean isHideHudForScreenshot() {
        return hideHudForScreenshot;
    }

    public List<HudElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (hideHudForScreenshot) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.currentScreen instanceof HudEditorScreen) {
            return;
        }
        HudModule hud = DGClient.getInstance().getModuleManager().get(HudModule.class);
        if (hud == null || !hud.isEnabled()) {
            return;
        }
        DrawContext ctx = event.getContext();
        int sw = event.getScreenWidth();
        int sh = event.getScreenHeight();
        for (HudElement elem : elements) {
            if (elem.isEnabled() && elem.isActiveByModule()) {
                elem.render(ctx, sw, sh, event.getTickDelta(), false);
            }
        }
        if (hud.toasts.getValue()) {
            renderToasts(ctx, sw, hud.hudGlow.getValue().floatValue());
        }
    }

    public void renderToasts(DrawContext ctx, int screenWidth, float glowStrength) {
        List<NotificationManager.Toast> toasts = DGClient.getInstance().getNotificationManager().getActiveToasts();
        if (toasts.isEmpty()) {
            return;
        }
        MatrixStack matrices = ctx.getMatrices();
        float y = 10.0f;
        for (NotificationManager.Toast toast : toasts) {
            toast.updateAnimation(0.22f);
            float ease = MathUtil.easeOutCubic(toast.getSlideProgress());
            float titleW = DGFontRenderer.getWidth(toast.getTitle(), true, 0.95f);
            float msgW = DGFontRenderer.getWidth(toast.getMessage(), false, 0.85f);
            float cardW = Math.max(146.0f, Math.max(titleW, msgW) + 26.0f);
            float cardH = 30.0f;
            float x = screenWidth - (cardW + 8.0f) * ease;

            int accent = switch (toast.getType()) {
                case DETECTION -> ColorUtil.NEON_GREEN;
                case MODULE_ON -> ColorUtil.MINT_GREEN;
                case MODULE_OFF -> ColorUtil.MUTED_WHITE;
                case WARNING -> ColorUtil.PURE_WHITE;
                case INFO -> ColorUtil.NEON_GREEN;
            };

            Render2DUtil.drawGaussianGlow(matrices, x, y, cardW, cardH, 6.0f, 7.0f, accent, glowStrength * 0.65f);
            Render2DUtil.drawRoundedRect(matrices, x, y, cardW, cardH, 6.0f, ColorUtil.PANEL_DARK);
            Render2DUtil.drawRoundedOutline(matrices, x, y, cardW, cardH, 6.0f, 1.0f, ColorUtil.withAlpha(accent, 175));
            Render2DUtil.drawRoundedRect(matrices, x + 2.5f, y + 4.0f, 2.5f, cardH - 8.0f, 1.2f, accent);

            DGFontRenderer.drawScaledText(ctx, toast.getTitle(), x + 10.0f, y + 5.0f, ColorUtil.PURE_WHITE, true, 0.92f, false);
            DGFontRenderer.drawScaledText(ctx, toast.getMessage(), x + 10.0f, y + 16.5f, ColorUtil.SOFT_WHITE, false, 0.82f, false);

            // Progress bar along bottom
            float progressW = (cardW - 12.0f) * toast.getLifeFraction();
            Render2DUtil.drawRoundedRect(matrices, x + 6.0f, y + cardH - 3.0f, progressW, 1.6f, 0.8f, accent);

            y += (cardH + 5.0f) * ease;
        }
    }

    private void registerDefaultElements() {
        // 1. Watermark
        elements.add(new HudElement("watermark", "Watermark", 0.008f, 0.012f) {
            @Override
            public boolean isActiveByModule() {
                HudModule m = DGClient.getInstance().getModuleManager().get(HudModule.class);
                return m != null && m.watermark.getValue();
            }

            @Override
            public void render(DrawContext ctx, int sw, int sh, float delta, boolean preview) {
                HudModule m = DGClient.getInstance().getModuleManager().get(HudModule.class);
                float glow = m != null ? m.hudGlow.getValue().floatValue() : 1.0f;
                String brand = "DGClient";
                String tag = "v" + DGClient.VERSION + "  |  1.21.1";
                float brandW = DGFontRenderer.getWidth(brand, true, 1.05f);
                float tagW = DGFontRenderer.getWidth(tag, false, 0.86f);
                float w = brandW + tagW + 22.0f;
                float h = 20.0f;
                setMeasuredSize(w, h);
                float x = getX(sw);
                float y = getY(sh);

                MatrixStack ms = ctx.getMatrices();
                Render2DUtil.drawGaussianGlow(ms, x, y, w, h, 6.0f, 8.0f, ColorUtil.NEON_GREEN, glow * 0.8f);
                Render2DUtil.drawRoundedRect(ms, x, y, w, h, 6.0f, ColorUtil.PANEL_DARK);
                Render2DUtil.drawRoundedOutline(ms, x, y, w, h, 6.0f, 1.0f, ColorUtil.withAlpha(ColorUtil.NEON_GREEN, 190));

                DGFontRenderer.drawAnimatedGradientText(ctx, brand, x + 7.5f, y + 5.5f, true, 1.05f, 1.2f, glow);
                DGFontRenderer.drawScaledText(ctx, tag, x + 13.5f + brandW, y + 6.2f, ColorUtil.SOFT_WHITE, false, 0.86f, false);
            }
        });

        // 2. ArrayList
        elements.add(new HudElement("arraylist", "Module ArrayList", 0.99f, 0.012f) {
            @Override
            public boolean isActiveByModule() {
                HudModule m = DGClient.getInstance().getModuleManager().get(HudModule.class);
                return m != null && m.arrayList.getValue();
            }

            @Override
            public void render(DrawContext ctx, int sw, int sh, float delta, boolean preview) {
                HudModule m = DGClient.getInstance().getModuleManager().get(HudModule.class);
                float glow = m != null ? m.hudGlow.getValue().floatValue() : 1.0f;
                List<Module> all = DGClient.getInstance().getModuleManager().getModules();
                List<Module> visible = new ArrayList<>();
                for (Module mod : all) {
                    if (!mod.isVisibleInArrayList()) {
                        continue;
                    }
                    float target = mod.isEnabled() ? 1.0f : 0.0f;
                    mod.setArraySlideAnim(MathUtil.approach(mod.getArraySlideAnim(), target, 0.22f));
                    if (mod.getArraySlideAnim() > 0.02f) {
                        visible.add(mod);
                    }
                }
                if (visible.isEmpty() && preview) {
                    setMeasuredSize(112.0f, 36.0f);
                    float x = getX(sw);
                    float y = getY(sh);
                    Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, y, 112.0f, 36.0f, 5.0f, ColorUtil.PANEL_DARK);
                    DGFontRenderer.drawText(ctx, "ArrayList", x + 8.0f, y + 13.0f, ColorUtil.NEON_GREEN, false);
                    return;
                }

                visible.sort(Comparator.comparingDouble((Module mod) -> {
                    String suffix = mod.getDisplayInfo();
                    String full = suffix == null || suffix.isEmpty() ? mod.getName() : mod.getName() + " " + suffix;
                    return DGFontRenderer.getWidth(full, false, 0.92f);
                }).reversed());

                float maxW = 90.0f;
                for (Module mod : visible) {
                    String suffix = mod.getDisplayInfo();
                    String full = suffix == null || suffix.isEmpty() ? mod.getName() : mod.getName() + " [" + suffix + "]";
                    maxW = Math.max(maxW, DGFontRenderer.getWidth(full, false, 0.92f) + 14.0f);
                }
                float rowH = 14.5f;
                setMeasuredSize(maxW, Math.max(18.0f, visible.size() * rowH));

                float boxLeft = getX(sw);
                float rightEdge = boxLeft + maxW;
                boolean rightAligned = (boxLeft + maxW * 0.5f) > sw * 0.5f;
                float cursorY = getY(sh);

                MatrixStack ms = ctx.getMatrices();
                int idx = 0;
                for (Module mod : visible) {
                    float slide = MathUtil.easeOutCubic(mod.getArraySlideAnim());
                    String name = mod.getName();
                    String info = mod.getDisplayInfo();
                    float nameW = DGFontRenderer.getWidth(name, false, 0.92f);
                    float infoW = (info != null && !info.isEmpty())
                            ? DGFontRenderer.getWidth(" [" + info + "]", false, 0.86f)
                            : 0.0f;
                    float entryW = nameW + infoW + 12.0f;

                    float drawX = rightAligned
                            ? (rightEdge - entryW * slide)
                            : (boxLeft - (1.0f - slide) * entryW);

                    int accent = ColorUtil.animatedAccent(-idx * 0.22, 1.1f);
                    Render2DUtil.drawRoundedRect(ms, drawX, cursorY, entryW, rowH - 1.0f, 3.0f, 0xCC0A0F0C);
                    float barX = rightAligned ? (drawX + entryW - 2.2f) : drawX;
                    Render2DUtil.drawGaussianGlow(ms, barX, cursorY, 2.2f, rowH - 1.0f, 1.0f, 4.5f, accent, glow * 0.7f);
                    Render2DUtil.drawRoundedRect(ms, barX, cursorY, 2.2f, rowH - 1.0f, 1.0f, accent);

                    float textX = rightAligned ? (drawX + 5.0f) : (drawX + 6.0f);
                    DGFontRenderer.drawScaledText(ctx, name, textX, cursorY + 2.8f, accent, false, 0.92f, false);
                    if (infoW > 0.0f) {
                        DGFontRenderer.drawScaledText(ctx, " [" + info + "]", textX + nameW, cursorY + 3.1f, ColorUtil.PURE_WHITE, false, 0.86f, false);
                    }
                    cursorY += rowH * slide;
                    idx++;
                }
            }
        });

        // 3. Coordinates & Nether/Overworld Conversion
        elements.add(new HudElement("coordinates", "Coordinates", 0.008f, 0.94f) {
            @Override
            public boolean isActiveByModule() {
                HudModule m = DGClient.getInstance().getModuleManager().get(HudModule.class);
                return m != null && m.coordinates.getValue();
            }

            @Override
            public void render(DrawContext ctx, int sw, int sh, float delta, boolean preview) {
                MinecraftClient mc = MinecraftClient.getInstance();
                HudModule m = DGClient.getInstance().getModuleManager().get(HudModule.class);
                float glow = m != null ? m.hudGlow.getValue().floatValue() : 1.0f;

                double px = mc.player != null ? mc.player.getX() : 0.0;
                double py = mc.player != null ? mc.player.getY() : 64.0;
                double pz = mc.player != null ? mc.player.getZ() : 0.0;
                boolean inNether = mc.world != null && mc.world.getRegistryKey().getValue().getPath().contains("nether");
                boolean inEnd = mc.world != null && mc.world.getRegistryKey().getValue().getPath().contains("end");

                String primary = String.format(Locale.ROOT, "XYZ  %.1f,  %.1f,  %.1f", px, py, pz);
                String secondary = inEnd ? "Dimension  The End"
                        : inNether ? String.format(Locale.ROOT, "Overworld  %.0f,  %.0f", px * 8.0, pz * 8.0)
                        : String.format(Locale.ROOT, "Nether  %.1f,  %.1f", px / 8.0, pz / 8.0);

                float w = Math.max(DGFontRenderer.getWidth(primary, true, 0.92f), DGFontRenderer.getWidth(secondary, false, 0.84f)) + 16.0f;
                float h = 28.0f;
                setMeasuredSize(w, h);
                float x = getX(sw);
                float y = getY(sh);

                MatrixStack ms = ctx.getMatrices();
                Render2DUtil.drawGaussianGlow(ms, x, y, w, h, 5.5f, 6.0f, ColorUtil.NEON_GREEN, glow * 0.55f);
                Render2DUtil.drawRoundedRect(ms, x, y, w, h, 5.5f, ColorUtil.PANEL_DARK);
                Render2DUtil.drawRoundedOutline(ms, x, y, w, h, 5.5f, 1.0f, ColorUtil.BORDER_SUBTLE);

                DGFontRenderer.drawScaledText(ctx, primary, x + 7.5f, y + 4.5f, ColorUtil.PURE_WHITE, true, 0.92f, false);
                DGFontRenderer.drawScaledText(ctx, secondary, x + 7.5f, y + 15.8f, ColorUtil.MINT_GREEN, false, 0.84f, false);
            }
        });

        // 4. Telemetry (FPS, Ping, TPS, Speed, Direction, Biome, Time)
        elements.add(new HudElement("telemetry", "Telemetry", 0.008f, 0.84f) {
            @Override
            public boolean isActiveByModule() {
                HudModule m = DGClient.getInstance().getModuleManager().get(HudModule.class);
                return m != null && m.telemetry.getValue();
            }

            @Override
            public void render(DrawContext ctx, int sw, int sh, float delta, boolean preview) {
                MinecraftClient mc = MinecraftClient.getInstance();
                ServerRateTracker tracker = DGClient.getInstance().getServerRateTracker();
                HudModule m = DGClient.getInstance().getModuleManager().get(HudModule.class);
                float glow = m != null ? m.hudGlow.getValue().floatValue() : 1.0f;

                int fps = mc.getCurrentFps();
                int ping = tracker.getPingMs();
                float tps = tracker.getTps();
                double speed = tracker.getBlocksPerSecond();

                String dir = "N [+Z]";
                String biomeName = "plains";
                String timeStr = "12:00";

                if (mc.player != null && mc.world != null) {
                    dir = formatDirection(mc.player.getYaw());
                    BlockPos bp = mc.player.getBlockPos();
                    Optional<RegistryKey<Biome>> key = mc.world.getBiome(bp).getKey();
                    if (key.isPresent()) {
                        biomeName = key.get().getValue().getPath().replace('_', ' ');
                    }
                    long dayTime = (mc.world.getTimeOfDay() + 6000L) % 24000L;
                    if (dayTime < 0) dayTime += 24000L;
                    int hours = (int) (dayTime / 1000L);
                    int mins = (int) ((dayTime % 1000L) * 60L / 1000L);
                    timeStr = String.format(Locale.ROOT, "%02d:%02d", hours, mins);
                }

                String line1 = String.format(Locale.ROOT, "FPS %d   Ping %dms   TPS %.1f   Speed %.2f m/s", fps, ping, tps, speed);
                String line2 = String.format(Locale.ROOT, "Dir %s   Biome %s   Time %s", dir, biomeName, timeStr);

                float w = Math.max(DGFontRenderer.getWidth(line1, false, 0.86f), DGFontRenderer.getWidth(line2, false, 0.84f)) + 16.0f;
                float h = 27.0f;
                setMeasuredSize(w, h);
                float x = getX(sw);
                float y = getY(sh);

                MatrixStack ms = ctx.getMatrices();
                Render2DUtil.drawGaussianGlow(ms, x, y, w, h, 5.5f, 5.5f, ColorUtil.NEON_GREEN, glow * 0.45f);
                Render2DUtil.drawRoundedRect(ms, x, y, w, h, 5.5f, ColorUtil.PANEL_DARK);
                Render2DUtil.drawRoundedOutline(ms, x, y, w, h, 5.5f, 1.0f, ColorUtil.BORDER_SUBTLE);

                DGFontRenderer.drawScaledText(ctx, line1, x + 7.5f, y + 4.5f, ColorUtil.NEON_GREEN, false, 0.86f, false);
                DGFontRenderer.drawScaledText(ctx, line2, x + 7.5f, y + 15.2f, ColorUtil.SOFT_WHITE, false, 0.84f, false);
            }
        });

        // 5. Armor & Durability HUD
        elements.add(new HudElement("armor", "Armor HUD", 0.52f, 0.88f) {
            @Override
            public boolean isActiveByModule() {
                HudModule m = DGClient.getInstance().getModuleManager().get(HudModule.class);
                return m != null && m.armorHud.getValue();
            }

            @Override
            public void render(DrawContext ctx, int sw, int sh, float delta, boolean preview) {
                MinecraftClient mc = MinecraftClient.getInstance();
                float w = 88.0f;
                float h = 24.0f;
                setMeasuredSize(w, h);
                float x = getX(sw);
                float y = getY(sh);

                List<ItemStack> armor = new ArrayList<>();
                if (mc.player != null) {
                    for (int i = 3; i >= 0; i--) {
                        armor.add(mc.player.getInventory().getArmorStack(i));
                    }
                }
                boolean any = armor.stream().anyMatch(s -> !s.isEmpty());
                if (!any && !preview) {
                    return;
                }

                MatrixStack ms = ctx.getMatrices();
                Render2DUtil.drawRoundedRect(ms, x, y, w, h, 5.0f, 0xCC0A0F0C);
                Render2DUtil.drawRoundedOutline(ms, x, y, w, h, 5.0f, 1.0f, ColorUtil.BORDER_SUBTLE);

                for (int i = 0; i < 4; i++) {
                    float slotX = x + 4.0f + i * 21.0f;
                    ItemStack stack = i < armor.size() ? armor.get(i) : ItemStack.EMPTY;
                    if (stack.isEmpty()) {
                        if (preview) {
                            ItemStack dummy = switch (i) {
                                case 0 -> new ItemStack(Items.NETHERITE_HELMET);
                                case 1 -> new ItemStack(Items.NETHERITE_CHESTPLATE);
                                case 2 -> new ItemStack(Items.NETHERITE_LEGGINGS);
                                default -> new ItemStack(Items.NETHERITE_BOOTS);
                            };
                            ctx.drawItem(dummy, Math.round(slotX), Math.round(y + 2.0f));
                            Render2DUtil.drawRoundedRect(ms, slotX + 1.0f, y + 19.5f, 14.0f, 2.2f, 1.0f, ColorUtil.NEON_GREEN);
                        }
                        continue;
                    }
                    ctx.drawItem(stack, Math.round(slotX), Math.round(y + 2.0f));
                    if (stack.isDamageable()) {
                        float ratio = MathUtil.clamp(1.0f - (float) stack.getDamage() / (float) stack.getMaxDamage(), 0.0f, 1.0f);
                        int barCol = ColorUtil.healthGreenWhite(ratio);
                        Render2DUtil.drawRoundedRect(ms, slotX + 1.0f, y + 19.5f, 14.0f, 2.2f, 1.0f, 0x8818221C);
                        Render2DUtil.drawRoundedRect(ms, slotX + 1.0f, y + 19.5f, 14.0f * ratio, 2.2f, 1.0f, barCol);
                    }
                }
            }
        });

        // 6. Potion Effects HUD
        elements.add(new HudElement("potions", "Potion Effects", 0.99f, 0.78f) {
            @Override
            public boolean isActiveByModule() {
                HudModule m = DGClient.getInstance().getModuleManager().get(HudModule.class);
                return m != null && m.potionHud.getValue();
            }

            @Override
            public void render(DrawContext ctx, int sw, int sh, float delta, boolean preview) {
                MinecraftClient mc = MinecraftClient.getInstance();
                Collection<StatusEffectInstance> effects = mc.player != null ? mc.player.getStatusEffects() : List.of();
                if (effects.isEmpty() && !preview) {
                    return;
                }
                List<String> lines = new ArrayList<>();
                if (effects.isEmpty()) {
                    lines.add("Speed II  01:30");
                    lines.add("Fire Resistance  04:15");
                } else {
                    for (StatusEffectInstance eff : effects) {
                        String name = eff.getEffectType().value().getName().getString();
                        int amp = eff.getAmplifier() + 1;
                        String ampStr = amp > 1 ? " " + amp : "";
                        int secs = Math.max(0, eff.getDuration() / 20);
                        String dur = eff.isInfinite() ? "**" : String.format(Locale.ROOT, "%02d:%02d", secs / 60, secs % 60);
                        lines.add(name + ampStr + "  " + dur);
                    }
                }
                float maxW = 90.0f;
                for (String s : lines) {
                    maxW = Math.max(maxW, DGFontRenderer.getWidth(s, false, 0.86f) + 14.0f);
                }
                float h = lines.size() * 13.0f + 6.0f;
                setMeasuredSize(maxW, h);
                float x = getX(sw);
                float y = getY(sh);

                Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, y, maxW, h, 5.0f, ColorUtil.PANEL_DARK);
                Render2DUtil.drawRoundedOutline(ctx.getMatrices(), x, y, maxW, h, 5.0f, 1.0f, ColorUtil.BORDER_SUBTLE);
                float cy = y + 4.5f;
                for (String s : lines) {
                    DGFontRenderer.drawScaledText(ctx, s, x + 7.0f, cy, ColorUtil.MINT_GREEN, false, 0.86f, false);
                    cy += 13.0f;
                }
            }
        });

        // 7. Totem Counter HUD
        elements.add(new HudElement("totems", "Totem Counter", 0.48f, 0.88f) {
            @Override
            public boolean isActiveByModule() {
                HudModule m = DGClient.getInstance().getModuleManager().get(HudModule.class);
                return m != null && m.totemCounter.getValue();
            }

            @Override
            public void render(DrawContext ctx, int sw, int sh, float delta, boolean preview) {
                MinecraftClient mc = MinecraftClient.getInstance();
                int count = 0;
                if (mc.player != null) {
                    for (int i = 0; i < mc.player.getInventory().size(); i++) {
                        ItemStack s = mc.player.getInventory().getStack(i);
                        if (s.isOf(Items.TOTEM_OF_UNDYING)) {
                            count += s.getCount();
                        }
                    }
                    if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        // getInventory().size() already includes offhand in 1.21.1 (slots 0..35, 36..39, 40),
                        // so no double count!
                    }
                }
                float w = 42.0f;
                float h = 22.0f;
                setMeasuredSize(w, h);
                float x = getX(sw);
                float y = getY(sh);

                MatrixStack ms = ctx.getMatrices();
                int border = count > 0 ? ColorUtil.withAlpha(ColorUtil.NEON_GREEN, 180) : ColorUtil.BORDER_SUBTLE;
                Render2DUtil.drawRoundedRect(ms, x, y, w, h, 5.0f, ColorUtil.PANEL_DARK);
                Render2DUtil.drawRoundedOutline(ms, x, y, w, h, 5.0f, 1.0f, border);
                ctx.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), Math.round(x + 3.0f), Math.round(y + 3.0f));
                int txtCol = count > 0 ? ColorUtil.NEON_GREEN : ColorUtil.PURE_WHITE;
                DGFontRenderer.drawScaledText(ctx, String.valueOf(count), x + 21.5f, y + 7.0f, txtCol, true, 0.95f, false);
            }
        });
    }

    private static String formatDirection(float yaw) {
        float norm = ((yaw % 360.0f) + 360.0f) % 360.0f;
        int idx = Math.round(norm / 45.0f) % 8;
        return switch (idx) {
            case 0 -> "S [+Z]";
            case 1 -> "SW [-X +Z]";
            case 2 -> "W [-X]";
            case 3 -> "NW [-X -Z]";
            case 4 -> "N [-Z]";
            case 5 -> "NE [+X -Z]";
            case 6 -> "E [+X]";
            default -> "SE [+X +Z]";
        };
    }
}
