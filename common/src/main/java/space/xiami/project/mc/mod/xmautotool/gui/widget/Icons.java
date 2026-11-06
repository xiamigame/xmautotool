package space.xiami.project.mc.mod.xmautotool.gui.widget;

import space.xiami.project.mc.mod.xmautotool.api.Game;
import space.xiami.project.mc.mod.xmautotool.api.IconRef;
import space.xiami.project.mc.mod.xmautotool.api.ItemView;
import space.xiami.project.mc.mod.xmautotool.core.BlockMatcher;
import space.xiami.project.mc.mod.xmautotool.core.ToolMatcher;

import java.util.Locale;

/** Representative item icons for matchers (blocks have no renderable icon otherwise). */
public final class Icons {

    private Icons() {
    }

    public static IconRef forBlock(BlockMatcher matcher) {
        if (matcher.id() != null) {
            return IconRef.block(matcher.id());
        }
        if (matcher.tag() != null) {
            return IconRef.item("minecraft:name_tag");
        }
        if (matcher.mod() != null) {
            return IconRef.item("minecraft:bricks");
        }
        return IconRef.EMPTY;
    }

    public static IconRef forItem(String itemId) {
        return itemId == null ? IconRef.EMPTY : IconRef.item(itemId);
    }

    public static IconRef forTool(ToolMatcher matcher, Game game) {
        if (matcher.any()) {
            return IconRef.item("minecraft:compass");
        }
        if (matcher.item() != null) {
            return IconRef.item(matcher.item());
        }
        if (matcher.slot() != null) {
            return IconRef.slot(matcher.slot());
        }
        if (matcher.type() != null) {
            return forKind(matcher.type());
        }
        if (matcher.capability() != null) {
            return forKind(matcher.capability());
        }
        if (matcher.nameContains() != null) {
            String needle = matcher.nameContains().toLowerCase(Locale.ROOT);
            for (ItemView view : game.inventoryItems()) {
                if (view.name().toLowerCase(Locale.ROOT).contains(needle)) {
                    return view.icon();
                }
            }
            return IconRef.item("minecraft:name_tag");
        }
        return IconRef.item("minecraft:paper");
    }

    public static IconRef forKind(String kind) {
        String path = kind.substring(kind.indexOf(':') + 1);
        return switch (path) {
            case "pickaxe", "hammer", "paxel" -> IconRef.item("minecraft:iron_pickaxe");
            case "axe" -> IconRef.item("minecraft:iron_axe");
            case "shovel" -> IconRef.item("minecraft:iron_shovel");
            case "hoe" -> IconRef.item("minecraft:iron_hoe");
            case "sword" -> IconRef.item("minecraft:iron_sword");
            case "shears" -> IconRef.item("minecraft:shears");
            default -> IconRef.item("minecraft:iron_pickaxe");
        };
    }
}
