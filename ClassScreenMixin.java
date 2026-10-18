package com.sovereign.classui.mixin;

import com.sovereign.classui.ClassPager;
import com.sovereign.classui.Skin;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Class screen as a pager: one class at a time (model, equipment, abilities, attributes with bars).
 * The mod's own card widgets stay alive (selection, confirm, keys) but are parked off-screen.
 */
@Mixin(targets = "com.example.classmod.client.screen.ClassSelectionScreen", remap = false)
public abstract class ClassScreenMixin {

    @Shadow(remap = false) private int buttonY;
    @Shadow(remap = false) private int buttonHeight;
    @Shadow(remap = false) private List classCards;
    @Shadow(remap = false) private int selectedIndex;

    @Shadow(remap = false) private void selectCard(int index) {}

    @Inject(method = "calculateAdaptiveSizes(I)V", at = @At("RETURN"), remap = false)
    private void sov$layout(int arg, CallbackInfo ci) {
        int pad = Skin.lastPad;
        if (pad <= 0) return;
        buttonY = ((Screen) (Object) this).height - pad - buttonHeight;
    }

    @Inject(method = "m_7856_()V", at = @At("RETURN"), remap = false)
    private void sov$park(CallbackInfo ci) {
        ClassPager.park(classCards);
    }

    @Inject(method = "updateCardPositions()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void sov$noMove(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "m_88315_(" + com.sovereign.classui.Sig.G + "IIF)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void sov$render(GuiGraphics g, int mx, int my, float pt, CallbackInfo ci) {
        if (selectedIndex < 0 && !classCards.isEmpty()) selectCard(0);
        ClassPager.draw((Screen) (Object) this, this, classCards, selectedIndex, g, mx, my);
        ci.cancel();
    }

    @Inject(method = "m_6375_(DDI)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void sov$click(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;
        int i = ClassPager.click((Screen) (Object) this, classCards, selectedIndex, mx, my);
        if (i >= 0) {
            selectCard(i);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "m_6050_(DDD)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void sov$scroll(double mx, double my, double delta, CallbackInfoReturnable<Boolean> cir) {
        if (ClassPager.scroll((Screen) (Object) this, classCards, mx, my, delta)) cir.setReturnValue(true);
    }
}
