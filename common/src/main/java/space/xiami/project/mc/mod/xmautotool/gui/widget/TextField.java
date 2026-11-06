package space.xiami.project.mc.mod.xmautotool.gui.widget;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.api.Hosts;

import java.util.function.Consumer;

/**
 * Editable single-line text field. Implements the subset of vanilla EditBox
 * behaviour the mod used: value/responder/max length, focus, cursor, selection,
 * backspace/delete, arrows, home/end and the usual Ctrl+A/C/V/X.
 */
public final class TextField extends Widget {

    private static final int KEY_BACKSPACE = 259;
    private static final int KEY_DELETE = 261;
    private static final int KEY_LEFT = 263;
    private static final int KEY_RIGHT = 262;
    private static final int KEY_HOME = 268;
    private static final int KEY_END = 269;
    private static final int KEY_A = 65;
    private static final int KEY_C = 67;
    private static final int KEY_V = 86;
    private static final int KEY_X = 88;

    private static final int MOD_SHIFT = 1;
    private static final int MOD_CONTROL = 2;

    private String value;
    private int maxLength = 32;
    private Consumer<String> responder = text -> {
    };
    private int cursor;
    private int anchor = -1;
    private int scroll;
    private boolean focused;

    public TextField(int x, int y, int width, int height, String initial) {
        super(x, y, width, height);
        this.value = initial == null ? "" : initial;
        this.cursor = value.length();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String text) {
        String next = text == null ? "" : text;
        if (next.length() > maxLength) {
            next = next.substring(0, maxLength);
        }
        boolean changed = !next.equals(value);
        value = next;
        cursor = value.length();
        anchor = -1;
        scroll = 0;
        if (changed) {
            responder.accept(value);
        }
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = Math.max(1, maxLength);
        if (value.length() > this.maxLength) {
            value = value.substring(0, this.maxLength);
            cursor = Math.min(cursor, value.length());
            responder.accept(value);
        }
    }

