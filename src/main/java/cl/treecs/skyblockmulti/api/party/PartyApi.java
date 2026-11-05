package cl.treecs.skyblockmulti.api.party;

import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Public registration API used by optional party integrations. */
public final class PartyApi {
    public static final int API_VERSION = 1;
    public static final String SOLO_PROVIDER_ID = "skyblockmulti:solo";
    private static final List<PartyProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private PartyApi() {
    }

    public static void registerProvider(PartyProvider provider) {
        Objects.requireNonNull(provider, "provider");
        PROVIDERS.removeIf(existing -> existing.id().equals(provider.id()));
        PROVIDERS.add(provider);
        PROVIDERS.sort(Comparator.comparingInt(PartyProvider::priority).reversed());
        System.out.println("[Skyblock Multi] Party provider registrado: " + provider.id());
    }

    public static PartySnapshot snapshot(ServerPlayer player) {
        PartyProvider provider = activeProvider();
        if (provider == null) {
            return PartySnapshot.solo();
        }

        try {
            PartySnapshot snapshot = provider.snapshot(player);
            if (snapshot == null || !snapshot.inParty()) {
                return PartySnapshot.solo();
            }
            if (snapshot.partyId() == null
                    || snapshot.ownerUuid() == null
                    || snapshot.ownerName() == null
                    || snapshot.ownerName().isBlank()) {
                System.err.println("[Skyblock Multi] Party provider " + provider.id()
                        + " devolvió una party incompleta; se usará modo individual.");
                return PartySnapshot.solo();
            }
            return snapshot;
        } catch (RuntimeException exception) {
            System.err.println("[Skyblock Multi] Party provider " + provider.id()
                    + " falló; se usará modo individual para esta consulta: " + exception);
            return PartySnapshot.solo();
        }
    }

    public static String activeProviderId() {
        PartyProvider provider = activeProvider();
        return provider == null ? SOLO_PROVIDER_ID : provider.id();
    }

    public static boolean hasExternalProvider() {
        return activeProvider() != null;
    }

    public static java.util.Optional<String> partyName(ServerPlayer player) {
        PartyProvider provider = activeProvider();
        if (provider == null) return java.util.Optional.empty();
        try {
            String name = provider.partyName(player);
            return name == null || name.isBlank() ? java.util.Optional.empty()
                    : java.util.Optional.of(name.substring(0, Math.min(name.length(), 128)));
        } catch (RuntimeException unavailable) {
            return java.util.Optional.empty();
        }
    }

    public static Boolean isMember(net.minecraft.server.MinecraftServer server,
                                   java.util.UUID partyId, java.util.UUID playerId) {
        PartyProvider provider = activeProvider();
        if (provider == null) return null;
        try {
            return provider.isMember(server, partyId, playerId);
        } catch (RuntimeException unavailable) {
            return null; // Unknown membership must never revoke a personal home.
        }
    }

    static void clearProvidersForTesting() {
        PROVIDERS.clear();
    }

    private static PartyProvider activeProvider() {
        for (PartyProvider provider : PROVIDERS) {
            try {
                if (provider.isAvailable()) {
                    return provider;
                }
            } catch (RuntimeException exception) {
                System.err.println("[Skyblock Multi] No fue posible consultar el party provider "
                        + provider.id() + ": " + exception);
            }
        }
        return null;
    }
}
