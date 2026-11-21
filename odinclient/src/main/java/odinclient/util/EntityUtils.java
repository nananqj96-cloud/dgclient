package odinclient.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.RegistryKey;
import net.minecraft.core.registries.RegistryKeys;
import net.minecraft.core.Registry.Entry;

import org.joml.Vector3d;

@Environment(EnvType.CLIENT)
public final class EntityUtils {
    private EntityUtils() {}

    public static Vector3d getInterpolatedPos(Vector3d out, Entity entity, double tickDelta) {
        return out.set(
                entity.lastX + (entity.getX() - entity.lastX) * tickDelta,
                entity.lastY + (entity.getY() - entity.lastY) * tickDelta,
                entity.lastZ + (entity.getZ() - entity.lastZ) * tickDelta);
    }

    public static int getEnchantmentLevel(ItemStack stack, RegistryKey<Enchantment> key) {
        Minecraft client = Minecraft.getInstance();
        if (client.world != null && stack != null && key != null) {
            Registry.Entry<Enchantment> entry =
                    client.world
                            .getRegistryManager()
                            .getOrThrow(RegistryKeys.ENCHANTMENT)
                            .getEntry(key.getValue())
                            .orElse(null);
            return entry == null ? 0 : EnchantmentHelper.getLevel(entry, stack);
        } else {
            return 0;
        }
    }
}