    public void setResponder(Consumer<String> responder) {
        this.responder = responder == null ? text -> {
        } : responder;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public void onFocusGained() {
        focused = true;
    }

    @Override
    public void onFocusLost() {
        focused = false;
        anchor = -1;
    }

    @Override
    public void render(Canvas canvas, int mouseX, int mouseY) {
        super.render(canvas, mouseX, mouseY);
        int border = focused ? 0xFFFFFFFF : 0xFFA0A0A0;
        canvas.fill(x, y, x + width, y + height, 0xFF000000);
        canvas.fill(x, y, x + width, y + 1, border);
        canvas.fill(x, y + height - 1, x + width, y + height, border);
        canvas.fill(x, y, x + 1, y + height, border);
        canvas.fill(x + width - 1, y, x + width, y + height, border);

        int inner = Math.max(8, width - 8);
        int cursorX = canvas.textWidth(value.substring(0, cursor));
        int maxScroll = Math.max(0, canvas.textWidth(value) - inner);
        if (cursorX - scroll > inner - 2) {
            scroll = cursorX - inner + 2;
        }
        if (cursorX - scroll < 0) {
            scroll = cursorX;
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        int start = 0;
        while (start < value.length() && canvas.textWidth(value.substring(0, start + 1)) <= scroll) {
            start++;
        }
        int startOffset = canvas.textWidth(value.substring(0, start)) - scroll;
        int textX = x + 4 + startOffset;

        int end = start;
        while (end < value.length()
                && canvas.textWidth(value.substring(start, end + 1)) + startOffset <= inner) {
            end++;
        }

        if (anchor >= 0 && anchor != cursor) {
            int a = Math.min(anchor, cursor);
            int b = Math.max(anchor, cursor);
            int x1 = textX + canvas.textWidth(value.substring(start, Math.max(start, a)));
            int x2 = textX + canvas.textWidth(value.substring(start, Math.max(start, b)));
            x1 = Math.max(x + 2, Math.min(x + width - 2, x1));
            x2 = Math.max(x + 2, Math.min(x + width - 2, x2));
            if (x2 > x1) {
                canvas.fill(x1, y + 2, x2, y + height - 2, 0xFF3A5A8A);
            }
        }

        if (end > start) {
            canvas.text(value.substring(start, end), textX, y + (height - 8) / 2, 0xFFE0E0E0);
        }

        long now = System.currentTimeMillis();
        if (focused && (now / 500) % 2 == 0) {
            int cx = textX + canvas.textWidth(value.substring(start, cursor));
            cx = Math.max(x + 2, Math.min(x + width - 3, cx));
            canvas.fill(cx, y + 2, cx + 1, y + height - 2, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY, int button) {
        if (!active || !contains(mouseX, mouseY)) {
            return false;
        }
        int target = (int) (mouseX - (x + 4)) + scroll;
        int best = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i <= value.length(); i++) {
            int distance = Math.abs(canvas.textWidth(value.substring(0, i)) - target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        cursor = best;
        anchor = -1;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) {
            return false;
        }
        boolean shift = (modifiers & MOD_SHIFT) != 0;
        boolean control = (modifiers & MOD_CONTROL) != 0;
        switch (keyCode) {
            case KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursor > 0) {
                    value = value.substring(0, cursor - 1) + value.substring(cursor);
                    cursor--;
                    changed();
                }
                return true;
            }
            case KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursor < value.length()) {
                    value = value.substring(0, cursor) + value.substring(cursor + 1);
                    changed();
                }
                return true;
            }
            case KEY_LEFT -> {
                moveTo(cursor - 1, shift);
                return true;
            }
            case KEY_RIGHT -> {
                moveTo(cursor + 1, shift);
                return true;
            }
            case KEY_HOME -> {
                moveTo(0, shift);
                return true;
            }
            case KEY_END -> {
                moveTo(value.length(), shift);
                return true;
            }
            case KEY_A -> {
                if (control) {
                    anchor = 0;
                    cursor = value.length();
                    return true;
                }
                return false;
            }
            case KEY_C -> {
                if (control && hasSelection()) {
                    Hosts.host().setClipboard(selectedText());
                    return true;
                }
                return false;
            }
            case KEY_V -> {
                if (control) {
                    insert(Hosts.host().clipboard());
                    return true;
                }
                return false;
            }
            case KEY_X -> {
                if (control && hasSelection()) {
                    Hosts.host().setClipboard(selectedText());
                    deleteSelection();
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!focused || c < ' ' || c == 127) {
            return false;
        }
        insert(String.valueOf(c));
        return true;
    }

    private void moveTo(int position, boolean shift) {
        if (shift) {
            if (anchor < 0) {
                anchor = cursor;
            }
        } else {
            anchor = -1;
        }
        cursor = Math.max(0, Math.min(value.length(), position));
    }

    private boolean hasSelection() {
        return anchor >= 0 && anchor != cursor;
    }

    private String selectedText() {
        int start = Math.min(anchor, cursor);
        int end = Math.max(anchor, cursor);
        return value.substring(start, end);
    }

    private void deleteSelection() {
        int start = Math.min(anchor, cursor);
        int end = Math.max(anchor, cursor);
        value = value.substring(0, start) + value.substring(end);
        cursor = start;
        anchor = -1;
        changed();
    }

    private void insert(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        StringBuilder clean = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= ' ' && c != 127) {
                clean.append(c);
            }
        }
        if (clean.isEmpty()) {
            return;
        }
        if (hasSelection()) {
            int start = Math.min(anchor, cursor);
            int end = Math.max(anchor, cursor);
            value = value.substring(0, start) + value.substring(end);
            cursor = start;
            anchor = -1;
        }
        int room = maxLength - value.length();
        if (room <= 0) {
            return;
        }
        String insert = clean.length() > room ? clean.substring(0, room) : clean.toString();
        value = value.substring(0, cursor) + insert + value.substring(cursor);
        cursor += insert.length();
        anchor = -1;
        changed();
    }

    private void changed() {
        responder.accept(value);
    }
}
