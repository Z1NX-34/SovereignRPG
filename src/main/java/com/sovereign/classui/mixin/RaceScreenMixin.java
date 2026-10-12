package com.sovereign.classui.mixin;

import com.sovereign.classui.RaceView;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Race screen: two (or more) big square image cards side by side instead of the list. */
@Mixin(targets = "com.example.classmod.client.screen.RaceSelectionScreen", remap = false)
public abstract class RaceScreenMixin {

    @Shadow(remap = false) private List races;
    @Shadow(remap = false) private List raceButtons;
    @Shadow(remap = false) private int selectedIndex;
    @Shadow(remap = false) private int visibleRows;

    @Shadow(remap = false) private void selectRace(int index) {}

    @Inject(method = "m_7856_()V", at = @At("HEAD"), remap = false)
    private void sov$init(CallbackInfo ci) {
        RaceView.reset();
    }

    /** Never scroll: every race gets a card. */
    @Inject(method = "applyAdaptiveLayout()V", at = @At("RETURN"), remap = false)
    private void sov$rows(CallbackInfo ci) {
        visibleRows = 99;
    }

    @Inject(method = "rebuildRaceWidgets()V", at = @At("RETURN"), remap = false)
    private void sov$place(CallbackInfo ci) {
        RaceView.placeButtons((Screen) (Object) this, raceButtons, races.size());
    }

    @Inject(method = "m_88315_(" + com.sovereign.classui.Sig.G + "IIF)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void sov$render(GuiGraphics g, int mx, int my, float pt, CallbackInfo ci) {
        RaceView.draw((Screen) (Object) this, races, raceButtons, selectedIndex, g, mx, my, pt);
        ci.cancel();
    }

    /**
     * Screen.mouseClicked is inherited, so the screen class has no such method to inject into.
     * Declaring it here makes Mixin add it as an override (SRG name m_6375_ = mouseClicked).
     */
    public boolean m_6375_(double mx, double my, int button) {
        Screen s = (Screen) (Object) this;
        if (button == 0) {
            int i = RaceView.hit(s, raceButtons, races.size(), mx, my);
            if (i >= 0) {
                selectRace(i);
                return true;
            }
        }
        return RaceView.passClick(s, mx, my, button);
    }
}
