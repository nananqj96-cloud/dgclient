package com.dg.client.module.modules.render;

import com.dg.client.DGClient;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.Render3DUtil;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Replaces vanilla player nameplates with crisp glowing 2D-projected DGClient cards
 * showing username, friend badge, HP + absorption, ping, distance, and equipped armor/hand items.
 */
public final class NametagsModule extends Module {
    private record ProjectedTag(PlayerEntity player, float sx, float sy, double dist) {
    }

    public final BooleanSetting showHealth = addSetting(new BooleanSetting("Health", "Displays numeric HP + absorption", true));
    public final BooleanSetting showPing = addSetting(new BooleanSetting("Ping", "Displays latency in ms", true));
    public final BooleanSetting showDistance = addSetting(new BooleanSetting("Distance", "Displays distance in meters", true));
    public final BooleanSetting showArmor = addSetting(new BooleanSetting("Armor & Items", "Renders equipped armor and held items above tag", true));
    public final DoubleSetting scale = addSetting(new DoubleSetting("Scale", "Nametag UI scale", 0.92, 0.6, 1.5, 0.05));
    public final DoubleSetting maxRange = addSetting(new DoubleSetting("Max Range", "Maximum nametag distance", 192.0, 24.0, 512.0, 8.0));

    private final List<ProjectedTag> frameTags = new ArrayList<>();

    public NametagsModule() {
        super("Nametags", "Custom glowing player nametags with HP, armor, ping, friend status, and distance", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        frameTags.clear();
        if (mc.world == null || mc.player == null || mc.getWindow() == null) {
            return;
        }
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        Matrix4f posMat = new Matrix4f(event.getPositionMatrix());
        Matrix4f projMat = new Matrix4f(event.getProjectionMatrix());
        float delta = event.getTickDelta();
        double maxSq = maxRange.getValue() * maxRange.getValue();

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) {
                continue;
            }
            double distSq = mc.player.squaredDistanceTo(p);
            if (distSq > maxSq) {
                continue;
            }
            double ix = MathUtil.lerp(p.lastX, p.getX(), delta);
            double iy = MathUtil.lerp(p.lastY, p.getY(), delta) + p.getHeight() + 0.35;
            double iz = MathUtil.lerp(p.lastZ, p.getZ(), delta);
            float[] screen = Render3DUtil.projectToScreen(new Vec3d(ix, iy, iz), event.getCamera(), posMat, projMat, sw, sh);
            if (screen != null) {
                frameTags.add(new ProjectedTag(p, screen[0], screen[1], Math.sqrt(distSq)));
            }
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (frameTags.isEmpty() || mc.options.hudHidden) {
            return;
        }
        DrawContext ctx = event.getContext();
        Matrix3x2fStack ms = ctx.getMatrices();
        float s = scale.getValue().floatValue();

        for (ProjectedTag tag : frameTags) {
            PlayerEntity p = tag.player();
            String name = p.getName().getString();
            boolean friend = DGClient.getInstance().getFriendManager().isFriend(name);

            StringBuilder sb = new StringBuilder();
            if (friend) sb.append("[F] ");
            sb.append(name);
            if (showHealth.getValue()) {
                float hp = p.getHealth() + p.getAbsorptionAmount();
                sb.append(String.format(Locale.ROOT, "  %.1fHP", hp));
            }
            if (showPing.getValue() && mc.getNetworkHandler() != null) {
                PlayerListEntry ple = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                if (ple != null) {
                    sb.append("  ").append(ple.getLatency()).append("ms");
                }
            }
            if (showDistance.getValue()) {
                sb.append(String.format(Locale.ROOT, "  %.0fm", tag.dist()));
            }

            String full = sb.toString();
            float textW = DGFontRenderer.getWidth(full, true, s);
            float cardW = textW + 12.0f;
            float cardH = 15.0f * s + 2.0f;
            float x = tag.sx() - cardW * 0.5f;
            float y = tag.sy() - cardH;

            int accent = friend ? ColorUtil.MINT_GREEN : ColorUtil.NEON_GREEN;
            Render2DUtil.drawGaussianGlow(ms, x, y, cardW, cardH, 4.5f, 5.0f, accent, 0.65f);
            Render2DUtil.drawRoundedRect(ms, x, y, cardW, cardH, 4.5f, ColorUtil.PANEL_DARK);
            Render2DUtil.drawRoundedOutline(ms, x, y, cardW, cardH, 4.5f, 1.0f, accent);
            DGFontRenderer.drawScaledText(ctx, full, x + 6.0f, y + 3.5f * s, friend ? ColorUtil.MINT_GREEN : ColorUtil.PURE_WHITE, true, s, false);

            if (showArmor.getValue()) {
                List<ItemStack> gear = new ArrayList<>();
                if (!p.getMainHandStack().isEmpty()) gear.add(p.getMainHandStack());
                for (EquipmentSlot eqSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    ItemStack a = p.getEquippedStack(eqSlot);
                    if (!a.isEmpty()) gear.add(a);
                }
                if (!p.getOffHandStack().isEmpty()) gear.add(p.getOffHandStack());
                if (!gear.isEmpty()) {
                    float totalGearW = gear.size() * 17.0f;
                    float gx = tag.sx() - totalGearW * 0.5f;
                    float gy = y - 18.0f;
                    for (ItemStack item : gear) {
                        ctx.drawItem(item, Math.round(gx), Math.round(gy));
                        gx += 17.0f;
                    }
                }
            }
        }
    }
}
