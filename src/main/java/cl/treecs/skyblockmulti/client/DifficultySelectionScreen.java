package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.network.DifficultySelectPayload;
import cl.treecs.skyblockmulti.network.BackToTreeSelectionPayload;
import cl.treecs.skyblockmulti.network.SelectionMenuClosedPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DifficultySelectionScreen extends CompactIslandScreen {
    private static final String[] NAMES = {"extreme", "hard", "standard", "easy"};
    private static final String[] DISPLAY_KEYS = {"empty", "basic", "standard", "beginner"};
    private boolean selectionContinues;

    public DifficultySelectionScreen() {
        super(Component.translatable("skyblockmulti.difficulty.title"));
    }

    @Override
    protected void init() {
        int centerX = left() + panelWidth() / 2;
        int startY = top() + 55;
        for (int index = 0; index < NAMES.length; index++) {
            int value = index + 1;
            String name = NAMES[index];
            String displayKey = DISPLAY_KEYS[index];
            Button button = Button.builder(
                            Component.translatable("skyblockmulti.difficulty." + name),
                            clicked -> confirmSelection(value, displayKey)
                    )
                    .bounds(left() + 10, startY + index * 24, innerWidth(), 20)
                    .tooltip(Tooltip.create(Component.translatable(
                            "skyblockmulti.difficulty." + name + ".hover"
                    )))
                    .build();
            button.active = ClientPlayNetworking.canSend(DifficultySelectPayload.TYPE);
            addRenderableWidget(button);
        }

        Button changeTreeButton = Button.builder(
                        Component.translatable("skyblockmulti.difficulty.change_tree"),
                        clicked -> backToTreeSelection()
                )
                .bounds(left() + 10, bottom() - 30, innerWidth(), 20)
                .build();
        changeTreeButton.active = ClientPlayNetworking.canSend(BackToTreeSelectionPayload.TYPE);
        addRenderableWidget(changeTreeButton);

        addCloseButton();
    }

    private void backToTreeSelection() {
        if (!ClientPlayNetworking.canSend(BackToTreeSelectionPayload.TYPE)) return;
        selectionContinues = true;
        ClientPlayNetworking.send(BackToTreeSelectionPayload.INSTANCE);
        onClose();
    }

    private void confirmSelection(int value, String displayKey) {
        Component difficulty = Component.translatable(
                "skyblockmulti.config.bonus_chest.mode." + displayKey
        );
        this.minecraft.setScreenAndShow(new IslandConfirmationScreen(
                this,
                ClientIslandSelectionDraft.tree(),
                difficulty,
                value,
                () -> select(value)
        ));
    }

    private void select(int value) {
        if (!ClientPlayNetworking.canSend(DifficultySelectPayload.TYPE)) return;
        selectionContinues = true;
        ClientPlayNetworking.send(new DifficultySelectPayload(value));
        ClientIslandSelectionDraft.clear();
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        text(graphics, Component.translatable("skyblockmulti.difficulty.instruction"), top() + 33, top() + 54, color(ClientPanelAppearance.get().value()));
        text(graphics, Component.translatable("skyblockmulti.difficulty.note"), top() + 155, bottom() - 34, color(ClientPanelAppearance.get().label()));
    }

    @Override
    public void onClose() {
        if (!selectionContinues && ClientPlayNetworking.canSend(SelectionMenuClosedPayload.TYPE)) {
            ClientPlayNetworking.send(SelectionMenuClosedPayload.INSTANCE);
        }
        super.onClose();
    }
}
