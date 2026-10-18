package com.sovereign.classui;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Class screen, "one class at a time" layout:
 *   left   Equipment (starter kit) + Special abilities (the class's passive description)
 *   centre name, tags, model preview, < n / N >, description
 *   right  Attributes as a striped list (scrolls)
 * Everything about the classes is read through reflection, so the class mod is never a build dependency.
 */
public final class ClassPager {
    private ClassPager() {}

    public static final int TEXT = 0xFFEFE6D2, HEAD = 0xFFFFD36A, DIM = 0xFFB9A9D8;

    // ---------- layout ----------
    public static final class Lay {
        public int pad, top, bottom, lx, lw, cx, cw, rx, rw, h;
        public int navY;      // arrows row (inside centre panel)
        public int modelTop, modelBottom, descTop, descBottom;
    }

    public static Lay lay(int w, int h, int btnH) {
        Lay l = new Lay();
        l.pad = Skin.lastPad > 0 ? Skin.lastPad : 28;
        l.top = l.pad + 38 + 6;
        l.bottom = h - l.pad - btnH - 6;
        l.h = l.bottom - l.top;
        int gap = 8, inner = w - 2 * l.pad;
        l.lw = (inner - 2 * gap) * 27 / 100;
        l.cw = (inner - 2 * gap) * 32 / 100;
        l.rw = inner - 2 * gap - l.lw - l.cw;
        l.lx = l.pad; l.cx = l.lx + l.lw + gap; l.rx = l.cx + l.cw + gap;
        l.modelTop = l.top + 30;
        int descH = Math.max(46, l.h * 28 / 100);
        l.descBottom = l.bottom - 8;
        l.descTop = l.descBottom - descH;
        l.navY = l.descTop - 32;
        l.modelBottom = l.navY - 6;
        return l;
    }

    // ---------- reflection helpers ----------
    private static Object call(Object o, String n) {
        try { return o.getClass().getMethod(n).invoke(o); } catch (Throwable t) { return null; }
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static String priv(Object screen, String name, Object arg) {
        try {
            for (Method m : screen.getClass().getDeclaredMethods())
                if (m.getName().equals(name) && m.getParameterCount() == 1) { m.setAccessible(true); return String.valueOf(m.invoke(screen, arg)); }
        } catch (Throwable ignored) {}
        return "?";
    }

    private static Map<String, Double> maxByAttr;
    private static Map<String, Double> maxes() {
        if (maxByAttr != null) return maxByAttr;
        Map<String, Double> m = new HashMap<>();
        try {
            Object all = Class.forName("com.example.classmod.data.ClassManager").getMethod("getClasses").invoke(null);
            for (Object def : (Collection<?>) all) {
                Object attrs = call(def, "getAttributes");
                if (attrs instanceof List<?> l) for (Object a : l) {
                    String id = str(call(a, "getAttributeId"));
                    double v = Math.abs(((Number) call(a, "getAmount")).doubleValue());
                    m.merge(id, v, Math::max);
                }
            }
        } catch (Throwable ignored) {}
        return maxByAttr = m;
    }

    // ---------- per-class cache ----------
    private static Object cDef;
    private static List<String[]> cAttr = new ArrayList<>();   // name, value, ratio
    private static List<ItemStack> cStacks = new ArrayList<>();
    private static List<String> cKit = new ArrayList<>();
    private static int attrScroll, descScroll, passScroll, kitScroll;

    private static void cache(Object screen, Object def) {
        if (def == cDef) return;
        cDef = def; attrScroll = descScroll = passScroll = kitScroll = 0;
        cAttr = new ArrayList<>(); cStacks = new ArrayList<>(); cKit = new ArrayList<>();
        Map<String, Double> mx = maxes();
        Object attrs = call(def, "getAttributes");
        if (attrs instanceof List<?> l) for (Object a : l) {
            String s = priv(screen, "formatAttribute", a);
            int i = s.lastIndexOf(' ');
            String name = i > 0 ? s.substring(0, i) : s, val = i > 0 ? s.substring(i + 1) : "";
            double amt = Math.abs(((Number) call(a, "getAmount")).doubleValue());
            double max = Math.max(1e-9, mx.getOrDefault(str(call(a, "getAttributeId")), amt));
            double r = Math.max(0.06, Math.min(1.0, amt / max));
            cAttr.add(new String[] {name, val, String.valueOf(r)});
        }
        Object kit = call(def, "getStarterKit");
        if (kit instanceof List<?> kl) for (Object e : kl) {
            ItemStack st = ItemStack.EMPTY;
            try { st = (ItemStack) call(e, "createStack"); } catch (Throwable ignored) {}
            cStacks.add(st == null ? ItemStack.EMPTY : st);
            cKit.add(str(call(e, "getCount")) + "× " + priv(screen, "getItemName", e));
        }
    }

    // ---------- drawing ----------
    private static void panel(GuiGraphics g, int x, int y, int w, int h, String title, Font f) {
        Skin.nine(g, Skin.PANEL, x, y, w, h, 8, 32);
        g.drawString(f, title, x + 12, y + 8, HEAD, false);
        g.fill(x + 10, y + 19, x + w - 10, y + 20, 0x88E8B028);
    }

    private static int btnH(Screen s, List<?> cards) {
        for (Object c : s.children()) if (c instanceof AbstractWidget a && !cards.contains(c)) return a.getHeight();
        return 22;
    }

    public static void draw(Screen s, Object screen, List<?> cards, int sel, GuiGraphics g, int mx, int my) {
        Font f = Minecraft.getInstance().font;
        int w = s.width, h = s.height;
        Lay l = lay(w, h, btnH(s, cards));
        Skin.background(g, w, h);

        // header
        Skin.nine(g, Skin.CARD, l.pad, l.pad, w - 2 * l.pad, 38, 6, 32);
        g.drawCenteredString(f, Component.literal("").append(s.getTitle()).withStyle(st -> st.withBold(true)), w / 2, l.pad + 9, HEAD);
        g.drawCenteredString(f, Component.translatable("classmod.gui.navigation_hint"), w / 2, l.pad + 23, TEXT);

        if (cards.isEmpty()) {
            g.drawCenteredString(f, Component.translatable("classmod.gui.no_classes"), w / 2, h / 2, 0xFFFF6060);
        } else {
            Object def = call(cards.get(Math.max(0, sel)), "getClassDef");
            if (def != null) { cache(screen, def); drawLeft(g, f, l, def); drawCentre(g, f, l, def, cards.size(), sel, mx, my); drawRight(g, f, l, mx, my); }
        }
        for (Object c : s.children())
            if (c instanceof net.minecraft.client.gui.components.Renderable r && !cards.contains(c)) r.render(g, mx, my, 0f);
    }

    private static void drawLeft(GuiGraphics g, Font f, Lay l, Object def) {
        int half = l.h * 52 / 100;
        panel(g, l.lx, l.top, l.lw, half, "Equipment", f);
        int y = l.top + 26, maxY = l.top + half - 8;
        int rows = Math.max(1, (maxY - y) / 18);
        kitScroll = Math.max(0, Math.min(kitScroll, cKit.size() - rows));
        if (cKit.isEmpty()) g.drawString(f, "—", l.lx + 14, y, DIM, false);
        for (int i = 0; i < rows && i + kitScroll < cKit.size(); i++) {
            int yy = y + i * 18;
            g.fill(l.lx + 12, yy, l.lx + 30, yy + 18, 0x66000000);
            g.renderItem(cStacks.get(i + kitScroll), l.lx + 13, yy + 1);
            g.drawString(f, f.plainSubstrByWidth(cKit.get(i + kitScroll), l.lw - 44), l.lx + 34, yy + 5, TEXT, false);
        }
        int py = l.top + half + 6, ph = l.bottom - py;
        panel(g, l.lx, py, l.lw, ph, "Special abilities", f);
        String pass = str(call(def, "getPassiveDescription"));
        if (pass.isBlank()) { g.drawString(f, "—", l.lx + 14, py + 26, DIM, false); return; }
        List<FormattedCharSequence> lines = f.split(FormattedText.of(pass), l.lw - 34);
        int vis = Math.max(1, (ph - 32) / 10);
        passScroll = Math.max(0, Math.min(passScroll, lines.size() - vis));
        for (int i = 0; i < vis && i + passScroll < lines.size(); i++) g.drawString(f, lines.get(i + passScroll), l.lx + 14, py + 26 + i * 10, TEXT, false);
        if (lines.size() > vis) scrollBar(g, l.lx + l.lw - 13, py + 24, vis * 10, vis, lines.size(), passScroll);
    }

    private static void scrollBar(GuiGraphics g, int x, int y, int h, int vis, int total, int pos) {
        g.fill(x - 1, y - 1, x + 4, y + h + 1, 0xAA0A0614);
        g.fill(x, y, x + 3, y + h, 0xFF2A1F4A);
        int th = Math.max(12, h * vis / total);
        int ty = y + (h - th) * pos / Math.max(1, total - vis);
        g.fill(x, ty, x + 3, ty + th, Skin.GOLD);
        g.fill(x, ty, x + 3, ty + 1, 0xFFFFE9A8);
    }

    private static void drawCentre(GuiGraphics g, Font f, Lay l, Object def, int n, int sel, int mx, int my) {
        Skin.nine(g, Skin.PANEL, l.cx, l.top, l.cw, l.h, 8, 32);
        String name = str(call(def, "getDisplayName"));
        g.drawCenteredString(f, Component.literal(name).withStyle(st -> st.withBold(true)), l.cx + l.cw / 2, l.top + 9, HEAD);
        String tags = str(call(def, "getTagsString"));
        if (!tags.isBlank()) g.drawCenteredString(f, Component.literal(f.plainSubstrByWidth(tags, l.cw - 20)), l.cx + l.cw / 2, l.top + 20, DIM);
        g.fill(l.cx + 10, l.top + 29, l.cx + l.cw - 10, l.top + 30, 0x88E8B028);

        // model + pedestal
        int baseY = l.modelBottom - 6;
        int pw = Math.min(l.cw - 40, 90);
        int pcx = l.cx + l.cw / 2;
        g.fill(pcx - pw / 2, baseY, pcx + pw / 2, baseY + 3, 0xFF824F10);
        g.fill(pcx - pw / 2 + 4, baseY + 3, pcx + pw / 2 - 4, baseY + 5, 0xFF46386C);
        g.fill(pcx - pw / 2, baseY, pcx + pw / 2, baseY + 1, Skin.GOLD);
        var player = Minecraft.getInstance().player;
        if (player != null) {
            int scale = Math.max(20, (baseY - l.modelTop) * 10 / 21);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, pcx, baseY, scale, (float) (pcx - mx), (float) (baseY - scale * 1.5f - my), player);
        }

        // arrows + counter + dots
        int ay = l.navY;
        arrow(g, f, l.cx + 14, ay, true, mx, my);
        arrow(g, f, l.cx + l.cw - 14 - 22, ay, false, mx, my);
        g.drawCenteredString(f, Component.literal((sel + 1) + " / " + n), pcx, ay + 3, TEXT);
        if (n <= 12) {
            int dx = pcx - n * 8 / 2 + 1;
            for (int i = 0; i < n; i++) {
                int x = dx + i * 8;
                g.fill(x, ay + 20, x + 6, ay + 23, i == sel ? Skin.GOLD : 0xFF46386C);
            }
        }

        // description
        int dy = l.descTop;
        g.fill(l.cx + 10, dy - 3, l.cx + l.cw - 10, dy - 2, 0x66E8B028);
        List<FormattedCharSequence> lines = f.split(FormattedText.of(str(call(def, "getDescription"))), l.cw - 36);
        int vis = Math.max(1, (l.descBottom - dy) / 10);
        descScroll = Math.max(0, Math.min(descScroll, lines.size() - vis));
        for (int i = 0; i < vis && i + descScroll < lines.size(); i++) g.drawString(f, lines.get(i + descScroll), l.cx + 14, dy + i * 10, TEXT, false);
        if (lines.size() > vis) scrollBar(g, l.cx + l.cw - 13, dy, vis * 10, vis, lines.size(), descScroll);
    }

    private static void arrow(GuiGraphics g, Font f, int x, int y, boolean left, int mx, int my) {
        boolean over = mx >= x && mx < x + 22 && my >= y && my < y + 14;
        Skin.button(g, x, y, 22, 14, over, true, over ? 1f : 0f);
        g.drawCenteredString(f, Component.literal(left ? "<" : ">"), x + 11, y + 3, over ? HEAD : TEXT);
    }

    private static void drawRight(GuiGraphics g, Font f, Lay l, int mx, int my) {
        panel(g, l.rx, l.top, l.rw, l.h, "Attributes", f);
        int y0 = l.top + 28, rowH = 18;
        int vis = Math.max(1, (l.bottom - 8 - y0) / rowH);
        attrScroll = Math.max(0, Math.min(attrScroll, cAttr.size() - vis));
        if (cAttr.isEmpty()) g.drawString(f, "—", l.rx + 14, y0, DIM, false);
        boolean sb = cAttr.size() > vis;
        int bx = l.rx + 12, bw = l.rw - 24 - (sb ? 8 : 0);
        for (int i = 0; i < vis && i + attrScroll < cAttr.size(); i++) {
            String[] a = cAttr.get(i + attrScroll);
            int y = y0 + i * rowH;
            if (((i + attrScroll) & 1) == 0) g.fill(bx - 2, y - 3, bx + bw + 2, y + rowH - 3, 0x33000000);
            String val = a[1];
            int col = val.startsWith("-") ? 0xFFFF8A80 : val.startsWith("+") ? 0xFF9BE58F : HEAD;
            g.drawString(f, f.plainSubstrByWidth(a[0], bw - f.width(val) - 10), bx + 2, y + 1, TEXT, false);
            g.drawString(f, val, bx + bw - f.width(val) - 2, y + 1, col, false);
        }
        if (sb) scrollBar(g, l.rx + l.rw - 13, y0 - 3, vis * rowH, vis, cAttr.size(), attrScroll);
    }

    // ---------- input ----------
    /** Returns new selected index, or -1 if the click was not on an arrow/dot. */
    public static int click(Screen s, List<?> cards, int sel, double mx, double my) {
        if (cards.isEmpty()) return -1;
        Lay l = lay(s.width, s.height, btnH(s, cards));
        int n = cards.size(), ay = l.navY;
        if (my >= ay && my < ay + 14) {
            if (mx >= l.cx + 14 && mx < l.cx + 36) return sel <= 0 ? n - 1 : sel - 1;
            if (mx >= l.cx + l.cw - 36 && mx < l.cx + l.cw - 14) return sel >= n - 1 ? 0 : sel + 1;
        }
        if (n <= 12 && my >= ay + 17 && my < ay + 26) {
            int pcx = l.cx + l.cw / 2, dx = pcx - n * 8 / 2 + 1;
            int i = (int) Math.floor((mx - dx) / 8.0);
            if (i >= 0 && i < n && mx - dx - i * 8 < 7) return i;
        }
        return -1;
    }

    public static boolean scroll(Screen s, List<?> cards, double mx, double my, double delta) {
        Lay l = lay(s.width, s.height, btnH(s, cards));
        int d = -(int) Math.signum(delta);
        if (mx >= l.rx && mx < l.rx + l.rw && my >= l.top && my < l.bottom) { attrScroll = Math.max(0, attrScroll + d); return true; }
        if (mx >= l.cx && mx < l.cx + l.cw && my >= l.descTop - 4 && my < l.bottom) { descScroll = Math.max(0, descScroll + d); return true; }
        if (mx >= l.lx && mx < l.lx + l.lw && my >= l.top && my < l.bottom) {
            if (my < l.top + l.h * 52 / 100) kitScroll = Math.max(0, kitScroll + d); else passScroll = Math.max(0, passScroll + d);
            return true;
        }
        return false;
    }

    /** Park the (now unused) card widgets off-screen; they still exist so the mod's own logic keeps working. */
    public static void park(List<?> cards) {
        for (Object o : cards) if (o instanceof AbstractWidget a) { a.setX(-3000); a.setY(-3000); }
    }
}
