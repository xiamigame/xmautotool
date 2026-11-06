package space.xiami.project.mc.mod.xmautotool.core;

import java.util.Map;
import java.util.Objects;

/**
 * Left-hand side of a policy: selects blocks. Fields are ANDed together inside
 * one matcher; a policy's list of matchers is ORed.
 */
public record BlockMatcher(
        String id,
        String tag,
        String mod,
        Map<String, String> states
) {
    public BlockMatcher {
        states = states == null ? Map.of() : Map.copyOf(states);
    }

    public static BlockMatcher id(String id) {
        return new BlockMatcher(id, null, null, Map.of());
    }

    public static BlockMatcher tag(String tag) {
        return new BlockMatcher(null, tag, null, Map.of());
    }

    public static BlockMatcher mod(String mod) {
        return new BlockMatcher(null, null, mod, Map.of());
    }

    public boolean matches(BlockSnapshot block) {
        if (id != null && !id.equals(block.id())) {
            return false;
        }
        if (tag != null && !block.tags().contains(tag)) {
            return false;
        }
        if (mod != null && !block.id().startsWith(mod + ":")) {
            return false;
        }
        for (Map.Entry<String, String> entry : states.entrySet()) {
            if (!Objects.equals(entry.getValue(), block.properties().get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return id == null && tag == null && mod == null && states.isEmpty();
    }
}
