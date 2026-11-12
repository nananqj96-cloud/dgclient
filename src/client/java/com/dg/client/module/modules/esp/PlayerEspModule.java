package com.dg.client.module.modules.esp;

import com.dg.client.DGClient;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.module.modules.render.NametagsModule;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.setting.IntSetting;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Full-featured Player ESP supporting Box, Shader Glow Outline, 2D Box, and Corner Box modes,
 * friend color integration, invisible-player rendering, frustum culling, and info labels.
 */
public final class PlayerEspModule extends Module {
    public final EnumSetting<EspRenderHelper.RenderMode> mode = addSetting(new EnumSetting<>(
            "Mode", "ESP box/shader style", EspRenderHelper.RenderMode.SHADER_GLOW));
    public final DoubleSetting range = addSetting(new DoubleSetting(
            "Range", "Maximum player ESP distance in blocks", 192.0, 16.0, 512.0, 8.0));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting(
            "Through Walls", "Renders ESP through blocks", true));
    public final BooleanSetting showInvisible = addSetting(new BooleanSetting(
            "Invisibles", "Highlights invisible players", true));
    public final BooleanSetting infoLabel = addSetting(new BooleanSetting(
            "Info Label", "Draws name/HP/armor/distance label when Nametags is off", true));
    public final IntSetting maxTargets = addSetting(new IntSetting(
            "Max Targets", "Nearest player cap for stable frame time", 64, 4, 200, 4));
    public final DoubleSetting lineWidth = addSetting(new DoubleSetting(
            "Line Width", "Outline width", 1.8, 1.0, 4.5, 0.2));
    public final IntSetting fillAlpha = addSetting(new IntSetting(
            "Fill Alpha", "Translucent box fill opacity", 36, 5, 160, 5));
    public final ColorSetting playerColor = addSetting(new ColorSetting(
            "Enemy Color", "Color for non-friend players", ColorUtil.NEON_GREEN));
    public final ColorSetting friendColor = addSetting(new ColorSetting(
            "Friend Color", "Color for friends", ColorUtil.MINT_GREEN));

    private final List<EspRenderHelper.ProjectedRect> rects2d = new ArrayList<>();

    public PlayerEspModule() {
        super("PlayerESP", "Highlights players with Box, GLSL Shader Glow, 2D Box, or Corner Box", ModuleCategory.RENDER);
    }

    @Override
    public String getDisplayInfo() {
        return mode.getValue().name();
    }

    public boolean shouldShaderOutline(Entity entity) {
        if (!isEnabled() || mode.getValue() != EspRenderHelper.RenderMode.SHADER_GLOW) {
            return false;
        }
        if (!(entity instanceof PlayerEntity p) || p == mc.player || mc.player == null) {
            return false;
        }
        if (p.isInvisible() && !showInvisible.getValue()) {
            return false;
        }
        return mc.player.squaredDistanceTo(p) <= range.getValue() * range.getValue();
    }

    public int getColorForPlayer(PlayerEntity p) {
        boolean friend = DGClient.getInstance().getFriendManager().isFriend(p.getName().getString());
        return friend ? friendColor.getColor() : playerColor.getColor();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        rects2d.clear();
        if (mc.world == null || mc.player == null) {
            return;
        }
        double maxSq = range.getValue() * range.getValue();
        List<PlayerEntity> candidates = new ArrayList<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) continue;
            if (p.isInvisible() && !showInvisible.getValue()) continue;
            if (mc.player.squaredDistanceTo(p) > maxSq) continue;
            candidates.add(p);
        }
        candidates.sort(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)));

        float delta = event.getTickDelta();
        NametagsModule nt = DGClient.getInstance().getModuleManager().get(NametagsModule.class);
        boolean nametagsActive = nt != null && nt.isEnabled();

        int count = 0;
        for (PlayerEntity p : candidates) {
            if (count >= maxTargets.getValue()) {
                break;
            }
            double ix = MathUtil.lerp(p.lastX, p.getX(), delta) - p.getX();
            double iy = MathUtil.lerp(p.lastY, p.getY(), delta) - p.getY();
            double iz = MathUtil.lerp(p.lastZ, p.getZ(), delta) - p.getZ();
            Box box = p.getBoundingBox().offset(ix, iy, iz).expand(0.04);

            if (event.getFrustum() != null && !event.getFrustum().isVisible(box.expand(0.35))) {
                continue;
            }
            int col = getColorForPlayer(p);
            float hpRatio = MathUtil.clamp(p.getHealth() / Math.max(1.0f, p.getMaxHealth()), 0.0f, 1.0f);
            EspRenderHelper.renderTargetBox(event, box, mode.getValue(), col, fillAlpha.getValue(),
                    lineWidth.getValue().floatValue(), throughWalls.getValue(), rects2d, hpRatio);

            if (infoLabel.getValue() && !nametagsActive) {
                int armorPct = computeArmorDurabilityPercent(p);
                double dist = Math.sqrt(mc.player.squaredDistanceTo(p));
                String tag = String.format(Locale.ROOT, "%s %.0fHP%s %.0fm",
                        p.getName().getString(),
                        p.getHealth() + p.getAbsorptionAmount(),
                        armorPct >= 0 ? (" " + armorPct + "%A") : "",
                        dist);
                Render3DUtil.drawBillboardLabel(event.getMatrices(), event.getCamera(),
                        new Vec3d(box.getCenter().x, box.maxY + 0.3, box.getCenter().z),
                        tag, ColorUtil.PURE_WHITE, col, 0.9f, throughWalls.getValue());
            }
            count++;
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!rects2d.isEmpty() && !mc.options.hudHidden) {
            EspRenderHelper.draw2DProjectedRects(event.getContext(), rects2d);
        }
    }

    private static int computeArmorDurabilityPercent(PlayerEntity p) {
        int totalMax = 0;
        int totalRem = 0;
        for (EquipmentSlot eqSlot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack s = p.getEquippedStack(eqSlot);
            if (!s.isEmpty() && s.isDamageable()) {
                totalMax += s.getMaxDamage();
                totalRem += (s.getMaxDamage() - s.getDamage());
            }
        }
        return totalMax > 0 ? Math.round((totalRem * 100.0f) / totalMax) : -1;
    }
}
