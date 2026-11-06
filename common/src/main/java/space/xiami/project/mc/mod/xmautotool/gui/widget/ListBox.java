package space.xiami.project.mc.mod.xmautotool.gui.widget;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;

/**
 * Small scrollable list with fixed row height. Rendering and hit testing only;
 * the owner draws the row content (icon + text).
 */
public final class ListBox {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int rowHeight;
    private int scroll;
    private int selected = -1;

    public ListBox(int x, int y, int width, int height, int rowHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rowHeight = rowHeight;
    }

    public void render(Canvas canvas, int count, Renderer renderer) {
        canvas.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF303030);
        canvas.fill(x, y, x + width, y + height, 0xFF101010);

        int visible = visibleRows();
        int first = Math.min(scroll, Math.max(0, count - visible));
        for (int i = 0; i < visible && first + i < count; i++) {
            int index = first + i;
            int rowY = y + i * rowHeight;
            if (index == selected) {
                canvas.fill(x, rowY, x + width, rowY + rowHeight, 0xFF2A4A8A);
            }
            renderer.render(canvas, index, x + 2, rowY + 1, width - 4, index == selected);
        }

        if (count > visible) {
            int barX = x + width - 3;
            int barHeight = Math.max(8, height * visible / count);
            int barY = y + (height - barHeight) * Math.min(scroll, count - visible) / Math.max(1, count - visible);
            canvas.fill(barX, barY, barX + 2, barY + barHeight, 0xFF808080);
        } else if (count == 0) {
            canvas.text("（空）", x + 4, y + 4, 0x606060);
        }
    }

    public int indexAt(double mouseX, double mouseY, int count) {
        if (!contains(mouseX, mouseY)) {
            return -1;
        }
        int index = scroll + (int) ((mouseY - y) / rowHeight);
        return index >= 0 && index < count ? index : -1;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean scrollBy(double delta, int count) {
        int visible = visibleRows();
        int max = Math.max(0, count - visible);
        int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        if (next != scroll) {
            scroll = next;
            return true;
        }
        return false;
    }

    public void clamp(int count) {
        scroll = Math.max(0, Math.min(Math.max(0, count - visibleRows()), scroll));
        if (selected >= count) {
            selected = count - 1;
        }
    }

    public void ensureVisible(int index, int count) {
        if (index < 0) {
            return;
        }
        int visible = visibleRows();
        if (index < scroll) {
            scroll = index;
        }
        if (index >= scroll + visible) {
            scroll = index - visible + 1;
        }
        clamp(count);
    }

    public int scrollOffset() {
        return scroll;
    }

    public void setScrollOffset(int offset) {
        this.scroll = Math.max(0, offset);
    }

    public int visibleRows() {
        return Math.max(1, height / rowHeight);
    }

    public int selected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @FunctionalInterface
    public interface Renderer {
        void render(Canvas canvas, int index, int x, int y, int width, boolean selected);
    }
}
