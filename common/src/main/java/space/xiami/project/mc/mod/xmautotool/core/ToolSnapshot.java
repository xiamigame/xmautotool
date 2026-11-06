package space.xiami.project.mc.mod.xmautotool.core;

import java.util.List;
import java.util.Map;

/**
 * Immutable view of one hotbar slot.
 *
 * @param kinds   directly assigned kinds (e.g. {@code xmautotool:hammer})
 * @param allKinds kinds plus all their ancestors, used for capability matching
 */
public record ToolSnapshot(
        int slot,
        String itemId,
        List<String> kinds,
        List<String> allKinds,
        float speed,
        boolean harvest,
        boolean damageable,
        int remainingDurability,
        String name,
        String fingerprint,
        Map<String, String> nbt
) {
    public boolean matchesKind(String kind) {
        return kinds.contains(kind);
    }

    public boolean matchesCapability(String capability) {
        return allKinds.contains(capability);
    }
}
