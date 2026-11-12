package com.dg.client.hud;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;

/**
 * Draggable, toggleable HUD widget managed by {@link HudManager} and editable in {@link HudEditorScreen}.
 */
public abstract class HudElement {
    private final String id;
    private final String displayName;
    // Normalized anchor ratios (0..1) so layout adapts cleanly to window resize
    private float normX;
    private float normY;
    private float lastWidth = 80.0f;
    private float lastHeight = 20.0f;
    private boolean enabled = true;

    protected HudElement(String id, String displayName, float defaultNormX, float defaultNormY) {
        this.id = id;
        this.displayName = displayName;
        this.normX = defaultNormX;
        this.normY = defaultNormY;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float getX(int screenWidth) {
        float maxX = Math.max(4.0f, screenWidth - lastWidth - 4.0f);
        return Math.max(4.0f, Math.min(maxX, normX * screenWidth));
    }

    public float getY(int screenHeight) {
        float maxY = Math.max(4.0f, screenHeight - lastHeight - 4.0f);
        return Math.max(4.0f, Math.min(maxY, normY * screenHeight));
    }

    public void setPosition(float pixelX, float pixelY, int screenWidth, int screenHeight) {
        if (screenWidth > 0) {
            this.normX = Math.max(0.0f, Math.min(1.0f, pixelX / (float) screenWidth));
        }
        if (screenHeight > 0) {
            this.normY = Math.max(0.0f, Math.min(1.0f, pixelY / (float) screenHeight));
        }
    }

    protected void setMeasuredSize(float width, float height) {
        this.lastWidth = Math.max(24.0f, width);
        this.lastHeight = Math.max(12.0f, height);
    }

    public float getWidth() {
        return lastWidth;
    }

    public float getHeight() {
        return lastHeight;
    }

    public abstract boolean isActiveByModule();

    public abstract void render(DrawContext context, int screenWidth, int screenHeight, float tickDelta, boolean editorPreview);

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("normX", normX);
        o.addProperty("normY", normY);
        o.addProperty("enabled", enabled);
        return o;
    }

    public void fromJson(JsonObject o) {
        if (o.has("normX")) {
            this.normX = o.get("normX").getAsFloat();
        }
        if (o.has("normY")) {
            this.normY = o.get("normY").getAsFloat();
        }
        if (o.has("enabled")) {
            this.enabled = o.get("enabled").getAsBoolean();
        }
    }
}
