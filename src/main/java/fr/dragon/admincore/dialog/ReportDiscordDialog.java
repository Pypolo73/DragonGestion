package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class ReportDiscordDialog {

    private ReportDiscordDialog() {
    }

    public static Dialog create(final String targetName, final DialogActionCallback callback) {
        return DialogHelper.create(
            Component.text("Signaler " + targetName),
            List.of(DialogBody.plainMessage(Component.text("Tu peux renseigner ton pseudo Discord pour faciliter la prise en charge."), 320)),
            List.of(DialogInput.text("discord", Component.text("Ton pseudo Discord"))
                .width(260)
                .labelVisible(true)
                .initial("")
                .maxLength(64)
                .build()),
            DialogType.notice(DialogHelper.button(
                Component.text("Suivant"),
                140,
                DialogAction.customClick(callback, DialogHelper.singleUseOptions())
            ))
        );
    }
}
