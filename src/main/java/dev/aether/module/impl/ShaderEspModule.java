package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class ShaderEspModule extends Module {
    private final BooleanSetting players = this.addSetting(new BooleanSetting("Players", true));
    private final BooleanSetting passives = this.addSetting(new BooleanSetting("Passives", false));
    private final BooleanSetting items = this.addSetting(new BooleanSetting("Items", false));
    private final BooleanSetting crystals = this.addSetting(new BooleanSetting("Crystals", true));
    private final BooleanSetting markTarget = this.addSetting(new BooleanSetting("Mark Target", true));
    private final ModeSetting colorMode = this.addSetting(new ModeSetting("Color", "Theme", "Theme", "Manual"));
    private final ColorSetting playerColor = this.addSetting(new ColorSetting("Player Color", -13338378));
    private final ColorSetting friendColor = this.addSetting(new ColorSetting("Friend Color", -7358982));
    private final SliderSetting range = this.addSetting(new SliderSetting("Range", 64.0, 16.0, 256.0, 8.0F));

    public ShaderEspModule() {
        super("glow-esp", "Outlines entities through walls with a glow.", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        double rangeSq = this.range.get() * this.range.get();
        for (Entity entity : mc.level.getAllEntities()) {
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            double dist = entity.distanceToSqr(player);
            if (dist > rangeSq) {
                continue;
            }
            if (entity instanceof Player && this.players.get()) {
                continue;
            }
            if (entity instanceof ItemFrame && this.items.get()) {
                continue;
            }
            if ((entity instanceof AbstractArrow || entity instanceof Fireball || entity instanceof ShulkerBullet
                    || entity instanceof SmallFireball || entity instanceof Snowball || entity instanceof ThrownTrident
                    || entity instanceof WitherSkull) && this.crystals.get()) {
                continue;
            }
        }
    }

    public int getPlayerColor() {
        return this.playerColor.argb();
    }

    public int getFriendColor() {
        return this.friendColor.argb();
    }
}
