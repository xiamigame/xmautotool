package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.api.distmarker.Dist;
import org.lwjgl.glfw.GLFW;
import space.xiami.project.mc.mod.xmautotool.AutoTool;

/** Key bindings (stored by vanilla in options.txt, not in the mod config). */
@Mod.EventBusSubscriber(modid = AutoTool.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KeyMappings {

    public static final String CATEGORY = "key.categories.xmautotool";

    public static final KeyMapping OPEN_GUI = new KeyMapping("key.xmautotool.open", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping SUPPRESS = new KeyMapping("key.xmautotool.suppress", GLFW.GLFW_KEY_UNKNOWN, CATEGORY);

    private KeyMappings() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientRegistry.registerKeyBinding(OPEN_GUI);
            ClientRegistry.registerKeyBinding(SUPPRESS);
        });
    }
}
