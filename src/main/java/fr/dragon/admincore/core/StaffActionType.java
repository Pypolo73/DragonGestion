package fr.dragon.admincore.core;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum StaffActionType {
    SANCTION_APPLY("Sanction", NamedTextColor.RED),
    SANCTION_REVOKE("Levee sanction", NamedTextColor.YELLOW),
    FREEZE_TOGGLE("Freeze", TextColor.color(0x7DD3FC)),
    VANISH_TOGGLE("Vanish", TextColor.color(0xC084FC)),
    INVENTORY_CLEAR("Clear inventaire", TextColor.color(0xFB7185)),
    INVENTORY_EDIT("Edition inventaire", TextColor.color(0x34D399)),
    TP_TO("Teleportation", TextColor.color(0x60A5FA)),
    TP_PULL("Pull joueur", TextColor.color(0xF59E0B)),
    TP_GROUND("Teleportation sol", TextColor.color(0x38BDF8)),
    TICKET_CREATE("Creation ticket", TextColor.color(0xFB7185)),
    TICKET_ASSIGN("Assignation ticket", TextColor.color(0xFACC15)),
    TICKET_MESSAGE("Message ticket", TextColor.color(0x60A5FA)),
    TICKET_CLOSE("Fermeture ticket", TextColor.color(0x22C55E)),
    TICKET_ARCHIVE("Archivage ticket", TextColor.color(0x94A3B8)),
    LUCKPERMS_EDIT("Edition LuckPerms", TextColor.color(0xA78BFA));

    private final String displayName;
    private final TextColor color;

    StaffActionType(final String displayName, final TextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return this.displayName;
    }

    public TextColor color() {
        return this.color;
    }
}
