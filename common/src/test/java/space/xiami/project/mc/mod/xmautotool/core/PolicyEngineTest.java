package space.xiami.project.mc.mod.xmautotool.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyEngineTest {

    private final PolicyEngine engine = new PolicyEngine();

    @Test
    void fallbackPicksFastestHarvestingTool() {
        BlockSnapshot stone = block("minecraft:stone", false);
        List<ToolSnapshot> hotbar = List.of(
                tool(0, "minecraft:stick", "", 1.0F, false),
                tool(1, "minecraft:iron_pickaxe", ToolKindRegistry.PICKAXE, 4.0F, true),
                tool(2, "minecraft:iron_axe", ToolKindRegistry.AXE, 1.0F, false));

        Decision decision = engine.evaluate(stone, hotbar, settings(), List.of(), 0);

        assertTrue(decision.switchSlot());
        assertEquals(1, decision.slot());
        assertEquals(PolicyEngine.FALLBACK.id(), decision.policyId());
    }

    @Test
    void usePolicyRestrictsTools() {
        BlockSnapshot log = block("minecraft:oak_log", false, "minecraft:logs");
        Policy policy = policy("logs", Action.USE,
                List.of(BlockMatcher.tag("minecraft:logs")),
                List.of(ToolMatcher.type(ToolKindRegistry.AXE)));
        List<ToolSnapshot> hotbar = List.of(
                tool(0, "minecraft:iron_pickaxe", ToolKindRegistry.PICKAXE, 5.0F, true),
                tool(1, "minecraft:iron_axe", ToolKindRegistry.AXE, 2.0F, true));

        Decision decision = engine.evaluate(log, hotbar, settings(), List.of(policy), 0);

        assertTrue(decision.switchSlot());
        assertEquals(1, decision.slot());
        assertEquals("logs", decision.policyId());
    }

    @Test
    void neverPolicyDoesNotSwitch() {
        BlockSnapshot flower = block("minecraft:dandelion", false, "minecraft:flowers");
        Policy policy = policy("flowers", Action.NEVER, List.of(BlockMatcher.tag("minecraft:flowers")), List.of());

        Decision decision = engine.evaluate(flower,
                List.of(tool(0, "minecraft:stick", "", 1.0F, false),
                        tool(1, "minecraft:iron_shovel", ToolKindRegistry.SHOVEL, 4.0F, true)),
                settings(), List.of(policy), 0);

        assertFalse(decision.switchSlot());
        assertEquals("policy-never", decision.reason());
    }

    @Test
    void usePolicyWithNoAllowedToolIsNoOp() {
        BlockSnapshot log = block("minecraft:oak_log", false, "minecraft:logs");
        Policy policy = policy("logs", Action.USE,
                List.of(BlockMatcher.tag("minecraft:logs")),
                List.of(ToolMatcher.type(ToolKindRegistry.AXE)));
        List<ToolSnapshot> hotbar = List.of(tool(0, "minecraft:iron_pickaxe", ToolKindRegistry.PICKAXE, 5.0F, true));

        Decision decision = engine.evaluate(log, hotbar, settings(), List.of(policy), 0);

        assertFalse(decision.switchSlot());
        assertEquals("no-allowed-tool", decision.reason());
    }

    @Test
    void gateBlocksWhenNoCandidateCanHarvest() {
        BlockSnapshot ore = block("minecraft:diamond_ore", true);
        List<ToolSnapshot> hotbar = List.of(
                tool(0, "minecraft:stick", "", 1.0F, false),
                tool(1, "minecraft:wooden_pickaxe", ToolKindRegistry.PICKAXE, 2.0F, false));

        Decision decision = engine.evaluate(ore, hotbar, settings(), List.of(), 0);

        assertFalse(decision.switchSlot());
        assertEquals("no-correct-tool", decision.reason());
    }

    @Test
    void gateOffAllowsFasterWrongTool() {
        BlockSnapshot ore = block("minecraft:diamond_ore", true);
        List<ToolSnapshot> hotbar = List.of(
                tool(0, "minecraft:stick", "", 1.0F, false),
                tool(1, "test:hammer", ToolKindRegistry.PICKAXE, 12.0F, false),
                tool(2, "minecraft:iron_pickaxe", ToolKindRegistry.PICKAXE, 4.0F, true));
        EngineSettings relaxed = new EngineSettings(true, true, false, 0, 10, List.of(), List.of());

        Decision decision = engine.evaluate(ore, hotbar, relaxed, List.of(), 0);

        assertTrue(decision.switchSlot());
        assertEquals(1, decision.slot());
    }

    @Test
    void hysteresisKeepsCurrentTool() {
        BlockSnapshot stone = block("minecraft:stone", true);
        List<ToolSnapshot> hotbar = List.of(
                tool(0, "minecraft:iron_pickaxe", ToolKindRegistry.PICKAXE, 4.0F, true),
                tool(1, "test:fast_pickaxe", ToolKindRegistry.PICKAXE, 4.3F, true));

        Decision decision = engine.evaluate(stone, hotbar, settings(), List.of(), 0);

        assertFalse(decision.switchSlot());
        assertEquals("hysteresis", decision.reason());
    }

    @Test
    void forceSwitchesIgnoringSpeed() {
        BlockSnapshot stone = block("minecraft:stone", true);
        Policy force = policy("force", Action.FORCE,
                List.of(BlockMatcher.id("minecraft:stone")),
                List.of(new ToolMatcher(3, null, null, null, null, null, Map.of(), false, 0)));
        List<ToolSnapshot> hotbar = List.of(
                tool(0, "minecraft:iron_pickaxe", ToolKindRegistry.PICKAXE, 4.0F, true),
                tool(3, "test:slow_pickaxe", ToolKindRegistry.PICKAXE, 1.0F, true));

        Decision decision = engine.evaluate(stone, hotbar, settings(), List.of(force), 0);

        assertTrue(decision.switchSlot());
        assertEquals(3, decision.slot());
    }

    @Test
    void firstMatchingPolicyWins() {
        BlockSnapshot log = block("minecraft:oak_log", false, "minecraft:logs");
        Policy first = policy("first", Action.USE,
                List.of(BlockMatcher.tag("minecraft:logs")),
                List.of(ToolMatcher.type(ToolKindRegistry.AXE)));
        Policy second = policy("second", Action.USE,
                List.of(BlockMatcher.tag("minecraft:logs")),
                List.of(ToolMatcher.type(ToolKindRegistry.PICKAXE)));
        List<ToolSnapshot> hotbar = List.of(
                tool(0, "minecraft:iron_pickaxe", ToolKindRegistry.PICKAXE, 5.0F, true),
                tool(1, "minecraft:iron_axe", ToolKindRegistry.AXE, 2.0F, true));

        Decision decision = engine.evaluate(log, hotbar, settings(), List.of(first, second), 0);

        assertEquals(1, decision.slot());
        assertEquals("first", decision.policyId());
    }

    @Test
    void weightPrefersNamedTool() {
        BlockSnapshot log = block("minecraft:oak_log", false, "minecraft:logs");
        Policy policy = policy("logs", Action.USE,
                List.of(BlockMatcher.tag("minecraft:logs")),
                List.of(
                        ToolMatcher.capability(ToolKindRegistry.AXE),
                        new ToolMatcher(null, "Silk", null, null, ToolKindRegistry.AXE, null, Map.of(), false, 30)));
        List<ToolSnapshot> hotbar = List.of(
                tool(0, "minecraft:stick", "", 1.0F, false),
                tool(1, "minecraft:iron_axe", ToolKindRegistry.AXE, 4.0F, true),
                tool(2, "test:silk_axe", ToolKindRegistry.AXE, 3.5F, true, "Silk Axe"));

        Decision decision = engine.evaluate(log, hotbar, settings(), List.of(policy), 0);

        assertTrue(decision.switchSlot());
        assertEquals(2, decision.slot());
    }

    @Test
    void durabilityFilterSkipsWornTools() {
        BlockSnapshot stone = block("minecraft:stone", true);
        ToolSnapshot worn = new ToolSnapshot(0, "minecraft:iron_pickaxe", List.of(ToolKindRegistry.PICKAXE),
                ToolKindRegistry.expand(List.of(ToolKindRegistry.PICKAXE)),
                4.0F, true, true, 3, "", "", Map.of());
        List<ToolSnapshot> hotbar = List.of(
                worn,
                tool(1, "minecraft:stone_pickaxe", ToolKindRegistry.PICKAXE, 3.0F, true));
        EngineSettings durability = new EngineSettings(true, true, true, 10, 10, List.of(), List.of());

        Decision decision = engine.evaluate(stone, hotbar, durability, List.of(), 0);

        assertEquals(1, decision.slot());
    }

    @Test
    void blockBlacklistStopsEvaluation() {
        BlockSnapshot bedrock = block("minecraft:bedrock", false);
        EngineSettings settings = new EngineSettings(true, true, true, 0, 10,
                List.of(BlockMatcher.id("minecraft:bedrock")), List.of());
        List<ToolSnapshot> hotbar = List.of(tool(0, "minecraft:iron_pickaxe", ToolKindRegistry.PICKAXE, 4.0F, true));

        Decision decision = engine.evaluate(bedrock, hotbar, settings, List.of(), 0);

        assertFalse(decision.switchSlot());
        assertEquals("block-blacklisted", decision.reason());
    }

    @Test
    void fallbackCanBeDisabled() {
        BlockSnapshot stone = block("minecraft:stone", false);
        EngineSettings noFallback = new EngineSettings(true, false, true, 0, 10, List.of(), List.of());
        List<ToolSnapshot> hotbar = List.of(tool(1, "minecraft:iron_pickaxe", ToolKindRegistry.PICKAXE, 4.0F, true));

        Decision decision = engine.evaluate(stone, hotbar, noFallback, List.of(), 0);

        assertFalse(decision.switchSlot());
        assertEquals("no-policy", decision.reason());
    }

    @Test
    void policyValidationRejectsForceWithTypeMatcher() {
        Policy force = policy("bad", Action.FORCE,
                List.of(BlockMatcher.id("minecraft:stone")),
                List.of(ToolMatcher.type(ToolKindRegistry.PICKAXE)));

        assertNotNull(force.invalidReason());
    }

    @Test
    void fallbackIsImplicitAnyToolPolicy() {
        assertEquals("__fallback", PolicyEngine.FALLBACK.id());
        assertEquals(Action.USE, PolicyEngine.FALLBACK.action());
        assertTrue(PolicyEngine.FALLBACK.allows(tool(0, "minecraft:iron_pickaxe", ToolKindRegistry.PICKAXE, 4.0F, true)));
    }

    private static BlockSnapshot block(String id, boolean needsCorrect, String... tags) {
        return new BlockSnapshot(id, Set.of(tags), Map.of(), needsCorrect);
    }

    private static ToolSnapshot tool(int slot, String item, String kind, float speed, boolean harvest) {
        return tool(slot, item, kind, speed, harvest, "");
    }

    private static ToolSnapshot tool(int slot, String item, String kind, float speed, boolean harvest, String name) {
        List<String> kinds = kind.isEmpty() ? List.of() : List.of(kind);
        return new ToolSnapshot(slot, item, kinds, ToolKindRegistry.expand(kinds),
                speed, harvest, false, Integer.MAX_VALUE, name, "", Map.of());
    }

    private static EngineSettings settings() {
        return new EngineSettings(true, true, true, 0, 10, List.of(), List.of());
    }

    private static Policy policy(String id, Action action, List<BlockMatcher> when, List<ToolMatcher> tools) {
        return new Policy(id, id, true, when, action, tools, Tri.INHERIT);
    }
}
