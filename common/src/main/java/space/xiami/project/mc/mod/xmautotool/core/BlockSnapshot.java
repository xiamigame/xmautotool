package space.xiami.project.mc.mod.xmautotool.core;

import java.util.Map;
import java.util.Set;

/**
 * Immutable view of the block being mined.
 *
 * <p>Built by the platform layer from a {@code BlockState}; the core never sees
 * Minecraft classes. Only tags referenced by the loaded policies are resolved
 * (see {@link Policy#tagsUsed()}).</p>
 */
public record BlockSnapshot(
        String id,
        Set<String> tags,
        Map<String, String> properties,
        boolean requiresCorrectToolForDrops
) {
}
