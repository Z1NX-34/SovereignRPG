package com.sovereign.classui.mixin;

import com.sovereign.classui.ClassView;
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
 * Class screen: cards sit in a panel between header and details panel (no more overlap),
 * and the details panel is replaced by a taller one with a scrollable attribute grid.
 */
@Mixin(targets = "com.example.classmod.client.screen.ClassSelectionScreen", remap = false)
public abstract class ClassScreenMixin {

    @Shadow(remap = false) private int cardsAreaMargin;
    @Shadow(remap = false) private int cardsAreaWidth;
    @Shadow(remap = false) private int cardsPanelY;
    @Shadow(remap = false) private int cardsPanelH;
    @Shadow(remap = false) private int cardsYOffset;
    @Shadow(remap = false) private int cardWidth;
    @Shadow(remap = false) private int cardHeight;
    @Shadow(remap = false) private int buttonY;
    @Shadow(remap = false) private int detailsX;
    @Shadow(remap = false) private int detailsY;
    @Shadow(remap = false) private int detailsW;
    @Shadow(remap = false) private int detailsH;
    @Shadow(remap = false) private List classCards;
    @Shadow(remap = false) private int selectedIndex;

    @Inject(method = "calculateAdaptiveSizes(I)V", at = @At("RETURN"), remap = false)
    private void sov$layout(int arg, CallbackInfo ci) {
        int pad = Skin.lastPad;
        if (pad <= 0) return;
        Screen s = (Screen) (Object) this;
        int w = s.width, h = s.height;

        cardsAreaMargin = pad + 6;                       // panel x = pad, same as header banner
        cardsAreaWidth = w - 2 * cardsAreaMargin;

        boolean details = h >= 300;
        int gap = 6;
        int headerBottom = pad + 38;
        detailsH = details ? Math.max(104, Math.min(150, h * 3 / 10)) : 0;
        detailsX = pad;
        detailsW = w - 2 * pad;
        detailsY = details ? buttonY - gap - detailsH : buttonY;

        int panelTop = headerBottom + gap + 2;
        int panelBottom = (details ? detailsY : buttonY) - gap;
        cardsPanelY = panelTop;
        cardsPanelH = Math.max(60, panelBottom - panelTop);

        int label = 14, bottomPad = 8;                   // label row for "N classes available"
        int avail = cardsPanelH - label - bottomPad;
        int ch = Math.min(cardHeight, avail);
        if (ch < cardHeight) {
            cardHeight = ch;
            cardWidth = Math.max(64, (int) (ch / 1.34));
        }
        cardsYOffset = cardsPanelY + label + Math.max(0, (avail - cardHeight) / 2);
    }

    @Inject(method = "drawSelectionIndicator(" + com.sovereign.classui.Sig.G + ")V", at = @At("HEAD"), cancellable = true, remap = false)
    private void sov$details(GuiGraphics g, CallbackInfo ci) {
        if (detailsH > 0) ClassView.draw(this, classCards, selectedIndex, detailsX, detailsY, detailsW, detailsH, g);
        ci.cancel();
    }

    @Inject(method = "m_6050_(DDD)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void sov$scroll(double mx, double my, double delta, CallbackInfoReturnable<Boolean> cir) {
        if (detailsH > 0 && selectedIndex >= 0 && ClassView.scroll(mx, my, delta, detailsX, detailsY, detailsW, detailsH)) {
            cir.setReturnValue(true);
        }
    }
}
