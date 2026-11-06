package space.xiami.project.mc.mod.xmautotool.gui;

import space.xiami.project.mc.mod.xmautotool.api.BlockView;
import space.xiami.project.mc.mod.xmautotool.api.Canvas;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;
import space.xiami.project.mc.mod.xmautotool.api.ItemView;
import space.xiami.project.mc.mod.xmautotool.config.ProfileManager;
import space.xiami.project.mc.mod.xmautotool.core.Action;
import space.xiami.project.mc.mod.xmautotool.core.BlockMatcher;
import space.xiami.project.mc.mod.xmautotool.core.Policy;
import space.xiami.project.mc.mod.xmautotool.core.ToolMatcher;
import space.xiami.project.mc.mod.xmautotool.core.Tri;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Autocomplete;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Button;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Guide;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Icons;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Kinds;
import space.xiami.project.mc.mod.xmautotool.gui.widget.ListBox;
import space.xiami.project.mc.mod.xmautotool.gui.widget.Suggestions;
import space.xiami.project.mc.mod.xmautotool.gui.widget.TextField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Two-column policy editor: blocks on the left, tools on the right, both as
 * icon lists with prefix suggestions for the id fields.
 */
public final class PolicyEditScreen extends Screen {

    private static final String[] BLOCK_TYPES = {"id", "tag", "mod"};
    private static final String[] TOOL_TYPES = {"cap", "type", "item", "slot", "name", "fp", "any"};

    private final Screen parent;
    private final int index;

    private final List<BlockMatcher> when = new ArrayList<>();
    private final List<ToolMatcher> tools = new ArrayList<>();

    private String name = "";
    private Action action = Action.USE;
    private Tri gate = Tri.INHERIT;
    private boolean enabled = true;
    private boolean loaded;

    private ListBox blockList;
    private ListBox toolList;
    private int blockSelected = -1;
    private int toolSelected = -1;

    private String blockType = "id";
    private String toolType = "cap";

    private TextField nameBox;
    private TextField blockValueBox;
    private TextField blockStatesBox;
    private TextField toolValueBox;
    private Autocomplete blockAutocomplete;
    private Autocomplete toolAutocomplete;
    private Guide guide = new Guide();

    private String error = "";
    private int blockX;
    private int toolX;
    private int columnWidth;

    public PolicyEditScreen(Screen parent, int index) {
        super("gui.xmautotool.policy_editor");
        this.parent = parent;
        this.index = index;
    }

    @Override
    public void init() {
        super.init();
        if (!loaded) {
            List<Policy> policies = ProfileManager.data().policies;
            if (index >= 0 && index < policies.size()) {
                Policy policy = policies.get(index);
                name = policy.name();
                action = policy.action();
                gate = policy.requireCorrectTool();
                enabled = policy.enabled();
                when.clear();
                when.addAll(policy.when());
                tools.clear();
                tools.addAll(policy.tools());
                if (!when.isEmpty()) {
                    blockSelected = 0;
                    blockType = typeOf(when.get(0));
                }
                if (!tools.isEmpty()) {
                    toolSelected = 0;
                    toolType = typeOf(tools.get(0));
                }
            }
            loaded = true;
        }
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        blockAutocomplete = null;
        toolAutocomplete = null;
        guide = new Guide();

        blockX = 8;
        columnWidth = Math.max(170, (width - 28) / 2);
        toolX = blockX + columnWidth + 12;

        blockList = new ListBox(blockX, 52, columnWidth, 90, 18);
        toolList = new ListBox(toolX, 52, columnWidth, 90, 18);
        blockSelected = clamp(blockSelected, when.size());
        toolSelected = clamp(toolSelected, tools.size());
        blockList.setSelected(blockSelected);
        toolList.setSelected(toolSelected);

        nameBox = new TextField(blockX, 24, 100, 16, "name");
        nameBox.setMaxLength(64);
        nameBox.setValue(name);
        addRenderableWidget(nameBox);
        nameBox.setResponder(text -> name = text);

        addRenderableWidget(new Button(blockX + 104, 24, 66, 18,
                "动作 " + actionLabel(action), b -> {
            syncEditor();
            action = nextAction(action);
            rebuild();
        }));
        addRenderableWidget(new Button(blockX + 174, 24, 76, 18,
                "门槛 " + gateLabel(gate), b -> {
            syncEditor();
            gate = nextGate(gate);
            rebuild();
        }));
        addRenderableWidget(new Button(blockX + 254, 24, 54, 18,
                enabled ? "启用" : "停用", b -> {
            syncEditor();
            enabled = !enabled;
            rebuild();
        }));

        buildAddButtons();
        buildEditorWidgets();
        addGuideHints();

        addRenderableWidget(new Button(width - 26, 8, 18, 18, IconRef.item("minecraft:arrow"), "", b -> back()));
        guide.add(width - 26, 8, 18, 18, "返回并保存（文本修改在此时写入文件）");
    }

