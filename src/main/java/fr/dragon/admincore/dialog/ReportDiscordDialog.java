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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public final class ReportDiscordDialog {

    private ReportDiscordDialog() {
    }

    public static Dialog createWithCancel(final String targetName, final DialogActionCallback confirmCallback, final Runnable skipDiscordCallback, final Runnable cancelCallback) {
        final var buttons = List.of(
            DialogHelper.button(
                Component.text("Suivant")
                    .color(TextColor.color(0x2ecc71)),
                120,
                DialogAction.customClick(confirmCallback, DialogHelper.singleUseOptions())
            ),
            DialogHelper.button(
                Component.text("Pas de Discord")
                    .color(NamedTextColor.RED),
                140,
                DialogAction.customClick((response, audience) -> skipDiscordCallback.run(), DialogHelper.singleUseOptions())
            ),
            DialogHelper.button(
                Component.text("Retour")
                    .color(NamedTextColor.GRAY),
                100,
                DialogAction.customClick((response, audience) -> cancelCallback.run(), DialogHelper.singleUseOptions())
            )
        );
        return DialogHelper.create(
            Component.text("Signaler " + targetName),
            List.of(DialogBody.plainMessage(Component.text("Ton pseudo Discord (facultatif)"), 320)),
            List.of(DialogInput.text("discord", Component.text("Pseudo Discord"))
                .width(260)
                .labelVisible(true)
                .initial("")
                .maxLength(64)
                .build()),
            DialogType.multiAction(buttons, null, 1)
        );
    }

    public static Dialog create(final String targetName, final DialogActionCallback confirmCallback, final Runnable skipDiscordCallback) {
        return createWithCancel(targetName, confirmCallback, skipDiscordCallback, () -> {});
    }

    public static Dialog create(final String targetName, final DialogActionCallback callback) {
        return create(targetName, callback, () -> {
        });
    }
}
