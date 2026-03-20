package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class ReportDescriptionDialog {

    private ReportDescriptionDialog() {
    }

    public static Dialog create(final DialogActionCallback callback) {
        return DialogHelper.create(
            Component.text("Decris le probleme"),
            List.of(DialogBody.plainMessage(Component.text("Ajoute des details si tu veux aider le staff. Ce champ est facultatif."), 320)),
            List.of(DialogInput.text("description", Component.text("Details"))
                .width(280)
                .labelVisible(true)
                .initial("")
                .maxLength(200)
                .build()),
            DialogType.notice(DialogHelper.button(
                Component.text("Envoyer le signalement"),
                180,
                DialogAction.customClick(callback, DialogHelper.singleUseOptions())
            ))
        );
    }
}
