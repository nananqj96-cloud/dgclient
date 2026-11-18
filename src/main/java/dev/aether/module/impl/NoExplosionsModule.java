package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;

/**
 * Removes explosion particle effects (anchors, TNT, creepers, crystals and any other explosions)
 * for a cleaner, less chaotic world.
 */
public class NoExplosionsModule extends Module {
   public final BooleanSetting anchorExplosions = this.addSetting(
      new BooleanSetting("Anchor Explosions", "Remove respawn anchor explosion effects", true)
   );
   public final BooleanSetting tntExplosions = this.addSetting(
      new BooleanSetting("TNT Explosions", "Remove TNT explosion effects", true)
   );
   public final BooleanSetting creeperExplosions = this.addSetting(
      new BooleanSetting("Creeper Explosions", "Remove creeper explosion effects", true)
   );
   public final BooleanSetting crystalExplosions = this.addSetting(
      new BooleanSetting("Crystal Explosions", "Remove end crystal explosion effects", true)
   );
   public final BooleanSetting otherExplosions = this.addSetting(
      new BooleanSetting("Other Explosions", "Remove all other explosion effects", true)
   );

   public NoExplosionsModule() {
      super("No Explosions", "Remove explosion particle effects", Category.VISUALS);
   }

   public boolean isBase() {
      return this.anchorExplosions.get()
         && this.tntExplosions.get()
         && this.creeperExplosions.get()
         && this.crystalExplosions.get()
         && this.otherExplosions.get();
   }

   public boolean isOtherExplosions() {
      return this.otherExplosions.get();
   }
}
