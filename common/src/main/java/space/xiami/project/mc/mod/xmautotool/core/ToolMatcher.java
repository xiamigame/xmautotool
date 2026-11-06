package space.xiami.project.mc.mod.xmautotool.core;

import java.util.Locale;
import java.util.Map;

/**
 * Right-hand side of a policy: selects tools. Fields are ANDed together inside
 * one matcher; a policy's list of matchers is ORed.
 *
 * <p>{@code any} is the "just pick the best" escape hatch; a matcher's
 * {@code weight} is added to the score when it matches (highest weight wins
 * when several matchers match the same tool).</p>
 */
public record ToolMatcher(
        Integer slot,
        String nameContains,
        String fingerprint,
        String item,
        String type,
        String capability,
        Map<String, String> nbt,
        boolean any,
        int weight
) {
    public ToolMatcher {
        nbt = nbt == null ? Map.of() : Map.copyOf(nbt);
    }

    public static ToolMatcher anyTool() {
        return new ToolMatcher(null, null, null, null, null, null, Map.of(), true, 0);
    }

    public static ToolMatcher type(String type) {
        return new ToolMatcher(null, null, null, null, type, null, Map.of(), false, 0);
    }

    public static ToolMatcher item(String item) {
        return new ToolMatcher(null, null, null, item, null, null, Map.of(), false, 0);
    }

    public static ToolMatcher slot(int slot) {
        return new ToolMatcher(slot, null, null, null, null, null, Map.of(), false, 0);
    }

    public static ToolMatcher name(String name) {
        return new ToolMatcher(null, name, null, null, null, null, Map.of(), false, 0);
    }

    public static ToolMatcher capability(String capability) {
        return new ToolMatcher(null, null, null, null, null, capability, Map.of(), false, 0);
    }

    public boolean matches(ToolSnapshot tool) {
        if (any) {
            return true;
        }
        if (slot != null && slot != tool.slot()) {
            return false;
        }
        if (nameContains != null
                && !tool.name().toLowerCase(Locale.ROOT).contains(nameContains.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (fingerprint != null && !fingerprint.equals(tool.fingerprint())) {
            return false;
        }
        if (item != null && !item.equals(tool.itemId())) {
            return false;
        }
        if (type != null && !tool.matchesKind(type)) {
            return false;
        }
        if (capability != null && !tool.matchesCapability(capability)) {
            return false;
        }
        for (Map.Entry<String, String> entry : nbt.entrySet()) {
            if (!entry.getValue().equals(tool.nbt().get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /** A matcher that pins a concrete tool; required for {@link Action#FORCE}. */
    public boolean isSpecific() {
        return slot != null || nameContains != null || fingerprint != null || item != null;
    }

    public boolean isEmpty() {
        return !any && slot == null && nameContains == null && fingerprint == null
                && item == null && type == null && capability == null && nbt.isEmpty();
    }
}
