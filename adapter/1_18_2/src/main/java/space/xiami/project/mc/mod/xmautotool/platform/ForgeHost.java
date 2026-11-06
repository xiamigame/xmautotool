package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.fml.loading.FMLPaths;
import space.xiami.project.mc.mod.xmautotool.api.Host;
import space.xiami.project.mc.mod.xmautotool.gui.Screen;

import java.nio.file.Path;

/** Host capabilities backed by Forge/Minecraft. */
public final class ForgeHost implements Host {

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String activeProfileId() {
        return AutoToolConfig.ACTIVE_PROFILE.get();
    }

    @Override
    public void setActiveProfileId(String id) {
        AutoToolConfig.ACTIVE_PROFILE.set(id);
    }

    @Override
    public void saveConfig() {
        AutoToolConfig.SPEC.save();
    }

    @Override
    public boolean debug() {
        return AutoToolConfig.DEBUG.get();
    }

    @Override
    public String translate(String key) {
        return new TranslatableComponent(key).getString();
    }

    @Override
    public void openScreen(Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(screen == null ? null : new MinecraftScreen(screen));
    }

    @Override
    public String clipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text == null ? "" : text);
    }
}
