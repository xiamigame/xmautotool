package space.xiami.project.mc.mod.xmautotool.api;

import java.util.Objects;

/** Static holder for the adapter-provided {@link Game}. */
public final class Games {

    private static volatile Game game;

    private Games() {
    }

    public static void install(Game value) {
        game = Objects.requireNonNull(value, "game");
    }

    public static Game game() {
        Game current = game;
        if (current == null) {
            throw new IllegalStateException("No adapter installed yet; the mod entry point must install a Game");
        }
        return current;
    }

    public static boolean ready() {
        return game != null;
    }
}
