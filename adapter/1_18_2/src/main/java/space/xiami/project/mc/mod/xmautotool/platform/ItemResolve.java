package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;

/** Resolves pure-JVM {@link IconRef}s to vanilla item stacks for drawing. */
public final class ItemResolve {

    private ItemResolve() {
    }

    public static ItemStack stack(IconRef icon) {
        if (icon == null || icon.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return switch (icon.kind()) {
            case ITEM -> {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(icon.id()));
                yield item == null ? ItemStack.EMPTY : new ItemStack(item);
            }
            case BLOCK -> {
                Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse(icon.id()));
                if (block == null || block == Blocks.AIR) {
                    yield ItemStack.EMPTY;
                }
                Item item = Item.byBlock(block);
                yield item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
            }
            case SLOT -> {
                Player player = Minecraft.getInstance().player;
                if (player == null) {
                    yield ItemStack.EMPTY;
                }
                yield icon.slot() < 0 ? player.getOffhandItem() : player.getInventory().getItem(icon.slot());
            }
            case EMPTY -> ItemStack.EMPTY;
        };
    }
}
