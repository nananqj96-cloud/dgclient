package dev.aether.module;

public enum Category {
   COMBAT("Combat", "combat"),
   MISC("Misc", "misc"),
   RENDER("Render", "render"),
   VISUALS("Visuals", "visuals"),
   MOVEMENT("Movement", "movement"),
   PLAYER("Player", "player"),
   BASEFINDING("Basefinding", "basefinding"),
   DONUT("Donut", "donut"),
   CLIENT("Client", "client");

   private final String displayName;
   private final String iconId;

   private Category(String displayName, String iconId) {
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
