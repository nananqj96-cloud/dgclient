package com.dg.client.setting;

import com.dg.client.util.MathUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class IntSetting extends Setting<Integer> {
    private final int min;
    private final int max;
    private final int step;

    public IntSetting(String name, String description, int defaultValue, int min, int max, int step) {
        super(name, description, MathUtil.clamp(defaultValue, min, max));
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        setValue(defaultValue);
    }

    public IntSetting(String name, String description, int defaultValue, int min, int max) {
        this(name, description, defaultValue, min, max, 1);
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getStep() {
        return step;
    }

    public float getNormalized() {
        if (max <= min) {
            return 0.0f;
        }
        return (float) (getValue() - min) / (float) (max - min);
    }

    public void setFromNormalized(float normalized) {
        double raw = min + MathUtil.clamp(normalized, 0.0f, 1.0f) * (max - min);
        int stepped = (int) Math.round(Math.round(raw / step) * step);
        setValue(stepped);
    }

    @Override
    protected Integer validate(Integer incoming) {
        if (incoming == null) {
            return getDefaultValue();
        }
        if (max == 0 && min == 0) {
            return incoming;
        }
        return MathUtil.clamp(incoming, min, max);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            setValue(element.getAsInt());
        }
    }
}
