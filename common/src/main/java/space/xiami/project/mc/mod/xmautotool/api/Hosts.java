package space.xiami.project.mc.mod.xmautotool.api;

import java.util.Objects;

/** Static holder for the adapter-provided {@link Host}. */
public final class Hosts {

    private static volatile Host host;

    private Hosts() {
    }

    public static void install(Host value) {
        host = Objects.requireNonNull(value, "host");
    }

    public static Host host() {
        Host current = host;
        if (current == null) {
            throw new IllegalStateException("No adapter installed yet; the mod entry point must install a Host");
        }
        return current;
    }

    public static boolean ready() {
        return host != null;
    }
}
