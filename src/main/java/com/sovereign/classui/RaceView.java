package com.sovereign.classui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;

/**
 * Race screen: big square image cards side by side, description panel underneath.
 * Card pictures: assets/sovereignclassui/textures/gui/race/<race id>.png  (any square size, 256x256 is good).
 */
public final class RaceView {
    private RaceView() {}

    public static final int TEXT = 0xFFEFE6D2;
    public static final int GOLD_TXT = 0xFFFFD36A;

    // ---------- layout ----------
    public static final class Layout {
        public int pad, headerH = 38, cardsY, size, plate = 20, gap, x0, count;
        public int detX, detY, detW, detH, btnY;
        public int cardX(int i) { return x0 + i * (size + gap); }
        public int cardH() { return size + plate; }
    }

    public static Layout layout(int w, int h, int count, int btnH) {
        Layout l = new Layout();
        l.pad = Skin.lastPad > 0 ? Skin.lastPad : 28;
        l.count = Math.max(1, count);
        l.btnY = h - l.pad - btnH;
        l.detH = h >= 300 ? 112 : 70;
        l.detY = l.btnY - 6 - l.detH;
        int top = l.pad + l.headerH + 8;
        int block = l.detY - 8 - top;
        int s = Math.max(48, Math.min(150, block - l.plate));
        int availW = w - 2 * l.pad - 16;
        l.gap = Math.max(14, Math.min(44, s / 3));
        int byW = (availW - (l.count - 1) * l.gap) / l.count;
        l.size = Math.max(40, Math.min(s, byW));
        int total = l.count * l.size + (l.count - 1) * l.gap;
        l.x0 = (w - total) / 2;
        l.cardsY = top + Math.max(0, (block - (l.size + l.plate)) / 2);
        l.detW = Math.min(w - 2 * l.pad, Math.max(total + 90, 360));
        l.detX = (w - l.detW) / 2;
        return l;
    }

    // ---------- race data (RaceDef is private inside the class mod) ----------
    private static Field fId, fName, fDesc;

    public static String[] read(Object raceDef) {
        try {
            if (fId == null) {
                Class<?> c = raceDef.getClass();
                fId = c.getDeclaredField("id");
                fName = c.getDeclaredField("displayName");
                fDesc = c.getDeclaredField("description");
                fId.setAccessible(true);
                fName.setAccessible(true);
                fDesc.setAccessible(true);
            }
            return new String[] {String.valueOf(fId.get(raceDef)), String.valueOf(fName.get(raceDef)), String.valueOf(fDesc.get(raceDef))};
        } catch (Throwable t) {
            return new String[] {"?", "?", ""};
        }
    }

    // ---------- images ----------
    private static final class Tex {
        ResourceLocation loc; int w = 256, h = 256; boolean custom;
    }
    private static final Map<String, Tex> CACHE = new HashMap<>();

    private static int lastHover = -1;
    private static int descScroll, descLines, descVis, lastSel = -2;

    public static void reset() { CACHE.clear(); descScroll = 0; lastHover = -1; }

    public static boolean scroll(Screen s, double mx, double my, double delta) {
        if (descLines <= descVis) return false;
        descScroll = Math.max(0, Math.min(descLines - descVis, descScroll - (int) Math.signum(delta)));
        return true;
    }

