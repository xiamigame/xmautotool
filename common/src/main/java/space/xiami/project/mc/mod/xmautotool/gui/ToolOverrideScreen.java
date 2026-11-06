package space.xiami.project.mc.mod.xmautotool.gui;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;
import space.xiami.project.mc.mod.xmautotool.api.ItemView;
import space.xiami.project.mc.mod.xmautotool.config.PolicyRuntime;
import space.xiami.project.mc.mod.xmautotool.config.ProfileManager;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Button;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Guide;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Icons;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Kinds;
import space.xiami.project.mc.mod.xmautotool.gui.widget.ListBox;
import space.xiami.project.mc.mod.xmautotool.gui.widget.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Item to tool-kind overrides, plus the "assume correct" / "speed override"
 * escape hatches for tools that do not expose useful values.
 *
 * <p>Left list + right detail; list rows redraw from {@link #data} every frame,
 * so typing in the detail panel updates the row immediately.</p>
 */
public final class ToolOverrideScreen extends Screen {

    private static final int ROW_HEIGHT = 20;

    private final Screen parent;
    private final List<PolicyRuntime.ToolOverride> data = new ArrayList<>();

    private int selected = -1;
    private String status = "";

    private ListBox list;
    private TextField speedBox;
    private Guide guide = new Guide();

    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int detailX;
    private int detailW;

    private int nameY;
    private int idY;
    private int baseY;
    private int effectiveY;
    private int chipsHeaderY;
    private int chipsY;
    private int assumeY;
    private int speedY;

    public ToolOverrideScreen(Screen parent) {
        super("gui.xmautotool.tool_overrides");
        this.parent = parent;
    }

    @Override
    public void init() {
        super.init();
        data.clear();
        data.addAll(ProfileManager.data().overrides);
        selected = data.isEmpty() ? -1 : Math.min(Math.max(selected, 0), data.size() - 1);
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        guide = new Guide();
        speedBox = null;

        listX = 10;
        listY = 40;
        listW = Math.max(150, (width - 30) / 2);
        detailX = listX + listW + 10;
        detailW = Math.max(150, width - detailX - 10);
        listH = Math.max(60, height - 100);

        int oldScroll = list == null ? 0 : list.scrollOffset();
        list = new ListBox(listX, listY, listW, listH, ROW_HEIGHT);
        list.setSelected(selected);
        list.setScrollOffset(oldScroll);
        list.clamp(data.size());
        list.ensureVisible(selected, data.size());

        int navY = listY + listH + 6;
        addRenderableWidget(new Button(listX, navY, 100, 18, "从手持添加", b -> addHeld()));
        Button remove = new Button(listX + 104, navY, 60, 18, "删除", b -> removeSelected());
        remove.active = selected >= 0 && selected < data.size();
        addRenderableWidget(remove);
        guide.add(listX, navY, 100, 18, "把手上物品加进来，类型取引擎识别结果");
        guide.add(listX + 104, navY, 60, 18, "删除选中覆盖");

        addRenderableWidget(new Button(width - 26, 8, 18, 18, IconRef.item("minecraft:arrow"), "", b -> {
            syncSelected();
            commit();
            host().openScreen(parent);
        }));
        guide.add(width - 26, 8, 18, 18, "返回并保存");

        if (selected < 0 || selected >= data.size()) {
            return;
        }

        PolicyRuntime.ToolOverride override = data.get(selected);
        nameY = listY + 2;
        idY = nameY + 12;
        baseY = idY + 14;
        effectiveY = baseY + 12;
        chipsHeaderY = effectiveY + 14;
        chipsY = chipsHeaderY + 12;

        int chipW = Math.min(64, Math.max(40, (detailW - 8) / 3 - 3));
        int chipH = 16;
        int cols = Math.max(1, (detailW - 8) / (chipW + 3));
        List<String> kinds = Kinds.ordered();
        for (int i = 0; i < kinds.size(); i++) {
            String kind = kinds.get(i);
            boolean on = override.kinds().contains(kind);
            int cx = detailX + 2 + (i % cols) * (chipW + 3);
            int cy = chipsY + (i / cols) * (chipH + 3);
            addRenderableWidget(new Button(cx, cy, chipW, chipH, Icons.forKind(kind),
                    Kinds.shortName(kind), on, b -> toggleKind(kind)));
            guide.add(cx, cy, chipW, chipH, "切换覆盖类型：" + Kinds.label(kind));
        }
        int rows = Math.max(1, (kinds.size() + cols - 1) / cols);
        assumeY = chipsY + rows * (chipH + 3) + 4;
        speedY = assumeY + 22;

        addRenderableWidget(new Button(detailX + 2, assumeY, 110, 18,
                "视为正确: " + (override.assumeCorrect() ? "是" : "否"), b -> {
            syncSelected();
            if (selected < 0 || selected >= data.size()) {
                return;
            }
            PolicyRuntime.ToolOverride current = data.get(selected);
            data.set(selected, new PolicyRuntime.ToolOverride(
                    current.item(), current.kinds(), !current.assumeCorrect(), current.speedOverride()));
            commit();
            rebuild();
        }));
        guide.add(detailX + 2, assumeY, 110, 18, "有些模组工具不报掉落，用这个强制视为正确");

        speedBox = new TextField(detailX + 54, speedY, 56, 16, "speed");
        speedBox.setMaxLength(8);
        speedBox.setValue(override.speedOverride() == null ? "" : trim(override.speedOverride()));
        addRenderableWidget(speedBox);
        speedBox.setResponder(text -> syncSelected());
        guide.add(detailX + 54, speedY, 56, 18, "覆盖物品自报速度，留空=不覆盖");

        addRenderableWidget(new Button(detailX + 116, speedY, 44, 18, "清除", b -> {
            syncSelected();
            if (selected < 0 || selected >= data.size()) {
                return;
            }
            PolicyRuntime.ToolOverride current = data.get(selected);
            data.set(selected, new PolicyRuntime.ToolOverride(
                    current.item(), current.kinds(), current.assumeCorrect(), null));
            commit();
            rebuild();
        }));
        guide.add(detailX + 116, speedY, 44, 18, "清除速度覆盖");
    }

    private void toggleKind(String kind) {
        syncSelected();
        if (selected < 0 || selected >= data.size()) {
            return;
        }
        PolicyRuntime.ToolOverride current = data.get(selected);
        List<String> kinds = new ArrayList<>(current.kinds());
        if (!kinds.remove(kind)) {
            kinds.add(kind);
        }
        data.set(selected, new PolicyRuntime.ToolOverride(
                current.item(), kinds, current.assumeCorrect(), current.speedOverride()));
        commit();
        rebuild();
    }

    private void addHeld() {
        syncSelected();
        if (!game().hasPlayer()) {
            return;
        }
        ItemView stack = game().heldItem();
        if (stack == null) {
            status = "手上没有物品";
            rebuild();
            return;
        }
        String item = stack.itemId();
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).item().equals(item)) {
                selected = i;
                status = "该物品已有覆盖，已选中";
                rebuild();
                return;
            }
        }
        data.add(new PolicyRuntime.ToolOverride(item, stack.kinds(), false, null));
        selected = data.size() - 1;
        commit();
        status = "已添加 " + item;
        rebuild();
    }

    private void removeSelected() {
        if (selected < 0 || selected >= data.size()) {
            return;
        }
        data.remove(selected);
        selected = Math.min(selected, data.size() - 1);
        commit();
        status = "已删除";
        rebuild();
    }

    private void syncSelected() {
        if (selected < 0 || selected >= data.size() || speedBox == null) {
            return;
        }
        PolicyRuntime.ToolOverride current = data.get(selected);
        data.set(selected, new PolicyRuntime.ToolOverride(
                current.item(), current.kinds(), current.assumeCorrect(), parseSpeed(speedBox.getValue())));
    }

    private void commit() {
        ProfileManager.data().overrides.clear();
        ProfileManager.data().overrides.addAll(data);
        ProfileManager.save();
    }

    @Override
    public void render(Canvas canvas, int mouseX, int mouseY, float partialTick) {
        canvas.background();
        drawCentered(canvas, title(), 8, 0xFFFFFF);
        drawString(canvas, "覆盖 = 完全替换引擎识别；速度留空 = 用物品自报", listX, 24, 0xA0A0A0);

        canvas.fill(detailX - 1, listY - 1, detailX + detailW + 1, height - 8, 0xFF303030);
        canvas.fill(detailX, listY, detailX + detailW, height - 9, 0xFF181818);

        if (list != null) {
            list.render(canvas, data.size(), (c, index, x, y, w, sel) -> {
                PolicyRuntime.ToolOverride override = data.get(index);
                IconRef icon = Icons.forItem(override.item());
                if (!icon.isEmpty()) {
                    canvas.item(icon, x, y - 1);
                }
                String name = icon.isEmpty() ? override.item() : game().iconName(icon);
                String badges = badges(override);
                int badgeWidth = canvas.textWidth(badges);
                drawString(c, cut(name, Math.max(4, (w - 18 - badgeWidth - 8) / 6)),
                        x + 18, y + 4, sel ? 0xFFFFFF : 0xC0C0C0);
                if (!badges.isEmpty()) {
                    drawString(c, badges, x + w - badgeWidth - 2, y + 4, sel ? 0xC0D8FF : 0x909090);
                }
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
        PolicyRuntime.ToolOverride override = data.get(selected);
        IconRef icon = Icons.forItem(override.item());
        String name = icon.isEmpty() ? override.item() : game().iconName(icon);
        drawString(canvas, cut(name, Math.max(4, detailW / 6)), detailX + 2, nameY, 0xFFFFFF);
        drawString(canvas, cut(override.item(), Math.max(4, detailW / 6)), detailX + 2, idY, 0x909090);

        List<String> base = icon.isEmpty() ? List.of() : game().baseKinds(override.item());
        List<String> effective = override.kinds().isEmpty() ? base : override.kinds();
        drawString(canvas, cut("引擎识别: " + joinKinds(base), Math.max(4, (detailW - 4) / 6)),
                detailX + 2, baseY, 0xA0A0A0);
        drawString(canvas, cut("覆盖后: " + joinKinds(effective), Math.max(4, (detailW - 4) / 6)),
                detailX + 2, effectiveY, 0x80C0FF);
        drawString(canvas, override.kinds().isEmpty() ? "类型覆盖（点击添加）" : "类型覆盖（点击切换）",
                detailX + 2, chipsHeaderY, 0x808080);
    }

    @Override
    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY, int button) {
        if (list != null) {
            int index = list.indexAt(mouseX, mouseY, data.size());
            if (index >= 0) {
                syncSelected();
                selected = index;
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

    private static String badges(PolicyRuntime.ToolOverride override) {
        List<String> parts = new ArrayList<>(3);
        if (override.assumeCorrect()) {
            parts.add("正确");
        }
        if (override.speedOverride() != null) {
            parts.add(trim(override.speedOverride()) + "×");
        }
        if (!override.kinds().isEmpty()) {
            parts.add(override.kinds().size() + "类");
        }
        return String.join(" · ", parts);
    }

    private static String joinKinds(List<String> kinds) {
        if (kinds.isEmpty()) {
            return "（无）";
        }
        List<String> names = new ArrayList<>(kinds.size());
        for (String kind : kinds) {
            names.add(Kinds.shortName(kind));
        }
        return String.join(" / ", names);
    }

    private static Float parseSpeed(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String trim(float value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static String cut(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "…";
    }
}
