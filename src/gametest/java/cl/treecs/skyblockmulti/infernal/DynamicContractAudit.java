package cl.treecs.skyblockmulti.infernal;

import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import java.util.ArrayList;
import java.util.Map;

/** Focused negative probes for documented dynamic-trial isolation and spawn staggering. */
public final class DynamicContractAudit {
    public static void run(ClientGameTestContext context) {
        var failures = new ArrayList<String>();
        int[] healthMaximum = new int[1];
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var arena = InfernalDefinitionReloadListener.parseArena("audit:arena", JsonParser.parseString("""
                    {"schema_version":1,"dimension":"minecraft:overworld","origin":[1000,200,1000],
                    "cleanup_radius":32,"floors":{"base":[[6,0,0]]}}
                    """).getAsJsonObject());
                var trial = InfernalDefinitionReloadListener.parseTrial("audit:trial", JsonParser.parseString("""
                    {"schema_version":1,"arena":"audit:arena",
                    "activations":[{"item":"minecraft:blaze_powder"}],"countdown_seconds":0,"cooldown_seconds":0,
                    "cleanup_mobs":false,"cleanup_items":false,
                    "phases":[{"mobs":[{"entity":"minecraft:blaze","count":3,"floor":"base","spawn_delay_ticks":200}]}]}
                    """).getAsJsonObject(), Map.of(arena.id(), arena));
                InfernalDataRegistry.replace(new InfernalDataRegistry.Snapshot(Map.of(arena.id(), arena), Map.of(trial.id(), trial)));
                command(server, "forceload add 976 976 1024 1024");
                command(server, "fill 990 199 990 1010 199 1010 minecraft:stone");
                command(server, "gamemode creative @a");
                command(server, "tp @a 1000.5 200 1000.5");
                command(server, "summon minecraft:slime 1008 200 1000 {Tags:[\"audit_unrelated\"],NoAI:1b,Size:0}");
                command(server, "summon minecraft:item 1000.5 200 1000.5 {PickupDelay:32767s,Item:{id:\"minecraft:blaze_powder\",count:1}}");
            });
            context.waitTicks(40);
            world.getServer().runOnServer(server -> {
                var unrelated = server.overworld().getEntities(EntityTypeTest.forClass(Entity.class), e -> e.entityTags().contains("audit_unrelated"));
                check(unrelated.size() == 1, "unrelated slime fixture exists", failures);
                check(unrelated.stream().noneMatch(e -> e.entityTags().contains("skyblockmulti_dynamic_enemy")),
                        "unrelated slime remains outside encounter membership", failures);
                long blazes = server.overworld().getEntities(EntityTypeTest.forClass(Entity.class),
                        e -> e.entityTags().contains("skyblockmulti_dynamic_enemy") && !e.entityTags().contains("audit_unrelated")).size();
                System.out.println("[DYNAMIC-CONTRACT] observed " + blazes + " blazes before 200-tick spawn delay");
                check(blazes < 3, "spawn delay prevents whole group spawning immediately", failures);
                var bar = server.getCustomBossEvents().get(net.minecraft.resources.Identifier.parse("skyblockmulti:dynamic_trial"));
                healthMaximum[0] = bar.max();
                check(bar.max() >= 600, "health maximum includes three planned blazes", failures);
                server.overworld().getEntities(EntityTypeTest.forClass(Entity.class),
                        e -> e.entityTags().contains("skyblockmulti_dynamic_enemy")).forEach(Entity::discard);
            });
            context.waitTicks(60);
            world.getServer().runOnServer(server -> check(!InfernalTrialRunner.activeParticipants().isEmpty(),
                    "empty arena cannot finish while spawns are pending", failures));
            context.waitTicks(130);
            world.getServer().runOnServer(server -> {
                var bar = server.getCustomBossEvents().get(net.minecraft.resources.Identifier.parse("skyblockmulti:dynamic_trial"));
                check(bar.max() == healthMaximum[0] && bar.value() < bar.max(),
                        "delayed spawn preserves phase health maximum and damage progress", failures);
                check(server.overworld().getEntities(EntityTypeTest.forClass(Entity.class),
                        e -> e.isAlive() && e.entityTags().contains("skyblockmulti_dynamic_enemy")).size() == 1,
                        "second enemy appears after the configured interval", failures);
                InfernalTrialRunner.abandonActive(server);
            });
            context.waitTicks(30);
            world.getServer().runOnServer(server -> check(server.overworld().getEntities(EntityTypeTest.forClass(Entity.class),
                    e -> e.isAlive() && e.entityTags().contains("audit_unrelated")).size() == 1,
                    "abandonment preserves unrelated slime", failures));
            context.waitTicks(220);
            world.getServer().runOnServer(server -> check(server.overworld().getEntities(EntityTypeTest.forClass(Entity.class),
                    e -> e.entityTags().contains("skyblockmulti_dynamic_enemy")).isEmpty(),
                    "abandonment cancels the third pending spawn", failures));
            world.getServer().runOnServer(server -> {
                command(server, "summon minecraft:slime 1006 200 1000 {Tags:[\"family_probe\",\"skyblockmulti_dynamic_enemy\",\"skyblockmulti_dynamic_children\",\"skyblockmulti_dynamic_victory\",\"sb_dyn_999\"],NoAI:1b,Size:2}");
                command(server, "summon minecraft:magma_cube 1008 200 1000 {Tags:[\"family_probe\",\"skyblock_infernal_enemy\"],NoAI:1b,Size:2}");
                command(server, "kill @e[tag=family_probe]");
            });
            context.waitTicks(30);
            world.getServer().runOnServer(server -> {
                var children = server.overworld().getEntities(EntityTypeTest.forClass(net.minecraft.world.entity.monster.cubemob.AbstractCubeMob.class),
                        e -> e.isAlive() && (e.entityTags().contains("sb_dyn_999") || e.entityTags().contains("skyblock_infernal_enemy")));
                check(children.size() >= 4, "both cube families produce tagged descendants", failures);
                check(children.stream().filter(e -> e.entityTags().contains("sb_dyn_999"))
                        .allMatch(e -> e.entityTags().contains("skyblockmulti_dynamic_victory")),
                        "dynamic children inherit victory membership", failures);
                children.forEach(e -> { e.addTag("skyblockmulti_cleanup"); e.discard(); });
            });
            context.waitTicks(60);
            world.getServer().runOnServer(server -> check(server.overworld().getEntities(EntityTypeTest.forClass(Entity.class),
                    e -> e.entityTags().contains("sb_dyn_999") || e.entityTags().contains("skyblock_infernal_enemy")).isEmpty(),
                    "cleanup removes both families without grandchildren", failures));
            world.getServer().runOnServer(server -> {
                var snapshot = InfernalDataRegistry.snapshot();
                var trial = snapshot.trials().get("audit:trial");
                var cleanupTrial = new InfernalDefinitions.Trial(trial.id(), trial.arena(), trial.displayJson(),
                        trial.activations(), trial.countdownSeconds(), trial.cooldownSeconds(), true, true,
                        trial.spawnReleaseSeconds(), trial.phases(), trial.scaling(), trial.rewards());
                InfernalDataRegistry.replace(new InfernalDataRegistry.Snapshot(snapshot.arenas(), Map.of(trial.id(), cleanupTrial)));
                command(server, "summon minecraft:magma_cube 1008 200 1000 {Tags:[\"audit_unrelated\"],NoAI:1b,Size:2}");
                command(server, "summon minecraft:item 1000.5 200 1000.5 {PickupDelay:32767s,Item:{id:\"minecraft:blaze_powder\",count:1}}");
            });
            context.waitTicks(40);
            world.getServer().runOnServer(server -> {
                check(server.overworld().getEntities(EntityTypeTest.forClass(net.minecraft.world.entity.monster.cubemob.AbstractCubeMob.class),
                        e -> e.isAlive()).isEmpty(), "enabled initial cleanup clears slime and magma families", failures);
                InfernalTrialRunner.abandonActive(server);
            });
        }
        if (!failures.isEmpty()) throw new AssertionError(failures.toString());
    }
    private static void command(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }
    private static void check(boolean passed, String name, java.util.List<String> failures) {
        System.out.println("[DYNAMIC-CONTRACT] " + (passed ? "PASS " : "FAIL ") + name);
        if (!passed) failures.add(name);
    }
}
