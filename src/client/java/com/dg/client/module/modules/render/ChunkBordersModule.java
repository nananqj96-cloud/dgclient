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
import net.minecraft.util.math.Vec3d;

/**
 * Renders a glowing neon green & white 16x16 chunk grid with configurable Y-range
 * and sub-chunk 16-block vertical section rings.
 */
public final class ChunkBordersModule extends Module {
    public final BooleanSetting subChunkRings = addSetting(new BooleanSetting(
            "Sub-Chunk Rings", "Draws horizontal rings every 16 Y-blocks", true));
    public final BooleanSetting innerGrid = addSetting(new BooleanSetting(
            "4-Block Subgrid", "Draws subtle 4-block subdivisions along chunk walls", true));
    public final BooleanSetting followPlayerY = addSetting(new BooleanSetting(
            "Relative to Player Y", "Centers vertical grid height around player Y", true));
    public final IntSetting minY = addSetting(new IntSetting(
            "Min Y", "Bottom Y of chunk border when fixed", -64, -64, 320, 16)
            .visibleWhen(() -> !followPlayerY.getValue()));
    public final IntSetting maxY = addSetting(new IntSetting(
            "Max Y", "Top Y of chunk border when fixed", 128, -64, 320, 16)
            .visibleWhen(() -> !followPlayerY.getValue()));
    public final IntSetting verticalSpan = addSetting(new IntSetting(
            "Vertical Span", "Half-height above and below player", 32, 8, 128, 8)
            .visibleWhen(followPlayerY::getValue));
    public final ColorSetting cornerColor = addSetting(new ColorSetting(
            "Corner Color", "Primary 16x16 corner pillar color", ColorUtil.NEON_GREEN));

    public ChunkBordersModule() {
        super("ChunkBorders", "Glowing neon green/white chunk border grid with configurable Y range", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        int cx = mc.player.getChunkPos().getStartX();
        int cz = mc.player.getChunkPos().getStartZ();

        double yBottom = followPlayerY.getValue()
                ? Math.max(mc.world.getBottomY(), Math.floor(mc.player.getY()) - verticalSpan.getValue())
                : minY.getValue();
        double yTop = followPlayerY.getValue()
                ? Math.min(mc.world.getTopY(), Math.floor(mc.player.getY()) + verticalSpan.getValue())
                : maxY.getValue();

        int mainCol = cornerColor.getColor();
        int subCol = ColorUtil.withAlpha(ColorUtil.PURE_WHITE, 95);

        // 4 main chunk corners
        double[][] corners = {{cx, cz}, {cx + 16, cz}, {cx + 16, cz + 16}, {cx, cz + 16}};
        for (double[] c : corners) {
            Render3DUtil.drawLine3D(event.getMatrices(), event.getCamera(),
                    new Vec3d(c[0], yBottom, c[1]), new Vec3d(c[0], yTop, c[1]), mainCol, mainCol, 2.4f, false);
        }

        if (innerGrid.getValue()) {
            for (int step = 4; step < 16; step += 4) {
                Render3DUtil.drawLine3D(event.getMatrices(), event.getCamera(),
                        new Vec3d(cx + step, yBottom, cz), new Vec3d(cx + step, yTop, cz), subCol, subCol, 1.1f, false);
                Render3DUtil.drawLine3D(event.getMatrices(), event.getCamera(),
                        new Vec3d(cx + step, yBottom, cz + 16), new Vec3d(cx + step, yTop, cz + 16), subCol, subCol, 1.1f, false);
                Render3DUtil.drawLine3D(event.getMatrices(), event.getCamera(),
                        new Vec3d(cx, yBottom, cz + step), new Vec3d(cx, yTop, cz + step), subCol, subCol, 1.1f, false);
                Render3DUtil.drawLine3D(event.getMatrices(), event.getCamera(),
                        new Vec3d(cx + 16, yBottom, cz + step), new Vec3d(cx + 16, yTop, cz + step), subCol, subCol, 1.1f, false);
            }
        }

        if (subChunkRings.getValue()) {
            int startSectionY = ((int) Math.floor(yBottom) >> 4) << 4;
            for (int sy = startSectionY; sy <= yTop; sy += 16) {
                if (sy < yBottom) continue;
                int ringCol = (sy == 0) ? ColorUtil.PURE_WHITE : ColorUtil.withAlpha(ColorUtil.MINT_GREEN, 160);
                Render3DUtil.drawLine3D(event.getMatrices(), event.getCamera(), new Vec3d(cx, sy, cz), new Vec3d(cx + 16, sy, cz), ringCol, ringCol, 1.6f, false);
                Render3DUtil.drawLine3D(event.getMatrices(), event.getCamera(), new Vec3d(cx + 16, sy, cz), new Vec3d(cx + 16, sy, cz + 16), ringCol, ringCol, 1.6f, false);
                Render3DUtil.drawLine3D(event.getMatrices(), event.getCamera(), new Vec3d(cx + 16, sy, cz + 16), new Vec3d(cx, sy, cz + 16), ringCol, ringCol, 1.6f, false);
                Render3DUtil.drawLine3D(event.getMatrices(), event.getCamera(), new Vec3d(cx, sy, cz + 16), new Vec3d(cx, sy, cz), ringCol, ringCol, 1.6f, false);
            }
        }
    }
}
