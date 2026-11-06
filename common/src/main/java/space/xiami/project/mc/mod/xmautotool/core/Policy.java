package space.xiami.project.mc.mod.xmautotool.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One row of the policy table: "these blocks may use these tools".
 */
public record Policy(
        String id,
        String name,
        boolean enabled,
        List<BlockMatcher> when,
        Action action,
        List<ToolMatcher> tools,
        Tri requireCorrectTool
) {
    public Policy {
        when = when == null ? List.of() : List.copyOf(when);
        tools = tools == null ? List.of() : List.copyOf(tools);
        requireCorrectTool = requireCorrectTool == null ? Tri.INHERIT : requireCorrectTool;
        action = action == null ? Action.USE : action;
    }

    public boolean matches(BlockSnapshot block) {
        for (BlockMatcher matcher : when) {
            if (matcher.matches(block)) {
                return true;
            }
        }
        return false;
    }

    public boolean allows(ToolSnapshot tool) {
        for (ToolMatcher matcher : tools) {
            if (matcher.matches(tool)) {
                return true;
            }
        }
        return false;
    }

    public int weightOf(ToolSnapshot tool) {
        int best = 0;
        for (ToolMatcher matcher : tools) {
            if (matcher.matches(tool)) {
                best = Math.max(best, matcher.weight());
            }
        }
        return best;
    }

    /** Tag names referenced by this policy's block matchers. */
    public Set<String> tagsUsed() {
        Set<String> tags = new LinkedHashSet<>();
        for (BlockMatcher matcher : when) {
            if (matcher.tag() != null) {
                tags.add(matcher.tag());
            }
        }
        return tags;
    }

    public boolean usesFingerprint() {
        for (ToolMatcher matcher : tools) {
            if (matcher.fingerprint() != null) {
                return true;
            }
        }
        return false;
    }

    public boolean usesNbt() {
        for (ToolMatcher matcher : tools) {
            if (!matcher.nbt().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return a human-readable reason why this policy cannot be used, or
     *         {@code null} when it is valid.
     */
    public String invalidReason() {
        if (id == null || id.isBlank()) {
            return "missing id";
        }
        if (when.isEmpty()) {
            return "empty when";
        }
        for (BlockMatcher matcher : when) {
            if (matcher.isEmpty()) {
                return "empty block matcher";
            }
        }
        if (action == Action.NEVER) {
            return null;
        }
        if (tools.isEmpty()) {
            return "empty tools";
        }
        for (ToolMatcher matcher : tools) {
            if (matcher.isEmpty()) {
                return "empty tool matcher";
            }
            if (action == Action.FORCE && !matcher.isSpecific()) {
                return "FORCE requires a specific tool matcher (slot/name/fingerprint/item)";
            }
        }
        return null;
    }

    public static List<String> allTags(List<Policy> policies) {
        List<String> tags = new ArrayList<>();
        for (Policy policy : policies) {
            tags.addAll(policy.tagsUsed());
        }
        return tags;
    }
}
