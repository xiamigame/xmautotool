package space.xiami.project.mc.mod.xmautotool.api;

import java.util.List;

/** One item in the player's inventory, as the UI sees it. */
public record ItemView(IconRef icon, String itemId, String name, int count, List<String> kinds) {

    public ItemView {
        kinds = List.copyOf(kinds);
    }
}
