package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class ReportDescriptionDialog {

    private ReportDescriptionDialog() {
    }

    public static Dialog create(final DialogActionCallback callback) {
        return create("Decris le probleme", "Ajoute des details si tu veux aider le staff. Ce champ est facultatif.", callback, () -> {});
    }

    public static Dialog create(final String title, final String subtitle, final DialogActionCallback callback) {
        return create(title, subtitle, callback, () -> {});
    }

    public static Dialog createWithCancel(final String title, final String subtitle, final DialogActionCallback callback, final Runnable cancelCallback) {
        final var buttons = List.of(
            DialogHelper.button(
                Component.text("Envoyer le signalement"),
                180,
                DialogAction.customClick(callback, DialogHelper.singleUseOptions())
            ),
            DialogHelper.button(
                Component.text("Retour")
                    .color(NamedTextColor.GRAY),
                100,
                DialogAction.customClick((response, audience) -> cancelCallback.run(), DialogHelper.singleUseOptions())
            )
        );
        return DialogHelper.create(
            Component.text(title),
            List.of(DialogBody.plainMessage(Component.text(subtitle), 320)),
            List.of(DialogInput.text("description", Component.text("Details"))
                .width(280)
                .labelVisible(true)
                .initial("")
                .maxLength(200)
                .build()),
            DialogType.multiAction(buttons, null, 1)
        );
    }

    public static Dialog create(final String title, final String subtitle, final DialogActionCallback callback, final Runnable cancelCallback) {
        return createWithCancel(title, subtitle, callback, cancelCallback);
    }
}
