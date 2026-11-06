package space.xiami.project.mc.mod.xmautotool.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.xiami.project.mc.mod.xmautotool.core.Action;
import space.xiami.project.mc.mod.xmautotool.core.BlockMatcher;
import space.xiami.project.mc.mod.xmautotool.core.Policy;
import space.xiami.project.mc.mod.xmautotool.core.ToolMatcher;
import space.xiami.project.mc.mod.xmautotool.core.Tri;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON (de)serialisation for a profile file. Hand-written to avoid Gson record reflection. */
public final class ProfileJson {

    private static final Logger LOGGER = LoggerFactory.getLogger("xmautotool");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int SCHEMA_VERSION = 3;

    private ProfileJson() {
    }

    // ------------------------------------------------------------------ write

    public static String write(ProfileData data) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.addProperty("name", data.name);
        root.add("settings", writeSettings(data.settings));

        JsonArray kinds = new JsonArray();
        for (UserKind kind : data.kinds) {
            JsonObject o = new JsonObject();
            o.addProperty("id", kind.id());
            o.add("parents", strings(kind.parents()));
            o.addProperty("color", kind.color());
            kinds.add(o);
        }
        root.add("kinds", kinds);

        JsonArray overrides = new JsonArray();
        for (PolicyRuntime.ToolOverride override : data.overrides) {
            JsonObject o = new JsonObject();
            o.addProperty("item", override.item());
            o.add("kinds", strings(override.kinds()));
            o.addProperty("assumeCorrect", override.assumeCorrect());
            if (override.speedOverride() != null) {
                o.addProperty("speedOverride", override.speedOverride());
            }
            overrides.add(o);
        }
        root.add("toolOverrides", overrides);

        JsonArray policies = new JsonArray();
        for (Policy policy : data.policies) {
            JsonObject o = new JsonObject();
            o.addProperty("id", policy.id());
            o.addProperty("name", policy.name());
            o.addProperty("enabled", policy.enabled());
            o.addProperty("action", policy.action().name().toLowerCase());
            o.addProperty("requireCorrectTool", policy.requireCorrectTool().name().toLowerCase());
            o.add("when", writeBlockMatchers(policy.when()));
            JsonArray tools = new JsonArray();
            for (ToolMatcher matcher : policy.tools()) {
                tools.add(writeToolMatcher(matcher));
            }
            o.add("tools", tools);
            policies.add(o);
        }
        root.add("policies", policies);

        JsonObject global = new JsonObject();
        global.add("blockBlacklist", writeBlockMatchers(data.blockBlacklist));
        JsonArray toolBlacklist = new JsonArray();
        for (ToolMatcher matcher : data.toolBlacklist) {
            toolBlacklist.add(writeToolMatcher(matcher));
        }
        global.add("toolBlacklist", toolBlacklist);
        root.add("global", global);

