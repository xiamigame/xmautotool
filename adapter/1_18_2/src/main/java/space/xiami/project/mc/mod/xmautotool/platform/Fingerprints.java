package space.xiami.project.mc.mod.xmautotool.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stable tool identity and flattened NBT views used by the {@code fingerprint}
 * and {@code nbt} tool matchers.
 */
public final class Fingerprints {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Fingerprints() {
    }

    /** SHA-1 over item id + NBT with durability/cost fields stripped. */
    public static String of(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        CompoundTag cleaned = tag == null ? null : tag.copy();
        if (cleaned != null) {
            cleaned.remove("Damage");
            cleaned.remove("RepairCost");
        }
        String payload = Registries.itemId(stack.getItem()) + '|' + (cleaned == null ? "" : cleaned);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return "sha1:" + hex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "sha1:error";
        }
    }

    /** Flat {@code a.b.c -> value} view, without durability/cost fields. */
    public static Map<String, String> flatten(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        walk(tag, "", out);
        out.remove("Damage");
        out.remove("RepairCost");
        return Map.copyOf(out);
    }

    private static void walk(CompoundTag tag, String prefix, Map<String, String> out) {
        for (String key : tag.getAllKeys()) {
            Tag value = tag.get(key);
            String path = prefix.isEmpty() ? key : prefix + '.' + key;
            if (value instanceof CompoundTag child) {
                walk(child, path, out);
            } else {
                out.put(path, value.getAsString());
            }
        }
    }

    private static String hex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            chars[i * 2] = HEX[b >>> 4];
            chars[i * 2 + 1] = HEX[b & 0x0F];
        }
        return new String(chars);
    }
}
