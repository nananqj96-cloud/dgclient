package com.dg.client.module.modules.esp;

import com.dg.client.DGClient;
import com.dg.client.core.detection.DetectionEntry;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render2DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.setting.IntSetting;
import com.dg.client.setting.SettingGroup;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draws anti-aliased tracer lines from the Crosshair, Screen Bottom, or Screen Top
 * to selected targets with Distance / Type / Health / Friend-Enemy green-white palettes
 * and optional gradient alpha fade along each line.
 */
public final class TracersModule extends Module {
    public enum Anchor {
        CROSSHAIR,
        BOTTOM,
        TOP
    }

    public enum ColorMode {
        DISTANCE,
        TYPE,
        HEALTH,
        FRIEND_ENEMY
    }

    private record TracerLine(float targetX, float targetY, int color, double distSq) {
    }

    private final SettingGroup targetGroup = createGroup("Targets");

    public final EnumSetting<Anchor> anchor = addSetting(new EnumSetting<>(
            "Anchor", "Screen origin point for tracer lines", Anchor.CROSSHAIR));
    public final EnumSetting<ColorMode> colorMode = addSetting(new EnumSetting<>(
            "Color Mode", "Green/white palette coloring rule", ColorMode.DISTANCE));
    public final BooleanSetting gradientFade = addSetting(new BooleanSetting(
            "Gradient Fade", "Fades tracer opacity smoothly from anchor toward target", true));
    public final DoubleSetting width = addSetting(new DoubleSetting(
            "Line Width", "Stroke thickness in pixels", 1.35, 0.6, 3.5, 0.1));
    public final IntSetting opacity = addSetting(new IntSetting(
            "Opacity", "Maximum tracer alpha (0..255)", 200, 30, 255, 5));
    public final DoubleSetting range = addSetting(new DoubleSetting(
            "Range", "Maximum tracer distance in blocks", 192.0, 16.0, 512.0, 8.0));
    public final IntSetting maxTargets = addSetting(new IntSetting(
            "Max Targets", "Nearest target cap", 48, 4, 160, 4));

    public final BooleanSetting players = targetGroup.add(new BooleanSetting("Players", "Trace other players", true));
    public final BooleanSetting hostiles = targetGroup.add(new BooleanSetting("Hostiles", "Trace hostile mobs", false));
    public final BooleanSetting passives = targetGroup.add(new BooleanSetting("Passives", "Trace passive mobs", false));
    public final BooleanSetting items = targetGroup.add(new BooleanSetting("Items", "Trace dropped items", false));
    public final BooleanSetting vehicles = targetGroup.add(new BooleanSetting("Vehicles", "Trace boats & minecarts", false));
    public final BooleanSetting storage = targetGroup.add(new BooleanSetting("Storage", "Trace StorageESP containers", false));
    public final BooleanSetting detections = targetGroup.add(new BooleanSetting("BaseFinder Chunks", "Trace active suspicious chunks", false));

    private final List<TracerLine> frameLines = new ArrayList<>();

    public TracersModule() {
        super("Tracers", "Crisp green/white tracer lines from crosshair or screen bottom to targets", ModuleCategory.RENDER);
    }

    @Override
    public String getDisplayInfo() {
        return colorMode.getValue().name();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        frameLines.clear();
        if (mc.world == null || mc.player == null || mc.getWindow() == null) {
            return;
        }
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        Matrix4f posMat = event.getPositionMatrix();
        Matrix4f projMat = event.getProjectionMatrix();
        Vec3d camPos = event.getCamera().getCameraPos();
        float delta = event.getTickDelta();
        double maxDist = range.getValue();
        double maxSq = maxDist * maxDist;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !e.isAlive()) continue;
            double dSq = mc.player.squaredDistanceTo(e);
            if (dSq > maxSq) continue;

            int color = evaluateEntityColor(e, Math.sqrt(dSq), maxDist);
            if (color == 0) continue;

