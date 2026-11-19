package dev.aether.accessory;

/**
 * The categories of avatar accessories the Accessories popup can equip. Every type is themed
 * around Aether Client.
 */
public enum AccessoryType {
   CROWN("Crowns", "crown"),
   HALOS("Halos", "halo"),
   HORNS("Horns", "horns"),
   MASKS("Masks", "mask"),
   WINGS("Wings", "wings"),
   FEATHERS("Feathers", "feathers"),
   CAPE("Capes", "cape"),
   BACKPACKS("Backpacks", "backpack");

   private final String displayName;
   private final String iconId;

   AccessoryType(String displayName, String iconId) {
      this.displayName = displayName;
      this.iconId = iconId;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getIconId() {
      return this.iconId;
   }

   /** Singular label used in buttons, e.g. "Crown". */
   public String getSingular() {
      return this.displayName.substring(0, this.displayName.length() - 1);
   }
}
