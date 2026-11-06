package space.xiami.project.mc.mod.xmautotool.gui;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;
import space.xiami.project.mc.mod.xmautotool.config.ProfileManager;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Button;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Guide;
import space.xiami.project.mc.mod.xmautotool.gui.widget.ListBox;
import space.xiami.project.mc.mod.xmautotool.gui.widget.TextField;

import java.util.ArrayList;
import java.util.List;

/** Profile file browser: switch, create, duplicate, rename and delete. */
public final class ProfileScreen extends Screen {

    private final Screen parent;
    private final List<String> ids = new ArrayList<>();
    private ListBox list;
    private int selected = -1;
    private TextField nameBox;
    private Guide guide = new Guide();
    private String status = "";

    public ProfileScreen(Screen parent) {
        super("gui.xmautotool.profiles");
        this.parent = parent;
    }

    @Override
    public void init() {
        super.init();
        ids.clear();
        ids.addAll(ProfileManager.ids());
        selected = Math.max(0, ids.indexOf(ProfileManager.activeId()));
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        guide = new Guide();
        int actionY = height - 66;
        list = new ListBox(10, 46, 180, Math.max(50, actionY - 6 - 46), 18);
        list.setSelected(selected);

        addRenderableWidget(new Button(10, actionY, 70, 18, "切换", b -> switchSelected()));
        addRenderableWidget(new Button(84, actionY, 70, 18, "新建", b -> {
            ProfileManager.createNew();
            status = "已新建并切换";
            refresh();
        }));
        addRenderableWidget(new Button(10, actionY + 22, 70, 18, "复制", b -> {
            ProfileManager.duplicate();
            status = "已复制并切换";
            refresh();
        }));
        Button delete = new Button(84, actionY + 22, 70, 18, "删除", b -> {
            if (selected >= 0 && selected < ids.size()) {
                ProfileManager.delete(ids.get(selected));
                status = "已删除";
                refresh();
            }
        });
        delete.active = ids.size() > 1;
        addRenderableWidget(delete);
        addRenderableWidget(new Button(width - 26, 8, 18, 18, IconRef.item("minecraft:arrow"), "",
                b -> host().openScreen(parent)));
        guide.add(width - 26, 8, 18, 18, "返回主界面");

        guide.add(10, actionY, 70, 18, "载入选中的配置文件（立即生效）");
        guide.add(84, actionY, 70, 18, "新建一个空配置文件并切换过去");
        guide.add(10, actionY + 22, 70, 18, "复制当前配置文件并切换过去");
        guide.add(84, actionY + 22, 70, 18, "删除选中配置文件（至少保留一个）");

        nameBox = new TextField(210, 46, 150, 16, "display name");
        nameBox.setMaxLength(48);
        nameBox.setValue(ProfileManager.data().name);
        addRenderableWidget(nameBox);
        addRenderableWidget(new Button(366, 46, 54, 18, "重命名", b ->
                ProfileManager.renameActive(nameBox.getValue())));
        guide.add(210, 46, 150, 18, "当前配置文件的显示名（文件名不变）");
        guide.add(366, 46, 54, 18, "保存显示名");
    }

    private void switchSelected() {
        if (selected < 0 || selected >= ids.size()) {
            return;
        }
        ProfileManager.switchTo(ids.get(selected));
        status = "已切换到 " + ids.get(selected);
        refresh();
    }

    private void refresh() {
        ids.clear();
        ids.addAll(ProfileManager.ids());
        selected = Math.max(0, ids.indexOf(ProfileManager.activeId()));
        rebuild();
    }

    @Override
    public void render(Canvas canvas, int mouseX, int mouseY, float partialTick) {
        canvas.background();
        drawCentered(canvas, title(), 8, 0xFFFFFF);
        drawString(canvas, "配置文件（每个文件包含全部策略与设置）", 10, 34, 0xA0A0A0);

        if (list != null) {
            list.render(canvas, ids.size(), (c, index, x, y, w, sel) -> {
                String id = ids.get(index);
                String label = (id.equals(ProfileManager.activeId()) ? "* " : "  ") + id;
                drawString(c, label, x + 2, y + 4, sel ? 0xFFFFFF : 0xC0C0C0);
            });
        }

        drawString(canvas, "当前: " + ProfileManager.activeId(), 210, 70, 0x55FF55);
        drawString(canvas, "切换即加载，编辑自动保存", 210, 82, 0x707070);
        if (!status.isEmpty()) {
            drawString(canvas, status, 210, 94, 0x55FF55);
        }
        super.render(canvas, mouseX, mouseY, partialTick);
        guide.render(canvas, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY, int button) {
        if (list != null) {
            int index = list.indexAt(mouseX, mouseY, ids.size());
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
        if (list != null && list.contains(mouseX, mouseY) && list.scrollBy(delta, ids.size())) {
            return true;
        }
        return super.mouseScrolled(canvas, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
