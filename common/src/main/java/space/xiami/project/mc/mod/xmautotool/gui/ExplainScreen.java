package space.xiami.project.mc.mod.xmautotool.gui;

import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Button;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Guide;
import space.xiami.project.mc.mod.xmautotool.gui.widget.ListBox;

import java.util.ArrayList;
import java.util.List;

/** Long-form explanation of every concept in the editor. */
public final class ExplainScreen extends Screen {

    private static final int LINE_HEIGHT = 11;

    private final Screen parent;
    private final List<Section> sections = sections();

    private final List<String> lines = new ArrayList<>();
    private ListBox list;
    private int selected;
    private int page;
    private int pageCount = 1;
    private Guide guide = new Guide();
    private Button prevButton;
    private Button nextButton;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int wrappedWidth = -1;

    public ExplainScreen(Screen parent) {
        super("说明");
        this.parent = parent;
    }

    @Override
    public void init() {
        super.init();
        selected = Math.max(0, Math.min(selected, sections.size() - 1));
        wrappedWidth = -1;

        int listX = 10;
        int listY = 40;
        int listW = 150;
        int listH = Math.max(60, height - 80);
        list = new ListBox(listX, listY, listW, listH, 18);
        list.setSelected(selected);

        panelX = listX + listW + 10;
        panelY = 40;
        panelW = Math.max(120, width - panelX - 10);
        panelH = Math.max(60, height - 80);

        prevButton = new Button(panelX, height - 24, 60, 18, "上一页", b -> turn(-1));
        nextButton = new Button(panelX + 64, height - 24, 60, 18, "下一页", b -> turn(1));
        addRenderableWidget(prevButton);
        addRenderableWidget(nextButton);

        addRenderableWidget(new Button(width - 26, 8, 18, 18, IconRef.item("minecraft:arrow"), "",
                b -> host().openScreen(parent)));
        guide.add(width - 26, 8, 18, 18, "返回主界面");
    }

    private void rewrap(Canvas canvas) {
        lines.clear();
        int maxWidth = Math.max(40, panelW - 8);
        for (String paragraph : sections.get(selected).body().split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            String rest = paragraph;
            while (!rest.isEmpty()) {
                int fit = fitWidth(canvas, rest, maxWidth);
                if (fit >= rest.length()) {
                    lines.add(rest);
                    break;
                }
                int cut = Math.max(1, fit);
                int space = rest.lastIndexOf(' ', cut);
                if (space > 0) {
                    cut = space;
                }
                lines.add(rest.substring(0, cut));
                rest = rest.substring(cut).trim();
            }
        }
        int perPage = Math.max(1, (panelH - 4) / LINE_HEIGHT);
        pageCount = Math.max(1, (lines.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pageCount - 1));
        prevButton.active = page > 0;
        nextButton.active = page < pageCount - 1;
        wrappedWidth = panelW;
    }

