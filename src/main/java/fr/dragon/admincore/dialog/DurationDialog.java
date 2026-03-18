package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public final class DurationDialog {

    private DurationDialog() {
    }

    public static Dialog create(
        final String title,
        final int initialAmount,
        final String initialUnit,
        final DialogActionCallback confirmCallback,
        final DialogActionCallback cancelCallback
    ) {
        return DialogHelper.create(
            Component.text(title),
            List.of(DialogBody.plainMessage(Component.text("Choisis la duree de la sanction."), 260)),
            List.of(
                DialogInput.numberRange("amount", Component.text("Valeur"), 1.0F, 9_999.0F)
                    .width(260)
                    .labelFormat("%s: %s")
                    .initial((float) initialAmount)
                    .step(1.0F)
                    .build(),
                DialogInput.singleOption(
                    "unit",
                    Component.text("Unite"),
                    List.of(
                        SingleOptionDialogInput.OptionEntry.create("minutes", Component.text("Minutes"), "minutes".equalsIgnoreCase(initialUnit)),
                        SingleOptionDialogInput.OptionEntry.create("hours", Component.text("Heures"), "hours".equalsIgnoreCase(initialUnit)),
                        SingleOptionDialogInput.OptionEntry.create("days", Component.text("Jours"), "days".equalsIgnoreCase(initialUnit)),
                        SingleOptionDialogInput.OptionEntry.create("weeks", Component.text("Semaines"), "weeks".equalsIgnoreCase(initialUnit)),
                        SingleOptionDialogInput.OptionEntry.create("months", Component.text("Mois"), "months".equalsIgnoreCase(initialUnit))
                    )
                ).width(260).labelVisible(true).build()
            ),
            DialogType.confirmation(
                DialogHelper.button(
                    Component.text("Confirmer le bannissement", TextColor.color(0xCC3333)),
                    180,
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
