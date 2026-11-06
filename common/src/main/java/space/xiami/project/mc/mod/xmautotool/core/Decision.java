package space.xiami.project.mc.mod.xmautotool.core;

/** Result of one evaluation. */
public record Decision(boolean switchSlot, int slot, String policyId, String reason) {

    public static Decision noOp(String reason) {
        return new Decision(false, -1, null, reason);
    }

    public static Decision switchTo(int slot, String policyId) {
        return new Decision(true, slot, policyId, "switch");
    }
}
