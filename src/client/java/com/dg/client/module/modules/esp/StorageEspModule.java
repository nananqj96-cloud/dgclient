package com.dg.client.module.modules.esp;

import com.dg.client.DGClient;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
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
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Storage ESP for loaded storage block entities, backed by {@link StorageBlockTracker}
 * for zero-stutter chunk/block-update tracking, double-chest merging, Y-0 leaked storage
 * integration, and optional inline tracers.
 */
public final class StorageEspModule extends Module {
    public record LeakedStorage(BlockPos pos, String label, float confidence) {
    }

    public enum ColorMode {
        GREEN_THEME,
        TYPE_DISTINCT
    }

    private final SettingGroup containers = createGroup("Containers");
    private final SettingGroup style = createGroup("Style & Tracers");

    public final BooleanSetting chests = containers.add(new BooleanSetting("Chests", "Highlight chests", true));
    public final BooleanSetting trappedChests = containers.add(new BooleanSetting("Trapped Chests", "Highlight trapped chests", true));
    public final BooleanSetting shulkers = containers.add(new BooleanSetting("Shulkers", "Highlight shulker boxes", true));
    public final BooleanSetting enderChests = containers.add(new BooleanSetting("Ender Chests", "Highlight ender chests", true));
    public final BooleanSetting barrels = containers.add(new BooleanSetting("Barrels", "Highlight barrels", true));
    public final BooleanSetting hoppers = containers.add(new BooleanSetting("Hoppers", "Highlight hoppers", false));
    public final BooleanSetting furnaces = containers.add(new BooleanSetting("Furnaces", "Highlight furnaces, smokers & blast furnaces", false));
    public final BooleanSetting dispensers = containers.add(new BooleanSetting("Dispensers / Droppers", "Highlight dispensers & droppers", false));
    public final BooleanSetting brewingStands = containers.add(new BooleanSetting("Brewing Stands", "Highlight brewing stands", false));
    public final BooleanSetting shelves = containers.add(new BooleanSetting("Shelves / Chiseled", "Highlight 1.21.11 shelves & chiseled bookshelves", false));
    public final BooleanSetting pots = containers.add(new BooleanSetting("Decorated Pots", "Highlight decorated pots", false));

    public final EnumSetting<EspRenderHelper.BoxStyle> boxStyle = style.add(
            new EnumSetting<>("Box Style", "Container ESP visual style", EspRenderHelper.BoxStyle.OUTLINE_AND_FILL));
    public final EnumSetting<ColorMode> colorMode = style.add(
            new EnumSetting<>("Color Mode", "DGClient green theme or distinct per-container colors", ColorMode.TYPE_DISTINCT));
    public final BooleanSetting mergeDoubleChests = style.add(
            new BooleanSetting("Merge Double Chests", "Merges adjacent double-chest halves into a single clean box", true));
    public final DoubleSetting lineWidth = style.add(new DoubleSetting("Line Width", "Outline width", 1.5, 0.8, 3.5, 0.1));
    public final IntSetting fillAlpha = style.add(new IntSetting("Fill Alpha", "Box fill opacity", 36, 0, 140, 4));
    public final IntSetting maxRange = style.add(new IntSetting("Max Range (m)", "Maximum render distance in blocks", 128, 16, 384, 8));
    public final IntSetting maxRendered = style.add(new IntSetting("Max Containers", "Cap on rendered containers for high FPS", 256, 32, 768, 32));
    public final BooleanSetting throughWalls = style.add(new BooleanSetting("Through Walls", "Renders container boxes through terrain", true));
    public final BooleanSetting showY0Leaks = style.add(new BooleanSetting(
            "Show Y-0 Leaked", "Renders underground storage leaked via Y0Bypass", true));
    public final BooleanSetting drawTracers = style.add(new BooleanSetting(
            "Inline Tracers", "Also draw tracer lines (or use standalone StorageTracers module for full options)", false));

    private Supplier<List<LeakedStorage>> leakedStorageSupplier = List::of;
    private int lastCount = 0;

    public StorageEspModule() {
        super("StorageESP", "Highlights chests, shulkers, barrels, e-chests, hoppers, furnaces & storage blocks", ModuleCategory.ESP);
    }

    public void setLeakedStorageSupplier(Supplier<List<LeakedStorage>> supplier) {
        this.leakedStorageSupplier = supplier != null ? supplier : List::of;
    }

    public Supplier<List<LeakedStorage>> getLeakedStorageSupplier() {
        return leakedStorageSupplier;
    }

