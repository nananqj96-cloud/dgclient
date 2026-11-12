package com.dg.client.module.modules.esp;

import com.dg.client.DGClient;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Highlights loaded storage BlockEntities (Chests with double-chest merging, Trapped Chests,
 * Barrels, Shulker Boxes, Ender Chests, Hoppers, Furnaces/Smokers/Blast Furnaces,
 * Dispensers/Droppers, Brewing Stands, 1.21.11 Shelves/Chiseled Bookshelves, Decorated Pots)
 * via event-driven {@link StorageBlockTracker}, plus Y-0 Bypass leaked underground storage
 * and optional batched inline 3D tracers.
 */
public final class StorageEspModule extends Module {
    public record LeakedStorage(BlockPos pos, String label, float confidence) {
    }

    public enum ColorMode {
        TYPE_DISTINCT,
        GREEN_THEME
    }

    private final SettingGroup filterGroup = createGroup("Containers");

    public final EnumSetting<EspRenderHelper.RenderMode> mode = addSetting(new EnumSetting<>(
            "Mode", "Storage box render style", EspRenderHelper.RenderMode.BOX_BOTH));
    public final EnumSetting<ColorMode> colorMode = addSetting(new EnumSetting<>(
            "Color Mode", "Distinct container colors or DGClient green/white palette", ColorMode.TYPE_DISTINCT));
    public final DoubleSetting range = addSetting(new DoubleSetting(
            "Range", "Maximum container distance in blocks", 144.0, 16.0, 384.0, 8.0));
    public final DoubleSetting lineWidth = addSetting(new DoubleSetting(
            "Line Width", "Outline & tracer stroke width", 1.5, 0.8, 3.5, 0.1));
    public final IntSetting fillAlpha = addSetting(new IntSetting(
            "Fill Alpha", "Box fill opacity", 36, 0, 140, 4));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting(
            "Through Walls", "Renders containers through terrain", true));
    public final BooleanSetting mergeDoubleChests = addSetting(new BooleanSetting(
            "Merge Double Chests", "Merges adjacent double-chest halves into one clean box", true));
    public final BooleanSetting showLabels = addSetting(new BooleanSetting(
            "Labels", "Displays container type & distance", false));
    public final BooleanSetting includeYZeroRecovered = addSetting(new BooleanSetting(
            "Include Y-0 Recovered", "Displays underground storage recovered by Y-0 Bypass", true));
    public final BooleanSetting drawTracers = addSetting(new BooleanSetting(
            "Inline Tracers", "Also draw 3D tracer lines (or use standalone StorageTracers module)", false));
    public final IntSetting maxTargets = addSetting(new IntSetting(
            "Max Containers", "Performance cap on rendered containers", 256, 16, 768, 16));

    public final BooleanSetting chests = filterGroup.add(new BooleanSetting("Chests", "Standard chests", true));
    public final BooleanSetting trappedChests = filterGroup.add(new BooleanSetting("Trapped Chests", "Trapped chests", true));
    public final BooleanSetting barrels = filterGroup.add(new BooleanSetting("Barrels", "Barrels", true));
    public final BooleanSetting shulkers = filterGroup.add(new BooleanSetting("Shulker Boxes", "All shulker boxes", true));
    public final BooleanSetting enderChests = filterGroup.add(new BooleanSetting("Ender Chests", "Ender chests", true));
    public final BooleanSetting hoppers = filterGroup.add(new BooleanSetting("Hoppers", "Hoppers", false));
    public final BooleanSetting furnaces = filterGroup.add(new BooleanSetting("Furnaces", "Furnaces, Blast Furnaces, Smokers", false));
    public final BooleanSetting dispensers = filterGroup.add(new BooleanSetting("Dispensers & Droppers", "Dispensers and droppers", false));
    public final BooleanSetting brewingStands = filterGroup.add(new BooleanSetting("Brewing Stands", "Brewing stands", false));
    public final BooleanSetting shelves = filterGroup.add(new BooleanSetting("Shelves / Chiseled", "1.21.11 Shelves & Chiseled Bookshelves", false));
    public final BooleanSetting pots = filterGroup.add(new BooleanSetting("Decorated Pots", "Decorated pots", false));

    public final ColorSetting primaryColor = addSetting(new ColorSetting("Chest/Barrel Color", "Standard storage color (Green Theme)", ColorUtil.NEON_GREEN));
    public final ColorSetting highValueColor = addSetting(new ColorSetting("Shulker/EChest Color", "High-value storage color (Green Theme)", ColorUtil.PURE_WHITE));

    private final List<EspRenderHelper.ProjectedRect> rects2d = new ArrayList<>();
    private Supplier<List<LeakedStorage>> leakedStorageSupplier = List::of;
    private int lastCount = 0;

    public StorageEspModule() {
        super("StorageESP", "Highlights chests, shulkers, barrels, e-chests, hoppers, furnaces & storage blocks", ModuleCategory.RENDER);
    }

    @Override
    public String getDisplayInfo() {
        return Integer.toString(lastCount);
    }

    @Override
    protected void onDisable() {
        rects2d.clear();
        lastCount = 0;
    }

    public void setLeakedStorageSupplier(Supplier<List<LeakedStorage>> supplier) {
        this.leakedStorageSupplier = supplier != null ? supplier : List::of;
    }

    public List<BlockPos> collectTrackedPositions(double maxDistance) {
        List<StorageBlockTracker.ResolvedStorage> list = DGClient.getInstance()
                .getStorageBlockTracker()
                .collectVisible(maxDistance, maxTargets.getValue(), mergeDoubleChests.getValue(), this::acceptsKind);
        List<BlockPos> out = new ArrayList<>(list.size());
        for (StorageBlockTracker.ResolvedStorage rs : list) {
            out.add(rs.pos());
        }
        return out;
    }

    public boolean acceptsKind(StorageBlockTracker.StorageKind kind) {
        return switch (kind) {
            case CHEST -> chests.getValue();
            case TRAPPED_CHEST -> trappedChests.getValue();
            case BARREL -> barrels.getValue();
            case SHULKER_BOX -> shulkers.getValue();
            case ENDER_CHEST -> enderChests.getValue();
            case HOPPER -> hoppers.getValue();
            case FURNACE -> furnaces.getValue();
            case DISPENSER -> dispensers.getValue();
            case BREWING_STAND -> brewingStands.getValue();
            case SHELF -> shelves.getValue();
            case DECORATED_POT -> pots.getValue();
        };
    }

    private int colorForKind(StorageBlockTracker.StorageKind kind) {
        if (colorMode.getValue() == ColorMode.TYPE_DISTINCT) {
            return kind.getDistinctColor();
        }
        if (kind == StorageBlockTracker.StorageKind.SHULKER_BOX || kind == StorageBlockTracker.StorageKind.ENDER_CHEST) {
            return highValueColor.getColor();
        }
        if (kind == StorageBlockTracker.StorageKind.TRAPPED_CHEST) {
            return ColorUtil.MINT_GREEN;
        }
        return primaryColor.getColor();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        rects2d.clear();
        if (mc.world == null || mc.player == null) {
            lastCount = 0;
            return;
        }
        double maxDist = range.getValue();
        double maxSq = maxDist * maxDist;
        float lw = lineWidth.getValue().floatValue();
        int fAlpha = fillAlpha.getValue();
        boolean xray = throughWalls.getValue();

        List<StorageBlockTracker.ResolvedStorage> targets = DGClient.getInstance()
                .getStorageBlockTracker()
                .collectVisible(maxDist, maxTargets.getValue(), mergeDoubleChests.getValue(), this::acceptsKind);

        StorageTracersModule standaloneTracers = DGClient.getInstance().getModuleManager().get(StorageTracersModule.class);
        boolean emitInlineTracers = drawTracers.getValue() && (standaloneTracers == null || !standaloneTracers.isEnabled());
        Vec3d tracerOrigin = emitInlineTracers ? Render3DUtil.computeTracerOrigin(
                event.getCamera(),
                DGClient.getInstance().getLastViewMatrix(),
                DGClient.getInstance().getLastProjectionMatrix(),
                0.0f, 0.0f) : null;
        List<Render3DUtil.LineSegment> tracerBatch = emitInlineTracers ? new ArrayList<>(targets.size()) : null;

        Set<BlockPos> seen = new HashSet<>();
        int rendered = 0;

        for (StorageBlockTracker.ResolvedStorage t : targets) {
            seen.add(t.pos());
            int col = colorForKind(t.kind());

            if (tracerBatch != null) {
                float ratio = (float) MathHelper.clamp(Math.sqrt(t.distanceSq()) / maxDist, 0.0, 1.0);
                int a = MathHelper.clamp(Math.round(210 * (1.0f - 0.55f * ratio)), 36, 230);
                tracerBatch.add(new Render3DUtil.LineSegment(
                        tracerOrigin,
                        t.center(),
                        ColorUtil.withAlpha(col, Math.round(a * 0.72f)),
                        ColorUtil.withAlpha(col, a),
                        lw
                ));
            }

            Box box = t.renderBox().contract(0.02);
            if (event.getFrustum() != null && !event.getFrustum().isVisible(box.expand(0.2))) {
                continue;
            }
            EspRenderHelper.renderTargetBox(event, box, mode.getValue(), col, fAlpha, lw, xray, rects2d, -1.0f);

            if (showLabels.getValue()) {
                String txt = String.format(Locale.ROOT, "%s %.0fm", t.kind().getDisplayName(), Math.sqrt(t.distanceSq()));
                Render3DUtil.drawBillboardLabel(
                        event.getMatrices(), event.getCamera(),
                        t.center().add(0.0, 0.72, 0.0),
                        txt, ColorUtil.PURE_WHITE, col, 0.85f, xray
                );
            }
            rendered++;
        }

        if (includeYZeroRecovered.getValue()) {
            for (LeakedStorage ls : leakedStorageSupplier.get()) {
                if (rendered >= maxTargets.getValue()) {
                    break;
                }
                if (seen.contains(ls.pos())) {
                    continue;
                }
                double dSq = mc.player.squaredDistanceTo(Vec3d.ofCenter(ls.pos()));
                if (dSq > maxSq) {
                    continue;
                }
                Box box = new Box(ls.pos()).contract(0.02);
                if (event.getFrustum() != null && !event.getFrustum().isVisible(box.expand(0.2))) {
                    continue;
                }
                int col = ColorUtil.scaleAlpha(ColorUtil.MINT_GREEN, Math.max(0.45f, ls.confidence()));
                EspRenderHelper.renderTargetBox(event, box, mode.getValue(), col, fAlpha, lw, xray, rects2d, -1.0f);
                if (showLabels.getValue()) {
                    String txt = String.format(Locale.ROOT, "%s [Y<0] %.0fm", ls.label(), Math.sqrt(dSq));
                    Render3DUtil.drawBillboardLabel(
                            event.getMatrices(), event.getCamera(),
                            Vec3d.ofCenter(ls.pos()).add(0.0, 0.72, 0.0),
                            txt, ColorUtil.PURE_WHITE, col, 0.85f, xray
                    );
                }
                rendered++;
            }
        }

        if (tracerBatch != null && !tracerBatch.isEmpty()) {
            Render3DUtil.drawLinesBatch(event.getMatrices(), event.getCamera(), tracerBatch, xray);
        }
        lastCount = rendered;
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!rects2d.isEmpty() && !mc.options.hudHidden) {
            EspRenderHelper.draw2DProjectedRects(event.getContext(), rects2d);
        }
    }
}