    private void buildAddButtons() {
        int y = 148;
        addRenderableWidget(new Button(blockX, y, 48, 18, "目视方块", b -> addLookingBlock()));
        addRenderableWidget(new Button(blockX + 52, y, 48, 18, "输入ID", b -> addBlock(BlockMatcher.id("minecraft:stone"))));
        addRenderableWidget(new Button(blockX + 104, y, 48, 18, "手持方块", b -> addHeldBlock()));

        addRenderableWidget(new Button(toolX, y, 44, 18, "手持", b -> addHeldTool()));
        addRenderableWidget(new Button(toolX + 48, y, 44, 18, "背包", b -> openItemPicker()));
        addRenderableWidget(new Button(toolX + 96, y, 44, 18, "自动", b -> addTool(ToolMatcher.anyTool())));
    }

    private void buildEditorWidgets() {
        if (blockSelected >= 0 && blockSelected < when.size()) {
            BlockMatcher matcher = when.get(blockSelected);
            int typeW = 40;
            int deleteW = 30;
            int gap = 2;
            int inputsW = columnWidth - typeW - deleteW - gap * 3;
            int valueW = Math.max(28, (int) (inputsW * 0.6D));
            int statesW = Math.max(24, inputsW - valueW);
            int valueX = blockX + typeW + gap;
            int statesX = valueX + valueW + gap;
            int deleteX = blockX + columnWidth - deleteW;

            addRenderableWidget(new Button(blockX, 168, typeW, 18,
                    blockTypeLabel(blockType), b -> {
                blockType = nextBlockType(blockType);
                syncEditor();
                rebuild();
                persist();
            }));
            blockValueBox = new TextField(valueX, 168, valueW, 16, "value");
            blockValueBox.setMaxLength(200);
            blockValueBox.setValue(blockValue(matcher, blockType));
            addRenderableWidget(blockValueBox);

            blockStatesBox = new TextField(statesX, 168, statesW, 16, "states");
            blockStatesBox.setMaxLength(200);
            blockStatesBox.setValue(statesString(matcher.states()));
            addRenderableWidget(blockStatesBox);
            blockStatesBox.setResponder(text -> syncEditor());
            addRenderableWidget(new Button(deleteX, 168, deleteW, 18,
                    "删除", b -> removeBlock()));

            blockAutocomplete = new Autocomplete(blockValueBox, valueX, 168, valueW,
                    () -> switch (blockType) {
                        case "tag" -> Suggestions.blockTags();
                        case "mod" -> Suggestions.mods();
                        default -> Suggestions.blockIds();
                    }, this::syncEditor);
        }

        if (toolSelected >= 0 && toolSelected < tools.size()) {
            ToolMatcher matcher = tools.get(toolSelected);
            addRenderableWidget(new Button(toolX, 168, 52, 18,
                    toolTypeLabel(toolType), b -> {
                toolType = nextToolType(toolType);
                syncEditor();
                rebuild();
                persist();
            }));
            toolValueBox = new TextField(toolX + 56, 168, columnWidth - 98, 16, "value");
            toolValueBox.setMaxLength(200);
            toolValueBox.setValue(toolValue(matcher, toolType));
            addRenderableWidget(toolValueBox);
            addRenderableWidget(new Button(toolX + columnWidth - 38, 168, 38, 18,
                    "删除", b -> removeTool()));

            toolAutocomplete = new Autocomplete(toolValueBox, toolX + 56, 168, columnWidth - 98,
                    () -> switch (toolType) {
                        case "item" -> Suggestions.itemIds();
                        case "cap", "type" -> Suggestions.kinds();
                        default -> List.of();
                    }, this::syncEditor);
        }
    }

    // ------------------------------------------------------------------ editing

