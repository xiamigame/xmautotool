package space.xiami.project.mc.mod.xmautotool.api;

import space.xiami.project.mc.mod.xmautotool.gui.Screen;

import java.nio.file.Path;

/**
 * Environment capabilities the adapter provides to the pure-JVM side.
 *
 * <p>Implemented once per Minecraft version; the rest of the mod never sees
 * Forge or Minecraft types.</p>
 */
public interface Host {

    /** Directory where user configuration lives (Forge: the instance config dir). */
    Path configDir();

    /** Id of the active profile file, as persisted in the machine config. */
    String activeProfileId();

    /** Persists the active profile id; callers should follow with {@link #saveConfig()}. */
    void setActiveProfileId(String id);

    /** Flushes the machine config to disk. */
    void saveConfig();

    /** Whether debug logging is enabled in the machine config. */
    boolean debug();

    /** Localised text for a translation key; unknown keys are returned as-is. */
    String translate(String key);

    /** Opens a UI screen; {@code null} closes the current one. */
    void openScreen(Screen screen);

    String clipboard();

    void setClipboard(String text);
}
