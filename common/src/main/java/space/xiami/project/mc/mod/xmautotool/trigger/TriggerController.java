package space.xiami.project.mc.mod.xmautotool.trigger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.xiami.project.mc.mod.xmautotool.api.Game;
import space.xiami.project.mc.mod.xmautotool.api.Games;
import space.xiami.project.mc.mod.xmautotool.api.Hosts;
import space.xiami.project.mc.mod.xmautotool.config.PolicyRuntime;
import space.xiami.project.mc.mod.xmautotool.config.ProfileManager;
import space.xiami.project.mc.mod.xmautotool.config.ProfileSettings;
import space.xiami.project.mc.mod.xmautotool.core.BlockSnapshot;
import space.xiami.project.mc.mod.xmautotool.core.Decision;
import space.xiami.project.mc.mod.xmautotool.core.PolicyEngine;
import space.xiami.project.mc.mod.xmautotool.core.ToolSnapshot;

import java.util.List;
import java.util.function.Supplier;

/**
 * Platform-independent trigger/guard state machine: cooldowns, mining
 * re-evaluation, slot restore and the decision handoff to the engine.
 *
 * <p>The adapter only forwards events and supplies lazily built snapshots.</p>
 */
public final class TriggerController {

    private static final Logger LOGGER = LoggerFactory.getLogger("xmautotool");

    private final PolicyEngine engine = new PolicyEngine();

    private int tickCounter;
    private int cooldown;
    private int restoreSlot = -1;
    private int lastSwitchTarget = -1;

    /** Per-client-tick upkeep: cooldown and slot restore. */
    public void onTick() {
        Game game = Games.game();
        if (cooldown > 0) {
            cooldown--;
        }
        maybeRestore(game);
    }

    /** Left-click on a block; snapshots are only built after the guards pass. */
    public void onLeftClick(Supplier<BlockSnapshot> block, Supplier<List<ToolSnapshot>> hotbar, int currentSlot) {
        evaluate(block, hotbar, currentSlot);
    }

    /**
     * Mining re-evaluation. The adapter calls this only when the crosshair is on
     * a block; the interval counter matches the original tick counter.
     */
    public void onMiningTick(Supplier<BlockSnapshot> block, Supplier<List<ToolSnapshot>> hotbar,
                             int currentSlot, int interval) {
        ProfileSettings settings = ProfileManager.settings();
        Game game = Games.game();
        if (!settings.triggerWhileMining) {
            return;
        }
        if (!game.attackDown() || !game.destroying()) {
            return;
        }
        if (tickCounter++ % Math.max(1, interval) != 0) {
            return;
        }
        evaluate(block, hotbar, currentSlot);
    }

    private void evaluate(Supplier<BlockSnapshot> blockSupplier, Supplier<List<ToolSnapshot>> hotbarSupplier,
                          int currentSlot) {
        Game game = Games.game();
        ProfileSettings settings = ProfileManager.settings();
        if (!settings.enabled) {
            return;
        }
        if (!game.levelReady()) {
            return;
        }
        if (game.screenOpen() || game.spectator() || game.usingItem() || game.sleeping()) {
            return;
        }
        if (game.suppressDown() || cooldown > 0) {
            return;
        }

        BlockSnapshot block = blockSupplier.get();
        if (block == null) {
            return;
        }
        List<ToolSnapshot> hotbar = hotbarSupplier.get();
        Decision decision = engine.evaluate(
                block, hotbar, ProfileManager.engineSettings(), PolicyRuntime.policies(), currentSlot);

        if (decision.switchSlot() && decision.slot() != currentSlot) {
            game.switchSlot(decision.slot());
            if (settings.restoreSlotAfterMining && restoreSlot < 0) {
                restoreSlot = currentSlot;
                lastSwitchTarget = decision.slot();
            }
            cooldown = Math.max(0, settings.switchCooldownTicks);
            if (Hosts.host().debug()) {
                LOGGER.debug("switch {} -> slot {} ({})", currentSlot, decision.slot(), decision.policyId());
            }
        }
    }

    private void maybeRestore(Game game) {
        if (restoreSlot < 0) {
            return;
        }
        if (!game.levelReady()) {
            restoreSlot = -1;
            return;
        }
        boolean mining = game.destroying() && game.attackDown();
        if (mining) {
            return;
        }
        if (game.selectedSlot() != lastSwitchTarget) {
            restoreSlot = -1;
            return;
        }
        game.switchSlot(restoreSlot);
        restoreSlot = -1;
    }
}
