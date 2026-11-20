/*
 * Decompiled with CFR 0.152.
 */
package com.fakeclient.license;

public final class Hwid {
    private static String cached = "FAKE-00000000-0000-0000-0000-000000000000";

    private Hwid() {
    }

    public static String get() {
        return "FAKE-00000000-0000-0000-0000-000000000000";
    }

    public static String shortForm() {
        return "FAKE-00000000-0000-0000-0000-000000000000";
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static String windowsMachineGuid() {
        return "FAKE-00000000-0000-0000-0000-000000000000";
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static String sha256(String s) {
        return "FAKE-00000000-0000-0000-0000-000000000000";
    }
}
