package cl.treecs.skyblockmulti;

import cl.treecs.skyblockmulti.api.party.PartyApi;
import cl.treecs.skyblockmulti.api.party.PartySnapshot;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;

import static net.minecraft.commands.Commands.literal;

/**
 * Diagnóstico puntual de SkyblockMulti.
 *
 * Comando:
 *   /skyblockmulti debug
 *
 * Muestra una sola línea en el chat con el estado actual.
 */
public final class SkyblockDebugOverlay implements ModInitializer {

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("skyblockmulti")
                                .then(
                                        literal("debug")
                                                .executes(context -> {
                                                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                                                        return 0;
                                                    }

                                                    showDebugSnapshot(
                                                            context.getSource().getServer(),
                                                            player
                                                    );
                                                    return 1;
                                                })
                                )
                )
        );

        System.out.println("[Skyblock Multi] Debug snapshot disponible con /skyblockmulti debug");
    }

    private static void showDebugSnapshot(MinecraftServer server, ServerPlayer player) {
        PartySnapshot partyInfo = PartyApi.snapshot(player);

        boolean inParty = partyInfo.inParty();
        boolean leader = inParty && partyInfo.owner();
        String playerName = player.getGameProfile().name();

        String partyText = inParty ? "Yes" : "No";
        String partyColor = inParty ? "green" : "red";
        String leaderText = leader ? "Yes" : "No";
        String leaderColor = leader ? "gold" : "gray";

        String json = "["
                + "{\"text\":\"SB\",\"color\":\"aqua\",\"bold\":true},"
                + "{\"text\":\" | State: \",\"color\":\"gray\"},"
                + score(playerName, "sb3_state", "white") + ","
                + "{\"text\":\" | Slot: \",\"color\":\"gray\"},"
                + score(playerName, "sb3_slot", "yellow") + ","
                + "{\"text\":\" | OWN X: \",\"color\":\"gray\"},"
                + score(playerName, "sb3_x", "white") + ","
                + "{\"text\":\" Z: \",\"color\":\"gray\"},"
                + score(playerName, "sb3_z", "white") + ","
                + "{\"text\":\" | ACTIVE X: \",\"color\":\"gray\"},"
                + score(playerName, "sb_active_x", "green") + ","
                + "{\"text\":\" Z: \",\"color\":\"gray\"},"
                + score(playerName, "sb_active_z", "green") + ","
                + "{\"text\":\" | Party: \",\"color\":\"gray\"},"
                + jsonLiteral(partyText, partyColor) + ","
                + "{\"text\":\" | Leader: \",\"color\":\"gray\"},"
                + jsonLiteral(leaderText, leaderColor) + ","
                + "{\"text\":\" | Provider: \",\"color\":\"gray\"},"
                + jsonLiteral(PartyApi.activeProviderId(), "aqua")
                + "]";

        try {
            new ServerCommandExecutor(server).run(
                    "tellraw " + playerName + " " + json
            );
        } catch (Exception e) {
            System.err.println(
                    "[Skyblock Multi] No fue posible mostrar el debug snapshot para "
                            + playerName + ": " + e
            );
        }
    }

    private static String score(String playerName, String objective, String color) {
        return "{\"score\":{\"name\":\"" + playerName
                + "\",\"objective\":\"" + objective
                + "\"},\"color\":\"" + color + "\"}";
    }

    private static String jsonLiteral(String text, String color) {
        return "{\"text\":\"" + text + "\",\"color\":\"" + color + "\"}";
    }

    private static final class ServerCommandExecutor {
        private final Object source;
        private final Object commands;
        private final Object dispatcher;
        private final Method commandMethod;
        private final Method dispatcherMethod;

        private ServerCommandExecutor(Object server) throws ReflectiveOperationException {
            Class<?> commandsClass = runtimeClass(
                    "net.minecraft.commands.Commands",
                    "net.minecraft.class_2170"
            );
            Class<?> sourceClass = runtimeClass(
                    "net.minecraft.commands.CommandSourceStack",
                    "net.minecraft.class_2168"
            );
            this.commands = invokeNoArgReturning(server, commandsClass);
            this.source = invokeNoArgReturning(server, sourceClass);
            this.commandMethod = findCommandMethod(commands.getClass(), sourceClass);

            if (this.commandMethod == null) {
                this.dispatcher = findDispatcher(commands);
                this.dispatcherMethod = findExecuteMethod(dispatcher.getClass());
            } else {
                this.dispatcher = null;
                this.dispatcherMethod = null;
            }
        }

        private int run(String command) throws ReflectiveOperationException {
            String clean = command.startsWith("/") ? command.substring(1) : command;
            Object result;

            if (commandMethod != null) {
                Class<?>[] params = commandMethod.getParameterTypes();
                result = params[0] == String.class
                        ? commandMethod.invoke(commands, clean, source)
                        : commandMethod.invoke(commands, source, clean);
            } else {
                result = dispatcherMethod.invoke(dispatcher, clean, source);
            }

            return result instanceof Number number ? number.intValue() : 0;
        }

        private static Method findCommandMethod(Class<?> commandsClass, Class<?> sourceClass) {
            Method fallback = null;

            for (Method method : commandsClass.getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 2) continue;

                boolean sourceString =
                        sourceClass.isAssignableFrom(params[0]) && params[1] == String.class;
                boolean stringSource =
                        params[0] == String.class && sourceClass.isAssignableFrom(params[1]);

                if (!sourceString && !stringSource) continue;

                String name = method.getName().toLowerCase(Locale.ROOT);
                if (name.contains("perform")
                        || name.contains("prefixed")
                        || name.contains("execute")) {
                    return method;
                }

                fallback = method;
            }

            return fallback;
        }

        private static Object findDispatcher(Object commands)
                throws ReflectiveOperationException {
            for (Method method : commands.getClass().getMethods()) {
                if (method.getParameterCount() == 0
                        && method.getReturnType().getName()
                                .equals("com.mojang.brigadier.CommandDispatcher")) {
                    return method.invoke(commands);
                }
            }

            throw new NoSuchMethodException("Commands -> CommandDispatcher");
        }

        private static Method findExecuteMethod(Class<?> dispatcherClass)
                throws NoSuchMethodException {
            for (Method method : dispatcherClass.getMethods()) {
                Class<?>[] params = method.getParameterTypes();

                if (method.getName().equals("execute")
                        && params.length == 2
                        && params[0] == String.class) {
                    return method;
                }
            }

            throw new NoSuchMethodException("CommandDispatcher.execute(String, source)");
        }

        private static Object invokeNoArgReturning(Object owner, Class<?> returnType)
                throws ReflectiveOperationException {
            for (Method method : owner.getClass().getMethods()) {
                if (method.getParameterCount() == 0
                        && returnType.isAssignableFrom(method.getReturnType())) {
                    return method.invoke(owner);
                }
            }

            throw new NoSuchMethodException(
                    owner.getClass().getName() + " -> " + returnType.getName()
            );
        }

        private static Class<?> runtimeClass(String namedClass, String intermediaryClass)
                throws ClassNotFoundException {
            MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();
            Collection<String> namespaces = resolver.getNamespaces();

            if (namespaces.contains("intermediary")) {
                try {
                    return Class.forName(
                            resolver.mapClassName("intermediary", intermediaryClass)
                    );
                } catch (RuntimeException | ClassNotFoundException ignored) {
                }
            }

            if (namespaces.contains("named")) {
                try {
                    return Class.forName(
                            resolver.mapClassName("named", namedClass)
                    );
                } catch (RuntimeException | ClassNotFoundException ignored) {
                }
            }

            try {
                return Class.forName(intermediaryClass);
            } catch (ClassNotFoundException ignored) {
                return Class.forName(namedClass);
            }
        }
    }
}
