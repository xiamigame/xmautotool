package space.xiami.project.mc.mod.xmautotool.gui.widget;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;

/** Base widget: bounds, hover/hit-testing and input hooks. */
public abstract class Widget {

    public int x;
    public int y;
    public int width;
    public int height;
    public boolean active = true;
    public boolean visible = true;

    protected boolean hovered;

    protected Widget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(Canvas canvas, int mouseX, int mouseY) {
        hovered = active && contains(mouseX, mouseY);
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(Canvas canvas, double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(Canvas canvas, double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char c, int modifiers) {
        return false;
    }

    public boolean focusable() {
        return false;
    }

    public void onFocusGained() {
    }

    public void onFocusLost() {
    }
}
