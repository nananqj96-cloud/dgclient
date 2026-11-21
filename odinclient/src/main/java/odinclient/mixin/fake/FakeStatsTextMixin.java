package odinclient.mixin.fake;

import net.minecraft.core.text.TextVisitFactory;

import odinclient.module.fake.FakeStats;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TextVisitFactory.class)
public abstract class FakeStatsTextMixin {
    @ModifyArg(
            method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/core/text/Style;Lnet/minecraft/core/text/CharacterVisitor;)Z",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/core/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/core/text/Style;Lnet/minecraft/core/text/Style;Lnet/minecraft/core/text/CharacterVisitor;)Z"),
            index = 0)
    private static String odinclient$replaceFakeStats(String text) {
        return FakeStats.applyFakeStats(text);
    }
}
