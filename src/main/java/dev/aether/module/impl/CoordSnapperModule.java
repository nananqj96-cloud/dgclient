package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CoordSnapperModule extends Module {
    private final KeybindSetting key = this.addSetting(new KeybindSetting("Key", "Key", -1));
    private final ModeSetting target = this.addSetting(new ModeSetting("Target", "Target", "Looked At", "Looked At", "Own Position"));
    private final SliderSetting range = this.addSetting(new SliderSetting("Range", "Range", 256.0, 16.0, 512.0, 1.0F));
    private final BooleanSetting stopAtFluids = this.addSetting(new BooleanSetting("Stop at Fluids", "Stop at Fluids", false));
    private final ModeSetting format = this.addSetting(new ModeSetting("Format", "Format", "X Y Z", "X Y Z", "X, Y, Z", "/tp Command"));
    private final BooleanSetting includeDimension = this.addSetting(new BooleanSetting("Include Dimension", "Include Dimension", true));
    private final BooleanSetting confirm = this.addSetting(new BooleanSetting("Chat Confirmation", "Chat Confirmation", true));
    private final BooleanSetting webhook = this.addSetting(new BooleanSetting("Webhook", "Webhook", false));

    public CoordSnapperModule() {
        super("coord-snapper", "Copies the coordinates you look at or stand on with one key.", Category.MISC);
    }

    @Override
    public boolean onKeyPress(int keyCode) {
        if (this.key.matches(keyCode)) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null) {
                return false;
            }
            BlockPos pos;
            if (this.target.is("Own Position")) {
                pos = player.blockPosition();
            } else {
                BlockHitResult hit = (BlockHitResult) player.pick(this.range.get(), 1.0F, false);
                if (hit == null || hit.getType() != BlockHitResult.Type.BLOCK) {
                    return false;
                }
                pos = hit.getBlockPos();
            }
            String coords = formatCoords(pos, player);
            if (this.confirm.get()) {
                AetherClient.notifications().pushInfo(coords);
            }
            return true;
        }
        return false;
    }

    private String formatCoords(BlockPos pos, LocalPlayer player) {
        String x = String.format("%.0f", (double) pos.getX());
        String y = String.format("%.0f", (double) pos.getY());
        String z = String.format("%.0f", (double) pos.getZ());
        if (this.format.is("X, Y, Z")) {
            return x + ", " + y + ", " + z;
        } else if (this.format.is("/tp Command")) {
            return "/tp @p " + x + " " + y + " " + z;
        } else {
            return x + " " + y + " " + z;
        }
    }
}
