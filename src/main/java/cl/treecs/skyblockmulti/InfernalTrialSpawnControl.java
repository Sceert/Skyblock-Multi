package cl.treecs.skyblockmulti;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import cl.treecs.skyblockmulti.infernal.InfernalTrialRunner;

/** Prevents unrelated natural mobs from entering the active Infernal Trial. */
public final class InfernalTrialSpawnControl {
    private static final String OBJECTIVE = "sb_infernal";
    private static final int MIN_ARENA_CHUNK = -2;
    private static final int MAX_ARENA_CHUNK = 2;

    private InfernalTrialSpawnControl() {
    }

    public static void register() {
        ServerEntityEvents.ALLOW_LOAD.register(InfernalTrialSpawnControl::allowEntityLoad);
    }

    private static boolean allowEntityLoad(
            net.minecraft.world.entity.Entity entity,
            ServerLevel level,
            EntitySpawnReason reason,
            boolean existing
    ) {
        if (!(entity instanceof Mob) || !level.dimension().equals(Level.NETHER)) {
            return true;
        }
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }

        int chunkX = entity.chunkPosition().x();
        int chunkZ = entity.chunkPosition().z();
        if (chunkX < MIN_ARENA_CHUNK || chunkX > MAX_ARENA_CHUNK
                || chunkZ < MIN_ARENA_CHUNK || chunkZ > MAX_ARENA_CHUNK) {
            return true;
        }

        Objective objective = level.getScoreboard().getObjective(OBJECTIVE);
        if (objective == null) {
            return true;
        }
        ReadOnlyScoreInfo lock = level.getScoreboard().getPlayerScoreInfo(
                ScoreHolder.forNameOnly("#spawn_lock"), objective);
        boolean builtInLock = lock != null && lock.value() != 0;
        return !builtInLock && !InfernalTrialRunner.isSpawnLocked(level);
    }
}
