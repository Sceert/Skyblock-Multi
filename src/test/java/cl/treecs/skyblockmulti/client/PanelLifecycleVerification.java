package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.network.IslandPanelPayload;
import cl.treecs.skyblockmulti.network.IslandPanelRequestPayload;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** Tests the native panel session, request correlation and lifecycle. */
public final class PanelLifecycleVerification {
    public static void main(String[] args) {
        var sent = new ArrayList<IslandPanelRequestPayload>();
        var state = new IslandPanelState(sent::add);
        var foreground = state.prepareForeground().toCompletableFuture();
        require(!foreground.isDone(), "Foreground must await correlated snapshot");
        var open = sent.getLast();
        state.update(snapshot(1, state.session(), open.request(), true));
        require(foreground.join().accepted(), "Fresh foreground accepted");
        state.tab = 2; state.textPage = 3;
        var action = state.act("island_party").toCompletableFuture();
        var request = sent.getLast();
        var background = state.prepareBackground().toCompletableFuture();
        state.act("island_party");
        require(sent.size() == 2, "Pending operation cannot duplicate action");
        state.update(snapshot(2, state.session(), 0, true));
        require(!action.isDone() && !background.isDone(), "Passive snapshot is not acknowledgement");
        require(state.tab == 2 && state.textPage == 3, "Unchanged remote data retains navigation");
        state.update(snapshot(3, state.session(), request.request(), false));
        require(!action.join().accepted() && !background.join().accepted(), "Server rejection vetoes transition");
        require(state.prepareBackground().toCompletableFuture().join().accepted(), "Rejected transition does not poison session");

        var waiting = state.prepareForeground().toCompletableFuture();
        var refresh = sent.getLast();
        state.suspend();
        state.update(snapshot(4, state.session(), refresh.request(), true));
        require(!waiting.join().accepted() && !state.valid(), "Suspension invalidates late preparation");
        var recovered = state.prepareForeground().toCompletableFuture();
        state.update(snapshot(5, state.session(), sent.getLast().request(), true));
        require(recovered.join().accepted(), "Resume validates new request");
        var operationBeforeFocus = state.act("island_party").toCompletableFuture();
        long actionId = sent.getLast().request();
        var focusAfterAction = state.prepareForeground().toCompletableFuture();
        state.update(snapshot(6, state.session(), actionId, true));
        require(operationBeforeFocus.join().accepted() && !focusAfterAction.isDone(), "Receipt must precede fresh foreground validation");
        require(sent.getLast().action().equals("open") && sent.getLast().request() != actionId, "Foreground requests fresh snapshot after action");
        state.update(snapshot(7, state.session(), sent.getLast().request(), true));
        require(focusAfterAction.join().accepted(), "Fresh validation completes queued foreground");
        var timeout = state.act("island_party").toCompletableFuture();
        for (int i = 0; i < 100; i++) state.tick();
        require(!timeout.join().accepted() && !state.valid(), "Timeout denies and disables actions");
        int before = sent.size();
        state.close(true);
        require(sent.size() == before && state.snapshot().entries().isEmpty(), "Disconnect clears data without sending");
        require(!state.update(snapshot(8, state.session(), 0, true)), "Closed instance cannot revive");
        var next = new IslandPanelState(sent::add);
        require(next.session() != state.session(), "New instance has new session");
        require(!next.update(snapshot(9, state.session(), 0, true)), "Prior session response ignored");
        var pending = next.prepareForeground().toCompletableFuture();
        before = sent.size(); next.close(true);
        require(!pending.join().accepted() && sent.size() == before, "Disconnect resolves pending preparation without actions");
        var closing = new IslandPanelState(sent::add);
        closing.close(false); closing.close(false);
        require(sent.getLast().action().equals("close") && sent.size() == before + 1, "Real close unsubscribes exactly once");
        System.out.println("Panel lifecycle verification passed (correlation, veto, suspension, timeout, disconnect)");
    }
    private static IslandPanelPayload snapshot(long revision, long session, long request, boolean accepted) {
        return new IslandPanelPayload(revision, false, List.of(new IslandPanelPayload.Entry(2, "party", Component.literal("Choose"),
                List.of(new IslandPanelPayload.Action("island_party", Component.literal("Accept"))))), session, request, accepted);
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
