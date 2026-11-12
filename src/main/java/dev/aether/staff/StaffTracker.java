package dev.aether.staff;

import dev.aether.module.impl.StaffListModule;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.PlayerTeam;

public final class StaffTracker {
   private final StaffListModule module;
   private volatile List<StaffEntry> current = List.of();
   private int ticks;
   private static volatile List<StaffEntry> debugInject;
   private static final Set<String> CONFIRMED_STAFF_NAMES = Set.of("archivepedro");

   public StaffTracker(StaffListModule var1) {
      this.module = var1;
   }

   public List<StaffEntry> current() {
      return debugInject == null ? this.current : debugInject;
   }

   public void reset() {
      this.current = List.of();
      this.ticks = 0;
   }

   public void clear() {
      this.reset();
   }

   public void tick() {
      if (debugInject == null && this.module.isEnabled() && ++this.ticks % 10 == 0) {
         Minecraft var1 = Minecraft.getInstance();
         ClientPacketListener var2 = var1.getConnection();
         if (var2 != null && var1.player != null) {
            ArrayList<StaffEntry> var3 = new ArrayList<>();

            for (PlayerInfo var5 : var2.getListedOnlinePlayers()) {
               PlayerTeam var6 = var5.getTeam();
               String var7 = var5.getProfile().name();
               StaffEntry var8 = StaffDetector.classifyConfirmedBadge(
                  var7,
                  var5.getTabListDisplayName(),
                  var6 == null ? null : var6.getPlayerPrefix(),
                  var6 == null ? null : var6.getPlayerSuffix(),
                  var5.getLatency()
               );
               if (var8 == null && CONFIRMED_STAFF_NAMES.contains(var7.toLowerCase(Locale.ROOT))) {
                  var8 = new StaffEntry(var7, "Staff", -16712568, false, var5.getLatency(), 2);
               }

               if (var8 != null) {
                  var3.add(var8);
               }
            }

            var3.sort(Comparator.comparing(var0 -> var0.name().toLowerCase(Locale.ROOT)));
            this.current = List.copyOf(var3);
         } else {
            this.current = List.of();
         }
      }
   }

   public static void injectForTest(List<StaffEntry> var0) {
      debugInject = var0 == null ? null : List.copyOf(var0);
   }

   public static void clearInject() {
      debugInject = null;
   }
}
