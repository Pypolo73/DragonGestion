package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public final class ReportTypeDialog {

    private ReportTypeDialog() {
    }

    public static Dialog create(
        final Runnable reportPlayerCallback,
        final Runnable reportBugCallback
    ) {
        final List<ActionButton> actions = List.of(
            DialogHelper.button(
                Component.text("Signaler un joueur")
                    .color(TextColor.color(0x3498db)),
                200,
                DialogAction.customClick((response, audience) -> reportPlayerCallback.run(), DialogHelper.singleUseOptions())
            ),
            DialogHelper.button(
                Component.text("Signaler un bug")
                    .color(TextColor.color(0x9b59b6)),
                200,
                DialogAction.customClick((response, audience) -> reportBugCallback.run(), DialogHelper.singleUseOptions())
            )
        );

        final List<DialogBody> body = List.of(
            DialogBody.plainMessage(Component.text("Quel type de signalement souhaites-tu faire ?"), 360),
            DialogBody.plainMessage(Component.text(" "), 360),
            DialogBody.plainMessage(Component.text("Choisis l'option qui correspond a ton probleme."), 360)
        );

        return DialogHelper.create(
            Component.text("Nouveau signalement"),
            body,
            List.of(),
            DialogType.multiAction(actions, null, 1)
        );
    }
}
