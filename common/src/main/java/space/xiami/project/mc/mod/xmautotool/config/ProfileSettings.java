package space.xiami.project.mc.mod.xmautotool.config;

/** Behaviour settings stored inside a profile file (not in the TOML anymore). */
public final class ProfileSettings {

    public boolean enabled = true;
    public boolean fallbackAuto = true;
    public boolean requireCorrectTool = true;
    public int minDurability = 10;
    public int minSpeedGainPercent = 10;
    public boolean triggerWhileMining = true;
    public int miningEvalInterval = 2;
    public int switchCooldownTicks = 2;
    public boolean restoreSlotAfterMining = false;
    public boolean guide = true;

    public ProfileSettings copy() {
        ProfileSettings copy = new ProfileSettings();
        copy.enabled = enabled;
        copy.fallbackAuto = fallbackAuto;
        copy.requireCorrectTool = requireCorrectTool;
        copy.minDurability = minDurability;
        copy.minSpeedGainPercent = minSpeedGainPercent;
        copy.triggerWhileMining = triggerWhileMining;
        copy.miningEvalInterval = miningEvalInterval;
        copy.switchCooldownTicks = switchCooldownTicks;
        copy.restoreSlotAfterMining = restoreSlotAfterMining;
        copy.guide = guide;
        return copy;
    }
}
