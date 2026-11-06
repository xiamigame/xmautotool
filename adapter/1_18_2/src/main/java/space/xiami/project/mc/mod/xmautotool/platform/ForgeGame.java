package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;
import space.xiami.project.mc.mod.xmautotool.api.BlockView;
import space.xiami.project.mc.mod.xmautotool.api.Game;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;
import space.xiami.project.mc.mod.xmautotool.api.ItemView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Game capabilities backed by the 1.18.2 client APIs. */
public final class ForgeGame implements Game {

    private static Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    @Override
    public boolean levelReady() {
        return minecraft().player != null && minecraft().level != null;
    }

    @Override
    public boolean hasPlayer() {
        return minecraft().player != null;
    }

    @Override
    public boolean screenOpen() {
        return minecraft().screen != null;
    }

    @Override
    public boolean spectator() {
        LocalPlayer player = minecraft().player;
        return player != null && player.isSpectator();
    }

    @Override
    public boolean usingItem() {
        LocalPlayer player = minecraft().player;
        return player != null && player.isUsingItem();
    }

    @Override
    public boolean sleeping() {
        LocalPlayer player = minecraft().player;
        return player != null && player.isSleeping();
    }

    @Override
    public boolean attackDown() {
        return minecraft().options.keyAttack.isDown();
    }

    @Override
    public boolean destroying() {
        return minecraft().gameMode != null && minecraft().gameMode.isDestroying();
    }

    @Override
    public boolean suppressDown() {
        return KeyMappings.SUPPRESS.isDown();
    }

    @Override
    public int selectedSlot() {
        LocalPlayer player = minecraft().player;
        return player == null ? -1 : player.getInventory().selected;
    }

    @Override
    public void switchSlot(int slot) {
        SlotSwitcher.switchTo(slot);
    }

    @Override
    public BlockView lookingBlock() {
        Minecraft minecraft = minecraft();
        if (minecraft.level == null) {
            return null;
        }
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
        return new BlockView(Registries.blockId(state.getBlock()), properties(state));
    }

    @Override
    public BlockView heldBlock() {
        LocalPlayer player = minecraft().player;
        if (player == null) {
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return null;
        }
        Block block = Block.byItem(stack.getItem());
        if (block == Blocks.AIR) {
            return null;
        }
        return new BlockView(Registries.blockId(block), Map.of());
    }

    @Override
    public ItemView heldItem() {
        LocalPlayer player = minecraft().player;
        if (player == null) {
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return null;
        }
        return view(stack, IconRef.item(Registries.itemId(stack.getItem())));
    }

    @Override
    public List<ItemView> inventoryItems() {
        LocalPlayer player = minecraft().player;
        if (player == null) {
            return List.of();
        }
        Map<String, ItemView> unique = new LinkedHashMap<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            add(unique, inventory.items.get(slot), IconRef.slot(slot));
        }
        add(unique, player.getOffhandItem(), IconRef.slot(-1));
        return List.copyOf(unique.values());
    }

    private static void add(Map<String, ItemView> unique, ItemStack stack, IconRef icon) {
        if (stack.isEmpty()) {
            return;
        }
        String id = Registries.itemId(stack.getItem());
        unique.putIfAbsent(id, view(stack, icon));
    }

    private static ItemView view(ItemStack stack, IconRef icon) {
        String id = Registries.itemId(stack.getItem());
        return new ItemView(icon, id, stack.getHoverName().getString(), stack.getCount(),
                CapabilityResolver.kinds(stack));
    }

    @Override
    public List<String> blockIds() {
        return ForgeRegistries.BLOCKS.getKeys().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }

    @Override
    public List<String> blockTags() {
        return ForgeRegistries.BLOCKS.tags().getTagNames()
                .map(TagKey::location)
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }

    @Override
    public List<String> itemIds() {
        return ForgeRegistries.ITEMS.getKeys().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }

    @Override
    public List<String> baseKinds(String itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemId));
        if (item == null) {
            return List.of();
        }
        return CapabilityResolver.baseKinds(new ItemStack(item));
    }

    @Override
    public String iconName(IconRef icon) {
        ItemStack stack = ItemResolve.stack(icon);
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, String> properties(BlockState state) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (Property<?> property : state.getProperties()) {
            properties.put(property.getName(), String.valueOf(state.getValue((Property) property)));
        }
        return properties;
    }
}
