package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.island.PanelAppearance;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.network.chat.Component;

final class PanelAppearanceConfig {
    private PanelAppearanceConfig() {}
    static Runnable add(ConfigBuilder builder) {
        var current = ClientPanelAppearance.get();
        var defaults = PanelAppearance.DEFAULT;
        int[] values = {current.width(), current.height(), current.background(), current.header(), current.border(),
                current.label(), current.value(), current.accent(), current.hudX(), current.hudY()};
        int[] reset = {defaults.width(), defaults.height(), defaults.background(), defaults.header(), defaults.border(),
                defaults.label(), defaults.value(), defaults.accent(), defaults.hudX(), defaults.hudY()};
        String[] names = {"width", "height", "background", "header", "border", "label", "value", "accent", "hud_x", "hud_y"};
        var category = builder.getOrCreateCategory(Component.translatable("skyblockmulti.panel.config.title"));
        var entries = builder.entryBuilder();
        category.addEntry(entries.startTextDescription(Component.translatable("skyblockmulti.panel.config.description")).build());
        for (int i = 0; i < values.length; i++) {
            final int index = i;
            Component title = Component.translatable("skyblockmulti.panel.config." + names[i]);
            if (i >= 2 && i <= 7) category.addEntry(entries.startColorField(title, values[i])
                    .setDefaultValue(reset[i]).setSaveConsumer(value -> values[index] = value).build());
            else category.addEntry(entries.startIntSlider(title, values[i], i == 0 ? 220 : i == 1 ? 210 : 0,
                            i == 0 ? 420 : i == 1 ? 360 : 100).setDefaultValue(reset[i])
                    .setSaveConsumer(value -> values[index] = value).build());
        }
        return () -> ClientPanelAppearance.save(new PanelAppearance(values[0], values[1], values[2], values[3], values[4],
                values[5], values[6], values[7], values[8], values[9]));
    }
}
