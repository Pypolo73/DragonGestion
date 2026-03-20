package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class InventoryActionDialog {

    private InventoryActionDialog() {
    }

    public static Dialog create(
        final String targetName,
        final boolean canEdit,
        final boolean canCreateBackup,
        final Runnable onView,
        final Runnable onEdit,
        final Runnable onCreateBackup,
        final Runnable onCancel
    ) {
        final List<ActionButton> actions = new ArrayList<>();
        actions.add(DialogHelper.button(
            Component.text("Voir"),
            120,
            DialogAction.customClick((response, audience) -> onView.run(), DialogHelper.singleUseOptions())
        ));
        if (canEdit) {
            actions.add(DialogHelper.button(
                Component.text("Modifier"),
                120,
                DialogAction.customClick((response, audience) -> onEdit.run(), DialogHelper.singleUseOptions())
            ));
        }
        if (canCreateBackup) {
            actions.add(DialogHelper.button(
                Component.text("Créer un backup"),
                150,
                DialogAction.customClick((response, audience) -> onCreateBackup.run(), DialogHelper.singleUseOptions())
            ));
        }
        actions.add(DialogHelper.button(
            Component.text("Annuler"),
            120,
            DialogAction.customClick((response, audience) -> onCancel.run(), DialogHelper.singleUseOptions())
        ));
        return DialogHelper.create(
            Component.text("Gestion de l'inventaire"),
            List.of(DialogBody.plainMessage(Component.text("Choisis une action pour " + targetName + "."), 280)),
            List.of(),
            DialogType.multiAction(actions, null, 1)
        );
    }
}
