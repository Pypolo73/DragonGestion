package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class TicketCloseDialog {

    private TicketCloseDialog() {
    }

    public static Dialog create(final String targetName, final DialogActionCallback closeCallback, final DialogActionCallback cancelCallback) {
        final List<ActionButton> actions = List.of(
            DialogHelper.button(
                Component.text("Fermer"),
                140,
                DialogAction.customClick(closeCallback, DialogHelper.singleUseOptions())
            ),
            DialogHelper.button(
                Component.text("Annuler"),
                140,
                DialogAction.customClick(cancelCallback, DialogHelper.singleUseOptions())
            )
        );
        return DialogHelper.create(
            Component.text("Cloturer le ticket"),
            List.of(DialogBody.plainMessage(Component.text("Ajoute une note de cloture pour " + targetName + "."), 300)),
            List.of(DialogInput.text("note", Component.text("Note de cloture"))
                .width(240)
                .labelVisible(true)
                .maxLength(200)
                .build()),
            DialogType.multiAction(actions, null, 1)
        );
    }
}
