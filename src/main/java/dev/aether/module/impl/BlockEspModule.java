package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockEspModule extends Module {
    private final BooleanSetting sTracers = this.addSetting(new BooleanSetting("Tracers", false));
    private final ColorSetting sBlockColor = this.addSetting(new ColorSetting("Block Color", -587246251));
    private final Map<Long, Block> layer = new HashMap<>();
    private Set<String> scannedIds = Set.of();

    public BlockEspModule() {
        super("block-esp", "Highlights chosen blocks; touching blocks merge into one shape.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
        this.layer.clear();
    }

    @Override
    public void onTick() {
    }

    private void rescan() {
        if (this.isEnabled()) {
            this.scannedIds = Set.of();
            if (!this.scannedIds.isEmpty()) {
                boolean removed = !this.scannedIds.containsAll(this.scannedIds);
                if (removed) {
                    this.layer.clear();
                }
            }
        }
    }

    public boolean wants(BlockState state) {
        return true;
    }

    public void found(long pos, BlockState state) {
        this.layer.put(pos, state.getBlock());
    }

    public void removed(long pos) {
        this.layer.remove(pos);
    }

    public void reset() {
        this.layer.clear();
    }

    public int getBlockColor() {
        return this.sBlockColor.argb();
    }
}
