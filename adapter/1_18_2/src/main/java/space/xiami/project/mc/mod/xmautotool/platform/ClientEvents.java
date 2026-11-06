package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import space.xiami.project.mc.mod.xmautotool.AutoTool;
import space.xiami.project.mc.mod.xmautotool.api.Hosts;
import space.xiami.project.mc.mod.xmautotool.config.ProfileManager;
import space.xiami.project.mc.mod.xmautotool.config.ProfileSettings;
import space.xiami.project.mc.mod.xmautotool.gui.AutoToolScreen;
import space.xiami.project.mc.mod.xmautotool.trigger.TriggerController;

/**
 * Forge event bridge: converts game events into the pure-JVM trigger controller
 * and lazily builds the snapshots it needs.
 */
@Mod.EventBusSubscriber(modid = AutoTool.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {

    private static final TriggerController CONTROLLER = new TriggerController();

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getSide() != LogicalSide.CLIENT) {
            return;
        }
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != player || minecraft.level == null) {
            return;
        }
        BlockState state = minecraft.level.getBlockState(event.getPos());
        if (state.isAir()) {
            return;
        }
        CONTROLLER.onLeftClick(
                () -> SnapshotFactory.block(state),
                () -> SnapshotFactory.hotbar(player, state),
                player.getInventory().selected);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        while (KeyMappings.OPEN_GUI.consumeClick()) {
            Hosts.host().openScreen(new AutoToolScreen());
        }
        CONTROLLER.onTick();

        ProfileSettings settings = ProfileManager.settings();
        if (!settings.triggerWhileMining) {
            return;
        }
        if (!minecraft.options.keyAttack.isDown()) {
            return;
        }
        if (minecraft.gameMode == null || !minecraft.gameMode.isDestroying()) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
        if (state.isAir()) {
            return;
        }
        CONTROLLER.onMiningTick(
                () -> SnapshotFactory.block(state),
                () -> SnapshotFactory.hotbar(minecraft.player, state),
                minecraft.player.getInventory().selected,
                Math.max(1, settings.miningEvalInterval));
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        SnapshotFactory.clear();
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        SnapshotFactory.clear();
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        SnapshotFactory.clear();
    }
}
