package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public class WeaponInspectModule extends Module {
    private final KeybindSetting key = this.addSetting(new KeybindSetting("Inspect Key", "Inspect Key", 70));
    private final ModeSetting skin = this.addSetting(new ModeSetting("Knife Skin", "Knife Skin", "Butterfly", "None", "Butterfly", "Karambit", "Katana"));
    private final ModeSetting style = this.addSetting(new ModeSetting("Style", "Style", "Classic", "Classic", "Twirl"));
    private final ModeSetting items = this.addSetting(new ModeSetting("Items", "Items", "Swords", "Swords", "Weapons", "Any Item"));
    private final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Speed", 1.0, 0.5, 2.0, 0.05F));
    private final SliderSetting knifeSize = this.addSetting(new SliderSetting("Knife Size", "Knife Size", 1.0, 0.5, 2.0, 0.05F));
    private final BooleanSetting showArm = this.addSetting(new BooleanSetting("Show Arm", "Show Arm", true));
    private final BooleanSetting deploy = this.addSetting(new BooleanSetting("Deploy Animation", "Deploy Animation", true));
    private final BooleanSetting slashTrail = this.addSetting(new BooleanSetting("Slash Trail", "Slash Trail", true));
    private final SliderSetting trailLength = this.addSetting(new SliderSetting("Trail Length", "Trail Length", 0.14, 0.05, 0.35, 0.01F));

    public WeaponInspectModule() {
        super("weapon-inspect", "Knife skins for your sword and CS-style inspects with a key of your choice (F by default); hold the key to keep the knife spinning.", Category.VISUALS);
    }

    @Override
    public boolean onKeyPress(int keyCode) {
        if (this.key.matches(keyCode)) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player != null) {
                ItemStack held = player.getMainHandItem();
                if (!held.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onTick() {
    }
}
