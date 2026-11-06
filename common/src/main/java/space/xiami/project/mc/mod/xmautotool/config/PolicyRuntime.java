package space.xiami.project.mc.mod.xmautotool.config;

import space.xiami.project.mc.mod.xmautotool.core.BlockMatcher;
import space.xiami.project.mc.mod.xmautotool.core.Policy;
import space.xiami.project.mc.mod.xmautotool.core.ToolKindRegistry;
import space.xiami.project.mc.mod.xmautotool.core.ToolMatcher;

import java.util.List;
import java.util.Map;

/**
 * Currently active user data: policies, blacklists and tool-kind overrides.
 *
 * <p>Populated from {@link ProfileManager} whenever a profile is loaded or saved.</p>
 */
public final class PolicyRuntime {

    private static volatile List<Policy> policies = List.of();
    private static volatile List<BlockMatcher> blockBlacklist = List.of();
    private static volatile List<ToolMatcher> toolBlacklist = List.of();
    private static volatile Map<String, ToolOverride> overrides = Map.of();
    private static volatile Map<String, List<String>> kindParents = ToolKindRegistry.builtins();
    private static volatile long policyHash = 1L;
    private static volatile boolean needsFingerprint;
    private static volatile boolean needsNbt;

    private PolicyRuntime() {
    }

    public static List<Policy> policies() {
        return policies;
    }

    public static List<BlockMatcher> blockBlacklist() {
        return blockBlacklist;
    }

    public static List<ToolMatcher> toolBlacklist() {
        return toolBlacklist;
    }

    public static Map<String, ToolOverride> overrides() {
        return overrides;
    }

    public static ToolOverride override(String itemId) {
        return overrides.get(itemId);
    }

    public static Map<String, List<String>> kindParents() {
        return kindParents;
    }

    public static long policyHash() {
        return policyHash;
    }

    public static boolean needsFingerprint() {
        return needsFingerprint;
    }

    public static boolean needsNbt() {
        return needsNbt;
    }

    public static synchronized void apply(List<Policy> newPolicies,
                                          List<BlockMatcher> newBlockBlacklist,
                                          List<ToolMatcher> newToolBlacklist,
                                          Map<String, ToolOverride> newOverrides,
                                          Map<String, List<String>> newKindParents) {
        policies = List.copyOf(newPolicies);
        blockBlacklist = List.copyOf(newBlockBlacklist);
        toolBlacklist = List.copyOf(newToolBlacklist);
        overrides = Map.copyOf(newOverrides);
        kindParents = Map.copyOf(newKindParents);
        policyHash = hashOf(policies, blockBlacklist, toolBlacklist, overrides, kindParents);

        boolean fingerprint = false;
        boolean nbt = false;
        for (Policy policy : policies) {
            fingerprint |= policy.usesFingerprint();
            nbt |= policy.usesNbt();
        }
        for (ToolMatcher matcher : toolBlacklist) {
            fingerprint |= matcher.fingerprint() != null;
            nbt |= !matcher.nbt().isEmpty();
        }
        needsFingerprint = fingerprint;
        needsNbt = nbt;
    }

    private static long hashOf(List<Policy> policies,
                               List<BlockMatcher> blockBlacklist,
                               List<ToolMatcher> toolBlacklist,
                               Map<String, ToolOverride> overrides,
                               Map<String, List<String>> kindParents) {
        long hash = 1125899906842597L;
        hash = 31 * hash + policies.hashCode();
        hash = 31 * hash + blockBlacklist.hashCode();
        hash = 31 * hash + toolBlacklist.hashCode();
        hash = 31 * hash + overrides.hashCode();
        hash = 31 * hash + kindParents.hashCode();
        return hash;
    }

    /** User-defined capability mapping for one item. */
    public record ToolOverride(
            String item,
            List<String> kinds,
            boolean assumeCorrect,
            Float speedOverride
    ) {
        public ToolOverride {
            kinds = kinds == null ? List.of() : List.copyOf(kinds);
        }
    }
}
