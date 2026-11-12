package com.dg.client.core;

import com.dg.client.util.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe toast notification queue rendered in the top-right corner of the HUD.
 */
public final class NotificationManager {
    public enum Type {
        INFO,
        MODULE_ON,
        MODULE_OFF,
        DETECTION,
        WARNING
    }

    public static final class Toast {
        private final Type type;
        private final String title;
        private final String message;
        private final long createdAt;
        private final long durationMs;
        private float slideProgress = 0.0f;

        Toast(Type type, String title, String message, long durationMs) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.createdAt = System.currentTimeMillis();
            this.durationMs = durationMs;
        }

        public Type getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public float getLifeFraction() {
            long age = System.currentTimeMillis() - createdAt;
            return MathUtil.clamp(1.0f - (float) age / (float) durationMs, 0.0f, 1.0f);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > durationMs + 280L;
        }

        public float getSlideProgress() {
            return slideProgress;
        }

        public void updateAnimation(float speed) {
            boolean closing = (System.currentTimeMillis() - createdAt) >= durationMs;
            float target = closing ? 0.0f : 1.0f;
            slideProgress = MathUtil.approach(slideProgress, target, speed);
        }
    }

    private final CopyOnWriteArrayList<Toast> toasts = new CopyOnWriteArrayList<>();

    public void push(Type type, String title, String message) {
        long duration = switch (type) {
            case DETECTION, WARNING -> 5000L;
            case INFO -> 3400L;
            case MODULE_ON, MODULE_OFF -> 2100L;
        };
        push(type, title, message, duration);
    }

    public void push(Type type, String title, String message, long durationMs) {
        if (toasts.size() >= 8) {
            toasts.remove(0);
        }
        toasts.add(new Toast(type, title, message, durationMs));
    }

    public List<Toast> getActiveToasts() {
        toasts.removeIf(Toast::isExpired);
        return new ArrayList<>(toasts);
    }
}
