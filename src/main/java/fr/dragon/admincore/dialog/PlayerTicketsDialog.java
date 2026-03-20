package fr.dragon.admincore.dialog;

import fr.dragon.admincore.reports.TicketRecord;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;

public final class PlayerTicketsDialog {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault());

    private PlayerTicketsDialog() {
    }

    public static Dialog create(
        final List<TicketRecord> tickets,
        final int page,
        final boolean hasNext,
        final Consumer<TicketRecord> openCallback,
        final Runnable previousCallback,
        final Runnable nextCallback,
        final Runnable closeCallback
    ) {
        final List<ActionButton> actions = new ArrayList<>();
        for (final TicketRecord ticket : tickets) {
            actions.add(DialogHelper.button(
                Component.text("#" + ticket.id() + " | " + ticket.effectiveCategory() + " | " + ticket.status().name() + " | "
                    + DATE_FORMAT.format(ticket.timestamp())),
                320,
                DialogAction.customClick((response, audience) -> openCallback.accept(ticket), DialogHelper.singleUseOptions())
            ));
        }
        if (tickets.isEmpty()) {
            actions.add(DialogHelper.button(
                Component.text("Aucun ticket"),
                320,
                DialogAction.customClick((response, audience) -> {
                }, DialogHelper.singleUseOptions())
            ));
        }
        final List<DialogBody> body = List.of(DialogBody.plainMessage(Component.text("Selectionne un ticket pour y repondre."), 360));
        if (page > 0) {
            actions.add(DialogHelper.button(
                Component.text("Page precedente"),
                150,
                DialogAction.customClick((response, audience) -> previousCallback.run(), DialogHelper.singleUseOptions())
            ));
        }
        if (hasNext) {
            actions.add(DialogHelper.button(
                Component.text("Page suivante"),
                150,
                DialogAction.customClick((response, audience) -> nextCallback.run(), DialogHelper.singleUseOptions())
            ));
        }
        actions.add(DialogHelper.button(
            Component.text("Fermer"),
            150,
            DialogAction.customClick((response, audience) -> closeCallback.run(), DialogHelper.singleUseOptions())
        ));
        return DialogHelper.create(
            Component.text("Mes tickets"),
            body,
            List.of(),
            DialogType.multiAction(actions, null, 1)
        );
    }
}
