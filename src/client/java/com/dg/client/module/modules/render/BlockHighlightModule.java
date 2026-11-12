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
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

/**
 * Replaces the vanilla thin black block selection outline with a glowing neon green/white
 * box, translucent fill, and optional targeted-face highlight.
 */
public final class BlockHighlightModule extends Module {
    public final BooleanSetting fill = addSetting(new BooleanSetting(
            "Fill Box", "Renders translucent neon fill over the targeted block", true));
    public final BooleanSetting faceHighlight = addSetting(new BooleanSetting(
            "Face Highlight", "Highlights the specific block face under the crosshair", true));
    public final BooleanSetting glow = addSetting(new BooleanSetting(
            "Outer Glow", "Adds multi-pass neon bloom around the block edges", true));
    public final DoubleSetting lineWidth = addSetting(new DoubleSetting(
            "Line Width", "Outline thickness", 2.0, 1.0, 5.0, 0.25));
    public final ColorSetting outlineColor = addSetting(new ColorSetting(
            "Outline Color", "Block outline color", ColorUtil.NEON_GREEN));
    public final ColorSetting fillColor = addSetting(new ColorSetting(
            "Fill Color", "Translucent fill color", ColorUtil.withAlpha(ColorUtil.NEON_GREEN, 38)));

    public BlockHighlightModule() {
        super("BlockHighlight", "Custom neon outline, translucent fill, and face highlight on targeted blocks", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK || mc.world == null || mc.player == null) {
            return;
        }
        BlockHitResult bhr = (BlockHitResult) mc.crosshairTarget;
        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        VoxelShape shape = state.getOutlineShape(mc.world, pos, ShapeContext.of(mc.player));
        Box box = shape.isEmpty() ? new Box(pos) : shape.getBoundingBox().offset(pos).expand(0.002);

        if (fill.getValue()) {
            Render3DUtil.drawFilledBox(event.getMatrices(), event.getCamera(), box, fillColor.getColor(), false);
        }
        if (glow.getValue()) {
            Render3DUtil.drawGlowingBox(event.getMatrices(), event.getCamera(), box,
                    outlineColor.getColor(), ColorUtil.PURE_WHITE, 0.75f, false);
        } else {
            Render3DUtil.drawBoxOutline(event.getMatrices(), event.getCamera(), box,
                    lineWidth.getValue().floatValue(), outlineColor.getColor(), false);
        }

        if (faceHighlight.getValue()) {
            Box faceBox = sliceFace(box, bhr.getSide());
            Render3DUtil.drawFilledBox(event.getMatrices(), event.getCamera(), faceBox,
                    ColorUtil.withAlpha(ColorUtil.PURE_WHITE, 72), false);
            Render3DUtil.drawBoxOutline(event.getMatrices(), event.getCamera(), faceBox, 2.2f, ColorUtil.PURE_WHITE, false);
        }
    }

    private static Box sliceFace(Box b, Direction side) {
        double eps = 0.004;
        return switch (side) {
            case UP -> new Box(b.minX, b.maxY - eps, b.minZ, b.maxX, b.maxY + eps, b.maxZ);
            case DOWN -> new Box(b.minX, b.minY - eps, b.minZ, b.maxX, b.minY + eps, b.maxZ);
            case NORTH -> new Box(b.minX, b.minY, b.minZ - eps, b.maxX, b.maxY, b.minZ + eps);
            case SOUTH -> new Box(b.minX, b.minY, b.maxZ - eps, b.maxX, b.maxY, b.maxZ + eps);
            case WEST -> new Box(b.minX - eps, b.minY, b.minZ, b.minX + eps, b.maxY, b.maxZ);
            case EAST -> new Box(b.maxX - eps, b.minY, b.minZ, b.maxX + eps, b.maxY, b.maxZ);
        };
    }
}
