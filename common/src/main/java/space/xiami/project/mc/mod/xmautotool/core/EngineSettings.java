package space.xiami.project.mc.mod.xmautotool.core;

import java.util.List;

/** Engine-facing snapshot of the user settings (no Minecraft types). */
public record EngineSettings(
        boolean enabled,
        boolean fallbackAuto,
        boolean requireCorrectTool,
        int minDurability,
        int minSpeedGainPercent,
        List<BlockMatcher> blockBlacklist,
        List<ToolMatcher> toolBlacklist
) {
    public EngineSettings {
        blockBlacklist = blockBlacklist == null ? List.of() : List.copyOf(blockBlacklist);
        toolBlacklist = toolBlacklist == null ? List.of() : List.copyOf(toolBlacklist);
    }

    public static EngineSettings defaults() {
        return new EngineSettings(true, true, true, 10, 10, List.of(), List.of());
    }
}
