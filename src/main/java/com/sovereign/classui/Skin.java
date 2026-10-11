package com.sovereign.classui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * All drawing lives here. Every texture is looked up by path, so a resource pack
 * (your texture pack) can replace any of them by shipping the same file under
 * assets/sovereignclassui/textures/gui/.  Keep the sizes listed in the README.
 */
public final class Skin {
    private Skin() {}

    private static ResourceLocation t(String n) {
        return new ResourceLocation("sovereignclassui", "textures/gui/" + n + ".png");
    }

    public static final ResourceLocation PANEL = t("panel");            // 32x32, border 8
    public static final ResourceLocation CARD = t("card");              // 32x32, border 6
    public static final ResourceLocation MINI = t("mini");              // 16x16, border 4
    public static final ResourceLocation BTN = t("button_normal");      // 24x24, border 6
    public static final ResourceLocation BTN_HOVER = t("button_hover");
    public static final ResourceLocation BTN_OFF = t("button_disabled");
    public static final ResourceLocation C_TL = t("corner_tl");         // 40x40 each
    public static final ResourceLocation C_TR = t("corner_tr");
    public static final ResourceLocation C_BL = t("corner_bl");
    public static final ResourceLocation C_BR = t("corner_br");

    /** Last value returned by the patched GuiLayout.outerPad (the header banner's inset). */
    public static volatile int lastPad = 0;

    public static final int CORNER = 40;
    public static final int GOLD = 0xFFE8B028;
    public static final int GOLD_DARK = 0xFF824F10;
    public static final int PURPLE = 0xFF46386C;
    public static final int BG_TOP = 0xFF0A0614;
    public static final int BG_BOT = 0xFF1B1133;

    public static int blend(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl; // RGB only (callers add alpha)
    }

    /** 9-slice: corners fixed, edges and centre stretched. */
    public static void nine(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int b, int ts) {
        if (w <= 0 || h <= 0) return;
        b = Math.min(b, Math.min(w, h) / 2);
        if (b <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int mw = w - 2 * b, mh = h - 2 * b, tm = ts - 2 * b;
        g.blit(tex, x, y, b, b, 0, 0, b, b, ts, ts);
        g.blit(tex, x + w - b, y, b, b, ts - b, 0, b, b, ts, ts);
        g.blit(tex, x, y + h - b, b, b, 0, ts - b, b, b, ts, ts);
        g.blit(tex, x + w - b, y + h - b, b, b, ts - b, ts - b, b, b, ts, ts);
        if (mw > 0) {
            g.blit(tex, x + b, y, mw, b, b, 0, tm, b, ts, ts);
            g.blit(tex, x + b, y + h - b, mw, b, b, ts - b, tm, b, ts, ts);
        }
        if (mh > 0) {
            g.blit(tex, x, y + b, b, mh, 0, b, b, tm, ts, ts);
            g.blit(tex, x + w - b, y + b, b, mh, ts - b, b, b, tm, ts, ts);
        }
        if (mw > 0 && mh > 0) g.blit(tex, x + b, y + b, mw, mh, b, b, tm, tm, ts, ts);
    }

    public static void button(GuiGraphics g, int x, int y, int w, int h, boolean hovered, boolean active, float hoverT) {
        ResourceLocation tex = !active ? BTN_OFF : (hovered || hoverT > 0.5f ? BTN_HOVER : BTN);
        nine(g, tex, x, y, w, h, 6, 24);
    }

    /** Replacement for the animated smoke/snow background: dark violet + gold frame + corner ornaments. */
    public static void background(GuiGraphics g, int w, int h) {
        g.fillGradient(0, 0, w, h, BG_TOP, BG_BOT);
        // soft edge vignette
        g.fillGradient(0, 0, w, h / 5, 0xAA000000, 0x00000000);
        g.fillGradient(0, h - h / 5, w, h, 0x00000000, 0xAA000000);
        int m = 6;
        // outer gold line + inner purple line
        rectLine(g, m, m, w - m, h - m, GOLD);
        rectLine(g, m + 2, m + 2, w - m - 2, h - m - 2, PURPLE);
        // thick top band
        g.fill(m + 1, m + 1, w - m - 1, m + 3, GOLD);
        g.fill(m + 3, m + 3, w - m - 3, m + 4, GOLD_DARK);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int c = CORNER;
        g.blit(C_TL, m - 1, m - 1, 0, 0, c, c, c, c);
        g.blit(C_TR, w - m + 1 - c, m - 1, 0, 0, c, c, c, c);
        g.blit(C_BL, m - 1, h - m + 1 - c, 0, 0, c, c, c, c);
        g.blit(C_BR, w - m + 1 - c, h - m + 1 - c, 0, 0, c, c, c, c);
    }

    private static void rectLine(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        g.fill(x0, y0, x1, y0 + 1, col);
        g.fill(x0, y1 - 1, x1, y1, col);
        g.fill(x0, y0, x0 + 1, y1, col);
        g.fill(x1 - 1, y0, x1, y1, col);
    }
}