    @Override
    public String getDisplayInfo() {
        return Integer.toString(lastCount);
    }

    @Override
    protected void onDisable() {
        lastCount = 0;
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

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) {
            lastCount = 0;
            return;
        }
        double maxDist = maxRange.getValue();
        List<StorageBlockTracker.ResolvedStorage> targets = DGClient.getInstance()
                .getStorageBlockTracker()
                .collectVisible(maxDist, maxRendered.getValue(), mergeDoubleChests.getValue(), this::acceptsKind);

        boolean xray = throughWalls.getValue();
        StorageTracersModule standaloneTracers = DGClient.getInstance().getModuleManager().get(StorageTracersModule.class);
        boolean emitInlineTracers = drawTracers.getValue() && (standaloneTracers == null || !standaloneTracers.isEnabled());

        Vec3d tracerOrigin = emitInlineTracers ? Render3DUtil.computeTracerOrigin(
                event.getCamera(),
                DGClient.getInstance().getLastViewMatrix(),
                DGClient.getInstance().getLastProjectionMatrix(),
                0.0f, 0.0f) : null;
        List<Render3DUtil.LineSegment> tracerBatch = emitInlineTracers ? new ArrayList<>(targets.size()) : null;

        int rendered = 0;
        for (StorageBlockTracker.ResolvedStorage target : targets) {
            int col = colorMode.getValue() == ColorMode.TYPE_DISTINCT
                    ? target.kind().getDistinctColor()
                    : target.kind().getThemeColor();

            if (tracerBatch != null) {
                float t = (float) MathHelper.clamp(Math.sqrt(target.distanceSq()) / maxDist, 0.0, 1.0);
                int a = MathHelper.clamp(Math.round(210 * (1.0f - 0.55f * t)), 36, 230);
                tracerBatch.add(new Render3DUtil.LineSegment(
                        tracerOrigin,
                        target.center(),
                        ColorUtil.withAlpha(col, Math.round(a * 0.72f)),
                        ColorUtil.withAlpha(col, a),
                        lineWidth.getFloatValue()
                ));
            }

            if (event.getFrustum() != null && !event.getFrustum().isVisible(target.renderBox())) {
                continue;
            }

            EspRenderHelper.renderBox(
                    event.getMatrices(),
                    event.getCamera(),
                    target.renderBox(),
                    boxStyle.getValue(),
                    col,
                    fillAlpha.getValue(),
                    lineWidth.getFloatValue(),
                    0.5f,
                    xray
            );
            rendered++;
        }

        if (showY0Leaks.getValue() && leakedStorageSupplier != null) {
            double maxDistSq = maxDist * maxDist;
            for (LeakedStorage leak : leakedStorageSupplier.get()) {
                double dSq = mc.player.squaredDistanceTo(
                        leak.pos().getX() + 0.5, leak.pos().getY() + 0.5, leak.pos().getZ() + 0.5);
                if (dSq > maxDistSq) {
                    continue;
                }
                Box leakBox = new Box(leak.pos()).contract(0.05);
                if (event.getFrustum() != null && !event.getFrustum().isVisible(leakBox)) {
                    continue;
                }
                int leakAlpha = MathHelper.clamp(Math.round(220 * leak.confidence()), 60, 235);
                int leakCol = ColorUtil.withAlpha(ColorUtil.NEON_GREEN, leakAlpha);
                EspRenderHelper.renderBox(
                        event.getMatrices(),
                        event.getCamera(),
                        leakBox,
                        EspRenderHelper.BoxStyle.CORNER_BRACKETS,
                        leakCol,
                        0,
                        lineWidth.getFloatValue(),
                        0.5f,
                        true
                );
                Render3DUtil.drawBillboardLabel(
                        event.getMatrices(),
                        event.getCamera(),
                        new Vec3d(leak.pos().getX() + 0.5, leak.pos().getY() + 1.25, leak.pos().getZ() + 0.5),
                        String.format(Locale.ROOT, "%s [%.0f%%]", leak.label(), leak.confidence() * 100.0f),
                        ColorUtil.PURE_WHITE,
                        ColorUtil.NEON_GREEN,
                        0.80f,
                        true
                );
                rendered++;
            }
        }

        if (tracerBatch != null && !tracerBatch.isEmpty()) {
            Render3DUtil.drawLinesBatch(event.getMatrices(), event.getCamera(), tracerBatch, xray);
        }
        lastCount = rendered;
    }
}
