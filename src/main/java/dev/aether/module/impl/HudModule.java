package dev.aether.module.impl;

import java.util.List;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.Setting;

public class HudModule extends Module {
    private final BooleanSetting coordinates = this.addSetting(new BooleanSetting("Overworld Coords", "Overworld Coords", true));
    private final BooleanSetting netherCoordinates = this.addSetting(new BooleanSetting("Nether Coords", "Nether Coords", true));
    private final BooleanSetting ping = this.addSetting(new BooleanSetting("Ping", "Ping", true));
    private final BooleanSetting fps = this.addSetting(new BooleanSetting("FPS", "FPS", true));
    private final BooleanSetting clock = this.addSetting(new BooleanSetting("Clock", "Clock", true));
    private final BooleanSetting logo = this.addSetting(new BooleanSetting("Logo", "Logo", true));
    private final BooleanSetting moduleList = this.addSetting(new BooleanSetting("Module List", "Module List", true));
    private final BooleanSetting potions = this.addSetting(new BooleanSetting("Potions", "Potions", true));
    private final BooleanSetting targetCard = this.addSetting(new BooleanSetting("Target HUD", "Target HUD", true));
    private final BooleanSetting targetPlayersOnly = this.addSetting(new BooleanSetting("Players Only", "Players Only", false));
    private final BooleanSetting targetOnLook = this.addSetting(new BooleanSetting("Show On Look", "Show On Look", false));
    private final BooleanSetting targetGear = this.addSetting(new BooleanSetting("Show Gear", "Show Gear", true));
    private final BooleanSetting armorStatus = this.addSetting(new BooleanSetting("Armor HUD", "Armor HUD", true));
    private final ModeSetting armorLayout = this.addSetting(new ModeSetting("Armor Layout", "Armor Layout", "Row", "Row", "Column"));
    private final ModeSetting armorWear = this.addSetting(new ModeSetting("Wear As", "Wear As", "Gauge", "Gauge", "Percent", "Gauge + Percent"));
    private final BooleanSetting armorHands = this.addSetting(new BooleanSetting("Include Hands", "Include Hands", false));
    private final BooleanSetting itemCooldowns = this.addSetting(new BooleanSetting("Cooldown", "Cooldown", true));
    private final ModeSetting cooldownDial = this.addSetting(new ModeSetting("Cooldown Dial", "Cooldown Dial", "Halo", "Halo", "Sandbar"));
    private final ModeSetting toastStyle = this.addSetting(new ModeSetting("Toast Style", "Toast Style", "Bar", "Bar", "Card", "Chip"));
    private final BooleanSetting shadow = this.addSetting(new BooleanSetting("Shadow", "Shadow", true));
    private final BooleanSetting customAccent = this.addSetting(new BooleanSetting("Custom Accent", "Custom Accent", false));
    private final ColorSetting accentColor = this.addSetting(new ColorSetting("Accent Color", "Accent Color", -13338378));

    public HudModule() {
        super("hud", "Module list, coordinates and status overlays.", Category.CLIENT);
        this.accentColor.visibleWhen(this.customAccent::get);
        this.targetPlayersOnly.visibleWhen(this.targetCard::get);
        this.targetOnLook.visibleWhen(this.targetCard::get);
        this.targetGear.visibleWhen(this.targetCard::get);
        this.armorLayout.visibleWhen(this.armorStatus::get);
        this.armorWear.visibleWhen(this.armorStatus::get);
        this.armorHands.visibleWhen(this.armorStatus::get);
        this.cooldownDial.visibleWhen(this.itemCooldowns::get);
    }

    public boolean coordinates() {
        return this.coordinates.get();
    }

    public boolean netherCoordinates() {
        return this.netherCoordinates.get();
    }

    public boolean ping() {
        return this.ping.get();
    }

    public boolean fps() {
        return this.fps.get();
    }

    public boolean clock() {
        return this.clock.get();
    }

    public boolean logo() {
        return this.logo.get();
    }

    public boolean moduleList() {
        return this.moduleList.get();
    }

    public boolean potions() {
        return this.potions.get();
    }

    public boolean targetCard() {
        return this.targetCard.get();
    }

    public boolean targetPlayersOnly() {
        return this.targetPlayersOnly.get();
    }

    public boolean targetOnLook() {
        return this.targetOnLook.get();
    }

    public boolean targetGear() {
        return this.targetGear.get();
    }

    public boolean armorStatus() {
        return this.armorStatus.get();
    }

    public boolean armorColumn() {
        return this.armorLayout.is("Column");
    }

    public boolean armorHands() {
        return this.armorHands.get();
    }

    public boolean itemCooldowns() {
        return this.itemCooldowns.get();
    }

    public boolean cooldownSandbar() {
        return this.cooldownDial.is("Sandbar");
    }

    public boolean shadow() {
        return this.shadow.get();
    }

    public boolean customAccent() {
        return this.customAccent.get();
    }

    public int accentArgb() {
//         return ThemeTint.accent(this.accentColor.argb());
    }

    public HudModule.ArmorWear armorWear() {
        return switch (this.armorWear.get()) {
            case "Percent" -> HudModule.ArmorWear.PERCENT;
            case "Gauge + Percent" -> HudModule.ArmorWear.BOTH;
            default -> HudModule.ArmorWear.GAUGE;
        };
    }

    public HudModule.ToastStyle toastStyle() {
        return switch (this.toastStyle.get()) {
            case "Card" -> HudModule.ToastStyle.CARD;
            case "Chip" -> HudModule.ToastStyle.CHIP;
            default -> HudModule.ToastStyle.BAR;
        };
    }

    public List<Setting> elementSettings(String elementId) {
        return switch (elementId) {
            case "info_bar" -> List.of(this.clock, this.fps, this.ping);
            case "coordinates" -> List.of(this.coordinates, this.netherCoordinates);
            case "module_list" -> List.of(this.moduleList);
            case "potion_tracker" -> List.of(this.potions);
            case "target_card" -> List.of(this.targetCard, this.targetPlayersOnly, this.targetOnLook, this.targetGear);
            case "armor_status" -> List.of(this.armorStatus, this.armorLayout, this.armorWear, this.armorHands);
            case "item_cooldowns" -> List.of(this.itemCooldowns, this.cooldownDial);
            case "toggle_toasts" -> List.of(this.toastStyle);
            default -> List.of();
        };
    }

        public enum ArmorWear {
        GAUGE,
        PERCENT,
        BOTH;
    }

        public enum ToastStyle {
        BAR,
        CARD,
        CHIP;
    }
}
