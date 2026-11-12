package com.dg.client.setting;

import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * ARGB + HSV color setting with alpha channel and optional animated neon pulse.
 * Constrained to the green/white hue band (95..165 deg) or neutral grayscale/white when saturation is low,
 * while exposing full Saturation, Value, Hue-band, and Alpha controls in the ClickGUI.
 */
public final class ColorSetting extends Setting<Integer> {
    private float hue;
    private float saturation;
    private float brightness;
    private int alpha;
    private boolean pulse;

    public ColorSetting(String name, String description, int defaultArgb) {
        this(name, description, defaultArgb, false);
    }

    public ColorSetting(String name, String description, int defaultArgb, boolean defaultPulse) {
        super(name, description, defaultArgb);
        this.pulse = defaultPulse;
        syncHsvFromArgb(defaultArgb);
    }

    private void syncHsvFromArgb(int argb) {
        float[] hsv = ColorUtil.argbToHsv(argb);
        this.hue = MathUtil.clamp(hsv[0], 95.0f, 165.0f);
        this.saturation = hsv[1];
        this.brightness = hsv[2];
        this.alpha = ColorUtil.getAlpha(argb);
    }

    public void setHsva(float hue, float saturation, float brightness, int alpha) {
        this.hue = MathUtil.clamp(hue, 95.0f, 165.0f);
        this.saturation = MathUtil.clamp(saturation, 0.0f, 1.0f);
        this.brightness = MathUtil.clamp(brightness, 0.0f, 1.0f);
        this.alpha = MathUtil.clamp(alpha, 0, 255);
        super.setValue(ColorUtil.hsvToArgb(this.hue, this.saturation, this.brightness, this.alpha));
    }

    @Override
    public void setValue(Integer newValue) {
        if (newValue != null) {
            syncHsvFromArgb(newValue);
            super.setValue(ColorUtil.hsvToArgb(this.hue, this.saturation, this.brightness, this.alpha));
        }
    }

    public int getColor() {
        int base = getValue();
        if (!pulse) {
            return base;
        }
        float wave = (float) ((Math.sin(System.currentTimeMillis() / 320.0) + 1.0) * 0.5);
        int target = ColorUtil.withAlpha(ColorUtil.PURE_WHITE, alpha);
        return ColorUtil.lerp(base, target, wave * 0.45f);
    }

    public float getHue() {
        return hue;
    }

    public float getSaturation() {
        return saturation;
    }

    public float getBrightness() {
        return brightness;
    }

    public int getAlpha() {
        return alpha;
    }

    public boolean isPulse() {
        return pulse;
    }

    public void setPulse(boolean pulse) {
        this.pulse = pulse;
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("argb", getValue());
        obj.addProperty("hue", hue);
        obj.addProperty("saturation", saturation);
        obj.addProperty("brightness", brightness);
        obj.addProperty("alpha", alpha);
        obj.addProperty("pulse", pulse);
        return obj;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            float h = obj.has("hue") ? obj.get("hue").getAsFloat() : this.hue;
            float s = obj.has("saturation") ? obj.get("saturation").getAsFloat() : this.saturation;
            float b = obj.has("brightness") ? obj.get("brightness").getAsFloat() : this.brightness;
            int a = obj.has("alpha") ? obj.get("alpha").getAsInt() : this.alpha;
            this.pulse = obj.has("pulse") && obj.get("pulse").getAsBoolean();
            setHsva(h, s, b, a);
        } else if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            setValue(element.getAsInt());
        }
    }
}
