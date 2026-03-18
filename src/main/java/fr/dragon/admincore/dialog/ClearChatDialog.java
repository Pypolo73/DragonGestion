package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class ClearChatDialog {

    private ClearChatDialog() {
    }

    public static Dialog create(final DialogActionCallback confirmCallback, final DialogActionCallback cancelCallback) {
        return DialogHelper.create(
            Component.text("Nettoyer le chat"),
            List.of(DialogBody.plainMessage(Component.text("Choisis les types de messages a effacer."), 240)),
            List.of(
                DialogInput.bool("public", Component.text("Messages du chat public"), true, "true", "false"),
                DialogInput.bool("players", Component.text("Messages des joueurs (bulles)"), true, "true", "false"),
                DialogInput.bool("logs", Component.text("Logs de connexion"), false, "true", "false"),
                DialogInput.bool("system", Component.text("Messages systeme"), true, "true", "false"),
                DialogInput.text("reason", Component.text("Motif visible"))
                    .width(260)
                    .labelVisible(true)
                    .initial("")
                    .maxLength(120)
                    .build()
            ),
            DialogType.confirmation(
                DialogHelper.button(Component.text("Effacer la selection"), 170, DialogAction.customClick(confirmCallback, DialogHelper.singleUseOptions())),
                DialogHelper.button(Component.text("Annuler"), 120, DialogAction.customClick(cancelCallback, DialogHelper.singleUseOptions()))
            )
        );
    }
}