    private static String clean(String id) {
        StringBuilder sb = new StringBuilder();
        for (char ch : id.toLowerCase(Locale.ROOT).toCharArray()) {
            sb.append((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '.' ? ch : '_');
        }
        return sb.toString();
    }

    private static Tex tex(String id) {
        Tex t = CACHE.get(id);
        if (t != null) return t;
        t = new Tex();
        String c = clean(id);
        try {
            var rm = Minecraft.getInstance().getResourceManager();
            ResourceLocation loc = new ResourceLocation("sovereignclassui", "textures/gui/race/" + c + ".png");
            Optional<Resource> r = rm.getResource(loc);
            if (r.isEmpty()) {
                loc = new ResourceLocation("sovereignclassui", "textures/gui/race/_default.png");
                r = rm.getResource(loc);
            } else {
                t.custom = true;
            }
            if (r.isPresent()) {
                try (InputStream in = r.get().open(); NativeImage img = NativeImage.read(in)) {
                    t.w = img.getWidth();
                    t.h = img.getHeight();
                }
                t.loc = loc;
            }
        } catch (Throwable ignored) {
            t.loc = null;
        }
        CACHE.put(id, t);
        return t;
    }

    // ---------- drawing ----------
    public static void draw(Screen s, List<?> races, List<?> raceButtons, int selected, GuiGraphics g, int mx, int my, float pt) {
        Font font = Minecraft.getInstance().font;
        int w = s.width, h = s.height;

        int btnH = 22;
        for (GuiEventListener c : s.children()) {
            if (c instanceof AbstractWidget a && !raceButtons.contains(c)) btnH = a.getHeight();
        }
        Layout l = layout(w, h, races.size(), btnH);

        Skin.background(g, w, h);

        // header banner
        Skin.nine(g, Skin.CARD, l.pad, l.pad, w - 2 * l.pad, l.headerH, 6, 32);
        g.drawCenteredString(font, Component.literal("").append(s.getTitle()).withStyle(st -> st.withBold(true)), w / 2, l.pad + 8, GOLD_TXT);
        g.drawCenteredString(font, Component.translatable("classmod.race.subtitle"), w / 2, l.pad + 22, TEXT);

        // cards
        int hover = -1;
        for (int i = 0; i < races.size(); i++) {
            int x = l.cardX(i), y = l.cardsY;
            boolean over = mx >= x && mx < x + l.size && my >= y && my < y + l.cardH();
            if (over) hover = i;
            card(g, font, l, i, x, y, over, i == selected, selected >= 0, read(races.get(i)));
        }

        // details panel
        Skin.nine(g, Skin.PANEL, l.detX, l.detY, l.detW, l.detH, 8, 32);
        int tx = l.detX + 14, tw = l.detW - 28;
        if (hover >= 0) lastHover = hover;
        int shown = hover >= 0 ? hover : selected >= 0 ? selected : lastHover;   // hovering previews a race
        if (shown != lastSel) { lastSel = shown; descScroll = 0; }
        if (shown >= 0 && shown < races.size()) {
            String[] r = read(races.get(shown));
            g.drawString(font, Component.literal(r[1]).withStyle(st -> st.withBold(true)), tx, l.detY + 9, GOLD_TXT, false);
            g.fill(tx, l.detY + 21, tx + tw, l.detY + 22, 0xAAE8B028);
            int top = l.detY + 28, bottom = l.detY + l.detH - 16;   // bottom strip is reserved for the hint
            int vis = Math.max(1, (bottom - top) / 11);
            List<FormattedCharSequence> all = font.split(FormattedText.of(r[2]), Math.max(40, tw - 14));
            descLines = all.size(); descVis = vis;
            descScroll = Math.max(0, Math.min(descScroll, all.size() - vis));
            for (int i = 0; i < vis && i + descScroll < all.size(); i++) {
                g.drawString(font, all.get(i + descScroll), tx, top + i * 11, TEXT, false);
            }
            if (all.size() > vis) {   // scroll bar + hint so the text never just stops
                int bx = tx + tw - 3, bh = vis * 11;
                g.fill(bx - 1, top - 1, bx + 4, top + bh + 1, 0xAA0A0614);
                g.fill(bx, top, bx + 3, top + bh, 0xFF2A1F4A);
                int th = Math.max(12, bh * vis / all.size());
                int ty = top + (bh - th) * descScroll / Math.max(1, all.size() - vis);
                g.fill(bx, ty, bx + 3, ty + th, Skin.GOLD);
                g.drawString(font, "scroll for more", tx, l.detY + l.detH - 12, 0xFF9C8FC0, false);
            }
            if (!tex(r[0]).custom) {
                String hint = "image: textures/gui/race/" + clean(r[0]) + ".png";
                g.drawString(font, hint, l.detX + l.detW - 14 - font.width(hint), l.detY + l.detH - 12, 0xFF9C8FC0, false);
            }
        } else {
            g.drawCenteredString(font, Component.literal("Hover a race to preview it, click to choose"), l.detX + l.detW / 2, l.detY + l.detH / 2 - 4, 0xFFB9A9D8);
        }

        // confirm / cancel (the race buttons themselves are drawn as cards above)
        for (GuiEventListener c : s.children()) {
            if (c instanceof Renderable rd && !raceButtons.contains(c)) rd.render(g, mx, my, pt);
        }
    }

    private static void card(GuiGraphics g, Font font, Layout l, int i, int x, int y, boolean over, boolean sel, boolean anySel, String[] r) {
        int w = l.size, h = l.cardH();
        int gold = Skin.GOLD;
        if (sel) { // soft outer glow
            g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0x33E8B028);
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0x55E8B028);
        }
        Skin.nine(g, Skin.CARD, x, y, w, h, 6, 32);