            Vec3d target = new Vec3d(
                    MathUtil.lerp(e.lastX, e.getX(), delta),
                    MathUtil.lerp(e.lastY, e.getY(), delta) + e.getHeight() * 0.5,
                    MathUtil.lerp(e.lastZ, e.getZ(), delta)
            );
            float[] pt = projectClamped(target, camPos, posMat, projMat, sw, sh);
            frameLines.add(new TracerLine(pt[0], pt[1], color, dSq));
        }

        if (storage.getValue()) {
            StorageEspModule se = DGClient.getInstance().getModuleManager().get(StorageEspModule.class);
            if (se != null) {
                for (BlockPos bp : se.collectTrackedPositions(maxDist)) {
                    double dSq = mc.player.squaredDistanceTo(Vec3d.ofCenter(bp));
                    int col = colorMode.getValue() == ColorMode.DISTANCE
                            ? ColorUtil.distanceGreenWhite(Math.sqrt(dSq), maxDist)
                            : ColorUtil.MINT_GREEN;
                    float[] pt = projectClamped(Vec3d.ofCenter(bp), camPos, posMat, projMat, sw, sh);
                    frameLines.add(new TracerLine(pt[0], pt[1], col, dSq));
                }
            }
        }

        if (detections.getValue()) {
            String dim = mc.world.getRegistryKey().getValue().toString();
            for (DetectionEntry d : DGClient.getInstance().getDetectionRegistry().getAll()) {
                if (d.getStatus() == DetectionEntry.Status.IGNORED || !d.getDimension().equals(dim)) continue;
                Vec3d p = new Vec3d(d.getCenterX(), d.getCenterY(), d.getCenterZ());
                double dSq = mc.player.squaredDistanceTo(p);
                if (dSq <= maxSq * 4.0) {
                    float[] pt = projectClamped(p, camPos, posMat, projMat, sw, sh);
                    frameLines.add(new TracerLine(pt[0], pt[1], ColorUtil.NEON_GREEN, dSq));
                }
            }
        }

        frameLines.sort(Comparator.comparingDouble(TracerLine::distSq));
        if (frameLines.size() > maxTargets.getValue()) {
            frameLines.subList(maxTargets.getValue(), frameLines.size()).clear();
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (frameLines.isEmpty() || mc.options.hudHidden) {
            return;
        }
        int sw = event.getScreenWidth();
        int sh = event.getScreenHeight();
        float ax = sw * 0.5f;
        float ay = switch (anchor.getValue()) {
            case CROSSHAIR -> sh * 0.5f;
            case BOTTOM -> sh - 2.0f;
            case TOP -> 2.0f;
        };
        Matrix3x2fStack ms = event.getContext().getMatrices();
        float lw = width.getValue().floatValue();
        int maxAlpha = opacity.getValue();

        for (TracerLine line : frameLines) {
            int endCol = ColorUtil.withAlpha(line.color(), maxAlpha);
            int startCol = gradientFade.getValue()
                    ? ColorUtil.withAlpha(ColorUtil.PURE_WHITE, Math.max(18, maxAlpha / 4))
                    : endCol;
            Render2DUtil.drawLine2D(ms, ax, ay, line.targetX(), line.targetY(), lw, startCol, endCol);
        }
    }

    private int evaluateEntityColor(Entity e, double dist, double maxDist) {
        boolean matched = false;
        int typeColor = ColorUtil.PURE_WHITE;
        boolean friend = false;

        if (e instanceof PlayerEntity p) {
            if (!players.getValue()) return 0;
            matched = true;
            friend = DGClient.getInstance().getFriendManager().isFriend(p.getName().getString());
            typeColor = friend ? ColorUtil.MINT_GREEN : ColorUtil.NEON_GREEN;
        } else if (e instanceof Monster) {
            if (!hostiles.getValue()) return 0;
            matched = true;
            typeColor = ColorUtil.NEON_GREEN;
        } else if (e instanceof PassiveEntity) {
            if (!passives.getValue()) return 0;
            matched = true;
            typeColor = ColorUtil.MINT_GREEN;
        } else if (e instanceof ItemEntity) {
            if (!items.getValue()) return 0;
            matched = true;
            typeColor = ColorUtil.PURE_WHITE;
        } else if (e instanceof BoatEntity || e instanceof AbstractMinecartEntity) {
            if (!vehicles.getValue()) return 0;
            matched = true;
            typeColor = ColorUtil.SOFT_WHITE;
        }
        if (!matched) {
            return 0;
        }

        return switch (colorMode.getValue()) {
            case DISTANCE -> friend ? ColorUtil.MINT_GREEN : ColorUtil.distanceGreenWhite(dist, maxDist);
            case TYPE -> typeColor;
            case HEALTH -> (e instanceof LivingEntity le)
                    ? ColorUtil.healthGreenWhite(le.getHealth() / Math.max(1.0f, le.getMaxHealth()))
                    : typeColor;
            case FRIEND_ENEMY -> friend ? ColorUtil.MINT_GREEN : ColorUtil.PURE_WHITE;
        };
    }

    private static float[] projectClamped(Vec3d worldPos, Vec3d camPos, Matrix4f posMat, Matrix4f projMat, int sw, int sh) {
        Vector4f clip = new Vector4f(
                (float) (worldPos.x - camPos.x),
                (float) (worldPos.y - camPos.y),
                (float) (worldPos.z - camPos.z),
                1.0f
        );
        clip.mul(posMat);
        clip.mul(projMat);
        boolean behind = clip.w <= 0.05f;
        float w = Math.max(0.05f, Math.abs(clip.w));
        float ndcX = clip.x / w;
        float ndcY = clip.y / w;
        if (behind) {
            ndcX = -ndcX;
            ndcY = -ndcY;
        }
        float sx = (ndcX * 0.5f + 0.5f) * sw;
        float sy = (1.0f - (ndcY * 0.5f + 0.5f)) * sh;
        if (behind) {
            sx = sx < sw * 0.5f ? 0.0f : sw;
            sy = MathUtil.clamp(sy, 0.0f, (float) sh);
        } else {
            sx = MathUtil.clamp(sx, 0.0f, (float) sw);
            sy = MathUtil.clamp(sy, 0.0f, (float) sh);
        }
        return new float[]{sx, sy};
    }
}
