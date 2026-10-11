package com.sovereign.classui.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pulls every screen's content inward so it sits inside the gold frame instead of hugging the edges. */
@Mixin(targets = "com.example.classmod.client.ui.GuiLayout", remap = false)
public abstract class LayoutMixin {

    @Inject(method = "outerPad(Lnet/minecraft/client/Minecraft;II)I", at = @At("RETURN"), cancellable = true, remap = false)
    private static void sov$pad(net.minecraft.client.Minecraft mc, int w, int h, CallbackInfoReturnable<Integer> cir) {
        int inset = Math.max(8, Math.min(30, Math.min(w, h) / 9));
        int v = cir.getReturnValueI() + inset;
        com.sovereign.classui.Skin.lastPad = v;
        cir.setReturnValue(v);
    }
}
