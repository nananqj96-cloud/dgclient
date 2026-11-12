package com.dg.client.module.modules.esp;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.util.ColorUtil;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Highlights Mob Spawners and Trial Spawners with spawned-mob labels and optional
 * 16-block activation radius boundary.
 */
public final class SpawnerEspModule extends Module {
    public final EnumSetting<EspRenderHelper.RenderMode> mode = addSetting(new EnumSetting<>(
            "Mode", "Spawner box style", EspRenderHelper.RenderMode.SHADER_GLOW));
    public final DoubleSetting range = addSetting(new DoubleSetting(
            "Range", "Maximum spawner render distance", 192.0, 24.0, 384.0, 8.0));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting(
            "Through Walls", "Renders spawners through terrain", true));
    public final BooleanSetting showMobLabel = addSetting(new BooleanSetting(
            "Mob Type Label", "Displays the spawner's configured mob type and distance", true));
    public final BooleanSetting activationRange = addSetting(new BooleanSetting(
            "16m Activation Box", "Draws subtle 16-block player activation boundary", false));
    public final ColorSetting color = addSetting(new ColorSetting(
            "Spawner Color", "Spawner highlight color", ColorUtil.MINT_GREEN));

    private final List<EspRenderHelper.ProjectedRect> rects2d = new ArrayList<>();

    public SpawnerEspModule() {
        super("SpawnerESP", "Highlights dungeon Mob Spawners and Trial Spawners with mob-type labels", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        rects2d.clear();
        if (mc.world == null || mc.player == null) {
            return;
        }
        double maxDist = range.getValue();
        double maxSq = maxDist * maxDist;

        for (WorldChunk chunk : EspRenderHelper.getLoadedChunksInRange(maxDist)) {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (!(be instanceof MobSpawnerBlockEntity) && !(be instanceof TrialSpawnerBlockEntity)) {
                    continue;
                }
                BlockPos pos = be.getPos();
                double dSq = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
                if (dSq > maxSq) {
                    continue;
                }
                Box box = new Box(pos);
                if (event.getFrustum() != null && !event.getFrustum().isVisible(box.expand(0.5))) {
                    continue;
                }

                EspRenderHelper.renderTargetBox(event, box, mode.getValue(), color.getColor(), 45, 2.0f,
                        throughWalls.getValue(), rects2d, -1.0f);

                if (activationRange.getValue() && dSq < 48 * 48) {
                    Box actBox = new Box(pos).expand(16.0, 1.0, 16.0);
                    Render3DUtil.drawBoxOutline(event.getMatrices(), event.getCamera(), actBox, 1.1f,
                            ColorUtil.withAlpha(ColorUtil.PURE_WHITE, 75), false);
                }

                if (showMobLabel.getValue()) {
                    String typeName = "Spawner";
                    if (be instanceof MobSpawnerBlockEntity msb) {
                        Entity rendered = msb.getLogic().getRenderedEntity(mc.world, pos);
                        if (rendered != null) {
                            typeName = rendered.getName().getString() + " Spawner";
                        }
                    } else {
                        typeName = "Trial Spawner";
                    }
                    String tag = String.format(Locale.ROOT, "%s %.0fm", typeName, Math.sqrt(dSq));
                    Render3DUtil.drawBillboardLabel(event.getMatrices(), event.getCamera(),
                            Vec3d.ofCenter(pos).add(0.0, 0.85, 0.0), tag, ColorUtil.PURE_WHITE, color.getColor(), 0.92f, throughWalls.getValue());
                }
            }
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!rects2d.isEmpty() && !mc.options.hudHidden) {
            EspRenderHelper.draw2DProjectedRects(event.getContext(), rects2d);
        }
    }
}
