package space.xiami.project.mc.mod.xmautotool.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Built-in tool kinds and the inheritance expansion used for capability
 * matching. Pure data so the core stays testable without Minecraft.
 */
public final class ToolKindRegistry {

    public static final String PICKAXE = "xmautotool:pickaxe";
    public static final String AXE = "xmautotool:axe";
    public static final String SHOVEL = "xmautotool:shovel";
    public static final String HOE = "xmautotool:hoe";
    public static final String SWORD = "xmautotool:sword";
    public static final String SHEARS = "xmautotool:shears";

    private static final Map<String, List<String>> PARENTS = new LinkedHashMap<>();

    static {
        PARENTS.put(PICKAXE, List.of());
        PARENTS.put(AXE, List.of());
        PARENTS.put(SHOVEL, List.of());
        PARENTS.put(HOE, List.of());
        PARENTS.put(SWORD, List.of());
        PARENTS.put(SHEARS, List.of());
    }

    private ToolKindRegistry() {
    }

    public static Map<String, List<String>> builtins() {
        return Map.copyOf(PARENTS);
    }

    public static boolean isBuiltin(String kind) {
        return PARENTS.containsKey(kind);
    }

    /**
     * Expands each kind with all of its ancestors (breadth-first, cycle-safe).
     */
    public static List<String> expand(Collection<String> kinds, Map<String, List<String>> parents) {
        Set<String> result = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(kinds);
        while (!queue.isEmpty()) {
            String kind = queue.removeFirst();
            if (kind == null || !result.add(kind)) {
                continue;
            }
            List<String> direct = parents.get(kind);
            if (direct != null) {
                queue.addAll(direct);
            }
        }
        return new ArrayList<>(result);
    }

    public static List<String> expand(Collection<String> kinds) {
        return expand(kinds, PARENTS);
    }

    /** True when {@code kind} equals {@code ancestor} or inherits from it. */
    public static boolean isA(String kind, String ancestor, Map<String, List<String>> parents) {
        return expand(List.of(kind), parents).contains(ancestor);
    }
}
