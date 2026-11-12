package com.dg.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class KeybindSetting extends Setting<Integer> {
    public static final int NONE = -1;

    private boolean holdMode;

    public KeybindSetting(String name, String description, int defaultKey) {
        this(name, description, defaultKey, false);
    }

    public KeybindSetting(String name, String description, int defaultKey, boolean holdMode) {
        super(name, description, defaultKey);
        this.holdMode = holdMode;
    }

    public boolean isBound() {
        return getValue() != null && getValue() > 0;
    }

    public boolean isHoldMode() {
        return holdMode;
    }

    public void setHoldMode(boolean holdMode) {
        this.holdMode = holdMode;
    }

    public void unbind() {
        setValue(NONE);
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", getValue());
        obj.addProperty("hold", holdMode);
        return obj;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("key")) {
                setValue(obj.get("key").getAsInt());
            }
            if (obj.has("hold")) {
                this.holdMode = obj.get("hold").getAsBoolean();
            }
        } else if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            setValue(element.getAsInt());
        }
    }
}
