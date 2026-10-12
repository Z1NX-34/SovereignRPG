package com.sovereign.classui.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The hint / counter texts were dark violet on dark violet. Make them readable. */
@Mixin(targets = "com.example.classmod.client.screen.ClassSelectionScreen", remap = false)
public abstract class TextMixin {
    private static final int BRIGHT = 0xE6DCC0;

    @ModifyConstant(method = "drawTitle(Lnet/minecraft/client/gui/GuiGraphics;)V", constant = @Constant(intValue = 10387552), remap = false)
    private int sov$t1(int v) { return BRIGHT; }

    @ModifyConstant(method = "drawTitle(Lnet/minecraft/client/gui/GuiGraphics;)V", constant = @Constant(floatValue = 150.0f), remap = false)
    private float sov$t2(float v) { return 255.0f; }

    @ModifyConstant(method = "drawNavigationHints(Lnet/minecraft/client/gui/GuiGraphics;)V", constant = @Constant(intValue = 10387552), remap = false)
    private int sov$t3(int v) { return BRIGHT; }

    @ModifyConstant(method = "drawNavigationHints(Lnet/minecraft/client/gui/GuiGraphics;)V", constant = @Constant(floatValue = 180.0f), remap = false)
    private float sov$t4(float v) { return 255.0f; }
}
