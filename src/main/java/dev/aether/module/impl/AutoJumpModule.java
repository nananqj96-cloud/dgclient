package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class AutoJumpModule extends Module {
    private final ModeSetting mode = this.addSetting(new ModeSetting("Mode", "Mode", "Jump", "Jump", "LowHop"));
    private final ModeSetting jumpIf = this.addSetting(new ModeSetting("Jump If", "Jump If", "Always", "Sprinting", "Walking", "Always"));
    private final SliderSetting velocityHeight = this.addSetting(new SliderSetting("Velocity Height", "Velocity Height", 0.25, 0.0, 2.0, 0.05F));

    public AutoJumpModule() {
        super("auto-jump", "Automatically jumps.", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
    }

    public void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            if (player.onGround() && !player.isShiftKeyDown() && this.shouldJump(player)) {
                if (this.mode.is("Jump")) {
                    player.jumpFromGround();
                } else {
                    Vec3 velocity = player.getDeltaMovement();
                    player.setDeltaMovement(velocity.x, this.velocityHeight.get(), velocity.z);
                }
            }
        }
    }

    private boolean shouldJump(LocalPlayer player) {
        boolean moving = player.zza != 0.0F || player.xxa != 0.0F;

        return switch (this.jumpIf.get()) {
            case "Sprinting" -> player.isSprinting() && moving;
            case "Walking" -> moving;
            default -> true;
        };
    }
}
