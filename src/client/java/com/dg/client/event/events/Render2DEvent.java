package com.dg.client.event.events;

import net.minecraft.client.gui.DrawContext;

public final class Render2DEvent {
    private final DrawContext context;
    private final int screenWidth;
    private final int screenHeight;
    private final float tickDelta;

    public Render2DEvent(DrawContext context, int screenWidth, int screenHeight, float tickDelta) {
        this.context = context;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.tickDelta = tickDelta;
    }

    public DrawContext getContext() {
        return context;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}
