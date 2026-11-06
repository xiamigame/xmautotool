package space.xiami.project.mc.mod.xmautotool.gui.widget;

import space.xiami.project.mc.mod.xmautotool.api.Games;
import space.xiami.project.mc.mod.xmautotool.config.PolicyRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Cached suggestion sources for the editors. */
public final class Suggestions {

    private static List<String> blockIds;
    private static List<String> blockTags;
    private static List<String> itemIds;

    private Suggestions() {
    }

    public static List<String> blockIds() {
        if (blockIds == null) {
            blockIds = List.copyOf(Games.game().blockIds());
        }
        return blockIds;
    }

    public static List<String> blockTags() {
        if (blockTags == null) {
            blockTags = List.copyOf(Games.game().blockTags());
        }
        return blockTags;
    }

    public static List<String> itemIds() {
        if (itemIds == null) {
            itemIds = List.copyOf(Games.game().itemIds());
        }
        return itemIds;
    }

    public static List<String> kinds() {
        return new ArrayList<>(PolicyRuntime.kindParents().keySet());
    }

    public static List<String> mods() {
        return blockIds().stream()
                .map(id -> id.substring(0, id.indexOf(':')))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
