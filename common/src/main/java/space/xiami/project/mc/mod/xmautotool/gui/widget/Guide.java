package space.xiami.project.mc.mod.xmautotool.gui.widget;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.config.ProfileManager;

import java.util.ArrayList;
import java.util.List;

/** Hover hints enabled by the {@code guide} profile setting. */
public final class Guide {

    private final List<Hint> hints = new ArrayList<>();

    public static boolean enabled() {
        return ProfileManager.initialized() && ProfileManager.settings().guide;
    }

    public void add(int x, int y, int width, int height, String text) {
        hints.add(new Hint(x, y, width, height, text));
    }

    public void render(Canvas canvas, int mouseX, int mouseY) {
        if (!enabled()) {
            return;
        }
        for (Hint hint : hints) {
            if (mouseX >= hint.x && mouseX <= hint.x + hint.width
                    && mouseY >= hint.y && mouseY <= hint.y + hint.height) {
                drawTooltip(canvas, hint.text, mouseX, mouseY);
                return;
            }
        }
    }

    private static void drawTooltip(Canvas canvas, String text, int mouseX, int mouseY) {
        int width = canvas.textWidth(text) + 8;
        int x = Math.min(mouseX + 8, 400);
        int y = Math.max(4, mouseY - 14);
        canvas.fill(x - 1, y - 1, x + width + 1, y + 12, 0xFF000000);
        canvas.fill(x, y, x + width, y + 11, 0xFF201010);
        canvas.text(text, x + 4, y + 2, 0xFFE0E0E0);
    }

    private record Hint(int x, int y, int width, int height, String text) {
    }
}
