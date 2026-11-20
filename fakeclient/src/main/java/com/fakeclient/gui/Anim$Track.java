/*
 * Decompiled with CFR 0.152.
 */
package com.fakeclient.gui;

public class Anim$Track {
    float value;
    float velocity;
    long time;

    Anim$Track(float value) {
        this.value = value;
        this.time = System.nanoTime();
    }
}
