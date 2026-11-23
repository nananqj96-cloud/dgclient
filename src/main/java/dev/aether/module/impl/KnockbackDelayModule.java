package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

public class KnockbackDelayModule extends Module {
    private final SliderSetting chance = this.addSetting(new SliderSetting("Chance (%)", 40.0, 0.0, 100.0, 5.0F));
    private final SliderSetting airMin = this.addSetting(new SliderSetting("Air Delay Min (ms)", 50.0, 0.0, 500.0, 10.0F));
    private final SliderSetting airMax = this.addSetting(new SliderSetting("Air Delay Max (ms)", 100.0, 0.0, 500.0, 10.0F));
    private final SliderSetting groundMin = this.addSetting(new SliderSetting("Ground Delay Min (ms)", 200.0, 0.0, 1000.0, 10.0F));
    private final SliderSetting groundMax = this.addSetting(new SliderSetting("Ground Delay Max (ms)", 250.0, 0.0, 1000.0, 10.0F));
    private final BooleanSetting waterCheck = this.addSetting(new BooleanSetting("Not In Water", false));
    private final BooleanSetting onlyFights = this.addSetting(new BooleanSetting("Only Near Enemies", true));

    public KnockbackDelayModule() {
        super("knockback-delay", "Takes some hits' knockback a random moment late: a little in the air, longer on the ground.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
    }

    public float getDelay(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return 0.0F;
        }
        boolean inAir = !player.onGround();
        float min = inAir ? this.airMin.getFloat() : this.groundMin.getFloat();
        float max = inAir ? this.airMax.getFloat() : this.groundMax.getFloat();
        return min + (float) Math.random() * (max - min);
    }

    public boolean shouldDelay() {
        return Math.random() * 100.0 < this.chance.get();
    }
}
