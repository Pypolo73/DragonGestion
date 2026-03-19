package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class PlayerPickerDialog {

    private PlayerPickerDialog() {
    }

    public static Dialog create(final String title, final Collection<? extends Player> players, final Consumer<Player> callback) {
        final List<ActionButton> actions = new ArrayList<>();
        for (final Player player : players) {
            actions.add(DialogHelper.button(
                Component.text(player.getName()),
                60,
                DialogAction.customClick((response, audience) -> callback.accept(player), DialogHelper.singleUseOptions())
            ));
        }
        return DialogHelper.create(
            Component.text(title),
            List.of(DialogBody.plainMessage(Component.text("Selectionne un joueur en ligne."), 170)),
            List.of(),
            DialogType.multiAction(actions, null, 3)
        );
    }
}