        return GSON.toJson(root);
    }

    private static JsonObject writeSettings(ProfileSettings settings) {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", settings.enabled);
        o.addProperty("fallbackAuto", settings.fallbackAuto);
        o.addProperty("requireCorrectTool", settings.requireCorrectTool);
        o.addProperty("minDurability", settings.minDurability);
        o.addProperty("minSpeedGainPercent", settings.minSpeedGainPercent);
        o.addProperty("triggerWhileMining", settings.triggerWhileMining);
        o.addProperty("miningEvalInterval", settings.miningEvalInterval);
        o.addProperty("switchCooldownTicks", settings.switchCooldownTicks);
        o.addProperty("restoreSlotAfterMining", settings.restoreSlotAfterMining);
        o.addProperty("guide", settings.guide);
        return o;
    }

    private static JsonArray writeBlockMatchers(List<BlockMatcher> matchers) {
        JsonArray array = new JsonArray();
        for (BlockMatcher matcher : matchers) {
            JsonObject o = new JsonObject();
            if (matcher.id() != null) {
                o.addProperty("id", matcher.id());
            }
            if (matcher.tag() != null) {
                o.addProperty("tag", matcher.tag());
            }
            if (matcher.mod() != null) {
                o.addProperty("mod", matcher.mod());
            }
            if (!matcher.states().isEmpty()) {
                JsonObject states = new JsonObject();
                matcher.states().forEach(states::addProperty);
                o.add("states", states);
            }
            array.add(o);
        }
        return array;
    }

    private static JsonObject writeToolMatcher(ToolMatcher matcher) {
        JsonObject o = new JsonObject();
        if (matcher.slot() != null) {
            o.addProperty("slot", matcher.slot());
        }
        if (matcher.nameContains() != null) {
            o.addProperty("nameContains", matcher.nameContains());
        }
        if (matcher.fingerprint() != null) {
            o.addProperty("fingerprint", matcher.fingerprint());
        }
        if (matcher.item() != null) {
            o.addProperty("item", matcher.item());
        }
        if (matcher.type() != null) {
            o.addProperty("type", matcher.type());
        }
        if (matcher.capability() != null) {
            o.addProperty("capability", matcher.capability());
        }
        if (!matcher.nbt().isEmpty()) {
            JsonObject nbt = new JsonObject();
            matcher.nbt().forEach(nbt::addProperty);
            o.add("nbt", nbt);
        }
        if (matcher.any()) {
            o.addProperty("any", true);
        }
        if (matcher.weight() != 0) {
            o.addProperty("weight", matcher.weight());
        }
        return o;
    }

    private static JsonArray strings(List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    // ------------------------------------------------------------------ read

    public static ProfileData read(String text, String fallbackName) {
        ProfileData data = new ProfileData();
        data.name = fallbackName;
        JsonObject root = JsonParser.parseString(text).getAsJsonObject();
        int version = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : 1;
        if (version > SCHEMA_VERSION) {
            LOGGER.warn("Profile schemaVersion {} is newer than {}; reading anyway", version, SCHEMA_VERSION);
        }

        if (root.has("name")) {
            data.name = root.get("name").getAsString();
        }
        if (root.has("settings")) {
            readSettings(root.getAsJsonObject("settings"), data.settings);
        }

        if (root.has("kinds")) {
            for (JsonElement element : root.getAsJsonArray("kinds")) {
                JsonObject o = element.getAsJsonObject();
                String id = string(o, "id");
                if (id == null) {
                    continue;
                }
                List<String> parents = strings(o, "parents");
                int color = o.has("color") ? o.get("color").getAsInt() : 0xFFFFFF;
                if (parents.contains(id)) {
                    LOGGER.warn("Skipping cyclic tool kind {}", id);
                    continue;
                }
                data.kinds.add(new UserKind(id, parents, color));
            }
        }

        if (root.has("toolOverrides")) {
            for (JsonElement element : root.getAsJsonArray("toolOverrides")) {
                JsonObject o = element.getAsJsonObject();
                String item = string(o, "item");
                if (item == null) {
                    continue;
                }
                boolean assume = o.has("assumeCorrect") && o.get("assumeCorrect").getAsBoolean();
                Float speed = o.has("speedOverride") && !o.get("speedOverride").isJsonNull()
                        ? o.get("speedOverride").getAsFloat() : null;
                data.overrides.add(new PolicyRuntime.ToolOverride(item, strings(o, "kinds"), assume, speed));
            }
        }

        if (root.has("policies")) {
            for (JsonElement element : root.getAsJsonArray("policies")) {
                Policy policy = readPolicy(element.getAsJsonObject());
                String invalid = policy.invalidReason();
                if (invalid != null) {
                    LOGGER.warn("Skipping invalid policy {}: {}", policy.id(), invalid);
                    continue;
                }
                data.policies.add(policy);
            }
        }

        if (root.has("global")) {
            JsonObject global = root.getAsJsonObject("global");
            data.blockBlacklist.addAll(readBlockMatchers(global, "blockBlacklist"));
            data.toolBlacklist.addAll(readToolMatchers(global, "toolBlacklist"));
        }
        return data;
    }

    private static void readSettings(JsonObject o, ProfileSettings settings) {
        settings.enabled = bool(o, "enabled", settings.enabled);
        settings.fallbackAuto = bool(o, "fallbackAuto", settings.fallbackAuto);
        settings.requireCorrectTool = bool(o, "requireCorrectTool", settings.requireCorrectTool);
        settings.minDurability = integer(o, "minDurability", settings.minDurability, 0, 100000);
        settings.minSpeedGainPercent = integer(o, "minSpeedGainPercent", settings.minSpeedGainPercent, 0, 200);
        settings.triggerWhileMining = bool(o, "triggerWhileMining", settings.triggerWhileMining);
        settings.miningEvalInterval = integer(o, "miningEvalInterval", settings.miningEvalInterval, 1, 10);
        settings.switchCooldownTicks = integer(o, "switchCooldownTicks", settings.switchCooldownTicks, 0, 20);
        settings.restoreSlotAfterMining = bool(o, "restoreSlotAfterMining", settings.restoreSlotAfterMining);
        settings.guide = bool(o, "guide", settings.guide);
    }

    private static Policy readPolicy(JsonObject o) {
        String id = string(o, "id");
        String name = string(o, "name");
        boolean enabled = !o.has("enabled") || o.get("enabled").getAsBoolean();
        Action action = parseEnum(Action.class, string(o, "action"), Action.USE);
        Tri gate = parseEnum(Tri.class, string(o, "requireCorrectTool"), Tri.INHERIT);
        return new Policy(id, name == null ? id : name, enabled,
                readBlockMatchers(o, "when"), action, readToolMatchers(o, "tools"), gate);
    }

    private static List<BlockMatcher> readBlockMatchers(JsonObject parent, String key) {
        List<BlockMatcher> matchers = new ArrayList<>();
        if (!parent.has(key)) {
            return matchers;
        }
        for (JsonElement element : parent.getAsJsonArray(key)) {
            JsonObject o = element.getAsJsonObject();
            Map<String, String> states = new LinkedHashMap<>();
            if (o.has("states")) {
                o.getAsJsonObject("states").entrySet()
                        .forEach(e -> states.put(e.getKey(), e.getValue().getAsString()));
            }
            matchers.add(new BlockMatcher(string(o, "id"), string(o, "tag"), string(o, "mod"), states));
        }
        return matchers;
    }

    private static List<ToolMatcher> readToolMatchers(JsonObject parent, String key) {
        List<ToolMatcher> matchers = new ArrayList<>();
        if (!parent.has(key)) {
            return matchers;
        }
        for (JsonElement element : parent.getAsJsonArray(key)) {
            matchers.add(readToolMatcher(element.getAsJsonObject()));
        }
        return matchers;
    }

    private static ToolMatcher readToolMatcher(JsonObject o) {
        Map<String, String> nbt = new LinkedHashMap<>();
        if (o.has("nbt")) {
            o.getAsJsonObject("nbt").entrySet()
                    .forEach(e -> nbt.put(e.getKey(), e.getValue().getAsString()));
        }
        return new ToolMatcher(
                o.has("slot") ? o.get("slot").getAsInt() : null,
                string(o, "nameContains"),
                string(o, "fingerprint"),
                string(o, "item"),
                string(o, "type"),
                string(o, "capability"),
                nbt,
                o.has("any") && o.get("any").getAsBoolean(),
                o.has("weight") ? o.get("weight").getAsInt() : 0);
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> values = new ArrayList<>();
        if (!o.has(key)) {
            return values;
        }
        for (JsonElement element : o.getAsJsonArray(key)) {
            values.add(element.getAsString());
        }
        return values;
    }

    private static String string(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        return o.has(key) ? o.get(key).getAsBoolean() : fallback;
    }

    private static int integer(JsonObject o, String key, int fallback, int min, int max) {
        if (!o.has(key)) {
            return fallback;
        }
        int value = o.get(key).getAsInt();
        return Math.max(min, Math.min(max, value));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
