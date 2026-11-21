package odinclient.mixin.fake;

import net.minecraft.client.Minecraft;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.core.component.DataComponentTypes;
import net.minecraft.core.component.type.EquippableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import odinclient.module.fake.FakeElytra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityRenderer.class)
public abstract class FakeElytraBipedMixin {
    @Inject(method = "updateBipedRenderState", at = @At("TAIL"))
    private static void odinclient$fakeElytraEquipment(
            LivingEntity entity,
            BipedEntityRenderState state,
            float tickDelta,
            ItemModelManager itemModelManager,
            CallbackInfo ci) {
        if (FakeElytra.isEnabled()) {
            ItemStack chestStack = state.equippedChestStack;
            if (chestStack != null && chestStack.isOf(Items.GOLDEN_CHESTPLATE)) {
                EquippableComponent elytra =
                        Items.ELYTRA.getDefaultStack().get(DataComponentTypes.EQUIPPABLE);
                if (elytra != null) {
                    chestStack.set(DataComponentTypes.EQUIPPABLE, elytra);
                }
            }

            if (entity == Minecraft.getInstance().player && FakeElytra.isGliding()) {
                state.isGliding = true;
                state.limbAmplitudeInverse = (float) entity.getVelocity().lengthSquared() / 0.2F;
                state.limbAmplitudeInverse =
                        Math.max(
                                1.0F,
                                state.limbAmplitudeInverse
                                        * state.limbAmplitudeInverse
                                        * state.limbAmplitudeInverse);
            }
        }
    }
}
