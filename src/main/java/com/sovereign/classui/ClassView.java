package com.sovereign.classui;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/**
 * Bigger class details panel: description on the left, attributes + starter kit in a
 * multi-column scrollable grid on the right. Talks to the class mod through reflection only.
 */
public final class ClassView {
    private ClassView() {}

    public static final int TEXT = 0xFFEFE6D2;
    public static final int HEAD = 0xFFFFD36A;

    private static int scrollRow = 0;
    private static Object lastDef = null;
    private static int rightX, rightY, rightW, rightH, rows, visRows;

    private static Object call(Object o, String name) {
        try {
            Method m = o.getClass().getMethod(name);
            return m.invoke(o);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String callPrivate(Object screen, String name, Object arg) {
        try {
            for (Method m : screen.getClass().getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 1) {
                    m.setAccessible(true);
                    return String.valueOf(m.invoke(screen, arg));
                }
            }
        } catch (Throwable ignored) {}
        return "?";
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    // cache
    private static Object cacheDef; private static int cacheCols; private static List<String[]> cacheRows = new ArrayList<>();

    private static List<String[]> rows(Object screen, Object def, int cols) {
        if (def == cacheDef && cols == cacheCols) return cacheRows;
        List<String[]> out = new ArrayList<>();
        Font font = Minecraft.getInstance().font;
        Object attrs = call(def, "getAttributes");
        List<String> cells = new ArrayList<>();
        if (attrs instanceof List<?> l) for (Object a : l) cells.add("• " + callPrivate(screen, "formatAttribute", a));
        out.add(new String[] {"#Attributes"});
        if (cells.isEmpty()) out.add(new String[] {"—"});
        for (int i = 0; i < cells.size(); i += cols) {
            String[] r = new String[Math.min(cols, cells.size() - i)];
            for (int k = 0; k < r.length; k++) r[k] = cells.get(i + k);
            out.add(r);
        }
        Object kit = call(def, "getStarterKit");
        if (kit instanceof List<?> kl && !kl.isEmpty()) {
            out.add(new String[] {"#Starter kit"});
            List<String> kc = new ArrayList<>();
            for (Object e : kl) {
                Object cnt = call(e, "getCount");
                kc.add("• " + str(cnt) + " × " + callPrivate(screen, "getItemName", e));
            }
            for (int i = 0; i < kc.size(); i += cols) {
                String[] r = new String[Math.min(cols, kc.size() - i)];
                for (int k = 0; k < r.length; k++) r[k] = kc.get(i + k);
                out.add(r);
            }
        }
        cacheDef = def; cacheCols = cols; cacheRows = out;
        return out;
    }

    public static void draw(Object screen, List<?> cards, int sel, int dx, int dy, int dw, int dh, GuiGraphics g) {
        if (sel < 0 || sel >= cards.size()) return;
        Font font = Minecraft.getInstance().font;
        Object def = call(cards.get(sel), "getClassDef");
        if (def == null) return;
        if (def != lastDef) { lastDef = def; scrollRow = 0; }

        Skin.nine(g, Skin.PANEL, dx, dy, dw, dh, 8, 32);
        int x = dx + 14, tw = dw - 28;
        g.drawString(font, Component.literal(str(call(def, "getDisplayName"))).withStyle(s -> s.withBold(true)), x, dy + 9, HEAD, false);
        String tags = str(call(def, "getTagsString"));
        if (!tags.isBlank()) g.drawString(font, tags, x + tw - font.width(tags), dy + 9, 0xFFCDBBF0, false);
        g.fill(x, dy + 21, x + tw, dy + 22, 0xAAE8B028);

        int cy = dy + 27, ch = dh - 27 - 9;
        boolean wide = dw >= 460;
        int leftW = wide ? tw * 2 / 5 : tw;
        // description
        g.drawString(font, "Description", x, cy, HEAD, false);
        int ly = cy + 12;
        int maxLines = Math.max(1, (ch - 12) / 10);
        List<FormattedCharSequence> lines = font.split(FormattedText.of(str(call(def, "getDescription"))), Math.max(30, leftW));
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            g.drawString(font, lines.get(i), x, ly + i * 10, TEXT, false);
        }
        if (lines.size() > maxLines) g.drawString(font, "\u2026", x + leftW - 8, ly + (maxLines - 1) * 10, HEAD, false);
        if (!wide) return;

        int gapX = 14;
        rightX = x + leftW + gapX; rightW = x + tw - rightX; rightY = cy; rightH = ch;
        g.fill(rightX - gapX / 2, cy, rightX - gapX / 2 + 1, cy + ch, 0x66E8B028);
        int cols = Math.max(1, Math.min(3, rightW / 150));
        int colW = rightW / cols;
        List<String[]> rs = rows(screen, def, cols);
        rows = rs.size();
        visRows = Math.max(1, ch / 10);
        scrollRow = Math.max(0, Math.min(scrollRow, rows - visRows));
        g.enableScissor(rightX, rightY, rightX + rightW, rightY + rightH);
        for (int i = 0; i < visRows && i + scrollRow < rows; i++) {
            String[] r = rs.get(i + scrollRow);
            int y = cy + i * 10;
            if (r.length == 1 && r[0].startsWith("#")) {
                g.drawString(font, r[0].substring(1), rightX, y, HEAD, false);
            } else {
                for (int k = 0; k < r.length; k++) {
                    g.drawString(font, font.plainSubstrByWidth(r[k], colW - 6), rightX + k * colW, y, TEXT, false);
                }
            }
        }
        g.disableScissor();
        if (rows > visRows) {
            int bx = rightX + rightW + 3;
            g.fill(bx, cy, bx + 2, cy + ch, 0x55000000);
            int th = Math.max(8, ch * visRows / rows);
            int ty = cy + (ch - th) * scrollRow / Math.max(1, rows - visRows);
            g.fill(bx, ty, bx + 2, ty + th, Skin.GOLD);
        }
    }

    /** Wheel over the attributes area scrolls it. Returns true if consumed. */
    public static boolean scroll(double mx, double my, double delta, int dx, int dy, int dw, int dh) {
        if (mx < dx || mx > dx + dw || my < dy || my > dy + dh || rows <= visRows) return false;
        scrollRow = Math.max(0, Math.min(rows - visRows, scrollRow - (int) Math.signum(delta)));
        return true;
    }
}
