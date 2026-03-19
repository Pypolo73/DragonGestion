package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class SanctionActorChoiceDialog {

    private SanctionActorChoiceDialog() {
    }

    public static Dialog create(
        final String title,
        final String moderatorName,
        final DialogActionCallback decisionCallback,
        final DialogActionCallback unspecifiedCallback,
        final DialogActionCallback moderatorCallback,
        final DialogActionCallback backCallback
    ) {
        final List<ActionButton> actions = List.of(
            DialogHelper.button(Component.text("Decision staff"), 160, DialogAction.customClick(decisionCallback, DialogHelper.singleUseOptions())),
            DialogHelper.button(Component.text("Non precise"), 150, DialogAction.customClick(unspecifiedCallback, DialogHelper.singleUseOptions())),
            DialogHelper.button(Component.text(moderatorName), 160, DialogAction.customClick(moderatorCallback, DialogHelper.singleUseOptions())),
            DialogHelper.button(Component.text("Retour"), 120, DialogAction.customClick(backCallback, DialogHelper.singleUseOptions()))
        );
        return DialogHelper.create(
            Component.text(title),
            List.of(DialogBody.plainMessage(Component.text("Choisis le texte affiche pour le staff."), 260)),
            List.of(),
            DialogType.multiAction(actions, null, 2)
        );
    }
}
