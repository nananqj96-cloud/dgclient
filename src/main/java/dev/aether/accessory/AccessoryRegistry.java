package dev.aether.accessory;

import java.util.ArrayList;
import java.util.List;

/** The full catalogue of Aether-themed avatar accessories. */
public final class AccessoryRegistry {
   // Aether palette
   private static final int AZURE = 0xFF3D7AFF;
   private static final int AZURE_BRIGHT = 0xFF8FB8FF;
   private static final int PINK = 0xFFFF5FA2;
   private static final int PINK_BRIGHT = 0xFFFF9EC4;
   private static final int NEBULA = 0xFF8A5CFF;
   private static final int NEBULA_BRIGHT = 0xFFB98CFF;
   private static final int STARLIGHT = 0xFFFFD166;
   private static final int STARLIGHT_BRIGHT = 0xFFFFE9A8;

   private static final List<Accessory> ALL = List.of(
      // Crowns
      new Accessory("aether-crown", AccessoryType.CROWN, "Aether Crown", "A jewelled crown bearing the Aether mark.", AZURE, AZURE_BRIGHT),
      new Accessory("pinkbloom-crown", AccessoryType.CROWN, "PinkBloom Crown", "A blossoming crown in PinkBloom pink.", PINK, PINK_BRIGHT),
      new Accessory("nebula-crown", AccessoryType.CROWN, "Nebula Crown", "A crown carved from deep nebula light.", NEBULA, NEBULA_BRIGHT),
      new Accessory("starlight-crown", AccessoryType.CROWN, "Starlight Crown", "A crown woven from starlight gold.", STARLIGHT, STARLIGHT_BRIGHT),
      // Halos
      new Accessory("aether-halo", AccessoryType.HALOS, "Aether Halo", "A glowing halo crowning the Aether mark.", AZURE, AZURE_BRIGHT),
      new Accessory("pinkbloom-halo", AccessoryType.HALOS, "PinkBloom Halo", "A soft pink halo of blossom light.", PINK, PINK_BRIGHT),
      new Accessory("nebula-halo", AccessoryType.HALOS, "Nebula Halo", "A halo ringed with nebula mist.", NEBULA, NEBULA_BRIGHT),
      new Accessory("starlight-halo", AccessoryType.HALOS, "Starlight Halo", "A halo of starlight gold.", STARLIGHT, STARLIGHT_BRIGHT),
      // Horns
      new Accessory("aether-horns", AccessoryType.HORNS, "Aether Horns", "Twisting azure horns engraved with the Aether mark.", AZURE, AZURE_BRIGHT),
      new Accessory("pinkbloom-horns", AccessoryType.HORNS, "PinkBloom Horns", "Curling pink horns of PinkBloom.", PINK, PINK_BRIGHT),
      new Accessory("nebula-horns", AccessoryType.HORNS, "Nebula Horns", "Horns wreathed in nebula flame.", NEBULA, NEBULA_BRIGHT),
      new Accessory("starlight-horns", AccessoryType.HORNS, "Starlight Horns", "Horns forged from starlight.", STARLIGHT, STARLIGHT_BRIGHT),
      // Masks
      new Accessory("aether-mask", AccessoryType.MASKS, "Aether Mask", "A visor mask stamped with the Aether mark.", AZURE, AZURE_BRIGHT),
      new Accessory("pinkbloom-mask", AccessoryType.MASKS, "PinkBloom Mask", "A petal-pink veil mask.", PINK, PINK_BRIGHT),
      new Accessory("nebula-mask", AccessoryType.MASKS, "Nebula Mask", "A mask of swirling nebula glass.", NEBULA, NEBULA_BRIGHT),
      new Accessory("starlight-mask", AccessoryType.MASKS, "Starlight Mask", "A gold starlight visor mask.", STARLIGHT, STARLIGHT_BRIGHT),
      // Wings
      new Accessory("aether-wings", AccessoryType.WINGS, "Aether Wings", "Glow-etched wings in the Aether azure.", AZURE, AZURE_BRIGHT),
      new Accessory("pinkbloom-wings", AccessoryType.WINGS, "PinkBloom Wings", "Petal wings in PinkBloom pink.", PINK, PINK_BRIGHT),
      new Accessory("nebula-wings", AccessoryType.WINGS, "Nebula Wings", "Wings trailing nebula mist.", NEBULA, NEBULA_BRIGHT),
      new Accessory("starlight-wings", AccessoryType.WINGS, "Starlight Wings", "Wings dusted with starlight.", STARLIGHT, STARLIGHT_BRIGHT),
      // Feathers
      new Accessory("aether-feathers", AccessoryType.FEATHERS, "Aether Feathers", "Floating azure feathers of the Aether.", AZURE, AZURE_BRIGHT),
      new Accessory("pinkbloom-feathers", AccessoryType.FEATHERS, "PinkBloom Feathers", "Floating PinkBloom petals.", PINK, PINK_BRIGHT),
      new Accessory("nebula-feathers", AccessoryType.FEATHERS, "Nebula Feathers", "Drifting nebula feathers.", NEBULA, NEBULA_BRIGHT),
      new Accessory("starlight-feathers", AccessoryType.FEATHERS, "Starlight Feathers", "Glimmering starlight feathers.", STARLIGHT, STARLIGHT_BRIGHT),
      // Capes
      new Accessory("aether-cloak", AccessoryType.CAPE, "Aether Cloak", "A flowing cloak in Aether azure.", AZURE, AZURE_BRIGHT),
      new Accessory("pinkbloom-cloak", AccessoryType.CAPE, "PinkBloom Cloak", "A blossom-pink flowing cloak.", PINK, PINK_BRIGHT),
      new Accessory("nebula-cloak", AccessoryType.CAPE, "Nebula Cloak", "A cloak of nebula night.", NEBULA, NEBULA_BRIGHT),
      new Accessory("starlight-cloak", AccessoryType.CAPE, "Starlight Cloak", "A golden starlight cloak.", STARLIGHT, STARLIGHT_BRIGHT),
      // Backpacks
      new Accessory("aether-pack", AccessoryType.BACKPACKS, "Aether Pack", "A pack bearing the Aether mark in azure.", AZURE, AZURE_BRIGHT),
      new Accessory("pinkbloom-pack", AccessoryType.BACKPACKS, "PinkBloom Pack", "A blossom-pink adventure pack.", PINK, PINK_BRIGHT),
      new Accessory("nebula-pack", AccessoryType.BACKPACKS, "Nebula Pack", "A pack of nebula leather.", NEBULA, NEBULA_BRIGHT),
      new Accessory("starlight-pack", AccessoryType.BACKPACKS, "Starlight Pack", "A golden starlight pack.", STARLIGHT, STARLIGHT_BRIGHT)
   );

   private AccessoryRegistry() {
   }

   public static List<Accessory> all() {
      return ALL;
   }

   public static List<Accessory> forType(AccessoryType type) {
      List<Accessory> out = new ArrayList<>();
      for (Accessory a : ALL) {
         if (a.type() == type) {
            out.add(a);
         }
      }
      return out;
   }

   public static Accessory find(String id) {
      for (Accessory a : ALL) {
         if (a.id().equals(id)) {
            return a;
         }
      }
      return null;
   }
}
