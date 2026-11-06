package space.xiami.project.mc.mod.xmautotool.gui;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;
import space.xiami.project.mc.mod.xmautotool.config.ProfileManager;
import space.xiami.project.mc.mod.xmautotool.config.ProfileSettings;
import space.xiami.project.mc.mod.xmautotool.core.Action;
import space.xiami.project.mc.mod.xmautotool.core.BlockMatcher;
import space.xiami.project.mc.mod.xmautotool.core.Policy;
import space.xiami.project.mc.mod.xmautotool.core.ToolMatcher;
import space.xiami.project.mc.mod.xmautotool.core.Tri;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Button;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Guide;
import space.xiami.project.mc.mod.xmautotool.gui.widget.ListBox;
import space.xiami.project.mc.mod.xmautotool.gui.widget.TextField;

import java.util.List;
import java.util.function.Consumer;

/**
 * Main screen: policy list on the left, all profile settings and the tool
 * editors on the right. Everything saves automatically.
 */
public final class AutoToolScreen extends Screen {

    private static final String[] NUMBER_LABELS = {"剩余耐久", "提速 %", "评估间隔", "切槽冷却"};

    private final int[] numberLabelX = new int[4];
    private final int[] numberLabelY = new int[4];

    private ListBox policyList;
    private int selected = -1;
    private Guide guide = new Guide();
    private String status = "已自动保存";

    private TextField durabilityBox;
    private TextField speedGainBox;
    private TextField intervalBox;
    private TextField cooldownBox;

    private int rightX;
    private int rightW;
    private int colW;

    public AutoToolScreen() {
        super("gui.xmautotool.title");
    }

    @Override
    public void init() {
        super.init();
        ProfileManager.init();
        List<Policy> policies = ProfileManager.data().policies;
        selected = policies.isEmpty() ? -1 : Math.min(Math.max(selected, 0), policies.size() - 1);
        rebuild();
    }

