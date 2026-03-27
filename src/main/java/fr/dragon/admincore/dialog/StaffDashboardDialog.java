package fr.dragon.admincore.dialog;

import fr.dragon.admincore.core.StaffRole;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public final class StaffDashboardDialog {

    private StaffDashboardDialog() {
    }

    public static Dialog create(
        final StaffRole role,
        final Runnable statsCallback,
        final Runnable inventoryCallback,
        final Runnable sanctionsCallback,
        final Runnable lookupCallback
    ) {
        final List<ActionButton> buttons = new ArrayList<>();
        
        buttons.add(DialogHelper.button(
            Component.text("Statistiques serveur")
                .color(TextColor.color(0x3498db)),
            200,
            DialogAction.customClick((response, audience) -> statsCallback.run(), DialogHelper.singleUseOptions())
        ));
        
        buttons.add(DialogHelper.button(
            Component.text("Inventaire")
                .color(TextColor.color(0xe67e22)),
            200,
            DialogAction.customClick((response, audience) -> inventoryCallback.run(), DialogHelper.singleUseOptions())
        ));
        
        buttons.add(DialogHelper.button(
            Component.text("Sanctions")
                .color(TextColor.color(0xe74c3c)),
            200,
            DialogAction.customClick((response, audience) -> sanctionsCallback.run(), DialogHelper.singleUseOptions())
        ));
        
        buttons.add(DialogHelper.button(
            Component.text("Rechercher un joueur")
                .color(TextColor.color(0x2ecc71)),
            200,
            DialogAction.customClick((response, audience) -> lookupCallback.run(), DialogHelper.singleUseOptions())
        ));
        
        final String roleDisplay = role.displayName();
        final Component title = Component.text("Panel Staff")
            .color(TextColor.color(0xFFE45E));
        
        final List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(
            Component.text("Bienvenue dans le panel staff, ")
                .append(Component.text(roleDisplay).color(TextColor.color(0xD83BFF))), 360));
        body.add(DialogBody.plainMessage(Component.text(" "), 360));
        body.add(DialogBody.plainMessage(Component.text("Choisis une action ci-dessous:"), 360));
        
        return DialogHelper.create(
            title,
            body,
            List.of(),
            DialogType.multiAction(buttons, null, 1)
        );
    }
}
