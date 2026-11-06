package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import space.xiami.project.mc.mod.xmautotool.AutoTool;
import space.xiami.project.mc.mod.xmautotool.config.ProfileManager;

/** Client startup: load the active profile file. */
@Mod.EventBusSubscriber(modid = AutoTool.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ProfileManager::init);
    }
}
