package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class ReasonDialog {

    private ReasonDialog() {
    }

    public static Dialog create(final String title, final String confirmLabel, final String initialReason, final DialogActionCallback callback) {
        return DialogHelper.create(
            Component.text(title),
            List.of(DialogBody.plainMessage(Component.text("Saisis une raison (120 caracteres max)."), 240)),
            List.of(DialogInput.text("reason", Component.text("Raison"))
                .width(280)
                .labelVisible(true)
                .initial(initialReason)
                .maxLength(120)
                .build()),
            DialogType.notice(DialogHelper.button(
                Component.text(confirmLabel),
                180,
                DialogAction.customClick(callback, DialogHelper.singleUseOptions())
            ))
        );
    }
}
