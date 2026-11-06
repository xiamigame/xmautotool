package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import space.xiami.project.mc.mod.xmautotool.config.PolicyRuntime;
import space.xiami.project.mc.mod.xmautotool.core.BlockSnapshot;
import space.xiami.project.mc.mod.xmautotool.core.Policy;
import space.xiami.project.mc.mod.xmautotool.core.ToolSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts game objects into the immutable snapshots the core works with.
 *
 * <p>Per-state snapshots are cached and invalidated when the policy hash
 * changes, when tags are reloaded, or when a world is (un)loaded.</p>
 */
public final class SnapshotFactory {

    private static final Map<BlockState, BlockSnapshot> BLOCK_CACHE = new IdentityHashMap<>();
    private static long cachedPolicyHash = Long.MIN_VALUE;
    private static List<String> cachedTags = List.of();

    private SnapshotFactory() {
    }

    public static BlockSnapshot block(BlockState state) {
        long hash = PolicyRuntime.policyHash();
        if (hash != cachedPolicyHash) {
            BLOCK_CACHE.clear();
            cachedPolicyHash = hash;
            cachedTags = Policy.allTags(PolicyRuntime.policies());
        }
        BlockSnapshot snapshot = BLOCK_CACHE.get(state);
        if (snapshot == null) {
            snapshot = buildBlock(state, cachedTags);
            BLOCK_CACHE.put(state, snapshot);
        }
        return snapshot;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockSnapshot buildBlock(BlockState state, List<String> tagsUsed) {
        String id = Registries.blockId(state.getBlock());
        Map<String, String> properties = new LinkedHashMap<>();
        for (Property<?> property : state.getProperties()) {
            properties.put(property.getName(), String.valueOf(state.getValue((Property) property)));
        }
        Set<String> tags = new HashSet<>();
        for (String tag : tagsUsed) {
            ResourceLocation location = ResourceLocation.tryParse(tag);
            if (location == null) {
                continue;
            }
            TagKey<Block> key = TagKey.create(Registry.BLOCK_REGISTRY, location);
            if (state.is(key)) {
                tags.add(tag);
            }
        }
        return new BlockSnapshot(id, tags, properties, state.requiresCorrectToolForDrops());
    }

    public static List<ToolSnapshot> hotbar(LocalPlayer player, BlockState state) {
        Inventory inventory = player.getInventory();
        List<ToolSnapshot> tools = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            tools.add(tool(slot, stack, state));
        }
        return tools;
    }

    private static ToolSnapshot tool(int slot, ItemStack stack, BlockState state) {
        List<String> kinds = CapabilityResolver.kinds(stack);
        float speed = stack.getDestroySpeed(state);
        Float override = CapabilityResolver.speedOverride(stack);
        if (override != null) {
            speed = override;
        }
        boolean harvest = stack.isCorrectToolForDrops(state) || CapabilityResolver.assumeCorrect(stack);
        boolean damageable = stack.isDamageableItem();
        int remaining = damageable ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE;
        String fingerprint = PolicyRuntime.needsFingerprint() ? Fingerprints.of(stack) : "";
        Map<String, String> nbt = PolicyRuntime.needsNbt() ? Fingerprints.flatten(stack) : Map.of();
        return new ToolSnapshot(
                slot,
                Registries.itemId(stack.getItem()),
                kinds,
                CapabilityResolver.allKinds(kinds),
                speed,
                harvest,
                damageable,
                remaining,
                stack.getHoverName().getString(),
                fingerprint,
                nbt);
    }

    public static void clear() {
        BLOCK_CACHE.clear();
        cachedPolicyHash = Long.MIN_VALUE;
    }
}
