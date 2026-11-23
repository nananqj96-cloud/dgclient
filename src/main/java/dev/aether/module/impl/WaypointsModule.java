package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import java.util.ArrayList;
import java.util.List;

public class WaypointsModule extends Module {
    private final KeybindSetting addKey = this.addSetting(new KeybindSetting("Add Here Key", "Add Here Key", -1));
    private final BooleanSetting deaths = this.addSetting(new BooleanSetting("Death Waypoints", "Death Waypoints", true));
    private final SliderSetting deathMemory = this.addSetting(new SliderSetting("Death Memory", "Death Memory", 3.0, 1.0, 10.0, 1.0F));
    private final BooleanSetting otherDimension = this.addSetting(new BooleanSetting("Show In Other Dimension", "Show In Other Dimension", true));
    private final BooleanSetting showDistance = this.addSetting(new BooleanSetting("Show Distance", "Show Distance", true));
    private final SliderSetting maxDistance = this.addSetting(new SliderSetting("Max Distance", "Max Distance", 0.0, 0.0, 30000.0, 100.0F));
    private final SliderSetting hideWithin = this.addSetting(new SliderSetting("Hide Within", "Hide Within", 3.0, 0.0, 32.0, 1.0F));
    private final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Scale", 1.0, 0.5, 2.0, 0.1F));
    private final ColorSetting color = this.addSetting(new ColorSetting("Color", "Color", -14006));
    private final ColorSetting deathColor = this.addSetting(new ColorSetting("Death Color", "Death Color", -47814));
    private final List<Waypoint> waypoints = new ArrayList<>();

    public WaypointsModule() {
        super("waypoints", "Saved places shown on screen with distance. Entries are \"Name x y z\", optionally ending in nether or end.", Category.RENDER);
    }

    @Override
    public boolean onKeyPress(int keyCode) {
        if (this.addKey.matches(keyCode)) {
            this.addHere();
            return true;
        }
        return false;
    }

    private void addHere() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && mc.level != null) {
            this.waypoints.add(new Waypoint(player.getBlockX(), player.getBlockY(), player.getBlockZ(), this.color.argb()));
        }
    }

    @Override
    public void onTick() {
    }

    public List<Waypoint> getWaypoints() {
        return this.waypoints;
    }

    public int getColor() {
        return this.color.argb();
    }

    public int getDeathColor() {
        return this.deathColor.argb();
    }

    public record Waypoint(int x, int y, int z, int color) {
    }
}
