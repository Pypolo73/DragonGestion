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

public final class StaffAssignmentDialog {

    private StaffAssignmentDialog() {
    }

    public static Dialog create(
        final DialogActionCallback guideCallback,
        final DialogActionCallback moderatorCallback,
        final DialogActionCallback adminCallback,
        final DialogActionCallback cancelCallback
    ) {
        final List<ActionButton> actions = List.of(
            DialogHelper.button(Component.text("Guide"), 120, DialogAction.customClick(guideCallback, DialogHelper.singleUseOptions())),
            DialogHelper.button(Component.text("Modo"), 120, DialogAction.customClick(moderatorCallback, DialogHelper.singleUseOptions())),
            DialogHelper.button(Component.text("Admin"), 120, DialogAction.customClick(adminCallback, DialogHelper.singleUseOptions())),
            DialogHelper.button(Component.text("Annuler"), 120, DialogAction.customClick(cancelCallback, DialogHelper.singleUseOptions()))
        );
        return DialogHelper.create(
            Component.text("Ajouter au staff"),
            List.of(DialogBody.plainMessage(Component.text("Entre le pseudo, puis choisis le role a attribuer."), 280)),
            List.of(DialogInput.text("player", Component.text("Pseudo"))
                .width(240)
                .labelVisible(true)
                .maxLength(32)
                .build()),
            DialogType.multiAction(actions, null, 2)
        );
    }
}
