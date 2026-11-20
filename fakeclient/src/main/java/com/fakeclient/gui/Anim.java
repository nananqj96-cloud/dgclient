/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.Anim$Anim$Track
 */
package com.fakeclient.gui;

import com.fakeclient.Settings;
import com.fakeclient.gui.Anim;
import java.util.HashMap;
import java.util.Map;

public final class Anim {
    private static final Map<String, Anim$Track> STATE = new HashMap<String, Anim$Track>();
    private static final float MAX_DT = 0.1f;

    private Anim() {
    }

    public static float to(String id, float target, float speed) {
        Anim$Track t = Anim.state(id, target);
        if (t == null) {
            return target;
        }
        float dt = Anim.advance(t);
        if (dt < 0.0f) {
            return t.value;
        }
        float k = speed * Anim.speedScale();
        float a = 1.0f - (float)Math.exp(-k * dt);
        float cur = t.value + (target - t.value) * a;
        if (Math.abs(target - cur) < 8.0E-4f) {
            cur = target;
        }
        t.value = cur;
        return cur;
    }

    public static float bool(String id, boolean on, float speed) {
        return Anim.to(id, on ? 1.0f : 0.0f, speed);
    }

    public static float spring(String id, float target) {
        return Anim.spring(id, target, 190.0f, 17.0f);
    }

    public static float spring(String id, float target, float stiffness, float damping) {
        Anim$Track t = Anim.state(id, target);
        if (t == null) {
            return target;
        }
        float dt = Anim.advance(t);
        if (dt < 0.0f) {
            return t.value;
        }
        float scaled = dt * Anim.speedScale();
        int steps = Math.max(1, Math.min(8, (int)Math.ceil(scaled / 0.008f)));
        float h = scaled / (float)steps;
        for (int i = 0; i < steps; ++i) {
            float force = (target - t.value) * stiffness - t.velocity * damping;
            t.velocity += force * h;
            t.value += t.velocity * h;
        }
        if (Math.abs(target - t.value) < 5.0E-4f && Math.abs(t.velocity) < 0.005f) {
            t.value = target;
            t.velocity = 0.0f;
        }
        return t.value;
    }

    public static float smooth(float t) {
        if (t <= 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }
        return t * t * (3.0f - 2.0f * t);
    }

    public static float easeOutExpo(float t) {
        if (t <= 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }
        return 1.0f - (float)Math.pow(2.0, -10.0 * (double)t);
    }

    public static float easeOutBack(float t) {
        if (t <= 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float p = t - 1.0f;
        return 1.0f + c3 * p * p * p + c1 * p * p;
    }

    public static float easeInOutCubic(float t) {
        if (t <= 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float)Math.pow(-2.0 * (double)t + 2.0, 3.0) / 2.0f;
    }

    public static float since(long startMs, float durationMs) {
        if (durationMs <= 0.0f || !Settings.get().animations) {
            return 1.0f;
        }
        float t = (float)(System.currentTimeMillis() - startMs) / (durationMs / Math.max(0.1f, Anim.speedScale()));
        return t < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
    }

    private static Anim$Track state(String id, float target) {
        Anim$Track t = STATE.get(id);
        if (t == null) {
            STATE.put(id, new Anim$Track(target));
            return null;
        }
        if (!Settings.get().animations) {
            t.value = target;
            t.velocity = 0.0f;
            t.time = System.nanoTime();
            return null;
        }
        return t;
    }

    private static float advance(Anim$Track t) {
        long now = System.nanoTime();
        float dt = (float)(now - t.time) / 1.0E9f;
        t.time = now;
        if (dt <= 0.0f) {
            return -1.0f;
        }
        return Math.min(dt, 0.1f);
    }

    private static float speedScale() {
        return (float)Settings.get().animSpeed / 100.0f;
    }
}
