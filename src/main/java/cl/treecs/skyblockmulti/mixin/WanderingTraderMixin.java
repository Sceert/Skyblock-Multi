package cl.treecs.skyblockmulti.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds renewable biome-dependent resources without replacing vanilla offers. */
@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin {
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void skyblockmulti$addSkyblockOffers(ServerLevel level, CallbackInfo ci) {
        add(Items.ARMADILLO_SCUTE, 2, 4, 8);
        add(Items.TURTLE_SCUTE, 2, 5, 8);
        add(Items.RABBIT_FOOT, 1, 4, 6);
        add(Items.INK_SAC, 4, 2, 8);
        add(Items.GLOW_INK_SAC, 2, 4, 6);
        add(Items.FROGSPAWN, 1, 6, 4);
        add(Items.GOAT_HORN, 1, 8, 2);
        add(Items.SNIFFER_EGG, 1, 16, 2);
    }

    private void add(Item result, int count, int emeraldCost, int maxUses) {
        WanderingTrader trader = (WanderingTrader) (Object) this;
        boolean alreadyPresent = trader.getOffers().stream()
                .anyMatch(offer -> offer.getResult().is(result));
        if (alreadyPresent) {
            return;
        }

        trader.getOffers().add(new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCost),
                new ItemStack(result, count),
                maxUses,
                1,
                0.05F
        ));
    }
}
