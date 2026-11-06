package space.xiami.project.mc.mod.xmautotool.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.TranslatableComponent;
import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.gui.Screen;

/** Bridges the pure-JVM screen to Minecraft's screen stack and input events. */
public final class MinecraftScreen extends net.minecraft.client.gui.screens.Screen {

    private final Screen ui;

    public MinecraftScreen(Screen ui) {
        super(new TranslatableComponent(ui.titleKey()));
        this.ui = ui;
    }

    private Canvas canvas(PoseStack poseStack) {
        return new McCanvas(this, poseStack);
    }

    @Override
    protected void init() {
        ui.resize(this.width, this.height);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        ui.render(canvas(poseStack), mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return ui.mouseClicked(canvas(new PoseStack()), mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return ui.mouseReleased(canvas(new PoseStack()), mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return ui.mouseDragged(canvas(new PoseStack()), mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return ui.mouseScrolled(canvas(new PoseStack()), mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ui.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (ui.charTyped(c, modifiers)) {
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public void removed() {
        ui.removed();
        super.removed();
    }

    @Override
    public void onClose() {
        ui.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return ui.isPauseScreen();
    }
}
