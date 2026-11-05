package cl.treecs.skyblockmulti.panels;

import cl.treecs.skyblockmulti.infernal.InfernalRewardLedger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import java.util.*;

/** Spawn/equipment/health/cleanup probes, not a combat-balance or multiplayer test. */
final class TrialsAudit {
    private static final List<String> failures = new ArrayList<>();
    static void run(ClientGameTestContext context) {
        failures.clear();
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var nether = server.getLevel(Level.NETHER);
                // Generated test save only. Keep the arena chunks loaded for command selectors.
                command(server, "execute in minecraft:the_nether run forceload add -32 -32 32 32");
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) nether.getChunk(x, z);
            });
            context.waitTicks(30);
            world.getServer().runOnServer(server -> {
                var nether = server.getLevel(Level.NETHER);
                for (int tier = 1; tier <= 10; tier++) {
                    enemies(nether).forEach(Entity::discard);
                    command(server, "scoreboard players set #trial_level sb_infernal " + tier);
                    command(server, "scoreboard players set #trial_difficulty sb_infernal 2");
                    command(server, "scoreboard players set #participant_count sb_infernal 1");
                    command(server, "execute in minecraft:the_nether run function skyblockmulti:infernal_trial/begin_wave");
                    check(!enemies(nether).isEmpty(), "tier " + tier + " spawns phase one");
                    check(armed(nether), "tier " + tier + " melee/ranged humanoids have weapons");
                    if (tier >= 7) {
                        enemies(nether).forEach(Entity::discard);
                        command(server, "execute in minecraft:the_nether run function skyblockmulti:infernal_trial/spawn_phase_2_start");
                        check(!enemies(nether).isEmpty(), "tier " + tier + " spawns phase two");
                        check(armed(nether), "tier " + tier + " phase two humanoids have weapons");
                    }
                }
                enemies(nether).forEach(Entity::discard);
                command(server, "execute in minecraft:the_nether run function skyblockmulti:infernal_trial/spawn_phase_3_champion");
                var champion = enemies(nether).stream().filter(e -> e.entityTags().contains("skyblock_infernal_champion")).findFirst().orElseThrow();
                check(champion.getMaxHealth() >= 160 && champion.getAttributeValue(Attributes.SCALE) > 1,
                        "champion has increased size and health");
                check(champion.hasCustomName() && !champion.getMainHandItem().isEmpty(), "champion has name and weapon");
                enemies(nether).forEach(Entity::discard);
                command(server, "execute in minecraft:the_nether run summon minecraft:blaze 0 100 0 {Tags:[\"skyblock_infernal_enemy\"],NoAI:1b}");
                command(server, "execute in minecraft:the_nether run summon minecraft:blaze 2 100 0 {Tags:[\"skyblock_infernal_enemy\"],NoAI:1b}");
                var update = InfernalRewardLedger.class.getDeclaredMethod("updateHealthBar", MinecraftServer.class);
                update.setAccessible(true);
                update.invoke(null, server);
                var bar = server.getCustomBossEvents().get(Identifier.parse("skyblockmulti:infernal_trial"));
                int maximum = bar.max();
                enemies(nether).getFirst().discard();
                update.invoke(null, server);
                check(bar.max() == maximum && bar.value() > 0 && bar.value() < maximum,
                        "defeated enemy lowers health bar without shrinking maximum");
                enemies(nether).forEach(Entity::discard);
                command(server, "execute in minecraft:the_nether run summon minecraft:magma_cube 0 100 0 {Tags:[\"audit_unrelated\"],NoAI:1b,Size:0}");
                command(server, "execute in minecraft:the_nether run summon minecraft:blaze 4 100 0 {Tags:[\"audit_unrelated\"],NoAI:1b}");
                command(server, "execute in minecraft:the_nether run summon minecraft:magma_cube 8 100 0 {Tags:[\"skyblock_infernal_enemy\"],NoAI:1b,Size:2}");
                command(server, "function skyblockmulti:infernal_trial/abandon");
            });
            context.waitTicks(100);
            world.getServer().runOnServer(server -> {
                var nether = server.getLevel(Level.NETHER);
                var unrelated = nether.getEntities(EntityTypeTest.forClass(LivingEntity.class), e -> e.isAlive() && e.entityTags().contains("audit_unrelated"));
                check(unrelated.stream().noneMatch(e -> e.getType() == EntityTypes.MAGMA_CUBE), "regional cleanup removes all arena magma cubes as specified");
                check(unrelated.stream().anyMatch(e -> e.getType() == EntityTypes.BLAZE), "abandon preserves unrelated blaze inside arena");
                check(enemies(nether).isEmpty(), "abandon removes trial enemies and tagged descendants");
                enemies(nether).forEach(e -> System.out.println("[TRIALS-AUDIT] survivor " + e.getType()
                        + " at " + e.position() + " tags=" + e.entityTags()));
            });
        } catch (Exception error) { throw new RuntimeException(error); }
        if (!failures.isEmpty()) throw new AssertionError("Trials audit failures: " + failures);
    }
    private static List<? extends LivingEntity> enemies(ServerLevel level) {
        return level.getEntities(EntityTypeTest.forClass(LivingEntity.class), e -> e.isAlive() && e.entityTags().contains("skyblock_infernal_enemy"));
    }
    private static boolean armed(ServerLevel level) {
        return enemies(level).stream().filter(e -> e.getType() == EntityTypes.WITHER_SKELETON
                || e.getType() == EntityTypes.PIGLIN || e.getType() == EntityTypes.PIGLIN_BRUTE).allMatch(e -> !e.getMainHandItem().isEmpty());
    }
    private static void command(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }
    private static void check(boolean passed, String name) {
        System.out.println("[TRIALS-AUDIT] " + (passed ? "PASS " : "FAIL ") + name);
        if (!passed) failures.add(name);
    }
}
