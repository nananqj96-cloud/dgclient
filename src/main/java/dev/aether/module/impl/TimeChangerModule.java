package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class TimeChangerModule extends Module {
    public static volatile TimeChangerModule active;
    private final SliderSetting time = this.addSetting(new NumberSetting("Time", 0.0F, -20000.0F, 20000.0F));
    private ClientLevel trackedLevel;
    private long serverDayTime;
    private long appliedDayTime;

    public TimeChangerModule() {
        super("time-changer", "Makes you able to set a custom time.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        active = this;
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            this.applyForcedTime(level);
        }
    }

    @Override
    protected void onDisable() {
        active = null;
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level == this.trackedLevel) {
            writeDayTime(level, this.currentServerDayTime(level));
        }

        this.trackedLevel = null;
    }

    private void onPostTick(TickEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            this.trackedLevel = null;
        } else {
            this.applyForcedTime(level);
        }
    }

    private void applyForcedTime(ClientLevel level) {
        this.serverDayTime = level == this.trackedLevel ? this.currentServerDayTime(level) : level.getDayTime();
        this.trackedLevel = level;
        this.appliedDayTime = this.forcedDayTime();
        writeDayTime(level, this.appliedDayTime);
    }

    private static void writeDayTime(ClientLevel level, long dayTime) {
        if (level.getDayTime() != dayTime) {
            level.getLevelData().setDayTime(dayTime);
            level.environmentAttributes().invalidateTickCache();
        }
    }

    private long currentServerDayTime(ClientLevel level) {
        return this.serverDayTime + (level.getDayTime() - this.appliedDayTime);
    }

    public long interceptServerDayTime(ClientLevel level, long dayTime) {
        this.trackedLevel = level;
        this.serverDayTime = dayTime;
        this.appliedDayTime = this.forcedDayTime();
        return this.appliedDayTime;
    }

    private long forcedDayTime() {
        return (long)this.time.get();
    }
}
