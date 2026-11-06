package space.xiami.project.mc.mod.xmautotool.api;

/**
 * Drawing primitives implemented by the adapter. All coordinates are GUI
 * pixels; colours are 0xAARRGGBB. The pure-JVM UI decides what to draw and
 * where, the adapter only knows how to put pixels on screen.
 */
public interface Canvas {

    /** Vanilla screen background (blurred/dirt depending on the game). */
    void background();

    void fill(int x1, int y1, int x2, int y2, int argb);

    int textWidth(String text);

    void text(String text, int x, int y, int argb);

    void textShadow(String text, int x, int y, int argb);

    /** Draws the item icon referenced by {@code icon}; EMPTY draws nothing. */
    void item(IconRef icon, int x, int y);
}
