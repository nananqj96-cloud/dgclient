package dev.aether.module.impl;

import dev.aether.AetherClient;

import java.util.List;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Set;

public class XrayModule extends Module {
    private static final boolean SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
    private static final List<String> ORES = List.of(
        "minecraft:coal_ore",
        "minecraft:deepslate_coal_ore",
        "minecraft:iron_ore",
        "minecraft:deepslate_iron_ore",
        "minecraft:gold_ore",
        "minecraft:deepslate_gold_ore",
        "minecraft:lapis_ore",
        "minecraft:deepslate_lapis_ore",
        "minecraft:redstone_ore",
        "minecraft:deepslate_redstone_ore",
        "minecraft:diamond_ore",
        "minecraft:deepslate_diamond_ore",
        "minecraft:emerald_ore",
        "minecraft:deepslate_emerald_ore",
        "minecraft:copper_ore",
        "minecraft:deepslate_copper_ore",
        "minecraft:nether_gold_ore",
        "minecraft:nether_quartz_ore",
        "minecraft:ancient_debris"
    );
    private final SliderSetting opacity = this.addSetting(new SliderSetting("Opacity", 10.0, 0.0, 100.0, 1.0F));
    private final BooleanSetting exposedOnly = this.addSetting(new BooleanSetting("Exposed Only", false));
    public static volatile XrayModule.Snapshot active;
    public static final ThreadLocal<float[]> CURRENT_ALPHA = ThreadLocal.withInitial(() -> new float[]{1.0F});
    private static final int RELOAD_DELAY_TICKS = 6;
    private int reloadDelay = -1;

    public XrayModule() {
        super("xray", "Hide or fade everything but a block whitelist to see ores through terrain.", Category.RENDER);
        this.usesNotifications();
    }

    @Override
    protected void onEnable() {
        if (SODIUM) {
//             AetherClient.chat(this, ModuleChat.body().text("Sodium detected; terrain Xray ").danger("isn't supported").text(" with it yet"));
            this.requestDisable();
        } else {
            this.rebuildSnapshot();
            this.reloadWorld();
        }
    }

    @Override
    protected void onDisable() {
        active = null;
        this.reloadDelay = -1;
        this.reloadWorld();
    }

//     @EventHandler
    public void onTick() {
        if (this.reloadDelay >= 0 && --this.reloadDelay <= 0) {
            this.reloadDelay = -1;
            this.rebuildSnapshot();
            this.reloadWorld();
        }
    }

    private void scheduleReload() {
        if (this.isEnabled()) {
            this.reloadDelay = 6;
        }
    }

    private void reloadWorld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    private void rebuildSnapshot() {
        active = new XrayModule.Snapshot(new java.util.HashSet<>(this.blocks.enabledBlocks()), this.exposedOnly.get(), this.opacity.get() / 100.0F);
    }

        public static final class Snapshot {
        private static final Direction[] DIRECTIONS = Direction.values();
        private static final ThreadLocal<MutableBlockPos> SCRATCH_POS = ThreadLocal.withInitial(MutableBlockPos::new);
        private final Set<Block> whitelist;
        private final boolean exposedOnly;
        private final float opacity;

        private Snapshot(Set<Block> whitelist, boolean exposedOnly, float opacity) {
            this.whitelist = whitelist;
            this.exposedOnly = exposedOnly;
            this.opacity = opacity;
        }

        public boolean isWhitelisted(Block block) {
            return this.whitelist.contains(block);
        }

        public float alphaFor(BlockAndTintGetter region, BlockState state, BlockPos pos) {
            return !this.whitelist.contains(state.getBlock()) || this.exposedOnly && !this.isExposed(region, pos) ? this.opacity : 1.0F;
        }

        private boolean isExposed(BlockAndTintGetter region, BlockPos pos) {
            MutableBlockPos scratch = SCRATCH_POS.get();

            for (Direction direction : DIRECTIONS) {
                scratch.setWithOffset(pos, direction);
                if (!region.getBlockState(scratch).isSolidRender()) {
                    return true;
                }
            }

            return false;
        }
    }
}
