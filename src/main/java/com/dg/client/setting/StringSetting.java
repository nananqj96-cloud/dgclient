package com.dg.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class StringSetting extends Setting<String> {
    private final int maxLength;

    public StringSetting(String name, String description, String defaultValue, int maxLength) {
        super(name, description, defaultValue == null ? "" : defaultValue);
        this.maxLength = Math.max(1, maxLength);
    }

    public StringSetting(String name, String description, String defaultValue) {
        this(name, description, defaultValue, 128);
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    protected String validate(String incoming) {
        if (incoming == null) {
            return "";
        }
        return incoming.length() > maxLength ? incoming.substring(0, maxLength) : incoming;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            setValue(element.getAsString());
        }
    }
}
