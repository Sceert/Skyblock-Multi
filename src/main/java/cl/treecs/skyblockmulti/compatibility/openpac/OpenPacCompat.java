package cl.treecs.skyblockmulti.compatibility.openpac;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions;

import java.util.UUID;

public final class OpenPacCompat {

    public static final String MOD_ID = "openpartiesandclaims";
    public static final String ASCENSION_NEXUS_CANONICAL_NAME = "The Ascension Nexus";
    private static final String ASCENSION_NEXUS_SUBCLAIM_ID = "ascension_nexus";
    private static final int ASCENSION_NEXUS_CLAIM_COLOR = 0x55D6FF; // celeste
    private static final UUID SERVER_CLAIM_UUID = new UUID(0L, 0L);

    // OpenPAC es una capa secundaria centrada en el domo. Se incluyen todos
    // los chunks que intersectan su círculo físico de radio 45, no un círculo
    // calculado a partir de índices de chunks.
    private static final int ASCENSION_NEXUS_DOME_RADIUS_BLOCKS = 45;
    private static final int LEGACY_NEXUS_SCAN_RADIUS_CHUNKS = 6;

    private OpenPacCompat() {
    }

    public static boolean isInstalled() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean isTerritorialIntegrationEnabled() {
        return isInstalled() && !FabricLoader.getInstance().isModLoaded("nexus_parties");
    }

    public static void initialize() {
        if (isInstalled() && FabricLoader.getInstance().isModLoaded("nexus_parties")) {
            System.out.println("[Skyblock Multi] Nexus Parties tiene prioridad; "
                    + "OpenPAC se limita al Server Claim permanente del Nexo.");
        } else if (isInstalled()) {
            System.out.println("[Skyblock Multi] OpenPAC detectado; integración opcional disponible.");
        } else {
            System.out.println("[Skyblock Multi] OpenPAC no instalado; integración desactivada.");
        }
    }

    /**
     * Mantiene el Server Claim central independientemente del proveedor de parties.
     * SkyblockMulti no crea ni administra claims de islas.
     */
    public static void prepareWorldClaims(MinecraftServer server) {
        if (isInstalled() && server != null) {
            reserveAscensionNexus(server);
        }
    }

    public static PartyInfo getPartyInfo(ServerPlayer player) {

        if (!isInstalled()) {
            return PartyInfo.noParty();
        }

        MinecraftServer server = player.level().getServer();

        if (server == null) {
            return PartyInfo.noParty();
        }

        IServerPartyAPI party = OpenPACServerAPI
                .get(server)
                .getPartyManager()
                .getPartyByMember(player.getUUID());

        if (party == null) {
            return PartyInfo.noParty();
        }

        UUID ownerUuid = party.getOwner().getUUID();
        String ownerName = party.getOwner().getUsername();

        return new PartyInfo(
                true,
                party.getId(),
                ownerUuid,
                ownerName,
                ownerUuid.equals(player.getUUID())
        );
    }

