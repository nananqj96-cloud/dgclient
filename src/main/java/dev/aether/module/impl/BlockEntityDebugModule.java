package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockEntityDebugModule extends Module {
    private static final int SETTINGS_VERSION = 1;
    private static final int SUMMARY_TICKS = 1200;
    private final ModeSetting sDetection = this.addSetting(new ModeSetting("Detection", "Detection", "All", "All", "Below Zero", "Below -20", "Below -30"));
    private final BooleanSetting sBlockFilter = this.addSetting(new BooleanSetting("Block Filter", "Block Filter", false));
    private final BooleanSetting sChat = this.addSetting(new BooleanSetting("Chat Notifications", "Chat Notifications", false));
    private final BooleanSetting sShowNbt = this.addSetting(new BooleanSetting("Show NBT", "Show NBT", true));
    private final BooleanSetting sHints = this.addSetting(new BooleanSetting("Hidden Block Hints", "Hidden Block Hints", true));
    private final BooleanSetting sLightLeaks = this.addSetting(new BooleanSetting("Light Leaks", "Light Leaks", true));
    private final BooleanSetting sTracers = this.addSetting(new BooleanSetting("Tracers", "Tracers", true));
    private final BooleanSetting sCustomTracerColor = this.addSetting(new BooleanSetting("Custom Tracer Color", "Custom Tracer Color", false));
    private final ColorSetting sTracerColor = this.addSetting(new ColorSetting("Tracer Color", "Tracer Color", -1));
    private final Map<Long, Block> layer = new HashMap<>();
    private final Set<Long> spawners = new HashSet<>();
    private int ticks;
    private String lastSummary = "";
    private int seenGeneration = -1;
    private final Set<String> scannedIds = new HashSet<>();

    public BlockEntityDebugModule() {
        super("block-entity-debug", "Renders every leaked block entity as ESP (always-on Netty capture), plus blocks the server hides but still gives away through chest/crafter events, sounds and light.", Category.EXPLOIT);
        this.sShowNbt.visibleWhen(this.sChat::get);
        this.sLightLeaks.visibleWhen(this.sHints::get);
        this.sCustomTracerColor.visibleWhen(this.sTracers::get);
        this.sTracerColor.visibleWhen(() -> this.sTracers.get() && this.sCustomTracerColor.get());
    }

    @Override
    public int settingsVersion() {
        return SETTINGS_VERSION;
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
        this.layer.clear();
        this.spawners.clear();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        this.ticks++;
    }

    public void onBlockUpdate(BlockPos pos, BlockState state) {
        if (pos != null) {
            this.layer.remove(pos.asLong());
        }
    }

    public int getTracerColor() {
        return this.sTracerColor.get();
    }

    public int getBlockColor() {
        return -1;
    }

    public void found(long pos, BlockState state) {
        this.layer.put(pos, state.getBlock());
        if (state.is(Blocks.SPAWNER)) {
            this.spawners.add(pos);
        }
    }

    public void removed(long pos) {
        this.layer.remove(pos);
        this.spawners.remove(pos);
    }

    public void reset() {
        this.layer.clear();
        this.spawners.clear();
    }
}
