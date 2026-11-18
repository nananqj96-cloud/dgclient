package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;

/**
 * Hides the vanilla fire particle effects on blocks, leaving the world clean and letting the
 * custom flame (if enabled) or the sky shader be the only fire-like visuals.
 */
public class NoFlameModule extends Module {
   public NoFlameModule() {
      super("No Flame", "Hide vanilla flame particle effects", Category.VISUALS);
   }
}
