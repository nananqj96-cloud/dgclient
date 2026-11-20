package fr.cake.client;
public final class SkinCatalog {
 // Stable network IDs: removed characters are not silently reassigned.
 public static final int[] IDS={28,30,31,32,24,25,26,27,21,22,20,23};
 public static boolean available(int id){for(int x:IDS)if(x==id)return true;return false;}
}
