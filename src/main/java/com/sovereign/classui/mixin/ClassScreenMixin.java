package com.sovereign.classui.mixin;

import com.sovereign.classui.Skin;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Class screen layout fixes, applied right after the screen computes its sizes:
 *  - card panel gets the same side margin as the header banner (they were different widths)
 *  - cards drop down inside the panel so the "N classes available" label no longer overlaps card 01
 *  - the card group is centred in the space between header and button, with a gap under the header
 */
@Mixin(targets = "com.example.classmod.client.screen.ClassSelectionScreen", remap = false)
public abstract class ClassScreenMixin {

    @Shadow(remap = false) private int cardsAreaMargin;
    @Shadow(remap = false) private int cardsAreaWidth;
    @Shadow(remap = false) private int cardsPanelY;
    @Shadow(remap = false) private int cardsPanelH;
    @Shadow(remap = false) private int cardsYOffset;

    @Inject(method = "calculateAdaptiveSizes(I)V", at = @At("RETURN"), remap = false)
    private void sov$layout(int arg, CallbackInfo ci) {
        int pad = Skin.lastPad;
        if (pad <= 0) return;
        Screen s = (Screen) (Object) this;
        int w = s.width, h = s.height;

        cardsAreaMargin = pad + 6;                 // panel x = margin - 6 = pad, same as the header
        cardsAreaWidth = w - 2 * cardsAreaMargin;

        int room = 14;                             // space for the "N classes available" label
        cardsYOffset += room;
        cardsPanelH += room;

        int headerBottom = pad + 38;               // header banner is 38 tall
        int buttonTop = (int) (h * 0.80f);
        int centred = headerBottom + Math.max(0, buttonTop - headerBottom - cardsPanelH) / 2;
        int target = Math.max(headerBottom + 8, centred);
        int dy = target - cardsPanelY;
        cardsPanelY += dy;
        cardsYOffset += dy;
    }
}
