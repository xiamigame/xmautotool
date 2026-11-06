package space.xiami.project.mc.mod.xmautotool.gui.widget;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Prefix suggestions for a {@link TextField}, rendered as a small drop-down. */
public final class Autocomplete {

    private static final int ROW_HEIGHT = 10;
    private static final int MAX_ROWS = 6;

    private final TextField field;
    private final int x;
    private final int y;
    private final int width;
    private final Supplier<List<String>> source;
    private final List<String> matches = new ArrayList<>();
    private boolean open;

    public Autocomplete(TextField field, int x, int y, int width, Supplier<List<String>> source) {
        this(field, x, y, width, source, () -> {
        });
    }

    public Autocomplete(TextField field, int x, int y, int width, Supplier<List<String>> source, Runnable onChanged) {
        this.field = field;
        this.x = x;
        this.y = y;
        this.width = width;
        this.source = source;
        field.setResponder(text -> {
            refresh();
            onChanged.run();
        });
    }

    private void refresh() {
        matches.clear();
        open = false;
        String text = field.getValue().trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.length() < 2) {
            return;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String candidate : source.get()) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(candidate);
                if (matches.size() >= MAX_ROWS) {
                    break;
                }
            }
        }
        open = !matches.isEmpty() && !(matches.size() == 1 && matches.get(0).equalsIgnoreCase(text));
    }

    public void render(Canvas canvas) {
        if (!open || matches.isEmpty()) {
            return;
        }
        int dropY = y + 18;
        int dropWidth = Math.max(width, 90);
        canvas.fill(x - 1, dropY - 1, x + dropWidth + 1, dropY + matches.size() * ROW_HEIGHT + 1, 0xFF000000);
        for (int i = 0; i < matches.size(); i++) {
            int rowY = dropY + i * ROW_HEIGHT;
            canvas.text(matches.get(i), x + 2, rowY + 1, 0xE0E0E0);
        }
    }

    /** @return true when the click was consumed by the drop-down. */
    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY) {
        if (!open || matches.isEmpty()) {
            return false;
        }
        int dropY = y + 18;
        int dropWidth = Math.max(width, 90);
        if (mouseX < x || mouseX > x + dropWidth || mouseY < dropY || mouseY > dropY + matches.size() * ROW_HEIGHT) {
            return false;
        }
        int index = (int) ((mouseY - dropY) / ROW_HEIGHT);
        index = Math.max(0, Math.min(matches.size() - 1, index));
        field.setValue(matches.get(index));
        open = false;
        return true;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
    }
}
