package space.xiami.project.mc.mod.xmautotool.gui.widget;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;

import java.util.function.Consumer;

/**
 * Flat button with an optional item icon, optional text and an optional
 * selected highlight. Ported from the original IconButton; visuals unchanged.
 */
public class Button extends Widget {

    private final IconRef icon;
    private final String label;
    private final Consumer<Button> onPress;
    private boolean selected;

    public Button(int x, int y, int width, int height, String label, Consumer<Button> onPress) {
        this(x, y, width, height, IconRef.EMPTY, label, false, onPress);
    }

    public Button(int x, int y, int width, int height, IconRef icon, String label, Consumer<Button> onPress) {
        this(x, y, width, height, icon, label, false, onPress);
    }

    public Button(int x, int y, int width, int height, IconRef icon, String label,
                  boolean selected, Consumer<Button> onPress) {
        super(x, y, width, height);
        this.icon = icon == null ? IconRef.EMPTY : icon;
        this.label = label == null ? "" : label;
        this.selected = selected;
        this.onPress = onPress;
    }

    public Button selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    public void render(Canvas canvas, int mouseX, int mouseY) {
        super.render(canvas, mouseX, mouseY);
        int background = !active ? 0xFF2A2A2A : (hovered ? 0xFF6F8FB5 : 0xFF4A4A4A);
        canvas.fill(x, y, x + width, y + height, background);
        canvas.fill(x, y, x + width, y + 1, 0xFF1A1A1A);
        canvas.fill(x, y + height - 1, x + width, y + height, 0xFF1A1A1A);
        canvas.fill(x, y, x + 1, y + height, 0xFF1A1A1A);
        canvas.fill(x + width - 1, y, x + width, y + height, 0xFF1A1A1A);

        if (selected) {
            canvas.fill(x, y, x + width, y + 1, 0xFF7FB0FF);
            canvas.fill(x, y + height - 1, x + width, y + height, 0xFF7FB0FF);
            canvas.fill(x, y, x + 1, y + height, 0xFF7FB0FF);
            canvas.fill(x + width - 1, y, x + width, y + height, 0xFF7FB0FF);
        }

        int iconY = y + (height - 16) / 2;
        if (!icon.isEmpty()) {
            if (label.isEmpty()) {
                canvas.item(icon, x + (width - 16) / 2, iconY);
            } else {
                canvas.item(icon, x + 2, iconY);
                String text = fit(canvas, label, width - 22);
                canvas.textShadow(text, x + 20, y + (height - 8) / 2, 0xFFFFFF);
            }
        } else if (!label.isEmpty()) {
            String text = fit(canvas, label, width - 4);
            canvas.textShadow(text, x + (width - canvas.textWidth(text)) / 2, y + (height - 8) / 2, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY, int button) {
        if (active && contains(mouseX, mouseY)) {
            onPress.accept(this);
            return true;
        }
        return false;
    }

    private static String fit(Canvas canvas, String text, int maxWidth) {
        if (canvas.textWidth(text) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && canvas.textWidth(cut + "…") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }
}
