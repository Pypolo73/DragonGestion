package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class PlayerSearchDialog {

    private PlayerSearchDialog() {
    }

    public static Dialog create(final String title, final String initialValue, final DialogActionCallback callback) {
        return DialogHelper.create(
            Component.text(title),
            List.of(DialogBody.plainMessage(Component.text("Ecris le pseudo a rechercher."), 240)),
            List.of(DialogInput.text("query", Component.text("Pseudo"))
                .width(240)
                .labelVisible(true)
                .initial(initialValue)
                .maxLength(16)
                .build()),
            DialogType.notice(DialogHelper.button(
                Component.text("Rechercher"),
                160,
                DialogAction.customClick(callback, DialogHelper.singleUseOptions())
            ))
        );
    }
}
