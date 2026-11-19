package dev.aether.accessory;

/**
 * A single avatar accessory. Every variant is themed around Aether Client — its name, its
 * description and its two-colour palette (primary + glow secondary) all read as Aether.
 */
public record Accessory(
   String id,
   AccessoryType type,
   String name,
   String description,
   int primary,
   int secondary
) {
   public boolean matches(String query) {
      String q = query.toLowerCase(java.util.Locale.ROOT);
      return this.name.toLowerCase(java.util.Locale.ROOT).contains(q)
         || this.description.toLowerCase(java.util.Locale.ROOT).contains(q)
         || this.type.getDisplayName().toLowerCase(java.util.Locale.ROOT).contains(q);
   }
}
