package space.xiami.project.mc.mod.xmautotool.core;

/** Three-state override used for per-policy correctness gating. */
public enum Tri {
    INHERIT,
    TRUE,
    FALSE;

    public boolean resolve(boolean fallback) {
        return switch (this) {
            case INHERIT -> fallback;
            case TRUE -> true;
            case FALSE -> false;
        };
    }
}
