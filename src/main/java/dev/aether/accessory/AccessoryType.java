package dev.aether.accessory;

/**
 * The categories of avatar accessories the Accessories popup can equip. Every type is themed
 * around Aether Client.
 */
public enum AccessoryType {
   CROWN("Crowns", "crown"),
   WINGS("Wings", "wings"),
   FEATHERS("Feathers", "feathers"),
   CAPE("Capes", "cape");

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
}