    private int fitWidth(Canvas canvas, String text, int maxWidth) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            int charWidth = canvas.textWidth(String.valueOf(text.charAt(i)));
            if (width + charWidth > maxWidth) {
                return i;
            }
            width += charWidth;
        }
        return text.length();
    }

    private void turn(int delta) {
        int next = Math.max(0, Math.min(pageCount - 1, page + delta));
        if (next != page) {
            page = next;
            prevButton.active = page > 0;
            nextButton.active = page < pageCount - 1;
        }
    }

    @Override
    public void render(Canvas canvas, int mouseX, int mouseY, float partialTick) {
        if (wrappedWidth != panelW) {
            rewrap(canvas);
        }
        canvas.background();
        drawCentered(canvas, title(), 8, 0xFFFFFF);
        drawString(canvas, "所有概念与用法的完整说明，左侧选章节，右侧翻页", 10, 24, 0xA0A0A0);

        canvas.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, 0xFF303030);
        canvas.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF181818);

        if (list != null) {
            list.render(canvas, sections.size(), (c, index, x, y, w, sel) -> {
                String title = sections.get(index).title();
                drawString(c, cut(title, (w - 6) / 6), x + 2, y + 4, sel ? 0xFFFFFF : 0xC0C0C0);
            });
        }

        int perPage = Math.max(1, (panelH - 4) / LINE_HEIGHT);
        int first = page * perPage;
        int y = panelY + 4;
        for (int i = first; i < Math.min(lines.size(), first + perPage); i++) {
            drawString(canvas, lines.get(i), panelX + 4, y, 0xE0E0E0);
            y += LINE_HEIGHT;
        }

        drawString(canvas, (page + 1) + "/" + pageCount, panelX + 132, height - 19, 0x808080);
        super.render(canvas, mouseX, mouseY, partialTick);
        guide.render(canvas, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY, int button) {
        if (list != null) {
            int index = list.indexAt(mouseX, mouseY, sections.size());
            if (index >= 0) {
                selected = index;
                page = 0;
                list.setSelected(index);
                wrappedWidth = -1;
                return true;
            }
        }
        return super.mouseClicked(canvas, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(Canvas canvas, double mouseX, double mouseY, double delta) {
        if (list != null && list.contains(mouseX, mouseY) && list.scrollBy(delta, sections.size())) {
            return true;
        }
        if (mouseX >= panelX && mouseX <= panelX + panelW
                && mouseY >= panelY && mouseY <= panelY + panelH) {
            turn(delta > 0 ? 1 : -1);
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
        host().openScreen(parent);
    }

    private static String cut(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "…";
    }

    private record Section(String title, String body) {
    }

    private static List<Section> sections() {
        return List.of(
                new Section("快速开始", """
                        按 G 打开主界面。
                        左侧是策略列表，右侧是行为设置、工具页与说明入口。
                        一条策略 = 适用的方块 + 允许的工具 + 动作。
                        策略从上到下匹配，第一条命中的生效。
                        没有命中时，若开启“未命中走兜底”，会按能否正确采集和速度自动选。
                        所有编辑自动保存：开关立即保存，文本在关闭/切换时保存。
                        """),
                new Section("策略与动作", """
                        USE（使用）：在允许的工具里按正确性与速度挑一个，并用提速阈值做迟滞。
                        NEVER（不切）：命中后保持当前工具，不自动切换。
                        FORCE（强制）：必须切到指定工具；只接受槽位、名称、指纹、物品这几种具体匹配器。
                        门槛：继承 / 必须 / 不要求。它决定这次是否要求“工具能正确采集（有掉落）”。
                        权重：同一个工具被多条工具匹配器命中时取最大权重，权重加到评分里。
                        兜底：所有策略都不命中时，可选的自动选择；它不受类型/动作影响。
                        """),
                new Section("方块圈定", """
                        id：精确方块 id，例如 minecraft:stone。
                        tag：方块标签，例如 minecraft:logs；输入时带不带 # 都可以。
                        mod：模组命名空间，例如 create:*。
                        states：方块状态，例如 axis=y；逗号分隔，留空表示不限制。
                        列表里任意一条匹配即可；顺序不影响命中。
                        框定可以用“目视方块”“手持方块”“输入ID”添加。
                        """),
                new Section("工具圈定与权重", """
                        自动（any）：不限定工具，按正确性与速度挑。
                        物品（item）：精确物品 id。
                        槽位（slot）：快捷栏 0 到 8。
                        名称（nameContains）：物品显示名包含指定文字。
                        类型（cap）：匹配该类型及其所有父类型，适合“镐/斧”这种大类。
                        类型ID（type）：只匹配直接类型，不看父类型。
                        指纹（fingerprint）：物品 id + NBT 的 SHA-1，适合区分同 id 但 NBT 不同的工具。
                        NBT（nbt）：按 NBT 键值精确匹配，目前只有 JSON 入口。
                        一条策略里任意一个工具匹配器允许该工具即可；权重取最大值。
                        FORCE 只接受具体匹配器（槽位/名称/指纹/物品）。
                        """),
                new Section("工具类型与继承", """
                        内置类型：镐、斧、锹、锄、剑、剪刀；它们的父类型固定。
                        自定义类型：可以新增（锤、镐斧锹、镰刀等），并指定父类型。
                        类型（cap）会沿父类型向上展开，所以“锤”把父类型设为“镐”后，按镐也能用它。
                        类型ID（type）只看直接类型，不会继承。
                        自定义类型可以改名；改名会同步所有引用它的策略、工具覆盖、父类型与黑名单。
                        删除仍被策略引用的类型后，那条策略会匹配不到工具。
                        颜色只用于列表显示，不参与引擎判定。
                        """),
                new Section("工具识别", """
                        用途：给引擎认不出或认错的物品指定类型。
                        覆盖会完全替换引擎识别结果，而不是追加。
                        识别顺序：用户覆盖 → Forge ToolActions → 原版类判断。
                        “视为正确”用于物品不报告“能正确采集”的情况，会强制当作正确。
                        “速度覆盖”用于物品自报速度不可信的情况，留空表示用游戏值。
                        示例：模组锤不报 ToolActions，也没有继承 PickaxeItem，就给它指定锤类型。
                        """),
                new Section("指纹与 NBT", """
                        指纹 = SHA-1(物品 id + 清理后的 NBT)。
                        计算前会去掉 Damage（耐久）与 RepairCost，所以耐久变化不影响指纹。
                        NBT 的其他变化（附魔、自定义标签、名称等）会改变指纹，旧指纹随即失效。
                        NBT 匹配器使用扁平化的 a.b.c=值 精确比较，同样忽略 Damage/RepairCost。
                        指纹适合“只认这一把/这一种配置”的工具；NBT 适合精确匹配某个标签。
                        整合包更新或标签变化会导致指纹漂移，这是正常现象。
                        """),
                new Section("设置项", """
                        总开关：关闭后完全不自动切换。
                        未命中走兜底：没有策略命中时按正确性+速度自动选。
                        必须能正确采集：全局默认门槛，策略里可以用三态覆盖。
                        挖掘中持续评估：按住左键挖矿时按间隔重新评估。
                        恢复槽位：挖完自动切回原来的槽位。
                        展示引导：鼠标悬停时显示按钮/输入框说明。
                        剩余耐久：低于该值不参选（0=不检查）。
                        提速 %：新工具速度需超过当前工具该百分比才切换（迟滞）。
                        评估间隔：挖掘中评估间隔（tick）。
                        切槽冷却：两次自动切槽的最小间隔（tick）。
                        """),
                new Section("配置文件", """
                        所有用户数据都在 config/xmautotool/profiles/<id>.json：
                        settings、policies、overrides、kinds、黑白名单。
                        切换配置文件会立即加载；结构修改立即写盘，文本输入在返回/切换时写盘。
                        config/xmautotool-client.toml 只保留 activeProfile 与 debug。
                        旧版 config/xmautotool/policies.json 首次启动会迁移到 profiles/default.json。
                        """),
                new Section("快捷键与触发", """
                        G：打开/关闭主界面。
                        抑制键：按住时暂停自动切换（可在原版按键设置里改）。
                        切槽会受冷却限制，避免连续切换被服务器判定异常。
                        开界面、旁观、使用物品、睡觉、按住抑制键时不会自动切换。
                        左键触发固定开启；挖掘中持续评估可以单独关闭。
                        """),
                new Section("已知边界", """
                        只适配 1.18.2 + Forge 40.x，不针对任何整合包写专用代码。
                        nbt、weight、方块/工具黑白名单目前主要走 JSON 入口。
                        内置类型的父类型不能在界面里改（JSON 里可以）。
                        整合包工具不报告 ToolActions 时，请用工具识别指定类型。
                        指纹依赖 NBT；NBT 常变的物品建议改用槽位/物品/名称匹配。
                        """));
    }
}
