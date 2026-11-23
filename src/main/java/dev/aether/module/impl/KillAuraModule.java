package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class KillAuraModule extends Module {
    private final BooleanSetting players = this.addSetting(new BooleanSetting("Players", true));
    private final BooleanSetting hostiles = this.addSetting(new BooleanSetting("Hostiles", false));
    private final ModeSetting priority = this.addSetting(new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle"));
    private final ModeSetting crits = this.addSetting(new ModeSetting("Crits", "Wait", "Off", "Wait", "Jump"));

    public KillAuraModule() {
        super("kill-aura", "Attacks nearby targets without moving your camera.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Kill aura logic stub - core target selection/attack functionality
        // preserved as module registration; detailed combat logic disabled
    }

    public boolean getPlayers() {
        return this.players.get();
    }

    public boolean getHostiles() {
        return this.hostiles.get();
    }

    public String getPriority() {
        return this.priority.get();
    }

    public String getCrits() {
        return this.crits.get();
    }
}
