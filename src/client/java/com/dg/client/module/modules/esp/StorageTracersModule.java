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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * High-efficiency 3D Storage Tracers module.
 *
 * <p>Independently configurable from StorageESP: draws crisp, bobbing-safe 3D lines from
 * the player's screen anchor to loaded storage containers in a single batched GPU draw call,
 * updating immediately as chunks load/unload or containers are placed/broken.
 */
public final class StorageTracersModule extends Module {
    public enum Anchor {
        CROSSHAIR(0.0f),
        BOTTOM(-0.90f),
        TOP(0.90f);

        private final float ndcY;

        Anchor(float ndcY) {
            this.ndcY = ndcY;
        }

        public float getNdcY() {
            return ndcY;
        }
    }

    public enum ColorMode {
        TYPE_DISTINCT,
        GREEN_THEME,
        DISTANCE_GRADIENT
    }

    private final SettingGroup containers = createGroup("Storage Types");
    private final SettingGroup visibility = createGroup("Range & Visibility");
    private final SettingGroup style = createGroup("Line Style");

    public final BooleanSetting chests = containers.add(new BooleanSetting("Chests", "Trace standard chests", true));
    public final BooleanSetting trappedChests = containers.add(new BooleanSetting("Trapped Chests", "Trace trapped chests", true));
    public final BooleanSetting barrels = containers.add(new BooleanSetting("Barrels", "Trace barrels", true));
    public final BooleanSetting shulkers = containers.add(new BooleanSetting("Shulker Boxes", "Trace shulker boxes", true));
    public final BooleanSetting enderChests = containers.add(new BooleanSetting("Ender Chests", "Trace ender chests", true));
    public final BooleanSetting hoppers = containers.add(new BooleanSetting("Hoppers", "Trace hoppers", false));
    public final BooleanSetting furnaces = containers.add(new BooleanSetting("Furnaces / Smokers", "Trace furnaces, smokers & blast furnaces", false));
    public final BooleanSetting dispensers = containers.add(new BooleanSetting("Dispensers / Droppers", "Trace dispensers & droppers", false));
    public final BooleanSetting brewingStands = containers.add(new BooleanSetting("Brewing Stands", "Trace brewing stands", false));
    public final BooleanSetting shelves = containers.add(new BooleanSetting("Shelves / Chiseled", "Trace 1.21.11 shelves & chiseled bookshelves", false));
    public final BooleanSetting decoratedPots = containers.add(new BooleanSetting("Decorated Pots", "Trace decorated pots", false));

    public final IntSetting maxDistance = visibility.add(new IntSetting(
            "Max Distance (m)", "Maximum distance to draw storage tracer lines", 128, 16, 384, 8));
    public final DoubleSetting minDistance = visibility.add(new DoubleSetting(
            "Min Distance (m)", "Hides lines for containers right next to your feet to reduce clutter", 0.0, 0.0, 24.0, 1.0));
    public final IntSetting maxTracers = visibility.add(new IntSetting(
            "Max Lines", "Maximum simultaneous tracer lines (nearest containers prioritized)", 192, 16, 512, 16));
    public final BooleanSetting throughWalls = visibility.add(new BooleanSetting(
            "Through Walls", "Renders tracer lines through solid terrain", true));
    public final BooleanSetting mergeDoubleChests = visibility.add(new BooleanSetting(
            "Merge Double Chests", "Draws one centered line per double chest instead of two", true));

    public final EnumSetting<Anchor> anchor = style.add(new EnumSetting<>(
            "Screen Anchor", "Screen origin point for tracer lines", Anchor.CROSSHAIR));
    public final EnumSetting<ColorMode> colorMode = style.add(new EnumSetting<>(
            "Color Mode", "Distinct per-container colors, DGClient green theme, or distance gradient", ColorMode.TYPE_DISTINCT));
    public final DoubleSetting lineWidth = style.add(new DoubleSetting(
            "Line Width", "Tracer stroke thickness", 1.5, 0.8, 3.5, 0.1));
    public final BooleanSetting distanceFade = style.add(new BooleanSetting(
            "Distance Fade", "Smoothly fades alpha near maximum tracer distance", true));
    public final IntSetting baseAlpha = style.add(new IntSetting(
            "Line Opacity", "Maximum line alpha (40..255)", 215, 40, 255, 5));

    private int lastRenderedCount = 0;

    public StorageTracersModule() {
        super("StorageTracers", "Draws batched 3D tracer lines toward loaded storage containers", ModuleCategory.RENDER);
    }

    @Override
    public String getDisplayInfo() {
        return Integer.toString(lastRenderedCount);
    }

    @Override
    protected void onDisable() {
        lastRenderedCount = 0;
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
            case DECORATED_POT -> decoratedPots.getValue();
        };
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) {
            lastRenderedCount = 0;
            return;
        }
        double maxDist = maxDistance.getValue();
        double minDistSq = minDistance.getValue() * minDistance.getValue();

        List<StorageBlockTracker.ResolvedStorage> targets = DGClient.getInstance()
                .getStorageBlockTracker()
                .collectVisible(maxDist, maxTracers.getValue(), mergeDoubleChests.getValue(), this::acceptsKind);

        if (targets.isEmpty()) {
            lastRenderedCount = 0;
            return;
        }

        Vec3d startWorld = Render3DUtil.computeTracerOrigin(
                event.getCamera(),
                DGClient.getInstance().getLastViewMatrix(),
                DGClient.getInstance().getLastProjectionMatrix(),
                0.0f,
                anchor.getValue().getNdcY()
        );

        float width = lineWidth.getValue().floatValue();
        int maxA = baseAlpha.getValue();
        boolean fade = distanceFade.getValue();
        ColorMode mode = colorMode.getValue();

        List<Render3DUtil.LineSegment> batch = new ArrayList<>(targets.size());
        for (StorageBlockTracker.ResolvedStorage target : targets) {
            if (target.distanceSq() < minDistSq) {
                continue;
            }
            double dist = Math.sqrt(target.distanceSq());
            float t = (float) MathHelper.clamp(dist / maxDist, 0.0, 1.0);

            int rgb = switch (mode) {
                case TYPE_DISTINCT -> target.kind().getDistinctColor();
                case GREEN_THEME -> target.kind().getThemeColor();
                case DISTANCE_GRADIENT -> ColorUtil.lerp(ColorUtil.PURE_WHITE, ColorUtil.NEON_GREEN, t);
            };
            // High-value shulkers & ender chests stay extra crisp even at long range
            boolean highValue = target.kind() == StorageBlockTracker.StorageKind.SHULKER_BOX
                    || target.kind() == StorageBlockTracker.StorageKind.ENDER_CHEST;
            float fadeFactor = fade ? (highValue ? (1.0f - 0.35f * t) : (1.0f - 0.62f * t)) : 1.0f;
            int endAlpha = MathHelper.clamp(Math.round(maxA * fadeFactor), 28, 255);
            int startAlpha = MathHelper.clamp(Math.round(endAlpha * 0.72f), 22, 240);

            batch.add(new Render3DUtil.LineSegment(
                    startWorld,
                    target.center(),
                    ColorUtil.withAlpha(rgb, startAlpha),
                    ColorUtil.withAlpha(rgb, endAlpha),
                    highValue ? Math.min(4.0f, width + 0.35f) : width
            ));
        }

        lastRenderedCount = batch.size();
        Render3DUtil.drawLinesBatch(event.getMatrices(), event.getCamera(), batch, throughWalls.getValue());
    }
}
