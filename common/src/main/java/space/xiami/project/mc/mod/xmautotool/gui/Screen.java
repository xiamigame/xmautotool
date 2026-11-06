package space.xiami.project.mc.mod.xmautotool.gui;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.api.Game;
import space.xiami.project.mc.mod.xmautotool.api.Games;
import space.xiami.project.mc.mod.xmautotool.api.Host;
import space.xiami.project.mc.mod.xmautotool.api.Hosts;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-JVM screen: layout, widget dispatch and lifecycle. The adapter wraps it
 * in a Minecraft screen and forwards render/input events.
 */
public abstract class Screen {

    private static final int KEY_ESCAPE = 256;

    protected final String titleKey;
    protected int width;
    protected int height;

    private final List<Widget> widgets = new ArrayList<>();
    private Widget focused;

    protected Screen(String titleKey) {
        this.titleKey = titleKey;
    }

    public String titleKey() {
        return titleKey;
    }

    public String title() {
        return host().translate(titleKey);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    protected Host host() {
        return Hosts.host();
    }

    protected Game game() {
        return Games.game();
    }

    /** Called by the adapter when the screen opens and on every resize. */
    public void init() {
        widgets.clear();
        focused = null;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        init();
    }

    public void addRenderableWidget(Widget widget) {
        widgets.add(widget);
    }

    public void clearWidgets() {
        widgets.clear();
        focused = null;
    }

    public void render(Canvas canvas, int mouseX, int mouseY, float partialTick) {
        for (Widget widget : widgets) {
            if (widget.visible) {
                widget.render(canvas, mouseX, mouseY);
            }
        }
    }

    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY, int button) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);
            if (widget.active && widget.visible && widget.mouseClicked(canvas, mouseX, mouseY, button)) {
                setFocused(widget.focusable() ? widget : null);
                return true;
            }
        }
        setFocused(null);
        return false;
    }

    public boolean mouseReleased(Canvas canvas, double mouseX, double mouseY, int button) {
        if (focused != null && focused.mouseReleased(canvas, mouseX, mouseY, button)) {
            return true;
        }
        for (Widget widget : widgets) {
            if (widget.active && widget.visible && widget.mouseReleased(canvas, mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(Canvas canvas, double mouseX, double mouseY, int button) {
        if (focused != null && focused.mouseDragged(canvas, mouseX, mouseY, button)) {
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(Canvas canvas, double mouseX, double mouseY, double delta) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focused != null && focused.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == KEY_ESCAPE) {
            onClose();
            return true;
        }
        return false;
    }

    public boolean charTyped(char c, int modifiers) {
        return focused != null && focused.charTyped(c, modifiers);
    }

    public void setFocused(Widget widget) {
        if (focused == widget) {
            return;
        }
        if (focused != null) {
            focused.onFocusLost();
        }
        focused = widget;
        if (widget != null) {
            widget.onFocusGained();
        }
    }

    public void removed() {
    }

    /** Saves state (if any) and navigates away; default closes to the game. */
    public void onClose() {
        host().openScreen(null);
    }

    public boolean isPauseScreen() {
        return false;
    }

    protected void drawString(Canvas canvas, String text, int x, int y, int color) {
        canvas.text(text, x, y, color);
    }

    protected void drawCentered(Canvas canvas, String text, int y, int color) {
        canvas.text(text, (width - canvas.textWidth(text)) / 2, y, color);
    }
}
