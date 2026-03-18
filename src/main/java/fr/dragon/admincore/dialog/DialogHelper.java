package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;

public final class DialogHelper {

    private DialogHelper() {
    }

    public static Dialog create(
        final Component title,
        final List<? extends DialogBody> body,
        final List<? extends DialogInput> inputs,
        final DialogType type
    ) {
        final DialogBase base = DialogBase.builder(title)
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .body(body)
            .inputs(inputs)
            .build();
        return Dialog.create(factory -> factory.empty().base(base).type(type));
    }

    public static ClickCallback.Options singleUseOptions() {
        return ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(5))
            .build();
    }

    public static ActionButton button(final Component label, final int width, final DialogAction action) {
        return ActionButton.builder(label).width(width).action(action).build();
    }
}
