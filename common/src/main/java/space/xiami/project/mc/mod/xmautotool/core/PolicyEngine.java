package space.xiami.project.mc.mod.xmautotool.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered, first-match policy evaluation plus scoring and hysteresis.
 *
 * <p>Deliberately free of Minecraft types so the whole decision table can be
 * unit tested on a plain JVM.</p>
 */
public final class PolicyEngine {

    /** Implicit last policy: no block matched, just pick the best tool by measurement. */
    public static final Policy FALLBACK = new Policy(
            "__fallback",
            "fallback",
            true,
            List.of(),
            Action.USE,
            List.of(ToolMatcher.anyTool()),
            Tri.INHERIT);

    public Decision evaluate(BlockSnapshot block,
                             List<ToolSnapshot> hotbar,
                             EngineSettings settings,
                             List<Policy> policies,
                             int currentSlot) {
        if (!settings.enabled()) {
            return Decision.noOp("disabled");
        }
        for (BlockMatcher matcher : settings.blockBlacklist()) {
            if (matcher.matches(block)) {
                return Decision.noOp("block-blacklisted");
            }
        }

        List<ToolSnapshot> candidates = new ArrayList<>(hotbar.size());
        for (ToolSnapshot tool : hotbar) {
            if (tool == null || tool.itemId() == null) {
                continue;
            }
            if (isToolBlacklisted(tool, settings)) {
                continue;
            }
            if (!hasEnoughDurability(tool, settings.minDurability())) {
                continue;
            }
            candidates.add(tool);
        }
        if (candidates.isEmpty()) {
            return Decision.noOp("no-candidate");
        }

        Policy policy = firstMatch(policies, block);
        if (policy == null) {
            if (!settings.fallbackAuto()) {
                return Decision.noOp("no-policy");
            }
            policy = FALLBACK;
        }

        return switch (policy.action()) {
            case NEVER -> Decision.noOp("policy-never");
            case FORCE -> force(policy, candidates, currentSlot);
            case USE -> decide(policy, candidates, block, currentSlot, settings);
        };
    }

    public Policy firstMatch(List<Policy> policies, BlockSnapshot block) {
        for (Policy policy : policies) {
            if (policy.enabled() && policy.matches(block)) {
                return policy;
            }
        }
        return null;
    }

    private Decision force(Policy policy, List<ToolSnapshot> candidates, int currentSlot) {
        for (ToolSnapshot tool : candidates) {
            if (policy.allows(tool)) {
                if (tool.slot() == currentSlot) {
                    return Decision.noOp("already-selected");
                }
                return Decision.switchTo(tool.slot(), policy.id());
            }
        }
        return Decision.noOp("force-missing");
    }

    private Decision decide(Policy policy,
                            List<ToolSnapshot> candidates,
                            BlockSnapshot block,
                            int currentSlot,
                            EngineSettings settings) {
        List<ToolSnapshot> eligible = new ArrayList<>(candidates.size());
        for (ToolSnapshot tool : candidates) {
            if (policy.allows(tool)) {
                eligible.add(tool);
            }
        }
        if (eligible.isEmpty()) {
            return Decision.noOp("no-allowed-tool");
        }

        boolean needsCorrect = block.requiresCorrectToolForDrops();
        boolean gate = policy.requireCorrectTool().resolve(settings.requireCorrectTool());
        if (needsCorrect && gate && !anyHarvest(eligible)) {
            return Decision.noOp("no-correct-tool");
        }

        ToolSnapshot best = pickBest(eligible, policy, needsCorrect, gate, currentSlot);
        if (best == null) {
            return Decision.noOp("no-best");
        }
        if (best.slot() == currentSlot) {
            return Decision.noOp("already-selected");
        }

        ToolSnapshot current = findSlot(candidates, currentSlot);
        if (current == null || !policy.allows(current)) {
            return Decision.switchTo(best.slot(), policy.id());
        }
        if (gate && needsCorrect && !current.harvest() && best.harvest()) {
            return Decision.switchTo(best.slot(), policy.id());
        }
        if (current.speed() < 0.05F) {
            return Decision.switchTo(best.slot(), policy.id());
        }
        double threshold = current.speed() * (1.0D + settings.minSpeedGainPercent() / 100.0D);
        if (best.speed() >= threshold) {
            return Decision.switchTo(best.slot(), policy.id());
        }
        return Decision.noOp("hysteresis");
    }

    private ToolSnapshot pickBest(List<ToolSnapshot> eligible,
                                  Policy policy,
                                  boolean needsCorrect,
                                  boolean gate,
                                  int currentSlot) {
        ToolSnapshot best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (ToolSnapshot tool : eligible) {
            double score = score(tool, policy, needsCorrect, gate, currentSlot);
            if (best == null
                    || score > bestScore
                    || (score == bestScore && betterTieBreak(tool, best, currentSlot))) {
                best = tool;
                bestScore = score;
            }
        }
        return best;
    }

    private double score(ToolSnapshot tool,
                         Policy policy,
                         boolean needsCorrect,
                         boolean gate,
                         int currentSlot) {
        double correctness = (gate && needsCorrect)
                ? (tool.harvest() ? 1000.0D : 0.0D)
                : (tool.harvest() ? 20.0D : 0.0D);
        return correctness
                + tool.speed() * 10.0D
                + policy.weightOf(tool)
                + (tool.slot() == currentSlot ? 2.0D : 0.0D);
    }

    private boolean betterTieBreak(ToolSnapshot candidate, ToolSnapshot best, int currentSlot) {
        if (candidate.slot() == currentSlot) {
            return true;
        }
        if (best.slot() == currentSlot) {
            return false;
        }
        return candidate.slot() < best.slot();
    }

    private boolean anyHarvest(List<ToolSnapshot> tools) {
        for (ToolSnapshot tool : tools) {
            if (tool.harvest()) {
                return true;
            }
        }
        return false;
    }

    private ToolSnapshot findSlot(List<ToolSnapshot> tools, int slot) {
        for (ToolSnapshot tool : tools) {
            if (tool.slot() == slot) {
                return tool;
            }
        }
        return null;
    }

    private boolean isToolBlacklisted(ToolSnapshot tool, EngineSettings settings) {
        for (ToolMatcher matcher : settings.toolBlacklist()) {
            if (matcher.matches(tool)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEnoughDurability(ToolSnapshot tool, int minDurability) {
        return !tool.damageable() || minDurability <= 0 || tool.remainingDurability() >= minDurability;
    }
}
