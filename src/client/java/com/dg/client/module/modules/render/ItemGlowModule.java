package com.dg.client.module.modules.render;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Highlights dropped ItemEntities with a glowing neon aura and floating name + stack count label.
 */
public final class ItemGlowModule extends Module {
    public final BooleanSetting showLabels = addSetting(new BooleanSetting(
            "Name Labels", "Displays floating item name and stack count", true));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting(
            "Through Walls", "Renders item glow and labels through terrain", true));
    public final DoubleSetting range = addSetting(new DoubleSetting(
            "Range", "Maximum item highlight distance in blocks", 48.0, 8.0, 128.0, 4.0));
    public final ColorSetting glowColor = addSetting(new ColorSetting(
            "Glow Color", "Item box glow color", ColorUtil.NEON_GREEN));

    public ItemGlowModule() {
        super("ItemGlow", "Glowing neon box and stack-count labels on dropped items", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        double maxSq = range.getValue() * range.getValue();
        float delta = event.getTickDelta();
        int rendered = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity item) || mc.player.squaredDistanceTo(item) > maxSq) {
                continue;
            }
            if (event.getFrustum() != null && !event.getFrustum().isVisible(item.getBoundingBox().expand(0.5))) {
                continue;
            }
            double ix = MathUtil.lerp(item.prevX, item.getX(), delta);
            double iy = MathUtil.lerp(item.prevY, item.getY(), delta);
            double iz = MathUtil.lerp(item.prevZ, item.getZ(), delta);
            Box box = new Box(ix - 0.18, iy + 0.05, iz - 0.18, ix + 0.18, iy + 0.42, iz + 0.18);

            Render3DUtil.drawGlowingBox(event.getMatrices(), event.getCamera(), box,
                    glowColor.getColor(), ColorUtil.PURE_WHITE, 0.7f, throughWalls.getValue());

            if (showLabels.getValue()) {
                ItemStack stack = item.getStack();
                String label = stack.getName().getString() + (stack.getCount() > 1 ? " x" + stack.getCount() : "");
                Render3DUtil.drawBillboardLabel(event.getMatrices(), event.getCamera(),
                        new Vec3d(ix, iy + 0.68, iz), label, ColorUtil.PURE_WHITE, glowColor.getColor(), 0.88f, throughWalls.getValue());
            }
            if (++rendered >= 96) {
                break;
            }
        }
    }
}
