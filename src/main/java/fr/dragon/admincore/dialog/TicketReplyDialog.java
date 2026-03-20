package fr.dragon.admincore.dialog;

import fr.dragon.admincore.reports.TicketMessageRecord;
import fr.dragon.admincore.reports.TicketRecord;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class TicketReplyDialog {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm")
        .withZone(ZoneId.systemDefault());

    private TicketReplyDialog() {
    }

    public static Dialog create(
        final TicketRecord ticket,
        final List<TicketMessageRecord> messages,
        final DialogActionCallback sendCallback,
        final DialogActionCallback cancelCallback
    ) {
        final List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("Categorie : " + ticket.effectiveCategory()), 360));
        body.add(DialogBody.plainMessage(Component.text("Statut : " + ticket.status().name()), 360));
        body.add(DialogBody.plainMessage(Component.text("Derniers messages :"), 360));
        if (messages.isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text("Aucun message pour le moment."), 360));
        } else {
            for (final TicketMessageRecord message : messages) {
                final String prefix = message.staffMessage() ? "[Staff]" : "[Toi]";
                body.add(DialogBody.plainMessage(
                    Component.text(DATE_FORMAT.format(message.timestamp()) + " " + prefix + " " + message.authorName() + " : " + message.content()),
                    360
                ));
            }
        }
        final List<ActionButton> actions = List.of(
            DialogHelper.button(
                Component.text("Envoyer"),
                150,
                DialogAction.customClick(sendCallback, DialogHelper.singleUseOptions())
            ),
            DialogHelper.button(
                Component.text("Fermer"),
                150,
                DialogAction.customClick(cancelCallback, DialogHelper.singleUseOptions())
            )
        );
        return DialogHelper.create(
            Component.text("Repondre au ticket #" + ticket.id()),
            body,
            List.of(DialogInput.text("message", Component.text("Ta reponse"))
                .width(300)
                .labelVisible(true)
                .maxLength(200)
                .build()),
            DialogType.multiAction(actions, null, 2)
        );
    }
}
