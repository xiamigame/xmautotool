package space.xiami.project.mc.mod.xmautotool.api;

import java.util.Map;

/** A block the player is looking at or holding, as the UI sees it. */
public record BlockView(String id, Map<String, String> states) {

    public BlockView {
        states = Map.copyOf(states);
    }
}