    private void rebuild() {
        String durability = durabilityBox == null ? null : durabilityBox.getValue();
        String speed = speedGainBox == null ? null : speedGainBox.getValue();
        String interval = intervalBox == null ? null : intervalBox.getValue();
        String cooldown = cooldownBox == null ? null : cooldownBox.getValue();

        clearWidgets();
        guide = new Guide();
        ProfileSettings settings = ProfileManager.settings();

        int listX = 10;
        int listY = 46;
        int listW = 180;
        int actionY = height - 26;
        int listBottom = actionY - 6;
        policyList = new ListBox(listX, listY, listW, Math.max(48, listBottom - listY), 18);
        policyList.setSelected(selected);
        policyList.clamp(ProfileManager.data().policies.size());

        int bx = listX;
        Button newButton = new Button(bx, actionY, 36, 18, "新建", b -> newPolicy());
        bx += 39;
        Button editButton = new Button(bx, actionY, 36, 18, "编辑", b -> editSelected());
        editButton.active = selected >= 0;
        bx += 39;
        Button deleteButton = new Button(bx, actionY, 36, 18, "删除", b -> deleteSelected());
        deleteButton.active = selected >= 0;
        bx += 39;
        Button upButton = new Button(bx, actionY, 30, 18, "^", b -> move(-1));
        upButton.active = selected > 0;
        bx += 33;
        Button downButton = new Button(bx, actionY, 30, 18, "v", b -> move(1));
        downButton.active = selected >= 0 && selected < ProfileManager.data().policies.size() - 1;
        addRenderableWidget(newButton);
        addRenderableWidget(editButton);
        addRenderableWidget(deleteButton);
        addRenderableWidget(upButton);
        addRenderableWidget(downButton);

        guide.add(listX, actionY, 36, 18, "新建一条策略");
        guide.add(listX + 39, actionY, 36, 18, "编辑选中策略");
        guide.add(listX + 78, actionY, 36, 18, "删除选中策略");
        guide.add(listX + 117, actionY, 30, 18, "上移：顺序即优先级");
        guide.add(listX + 147, actionY, 30, 18, "下移：顺序即优先级");

        rightX = 200;
        rightW = Math.max(150, width - rightX - 10);
        colW = (rightW - 6) / 2;

        addRenderableWidget(new Button(rightX, 32, rightW, 18, IconRef.item("minecraft:writable_book"),
                "配置文件: " + cut(ProfileManager.data().name, 16), b -> open(new ProfileScreen(this))));
        guide.add(rightX, 32, rightW, 18, "切换/新建/复制配置文件；切换立即加载");

        int wide = (rightW - 4) / 2;
        addRenderableWidget(new Button(rightX, 54, wide, 18, IconRef.item("minecraft:compass"),
                "重新加载", b -> reload()));
        addRenderableWidget(new Button(rightX + wide + 4, 54, wide, 18, IconRef.EMPTY,
                "恢复默认", b -> restoreDefaults()));
        guide.add(rightX, 54, wide, 18, "丢弃未保存的编辑，从磁盘重新读取");
        guide.add(rightX + wide + 4, 54, wide, 18, "把当前配置文件的设置恢复为默认值");

        addRenderableWidget(toggle(rightX, 78, colW, "总开关", settings.enabled, v -> settings.enabled = v));
        addRenderableWidget(toggle(rightX + colW + 6, 78, colW, "未命中走兜底", settings.fallbackAuto,
                v -> settings.fallbackAuto = v));
        addRenderableWidget(toggle(rightX, 98, colW, "必须能正确采集", settings.requireCorrectTool,
                v -> settings.requireCorrectTool = v));
        addRenderableWidget(toggle(rightX + colW + 6, 98, colW, "挖掘中持续评估", settings.triggerWhileMining,
                v -> settings.triggerWhileMining = v));
        addRenderableWidget(toggle(rightX, 118, colW, "恢复槽位", settings.restoreSlotAfterMining,
                v -> settings.restoreSlotAfterMining = v));
        addRenderableWidget(toggle(rightX + colW + 6, 118, colW, "展示引导", settings.guide,
                v -> settings.guide = v));

        guide.add(rightX, 78, colW, 18, "关闭后完全不自动切换");
        guide.add(rightX + colW + 6, 78, colW, 18, "未命中时按正确性+速度兜底");
        guide.add(rightX, 98, colW, 18, "只有能正确采集的工具才会被切换");
        guide.add(rightX + colW + 6, 98, colW, 18, "按住左键挖矿时按间隔重新评估");
        guide.add(rightX, 118, colW, 18, "挖完自动切回原来的槽位");
        guide.add(rightX + colW + 6, 118, colW, 18, "悬停时显示按钮/输入框说明");

        numberLabelX[0] = rightX;
        numberLabelY[0] = 142;
        numberLabelX[1] = rightX + colW + 6;
        numberLabelY[1] = 142;
        numberLabelX[2] = rightX;
        numberLabelY[2] = 164;
        numberLabelX[3] = rightX + colW + 6;
        numberLabelY[3] = 164;

        durabilityBox = field(numberLabelX[0], numberLabelY[0], durability, settings.minDurability);
        speedGainBox = field(numberLabelX[1], numberLabelY[1], speed, settings.minSpeedGainPercent);
        intervalBox = field(numberLabelX[2], numberLabelY[2], interval, settings.miningEvalInterval);
        cooldownBox = field(numberLabelX[3], numberLabelY[3], cooldown, settings.switchCooldownTicks);

        guide.add(rightX, 142, colW, 18, "剩余耐久低于该值不参选（0=不检查）");
        guide.add(rightX + colW + 6, 142, colW, 18, "新工具需快过当前工具该百分比才切换");
        guide.add(rightX, 164, colW, 18, "挖掘中评估间隔（tick）");
        guide.add(rightX + colW + 6, 164, colW, 18, "两次自动切槽最小间隔（tick）");

        int navW = (rightW - 8) / 3;
        addRenderableWidget(new Button(rightX, 188, navW, 18, IconRef.item("minecraft:name_tag"),
                "工具识别", b -> open(new ToolOverrideScreen(this))));
        addRenderableWidget(new Button(rightX + navW + 4, 188, navW, 18, IconRef.item("minecraft:iron_pickaxe"),
                "工具类型", b -> open(new KindsScreen(this))));
        addRenderableWidget(new Button(rightX + 2 * (navW + 4), 188, navW, 18, IconRef.item("minecraft:book"),
                "说明", b -> open(new ExplainScreen(this))));

        guide.add(rightX, 188, navW, 18, "给识别不到的工具指定类型");
        guide.add(rightX + navW + 4, 188, navW, 18, "新增锤/镰等工具类型");
        guide.add(rightX + 2 * (navW + 4), 188, navW, 18, "查看全部概念与用法说明");

        addRenderableWidget(new Button(width - 26, 8, 18, 18, IconRef.item("minecraft:barrier"), "", b -> onClose()));
        guide.add(width - 26, 8, 18, 18, "关闭界面（自动保存）");
    }

    private Button toggle(int x, int y, int width, String label, boolean value, Consumer<Boolean> setter) {
        return new Button(x, y, width, 18, label + ": " + (value ? "开" : "关"), b -> {
            setter.accept(!value);
            ProfileManager.save();
            status = "已自动保存";
            rebuild();
        });
    }

    private TextField field(int x, int y, String pending, int value) {
        TextField box = new TextField(x + 52, y, colW - 52, 16, "value");
        box.setMaxLength(6);
        box.setValue(pending == null ? Integer.toString(value) : pending);
        addRenderableWidget(box);
        box.setResponder(text -> syncNumbers());
        return box;
    }

