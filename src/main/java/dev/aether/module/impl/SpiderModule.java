package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class SpiderModule extends Module {
    private static final double CLIMB_VELOCITY_THRESHOLD = 0.2;
    private final SliderSetting climbSpeed = this.addSetting(new SliderSetting("Climb Speed", 0.2, 0.0, 10.0, 0.1F));

    public SpiderModule() {
        super("spider", "Allows you to climb walls like a spider.", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
    }

    private void onPostTick(TickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.horizontalCollision) {
            Vec3 velocity = player.getDeltaMovement();
            if (!(velocity.y >= 0.2)) {
                player.setDeltaMovement(velocity.x, this.climbSpeed.get(), velocity.z);
            }
        }
    }
}
