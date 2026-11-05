package cl.treecs.skyblockmulti.island;

import cl.treecs.skyblockmulti.api.party.IslandTransferCoordinator;
import cl.treecs.skyblockmulti.api.party.PartyApi;
import cl.treecs.skyblockmulti.api.party.PartyHomeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.UUID;

/** Projects only ownership/active metadata. No block, inventory, reward or claim commands. */
public final class IslandTransferWorldAccess implements IslandTransferCoordinator.WorldAccess {
    private final MinecraftServer server;
    public IslandTransferWorldAccess(MinecraftServer server) { this.server = server; }

    @Override public IslandTransferCoordinator.Snapshot inspect(UUID sender, UUID recipient) {
        if (!IslandTransferCoordinator.PROVIDER.equals(PartyApi.activeProviderId())) return null;
        ServerPlayer from = server.getPlayerList().getPlayer(sender);
        ServerPlayer to = server.getPlayerList().getPlayer(recipient);
        if (from == null || to == null) return null;
        var first = PartyApi.snapshot(from);
        var second = PartyApi.snapshot(to);
        if (!first.inParty() || !second.inParty() || !first.partyId().equals(second.partyId())
                || !first.ownerUuid().equals(second.ownerUuid())) return null;
        if (PartyHomeRegistry.pending(first.partyId()) != null) return null;
        int slot = score(from, "sb3_slot");
        Objective ownership = server.getScoreboard().getObjective("sb3_slot");
        if (ownership == null || slot < 1 || slot > IslandRingLayout.TECHNICAL_LIMIT
                || score(from, "sb3_state") != 2) return null;
        if (server.getScoreboard().listPlayerScores(ownership).stream()
                .anyMatch(entry -> entry.value() == slot && !entry.owner().equals(from.getScoreboardName()))) {
            return null; // Refuse ambiguous pre-existing ownership instead of transferring it.
        }
        var configured = cl.treecs.skyblockmulti.SkyblockMultiMod.getConfiguredIslandLayout().positions();
        if (configured.stream().noneMatch(position -> position.slotId() == slot
                && position.x() == score(from, "sb3_x") && position.z() == score(from, "sb3_z"))) return null;
        return new IslandTransferCoordinator.Snapshot(PartyApi.activeProviderId(), first.partyId(),
                first.ownerUuid(), PartyHomeRegistry.homeOwner(first), sender,
                from.getGameProfile().name(), own(from), recipient, to.getGameProfile().name(), own(to));
    }

    private IslandTransferCoordinator.Island own(ScoreHolder player) {
        int slot = score(player, "sb3_slot");
        if (slot < 1 || slot > IslandRingLayout.TECHNICAL_LIMIT) return null;
        return new IslandTransferCoordinator.Island(slot, score(player, "sb3_x"),
                score(player, "sb3_z"), score(player, "sb_chest"));
    }

    @Override public void project(IslandTransferCoordinator.Decision decision) {
        var expected = decision.expected();
        var island = expected.own();
        ScoreHolder from = holder(expected.sender(), expected.senderName());
        ScoreHolder to = holder(expected.recipient(), expected.recipientName());
        // Never overwrite an out-of-protocol assignment during recovery.
        if ((score(from, "sb_own_rev") < decision.revision()
                && score(from, "sb3_slot") != 0 && score(from, "sb3_slot") != island.slot())
                || (score(to, "sb_own_rev") < decision.revision()
                && score(to, "sb3_slot") != 0 && score(to, "sb3_slot") != island.slot())) {
            throw new IllegalStateException("Conflicting OWN while recovering an island transfer");
        }
        if (score(from, "sb_own_rev") < decision.revision()) {
            set(from, "sb3_slot", 0);
            set(from, "sb3_x", 0);
            set(from, "sb3_z", 0);
            set(from, "sb_active_x", island.x());
            set(from, "sb_active_z", island.z());
            set(from, "sb_party_pref", 1);
            ServerPlayer online = server.getPlayerList().getPlayer(expected.sender());
            if (online != null) online.addTag("skyblock_party_guest");
            set(from, "sb_own_rev", decision.revision());
        }
        if (score(to, "sb_own_rev") < decision.revision()) {
            set(to, "sb3_slot", island.slot());
            set(to, "sb3_x", island.x());
            set(to, "sb3_z", island.z());
            set(to, "sb_active_x", island.x());
            set(to, "sb_active_z", island.z());
            set(to, "sb3_state", 2);
            set(to, "sb_chest", island.difficulty());
            set(to, "sb_party_pref", 0);
            set(to, "sb_tree", 0);
            set(to, "sb_difficulty", 0);
            set(to, "sb_menu", 0);
            ServerPlayer online = server.getPlayerList().getPlayer(expected.recipient());
            if (online != null) {
                online.removeTag("skyblock_party_guest");
                online.removeTag("skyblock_party_reentry");
                online.removeTag("skyblock_selection_locked");
            }
            set(to, "sb_own_rev", decision.revision());
        }
        ScoreHolder party = ScoreHolder.forNameOnly("#pt" + expected.party().toString().replace("-", ""));
        if (score(party, "sb_own_rev") < decision.revision()) {
            PartyHomeRegistry.setHome(expected.party(), expected.recipient());
            set(party, "sb_own_rev", decision.revision());
        }
    }

    private ScoreHolder holder(UUID id, String capturedName) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        return online == null ? ScoreHolder.forNameOnly(capturedName) : online;
    }

    private int score(ScoreHolder holder, String name) {
        Objective objective = server.getScoreboard().getObjective(name);
        var value = objective == null ? null : server.getScoreboard().getPlayerScoreInfo(holder, objective);
        return value == null ? 0 : value.value();
    }

    private void set(ScoreHolder holder, String name, int value) {
        var scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(name);
        if (objective == null) objective = scoreboard.addObjective(name, ObjectiveCriteria.DUMMY,
                Component.literal(name), ObjectiveCriteria.RenderType.INTEGER, false, null);
        scoreboard.getOrCreatePlayerScore(holder, objective).set(value);
    }

}