    private void syncNumbers() {
        if (durabilityBox == null) {
            return;
        }
        ProfileSettings settings = ProfileManager.settings();
        settings.minDurability = parse(durabilityBox, settings.minDurability, 0, 100000);
        settings.minSpeedGainPercent = parse(speedGainBox, settings.minSpeedGainPercent, 0, 200);
        settings.miningEvalInterval = parse(intervalBox, settings.miningEvalInterval, 1, 10);
        settings.switchCooldownTicks = parse(cooldownBox, settings.switchCooldownTicks, 0, 20);
    }

    private void open(Screen screen) {
        syncNumbers();
        ProfileManager.save();
        host().openScreen(screen);
    }

    private void reload() {
        durabilityBox = null;
        speedGainBox = null;
        intervalBox = null;
        cooldownBox = null;
        ProfileManager.reload();
        selected = -1;
        status = "已重新加载";
        rebuild();
    }

    private void restoreDefaults() {
        durabilityBox = null;
        speedGainBox = null;
        intervalBox = null;
        cooldownBox = null;
        ProfileManager.data().settings = new ProfileSettings();
        ProfileManager.save();
        status = "已恢复默认设置";
        rebuild();
    }

    private void newPolicy() {
        List<Policy> policies = ProfileManager.data().policies;
        int n = 1;
        while (hasId(policies, "policy_" + n)) {
            n++;
        }
        Policy policy = new Policy("policy_" + n, "新策略 " + n, true,
                List.of(BlockMatcher.id("minecraft:stone")),
                Action.USE,
                List.of(ToolMatcher.anyTool()),
                Tri.INHERIT);
        policies.add(policy);
        ProfileManager.save();
        selected = policies.size() - 1;
        open(new PolicyEditScreen(this, selected));
    }

    private void editSelected() {
        if (selected < 0 || selected >= ProfileManager.data().policies.size()) {
            return;
        }
        open(new PolicyEditScreen(this, selected));
    }

    private void deleteSelected() {
        List<Policy> policies = ProfileManager.data().policies;
        if (selected < 0 || selected >= policies.size()) {
            return;
        }
        policies.remove(selected);
        ProfileManager.save();
        selected = Math.min(selected, policies.size() - 1);
        status = "已删除";
        rebuild();
    }

    private void move(int delta) {
        List<Policy> policies = ProfileManager.data().policies;
        int target = selected + delta;
        if (selected < 0 || selected >= policies.size() || target < 0 || target >= policies.size()) {
            return;
        }
        Policy tmp = policies.get(selected);
        policies.set(selected, policies.get(target));
        policies.set(target, tmp);
        ProfileManager.save();
        selected = target;
        rebuild();
    }

    @Override
    public void render(Canvas canvas, int mouseX, int mouseY, float partialTick) {
        canvas.background();
        drawCentered(canvas, title(), 8, 0xFFFFFF);
        drawString(canvas, "策略（顺序即优先级）", 10, 24, 0xA0A0A0);
        drawString(canvas, "状态: " + status, 10, 34, 0x55FF55);

        if (policyList != null) {
            List<Policy> policies = ProfileManager.data().policies;
            policyList.render(canvas, policies.size(), (c, index, x, y, w, sel) -> {
                Policy policy = policies.get(index);
                String label = (policy.enabled() ? "" : "[off] ") + policy.name() + "  " + policy.action();
                drawString(c, cut(label, (w - 6) / 6), x + 2, y + 4, sel ? 0xFFFFFF : 0xC0C0C0);
            });
        }

        for (int i = 0; i < NUMBER_LABELS.length; i++) {
            drawString(canvas, NUMBER_LABELS[i], numberLabelX[i], numberLabelY[i] + 5, 0xA0A0A0);
        }

        super.render(canvas, mouseX, mouseY, partialTick);
        guide.render(canvas, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY, int button) {
        syncNumbers();
        if (policyList != null) {
            int index = policyList.indexAt(mouseX, mouseY, ProfileManager.data().policies.size());
            if (index >= 0) {
                selected = index;
                policyList.setSelected(index);
                rebuild();
                return true;
            }
        }
        return super.mouseClicked(canvas, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(Canvas canvas, double mouseX, double mouseY, double delta) {
        if (policyList != null && policyList.contains(mouseX, mouseY)
                && policyList.scrollBy(delta, ProfileManager.data().policies.size())) {
            return true;
        }
        return super.mouseScrolled(canvas, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        syncNumbers();
        ProfileManager.save();
        super.removed();
    }

    @Override
    public void onClose() {
        syncNumbers();
        ProfileManager.save();
        super.onClose();
    }

    private static int parse(TextField box, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(box.getValue().trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean hasId(List<Policy> policies, String id) {
        for (Policy policy : policies) {
            if (policy.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static String cut(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "…";
    }
}
