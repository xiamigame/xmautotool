package space.xiami.project.mc.mod.xmautotool.config;

import space.xiami.project.mc.mod.xmautotool.core.BlockMatcher;
import space.xiami.project.mc.mod.xmautotool.core.Policy;
import space.xiami.project.mc.mod.xmautotool.core.ToolMatcher;

import java.util.ArrayList;
import java.util.List;

/** Complete user configuration of one profile file. */
public final class ProfileData {

    public String name = "默认";
    public ProfileSettings settings = new ProfileSettings();
    public final List<Policy> policies = new ArrayList<>();
    public final List<BlockMatcher> blockBlacklist = new ArrayList<>();
    public final List<ToolMatcher> toolBlacklist = new ArrayList<>();
    public final List<UserKind> kinds = new ArrayList<>();
    public final List<PolicyRuntime.ToolOverride> overrides = new ArrayList<>();

    public ProfileData copy() {
        ProfileData copy = new ProfileData();
        copy.name = name;
        copy.settings = settings.copy();
        copy.policies.addAll(policies);
        copy.blockBlacklist.addAll(blockBlacklist);
        copy.toolBlacklist.addAll(toolBlacklist);
        copy.kinds.addAll(kinds);
        copy.overrides.addAll(overrides);
        return copy;
    }
}
