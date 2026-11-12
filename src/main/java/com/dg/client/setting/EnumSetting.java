package com.dg.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] constants;

    public EnumSetting(String name, String description, E defaultValue) {
        super(name, description, defaultValue);
        this.constants = defaultValue.getDeclaringClass().getEnumConstants();
    }

    public E[] getConstants() {
        return constants;
    }

    public void cycleForward() {
        int next = (getValue().ordinal() + 1) % constants.length;
        setValue(constants[next]);
    }

    public void cycleBackward() {
        int prev = (getValue().ordinal() - 1 + constants.length) % constants.length;
        setValue(constants[prev]);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue().name());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String target = element.getAsString();
            for (E constant : constants) {
                if (constant.name().equalsIgnoreCase(target)) {
                    setValue(constant);
                    return;
                }
            }
        }
    }
}
