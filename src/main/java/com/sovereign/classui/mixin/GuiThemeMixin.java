package com.sovereign.classui.mixin;

import com.sovereign.classui.Sig;
import com.sovereign.classui.Skin;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces the class mod's flat code-drawn panels/buttons with textured ones. */
@Mixin(targets = "com.example.classmod.client.ui.GuiTheme", remap = false)
public abstract class GuiThemeMixin {

    @Inject(method = "drawPanel(" + Sig.G + "IIIII)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sov$panel(GuiGraphics g, int x, int y, int w, int h, int accent, CallbackInfo ci) {
        Skin.nine(g, Skin.PANEL, x, y, w, h, 8, 32);
        ci.cancel();
    }

    @Inject(method = "drawPanelFlat(" + Sig.G + "IIIII)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sov$flat(GuiGraphics g, int x, int y, int w, int h, int accent, CallbackInfo ci) {
        Skin.nine(g, Skin.CARD, x, y, w, h, 6, 32);
        ci.cancel();
    }

    @Inject(method = "drawMiniPanel(" + Sig.G + "IIIII)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sov$mini(GuiGraphics g, int x, int y, int w, int h, int accent, CallbackInfo ci) {
        Skin.nine(g, Skin.MINI, x, y, w, h, 4, 16);
        ci.cancel();
    }

    // drawButtonFrame(g, x, y, w, h, accent, hovered, active, hoverProgress, pressProgress)
    @Inject(method = "drawButtonFrame(" + Sig.G + "IIIIIZZFF)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sov$btn(GuiGraphics g, int x, int y, int w, int h, int accent,
                                boolean hovered, boolean active, float hoverT, float pressT, CallbackInfo ci) {
        Skin.button(g, x, y, w, h, hovered, active, hoverT);
        ci.cancel();
    }

    // same, with the extra outer-glow flag
    @Inject(method = "drawButtonFrame(" + Sig.G + "IIIIIZZFFZ)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sov$btnGlow(GuiGraphics g, int x, int y, int w, int h, int accent,
                                    boolean hovered, boolean active, float hoverT, float pressT, boolean glow, CallbackInfo ci) {
        Skin.button(g, x, y, w, h, hovered, active, hoverT);
        ci.cancel();
    }

    // Pull every class/side accent colour toward gold so glows, text accents and scrollbars match the theme.
    @Inject(method = "resolveAccent(I)I", at = @At("RETURN"), cancellable = true, remap = false)
    private static void sov$accent(int in, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Skin.blend(cir.getReturnValueI(), Skin.GOLD & 0xFFFFFF, 0.65f));
    }

    @Inject(method = "currentAccent()I", at = @At("RETURN"), cancellable = true, remap = false)
    private static void sov$current(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Skin.GOLD & 0xFFFFFF);
    }

    // Class cards and selection outlines derive their colours from these; pull them toward gold too.
    @Inject(method = "accentFrom(I)I", at = @At("RETURN"), cancellable = true, remap = false)
    private static void sov$accentFrom(int in, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Skin.blend(cir.getReturnValueI(), 0xE8B028, 0.7f));
    }

    @Inject(method = "borderFrom(I)I", at = @At("RETURN"), cancellable = true, remap = false)
    private static void sov$borderFrom(int in, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Skin.blend(cir.getReturnValueI(), 0xB88A20, 0.7f));
    }

    @Inject(method = "highlightFrom(I)I", at = @At("RETURN"), cancellable = true, remap = false)
    private static void sov$highlightFrom(int in, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Skin.blend(cir.getReturnValueI(), 0xFFCD46, 0.7f));
    }
}
