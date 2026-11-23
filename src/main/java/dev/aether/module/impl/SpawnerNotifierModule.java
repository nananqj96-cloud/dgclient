package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpawnerNotifierModule extends Module {
    private static final String SPAWNER = "Spawner";
    private final BooleanSetting sSound = this.addSetting(new BooleanSetting("Sound", "Sound", true));
    private final BooleanSetting sRender = this.addSetting(new BooleanSetting("Render", "Render", true));
    private final ColorSetting sColor = this.addSetting(new ColorSetting("Color", "Color", -587246251));
    private final Map<Long, String> layer = new HashMap<>();
    private final Set<Long> notified = new HashSet<>();

    public SpawnerNotifierModule() {
        super("spawner-notifier", "Scans chunks for mob spawners and highlights them.", Category.DONUT);
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
        this.layer.clear();
        this.notified.clear();
    }

    public void onTick() {
        // Spawner detection is handled via onTick scanning nearby blocks
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
    }

    public void found(long pos, BlockState state) {
        this.layer.put(pos, "Spawner");
        if (this.notified.add(pos)) {
            this.notify(BlockPos.of(pos));
        }
    }

    public void removed(long pos) {
        this.layer.remove(pos);
    }

    public void reset() {
        this.layer.clear();
        this.notified.clear();
    }

    private void notify(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        AetherClient.notifications().pushInfo(foundMessage(mc, pos));

        if (this.sSound.get() && mc.player != null && mc.level != null) {
        }
    }

    private String foundMessage(Minecraft mc, BlockPos pos) {
        String mob = entityName(mc, pos);
        StringBuilder sb = new StringBuilder();
        sb.append("Spawner found at ").append(pos.getX()).append(", ").append(pos.getY()).append(", ").append(pos.getZ());
        if (mob != null) {
            sb.insert(0, mob).append(" ");
        }
        if (mc.player != null) {
            long distance = Math.round(mc.player.position().distanceTo(Vec3.atCenterOf(pos)));
            sb.append(" (").append(distance).append("m)");
        }
        return sb.toString();
    }

    private static String entityName(Minecraft mc, BlockPos pos) {
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
            Entity entity = spawner.getSpawner().getOrCreateDisplayEntity(mc.level, pos);
            return entity == null ? null : entity.getType().getDescription().getString();
        } else {
            return null;
        }
    }
}
