package space.xiami.project.mc.mod.xmautotool.gui.widget;

import space.xiami.project.mc.mod.xmautotool.config.PolicyRuntime;
import space.xiami.project.mc.mod.xmautotool.core.ToolKindRegistry;

import java.util.ArrayList;
import java.util.List;

/** Display names and a stable ordering for tool kinds, shared by the editors. */
public final class Kinds {

    /** Built-ins in a fixed display order (map iteration order is not guaranteed). */
    private static final List<String> BUILTIN_ORDER = List.of(
            ToolKindRegistry.PICKAXE,
            ToolKindRegistry.AXE,
            ToolKindRegistry.SHOVEL,
            ToolKindRegistry.HOE,
            ToolKindRegistry.SWORD,
            ToolKindRegistry.SHEARS);

    private Kinds() {
    }

    public static String path(String kind) {
        int colon = kind.indexOf(':');
        return colon >= 0 ? kind.substring(colon + 1) : kind;
    }

    public static String shortName(String kind) {
        return switch (path(kind)) {
            case "pickaxe" -> "镐";
            case "axe" -> "斧";
            case "shovel" -> "锹";
            case "hoe" -> "锄";
            case "sword" -> "剑";
            case "shears" -> "剪刀";
            case "hammer" -> "锤";
            case "paxel" -> "镐斧锹";
            default -> path(kind);
        };
    }

    public static String label(String kind) {
        return shortName(kind) + " (" + path(kind) + ")";
    }

    /** Built-ins first in fixed order, then user-defined kinds sorted by id. */
    public static List<String> ordered() {
        List<String> result = new ArrayList<>(BUILTIN_ORDER);
        PolicyRuntime.kindParents().keySet().stream()
                .filter(kind -> !result.contains(kind))
                .sorted()
                .forEach(result::add);
        return result;
    }
}
