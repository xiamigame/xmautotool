package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** Small helpers around the 1.18.2 registries. */
public final class Registries {

    private Registries() {
    }

    public static String itemId(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? "minecraft:air" : key.toString();
    }

    public static String blockId(Block block) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return key == null ? "minecraft:air" : key.toString();
    }
}
