package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import java.util.ArrayList;
import java.util.List;

public class TrajectoriesModule extends Module {
    private final BooleanSetting aimPreview = this.addSetting(new BooleanSetting("Aim Preview", "Aim Preview", true));
    private final BooleanSetting ghostArc = this.addSetting(new BooleanSetting("Ghost Arc", "Ghost Arc", true));
    private final BooleanSetting inFlight = this.addSetting(new BooleanSetting("In Flight", "In Flight", true));
    private final BooleanSetting othersShots = this.addSetting(new BooleanSetting("Others' Shots", "Others' Shots", true));
    private final SliderSetting range = this.addSetting(new SliderSetting("Range", "Range", 64.0, 16.0, 160.0, 8.0F));
    private final BooleanSetting bows = this.addSetting(new BooleanSetting("Bows & Crossbows", "Bows & Crossbows", true));
    private final BooleanSetting tridents = this.addSetting(new BooleanSetting("Tridents", "Tridents", true));
    private final BooleanSetting throwables = this.addSetting(new BooleanSetting("Throwables", "Throwables", true));
    private final BooleanSetting pearls = this.addSetting(new BooleanSetting("Ender Pearls", "Ender Pearls", true));
    private final ModeSetting pathStyle = this.addSetting(new ModeSetting("Path", "Path", "Beads", "Beads", "Dashes", "Solid"));
    private final List<Projectile> tracked = new ArrayList<>();

    public TrajectoriesModule() {
        super("trajectories", "Shows where your projectiles will land.", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Track nearby projectiles
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null || entity.isRemoved() || !(entity instanceof Projectile shot)) {
                continue;
            }
            if (!this.inFlight.get()) {
                continue;
            }
            boolean own = shot.getOwner() == player;
            if (!own && !this.othersShots.get()) {
                continue;
            }
            double dist = shot.distanceToSqr(player);
            double rangeSq = this.range.get() * this.range.get();
            if (dist <= rangeSq && !this.tracked.contains(shot)) {
                this.tracked.add(shot);
            }
        }
        // Remove dead projectiles
        this.tracked.removeIf(Projectile::isRemoved);
    }

    @Override
    protected void onDisable() {
        this.tracked.clear();
    }
}
