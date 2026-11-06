package space.xiami.project.mc.mod.xmautotool.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.xiami.project.mc.mod.xmautotool.api.Hosts;
import space.xiami.project.mc.mod.xmautotool.core.EngineSettings;
import space.xiami.project.mc.mod.xmautotool.core.Policy;
import space.xiami.project.mc.mod.xmautotool.core.ToolKindRegistry;
import space.xiami.project.mc.mod.xmautotool.core.ToolMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Named profile files: every profile contains the complete user configuration
 * (settings, policies, tool recognition, tool kinds, blacklists).
 *
 * <p>Switching a profile loads it immediately; edits are written straight back.</p>
 */
public final class ProfileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("xmautotool");

    private static Path dir;
    private static String activeId = "default";
    private static ProfileData active = new ProfileData();
    private static boolean initialized;

    private ProfileManager() {
    }

    // ------------------------------------------------------------------ lifecycle

    public static void init() {
        if (initialized) {
            return;
        }
        dir = Hosts.host().configDir().resolve("xmautotool").resolve("profiles");
        try {
            Files.createDirectories(dir);
            ensureDefaultProfile();
        } catch (IOException e) {
            LOGGER.error("Failed to prepare profile directory", e);
        }
        String configured;
        try {
            configured = sanitize(Hosts.host().activeProfileId());
        } catch (Exception e) {
            configured = "default";
        }
        if (!Files.exists(file(configured))) {
            configured = "default";
        }
        activeId = configured;
        load(activeId);
        initialized = true;
    }

    public static boolean initialized() {
        return initialized;
    }

    private static void ensureDefaultProfile() throws IOException {
        if (Files.exists(file("default"))) {
            return;
        }
        ProfileData data = null;
        Path legacy = dir.getParent().resolve("policies.json");
        if (Files.exists(legacy)) {
            try {
                data = ProfileJson.read(Files.readString(legacy, StandardCharsets.UTF_8), "默认");
                LOGGER.info("Migrated legacy policies.json into profiles/default.json");
            } catch (Exception e) {
                LOGGER.warn("Could not migrate legacy policies.json; starting fresh", e);
            }
        }
        if (data == null) {
            data = new ProfileData();
        }
        write("default", data);
    }

    public static void load(String id) {
        activeId = id;
        Path file = file(id);
        try {
            if (Files.exists(file)) {
                active = ProfileJson.read(Files.readString(file, StandardCharsets.UTF_8), id);
            } else {
                active = new ProfileData();
                active.name = id;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read profile {}, keeping the previous one", id, e);
            backup(file);
        }
        apply();
    }

    public static void save() {
        write(activeId, active);
        apply();
    }

    public static void reload() {
        load(activeId);
    }

    // ------------------------------------------------------------------ access

    public static ProfileData data() {
        return active;
    }

    public static ProfileSettings settings() {
        return active.settings;
    }

    public static String activeId() {
        return activeId;
    }

    public static List<String> ids() {
        if (dir == null) {
            return List.of("default");
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replaceFirst("\\.json$", ""))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.warn("Failed to list profiles", e);
            return List.of(activeId);
        }
    }

    public static EngineSettings engineSettings() {
        ProfileSettings s = active.settings;
        return new EngineSettings(
                s.enabled,
                s.fallbackAuto,
                s.requireCorrectTool,
                s.minDurability,
                s.minSpeedGainPercent,
                active.blockBlacklist,
                active.toolBlacklist);
    }

    /**
     * Renames a user-defined kind and rewrites every reference in this profile
     * (kind parents, tool overrides, policy tools, tool blacklist).
     *
     * @return an error message, or {@code null} on success.
     */
    public static String renameKind(String oldId, String newId) {
        if (oldId == null || oldId.isBlank()) {
            return "缺少原类型 id";
        }
        String target = normalizeKindId(newId);
        if (target == null) {
            return "id 格式无效，例：xmautotool:hammer";
        }
        if (ToolKindRegistry.isBuiltin(target)) {
            return "不能与内置类型重名";
        }
        if (target.equals(oldId)) {
            return "id 没有变化";
        }
        int index = -1;
        for (int i = 0; i < active.kinds.size(); i++) {
            if (active.kinds.get(i).id().equals(oldId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return "找不到要改名的类型";
        }
        for (UserKind kind : active.kinds) {
            if (kind.id().equals(target)) {
                return "id 已存在";
            }
        }

        for (int i = 0; i < active.kinds.size(); i++) {
            UserKind kind = active.kinds.get(i);
            String id = i == index ? target : kind.id();
            List<String> parents = new ArrayList<>(kind.parents().size());
            for (String parent : kind.parents()) {
                parents.add(parent.equals(oldId) ? target : parent);
            }
            active.kinds.set(i, new UserKind(id, parents, kind.color()));
        }
        for (int i = 0; i < active.overrides.size(); i++) {
            PolicyRuntime.ToolOverride override = active.overrides.get(i);
            active.overrides.set(i, new PolicyRuntime.ToolOverride(
                    override.item(), renameKind(override.kinds(), oldId, target),
                    override.assumeCorrect(), override.speedOverride()));
        }
        for (int i = 0; i < active.policies.size(); i++) {
            Policy policy = active.policies.get(i);
            List<ToolMatcher> tools = new ArrayList<>(policy.tools().size());
            for (ToolMatcher matcher : policy.tools()) {
                tools.add(renameKind(matcher, oldId, target));
            }
            active.policies.set(i, new Policy(policy.id(), policy.name(), policy.enabled(),
                    policy.when(), policy.action(), tools, policy.requireCorrectTool()));
        }
        for (int i = 0; i < active.toolBlacklist.size(); i++) {
            active.toolBlacklist.set(i, renameKind(active.toolBlacklist.get(i), oldId, target));
        }
        save();
        return null;
    }

    /** @return normalized {@code namespace:path} id, or {@code null} when invalid. */
    public static String normalizeKindId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (!id.contains(":")) {
            id = "xmautotool:" + id;
        }
        return id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") ? id : null;
    }

    private static List<String> renameKind(List<String> kinds, String oldId, String newId) {
        boolean changed = false;
        List<String> out = new ArrayList<>(kinds.size());
        for (String kind : kinds) {
            if (kind.equals(oldId)) {
                out.add(newId);
                changed = true;
            } else {
                out.add(kind);
            }
        }
        return changed ? out : kinds;
    }

    private static ToolMatcher renameKind(ToolMatcher matcher, String oldId, String newId) {
        String type = oldId.equals(matcher.type()) ? newId : matcher.type();
        String capability = oldId.equals(matcher.capability()) ? newId : matcher.capability();
        if (Objects.equals(type, matcher.type()) && Objects.equals(capability, matcher.capability())) {
            return matcher;
        }
        return new ToolMatcher(matcher.slot(), matcher.nameContains(), matcher.fingerprint(),
                matcher.item(), type, capability, matcher.nbt(), matcher.any(), matcher.weight());
    }

    // ------------------------------------------------------------------ actions

    public static void switchTo(String id) {
        if (id.equals(activeId)) {
            return;
        }
        save();
        load(id);
        Hosts.host().setActiveProfileId(id);
        Hosts.host().saveConfig();
    }

    public static String createNew() {
        save();
        String id = uniqueId("profile");
        ProfileData data = new ProfileData();
        data.name = "配置 " + id.replace("profile_", "");
        write(id, data);
        load(id);
        Hosts.host().setActiveProfileId(id);
        Hosts.host().saveConfig();
        return id;
    }

    public static String duplicate() {
        save();
        String id = uniqueId(activeId);
        ProfileData copy = active.copy();
        copy.name = copy.name + " 副本";
        write(id, copy);
        load(id);
        Hosts.host().setActiveProfileId(id);
        Hosts.host().saveConfig();
        return id;
    }

    public static void renameActive(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return;
        }
        active.name = displayName.trim();
        save();
    }

    public static void delete(String id) {
        List<String> ids = new ArrayList<>(ids());
        if (ids.size() <= 1) {
            return;
        }
        try {
            Files.deleteIfExists(file(id));
        } catch (IOException e) {
            LOGGER.error("Failed to delete profile {}", id, e);
            return;
        }
        if (id.equals(activeId)) {
            String next = ids.stream().filter(candidate -> !candidate.equals(id)).findFirst().orElse("default");
            load(next);
            Hosts.host().setActiveProfileId(next);
            Hosts.host().saveConfig();
        }
    }

    // ------------------------------------------------------------------ internals

    private static void apply() {
        Map<String, List<String>> parents = new LinkedHashMap<>(ToolKindRegistry.builtins());
        for (UserKind kind : active.kinds) {
            parents.put(kind.id(), kind.parents());
        }
        Map<String, PolicyRuntime.ToolOverride> overrides = new LinkedHashMap<>();
        for (PolicyRuntime.ToolOverride override : active.overrides) {
            overrides.put(override.item(), override);
        }
        PolicyRuntime.apply(active.policies, active.blockBlacklist, active.toolBlacklist, overrides, parents);
    }

    private static void write(String id, ProfileData data) {
        try {
            Path file = file(id);
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, ProfileJson.write(data), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save profile {}", id, e);
        }
    }

    private static void backup(Path file) {
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Failed to back up profile {}", file, e);
        }
    }

    private static Path file(String id) {
        return dir.resolve(sanitize(id) + ".json");
    }

    private static String uniqueId(String base) {
        String clean = sanitize(base);
        String candidate = clean;
        int n = 2;
        while (Files.exists(file(candidate))) {
            candidate = clean + "_" + n++;
        }
        return candidate;
    }

    private static String sanitize(String id) {
        if (id == null || id.isBlank()) {
            return "default";
        }
        String clean = id.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
        return clean.isBlank() ? "default" : clean;
    }
}
