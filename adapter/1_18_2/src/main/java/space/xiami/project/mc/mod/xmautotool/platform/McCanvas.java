package space.xiami.project.mc.mod.xmautotool.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.world.item.ItemStack;
import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;

/** Canvas primitives backed by the 1.18.2 PoseStack/Font/ItemRenderer. */
public final class McCanvas implements Canvas {

    private final net.minecraft.client.gui.screens.Screen screen;
    private final PoseStack pose;
    private final Minecraft minecraft;

    public McCanvas(net.minecraft.client.gui.screens.Screen screen, PoseStack pose) {
        this.screen = screen;
        this.pose = pose;
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    public void background() {
        screen.renderBackground(pose);
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        GuiComponent.fill(pose, x1, y1, x2, y2, argb);
    }

    @Override
    public int textWidth(String text) {
        return minecraft.font.width(text);
    }

    @Override
    public void text(String text, int x, int y, int argb) {
        minecraft.font.draw(pose, text, x, y, argb);
    }

    @Override
    public void textShadow(String text, int x, int y, int argb) {
        minecraft.font.drawShadow(pose, text, x, y, argb);
    }

    @Override
    public void item(IconRef icon, int x, int y) {
        ItemStack stack = ItemResolve.stack(icon);
        if (!stack.isEmpty()) {
            minecraft.getItemRenderer().renderGuiItem(stack, x, y);
        }
    }
}
