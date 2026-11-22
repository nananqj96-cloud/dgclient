package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import net.minecraft.world.entity.player.Input;

public class TenacitySnapTapModule extends Module {
   private boolean pl, pr, pf, pb;
   private int lastH, lastV;

   public TenacitySnapTapModule() {
      super("Tenacity Snap Tap", "Last pressed key wins when holding opposite movement keys (Tenacity port)", Category.MISC);
   }

   /** Apply snap-tap logic to player input. Called from the movement mixin. */
   public static Input apply(Input input) {
      TenacitySnapTapModule instance = instance();
      if (instance == null || !instance.isEnabled()) return input;
      boolean l = input.left();
      boolean r = input.right();
      boolean f = input.forward();
      boolean b = input.backward();
      if (l && !instance.pl) instance.lastH = -1;
      if (r && !instance.pr) instance.lastH = 1;
      if (f && !instance.pf) instance.lastV = 1;
      if (b && !instance.pb) instance.lastV = -1;
      instance.pl = l; instance.pr = r; instance.pf = f; instance.pb = b;
      if (l && r) { l = instance.lastH == -1; r = instance.lastH == 1; }
      if (f && b) { f = instance.lastV == 1; b = instance.lastV == -1; }
      return new Input(f, b, l, r, input.jump(), input.shift(), input.sprint());
   }

   private static TenacitySnapTapModule instance;

   public static TenacitySnapTapModule instance() { return instance; }

   @Override
   public void onEnable() { instance = this; }

   @Override
   protected void onDisable() {
      instance = null;
      pl = pr = pf = pb = false;
      lastH = lastV = 0;
   }
}
