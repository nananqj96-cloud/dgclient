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
import com.dg.client.setting.IntSetting;
import com.dg.client.setting.SettingGroup;
import com.dg.client.util.ColorUtil;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Highlights loaded storage BlockEntities (Chests, Trapped Chests, Barrels, Shulker Boxes,
 * Ender Chests, Hoppers, Furnaces, Dispensers/Droppers) plus any underground storage leaked
 * via the Y-0 Bypass store.
 */
public final class StorageEspModule extends Module {
    public record LeakedStorage(BlockPos pos, String label, float confidence) {
    }

    private record StorageTarget(BlockPos pos, String label, int color, double distSq) {
    }

    private final SettingGroup filterGroup = createGroup("Containers");

    public final EnumSetting<EspRenderHelper.RenderMode> mode = addSetting(new EnumSetting<>(
            "Mode", "Storage box render style", EspRenderHelper.RenderMode.BOX_BOTH));
    public final DoubleSetting range = addSetting(new DoubleSetting(
            "Range", "Maximum container distance in blocks", 144.0, 16.0, 384.0, 8.0));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting(
            "Through Walls", "Renders containers through terrain", true));
    public final BooleanSetting showLabels = addSetting(new BooleanSetting(
            "Labels", "Displays container type & distance", false));
    public final BooleanSetting includeYZeroRecovered = addSetting(new BooleanSetting(
            "Include Y-0 Recovered", "Displays underground storage recovered by Y-0 Bypass", true));
    public final IntSetting maxTargets = addSetting(new IntSetting(
            "Max Containers", "Performance cap on rendered containers", 160, 16, 512, 16));

    public final BooleanSetting chests = filterGroup.add(new BooleanSetting("Chests", "Standard chests", true));
    public final BooleanSetting trappedChests = filterGroup.add(new BooleanSetting("Trapped Chests", "Trapped chests", true));
    public final BooleanSetting barrels = filterGroup.add(new BooleanSetting("Barrels", "Barrels", true));
    public final BooleanSetting shulkers = filterGroup.add(new BooleanSetting("Shulker Boxes", "All shulker boxes", true));
    public final BooleanSetting enderChests = filterGroup.add(new BooleanSetting("Ender Chests", "Ender chests", true));
    public final BooleanSetting hoppers = filterGroup.add(new BooleanSetting("Hoppers", "Hoppers", true));
    public final BooleanSetting furnaces = filterGroup.add(new BooleanSetting("Furnaces", "Furnaces, Blast Furnaces, Smokers", false));
    public final BooleanSetting dispensers = filterGroup.add(new BooleanSetting("Dispensers & Droppers", "Dispensers and droppers", false));

    public final ColorSetting primaryColor = addSetting(new ColorSetting("Chest/Barrel Color", "Standard storage color", ColorUtil.NEON_GREEN));
    public final ColorSetting highValueColor = addSetting(new ColorSetting("Shulker/EChest Color", "High-value storage color", ColorUtil.PURE_WHITE));

    private final List<EspRenderHelper.ProjectedRect> rects2d = new ArrayList<>();
    private Supplier<List<LeakedStorage>> leakedStorageSupplier = List::of;

    public StorageEspModule() {
        super("StorageESP", "Highlights chests, shulkers, barrels, e-chests, hoppers, furnaces & dispensers", ModuleCategory.RENDER);
    }

    public void setLeakedStorageSupplier(Supplier<List<LeakedStorage>> supplier) {
        this.leakedStorageSupplier = supplier != null ? supplier : List::of;
    }

    public List<BlockPos> collectTrackedPositions(double maxDistance) {
        List<BlockPos> out = new ArrayList<>();
        if (mc.player == null || mc.world == null) {
            return out;
        }
        double maxSq = maxDistance * maxDistance;
        for (WorldChunk chunk : EspRenderHelper.getLoadedChunksInRange(maxDistance)) {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (classify(be) != null && mc.player.squaredDistanceTo(Vec3d.ofCenter(be.getPos())) <= maxSq) {
                    out.add(be.getPos());
                }
            }
        }
        return out;
    }

    private String classify(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return trappedChests.getValue() ? "Trapped Chest" : null;
        if (be instanceof ChestBlockEntity) return chests.getValue() ? "Chest" : null;
        if (be instanceof BarrelBlockEntity) return barrels.getValue() ? "Barrel" : null;
        if (be instanceof ShulkerBoxBlockEntity) return shulkers.getValue() ? "Shulker" : null;
        if (be instanceof EnderChestBlockEntity) return enderChests.getValue() ? "Ender Chest" : null;
        if (be instanceof HopperBlockEntity) return hoppers.getValue() ? "Hopper" : null;
        if (be instanceof AbstractFurnaceBlockEntity) return furnaces.getValue() ? "Furnace" : null;
        if (be instanceof DropperBlockEntity) return dispensers.getValue() ? "Dropper" : null;
        if (be instanceof DispenserBlockEntity) return dispensers.getValue() ? "Dispenser" : null;
        return null;
    }

    private int colorFor(BlockEntity be) {
        if (be instanceof ShulkerBoxBlockEntity || be instanceof EnderChestBlockEntity) {
            return highValueColor.getColor();
        }
        if (be instanceof TrappedChestBlockEntity) {
            return ColorUtil.MINT_GREEN;
        }
        return primaryColor.getColor();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        rects2d.clear();
        if (mc.world == null || mc.player == null) {
            return;
        }
        double maxDist = range.getValue();
        double maxSq = maxDist * maxDist;
        List<StorageTarget> targets = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();

        for (WorldChunk chunk : EspRenderHelper.getLoadedChunksInRange(maxDist)) {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                String label = classify(be);
                if (label == null) continue;
                BlockPos pos = be.getPos();
                double dSq = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
                if (dSq > maxSq) continue;
                seen.add(pos);
                targets.add(new StorageTarget(pos, label, colorFor(be), dSq));
            }
        }

        if (includeYZeroRecovered.getValue()) {
            for (LeakedStorage ls : leakedStorageSupplier.get()) {
                if (seen.contains(ls.pos())) continue;
                double dSq = mc.player.squaredDistanceTo(Vec3d.ofCenter(ls.pos()));
                if (dSq <= maxSq) {
                    int col = ColorUtil.scaleAlpha(ColorUtil.MINT_GREEN, Math.max(0.45f, ls.confidence()));
                    targets.add(new StorageTarget(ls.pos(), ls.label() + " [Y<0]", col, dSq));
                }
            }
        }

        targets.sort(Comparator.comparingDouble(StorageTarget::distSq));
        int rendered = 0;
        for (StorageTarget t : targets) {
            if (rendered >= maxTargets.getValue()) {
                break;
            }
            Box box = new Box(t.pos()).contract(0.02);
            if (event.getFrustum() != null && !event.getFrustum().isVisible(box.expand(0.2))) {
                continue;
            }
            EspRenderHelper.renderTargetBox(event, box, mode.getValue(), t.color(), 36, 1.6f,
                    throughWalls.getValue(), rects2d, -1.0f);

            if (showLabels.getValue()) {
                String txt = String.format(Locale.ROOT, "%s %.0fm", t.label(), Math.sqrt(t.distSq()));
                Render3DUtil.drawBillboardLabel(event.getMatrices(), event.getCamera(),
                        Vec3d.ofCenter(t.pos()).add(0.0, 0.72, 0.0), txt, ColorUtil.PURE_WHITE, t.color(), 0.85f, throughWalls.getValue());
            }
            rendered++;
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!rects2d.isEmpty() && !mc.options.hudHidden) {
            EspRenderHelper.draw2DProjectedRects(event.getContext(), rects2d);
        }
    }
}
