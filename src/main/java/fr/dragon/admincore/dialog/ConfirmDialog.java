package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    public static Dialog create(
        final String title,
        final List<Component> body,
        final String confirmLabel,
        final String backLabel,
        final DialogActionCallback confirmCallback,
        final DialogActionCallback backCallback
    ) {
        return DialogHelper.create(
            Component.text(title),
            body.stream().map(component -> DialogBody.plainMessage(component, 260)).toList(),
            List.of(),
            DialogType.confirmation(
                DialogHelper.button(Component.text(confirmLabel), 150, DialogAction.customClick(confirmCallback, DialogHelper.singleUseOptions())),
                DialogHelper.button(Component.text(backLabel), 150, DialogAction.customClick(backCallback, DialogHelper.singleUseOptions()))
            )
        );
    }
}
