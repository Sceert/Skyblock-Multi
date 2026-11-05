package cl.treecs.skyblockmulti.infernal;

import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import java.util.Map;

/** Parsed definitions injected into a disposable world; does not test resource-pack reload. */
public final class DynamicTrialAudit {
    public static void run(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var arena = InfernalDefinitionReloadListener.parseArena("audit:arena", JsonParser.parseString("""
                    {"schema_version":1,"dimension":"minecraft:overworld","origin":[1000,200,1000],
                    "cleanup_radius":32,"floors":{"base":[[6,0,0]]}}
                    """).getAsJsonObject());
                var trial = InfernalDefinitionReloadListener.parseTrial("audit:trial", JsonParser.parseString("""
                    {"schema_version":1,"arena":"audit:arena","display":{"text":"Audit"},
                    "activations":[{"item":"minecraft:blaze_powder","consume":true}],
                    "countdown_seconds":1,"cooldown_seconds":3,"spawn_release_seconds":1,
                    "cleanup_mobs":false,"cleanup_items":false,
                    "phases":[{"id":"first","mobs":[{"entity":"minecraft:blaze","count":2,"floor":"base"}]},
                    {"id":"second","delay_after_previous_seconds":1,
                    "mobs":[{"entity":"minecraft:blaze","count":1,"floor":"base","champion":true}]}]}
                    """).getAsJsonObject(), Map.of(arena.id(), arena));
                InfernalDataRegistry.replace(new InfernalDataRegistry.Snapshot(Map.of(arena.id(), arena), Map.of(trial.id(), trial)));
                command(server, "forceload add 976 976 1024 1024");
                command(server, "fill 990 199 990 1010 199 1010 minecraft:stone");
                command(server, "gamemode creative @a");
                command(server, "tp @a 1000.5 200 1000.5");
                command(server, "summon minecraft:blaze 1008 200 1000 {Tags:[\"audit_unrelated\"],NoAI:1b}");
                catalyst(server);
            });
            context.waitTicks(50);
            world.getServer().runOnServer(server -> {
                check(enemies(server) == 2, "first phase spawns two enemies");
                check(InfernalTrialRunner.activeParticipants().size() == 1, "solo participant snapshot");
                check(InfernalTrialRunner.isSpawnLocked(server.overworld()), "active encounter locks spawning");
                check(server.overworld().getEntities(EntityTypeTest.forClass(Entity.class), e -> e.entityTags().contains("audit_unrelated")).size() == 1,
                        "cleanup disabled preserves unrelated mob");
                discardEnemies(server);
            });
            context.waitTicks(45);
            world.getServer().runOnServer(server -> {
                check(enemies(server) == 1, "second phase starts after victory and delay");
                check(server.overworld().getEntities(EntityTypeTest.forClass(Entity.class), e -> e.entityTags().contains("skyblockmulti_dynamic_champion")).size() == 1,
                        "second phase champion tagged");
                discardEnemies(server);
            });
            context.waitTicks(15);
            world.getServer().runOnServer(server -> {
                check(InfernalTrialRunner.activeParticipants().isEmpty(), "completion clears active participant snapshot");
                catalyst(server);
            });
            context.waitTicks(20);
            world.getServer().runOnServer(server -> check(enemies(server) == 0, "cooldown prevents immediate restart"));
            context.waitTicks(80);
            world.getServer().runOnServer(server -> {
                check(enemies(server) == 2, "retained catalyst starts next run after cooldown");
                InfernalTrialRunner.abandonActive(server);
                check(InfernalTrialRunner.activeParticipants().isEmpty(), "explicit abandonment clears participants");
            });
            context.waitTicks(30);
            world.getServer().runOnServer(server -> {
                check(enemies(server) == 0, "abandonment removes encounter enemies");
                check(!InfernalTrialRunner.isSpawnLocked(server.overworld()), "spawn lock released after configured grace");
                check(server.overworld().getEntities(EntityTypeTest.forClass(Entity.class), e -> e.entityTags().contains("audit_unrelated")).size() == 1,
                        "abandonment preserves unrelated mob");
            });
        }
    }
    private static long enemies(MinecraftServer server) {
        return server.overworld().getEntities(EntityTypeTest.forClass(Entity.class), e -> e.isAlive() && e.entityTags().contains("skyblockmulti_dynamic_enemy")).size();
    }
    private static void discardEnemies(MinecraftServer server) {
        server.overworld().getEntities(EntityTypeTest.forClass(Entity.class), e -> e.entityTags().contains("skyblockmulti_dynamic_enemy")).forEach(Entity::discard);
    }
    private static void catalyst(MinecraftServer server) {
        command(server, "summon minecraft:item 1000.5 200 1000.5 {PickupDelay:32767s,Item:{id:\"minecraft:blaze_powder\",count:1}}");
    }
    private static void command(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }
    private static void check(boolean passed, String name) {
        System.out.println("[DYNAMIC-AUDIT] " + (passed ? "PASS " : "FAIL ") + name);
        if (!passed) throw new AssertionError(name);
    }
}
