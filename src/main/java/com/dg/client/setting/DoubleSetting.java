package com.dg.client.setting;

import com.dg.client.util.MathUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class DoubleSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public DoubleSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, MathUtil.clamp(defaultValue, min, max));
        this.min = min;
        this.max = max;
        this.step = step <= 0.0 ? 0.01 : step;
        setValue(defaultValue);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public float getNormalized() {
        if (max <= min) {
            return 0.0f;
        }
        return (float) ((getValue() - min) / (max - min));
    }

    public void setFromNormalized(float normalized) {
        double raw = min + MathUtil.clamp(normalized, 0.0f, 1.0f) * (max - min);
        setValue(MathUtil.roundToStep(raw, step));
    }

    @Override
    protected Double validate(Double incoming) {
        if (incoming == null || Double.isNaN(incoming)) {
            return getDefaultValue();
        }
        if (max == 0.0 && min == 0.0) {
            return incoming;
        }
        return MathUtil.roundToStep(MathUtil.clamp(incoming, min, max), step > 0.0 ? step : 0.01);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            setValue(element.getAsDouble());
        }
    }
}