    /**
     * Server Claim permanente para la zona central.
     *
     * Incluye exactamente los chunks que intersectan el círculo físico del
     * domo de radio 45. OpenPAC protege toda la columna de esos chunks, pero
     * la fortaleza exterior depende únicamente de la protección nativa.
     *
     * Este claim nunca se transforma en player claim.
     */
    private static boolean reserveAscensionNexus(MinecraftServer server) {

        if (!isInstalled() || server == null) {
            return false;
        }

        Identifier overworld = Identifier.parse("minecraft:overworld");

        OpenPACServerAPI api = OpenPACServerAPI.get(server);

        IServerClaimsManagerAPI claims = api.getServerClaimsManager();

        /*
         * Usamos un sub-claim propio del servidor para que SOLO el centro
         * aparezca con el nombre "The Ascension Nexus".
         *
         * El sub-claim evita modificar la identidad global de los Server Claims.
         */
        IPlayerConfigAPI serverClaimsConfig = api
                .getPlayerConfigManager()
                .getServerClaimConfig();

        IPlayerConfigAPI nexusSubConfig =
                serverClaimsConfig.getSubConfig(ASCENSION_NEXUS_SUBCLAIM_ID);

        if (nexusSubConfig == null) {
            nexusSubConfig =
                    serverClaimsConfig.createSubConfig(ASCENSION_NEXUS_SUBCLAIM_ID);
        }

        if (nexusSubConfig == null) {
            System.err.println(
                    "[Skyblock Multi] OpenPAC: no fue posible crear el sub-claim "
                            + ASCENSION_NEXUS_SUBCLAIM_ID
                            + " para "
                            + ASCENSION_NEXUS_CANONICAL_NAME
                            + "."
            );
            return false;
        }

        IPlayerConfigAPI.SetResult nameResult = nexusSubConfig.tryToSet(
                PlayerConfigOptions.CLAIMS_NAME,
                ASCENSION_NEXUS_CANONICAL_NAME
        );

        IPlayerConfigAPI.SetResult nexusColorResult = nexusSubConfig.tryToSet(
                PlayerConfigOptions.CLAIMS_COLOR,
                ASCENSION_NEXUS_CLAIM_COLOR
        );

        /*
         * OpenPAC normally treats opening containers and trapdoors as a
         * protected block interaction. Let it delegate player interactions
         * inside the Nexus to SkyblockMulti's native protection instead:
         * native protection still denies breaking, placing and destructive
         * item use, while allowing the intended doors, lecterns and chests.
         */
        IPlayerConfigAPI.SetResult interactionResult = nexusSubConfig.tryToSet(
                PlayerConfigOptions.CLAIM_EXCEPTION_BLOCKS_BY_PLAYERS,
                "E"
        );
        IPlayerConfigAPI.SetResult itemUseResult = nexusSubConfig.tryToSet(
                PlayerConfigOptions.CLAIM_EXCEPTION_ITEM_USE,
                "E"
        );
        IPlayerConfigAPI.SetResult entityInteractionResult = nexusSubConfig.tryToSet(
                PlayerConfigOptions.CLAIM_EXCEPTION_ENTITIES_BY_PLAYERS,
                "E"
        );

        int nexusSubConfigIndex = nexusSubConfig.getSubIndex();

        int minChunk = Math.floorDiv(-ASCENSION_NEXUS_DOME_RADIUS_BLOCKS, 16);
        int maxChunk = Math.floorDiv(ASCENSION_NEXUS_DOME_RADIUS_BLOCKS, 16);

        /*
         * Primer pase: seguridad.
         * Se revisan únicamente los chunks que forman el círculo.
         * Nunca se pisa un claim de jugador.
         */
        for (int x = minChunk; x <= maxChunk; x++) {
            for (int z = minChunk; z <= maxChunk; z++) {

                if (!chunkIntersectsNexusDome(x, z)) {
                    continue;
                }

                IPlayerChunkClaimAPI existing = claims.get(overworld, x, z);

                if (existing != null
                        && !SERVER_CLAIM_UUID.equals(existing.getPlayerId())) {

                    System.err.println(
                            "[Skyblock Multi] OpenPAC: no fue posible reservar completamente "
                                    + ASCENSION_NEXUS_CANONICAL_NAME
                                    + ". El chunk "
                                    + x + "," + z
                                    + " pertenece a "
                                    + existing.getPlayerId()
                                    + "."
                    );

                    return false;
                }
            }
        }

        int claimedChunks = 0;

        /*
         * Segundo pase: reclamar cada chunk que contenga al menos un bloque
         * dentro del círculo físico del domo.
         */
        for (int x = minChunk; x <= maxChunk; x++) {
            for (int z = minChunk; z <= maxChunk; z++) {

                if (!chunkIntersectsNexusDome(x, z)) {
                    continue;
                }

                IPlayerChunkClaimAPI existing = claims.get(overworld, x, z);

                boolean alreadyCorrectNexusClaim =
                        existing != null
                                && SERVER_CLAIM_UUID.equals(existing.getPlayerId())
                                && existing.getSubConfigIndex() == nexusSubConfigIndex;

                if (!alreadyCorrectNexusClaim) {
                    claims.claim(
                            overworld,
                            SERVER_CLAIM_UUID,
                            nexusSubConfigIndex,
                            x,
                            z,
                            false
                    );
                }

                claimedChunks++;
            }
        }

        int removedLegacyChunks = 0;
        for (int x = -LEGACY_NEXUS_SCAN_RADIUS_CHUNKS;
             x <= LEGACY_NEXUS_SCAN_RADIUS_CHUNKS; x++) {
            for (int z = -LEGACY_NEXUS_SCAN_RADIUS_CHUNKS;
                 z <= LEGACY_NEXUS_SCAN_RADIUS_CHUNKS; z++) {
                if (chunkIntersectsNexusDome(x, z)) {
                    continue;
                }

                IPlayerChunkClaimAPI existing = claims.get(overworld, x, z);
                if (existing != null
                        && SERVER_CLAIM_UUID.equals(existing.getPlayerId())
                        && existing.getSubConfigIndex() == nexusSubConfigIndex) {
                    claims.unclaim(overworld, x, z);
                    removedLegacyChunks++;
                }
            }
        }

        System.out.println(
                "[Skyblock Multi] OpenPAC: "
                        + ASCENSION_NEXUS_CANONICAL_NAME
                        + " reservado permanentemente alrededor del domo. "
                        + "Radio=" + ASCENSION_NEXUS_DOME_RADIUS_BLOCKS
                        + " bloques, chunks reclamados=" + claimedChunks
                        + ", chunks heredados retirados=" + removedLegacyChunks
                        + ", sub-claim=" + ASCENSION_NEXUS_SUBCLAIM_ID
                        + ", nombre_resultado=" + nameResult
                        + ", color_nexus_resultado=" + nexusColorResult
                        + ", interacciones_resultado=" + interactionResult
                        + ", uso_items_resultado=" + itemUseResult
                        + ", entidades_resultado=" + entityInteractionResult
                        + "."
        );

        return true;
    }

    private static boolean chunkIntersectsNexusDome(int chunkX, int chunkZ) {
        int closestX = closestCoordinateToOrigin(chunkX);
        int closestZ = closestCoordinateToOrigin(chunkZ);
        int radius = ASCENSION_NEXUS_DOME_RADIUS_BLOCKS;
        return closestX * closestX + closestZ * closestZ <= radius * radius;
    }

    private static int closestCoordinateToOrigin(int chunkCoordinate) {
        int min = chunkCoordinate * 16;
        int max = min + 15;
        if (min > 0) {
            return min;
        }
        if (max < 0) {
            return -max;
        }
        return 0;
    }

    public record PartyInfo(
            boolean inParty,
            UUID partyId,
            UUID ownerUuid,
            String ownerName,
            boolean owner
    ) {

        public static PartyInfo noParty() {
            return new PartyInfo(
                    false,
                    null,
                    null,
                    null,
                    false
            );
        }
    }
}
