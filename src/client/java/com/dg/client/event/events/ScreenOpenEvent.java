package com.dg.client.event.events;

import com.dg.client.event.CancellableEvent;
import net.minecraft.client.gui.screen.Screen;

public final class ScreenOpenEvent extends CancellableEvent {
    private Screen screen;

    public ScreenOpenEvent(Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() {
        return screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }
}
