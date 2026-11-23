package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.PiercingWeapon;

public class WTapRecodeModule extends Module {
    private static final String W_TAP = "W Tap";
    private static final String S_TAP = "S Tap";
    private final ModeSetting mode = this.addSetting(new ModeSetting("Mode", "Mode", "W Tap", "W Tap", "S Tap"));
    private final SliderSetting hold = this.addSetting(new SliderSetting("Hold", "Hold", 1.0, 1.0, 3.0, 1.0F));
    private boolean sprintHit;
    private int taps;
    private int tapTick;

    public WTapRecodeModule() {
        super("w-tap-recode", "Resets your sprint right after a sprint hit so the next hit knocks back too.", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        this.taps = 0;
    }

    public void beforeAttack(Player player, Entity target) {
        LocalPlayer self = Minecraft.getInstance().player;
        this.sprintHit = player == self
            && self.isSprinting()
            && MeleeAttacks.charge(self) > 0.9F
            && target instanceof LivingEntity
            && self.input.keyPresses.forward();
    }

    public void afterAttack() {
        if (this.sprintHit) {
            this.start(Minecraft.getInstance().player);
        }
    }

    public void afterStab() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player.isSprinting() && player.input.keyPresses.forward()) {
            if (MeleeAttacks.stabTarget(player, entity -> PiercingWeapon.canHitEntity(player, entity)) != null) {
                this.start(player);
            }
        }
    }

//     @EventHandler(priority = EventPriority.HIGH)
    public void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            this.taps = 0;
        } else {
            if (this.taps > 0 && player.tickCount != this.tapTick) {
                this.apply();
            }
        }
    }

    private void start(LocalPlayer player) {
        this.taps = this.hold.getInt();
        this.tapTick = player.tickCount;
        this.apply();
    }

    private void apply() {
        InputControl.releaseForward(this.mode.is("S Tap"));
        this.taps--;
    }
}
