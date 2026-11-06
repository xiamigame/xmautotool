package space.xiami.project.mc.mod.xmautotool;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import space.xiami.project.mc.mod.xmautotool.api.Games;
import space.xiami.project.mc.mod.xmautotool.api.Hosts;
import space.xiami.project.mc.mod.xmautotool.platform.AutoToolConfig;
import space.xiami.project.mc.mod.xmautotool.platform.ForgeGame;
import space.xiami.project.mc.mod.xmautotool.platform.ForgeHost;

/**
 * XMAutoTool entry point for the 1.18.2 adapter.
 *
 * <p>Installs the MDK-backed ports and registers the Forge config; all game
 * logic and UI live in the pure-JVM {@code common} module.</p>
 */
@Mod(AutoTool.MODID)
public final class AutoTool {

    public static final String MODID = "xmautotool";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AutoTool() {
        Hosts.install(new ForgeHost());
        Games.install(new ForgeGame());
        AutoToolConfig.register();
        LOGGER.info("XMAutoTool initialising");
    }
}
