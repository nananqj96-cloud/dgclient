package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SneakModule extends Module {
    public static volatile SneakModule active;
    private final ModeSetting mode = this.addSetting(new ModeSetting("Mode", "Vanilla", "Packet", "Vanilla"));

    public SneakModule() {
        super("sneak", "Sneaks for you.", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
    }

    public boolean doPacket() {
        return this.isEnabled() && this.mode.is("Packet") && !isFlying();
    }

    public boolean doVanilla() {
        return this.isEnabled() && this.mode.is("Vanilla") && !isFlying();
    }

    private static boolean isFlying() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null || player.getAbilities().flying;
    }
}
