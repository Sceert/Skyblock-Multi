package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.network.IslandPanelPayload;
import cl.treecs.skyblockmulti.network.IslandPanelRequestPayload;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/** Owner's session model. No Screen, Fabric hooks or Nexus classes. Client-thread confined. */
public final class IslandPanelState {
    public record Decision(boolean accepted, String key) {
        static Decision allow() { return new Decision(true, ""); }
        static Decision deny(String key) { return new Decision(false, "skyblockmulti.panel." + key); }
    }
    private static long nextSession;
    private final long session = ++nextSession;
    private final Consumer<IslandPanelRequestPayload> send;
    private IslandPanelPayload snapshot = new IslandPanelPayload(0, false, List.of());
    private long nextRequest, pendingRequest;
    private int ticks;
    private boolean closed, valid, suspended;
    private long preparationEpoch;
    private CompletableFuture<Decision> pending;
    private String statusKey = "";
    public int tab, page, textPage;
    public boolean refreshOnReturn;
    // The current panel has no editable draft. Future editors must store drafts here.

    public IslandPanelState(Consumer<IslandPanelRequestPayload> send) { this.send = send; }
    public IslandPanelPayload snapshot() { return snapshot; }
    public List<IslandPanelPayload.Entry> entries() { return snapshot.entries().stream().filter(e -> e.tab() == tab).toList(); }
    public IslandPanelPayload.Entry entry() { return entries().get(page); }
    public boolean pending() { return pending != null; }
    public boolean valid() { return valid && !closed; }
    public long session() { return session; }
    public String statusKey() { return statusKey; }
    public CompletionStage<Decision> refresh() {
        if (closed) return CompletableFuture.completedFuture(Decision.deny("session_closed"));
        if (pending != null) return pending;
        valid = false;
        return request("open", 0);
    }
    public CompletionStage<Decision> act(String action) {
        if (!valid() || pending() || entries().isEmpty() || entry().actions().stream().noneMatch(a -> a.id().equals(action)))
            return CompletableFuture.completedFuture(Decision.deny("unavailable"));
        return request(action, snapshot.revision());
    }
    private CompletionStage<Decision> request(String action, long revision) {
        var result = new CompletableFuture<Decision>();
        pending = result; pendingRequest = ++nextRequest; ticks = 100; statusKey = "";
        try { send.accept(new IslandPanelRequestPayload(revision, action, session, pendingRequest)); }
        catch (RuntimeException failure) { valid = false; finish(Decision.deny("unavailable")); }
        return result;
    }
    public boolean update(IslandPanelPayload value) {
        if (closed || value.session() != session || value.revision() <= snapshot.revision()) return false;
        var previous = entries().isEmpty() ? null : entry();
        boolean first = snapshot.revision() == 0;
        snapshot = value;
        if (first && value.entries().stream().anyMatch(e -> e.tab() == 2 && !e.actions().isEmpty())) tab = 2;
        if (entries().isEmpty()) { tab = 0; page = 0; textPage = 0; }
        page = Math.min(page, Math.max(0, entries().size() - 1));
        if (!entries().isEmpty() && !entry().equals(previous)) textPage = 0;
        if (pending != null && value.request() == pendingRequest) {
            valid = !suspended;
            finish(value.accepted() ? Decision.allow() : Decision.deny("rejected"));
        }
        return true;
    }
    public CompletionStage<Decision> prepareBackground() {
        return guard(pending != null ? pending : CompletableFuture.completedFuture(valid() ? Decision.allow() : Decision.deny("unavailable")));
    }
    public CompletionStage<Decision> prepareForeground() {
        suspended = false;
        long epoch = preparationEpoch;
        // A receipt from the old foreground is not a fresh foreground validation.
        var work = pending == null ? refresh() : pending.thenCompose(result -> {
            if (!result.accepted() || epoch != preparationEpoch || closed) return CompletableFuture.completedFuture(Decision.deny("unavailable"));
            return refresh();
        });
        return guard(work);
    }
    private CompletionStage<Decision> guard(CompletionStage<Decision> work) {
        long epoch = preparationEpoch;
        return work.thenApply(result -> epoch == preparationEpoch && !closed ? result : Decision.deny("session_closed"));
    }
    public void tick() {
        if (pending != null && --ticks <= 0) { valid = false; finish(Decision.deny("timeout")); }
    }
    /** Invalidate preparations, but keep correlated operation receipt alive across suspension. */
    public void suspend() { valid = false; suspended = true; preparationEpoch++; }
    private void finish(Decision decision) {
        statusKey = decision.key();
        var result = pending; pending = null; pendingRequest = 0;
        if (result != null) result.complete(decision);
    }
    public void close(boolean disconnect) {
        if (closed) return;
        closed = true; valid = false; preparationEpoch++;
        finish(Decision.deny("session_closed"));
        snapshot = new IslandPanelPayload(0, false, List.of());
        statusKey = "";
        tab = page = textPage = 0;
        if (!disconnect) send.accept(new IslandPanelRequestPayload(0, "close", session, 0));
    }
}