    private void syncEditor() {
        if (blockSelected >= 0 && blockSelected < when.size() && blockValueBox != null) {
            when.set(blockSelected, buildBlockFromEditor());
        }
        if (toolSelected >= 0 && toolSelected < tools.size() && toolValueBox != null) {
            tools.set(toolSelected, buildToolFromEditor());
        }
    }

    private BlockMatcher buildBlockFromEditor() {
        String value = blockValueBox.getValue().trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        Map<String, String> states = parseStates(blockStatesBox.getValue());
        String v = blankToNull(value);
        return switch (blockType) {
            case "tag" -> new BlockMatcher(null, v, null, states);
            case "mod" -> new BlockMatcher(null, null, v, states);
            default -> new BlockMatcher(v, null, null, states);
        };
    }

    private ToolMatcher buildToolFromEditor() {
        String value = blankToNull(toolValueBox.getValue());
        return switch (toolType) {
            case "item" -> ToolMatcher.item(value);
            case "slot" -> parseSlot(value) == null ? ToolMatcher.anyTool() : ToolMatcher.slot(parseSlot(value));
            case "name" -> ToolMatcher.name(value);
            case "type" -> value == null ? ToolMatcher.anyTool() : ToolMatcher.type(value);
            case "cap" -> value == null ? ToolMatcher.anyTool() : ToolMatcher.capability(value);
            case "fp" -> new ToolMatcher(null, null, value, null, null, null, Map.of(), false, 0);
            default -> ToolMatcher.anyTool();
        };
    }

    private void addBlock(BlockMatcher matcher) {
        syncEditor();
        error = "";
        when.add(matcher);
        blockSelected = when.size() - 1;
        blockType = typeOf(matcher);
        rebuild();
        persist();
    }

    private void addTool(ToolMatcher matcher) {
        syncEditor();
        error = "";
        tools.add(matcher);
        toolSelected = tools.size() - 1;
        toolType = typeOf(matcher);
        rebuild();
        persist();
    }

    private void removeBlock() {
        if (blockSelected < 0 || blockSelected >= when.size()) {
            return;
        }
        syncEditor();
        when.remove(blockSelected);
        blockSelected = Math.min(blockSelected, when.size() - 1);
        if (blockSelected >= 0) {
            blockType = typeOf(when.get(blockSelected));
        }
        rebuild();
        persist();
    }

    private void removeTool() {
        if (toolSelected < 0 || toolSelected >= tools.size()) {
            return;
        }
        syncEditor();
        tools.remove(toolSelected);
        toolSelected = Math.min(toolSelected, tools.size() - 1);
        if (toolSelected >= 0) {
            toolType = typeOf(tools.get(toolSelected));
        }
        rebuild();
        persist();
    }

    private void addLookingBlock() {
        BlockView block = game().lookingBlock();
        if (block == null) {
            error = "准星没有对着方块";
            return;
        }
        error = "";
        addBlock(new BlockMatcher(block.id(), null, null, block.states()));
    }

    private void addHeldBlock() {
        if (!game().hasPlayer()) {
            return;
        }
        ItemView held = game().heldItem();
        if (held == null) {
            error = "手上没有物品";
            return;
        }
        BlockView block = game().heldBlock();
        if (block == null) {
            error = "手持物品不是方块";
            return;
        }
        error = "";
        addBlock(new BlockMatcher(block.id(), null, null, Map.of()));
    }

    private void addHeldTool() {
        if (!game().hasPlayer()) {
            return;
        }
        ItemView held = game().heldItem();
        if (held == null) {
            error = "手上没有物品";
            return;
        }
        List<String> kinds = held.kinds();
        if (kinds.isEmpty()) {
            addTool(ToolMatcher.item(held.itemId()));
        } else {
            addTool(ToolMatcher.capability(kinds.get(0)));
        }
    }

    private void openItemPicker() {
        syncEditor();
        host().openScreen(new ItemPickScreen(this, matcher -> {
            tools.add(matcher);
            toolSelected = tools.size() - 1;
            toolType = typeOf(matcher);
        }));
    }

    private void persist() {
        if (index < 0) {
            return;
        }
        syncEditor();
        List<Policy> policies = ProfileManager.data().policies;
        if (index >= policies.size()) {
            return;
        }
        String id = policies.get(index).id();
        Policy updated = new Policy(id, name.isBlank() ? id : name, enabled, when, action, tools, gate);
        String invalid = updated.invalidReason();
        if (invalid != null) {
            error = "无法保存: " + invalid;
            return;
        }
        policies.set(index, updated);
        error = "";
        ProfileManager.save();
    }

