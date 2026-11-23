package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class TracersModule extends Module {
    private final BooleanSetting players = this.addSetting(new BooleanSetting("Players", "Players", true));
    private final BooleanSetting animals = this.addSetting(new BooleanSetting("Animals", "Animals", false));
    private final BooleanSetting waterAnimals = this.addSetting(new BooleanSetting("Water Animals", "Water Animals", false));
    private final BooleanSetting monsters = this.addSetting(new BooleanSetting("Monsters", "Monsters", false));
    private final BooleanSetting ambient = this.addSetting(new BooleanSetting("Ambient", "Ambient", false));
    private final BooleanSetting misc = this.addSetting(new BooleanSetting("Misc", "Misc", false));
    private final BooleanSetting ignoreSelf = this.addSetting(new BooleanSetting("Ignore Self", "Ignore Self", false));
    private final BooleanSetting ignoreFriends = this.addSetting(new BooleanSetting("Ignore Friends", "Ignore Friends", false));
    private final ColorSetting color = this.addSetting(new ColorSetting("Color", "Color", -13338378));
    private final SliderSetting maxDistance = this.addSetting(new SliderSetting("Max Distance", "Max Distance", 256.0, 32.0, 512.0, 8.0F));

    public TracersModule() {
        super("tracers", "Displays tracer lines to specified entities.", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        double maxSq = this.maxDistance.get() * this.maxDistance.get();
        for (Entity entity : mc.level.getAllEntities()) {
            if (entity == null || entity.isRemoved() || entity == player) {
                continue;
            }
            double dist = entity.distanceToSqr(camera);
            if (dist > maxSq) {
                continue;
            }
            if (entity instanceof Player && !this.players.get()) {
                continue;
            }
            if (entity instanceof Animal && !this.animals.get()) {
                continue;
            }
            if (entity instanceof Monster && !this.monsters.get()) {
                continue;
            }
        }
    }

    public int getColor() {
        return this.color.argb();
    }
}
