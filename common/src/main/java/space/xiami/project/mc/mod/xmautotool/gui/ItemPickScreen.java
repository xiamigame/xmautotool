package space.xiami.project.mc.mod.xmautotool.gui;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.api.Game;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;
import space.xiami.project.mc.mod.xmautotool.api.ItemView;
import space.xiami.project.mc.mod.xmautotool.core.ToolMatcher;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Button;
import space.xiami.project.mc.mod.xmautotool.gui.widget.ListBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Pick an item from the player's inventory and add it as a tool matcher. */
public final class ItemPickScreen extends Screen {

    private final Screen parent;
    private final Consumer<ToolMatcher> onPick;

    private final List<ItemView> stacks = new ArrayList<>();
    private ListBox list;
    private int selected = -1;
    private String status = "";

    public ItemPickScreen(Screen parent, Consumer<ToolMatcher> onPick) {
        super("gui.xmautotool.pick_item");
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    public void init() {
        super.init();
        collectStacks();
        list = new ListBox(16, 34, width - 32, Math.max(40, height - 96), 18);
        list.setSelected(selected);
        rebuild();
    }

    private void collectStacks() {
        stacks.clear();
        Game game = game();
        if (!game.hasPlayer()) {
            return;
        }
        stacks.addAll(game.inventoryItems());
    }

    private void rebuild() {
        clearWidgets();
        int bottom = height - 24;
        addRenderableWidget(new Button(16, bottom, 90, 18, "加为物品", b -> {
            if (selected >= 0 && selected < stacks.size()) {
                ItemView stack = stacks.get(selected);
                onPick.accept(ToolMatcher.item(stack.itemId()));
                host().openScreen(parent);
            }
        }));
        Button asKind = new Button(110, bottom, 90, 18, "加为类型", b -> {
            if (selected >= 0 && selected < stacks.size()) {
                List<String> kinds = stacks.get(selected).kinds();
                if (kinds.isEmpty()) {
                    status = "这个物品没有可用类型";
                } else {
                    onPick.accept(ToolMatcher.capability(kinds.get(0)));
                    host().openScreen(parent);
                }
            }
        });
        asKind.active = selected >= 0 && selected < stacks.size()
                && !stacks.get(selected).kinds().isEmpty();
        addRenderableWidget(asKind);
        addRenderableWidget(new Button(204, bottom, 70, 18, "取消", b -> host().openScreen(parent)));
        addRenderableWidget(new Button(width - 26, 8, 18, 18, IconRef.item("minecraft:barrier"), "",
                b -> host().openScreen(parent)));
    }

    @Override
    public void render(Canvas canvas, int mouseX, int mouseY, float partialTick) {
        canvas.background();
        drawCentered(canvas, title(), 8, 0xFFFFFF);
        drawString(canvas, "选择背包里的物品（去重）", 16, 24, 0xA0A0A0);
        if (list != null) {
            list.render(canvas, stacks.size(), (c, index, x, y, w, sel) -> {
                ItemView stack = stacks.get(index);
                canvas.item(stack.icon(), x, y - 1);
                String name = stack.name();
                if (stack.count() > 1) {
                    name += " x" + stack.count();
                }
                drawString(c, cut(name, 40), x + 18, y + 4, sel ? 0xFFFFFF : 0xC0C0C0);
            });
        }
        if (!status.isEmpty()) {
            drawString(canvas, status, 16, height - 38, 0xFF5555);
        }
        super.render(canvas, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY, int button) {
        if (list != null) {
            int index = list.indexAt(mouseX, mouseY, stacks.size());
            if (index >= 0) {
                selected = index;
                list.setSelected(index);
                rebuild();
                return true;
            }
        }
        return super.mouseClicked(canvas, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(Canvas canvas, double mouseX, double mouseY, double delta) {
        if (list != null && list.scrollBy(delta, stacks.size())) {
            return true;
        }
        return super.mouseScrolled(canvas, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
