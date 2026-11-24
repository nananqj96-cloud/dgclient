package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SpearLungeModule extends Module {
    private final KeybindSetting key = this.addSetting(new KeybindSetting("Lunge Key", "Lunge Key", -1));
    private final BooleanSetting reachPoke = this.addSetting(new BooleanSetting("Reach Poke", "Reach Poke", false));
    private final BooleanSetting switchBack = this.addSetting(new BooleanSetting("Switch Back", "Switch Back", true));

    public SpearLungeModule() {
        super("spear-swap", "Swaps to your Lunge spear and lunges. Reach Poke stabs with a spear past sword reach.", Category.COMBAT);
    }

    @Override
    public boolean onKeyPress(int keyCode) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return false;
        }
        // Spear lunge logic stub - core key binding and swap functionality
        // preserved as module registration; detailed logic disabled
        return false;
    }

    public boolean getReachPoke() {
        return this.reachPoke.get();
    }

    public boolean getSwitchBack() {
        return this.switchBack.get();
    }
}
