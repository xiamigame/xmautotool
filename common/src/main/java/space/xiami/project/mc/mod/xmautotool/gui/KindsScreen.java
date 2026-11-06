package space.xiami.project.mc.mod.xmautotool.gui;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;
import space.xiami.project.mc.mod.xmautotool.config.ProfileManager;
import space.xiami.project.mc.mod.xmautotool.config.UserKind;
import space.xiami.project.mc.mod.xmautotool.core.ToolKindRegistry;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Button;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Guide;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Icons;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Kinds;
import space.xiami.project.mc.mod.xmautotool.gui.widget.ListBox;
import space.xiami.project.mc.mod.xmautotool.gui.widget.TextField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User-defined tool kinds (hammer, paxel, ...) with parents and colour.
 *
 * <p>Left list + right detail; rows redraw from {@link #data} every frame so
 * parent/colour edits show up immediately.</p>
 */
public final class KindsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;

    private final Screen parent;
    private final List<UserKind> data = new ArrayList<>();

    private int selected = -1;
    private String status = "";

    private ListBox list;
    private TextField colorBox;
    private TextField idBox;
    private Guide guide = new Guide();
    private String error = "";

    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int detailX;
    private int detailW;

    private int nameY;
    private int idY;
    private int parentsHeaderY;
    private int chipsY;
    private int colorY;
    private int expandHeaderY;
    private int expandY;

    public KindsScreen(Screen parent) {
        super("gui.xmautotool.kinds");
        this.parent = parent;
    }

    @Override
    public void init() {
        super.init();
        data.clear();
        data.addAll(ProfileManager.data().kinds);
        selected = data.isEmpty() ? -1 : Math.min(Math.max(selected, 0), data.size() - 1);
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        guide = new Guide();
        colorBox = null;
        idBox = null;

        listX = 10;
        listY = 48;
        listW = Math.max(150, (width - 30) / 2);
        detailX = listX + listW + 10;
        detailW = Math.max(150, width - detailX - 10);
        listH = Math.max(60, height - 110);

        int oldScroll = list == null ? 0 : list.scrollOffset();
        list = new ListBox(listX, listY, listW, listH, ROW_HEIGHT);
        list.setSelected(selected);
        list.setScrollOffset(oldScroll);
        list.clamp(data.size());
        list.ensureVisible(selected, data.size());

        int navY = listY + listH + 6;
        addRenderableWidget(new Button(listX, navY, 80, 18, "新增类型", b -> addKind()));
        Button remove = new Button(listX + 84, navY, 60, 18, "删除", b -> removeSelected());
        remove.active = selected >= 0 && selected < data.size();
        addRenderableWidget(remove);
        guide.add(listX, navY, 80, 18, "新增一个自定义类型（id 自动生成）");
        guide.add(listX + 84, navY, 60, 18, "删除选中类型");

        addRenderableWidget(new Button(width - 26, 8, 18, 18, IconRef.item("minecraft:arrow"), "", b -> {
            syncSelected();
            commit();
            host().openScreen(parent);
        }));
        guide.add(width - 26, 8, 18, 18, "返回并保存");

        if (selected < 0 || selected >= data.size()) {
            return;
        }

        UserKind kind = data.get(selected);
        nameY = listY + 2;
        idY = nameY + 14;
        parentsHeaderY = idY + 30;
        chipsY = parentsHeaderY + 12;

        List<String> options = new ArrayList<>(Kinds.ordered());
        options.remove(kind.id());
        int chipW = Math.min(64, Math.max(40, (detailW - 8) / 3 - 3));
        int chipH = 16;
        int cols = Math.max(1, (detailW - 8) / (chipW + 3));
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            boolean on = kind.parents().contains(option);
            int cx = detailX + 2 + (i % cols) * (chipW + 3);
            int cy = chipsY + (i / cols) * (chipH + 3);
            addRenderableWidget(new Button(cx, cy, chipW, chipH, Icons.forKind(option),
                    Kinds.shortName(option), on, b -> toggleParent(option)));
            guide.add(cx, cy, chipW, chipH, "切换父类型：" + Kinds.label(option));
        }
        int rows = Math.max(1, (options.size() + cols - 1) / cols);
        colorY = chipsY + rows * (chipH + 3) + 6;
        expandHeaderY = colorY + 24;
        expandY = expandHeaderY + 12;

        idBox = new TextField(detailX + 2, idY, detailW - 70, 16, "kind id");
        idBox.setMaxLength(64);
        idBox.setValue(kind.id());
        addRenderableWidget(idBox);
        addRenderableWidget(new Button(detailX + detailW - 64, idY, 62, 18, "改名", b -> renameKind()));
        guide.add(detailX + 2, idY, detailW - 70, 18, "自定义类型 id；改名会同步所有引用");
        guide.add(detailX + detailW - 64, idY, 62, 18, "应用改名，同步策略/覆盖/父类型");

        colorBox = new TextField(detailX + 40, colorY, 56, 16, "color");
        colorBox.setMaxLength(6);
        colorBox.setValue(String.format("%06X", kind.color() & 0xFFFFFF));
        addRenderableWidget(colorBox);
        colorBox.setResponder(text -> syncSelected());
        guide.add(detailX + 40, colorY, 56, 18, "颜色 RRGGBB，只影响列表显示");
    }

    private void renameKind() {
        syncSelected();
        commit();
        if (selected < 0 || selected >= data.size()) {
            return;
        }
        String oldId = data.get(selected).id();
        String failure = ProfileManager.renameKind(oldId, idBox == null ? "" : idBox.getValue());
        if (failure != null) {
            error = failure;
            rebuild();
            return;
        }
        String newId = ProfileManager.normalizeKindId(idBox.getValue());
        data.clear();
        data.addAll(ProfileManager.data().kinds);
        selected = indexOf(newId);
        error = "";
        status = "已改名为 " + newId;
        rebuild();
    }

    private int indexOf(String id) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).id().equals(id)) {
                return i;
            }
        }
        return data.isEmpty() ? -1 : 0;
    }

    private void toggleParent(String parent) {
        syncSelected();
        if (selected < 0 || selected >= data.size()) {
            return;
        }
        UserKind current = data.get(selected);
        List<String> parents = new ArrayList<>(current.parents());
        if (!parents.remove(parent)) {
            parents.add(parent);
        }
        data.set(selected, new UserKind(current.id(), parents, current.color()));
        commit();
        rebuild();
    }

    private void addKind() {
        error = "";
        int n = 1;
        while (exists("xmautotool:custom_" + n)) {
            n++;
        }
        data.add(new UserKind("xmautotool:custom_" + n, List.of(), 0xFFFFFF));
        selected = data.size() - 1;
        commit();
        status = "已新增，记得填父类型";
        rebuild();
    }

    private void removeSelected() {
        if (selected < 0 || selected >= data.size()) {
            return;
        }
        error = "";
        data.remove(selected);
        selected = Math.min(selected, data.size() - 1);
        commit();
        status = "已删除";
        rebuild();
    }

    private void syncSelected() {
        if (selected < 0 || selected >= data.size() || colorBox == null) {
            return;
        }
        int color = parseColor(colorBox.getValue());
        if (color == -1) {
            return;
        }
        UserKind current = data.get(selected);
        data.set(selected, new UserKind(current.id(), current.parents(), color));
    }

    private void commit() {
        ProfileManager.data().kinds.clear();
        ProfileManager.data().kinds.addAll(data);
        ProfileManager.save();
    }

    @Override
    public void render(Canvas canvas, int mouseX, int mouseY, float partialTick) {
        canvas.background();
        drawCentered(canvas, title(), 8, 0xFFFFFF);
        drawString(canvas, "只有要新增锤/镰等类型时才用；父类型决定它算不算镐/斧",
                listX, 24, 0xA0A0A0);
        drawString(canvas, "内置类型: " + builtinNames(), listX, 36, 0x808080);

        canvas.fill(detailX - 1, listY - 1, detailX + detailW + 1, height - 8, 0xFF303030);
        canvas.fill(detailX, listY, detailX + detailW, height - 9, 0xFF181818);

        if (list != null) {
            list.render(canvas, data.size(), (c, index, x, y, w, sel) -> {
                UserKind kind = data.get(index);
                int color = 0xFF000000 | (kind.color() & 0xFFFFFF);
                canvas.fill(x + 1, y + 5, x + 9, y + 13, 0xFF000000);
                canvas.fill(x + 2, y + 6, x + 8, y + 12, color);
                IconRef icon = Icons.forKind(kind.id());
                if (!icon.isEmpty()) {
                    canvas.item(icon, x + 12, y - 1);
                }
                String badge = kind.parents().isEmpty() ? "无父" : "父" + kind.parents().size();
                int badgeWidth = canvas.textWidth(badge);
                drawString(c, cut(kind.id(), Math.max(4, (w - 32 - badgeWidth - 8) / 6)),
                        x + 32, y + 4, sel ? 0xFFFFFF : 0xC0C0C0);
                drawString(c, badge, x + w - badgeWidth - 2, y + 4, sel ? 0xC0D8FF : 0x909090);
            });
        }

        if (selected >= 0 && selected < data.size()) {
            renderDetail(canvas);
        }

        if (!status.isEmpty()) {
            drawString(canvas, status, listX, height - 20, 0x55FF55);
        }
        super.render(canvas, mouseX, mouseY, partialTick);
        guide.render(canvas, mouseX, mouseY);
    }

    private void renderDetail(Canvas canvas) {
        UserKind kind = data.get(selected);
        drawString(canvas, "自定义类型", detailX + 2, nameY, 0xFFFFFF);
        drawString(canvas, error.isEmpty() ? "id 形如 xmautotool:hammer" : error,
                detailX + 2, idY + 20, error.isEmpty() ? 0x707070 : 0xFF5555);
        drawString(canvas, kind.parents().isEmpty() ? "父类型（点击添加）" : "父类型（点击切换）",
                detailX + 2, parentsHeaderY, 0x808080);

        int color = 0xFF000000 | (kind.color() & 0xFFFFFF);
        canvas.fill(detailX + 102, colorY, detailX + 118, colorY + 16, 0xFF000000);
        canvas.fill(detailX + 103, colorY + 1, detailX + 117, colorY + 15, color);
        drawString(canvas, "颜色", detailX + 2, colorY + 5, 0xA0A0A0);

        drawString(canvas, "展开后（引擎视角）", detailX + 2, expandHeaderY, 0x808080);
        drawString(canvas, cut(expansion(kind), Math.max(4, detailW / 6)), detailX + 2, expandY, 0x80C0FF);
        drawString(canvas, "父类型决定它算不算镐/斧；颜色只影响显示", detailX + 2, height - 22, 0x808080);
    }

    @Override
    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY, int button) {
        if (list != null) {
            int index = list.indexAt(mouseX, mouseY, data.size());
            if (index >= 0) {
                syncSelected();
                selected = index;
                error = "";
                rebuild();
                return true;
            }
        }
        return super.mouseClicked(canvas, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(Canvas canvas, double mouseX, double mouseY, double delta) {
        if (list != null && list.contains(mouseX, mouseY) && list.scrollBy(delta, data.size())) {
            return true;
        }
        return super.mouseScrolled(canvas, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        syncSelected();
        commit();
        host().openScreen(parent);
    }

    private String expansion(UserKind kind) {
        Map<String, List<String>> parents = new LinkedHashMap<>(ToolKindRegistry.builtins());
        for (UserKind item : data) {
            parents.put(item.id(), item.parents());
        }
        List<String> chain = ToolKindRegistry.expand(List.of(kind.id()), parents);
        List<String> names = new ArrayList<>(chain.size());
        for (String id : chain) {
            names.add(Kinds.shortName(id));
        }
        return String.join(" → ", names);
    }

    private boolean exists(String id) {
        if (ToolKindRegistry.isBuiltin(id)) {
            return true;
        }
        for (UserKind kind : data) {
            if (kind.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static String builtinNames() {
        List<String> names = new ArrayList<>();
        for (String id : Kinds.ordered()) {
            if (ToolKindRegistry.isBuiltin(id)) {
                names.add(Kinds.shortName(id));
            }
        }
        return String.join(", ", names);
    }

    private static int parseColor(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        String value = text.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String cut(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "…";
    }
}
