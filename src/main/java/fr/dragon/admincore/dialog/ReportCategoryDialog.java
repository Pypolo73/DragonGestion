package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class ReportCategoryDialog {

    private ReportCategoryDialog() {
    }

    public static Dialog create(final List<String> categories, final DialogActionCallback callback) {
        final List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
        for (int index = 0; index < categories.size(); index++) {
            final String category = categories.get(index);
            entries.add(SingleOptionDialogInput.OptionEntry.create(category, Component.text(category), index == 0));
        }
        return DialogHelper.create(
            Component.text("Categorie du signalement"),
            List.of(DialogBody.plainMessage(Component.text("Choisis la categorie la plus proche du probleme."), 320)),
            List.of(DialogInput.singleOption("categorie", 220, entries, Component.text("Categorie"), true)),
            DialogType.notice(DialogHelper.button(
                Component.text("Suivant"),
                140,
                DialogAction.customClick(callback, DialogHelper.singleUseOptions())
            ))
        );
    }
}
