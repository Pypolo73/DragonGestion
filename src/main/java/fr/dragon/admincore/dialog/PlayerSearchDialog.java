package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class PlayerSearchDialog {

    private PlayerSearchDialog() {
    }

    public static Dialog create(final DialogActionCallback callback, final Runnable backCallback) {
        return create("Rechercher un joueur", "", callback, backCallback);
    }

    public static Dialog create(final String title, final String initialValue, final DialogActionCallback callback, final Runnable backCallback) {
        final var buttons = List.of(
            DialogHelper.button(
                Component.text("Rechercher").color(NamedTextColor.GREEN),
                140,
                DialogAction.customClick(callback, DialogHelper.singleUseOptions())
            ),
            DialogHelper.button(
                Component.text("Retour")
                    .color(NamedTextColor.GRAY),
                100,
                DialogAction.customClick((response, audience) -> backCallback.run(), DialogHelper.singleUseOptions())
            )
        );
        
        return DialogHelper.create(
            Component.text(title).color(NamedTextColor.AQUA),
            List.of(
                DialogBody.plainMessage(Component.text("Entre le nom du joueur a rechercher.").color(NamedTextColor.WHITE), 320),
                DialogBody.plainMessage(Component.text("Tu peux entrer un nom partiel.").color(NamedTextColor.GRAY), 320)
            ),
            List.of(DialogInput.text("player", Component.text("Nom du joueur"))
                .width(260)
                .labelVisible(true)
                .initial(initialValue)
                .maxLength(32)
                .build()),
            DialogType.multiAction(buttons, null, 1)
        );
    }
}
