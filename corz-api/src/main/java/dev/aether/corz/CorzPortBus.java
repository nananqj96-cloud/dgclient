package dev.aether.corz;

public final class CorzPortBus {
   private static volatile CorzPortProvider provider;

   private CorzPortBus() {
   }

   public static void register(CorzPortProvider impl) {
      provider = impl;
   }

   public static CorzPortProvider provider() {
      return provider;
   }

   public static boolean isAvailable() {
      return provider != null;
   }
}
