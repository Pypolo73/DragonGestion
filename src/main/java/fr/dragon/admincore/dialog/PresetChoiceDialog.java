package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class PresetChoiceDialog {

    private PresetChoiceDialog() {
    }

    public static Dialog create(
        final String title,
        final String subtitle,
        final List<String> options,
        final Consumer<String> optionCallback,
        final String customLabel,
        final Runnable customCallback,
        final String backLabel,
        final Runnable backCallback,
        final int columns
    ) {
        final List<ActionButton> actions = new ArrayList<>();
        for (final String option : options) {
            actions.add(DialogHelper.button(
                Component.text(option).color(NamedTextColor.WHITE),
                160,
                DialogAction.customClick((response, audience) -> optionCallback.accept(option), DialogHelper.singleUseOptions())
            ));
        }
        actions.add(DialogHelper.button(
            Component.text(customLabel).color(NamedTextColor.GOLD),
            200,
            DialogAction.customClick((response, audience) -> customCallback.run(), DialogHelper.singleUseOptions())
        ));
        actions.add(DialogHelper.button(
            Component.text(backLabel).color(NamedTextColor.GRAY),
            160,
            DialogAction.customClick((response, audience) -> backCallback.run(), DialogHelper.singleUseOptions())
        ));
        return DialogHelper.create(
            Component.text(title).color(NamedTextColor.AQUA),
            List.of(DialogBody.plainMessage(Component.text(subtitle).color(NamedTextColor.WHITE), 360)),
            List.of(),
            DialogType.multiAction(actions, null, Math.max(1, columns))
        );
    }
}
