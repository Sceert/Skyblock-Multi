package cl.treecs.skyblockmulti.api.addon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import java.util.*;
import java.util.function.Function;

/** Generic, side-effect-free reward providers. Register during mod initialization. */
public final class AddonRewardApi {
    public static final int API_VERSION = 1;
    private static final Map<String, Function<StarterContext, List<ItemGrant>>> STARTER = new TreeMap<>();
    private static final Map<String, Function<TrialContext, List<ItemGrant>>> TRIAL = new TreeMap<>();
    private AddonRewardApi() {}

    public record ItemGrant(String item, int count) {
        public ItemGrant {
            Identifier.parse(item);
            if (count < 1 || count > 64) throw new IllegalArgumentException("Count must be 1..64");
        }
    }
    public record StarterContext(UUID player) {}
    public record TrialContext(UUID player, String trialId, String completionId, int level, int difficulty) {}

    public static void registerStarter(String id, Function<StarterContext, List<ItemGrant>> provider) {
        register(STARTER, id, provider);
    }
    public static void registerTrial(String id, Function<TrialContext, List<ItemGrant>> provider) {
        register(TRIAL, id, provider);
    }
    private static <T> void register(Map<String, T> registry, String id, T provider) {
        Identifier.parse(id);
        if (registry.putIfAbsent(id, Objects.requireNonNull(provider)) != null)
            throw new IllegalArgumentException("Duplicate reward provider: " + id);
    }
    public static List<ItemGrant> starterItems(StarterContext context) { return collect(STARTER, context); }
    public static List<ItemGrant> trialItems(TrialContext context) { return collect(TRIAL, context); }
    private static <T> List<ItemGrant> collect(Map<String, Function<T, List<ItemGrant>>> providers, T context) {
        List<ItemGrant> result = new ArrayList<>();
        providers.forEach((id, provider) -> {
            try {
                List<ItemGrant> items = List.copyOf(provider.apply(context));
                if (items.size() > 16 || items.stream().anyMatch(item ->
                        BuiltInRegistries.ITEM.getOptional(Identifier.parse(item.item()))
                                .filter(value -> value != Items.AIR).isEmpty()))
                    throw new IllegalArgumentException("Invalid reward items");
                result.addAll(items);
            } catch (RuntimeException error) {
                System.err.println("[Skyblock Multi] Reward provider " + id + " failed: " + error);
            }
        });
        return List.copyOf(result);
    }
}
