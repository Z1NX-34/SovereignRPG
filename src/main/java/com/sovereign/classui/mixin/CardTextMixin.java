package com.sovereign.classui.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Brighter card numbers (01, 02 ...). */
@Mixin(targets = "com.example.classmod.client.screen.widgets.ClassCardWidget", remap = false)
public abstract class CardTextMixin {
    @ModifyConstant(method = "drawCardStatus(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;)V", constant = @Constant(intValue = 7700113), remap = false)
    private int sov$num(int v) { return 0xD9C98F; }
}