    private void back() {
        persist();
        if (error.isEmpty()) {
            host().openScreen(parent);
        } else {
            rebuild();
        }
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(Canvas canvas, int mouseX, int mouseY, float partialTick) {
        canvas.background();
        drawCentered(canvas, title(), 8, 0xFFFFFF);
        drawString(canvas, "方块圈定（左） " + when.size(), blockX, 42, 0xA0A0A0);
        drawString(canvas, "工具圈定（右） " + tools.size(), toolX, 42, 0xA0A0A0);

        renderBlockList(canvas);
        renderToolList(canvas);

        drawString(canvas, "id/tag/mod + 状态", blockX, 210, 0x707070);
        drawString(canvas, "自动=按速度/正确性挑，类型=只允许该类", toolX, 210, 0x707070);
        if (!error.isEmpty()) {
            drawString(canvas, error, blockX, height - 34, 0xFF5555);
        }

        super.render(canvas, mouseX, mouseY, partialTick);
        if (blockAutocomplete != null) {
            blockAutocomplete.render(canvas);
        }
        if (toolAutocomplete != null) {
            toolAutocomplete.render(canvas);
        }
        guide.render(canvas, mouseX, mouseY);
    }

    private void addGuideHints() {
        guide.add(blockX, 24, 100, 18, "策略名称");
        guide.add(blockX + 104, 24, 66, 18, "动作：使用 / 不切 / 强制");
        guide.add(blockX + 174, 24, 76, 18, "掉落门槛：继承 / 必须 / 不要求");
        guide.add(blockX + 254, 24, 54, 18, "这条策略是否生效");
        guide.add(blockX, 34, columnWidth, 10, "顺序即优先级，第一条命中的策略生效");
        guide.add(blockX, 52, columnWidth, 90, "方块圈定：任意一条命中即生效；滚轮滚动，点选编辑");
        guide.add(toolX, 52, columnWidth, 90, "工具圈定：任意一条允许即可用；点选编辑");
        guide.add(blockX, 148, 48, 18, "取准星所指的方块（含当前状态）");
        guide.add(blockX + 52, 148, 48, 18, "添加一条空方块，手动输入 id");
        guide.add(blockX + 104, 148, 48, 18, "取手上方块的方块 id");
        guide.add(toolX, 148, 44, 18, "添加手上工具的能力（没有能力则按物品）");
        guide.add(toolX + 48, 148, 44, 18, "从背包选择物品，可加为物品或类型");
        guide.add(toolX + 96, 148, 44, 18, "自动：不限定工具，按能掉落+速度挑");
        if (blockSelected >= 0) {
            int typeW = 40;
            int deleteW = 30;
            int gap = 2;
            int inputsW = columnWidth - typeW - deleteW - gap * 3;
            int valueW = Math.max(28, (int) (inputsW * 0.6D));
            int statesW = Math.max(24, inputsW - valueW);
            int valueX = blockX + typeW + gap;
            int statesX = valueX + valueW + gap;
            int deleteX = blockX + columnWidth - deleteW;
            guide.add(blockX, 168, typeW, 18, "切换类型：方块ID / 标签 / 模组");
            guide.add(valueX, 168, valueW, 18, "值；输入 2 个字符后出现前缀补全");
            guide.add(statesX, 168, statesW, 18, "方块状态，如 axis=y（可留空）");
            guide.add(deleteX, 168, deleteW, 18, "删除这条方块圈定");
        }
        if (toolSelected >= 0) {
            guide.add(toolX, 168, 52, 18, "切换工具匹配方式");
            guide.add(toolX + 56, 168, columnWidth - 98, 18, "值：类型 id / 物品 id / 槽位 / 名称 / 指纹");
            guide.add(toolX + columnWidth - 38, 168, 38, 18, "删除这条工具圈定");
        }
    }

    private void renderBlockList(Canvas canvas) {
        blockList.render(canvas, when.size(), (c, index, x, y, w, selected) -> {
            BlockMatcher matcher = when.get(index);
            IconRef icon = Icons.forBlock(matcher);
            if (!icon.isEmpty()) {
                canvas.item(icon, x, y - 1);
            }
            String badge = blockTypeLabel(typeOf(matcher))
                    + (matcher.states().isEmpty() ? "" : " +状态");
            int badgeWidth = c.textWidth(badge);
            drawString(c, cut(blockLabel(matcher), Math.max(4, (w - 18 - badgeWidth - 8) / 6)),
                    x + 18, y + 4, selected ? 0xFFFFFF : 0xC0C0C0);
            drawString(c, badge, x + w - badgeWidth - 2, y + 4, selected ? 0xC0D8FF : 0x909090);
        });
    }

    private void renderToolList(Canvas canvas) {
        toolList.render(canvas, tools.size(), (c, index, x, y, w, selected) -> {
            ToolMatcher matcher = tools.get(index);
            IconRef icon = Icons.forTool(matcher, game());
            if (!icon.isEmpty()) {
                canvas.item(icon, x, y - 1);
            }
            String badge = toolTypeLabel(typeOf(matcher));
            int badgeWidth = c.textWidth(badge);
            drawString(c, cut(toolLabel(matcher), Math.max(4, (w - 18 - badgeWidth - 8) / 6)),
                    x + 18, y + 4, selected ? 0xFFFFFF : 0xC0C0C0);
            drawString(c, badge, x + w - badgeWidth - 2, y + 4, selected ? 0xC0D8FF : 0x909090);
        });
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        syncEditor();
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(Canvas canvas, double mouseX, double mouseY, int button) {
        syncEditor();
        if (blockAutocomplete != null && blockAutocomplete.isOpen()) {
            if (blockAutocomplete.mouseClicked(canvas, mouseX, mouseY)) {
                return true;
            }
            blockAutocomplete.close();
        }
        if (toolAutocomplete != null && toolAutocomplete.isOpen()) {
            if (toolAutocomplete.mouseClicked(canvas, mouseX, mouseY)) {
                return true;
            }
            toolAutocomplete.close();
        }
        int blockIndex = blockList == null ? -1 : blockList.indexAt(mouseX, mouseY, when.size());
        if (blockIndex >= 0) {
            syncEditor();
            blockSelected = blockIndex;
            blockType = typeOf(when.get(blockIndex));
            rebuild();
            persist();
            return true;
        }
        int toolIndex = toolList == null ? -1 : toolList.indexAt(mouseX, mouseY, tools.size());
        if (toolIndex >= 0) {
            syncEditor();
            toolSelected = toolIndex;
            toolType = typeOf(tools.get(toolIndex));
            rebuild();
            persist();
            return true;
        }
        return super.mouseClicked(canvas, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(Canvas canvas, double mouseX, double mouseY, double delta) {
        if (blockList != null && blockList.contains(mouseX, mouseY)
                && blockList.scrollBy(delta, when.size())) {
            return true;
        }
        if (toolList != null && toolList.contains(mouseX, mouseY)
                && toolList.scrollBy(delta, tools.size())) {
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
        persist();
        if (error.isEmpty()) {
            super.onClose();
        } else {
            rebuild();
        }
    }

    // ------------------------------------------------------------------ labels

    private static String typeOf(BlockMatcher matcher) {
        return matcher.tag() != null ? "tag" : matcher.mod() != null ? "mod" : "id";
    }

    private static String typeOf(ToolMatcher matcher) {
        if (matcher.any()) {
            return "any";
        }
        if (matcher.capability() != null) {
            return "cap";
        }
        if (matcher.type() != null) {
            return "type";
        }
        if (matcher.item() != null) {
            return "item";
        }
        if (matcher.slot() != null) {
            return "slot";
        }
        if (matcher.nameContains() != null) {
            return "name";
        }
        if (matcher.fingerprint() != null) {
            return "fp";
        }
        return "any";
    }

    private static String blockTypeLabel(String type) {
        return switch (type) {
            case "tag" -> "标签";
            case "mod" -> "模组";
            default -> "方块ID";
        };
    }

    private static String toolTypeLabel(String type) {
        return switch (type) {
            case "cap" -> "类型";
            case "type" -> "类型ID";
            case "item" -> "物品";
            case "slot" -> "槽位";
            case "name" -> "名称";
            case "fp" -> "指纹";
            default -> "自动";
        };
    }

    private static String nextBlockType(String type) {
        for (int i = 0; i < BLOCK_TYPES.length; i++) {
            if (BLOCK_TYPES[i].equals(type)) {
                return BLOCK_TYPES[(i + 1) % BLOCK_TYPES.length];
            }
        }
        return "id";
    }

    private static String nextToolType(String type) {
        for (int i = 0; i < TOOL_TYPES.length; i++) {
            if (TOOL_TYPES[i].equals(type)) {
                return TOOL_TYPES[(i + 1) % TOOL_TYPES.length];
            }
        }
        return "cap";
    }

    private static String blockValue(BlockMatcher matcher, String type) {
        return switch (type) {
            case "tag" -> matcher.tag() == null ? "" : matcher.tag();
            case "mod" -> matcher.mod() == null ? "" : matcher.mod();
            default -> matcher.id() == null ? "" : matcher.id();
        };
    }

    private static String toolValue(ToolMatcher matcher, String type) {
        return switch (type) {
            case "item" -> matcher.item() == null ? "" : matcher.item();
            case "slot" -> matcher.slot() == null ? "" : Integer.toString(matcher.slot());
            case "name" -> matcher.nameContains() == null ? "" : matcher.nameContains();
            case "type" -> matcher.type() == null ? "" : matcher.type();
            case "cap" -> matcher.capability() == null ? "" : matcher.capability();
            case "fp" -> matcher.fingerprint() == null ? "" : matcher.fingerprint();
            default -> "";
        };
    }

    private String blockLabel(BlockMatcher matcher) {
        StringBuilder builder = new StringBuilder();
        if (matcher.id() != null) {
            IconRef icon = Icons.forBlock(matcher);
            String label = game().iconName(icon);
            builder.append(label.isEmpty() ? matcher.id() : label);
        } else if (matcher.tag() != null) {
            builder.append('#').append(matcher.tag());
        } else if (matcher.mod() != null) {
            builder.append(matcher.mod()).append(":*");
        }
        if (!matcher.states().isEmpty()) {
            builder.append(" [");
            boolean first = true;
            for (Map.Entry<String, String> entry : matcher.states().entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                builder.append(entry.getKey()).append('=').append(entry.getValue());
                first = false;
            }
            builder.append(']');
        }
        return builder.toString();
    }

    private String toolLabel(ToolMatcher matcher) {
        if (matcher.any()) {
            return "自动（按速度/正确性）";
        }
        if (matcher.capability() != null) {
            return "类型: " + Kinds.label(matcher.capability());
        }
        if (matcher.type() != null) {
            return "类型: " + Kinds.label(matcher.type());
        }
        if (matcher.item() != null) {
            IconRef icon = Icons.forTool(matcher, game());
            String label = game().iconName(icon);
            return "物品: " + (label.isEmpty() ? matcher.item() : label);
        }
        if (matcher.slot() != null) {
            return "槽位: " + matcher.slot();
        }
        if (matcher.nameContains() != null) {
            return "名称含: " + matcher.nameContains();
        }
        if (matcher.fingerprint() != null) {
            return "指纹: " + cut(matcher.fingerprint(), 18);
        }
        return "自动";
    }

    private static String actionLabel(Action action) {
        return switch (action) {
            case USE -> "使用";
            case NEVER -> "不切";
            case FORCE -> "强制";
        };
    }

    private static Action nextAction(Action action) {
        return switch (action) {
            case USE -> Action.NEVER;
            case NEVER -> Action.FORCE;
            case FORCE -> Action.USE;
        };
    }

    private static String gateLabel(Tri gate) {
        return switch (gate) {
            case INHERIT -> "继承";
            case TRUE -> "必须";
            case FALSE -> "不要求";
        };
    }

    private static Tri nextGate(Tri gate) {
        return switch (gate) {
            case INHERIT -> Tri.TRUE;
            case TRUE -> Tri.FALSE;
            case FALSE -> Tri.INHERIT;
        };
    }

    private static int clamp(int value, int size) {
        if (size <= 0) {
            return -1;
        }
        return Math.max(0, Math.min(value, size - 1));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static Integer parseSlot(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String statesString(Map<String, String> states) {
        StringBuilder builder = new StringBuilder();
        states.forEach((key, value) -> {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(key).append('=').append(value);
        });
        return builder.toString();
    }

    private static Map<String, String> parseStates(String text) {
        Map<String, String> states = new LinkedHashMap<>();
        for (String part : text.split(",")) {
            String trimmed = part.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0 && eq < trimmed.length() - 1) {
                states.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
            }
        }
        return states;
    }

    private static String cut(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "…";
    }
}
