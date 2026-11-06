package space.xiami.project.mc.mod.xmautotool.core;

/** What a policy does with the blocks captured by its left-hand side. */
public enum Action {
    /** Only tools matching the right-hand side may be used. */
    USE,
    /** Never switch for these blocks. */
    NEVER,
    /** Switch to the first matching tool, ignoring speed and hysteresis. */
    FORCE
}
