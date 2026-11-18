/*
 * Decompiled with CFR 0.152.
 */
package de.donutlarp;

import de.donutlarp.DonutLarp;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ActivityStore {
    private static volatile List<Activity> current = List.of();
    private static volatile long updated;

    private ActivityStore() {
    }

    public static List<Activity> all() {
        return System.currentTimeMillis() - updated > 5000L ? List.of() : current;
    }

    public static void clear() {
        current = List.of();
    }

    public static void handle(byte[] data) {
        ArrayList<Activity> list = new ArrayList<Activity>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));){
            int n = in.readInt();
            for (int i = 0; i < n; ++i) {
                list.add(new Activity(in.readInt(), in.readInt(), in.readInt()));
            }
        }
        catch (IOException e) {
            DonutLarp.LOG.warn("Bad donutlarp:activity packet", (Throwable)e);
            return;
        }
        current = List.copyOf(list);
        updated = System.currentTimeMillis();
    }

    public record Activity(int chunkX, int chunkZ, int players) {
    }
}

