package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Machine-level config: which profile file is active, and the debug switch.
 * All behaviour settings live inside the profile file now.
 */
public final class AutoToolConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> ACTIVE_PROFILE;
    public static final ForgeConfigSpec.BooleanValue DEBUG;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ACTIVE_PROFILE = builder
                .comment("Id of the active profile file under config/xmautotool/profiles/.")
                .define("activeProfile", "default");
        DEBUG = builder
                .comment("Log switch decisions to the game log.")
                .define("debug", false);
        SPEC = builder.build();
    }

    private AutoToolConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }
}
