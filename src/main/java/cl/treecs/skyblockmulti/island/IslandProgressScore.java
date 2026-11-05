package cl.treecs.skyblockmulti.island;

import net.minecraft.server.level.ServerPlayer;

/** Personal progression, derived from durable vanilla advancement progress rather than repeatable events. */
public final class IslandProgressScore {
    public static final int POINTS_PER_MILESTONE = 10;
    private IslandProgressScore() {}
    public static int completedMilestones(ServerPlayer player) {
        int completed = 0;
        for (var holder : player.level().getServer().getAdvancements().getAllAdvancements()) {
            var advancement = holder.value();
            if (!countsAsMilestone(holder.id().getNamespace(), advancement.parent().isPresent(),
                    advancement.display().isPresent(), advancement.display().map(display -> display.hidden()).orElse(false))) continue;
            if (player.getAdvancements().getOrStartProgress(holder).isDone()) completed++;
        }
        return completed;
    }
    public static int points(int milestones) { return Math.multiplyExact(Math.max(0, milestones), POINTS_PER_MILESTONE); }
    public static boolean countsAsMilestone(String namespace, boolean hasParent, boolean hasDisplay, boolean hidden) {
        return "skyblockmulti".equals(namespace) && hasParent && hasDisplay && !hidden;
    }
}
