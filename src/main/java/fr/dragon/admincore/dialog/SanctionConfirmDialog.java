package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class SanctionConfirmDialog {

    private SanctionConfirmDialog() {
    }

    public static Dialog create(
        final String title,
        final List<Component> body,
        final DialogActionCallback confirmCallback,
        final DialogActionCallback actorChoiceCallback,
        final DialogActionCallback backCallback
    ) {
        final List<ActionButton> actions = List.of(
            DialogHelper.button(Component.text("Confirmer"), 140, DialogAction.customClick(confirmCallback, DialogHelper.singleUseOptions())),
            DialogHelper.button(Component.text("Choisir affichage"), 180, DialogAction.customClick(actorChoiceCallback, DialogHelper.singleUseOptions())),
            DialogHelper.button(Component.text("Retour"), 120, DialogAction.customClick(backCallback, DialogHelper.singleUseOptions()))
        );
        return DialogHelper.create(
            Component.text(title),
            body.stream().map(component -> DialogBody.plainMessage(component, 260)).toList(),
            List.of(),
            DialogType.multiAction(actions, null, 3)
        );
    }
}
