package space.xiami.project.mc.mod.xmautotool.api;

import java.util.Objects;

/**
 * Opaque reference to something the adapter can draw as an item icon.
 * The pure-JVM side never holds Minecraft item stacks.
 */
public final class IconRef {

    public enum Kind {
        EMPTY, ITEM, BLOCK, SLOT
    }

    public static final IconRef EMPTY = new IconRef(Kind.EMPTY, "", -1);

    private final Kind kind;
    private final String id;
    private final int slot;

    private IconRef(Kind kind, String id, int slot) {
        this.kind = kind;
        this.id = id;
        this.slot = slot;
    }

    public static IconRef item(String itemId) {
        return new IconRef(Kind.ITEM, Objects.requireNonNull(itemId, "itemId"), -1);
    }

    public static IconRef block(String blockId) {
        return new IconRef(Kind.BLOCK, Objects.requireNonNull(blockId, "blockId"), -1);
    }

    public static IconRef slot(int slot) {
        return new IconRef(Kind.SLOT, "", slot);
    }

    public Kind kind() {
        return kind;
    }

    public String id() {
        return id;
    }

    public int slot() {
        return slot;
    }

    public boolean isEmpty() {
        return kind == Kind.EMPTY;
    }
}
