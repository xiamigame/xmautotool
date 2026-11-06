package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

/** The only place that mutates the selected hotbar slot. */
public final class SlotSwitcher {

    private SlotSwitcher() {
    }

    public static void switchTo(int slot) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        player.getInventory().selected = slot;
        if (player.connection != null) {
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        }
    }
}
