package com.dg.client.module.modules.render;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.util.ColorUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Spawns floating neon green (heal) and crisp white (damage) numbers when nearby
 * LivingEntities lose or gain health.
 */
public final class DamageIndicatorsModule extends Module {
    private record FloatingNumber(Vec3d startPos, float deltaHp, long spawnedAt) {
    }

    public final BooleanSetting showHeals = addSetting(new BooleanSetting(
            "Show Healing", "Displays green +HP indicators when entities heal", true));
    public final DoubleSetting maxRange = addSetting(new DoubleSetting(
            "Range", "Maximum tracking distance in blocks", 48.0, 8.0, 128.0, 4.0));
    public final DoubleSetting scale = addSetting(new DoubleSetting(
            "Scale", "Floating text size multiplier", 1.0, 0.5, 2.0, 0.1));

    private final Map<Integer, Float> trackedHealth = new HashMap<>();
    private final CopyOnWriteArrayList<FloatingNumber> indicators = new CopyOnWriteArrayList<>();

    public DamageIndicatorsModule() {
        super("DamageIndicators", "Floating damage and heal numbers in DGClient's green/white style", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) {
            trackedHealth.clear();
            return;
        }
        double maxDistSq = maxRange.getValue() * maxRange.getValue();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == mc.player) {
                continue;
            }
            if (mc.player.squaredDistanceTo(living) > maxDistSq) {
                continue;
            }
            float current = living.getHealth() + living.getAbsorptionAmount();
            Float previous = trackedHealth.put(living.getId(), current);
            if (previous != null) {
                float diff = current - previous;
                if (Math.abs(diff) >= 0.25f) {
                    if (diff < 0.0f || showHeals.getValue()) {
                        Vec3d pos = living.getPos().add(0.0, living.getHeight() + 0.25, 0.0);
                        indicators.add(new FloatingNumber(pos, diff, System.currentTimeMillis()));
                        if (indicators.size() > 64) {
                            indicators.remove(0);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        long now = System.currentTimeMillis();
        indicators.removeIf(i -> now - i.spawnedAt() > 1150L);
        for (FloatingNumber fn : indicators) {
            float progress = (now - fn.spawnedAt()) / 1150.0f;
            Vec3d pos = fn.startPos().add(0.0, progress * 0.95, 0.0);
            boolean heal = fn.deltaHp() > 0.0f;
            String text = String.format(Locale.ROOT, "%s%.1f", heal ? "+" : "", fn.deltaHp());
            int textColor = heal ? ColorUtil.NEON_GREEN : ColorUtil.PURE_WHITE;
            int borderColor = heal ? ColorUtil.MINT_GREEN : ColorUtil.NEON_GREEN;
            Render3DUtil.drawBillboardLabel(event.getMatrices(), event.getCamera(), pos,
                    text, textColor, borderColor, scale.getValue().floatValue(), true);
        }
    }
}
