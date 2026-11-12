package com.dg.client.module.modules.render;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.IntSetting;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flashes a decaying neon green/white volumetric glow on entities struck by the player.
 */
public final class HitGlowModule extends Module {
    public final IntSetting durationMs = addSetting(new IntSetting(
            "Duration (ms)", "Fade duration after striking an entity", 550, 150, 2000, 50));
    public final ColorSetting hitColor = addSetting(new ColorSetting(
            "Glow Color", "Hit flash accent color", ColorUtil.withAlpha(ColorUtil.NEON_GREEN, 160)));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting(
            "Through Walls", "Renders hit flash through blocks", true));

    private final Map<Integer, Long> hitTimestamps = new ConcurrentHashMap<>();

    public HitGlowModule() {
        super("HitGlow", "Neon green/white flash and decaying glow on entities you hit", ModuleCategory.RENDER);
    }

    public void recordHit(Entity target) {
        if (isEnabled() && target != null) {
            hitTimestamps.put(target.getId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (hitTimestamps.isEmpty() || event.getWorld() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long maxAge = durationMs.getValue();
        float delta = event.getTickDelta();

        Iterator<Map.Entry<Integer, Long>> it = hitTimestamps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            long elapsed = now - entry.getValue();
            if (elapsed > maxAge) {
                it.remove();
                continue;
            }
            Entity entity = event.getWorld().getEntityById(entry.getKey());
            if (entity == null || !entity.isAlive()) {
                it.remove();
                continue;
            }
            float life = 1.0f - (float) elapsed / (float) maxAge;
            double ix = MathUtil.lerp(entity.prevX, entity.getX(), delta) - entity.getX();
            double iy = MathUtil.lerp(entity.prevY, entity.getY(), delta) - entity.getY();
            double iz = MathUtil.lerp(entity.prevZ, entity.getZ(), delta) - entity.getZ();
            Box box = entity.getBoundingBox().offset(ix, iy, iz).expand(0.05 + (1.0f - life) * 0.12);

            int base = hitColor.getColor();
            int fill = ColorUtil.scaleAlpha(base, life * 0.45f);
            int edge = ColorUtil.withAlpha(ColorUtil.lerp(ColorUtil.NEON_GREEN, ColorUtil.PURE_WHITE, life), Math.round(235 * life));
            Render3DUtil.drawFilledBox(event.getMatrices(), event.getCamera(), box, fill, throughWalls.getValue());
            Render3DUtil.drawGlowingBox(event.getMatrices(), event.getCamera(), box,
                    ColorUtil.NEON_GREEN, edge, life * 1.15f, throughWalls.getValue());
        }
    }
}
