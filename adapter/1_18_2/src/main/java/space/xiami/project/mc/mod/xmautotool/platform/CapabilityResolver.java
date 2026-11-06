package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.common.ToolActions;
import space.xiami.project.mc.mod.xmautotool.config.PolicyRuntime;
import space.xiami.project.mc.mod.xmautotool.core.ToolKindRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps an item stack to tool kinds, using (in order): user overrides, Forge
 * ToolActions, vanilla class checks.
 */
public final class CapabilityResolver {

    private CapabilityResolver() {
    }

    public static List<String> kinds(ItemStack stack) {
        String itemId = Registries.itemId(stack.getItem());
        PolicyRuntime.ToolOverride override = PolicyRuntime.override(itemId);
        if (override != null) {
            return override.kinds();
        }
        return baseKinds(stack);
    }

    /** Engine recognition without the user override, for side-by-side display. */
    public static List<String> baseKinds(ItemStack stack) {
        Item item = stack.getItem();
        List<String> kinds = new ArrayList<>(2);
        if (item.canPerformAction(stack, ToolActions.PICKAXE_DIG)) {
            kinds.add(ToolKindRegistry.PICKAXE);
        }
        if (item.canPerformAction(stack, ToolActions.AXE_DIG)) {
            kinds.add(ToolKindRegistry.AXE);
        }
        if (item.canPerformAction(stack, ToolActions.SHOVEL_DIG)) {
            kinds.add(ToolKindRegistry.SHOVEL);
        }
        if (item.canPerformAction(stack, ToolActions.HOE_DIG)) {
            kinds.add(ToolKindRegistry.HOE);
        }
        if (item.canPerformAction(stack, ToolActions.SWORD_DIG)) {
            kinds.add(ToolKindRegistry.SWORD);
        }
        if (item.canPerformAction(stack, ToolActions.SHEARS_DIG)) {
            kinds.add(ToolKindRegistry.SHEARS);
        }
        if (!kinds.isEmpty()) {
            return kinds;
        }

        if (item instanceof PickaxeItem) {
            kinds.add(ToolKindRegistry.PICKAXE);
        } else if (item instanceof AxeItem) {
            kinds.add(ToolKindRegistry.AXE);
        } else if (item instanceof ShovelItem) {
            kinds.add(ToolKindRegistry.SHOVEL);
        } else if (item instanceof HoeItem) {
            kinds.add(ToolKindRegistry.HOE);
        } else if (item instanceof ShearsItem) {
            kinds.add(ToolKindRegistry.SHEARS);
        } else if (item instanceof SwordItem) {
            kinds.add(ToolKindRegistry.SWORD);
        }
        return kinds;
    }

    public static List<String> allKinds(List<String> kinds) {
        return ToolKindRegistry.expand(kinds, PolicyRuntime.kindParents());
    }

    public static boolean assumeCorrect(ItemStack stack) {
        PolicyRuntime.ToolOverride override = PolicyRuntime.override(Registries.itemId(stack.getItem()));
        return override != null && override.assumeCorrect();
    }

    public static Float speedOverride(ItemStack stack) {
        PolicyRuntime.ToolOverride override = PolicyRuntime.override(Registries.itemId(stack.getItem()));
        return override == null ? null : override.speedOverride();
    }
}
