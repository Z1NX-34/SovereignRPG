package com.sovereign.classui.mixin;

import com.sovereign.classui.Sig;
import com.sovereign.classui.Skin;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the animated smoke/snow background (cheaper on weak GPUs too). */
@Mixin(targets = "com.example.classmod.client.render.ClassMenuBackgroundRenderer", remap = false)
public abstract class BackgroundMixin {

    @Inject(method = "render(" + Sig.G + "IIFF)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sov$r5(GuiGraphics g, int w, int h, float a, float b, CallbackInfo ci) {
        Skin.background(g, w, h);
        ci.cancel();
    }

    @Inject(method = "render(" + Sig.G + "IIFF" + Sig.I + ")V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sov$r6(GuiGraphics g, int w, int h, float a, float b, Integer acc, CallbackInfo ci) {
        Skin.background(g, w, h);
        ci.cancel();
    }

    @Inject(method = "render(" + Sig.G + "IIFF" + Sig.I + "II)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sov$r8(GuiGraphics g, int w, int h, float a, float b, Integer acc, int c, int d, CallbackInfo ci) {
        Skin.background(g, w, h);
        ci.cancel();
    }

    @Inject(method = "renderLowFx(" + Sig.G + "IIFF" + Sig.I + "II)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sov$low(GuiGraphics g, int w, int h, float a, float b, Integer acc, int c, int d, CallbackInfo ci) {
        Skin.background(g, w, h);
        ci.cancel();
    }
}