        int ix = x + 5, iy = y + 5, iw = w - 10, ih = w - 10;
        g.fill(ix, iy, ix + iw, iy + ih, 0xFF0A0614);
        Tex t = tex(r[0]);
        if (t.loc != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(t.loc, ix, iy, iw, ih, 0, 0, t.w, t.h, t.w, t.h);
        }
        // inner frame
        int fc = sel ? gold : (over ? 0xFFFFCD46 : 0xFF824F10);
        g.fill(ix, iy, ix + iw, iy + 1, fc);
        g.fill(ix, iy + ih - 1, ix + iw, iy + ih, fc);
        g.fill(ix, iy, ix + 1, iy + ih, fc);
        g.fill(ix + iw - 1, iy, ix + iw, iy + ih, fc);
        if (sel) {
            g.fill(ix + 1, iy + 1, ix + iw - 1, iy + 2, gold);
            g.fill(ix + 1, iy + ih - 2, ix + iw - 1, iy + ih - 1, gold);
            g.fill(ix + 1, iy + 1, ix + 2, iy + ih - 1, gold);
            g.fill(ix + iw - 2, iy + 1, ix + iw - 1, iy + ih - 1, gold);
        }
        if (over && !sel) g.fill(ix + 1, iy + 1, ix + iw - 1, iy + ih - 1, 0x22FFFFFF);
        if (anySel && !sel && !over) g.fill(ix + 1, iy + 1, ix + iw - 1, iy + ih - 1, 0x66000000);

        // name plate
        int py = y + w - 3;
        String name = r[1];
        int col = sel ? GOLD_TXT : TEXT;
        g.drawCenteredString(font, Component.literal(name).withStyle(st -> st.withBold(true)), x + w / 2, py + (l.plate - 8) / 2 + 1, col);
    }

    /** Footer buttons are created by the class mod; keep them inside the frame. */
    public static void placeButtons(Screen s, List<?> raceButtons, int count) {
        int btnH = 22;
        for (GuiEventListener c : s.children()) {
            if (c instanceof AbstractWidget a && !raceButtons.contains(c)) btnH = a.getHeight();
        }
        Layout l = layout(s.width, s.height, count, btnH);
        for (GuiEventListener c : s.children()) {
            if (c instanceof AbstractWidget a && !raceButtons.contains(c)) a.setY(l.btnY);
        }
        for (Object o : raceButtons) {      // parked off-screen: the cards handle clicks
            AbstractWidget b = (AbstractWidget) o;
            b.setX(-2000);
            b.setY(-2000);
        }
    }

    /** Index of the card under the mouse, or -1. */
    public static int hit(Screen s, List<?> raceButtons, int count, double mx, double my) {
        int btnH = 22;
        for (GuiEventListener c : s.children()) {
            if (c instanceof AbstractWidget a && !raceButtons.contains(c)) btnH = a.getHeight();
        }
        Layout l = layout(s.width, s.height, count, btnH);
        for (int i = 0; i < count; i++) {
            int x = l.cardX(i);
            if (mx >= x && mx < x + l.size && my >= l.cardsY && my < l.cardsY + l.cardH()) return i;
        }
        return -1;
    }

    /** Same loop as vanilla Screen.mouseClicked, for clicks that are not on a card. */
    public static boolean passClick(Screen s, double mx, double my, int button) {
        for (GuiEventListener c : s.children()) {
            if (c.mouseClicked(mx, my, button)) {
                s.setFocused(c);
                if (button == 0) s.setDragging(true);
                return true;
            }
        }
        return false;
    }
}
