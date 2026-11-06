package space.xiami.project.mc.mod.xmautotool.api;

import java.util.List;

/**
 * Gameplay data and actions the pure-JVM side needs. Implemented by the
 * adapter; every method maps to one atomic Minecraft/Forge capability.
 */
public interface Game {

    // ------------------------------------------------------------- guards

    boolean levelReady();

    boolean hasPlayer();

    boolean screenOpen();

    boolean spectator();

    boolean usingItem();

    boolean sleeping();

    boolean attackDown();

    boolean destroying();

    boolean suppressDown();

    int selectedSlot();

    void switchSlot(int slot);

    // ------------------------------------------------------------- UI data

    /** Block under the crosshair, or {@code null} when not looking at one. */
    BlockView lookingBlock();

    /** Block formed by the item in the main hand, or {@code null}. */
    BlockView heldBlock();

    /** The item in the main hand, or {@code null}. */
    ItemView heldItem();

    /** Unique items of the player's inventory (main inventory + offhand). */
    List<ItemView> inventoryItems();

    List<String> blockIds();

    List<String> blockTags();

    List<String> itemIds();

    /** Kinds the engine infers for an item, ignoring user overrides. */
    List<String> baseKinds(String itemId);

    /** Display name for an icon; falls back to the raw id, or "" when empty. */
    String iconName(IconRef icon);
}
