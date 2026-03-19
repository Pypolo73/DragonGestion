package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public final class DurationDialog {

    private DurationDialog() {
    }

    public static Dialog create(
        final String title,
        final String initialValue,
        final DialogActionCallback confirmCallback,
        final DialogActionCallback cancelCallback
    ) {
        return DialogHelper.create(
            Component.text(title),
            List.of(DialogBody.plainMessage(Component.text("Saisis une duree libre. Exemples: 30m, 1h, 3d, 1w, 1mo."), 280)),
            List.of(
                DialogInput.text("duration", Component.text("Duree"))
                    .width(240)
                    .labelVisible(true)
                    .initial(initialValue)
                    .maxLength(12)
                    .build()
            ),
            DialogType.confirmation(
                DialogHelper.button(
                    Component.text("Confirmer", TextColor.color(0xCC3333)),
                    160,
                    DialogAction.customClick(confirmCallback, DialogHelper.singleUseOptions())
                ),
                DialogHelper.button(
                    Component.text("Annuler", TextColor.color(0x888888)),
                    140,
                    DialogAction.customClick(cancelCallback, DialogHelper.singleUseOptions())
                )
            )
        );
    }
}
