package dev.aether.corz;

public final class CorzBus {
   private static volatile CorzProvider provider;

   private CorzBus() {
   }

   public static void register(CorzProvider impl) {
      provider = impl;
   }

   public static CorzProvider provider() {
      return provider;
   }

   public static boolean isAvailable() {
      return provider != null;
   }
}
